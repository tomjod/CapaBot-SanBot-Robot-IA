from __future__ import annotations

import unittest

from backend.app.domain.contact import Contact


class ContactTest(unittest.TestCase):
    def test_defaults_manual_availability_to_enabled_for_legacy_payloads(self) -> None:
        contact = Contact.from_dict(
            {
                "id": "ventas-1",
                "display_name": "Ventas",
                "job_title": "Ejecutiva comercial",
                "company": "transformapp",
                "phone": "+56 9 1234 5678",
                "telegram_chat_id": "chat-1",
            }
        )

        self.assertTrue(contact.enabled)
        self.assertTrue(contact.available)

    def test_enabled_contact_with_operational_channel_is_available(self) -> None:
        contact = Contact(
            id="ventas-1",
            display_name="Ventas",
            company="transformapp",
            enabled=True,
            telegram_chat_id="chat-1",
        )

        self.assertTrue(contact.available)

    def test_disabled_contact_stays_unavailable_even_when_channels_exist(self) -> None:
        contact = Contact(
            id="ventas-1",
            display_name="Ventas",
            company="transformapp",
            enabled=False,
            telegram_chat_id="chat-1",
            email="ventas@example.com",
            email_enabled=True,
        )

        self.assertFalse(contact.available)


if __name__ == "__main__":
    unittest.main()
