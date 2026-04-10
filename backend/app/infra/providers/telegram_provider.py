from __future__ import annotations

import json
from typing import Callable
from urllib import request as urllib_request

from backend.app.domain.contact import Contact
from backend.app.domain.notification import NotificationRequest
from backend.app.domain.providers import ChannelDelivery, ChannelStatus


Transport = Callable[[str, bytes, dict[str, str]], int]


def _default_transport(url: str, data: bytes, headers: dict[str, str]) -> int:
    http_request = urllib_request.Request(url=url, data=data, headers=headers, method="POST")
    timeout = float(headers.pop("X-Timeout-Seconds", "10"))
    with urllib_request.urlopen(http_request, timeout=timeout) as response:
        return response.getcode()


class TelegramBotProvider:
    def __init__(
        self,
        bot_token: str,
        transport: Transport | None = None,
        api_base_url: str = "https://api.telegram.org",
        timeout_seconds: float = 10.0,
    ) -> None:
        self._bot_token = bot_token.strip()
        self._transport = transport or _default_transport
        self._api_base_url = api_base_url.rstrip("/")
        self._timeout_seconds = timeout_seconds

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        if not contact.telegram_chat_id:
            return ChannelDelivery(status="unavailable")

        payload = json.dumps({"chat_id": contact.telegram_chat_id, "text": message}).encode("utf-8")
        status_code = self._transport(
            f"{self._api_base_url}/bot{self._bot_token}/sendMessage",
            payload,
            {"Content-Type": "application/json", "X-Timeout-Seconds": str(self._timeout_seconds)},
        )
        if 200 <= status_code < 300:
            return ChannelDelivery(status="sent")
        return ChannelDelivery(status="failed")


class StubTelegramProvider:
    def __init__(self, status: ChannelStatus = "accepted") -> None:
        self._status = status
        self.sent_messages: list[tuple[str, str, str]] = []

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        self.sent_messages.append((contact.id, request.device_id, message))
        return ChannelDelivery(status=self._status)


FakeTelegramProvider = StubTelegramProvider
