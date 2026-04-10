from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from backend.app.infra.json_contacts import JsonContactRepository


class JsonContactRepositoryTest(unittest.TestCase):
    def _write_contacts(self, payload: list[dict[str, object]]) -> Path:
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        file_path = Path(temp_dir.name) / "contacts.json"
        file_path.write_text(json.dumps(payload), encoding="utf-8")
        return file_path

    def test_loads_contacts_with_stable_ids_and_channel_availability(self) -> None:
        repository = JsonContactRepository(
            self._write_contacts(
                [
                    {
                        "id": "ventas-1",
                        "display_name": "Ventas",
                        "telegram_chat_id": "12345",
                        "email": "ventas@example.com",
                        "email_enabled": True,
                    },
                    {
                        "id": "rrhh-1",
                        "display_name": "RRHH",
                        "telegram_chat_id": None,
                        "email": "rrhh@example.com",
                        "email_enabled": True,
                    },
                ]
            )
        )

        contacts = repository.list_contacts()

        self.assertEqual([contact.id for contact in contacts], ["ventas-1", "rrhh-1"])
        self.assertEqual(contacts[0].to_summary()["channels"], {"telegram": True, "email": True})
        self.assertEqual(contacts[1].to_summary()["channels"], {"telegram": False, "email": True})
        self.assertTrue(contacts[1].available)

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
