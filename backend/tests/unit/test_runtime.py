from __future__ import annotations

import os
import unittest

from backend.app.runtime import BackendSettings, build_runtime
from backend.app.infra.providers.email_provider import SmtpEmailProvider, StubEmailProvider
from backend.app.infra.providers.telegram_provider import StubTelegramProvider, TelegramBotProvider


class RuntimeWiringTest(unittest.TestCase):
    def test_uses_stub_providers_when_credentials_are_missing(self) -> None:
        runtime = build_runtime(BackendSettings.from_env({}))

        self.assertIsInstance(runtime.telegram_provider, StubTelegramProvider)
        self.assertIsInstance(runtime.email_provider, StubEmailProvider)
        self.assertFalse(runtime.settings.allow_stub_delivery)

    def test_uses_real_providers_when_credentials_are_present(self) -> None:
        env = {
            "VISITOR_NOTIFY_TELEGRAM_BOT_TOKEN": "token-123",
            "VISITOR_NOTIFY_TELEGRAM_API_BASE_URL": "https://telegram.internal",
            "VISITOR_NOTIFY_TELEGRAM_TIMEOUT_SECONDS": "15",
            "VISITOR_NOTIFY_EMAIL_SMTP_HOST": "smtp.example.com",
            "VISITOR_NOTIFY_EMAIL_FROM": "robot@example.com",
            "VISITOR_NOTIFY_EMAIL_TIMEOUT_SECONDS": "7.5",
        }

        runtime = build_runtime(BackendSettings.from_env(env))

        self.assertIsInstance(runtime.telegram_provider, TelegramBotProvider)
        self.assertIsInstance(runtime.email_provider, SmtpEmailProvider)
        self.assertEqual(runtime.settings.telegram_api_base_url, "https://telegram.internal")
        self.assertEqual(runtime.settings.telegram_timeout_seconds, 15.0)
        self.assertEqual(runtime.settings.email_timeout_seconds, 7.5)

    def test_reads_boolean_flags_from_environment(self) -> None:
        settings = BackendSettings.from_env({"VISITOR_NOTIFY_ALLOW_STUB_DELIVERY": "false"})

        self.assertFalse(settings.allow_stub_delivery)

    def test_allows_explicit_stub_delivery_for_local_seams(self) -> None:
        settings = BackendSettings.from_env({"VISITOR_NOTIFY_ALLOW_STUB_DELIVERY": "true"})

        runtime = build_runtime(settings)

        self.assertTrue(runtime.settings.allow_stub_delivery)
        self.assertIsInstance(runtime.telegram_provider, StubTelegramProvider)
        self.assertIsInstance(runtime.email_provider, StubEmailProvider)


if __name__ == "__main__":
    unittest.main()
