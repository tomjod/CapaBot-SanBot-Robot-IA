from __future__ import annotations

import unittest
from datetime import datetime, timezone

from backend.app.domain.contact import Contact
from backend.app.domain.notification import NotificationRequest
from backend.app.infra.providers.email_provider import SmtpEmailProvider, StubEmailProvider


class RecordingSmtpClient:
    def __init__(self) -> None:
        self.sent_messages: list[tuple[str, list[str], str]] = []
        self.closed = False

    def sendmail(self, from_address: str, to_addresses: list[str], message: str) -> None:
        self.sent_messages.append((from_address, to_addresses, message))

    def quit(self) -> None:
        self.closed = True


class EmailProviderTest(unittest.TestCase):
    def _contact(self) -> Contact:
        return Contact(
            id="ventas-1",
            display_name="Ventas",
            telegram_chat_id="-100123",
            email="ventas@example.com",
            email_enabled=True,
        )

    def _request(self) -> NotificationRequest:
        return NotificationRequest(
            contact_id="ventas-1",
            device_id="sanbot-01",
            requested_at=datetime(2026, 4, 9, 18, 0, tzinfo=timezone.utc),
        )

    def test_sends_email_with_injected_smtp_client(self) -> None:
        client = RecordingSmtpClient()
        provider = SmtpEmailProvider(
            host="smtp.example.com",
            from_address="robot@example.com",
            client_factory=lambda host, port, timeout: client,
        )

        outcome = provider.send(self._contact(), "hola", self._request())

        self.assertEqual(outcome.status, "sent")
        self.assertEqual(client.sent_messages[0][0], "robot@example.com")
        self.assertEqual(client.sent_messages[0][1], ["ventas@example.com"])
        self.assertIn("de visita", client.sent_messages[0][2])
        self.assertTrue(client.closed)

    def test_stub_provider_keeps_optional_email_contract_available(self) -> None:
        provider = StubEmailProvider()

        outcome = provider.send(self._contact(), "hola", self._request())

        self.assertEqual(outcome.status, "accepted")


if __name__ == "__main__":
    unittest.main()
