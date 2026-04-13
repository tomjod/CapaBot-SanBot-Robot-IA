from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Mapping

from backend.app.domain.message_builder import TemplateMessageBuilder
from backend.app.domain.notification_service import NotificationService
from backend.app.infra.json_contacts import JsonContactRepository
from backend.app.infra.providers.email_provider import MailtrapEmailProvider, SmtpEmailProvider, StubEmailProvider
from backend.app.infra.providers.telegram_provider import StubTelegramProvider, TelegramBotProvider


def _parse_bool(value: str | None, default: bool) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class BackendSettings:
    contacts_path: Path
    telegram_bot_token: str | None
    telegram_api_base_url: str
    telegram_timeout_seconds: float
    email_mailtrap_token: str | None
    email_smtp_host: str | None
    email_from: str | None
    email_from_name: str | None
    email_smtp_port: int
    email_timeout_seconds: float
    allow_stub_delivery: bool

    @classmethod
    def from_env(cls, env: Mapping[str, str] | None = None) -> "BackendSettings":
        values = env or os.environ
        default_contacts_path = Path(__file__).resolve().parent / "data" / "contacts.json"
        raw_contacts_path = values.get("VISITOR_NOTIFY_CONTACTS_PATH")
        return cls(
            contacts_path=Path(raw_contacts_path) if raw_contacts_path else default_contacts_path,
            telegram_bot_token=values.get("VISITOR_NOTIFY_TELEGRAM_BOT_TOKEN") or None,
            telegram_api_base_url=values.get("VISITOR_NOTIFY_TELEGRAM_API_BASE_URL", "https://api.telegram.org"),
            telegram_timeout_seconds=float(values.get("VISITOR_NOTIFY_TELEGRAM_TIMEOUT_SECONDS", "10")),
            email_mailtrap_token=values.get("VISITOR_NOTIFY_EMAIL_MAILTRAP_TOKEN") or None,
            email_smtp_host=values.get("VISITOR_NOTIFY_EMAIL_SMTP_HOST") or None,
            email_from=values.get("VISITOR_NOTIFY_EMAIL_FROM") or None,
            email_from_name=values.get("VISITOR_NOTIFY_EMAIL_FROM_NAME") or None,
            email_smtp_port=int(values.get("VISITOR_NOTIFY_EMAIL_SMTP_PORT", "25")),
            email_timeout_seconds=float(values.get("VISITOR_NOTIFY_EMAIL_TIMEOUT_SECONDS", "10")),
            allow_stub_delivery=_parse_bool(values.get("VISITOR_NOTIFY_ALLOW_STUB_DELIVERY"), False),
        )


@dataclass(frozen=True)
class BackendRuntime:
    settings: BackendSettings
    repository: JsonContactRepository
    notification_service: NotificationService
    telegram_provider: object
    email_provider: object


def build_runtime(settings: BackendSettings | None = None) -> BackendRuntime:
    resolved_settings = settings or BackendSettings.from_env()
    repository = JsonContactRepository(resolved_settings.contacts_path)
    telegram_provider = _build_telegram_provider(resolved_settings)
    email_provider = _build_email_provider(resolved_settings)
    notification_service = NotificationService(
        contact_repository=repository,
        telegram_provider=telegram_provider,
        email_provider=email_provider,
        message_builder=TemplateMessageBuilder(),
    )
    return BackendRuntime(
        settings=resolved_settings,
        repository=repository,
        notification_service=notification_service,
        telegram_provider=telegram_provider,
        email_provider=email_provider,
    )


def _build_telegram_provider(settings: BackendSettings):
    if settings.telegram_bot_token:
        return TelegramBotProvider(
            bot_token=settings.telegram_bot_token,
            api_base_url=settings.telegram_api_base_url,
            timeout_seconds=settings.telegram_timeout_seconds,
        )
    if settings.allow_stub_delivery:
        return StubTelegramProvider(status="accepted")
    return StubTelegramProvider(status="unavailable")


def _build_email_provider(settings: BackendSettings):
    if settings.email_mailtrap_token and settings.email_from:
        return MailtrapEmailProvider(
            token=settings.email_mailtrap_token,
            from_address=settings.email_from,
            from_name=settings.email_from_name,
        )
    if settings.email_smtp_host and settings.email_from:
        return SmtpEmailProvider(
            host=settings.email_smtp_host,
            from_address=settings.email_from,
            port=settings.email_smtp_port,
            timeout=settings.email_timeout_seconds,
        )
    if settings.allow_stub_delivery:
        return StubEmailProvider(status="accepted")
    return StubEmailProvider(status="skipped")
