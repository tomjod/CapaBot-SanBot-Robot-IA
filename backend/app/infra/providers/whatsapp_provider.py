from __future__ import annotations

import httpx
from dataclasses import dataclass
from typing import Any, Callable, Mapping, Protocol

from backend.app.domain.contact import Contact
from backend.app.domain.notification import NotificationRequest
from backend.app.domain.providers import ChannelDelivery, ChannelStatus


class SupportsWhatsAppClient(Protocol):
    def post(self, endpoint: str, json: Mapping[str, object]) -> Mapping[str, object]:
        ...


ClientFactory = Callable[[str, float], SupportsWhatsAppClient]


def _default_client_factory(base_url: str, timeout: float) -> SupportsWhatsAppClient:
    return _HttpxWhatsAppClient(base_url, timeout)


class _HttpxWhatsAppClient:
    def __init__(self, base_url: str, timeout: float) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout

    def post(self, endpoint: str, json: Mapping[str, object]) -> Mapping[str, object]:
        url = f"{self._base_url}/{endpoint.lstrip('/')}"
        with httpx.Client(timeout=self._timeout) as client:
            response = client.post(url, json=dict(json))
            response.raise_for_status()
            return response.json()


class WhatsAppBridgeProvider:
    """Provider que usa el whatsapp-bridge (Baileys) para enviar mensajes."""

    def __init__(
        self,
        bridge_base_url: str,
        timeout: float = 10.0,
        client_factory: ClientFactory | None = None,
    ) -> None:
        self._bridge_base_url = bridge_base_url.rstrip("/")
        self._timeout = timeout
        self._client_factory = client_factory or _default_client_factory

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        if not contact.normalized_phone:
            return ChannelDelivery(status="unavailable")

        client = self._client_factory(self._bridge_base_url, self._timeout)

        try:
            response = client.post(
                "/messages/send",
                {
                    "phone": contact.normalized_phone,
                    "message": message,
                },
            )
        except httpx.TimeoutException:
            return ChannelDelivery(status="failed", detail="WhatsApp bridge timeout")
        except Exception as error:
            return ChannelDelivery(status="failed", detail=str(error))

        if response.get("ok") is not True:
            detail = response.get("error") or response.get("message")
            return ChannelDelivery(status="failed", detail=str(detail) if detail else None)

        return ChannelDelivery(status="sent")


class StubWhatsAppProvider:
    def __init__(self, status: ChannelStatus = "accepted") -> None:
        self._status = status
        self.sent_messages: list[tuple[str, str, str]] = []

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        self.sent_messages.append((contact.id, request.device_id, message))
        return ChannelDelivery(status=self._status)


FakeWhatsAppProvider = StubWhatsAppProvider
