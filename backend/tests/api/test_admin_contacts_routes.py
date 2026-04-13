from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from fastapi import FastAPI
from fastapi.testclient import TestClient

from backend.app.api.routes.admin_contacts import register_admin_contacts_routes
from backend.app.infra.json_contacts import JsonContactRepository
from backend.app.infra.json_pending_registrations import JsonPendingTelegramRegistrationRepository


class AdminContactsRoutesTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp_dir.cleanup)
        self.file_path = Path(self.temp_dir.name) / "contacts.json"
        self.file_path.write_text(
            json.dumps(
                [
                    {
                        "id": "ventas",
                        "display_name": "Ventas",
                        "job_title": "Ejecutiva comercial",
                        "company": "transformapp",
                        "enabled": True,
                        "phone": "+56 9 1234 5678",
                        "telegram_chat_id": "123",
                        "telegram_user_id": "321",
                        "email": "ventas@example.com",
                        "email_enabled": True,
                    }
                ]
            ),
            encoding="utf-8",
        )
        self.pending_file_path = Path(self.temp_dir.name) / "pending_registrations.json"
        self.pending_file_path.write_text(
            json.dumps(
                [
                    {
                        "phone_number": "+56 9 0000 0000",
                        "normalized_phone": "56900000000",
                        "telegram_user_id": "444",
                        "telegram_chat_id": "555",
                        "telegram_username": "ada_recepciones",
                        "first_name": "Ada",
                        "company": "transformapp",
                        "job_title": "Recepcionista",
                        "flow_state": "ready_for_approval",
                        "status": "pending",
                        "created_at": "2026-04-13T12:00:00Z",
                    }
                ]
            ),
            encoding="utf-8",
        )
        app = FastAPI()
        register_admin_contacts_routes(
            app,
            JsonContactRepository(self.file_path),
            JsonPendingTelegramRegistrationRepository(self.pending_file_path),
        )
        self.client = TestClient(app)

    def test_admin_page_renders_contact_list(self) -> None:
        response = self.client.get("/admin/contacts")

        self.assertEqual(response.status_code, 200)
        self.assertIn("Administración de Contactos", response.text)
        self.assertIn("Ventas", response.text)
        self.assertIn("Cargo", response.text)
        self.assertIn("Empresa", response.text)
        self.assertIn("Disponible", response.text)
        self.assertNotIn("Telegram Chat ID", response.text)

    def test_admin_page_renders_pending_registration_requests(self) -> None:
        response = self.client.get("/admin/contacts")

        self.assertEqual(response.status_code, 200)
        self.assertIn("Solicitudes pendientes de registro Telegram", response.text)
        self.assertIn("ada_recepciones", response.text)
        self.assertIn("56900000000", response.text)
        self.assertIn("2026-04-13T12:00:00Z", response.text)
        self.assertIn("Transformapp", response.text)
        self.assertIn("Recepcionista", response.text)
        self.assertIn("Aprobar y crear contacto", response.text)

    def test_admin_can_approve_pending_registration_into_contact(self) -> None:
        response = self.client.post(
            "/admin/pending-registrations/approve",
            data={
                "normalized_phone": "56900000000",
                "telegram_user_id": "444",
                "display_name": "Ada Recepciones",
                "job_title": "Recepcionista",
                "company": "transformapp",
                "enabled": "true",
                "phone": "+56 9 0000 0000",
                "email": "",
                "email_enabled": "false",
            },
            follow_redirects=False,
        )

        self.assertEqual(response.status_code, 303)
        self.assertEqual(response.headers["location"], "/admin/contacts")

        contacts_payload = json.loads(self.file_path.read_text(encoding="utf-8"))
        self.assertEqual(len(contacts_payload), 2)
        self.assertEqual(
            contacts_payload[1],
            {
                "id": "ta-recepcionista-001",
                "display_name": "Ada Recepciones",
                "job_title": "Recepcionista",
                "company": "transformapp",
                "enabled": True,
                "phone": "+56 9 0000 0000",
                "telegram_chat_id": "555",
                "telegram_user_id": "444",
                "telegram_username": "ada_recepciones",
                "email": None,
                "email_enabled": False,
            },
        )

        pending_payload = json.loads(self.pending_file_path.read_text(encoding="utf-8"))
        self.assertEqual(pending_payload[0]["status"], "approved")

        follow_up = self.client.get("/admin/contacts")
        self.assertEqual(follow_up.status_code, 200)
        self.assertNotIn("ada_recepciones", follow_up.text)

    def test_admin_approval_generates_next_unique_contact_id(self) -> None:
        self.file_path.write_text(
            json.dumps(
                [
                    {
                        "id": "ventas",
                        "display_name": "Ventas",
                        "job_title": "Ejecutiva comercial",
                        "company": "transformapp",
                        "enabled": True,
                        "phone": "+56 9 1234 5678",
                        "telegram_chat_id": "123",
                        "telegram_user_id": "321",
                        "email": "ventas@example.com",
                        "email_enabled": True,
                    },
                    {
                        "id": "ta-recepcionista-001",
                        "display_name": "Recepción Turno A",
                        "job_title": "Recepcionista",
                        "company": "transformapp",
                        "enabled": True,
                        "phone": "+56 9 1000 0000",
                        "telegram_chat_id": None,
                        "telegram_user_id": None,
                        "email": None,
                        "email_enabled": False,
                    },
                ]
            ),
            encoding="utf-8",
        )

        response = self.client.post(
            "/admin/pending-registrations/approve",
            data={
                "normalized_phone": "56900000000",
                "telegram_user_id": "444",
                "display_name": "Ada Recepciones",
                "job_title": "Recepcionista",
                "company": "transformapp",
                "enabled": "true",
                "phone": "+56 9 0000 0000",
                "email": "",
                "email_enabled": "false",
            },
            follow_redirects=False,
        )

        self.assertEqual(response.status_code, 303)
        contacts_payload = json.loads(self.file_path.read_text(encoding="utf-8"))
        self.assertEqual(contacts_payload[-1]["id"], "ta-recepcionista-002")

    def test_admin_cannot_approve_incomplete_pending_registration(self) -> None:
        self.pending_file_path.write_text(
            json.dumps(
                [
                    {
                        "phone_number": "+56 9 0000 0000",
                        "normalized_phone": "56900000000",
                        "telegram_user_id": "444",
                        "telegram_chat_id": "555",
                        "telegram_username": "ada_recepciones",
                        "first_name": "Ada",
                        "company": "transformapp",
                        "job_title": None,
                        "flow_state": "awaiting_job_title",
                        "status": "pending",
                        "created_at": "2026-04-13T12:00:00Z",
                    }
                ]
            ),
            encoding="utf-8",
        )

        response = self.client.post(
            "/admin/pending-registrations/approve",
            data={
                "normalized_phone": "56900000000",
                "telegram_user_id": "444",
                "display_name": "Ada Recepciones",
                "job_title": " ",
                "company": "transformapp",
                "enabled": "true",
                "phone": "+56 9 0000 0000",
                "email": "",
                "email_enabled": "false",
            },
            follow_redirects=False,
        )

        self.assertEqual(response.status_code, 303)
        self.assertIn("error_mode=approve", response.headers["location"])
        self.assertIn("incomplete", response.headers["location"])

    def test_admin_approval_ignores_tampered_telegram_hidden_fields(self) -> None:
        response = self.client.post(
            "/admin/pending-registrations/approve",
            data={
                "normalized_phone": "56900000000",
                "telegram_user_id": "444",
                "telegram_chat_id": "999999",
                "telegram_username": "mallory_override",
                "first_name": "Mallory",
                "display_name": "Recepción Segura",
                "job_title": "Recepcionista",
                "company": "transformapp",
                "enabled": "true",
                "phone": "+56 9 0000 0000",
                "email": "",
                "email_enabled": "false",
            },
            follow_redirects=False,
        )

        self.assertEqual(response.status_code, 303)
        self.assertEqual(response.headers["location"], "/admin/contacts")

        contacts_payload = json.loads(self.file_path.read_text(encoding="utf-8"))
        self.assertEqual(contacts_payload[1]["telegram_chat_id"], "555")
        self.assertEqual(contacts_payload[1]["telegram_user_id"], "444")
        self.assertEqual(contacts_payload[1]["telegram_username"], "ada_recepciones")

    def test_admin_approval_of_nonexistent_pending_registration_redirects_safely(self) -> None:
        response = self.client.post(
            "/admin/pending-registrations/approve",
            data={
                "normalized_phone": "56999999999",
                "telegram_user_id": "999",
                "display_name": "Recepción Fantasma",
                "job_title": "Recepcionista",
                "company": "transformapp",
                "enabled": "true",
                "phone": "+56 9 9999 9999",
                "email": "",
                "email_enabled": "false",
            },
            follow_redirects=False,
        )

        self.assertEqual(response.status_code, 303)
        self.assertIn("error_mode=approve", response.headers["location"])
        self.assertIn("Pending+Telegram+registration+not+found", response.headers["location"])

        contacts_payload = json.loads(self.file_path.read_text(encoding="utf-8"))
        self.assertEqual(len(contacts_payload), 1)

    def test_admin_approval_of_already_resolved_registration_does_not_create_contact(self) -> None:
        first_response = self.client.post(
            "/admin/pending-registrations/approve",
            data={
                "normalized_phone": "56900000000",
                "telegram_user_id": "444",
                "display_name": "Ada Recepciones",
                "job_title": "Recepcionista",
                "company": "transformapp",
                "enabled": "true",
                "phone": "+56 9 0000 0000",
                "email": "",
                "email_enabled": "false",
            },
            follow_redirects=False,
        )
        self.assertEqual(first_response.status_code, 303)

        second_response = self.client.post(
            "/admin/pending-registrations/approve",
            data={
                "normalized_phone": "56900000000",
                "telegram_user_id": "444",
                "display_name": "Ada Duplicada",
                "job_title": "Recepcionista",
                "company": "transformapp",
                "enabled": "true",
                "phone": "+56 9 0000 0000",
                "email": "",
                "email_enabled": "false",
            },
            follow_redirects=False,
        )

        self.assertEqual(second_response.status_code, 303)
        self.assertIn("error_mode=approve", second_response.headers["location"])
        self.assertIn("Pending+Telegram+registration+not+found", second_response.headers["location"])

        contacts_payload = json.loads(self.file_path.read_text(encoding="utf-8"))
        self.assertEqual(len(contacts_payload), 2)

    def test_admin_can_mark_pending_registration_as_resolved_without_creating_contact(self) -> None:
        response = self.client.post(
            "/admin/pending-registrations/resolve",
            data={
                "normalized_phone": "56900000000",
                "telegram_user_id": "444",
            },
            follow_redirects=False,
        )

        self.assertEqual(response.status_code, 303)
        self.assertEqual(response.headers["location"], "/admin/contacts")

        pending_payload = json.loads(self.pending_file_path.read_text(encoding="utf-8"))
        self.assertEqual(pending_payload[0]["status"], "resolved")

        follow_up = self.client.get("/admin/contacts")
        self.assertEqual(follow_up.status_code, 200)
        self.assertNotIn("ada_recepciones", follow_up.text)

    def test_admin_create_redirects_with_visible_error_feedback(self) -> None:
        response = self.client.post(
            "/admin/contacts",
            data={
                "id": "ventas",
                "display_name": "Ventas duplicadas",
                "job_title": "Gerente comercial",
                "company": "transformapp",
                "enabled": "true",
                "phone": "+56 9 9999 0000",
                "email": "ventas2@example.com",
                "email_enabled": "true",
            },
            follow_redirects=False,
        )

        self.assertEqual(response.status_code, 303)
        self.assertIn("error_mode=create", response.headers["location"])
        self.assertIn("already+exists", response.headers["location"])

        follow_up = self.client.get(response.headers["location"])

        self.assertEqual(follow_up.status_code, 200)
        self.assertIn("Contact with id &#39;ventas&#39; already exists", follow_up.text)
        self.assertIn('id="createModal"', follow_up.text)
        self.assertIn('value="Ventas duplicadas"', follow_up.text)
        self.assertIn('value="Gerente comercial"', follow_up.text)

    def test_admin_update_redirects_with_visible_error_feedback(self) -> None:
        response = self.client.post(
            "/admin/contacts/ventas/update",
            data={
                "display_name": " ",
                "job_title": "Recepción",
                "company": "transformapp",
                "enabled": "true",
                "phone": "+56 9 8888 7777",
                "email": "ventas@example.com",
                "email_enabled": "true",
            },
            follow_redirects=False,
        )

        self.assertEqual(response.status_code, 303)
        self.assertIn("error_mode=update", response.headers["location"])
        self.assertIn("display_name", response.headers["location"])

        follow_up = self.client.get(response.headers["location"])

        self.assertEqual(follow_up.status_code, 200)
        self.assertIn("display_name must be a non-empty string", follow_up.text)
        self.assertIn('id="editForm" method="post" action="/admin/contacts/ventas/update"', follow_up.text)
        self.assertIn('id="edit-phone" name="phone"', follow_up.text)
        self.assertIn('value="+56 9 8888 7777"', follow_up.text)

    def test_admin_page_uses_data_attributes_for_edit_actions(self) -> None:
        self.file_path.write_text(
            json.dumps(
                [
                    {
                        "id": "ventas",
                        "display_name": 'Ventas "North"',
                        "job_title": 'Gerente "North"',
                        "company": "transformapp",
                        "enabled": False,
                        "phone": "+56 9 1111 2222",
                        "telegram_chat_id": "12'3",
                        "email": "ventas@example.com",
                        "email_enabled": True,
                    }
                ]
            ),
            encoding="utf-8",
        )

        response = self.client.get("/admin/contacts")

        self.assertEqual(response.status_code, 200)
        self.assertIn("js-edit-contact", response.text)
        self.assertIn('data-contact-display-name="Ventas &#34;North&#34;"', response.text)
        self.assertIn('data-contact-job-title="Gerente &#34;North&#34;"', response.text)
        self.assertIn('data-contact-enabled="false"', response.text)
        self.assertNotIn("showEditModal('{{", response.text)

    def test_admin_forms_create_update_and_delete_contacts(self) -> None:
        created = self.client.post(
            "/admin/contacts",
            data={
                "id": "legal",
                "display_name": "Legal",
                "job_title": "Abogada",
                "company": "tps",
                "enabled": "false",
                "phone": "+56 9 7000 0000",
                "email": "legal@example.com",
                "email_enabled": "true",
            },
            follow_redirects=False,
        )
        updated = self.client.post(
            "/admin/contacts/legal/update",
            data={
                "display_name": "Legal Team",
                "job_title": "Abogada senior",
                "company": "lb",
                "enabled": "true",
                "phone": "+56 9 7000 1111",
                "email": "legal@example.com",
                "email_enabled": "true",
            },
            follow_redirects=False,
        )
        deleted = self.client.post(
            "/admin/contacts/legal/delete",
            follow_redirects=False,
        )

        self.assertEqual(created.status_code, 303)
        self.assertEqual(created.headers["location"], "/admin/contacts")
        self.assertEqual(updated.status_code, 303)
        self.assertEqual(deleted.status_code, 303)

        payload = json.loads(self.file_path.read_text(encoding="utf-8"))
        self.assertEqual(payload, [
            {
                "id": "ventas",
                "display_name": "Ventas",
                "job_title": "Ejecutiva comercial",
                "company": "transformapp",
                "enabled": True,
                "phone": "+56 9 1234 5678",
                "telegram_chat_id": "123",
                "telegram_user_id": "321",
                "email": "ventas@example.com",
                "email_enabled": True,
            }
        ])


if __name__ == "__main__":
    unittest.main()
