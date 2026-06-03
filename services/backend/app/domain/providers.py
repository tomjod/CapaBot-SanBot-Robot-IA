from __future__ import annotations

from dataclasses import dataclass
from typing import Literal, Protocol

from backend.app.domain.contact import Contact
from backend.app.domain.notification import NotificationRequest

ChannelStatus = Literal["accepted", "sent", "unavailable", "failed", "skipped"]
BusinessStatus = Literal["accepted", "delivered_or_queued", "unavailable", "failed"]


@dataclass(frozen=True)
class ChannelDelivery:
    status: ChannelStatus
    detail: str | None = None

    @property
    def successful(self) -> bool:
        return self.status in {"accepted", "sent"}


@dataclass(frozen=True)
class NotificationOutcome:
    status: BusinessStatus
    telegram: ChannelDelivery
    email: ChannelDelivery
    whatsapp: ChannelDelivery
    retryable: bool
    detail: str | None = None

    def to_dict(self) -> dict[str, object]:
        payload: dict[str, object] = {
            "status": self.status,
            "channels": {
                "telegram": self.telegram.status,
                "email": self.email.status,
                "whatsapp": self.whatsapp.status,
            },
            "retryable": self.retryable,
        }
        if self.detail:
            payload["detail"] = self.detail
        return payload


class TelegramProvider(Protocol):
    """.. deprecated::

        Telegram channel is disabled by default as of 2026-06.
        Set ``VISITOR_NOTIFY_ENABLE_TELEGRAM=true`` to re-enable.
        Prefer :class:`WhatsappProvider` as the primary notification channel.
    """

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        ...


class EmailProvider(Protocol):
    """.. deprecated::

        Email channel is disabled by default as of 2026-06.
        Set ``VISITOR_NOTIFY_ENABLE_EMAIL=true`` to re-enable.
        Prefer :class:`WhatsappProvider` as the primary notification channel.
    """

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        ...


class WhatsappProvider(Protocol):
    """Primary notification channel. Always enabled."""

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        ...