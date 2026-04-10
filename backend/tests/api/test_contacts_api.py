from __future__ import annotations

import unittest

from backend.app.api.routes.contacts import list_contacts_response
from backend.app.domain.contact import Contact


class StaticRepository:
    def __init__(self, contacts: list[Contact]) -> None:
        self._contacts = contacts

    def list_contacts(self) -> list[Contact]:
        return self._contacts


class ContactsRouteContractTest(unittest.TestCase):
    def test_returns_contacts_contract_with_availability_shape(self) -> None:
        repository = StaticRepository(
            [
                Contact(id="ventas-1", display_name="Ventas", telegram_chat_id="chat-1"),
                Contact(id="demo-1", display_name="Demo", email="demo@example.com", email_enabled=True),
            ]
        )

        payload = list_contacts_response(repository)

        self.assertEqual(payload[0], {
            "id": "ventas-1",
            "display_name": "Ventas",
            "channels": {"telegram": True, "email": False},
            "available": True,
        })
        self.assertEqual(payload[1]["channels"], {"telegram": False, "email": True})
        self.assertTrue(payload[1]["available"])

    def test_marks_unavailable_contacts_explicitly(self) -> None:
        repository = StaticRepository(
            [Contact(id="ops-1", display_name="Operaciones", telegram_chat_id=None, email=None, email_enabled=False)]
        )

        payload = list_contacts_response(repository)

        self.assertEqual(payload, [{
            "id": "ops-1",
            "display_name": "Operaciones",
            "channels": {"telegram": False, "email": False},
            "available": False,
        }])


if __name__ == "__main__":
    unittest.main()
