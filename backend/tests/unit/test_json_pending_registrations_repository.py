from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from backend.app.domain.telegram_onboarding import PendingTelegramRegistration
from backend.app.infra.json_pending_registrations import JsonPendingTelegramRegistrationRepository


class JsonPendingTelegramRegistrationRepositoryTest(unittest.TestCase):
    def _repository(self, payload: list[dict[str, object]] | None = None) -> JsonPendingTelegramRegistrationRepository:
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        file_path = Path(temp_dir.name) / "pending_registrations.json"
        file_path.write_text(json.dumps(payload or []), encoding="utf-8")
        return JsonPendingTelegramRegistrationRepository(file_path)

    def test_lists_pending_registrations_in_reverse_chronological_order(self) -> None:
        repository = self._repository(
            [
                {
                    "phone_number": "+56 9 1111 1111",
                    "normalized_phone": "56911111111",
                    "telegram_user_id": "111",
                    "telegram_chat_id": "211",
                    "company": "transformapp",
                    "job_title": "Recepcionista",
                    "flow_state": "ready_for_approval",
                    "status": "pending",
                    "created_at": "2026-04-13T11:00:00Z",
                },
                {
                    "phone_number": "+56 9 2222 2222",
                    "normalized_phone": "56922222222",
                    "telegram_user_id": "222",
                    "telegram_chat_id": "322",
                    "company": "tps",
                    "job_title": "Operaciones",
                    "flow_state": "ready_for_approval",
                    "status": "pending",
                    "created_at": "2026-04-13T12:00:00Z",
                },
            ]
        )

        registrations = repository.list_pending_registrations()

        self.assertEqual([item.telegram_user_id for item in registrations], ["222", "111"])

    def test_upserts_existing_pending_registration_by_phone_or_user(self) -> None:
        repository = self._repository(
            [
                {
                    "phone_number": "+56 9 1111 1111",
                    "normalized_phone": "56911111111",
                    "telegram_user_id": "111",
                    "telegram_chat_id": "211",
                    "telegram_username": "ada_old",
                    "company": "transformapp",
                    "job_title": "Recepcionista",
                    "flow_state": "ready_for_approval",
                    "status": "pending",
                    "created_at": "2026-04-13T11:00:00Z",
                }
            ]
        )

        repository.upsert_pending_registration(
            PendingTelegramRegistration(
                phone_number="56911111111",
                normalized_phone="56911111111",
                telegram_user_id="111",
                telegram_chat_id="999",
                telegram_username="ada_new",
                first_name="Ada",
                company="tps",
                job_title="Operaciones",
                flow_state="ready_for_approval",
                created_at="2026-04-13T12:00:00Z",
            )
        )

        registrations = repository.list_pending_registrations()

        self.assertEqual(len(registrations), 1)
        self.assertEqual(registrations[0].telegram_chat_id, "999")
        self.assertEqual(registrations[0].telegram_username, "ada_new")
        self.assertEqual(registrations[0].company, "tps")
        self.assertEqual(registrations[0].job_title, "Operaciones")

    def test_gets_pending_registration_by_telegram_user_id(self) -> None:
        repository = self._repository(
            [
                {
                    "phone_number": "+56 9 1111 1111",
                    "normalized_phone": "56911111111",
                    "telegram_user_id": "111",
                    "telegram_chat_id": "211",
                    "company": "transformapp",
                    "job_title": "Recepcionista",
                    "flow_state": "ready_for_approval",
                    "status": "pending",
                    "created_at": "2026-04-13T11:00:00Z",
                }
            ]
        )

        registration = repository.get_pending_registration_by_telegram_user_id("111")

        self.assertIsNotNone(registration)
        assert registration is not None
        self.assertEqual(registration.company, "transformapp")
        self.assertEqual(registration.job_title, "Recepcionista")

    def test_resolve_marks_registration_as_non_pending(self) -> None:
        repository = self._repository(
            [
                {
                    "phone_number": "+56 9 1111 1111",
                    "normalized_phone": "56911111111",
                    "telegram_user_id": "111",
                    "telegram_chat_id": "211",
                    "status": "pending",
                    "created_at": "2026-04-13T11:00:00Z",
                }
            ]
        )

        resolved = repository.resolve_pending_registration(
            normalized_phone="56911111111",
            telegram_user_id="111",
            status="approved",
        )

        self.assertEqual(resolved.status, "approved")
        self.assertEqual(repository.list_pending_registrations(), [])


if __name__ == "__main__":
    unittest.main()
