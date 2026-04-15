from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from fastapi import FastAPI
from fastapi.testclient import TestClient

from backend.app.api.routes.contacts import register_contacts_routes
from backend.app.infra.json_contacts import JsonContactRepository


class ContactsCrudEndpointsTest(unittest.TestCase):
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
        app = FastAPI()
        register_contacts_routes(app, JsonContactRepository(self.file_path))
        self.client = TestClient(app)

    def test_crud_endpoints_work_end_to_end(self) -> None:
        created = self.client.post(
            "/contacts",
            json={
                "id": "rrhh",
                "display_name": "RRHH",
                "job_title": "People partner",
                "company": "tps",
                "enabled": False,
                "phone": "+56 9 9999 0000",
                "email": "rrhh@example.com",
                "email_enabled": True,
            },
        )

        self.assertEqual(created.status_code, 201)
        self.assertEqual(created.json()["channels"], {"telegram": False, "email": True})
        self.assertFalse(created.json()["available"])
        self.assertFalse(created.json()["enabled"])

        fetched = self.client.get("/contacts/rrhh")

        self.assertEqual(fetched.status_code, 200)
        self.assertEqual(fetched.json()["display_name"], "RRHH")
        self.assertEqual(fetched.json()["company_label"], "TPS")
        self.assertEqual(fetched.json()["email"], "rrhh@example.com")
        self.assertFalse(fetched.json()["enabled"])

        updated = self.client.put(
            "/contacts/rrhh",
            json={
                "display_name": "People Ops",
                "job_title": "People lead",
                "company": "micro_renta",
                "enabled": True,
                "phone": "+56 9 9999 1111",
                "email": "ops@example.com",
                "email_enabled": False,
            },
        )

        self.assertEqual(updated.status_code, 200)
        self.assertEqual(updated.json()["display_name"], "People Ops")
        self.assertEqual(updated.json()["company"], "micro_renta")
        self.assertTrue(updated.json()["enabled"])
        self.assertEqual(updated.json()["channels"], {"telegram": False, "email": False})

        companies = self.client.get("/contacts/companies")
        self.assertEqual(companies.status_code, 200)
        self.assertGreaterEqual(len(companies.json()), 3)

        deleted = self.client.delete("/contacts/rrhh")

        self.assertEqual(deleted.status_code, 204)
        self.assertEqual(self.client.get("/contacts/rrhh").status_code, 404)

    def test_create_endpoint_reads_json_body_instead_of_query_param(self) -> None:
        response = self.client.post(
            "/contacts",
            json={
                "id": "soporte",
                "display_name": "Soporte",
                "job_title": "Analista",
                "company": "transformapp",
                "phone": "+56 9 3333 4444",
            },
        )

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["id"], "soporte")

    def test_rejects_duplicate_create_and_missing_delete(self) -> None:
        duplicate = self.client.post(
            "/contacts",
            json={
                "id": "ventas",
                "display_name": "Ventas duplicado",
                "job_title": "Duplicado",
                "company": "transformapp",
                "phone": "+56 9 1111 2222",
            },
        )
        missing_delete = self.client.delete("/contacts/no-existe")

        self.assertEqual(duplicate.status_code, 400)
        self.assertIn("already exists", duplicate.json()["detail"])
        self.assertEqual(missing_delete.status_code, 404)

    def test_rejects_unknown_company_catalog_entries(self) -> None:
        invalid = self.client.post(
            "/contacts",
            json={
                "id": "rrhh",
                "display_name": "RRHH",
                "job_title": "People partner",
                "company": "empresa-invalida",
                "phone": "+56 9 9999 0000",
            },
        )

        self.assertEqual(invalid.status_code, 400)
        self.assertIn("configured catalog", invalid.json()["detail"])


if __name__ == "__main__":
    unittest.main()
