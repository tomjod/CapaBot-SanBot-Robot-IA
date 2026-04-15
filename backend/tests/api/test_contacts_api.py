from __future__ import annotations

import unittest

from backend.app.api.routes.contacts import (
    full_contact_response,
    list_contacts_response,
    register_contacts_routes,
)
from backend.app.presentation.http.contacts_router import (
    full_contact_response as presentation_full_contact_response,
    list_contacts_response as presentation_list_contacts_response,
    register_contacts_routes as presentation_register_contacts_routes,
)
from backend.app.domain.contact import Contact


class StaticRepository:
    def __init__(self, contacts: list[Contact]) -> None:
        self._contacts = contacts

    def list_contacts(self) -> list[Contact]:
        return self._contacts


class ContactsRouteContractTest(unittest.TestCase):
    def test_legacy_module_bridges_to_presentation_router_contracts(self) -> None:
        self.assertIs(list_contacts_response, presentation_list_contacts_response)
        self.assertIs(full_contact_response, presentation_full_contact_response)
        self.assertIs(register_contacts_routes, presentation_register_contacts_routes)

    def test_returns_contacts_contract_with_availability_shape(self) -> None:
        repository = StaticRepository(
            [
                Contact(
                    id="ventas-1",
                    display_name="Ventas",
                    job_title="Ejecutiva comercial",
                    company="transformapp",
                    phone="+56 9 1234 5678",
                    telegram_chat_id="chat-1",
                ),
                Contact(
                    id="demo-1",
                    display_name="Demo",
                    job_title="Especialista demo",
                    company="tra",
                    phone="+56 9 1234 5679",
                    email="demo@example.com",
                    email_enabled=True,
                ),
            ]
        )

        payload = list_contacts_response(repository)

        self.assertEqual(payload[0], {
            "id": "ventas-1",
            "display_name": "Ventas",
            "job_title": "Ejecutiva comercial",
            "company": "transformapp",
            "company_label": "Transformapp",
            "enabled": True,
            "phone": "+56 9 1234 5678",
            "channels": {"telegram": True, "email": False},
            "available": True,
        })
        self.assertEqual(payload[1]["channels"], {"telegram": False, "email": True})
        self.assertTrue(payload[1]["available"])

    def test_marks_unavailable_contacts_explicitly(self) -> None:
        repository = StaticRepository(
            [
                Contact(
                    id="ops-1",
                    display_name="Operaciones",
                    job_title="Coordinador",
                    company="data_center",
                    phone="+56 9 1111 1111",
                    telegram_chat_id=None,
                    email=None,
                    email_enabled=False,
                )
            ]
        )

        payload = list_contacts_response(repository)

        self.assertEqual(payload, [{
            "id": "ops-1",
            "display_name": "Operaciones",
            "job_title": "Coordinador",
            "company": "data_center",
            "company_label": "Data Center",
            "enabled": True,
            "phone": "+56 9 1111 1111",
            "channels": {"telegram": False, "email": False},
            "available": False,
        }])

    def test_full_response_exposes_company_aliases_and_telegram_binding_fields(self) -> None:
        payload = full_contact_response(
            Contact(
                id="ventas-1",
                display_name="Ventas",
                job_title="Ejecutiva comercial",
                company="transformapp",
                phone="+56 9 1234 5678",
                telegram_chat_id="777",
                telegram_user_id="888",
                telegram_username="ventas_contacto",
                email="ventas@example.com",
                email_enabled=True,
            )
        )

        self.assertEqual(payload["cargo"], "Ejecutiva comercial")
        self.assertEqual(payload["empresa"], "transformapp")
        self.assertEqual(payload["telefono"], "+56 9 1234 5678")
        self.assertEqual(payload["telegram_user_id"], "888")
        self.assertEqual(payload["telegram_username"], "ventas_contacto")
        self.assertTrue(payload["enabled"])

    def test_manual_disable_overrides_operational_channels(self) -> None:
        payload = full_contact_response(
            Contact(
                id="ventas-2",
                display_name="Ventas",
                job_title="Ejecutiva comercial",
                company="transformapp",
                enabled=False,
                phone="+56 9 1234 5680",
                telegram_chat_id="777",
                email="ventas@example.com",
                email_enabled=True,
            )
        )

        self.assertEqual(payload["channels"], {"telegram": True, "email": True})
        self.assertFalse(payload["available"])
        self.assertFalse(payload["enabled"])


if __name__ == "__main__":
    unittest.main()
