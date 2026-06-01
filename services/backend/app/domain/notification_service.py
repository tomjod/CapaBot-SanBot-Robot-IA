from __future__ import annotations

from typing import Protocol

from backend.app.domain.contact import Contact
from backend.app.domain.message_builder import TemplateMessageBuilder
from backend.app.domain.notification import NotificationRequest
from backend.app.domain.providers import ChannelDelivery, EmailProvider, NotificationOutcome, TelegramProvider, WhatsappProvider


class ContactRepository(Protocol):
    def get_contact(self, contact_id: str) -> Contact | None:
        ...


class NotificationService:
    def __init__(
        self,
        contact_repository: ContactRepository,
        telegram_provider: TelegramProvider,
        message_builder: TemplateMessageBuilder,
        whatsapp_provider: WhatsappProvider | None = None,
        email_provider: EmailProvider | None = None,
    ) -> None:
        self._contact_repository = contact_repository
        self._telegram_provider = telegram_provider
        self._message_builder = message_builder
        self._email_provider = email_provider
        self._whatsapp_provider = whatsapp_provider

    def submit(self, request: NotificationRequest) -> NotificationOutcome:
        contact = self._contact_repository.get_contact(request.contact_id)
        if contact is None:
            return NotificationOutcome(
                status="unavailable",
                telegram=ChannelDelivery("unavailable"),
                email=ChannelDelivery("unavailable"),
                whatsapp=ChannelDelivery("unavailable"),
                retryable=False,
                detail="No encontramos el contacto solicitado.",
            )

        if not contact.available:
            return NotificationOutcome(
                status="unavailable",
                telegram=ChannelDelivery("unavailable"),
                email=ChannelDelivery("unavailable"),
                whatsapp=ChannelDelivery("unavailable"),
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

        if contact.normalized_phone and self._whatsapp_provider is not None:
            whatsapp_status = self._whatsapp_provider.send(contact, message, request)
        else:
            whatsapp_status = ChannelDelivery("skipped")

        return NotificationOutcome(
            status=self._resolve_business_status(contact, telegram_status, email_status, whatsapp_status),
            telegram=telegram_status,
            email=email_status,
            whatsapp=whatsapp_status,
            retryable=self._resolve_retryable(telegram_status, email_status, whatsapp_status),
            detail=self._build_detail(contact, telegram_status, email_status, whatsapp_status),
        )

    def _build_detail(
        self,
        contact: Contact,
        telegram_status: ChannelDelivery,
        email_status: ChannelDelivery,
        whatsapp_status: ChannelDelivery,
    ) -> str:
        business_status = self._resolve_business_status(
            contact, telegram_status, email_status, whatsapp_status
        )
        if business_status in {"accepted", "delivered_or_queued"}:
            return self._build_success_detail(contact, telegram_status, email_status, whatsapp_status)
        if business_status == "unavailable":
            return self._build_unavailable_detail(contact)
        return self._build_failure_detail(contact, telegram_status, email_status, whatsapp_status)

    @staticmethod
    def _resolve_business_status(
        contact: Contact,
        telegram_status: ChannelDelivery,
        email_status: ChannelDelivery,
        whatsapp_status: ChannelDelivery,
    ) -> str:
        statuses = (telegram_status, email_status, whatsapp_status)

        if any(channel.status == "sent" for channel in statuses):
            return "delivered_or_queued"

        if any(channel.status == "accepted" for channel in statuses):
            return "accepted"

        if not contact.available:
            return "unavailable"

        if all(channel.status in {"skipped", "unavailable"} for channel in statuses):
            return "unavailable"

        return "failed"

    @staticmethod
    def _resolve_retryable(
        telegram_status: ChannelDelivery,
        email_status: ChannelDelivery,
        whatsapp_status: ChannelDelivery,
    ) -> bool:
        return any(
            channel.status == "failed"
            for channel in (telegram_status, email_status, whatsapp_status)
        )

    @staticmethod
    def _join_channels(labels: list[str], conjunction: str) -> str:
        if len(labels) == 1:
            return labels[0]
        return f"{', '.join(labels[:-1])} {conjunction} {labels[-1]}"

    @classmethod
    def _build_success_detail(
        cls,
        contact: Contact,
        telegram_status: ChannelDelivery,
        email_status: ChannelDelivery,
        whatsapp_status: ChannelDelivery,
    ) -> str:
        channels = (
            ("Telegram", telegram_status),
            ("email", email_status),
            ("WhatsApp", whatsapp_status),
        )
        delivered = [label for label, channel in channels if channel.status in {"accepted", "sent"}]
        if delivered:
            return f"Listo, avisamos a {contact.display_name} por {cls._join_channels(delivered, 'y')}."
        return f"La notificación para {contact.display_name} fue aceptada."

    @staticmethod
    def _build_unavailable_detail(contact: Contact) -> str:
        return f"{contact.display_name} no tiene Telegram disponible y el email no está habilitado."

    @classmethod
    def _build_failure_detail(
        cls,
        contact: Contact,
        telegram_status: ChannelDelivery,
        email_status: ChannelDelivery,
        whatsapp_status: ChannelDelivery,
    ) -> str:
        channels = (
            ("Telegram", telegram_status),
            ("email", email_status),
            ("WhatsApp", whatsapp_status),
        )
        failed = [label for label, channel in channels if channel.status == "failed"]
        if failed:
            return f"No pudimos avisar a {contact.display_name} por {cls._join_channels(failed, 'ni')}."
        return f"No pudimos completar la notificación para {contact.display_name}."
