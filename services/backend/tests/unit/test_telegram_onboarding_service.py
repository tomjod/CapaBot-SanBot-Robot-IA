from __future__ import annotations

import json
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path

from backend.app.domain.telegram_onboarding import (
    TelegramCompanySelectionRequest,
    TelegramJobTitleRequest,
    TelegramOnboardingRequest,
    TelegramOnboardingService,
)
from backend.app.infra.json_contacts import JsonContactRepository
from backend.app.infra.json_pending_registrations import JsonPendingTelegramRegistrationRepository


class TelegramOnboardingServiceTest(unittest.TestCase):
    def _repositories(self) -> tuple[JsonContactRepository, JsonPendingTelegramRegistrationRepository]:
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        contacts_path = Path(temp_dir.name) / "contacts.json"
        contacts_path.write_text(
            json.dumps(
                [
                    {
                        "id": "ventas",
                        "display_name": "Ventas",
                        "job_title": "Ejecutiva comercial",
                        "company": "transformapp",
                        "phone": "+56 9 1234 5678",
                        "telegram_chat_id": None,
                        "email": "ventas@example.com",
                        "email_enabled": True,
                    }
                ]
            ),
            encoding="utf-8",
        )
        pending_path = Path(temp_dir.name) / "pending_registrations.json"
        pending_path.write_text("[]", encoding="utf-8")
        return JsonContactRepository(contacts_path), JsonPendingTelegramRegistrationRepository(pending_path)

    def test_binds_contact_when_shared_phone_matches(self) -> None:
        repository, pending_repository = self._repositories()
        service = TelegramOnboardingService(repository, pending_repository)

        result = service.onboard(
            TelegramOnboardingRequest(
                phone_number="56912345678",
                telegram_user_id="444",
                telegram_chat_id="555",
                telegram_username="ventas_contacto",
            )
        )

        self.assertEqual(result.status, "matched")
        self.assertEqual(result.contact.id, "ventas")
        self.assertEqual(result.contact.telegram_chat_id, "555")
        self.assertEqual(result.contact.telegram_user_id, "444")

    def test_returns_company_selection_when_phone_does_not_match(self) -> None:
        repository, pending_repository = self._repositories()
        service = TelegramOnboardingService(
            repository,
            pending_repository,
            now_provider=lambda: datetime(2026, 4, 13, 12, 0, tzinfo=timezone.utc),
        )

        result = service.onboard(
            TelegramOnboardingRequest(
                phone_number="+56 9 0000 0000",
                telegram_user_id="444",
                telegram_chat_id="555",
                telegram_username="ventas_contacto",
                first_name="Ada",
            )
        )

        self.assertEqual(result.status, "awaiting_company_selection")
        self.assertIsNone(result.contact)
        self.assertEqual(result.normalized_phone, "56900000000")
        self.assertIsNotNone(result.pending_registration)
        assert result.pending_registration is not None
        self.assertEqual(result.pending_registration.first_name, "Ada")
        self.assertEqual(result.pending_registration.flow_state, "awaiting_company")
        self.assertEqual(result.pending_registration.created_at, "2026-04-13T12:00:00Z")
        self.assertEqual(pending_repository.list_pending_registrations()[0].normalized_phone, "56900000000")
        self.assertTrue(result.available_companies)

    def test_duplicate_pending_requests_are_upserted_by_phone_or_telegram_user(self) -> None:
        repository, pending_repository = self._repositories()
        service = TelegramOnboardingService(
            repository,
            pending_repository,
            now_provider=lambda: datetime(2026, 4, 13, 12, 30, tzinfo=timezone.utc),
        )

        service.onboard(
            TelegramOnboardingRequest(
                phone_number="+56 9 0000 0000",
                telegram_user_id="444",
                telegram_chat_id="555",
                telegram_username="ada_old",
                first_name="Ada",
            )
        )
        service.onboard(
            TelegramOnboardingRequest(
                phone_number="56900000000",
                telegram_user_id="444",
                telegram_chat_id="777",
                telegram_username="ada_new",
                first_name="Ada Nueva",
            )
        )

        registrations = pending_repository.list_pending_registrations()

        self.assertEqual(len(registrations), 1)
        self.assertEqual(registrations[0].telegram_chat_id, "777")
        self.assertEqual(registrations[0].telegram_username, "ada_new")
        self.assertEqual(registrations[0].first_name, "Ada Nueva")

    def test_company_selection_updates_pending_registration(self) -> None:
        repository, pending_repository = self._repositories()
        service = TelegramOnboardingService(repository, pending_repository)
        service.onboard(
            TelegramOnboardingRequest(
                phone_number="+56 9 0000 0000",
                telegram_user_id="444",
                telegram_chat_id="555",
                telegram_username="ada",
                first_name="Ada",
            )
        )

        result = service.select_company(
            TelegramCompanySelectionRequest(
                telegram_user_id="444",
                company="transformapp",
            )
        )

        self.assertEqual(result.status, "awaiting_job_title")
        assert result.pending_registration is not None
        self.assertEqual(result.pending_registration.company, "transformapp")
        self.assertEqual(result.pending_registration.flow_state, "awaiting_job_title")

    def test_job_title_submission_completes_pending_registration(self) -> None:
        repository, pending_repository = self._repositories()
        service = TelegramOnboardingService(repository, pending_repository)
        service.onboard(
            TelegramOnboardingRequest(
                phone_number="+56 9 0000 0000",
                telegram_user_id="444",
                telegram_chat_id="555",
                telegram_username="ada",
                first_name="Ada",
            )
        )
        service.select_company(TelegramCompanySelectionRequest(telegram_user_id="444", company="transformapp"))

        result = service.submit_job_title(
            TelegramJobTitleRequest(
                telegram_user_id="444",
                job_title="Recepcionista",
            )
        )

        self.assertEqual(result.status, "pending_registration_ready")
        assert result.pending_registration is not None
        self.assertEqual(result.pending_registration.job_title, "Recepcionista")
        self.assertEqual(result.pending_registration.flow_state, "ready_for_approval")


if __name__ == "__main__":
    unittest.main()
