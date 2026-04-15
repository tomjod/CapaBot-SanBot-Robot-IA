from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from backend.app.bootstrap.settings import BackendSettings
from backend.app.domain.message_builder import TemplateMessageBuilder
from backend.app.domain.notification_service import NotificationService
from backend.app.domain.telegram_onboarding import TelegramOnboardingService
from backend.app.infrastructure.json.contacts_repository import JsonContactRepository
from backend.app.infrastructure.json.pending_registrations_repository import JsonPendingTelegramRegistrationRepository


@dataclass(frozen=True)
class BackendRuntime:
    settings: BackendSettings
    repository: JsonContactRepository
    pending_registration_repository: JsonPendingTelegramRegistrationRepository
    notification_service: NotificationService
    telegram_onboarding_service: TelegramOnboardingService
    telegram_provider: Any
    email_provider: Any


def build_runtime(settings: BackendSettings | None = None) -> BackendRuntime:
    resolved_settings = settings or BackendSettings.from_env()
    repository = JsonContactRepository(resolved_settings.contacts_path)
    pending_registration_repository = JsonPendingTelegramRegistrationRepository(
        resolved_settings.pending_registrations_path
    )
    telegram_provider = _build_telegram_provider(resolved_settings)
    email_provider = _build_email_provider(resolved_settings)
    notification_service = NotificationService(
        contact_repository=repository,
        telegram_provider=telegram_provider,
        email_provider=email_provider,
        message_builder=TemplateMessageBuilder(),
    )
    telegram_onboarding_service = TelegramOnboardingService(
        repository=repository,
        pending_registration_repository=pending_registration_repository,
    )
    return BackendRuntime(
        settings=resolved_settings,
        repository=repository,
        pending_registration_repository=pending_registration_repository,
        notification_service=notification_service,
        telegram_onboarding_service=telegram_onboarding_service,
        telegram_provider=telegram_provider,
        email_provider=email_provider,
    )


def _build_telegram_provider(settings: BackendSettings):
    from backend.app.infrastructure.providers.telegram_provider import StubTelegramProvider, TelegramBotProvider

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
    from backend.app.infrastructure.providers.email_provider import (
        MailtrapEmailProvider,
        SmtpEmailProvider,
        StubEmailProvider,
    )

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
