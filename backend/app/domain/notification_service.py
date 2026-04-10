from __future__ import annotations

from typing import Protocol

from backend.app.domain.contact import Contact
from backend.app.domain.message_builder import TemplateMessageBuilder
from backend.app.domain.notification import NotificationRequest
from backend.app.domain.providers import ChannelDelivery, EmailProvider, NotificationOutcome, TelegramProvider


class ContactRepository(Protocol):
    def get_contact(self, contact_id: str) -> Contact | None:
        ...


class NotificationService:
    def __init__(
        self,
        contact_repository: ContactRepository,
        telegram_provider: TelegramProvider,
        message_builder: TemplateMessageBuilder,
        email_provider: EmailProvider | None = None,
    ) -> None:
        self._contact_repository = contact_repository
        self._telegram_provider = telegram_provider
        self._message_builder = message_builder
        self._email_provider = email_provider

    def submit(self, request: NotificationRequest) -> NotificationOutcome:
        contact = self._contact_repository.get_contact(request.contact_id)
        if contact is None:
            return NotificationOutcome(
                status="unavailable",
                telegram=ChannelDelivery("unavailable"),
                email=ChannelDelivery("unavailable"),
                retryable=False,
                detail="No encontramos el contacto solicitado.",
            )

        if not contact.available:
            return NotificationOutcome(
                status="unavailable",
                telegram=ChannelDelivery("unavailable"),
                email=ChannelDelivery("unavailable"),
                retryable=False,
                detail=self._build_unavailable_detail(contact),
            )

        message = self._message_builder.build(contact, request)

        telegram_status = ChannelDelivery("unavailable")
        if contact.telegram_available:
            telegram_status = self._telegram_provider.send(contact, message, request)

        if contact.email_available and self._email_provider is not None:
            email_status = self._email_provider.send(contact, message, request)
        elif contact.email_available:
            email_status = ChannelDelivery("skipped")
        else:
            email_status = ChannelDelivery("skipped")

        return NotificationOutcome(
            status=self._resolve_business_status(contact, telegram_status, email_status),
            telegram=telegram_status,
            email=email_status,
            retryable=self._resolve_retryable(telegram_status, email_status),
            detail=self._build_detail(contact, telegram_status, email_status),
        )

    def _build_detail(
        self,
        contact: Contact,
        telegram_status: ChannelDelivery,
        email_status: ChannelDelivery,
    ) -> str:
        business_status = self._resolve_business_status(contact, telegram_status, email_status)
        if business_status in {"accepted", "delivered_or_queued"}:
            return self._build_success_detail(contact, telegram_status, email_status)
        if business_status == "unavailable":
            return self._build_unavailable_detail(contact)
        return self._build_failure_detail(contact, telegram_status, email_status)

    @staticmethod
    def _resolve_business_status(
        contact: Contact,
        telegram_status: ChannelDelivery,
        email_status: ChannelDelivery,
    ) -> str:
        if telegram_status.status == "sent" or email_status.status == "sent":
            return "delivered_or_queued"

        if telegram_status.status == "accepted" or email_status.status == "accepted":
            return "accepted"

        if not contact.available:
            return "unavailable"

        if telegram_status.status == "unavailable" and email_status.status in {"skipped", "unavailable"}:
            return "unavailable"

        return "failed"

    @staticmethod
    def _resolve_retryable(telegram_status: ChannelDelivery, email_status: ChannelDelivery) -> bool:
        return telegram_status.status == "failed" or email_status.status == "failed"

    @staticmethod
    def _build_success_detail(
        contact: Contact,
        telegram_status: ChannelDelivery,
        email_status: ChannelDelivery,
    ) -> str:
        telegram_ok = telegram_status.status in {"accepted", "sent"}
        email_ok = email_status.status in {"accepted", "sent"}
        if telegram_ok and email_ok:
            return f"Listo, avisamos a {contact.display_name} por Telegram y email."
        if telegram_ok:
            return f"Listo, avisamos a {contact.display_name} por Telegram."
        if email_ok:
            return f"Listo, avisamos a {contact.display_name} por email."
        return f"La notificación para {contact.display_name} fue aceptada."

    @staticmethod
    def _build_unavailable_detail(contact: Contact) -> str:
        return f"{contact.display_name} no tiene Telegram disponible y el email no está habilitado."

    @staticmethod
    def _build_failure_detail(
        contact: Contact,
        telegram_status: ChannelDelivery,
        email_status: ChannelDelivery,
    ) -> str:
        telegram_failed = telegram_status.status == "failed"
        email_failed = email_status.status == "failed"
        if telegram_failed and email_failed:
            return f"No pudimos avisar a {contact.display_name} por Telegram ni email."
        if telegram_failed:
            return f"No pudimos avisar a {contact.display_name} por Telegram."
        if email_failed:
            return f"No pudimos avisar a {contact.display_name} por email."
        return f"No pudimos completar la notificación para {contact.display_name}."
