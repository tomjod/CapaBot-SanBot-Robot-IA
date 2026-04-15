from __future__ import annotations

import os
import tempfile
import unittest
from pathlib import Path

from backend.app.bootstrap.settings import BackendSettings
from backend.app.infra.providers.email_provider import MailtrapEmailProvider, SmtpEmailProvider, StubEmailProvider
from backend.app.infra.json_pending_registrations import JsonPendingTelegramRegistrationRepository
from backend.app.infra.providers.telegram_provider import StubTelegramProvider, TelegramBotProvider
from backend.app.runtime import build_runtime


class RuntimeWiringTest(unittest.TestCase):
    def _backend_root(self) -> Path:
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        return Path(temp_dir.name)

    def test_prefers_runtime_data_paths_by_default(self) -> None:
        backend_root = self._backend_root()

        settings = BackendSettings.from_env({}, backend_root=backend_root)

        self.assertEqual(settings.contacts_path, backend_root / "runtime_data" / "contacts.json")
        self.assertEqual(
            settings.pending_registrations_path,
            backend_root / "runtime_data" / "pending_registrations.json",
        )

    def test_falls_back_to_legacy_data_paths_when_runtime_data_is_missing(self) -> None:
        backend_root = self._backend_root()
        legacy_data_dir = backend_root / "app" / "data"
        legacy_data_dir.mkdir(parents=True)
        (legacy_data_dir / "contacts.json").write_text("[]", encoding="utf-8")
        (legacy_data_dir / "pending_registrations.json").write_text("[]", encoding="utf-8")

        settings = BackendSettings.from_env({}, backend_root=backend_root)

        self.assertEqual(settings.contacts_path, legacy_data_dir / "contacts.json")
        self.assertEqual(settings.pending_registrations_path, legacy_data_dir / "pending_registrations.json")

    def test_env_paths_override_runtime_defaults_and_legacy_fallbacks(self) -> None:
        backend_root = self._backend_root()
        custom_contacts_path = backend_root / "custom" / "contacts.json"
        custom_pending_path = backend_root / "custom" / "pending_registrations.json"

        settings = BackendSettings.from_env(
            {
                "VISITOR_NOTIFY_CONTACTS_PATH": str(custom_contacts_path),
                "VISITOR_NOTIFY_PENDING_REGISTRATIONS_PATH": str(custom_pending_path),
            },
            backend_root=backend_root,
        )

        self.assertEqual(settings.contacts_path, custom_contacts_path)
        self.assertEqual(settings.pending_registrations_path, custom_pending_path)

    def test_uses_stub_providers_when_credentials_are_missing(self) -> None:
        runtime = build_runtime(BackendSettings.from_env({}))

        self.assertIsInstance(runtime.telegram_provider, StubTelegramProvider)
        self.assertIsInstance(runtime.email_provider, StubEmailProvider)
        self.assertIsInstance(runtime.pending_registration_repository, JsonPendingTelegramRegistrationRepository)
        self.assertFalse(runtime.settings.allow_stub_delivery)

    def test_uses_real_providers_when_credentials_are_present(self) -> None:
        env = {
            "VISITOR_NOTIFY_TELEGRAM_BOT_TOKEN": "token-123",
            "VISITOR_NOTIFY_TELEGRAM_API_BASE_URL": "https://telegram.internal",
            "VISITOR_NOTIFY_TELEGRAM_TIMEOUT_SECONDS": "15",
            "VISITOR_NOTIFY_TELEGRAM_REGISTRATION_HELP_TEXT": "Escribile a RRHH.",
            "VISITOR_NOTIFY_EMAIL_MAILTRAP_TOKEN": "mailtrap-token",
            "VISITOR_NOTIFY_EMAIL_FROM": "robot@example.com",
            "VISITOR_NOTIFY_EMAIL_FROM_NAME": "CapaBot",
            "VISITOR_NOTIFY_EMAIL_TIMEOUT_SECONDS": "7.5",
        }

        runtime = build_runtime(BackendSettings.from_env(env))

        self.assertIsInstance(runtime.telegram_provider, TelegramBotProvider)
        self.assertIsInstance(runtime.email_provider, MailtrapEmailProvider)
        self.assertEqual(runtime.settings.telegram_api_base_url, "https://telegram.internal")
        self.assertEqual(runtime.settings.telegram_timeout_seconds, 15.0)
        self.assertEqual(runtime.settings.telegram_registration_help_text, "Escribile a RRHH.")
        self.assertTrue(str(runtime.settings.pending_registrations_path).endswith("pending_registrations.json"))
        self.assertEqual(runtime.settings.email_mailtrap_token, "mailtrap-token")
        self.assertEqual(runtime.settings.email_from_name, "CapaBot")
        self.assertEqual(runtime.settings.email_timeout_seconds, 7.5)

    def test_keeps_legacy_smtp_runtime_path_available(self) -> None:
        env = {
            "VISITOR_NOTIFY_EMAIL_SMTP_HOST": "smtp.example.com",
            "VISITOR_NOTIFY_EMAIL_FROM": "robot@example.com",
            "VISITOR_NOTIFY_EMAIL_SMTP_PORT": "2525",
        }

        runtime = build_runtime(BackendSettings.from_env(env))

        self.assertIsInstance(runtime.email_provider, SmtpEmailProvider)
        self.assertEqual(runtime.settings.email_smtp_port, 2525)

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
