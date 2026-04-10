from __future__ import annotations

import json
import unittest
from datetime import datetime, timezone

from backend.app.domain.contact import Contact
from backend.app.domain.notification import NotificationRequest
from backend.app.infra.providers.telegram_provider import StubTelegramProvider, TelegramBotProvider


class RecordingTransport:
    def __init__(self, status_code: int = 200) -> None:
        self.status_code = status_code
        self.requests: list[tuple[str, bytes, dict[str, str]]] = []

    def __call__(self, url: str, data: bytes, headers: dict[str, str]) -> int:
        self.requests.append((url, data, headers))
        return self.status_code


class TelegramProviderTest(unittest.TestCase):
    def _contact(self) -> Contact:
        return Contact(id="ventas-1", display_name="Ventas", telegram_chat_id="-100123")

    def _request(self) -> NotificationRequest:
        return NotificationRequest(
            contact_id="ventas-1",
            device_id="sanbot-01",
            requested_at=datetime(2026, 4, 9, 18, 0, tzinfo=timezone.utc),
        )

    def test_posts_message_to_telegram_bot_api(self) -> None:
        transport = RecordingTransport(status_code=200)
        provider = TelegramBotProvider(
            bot_token="token-123",
            transport=transport,
            api_base_url="https://telegram.internal",
            timeout_seconds=15,
        )

        outcome = provider.send(self._contact(), "hola", self._request())

        self.assertEqual(outcome.status, "sent")
        self.assertEqual(len(transport.requests), 1)
        url, raw_body, headers = transport.requests[0]
        self.assertEqual(url, "https://telegram.internal/bottoken-123/sendMessage")
        self.assertEqual(headers["Content-Type"], "application/json")
        self.assertEqual(headers["X-Timeout-Seconds"], "15")
        self.assertEqual(json.loads(raw_body.decode("utf-8")), {"chat_id": "-100123", "text": "hola"})

    def test_stub_provider_keeps_android_contract_executable_without_credentials(self) -> None:
        provider = StubTelegramProvider()

        outcome = provider.send(self._contact(), "hola", self._request())

        self.assertEqual(outcome.status, "accepted")


if __name__ == "__main__":
    unittest.main()
