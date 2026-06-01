from __future__ import annotations

import unittest
from datetime import datetime, timezone
from typing import Mapping

import httpx

from backend.app.domain.contact import Contact
from backend.app.domain.notification import NotificationRequest
from backend.app.infra.providers.whatsapp_provider import (
    StubWhatsAppProvider,
    WhatsAppBridgeProvider,
)


def _contact(phone: str | None = "+54 9 11 2233-4455") -> Contact:
    return Contact(
        id="mv-1",
        display_name="Recepción",
        company="mundos_virtuales",
        phone=phone,
    )


def _request() -> NotificationRequest:
    return NotificationRequest(
        contact_id="mv-1",
        device_id="sanbot-01",
        requested_at=datetime(2026, 6, 1, 12, 0, tzinfo=timezone.utc),
    )


class _FakeClient:
    def __init__(self, response: Mapping[str, object]) -> None:
        self._response = response
        self.calls: list[tuple[str, Mapping[str, object]]] = []

    def post(self, endpoint: str, json: Mapping[str, object]) -> Mapping[str, object]:
        self.calls.append((endpoint, json))
        return self._response


class WhatsAppBridgeProviderTest(unittest.TestCase):
    def test_posts_to_messages_text_with_phone_and_text(self) -> None:
        fake = _FakeClient({"ok": True, "jid": "x@s.whatsapp.net", "messageId": "m1"})
        captured: dict[str, object] = {}

        def factory(base_url: str, timeout: float, api_key: str | None):
            captured["base_url"] = base_url
            captured["timeout"] = timeout
            captured["api_key"] = api_key
            return fake

        provider = WhatsAppBridgeProvider(
            bridge_base_url="http://bridge:3001",
            internal_api_key="secret-key",
            timeout=7.5,
            client_factory=factory,
        )

        delivery = provider.send(_contact(), "Hola", _request())

        self.assertEqual(delivery.status, "sent")
        self.assertEqual(captured["base_url"], "http://bridge:3001")
        self.assertEqual(captured["timeout"], 7.5)
        self.assertEqual(captured["api_key"], "secret-key")
        self.assertEqual(len(fake.calls), 1)
        endpoint, body = fake.calls[0]
        self.assertEqual(endpoint, "/messages/text")
        self.assertEqual(body, {"phone": "5491122334455", "text": "Hola"})

    def test_unavailable_when_no_phone(self) -> None:
        provider = WhatsAppBridgeProvider(bridge_base_url="http://bridge:3001")

        delivery = provider.send(_contact(phone=None), "Hola", _request())

        self.assertEqual(delivery.status, "unavailable")

    def test_failed_when_bridge_returns_not_ok(self) -> None:
        fake = _FakeClient({"ok": False, "message": "WhatsApp is not connected yet"})
        provider = WhatsAppBridgeProvider(
            bridge_base_url="http://bridge:3001",
            client_factory=lambda *_: fake,
        )

        delivery = provider.send(_contact(), "Hola", _request())

        self.assertEqual(delivery.status, "failed")
        self.assertEqual(delivery.detail, "WhatsApp is not connected yet")

    def test_failed_on_timeout(self) -> None:
        def factory(*_):
            class _Boom:
                def post(self, *_args, **_kwargs):
                    raise httpx.TimeoutException("timeout")

            return _Boom()

        provider = WhatsAppBridgeProvider(
            bridge_base_url="http://bridge:3001",
            client_factory=factory,
        )

        delivery = provider.send(_contact(), "Hola", _request())

        self.assertEqual(delivery.status, "failed")
        self.assertIn("timeout", (delivery.detail or "").lower())

    def test_default_httpx_client_sends_api_key_header_to_messages_text(self) -> None:
        seen: dict[str, object] = {}

        def handler(request: httpx.Request) -> httpx.Response:
            seen["url"] = str(request.url)
            seen["x-api-key"] = request.headers.get("x-api-key")
            return httpx.Response(200, json={"ok": True})

        from backend.app.infra.providers import whatsapp_provider as wp

        transport = httpx.MockTransport(handler)
        original = wp.httpx.Client

        def patched_client(*args, **kwargs):
            kwargs["transport"] = transport
            return original(*args, **kwargs)

        wp.httpx.Client = patched_client  # type: ignore[attr-defined]
        try:
            provider = WhatsAppBridgeProvider(
                bridge_base_url="http://bridge:3001",
                internal_api_key="secret-key",
            )
            delivery = provider.send(_contact(), "Hola", _request())
        finally:
            wp.httpx.Client = original  # type: ignore[attr-defined]

        self.assertEqual(delivery.status, "sent")
        self.assertEqual(seen["url"], "http://bridge:3001/messages/text")
        self.assertEqual(seen["x-api-key"], "secret-key")


class StubWhatsAppProviderTest(unittest.TestCase):
    def test_records_and_returns_configured_status(self) -> None:
        provider = StubWhatsAppProvider(status="accepted")

        delivery = provider.send(_contact(), "Hola", _request())

        self.assertEqual(delivery.status, "accepted")
        self.assertEqual(len(provider.sent_messages), 1)


if __name__ == "__main__":
    unittest.main()
