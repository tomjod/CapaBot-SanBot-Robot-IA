from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from backend.app.infra.json_contacts import JsonContactRepository
from backend.app.infrastructure.json.contacts_repository import (
    JsonContactRepository as InfrastructureJsonContactRepository,
)


class JsonContactRepositoryTest(unittest.TestCase):
    def test_legacy_module_bridges_to_infrastructure_repository(self) -> None:
        self.assertIs(JsonContactRepository, InfrastructureJsonContactRepository)

    def _write_contacts(self, payload: list[dict[str, object]]) -> Path:
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        file_path = Path(temp_dir.name) / "contacts.json"
        file_path.write_text(json.dumps(payload), encoding="utf-8")
        return file_path

    def test_loads_contacts_with_stable_ids_and_backward_compatible_manual_availability(self) -> None:
        repository = JsonContactRepository(
            self._write_contacts(
                [
                    {
                        "id": "ventas-1",
                        "display_name": "Ventas",
                        "job_title": "Ejecutiva comercial",
                        "company": "transformapp",
                        "phone": "+56 9 1234 5678",
                        "telegram_chat_id": "12345",
                        "telegram_user_id": "777",
                        "email": "ventas@example.com",
                        "email_enabled": True,
                    },
                    {
                        "id": "rrhh-1",
                        "display_name": "RRHH",
                        "job_title": "People partner",
                        "company": "tps",
                        "phone": "+56 9 1234 5679",
                        "telegram_chat_id": None,
                        "email": "rrhh@example.com",
                        "email_enabled": True,
                    },
                ]
            )
        )

        contacts = repository.list_contacts()

        self.assertEqual([contact.id for contact in contacts], ["ventas-1", "rrhh-1"])
        self.assertEqual(contacts[0].company_label, "Transformapp")
        self.assertEqual(contacts[0].normalized_phone, "56912345678")
        self.assertEqual(contacts[0].to_summary()["channels"], {"telegram": True, "email": True})
        self.assertEqual(contacts[1].to_summary()["channels"], {"telegram": False, "email": True})
        self.assertTrue(contacts[0].enabled)
        self.assertTrue(contacts[1].enabled)
        self.assertTrue(contacts[1].available)

    def test_manual_disable_keeps_contact_unavailable_even_with_channels(self) -> None:
        repository = JsonContactRepository(
            self._write_contacts(
                [
                    {
                        "id": "ventas-1",
                        "display_name": "Ventas",
                        "job_title": "Ejecutiva comercial",
                        "company": "transformapp",
                        "enabled": False,
                        "phone": "+56 9 1234 5678",
                        "telegram_chat_id": "12345",
                        "telegram_user_id": "777",
                        "email": "ventas@example.com",
                        "email_enabled": True,
                    }
                ]
            )
        )

        contact = repository.list_contacts()[0]

        self.assertTrue(contact.telegram_available)
        self.assertTrue(contact.email_available)
        self.assertFalse(contact.available)

    def test_finds_and_binds_contact_by_normalized_phone(self) -> None:
        repository = JsonContactRepository(
            self._write_contacts(
                [
                    {
                        "id": "ventas-1",
                        "display_name": "Ventas",
                        "job_title": "Ejecutiva comercial",
                        "company": "transformapp",
                        "phone": "+56 9 1234 5678",
                        "telegram_chat_id": None,
                        "email": "ventas@example.com",
                        "email_enabled": True,
                    }
                ]
            )
        )

        matched = repository.find_contact_by_phone("56912345678")
        self.assertIsNotNone(matched)
        assert matched is not None

        updated = repository.bind_contact_telegram(
            contact_id=matched.id,
            telegram_chat_id="999",
            telegram_user_id="555",
            telegram_username="ventas_bot",
        )

        self.assertEqual(updated.telegram_chat_id, "999")
        self.assertEqual(updated.telegram_user_id, "555")
        self.assertEqual(updated.telegram_username, "ventas_bot")

    def test_rejects_invalid_contact_payloads(self) -> None:
        repository = JsonContactRepository(
            self._write_contacts(
                [
                    {
                        "id": "demo-1",
                        "display_name": "   ",
                        "telegram_chat_id": "12345",
                    }
                ]
            )
        )

        with self.assertRaisesRegex(ValueError, "display_name must be a non-empty string"):
            repository.list_contacts()


if __name__ == "__main__":
    unittest.main()
