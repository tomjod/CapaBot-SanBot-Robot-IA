from __future__ import annotations

import json
import threading
from pathlib import Path

from backend.app.domain.telegram_onboarding import PendingTelegramRegistration


class JsonPendingTelegramRegistrationRepository:
    def __init__(self, file_path: str | Path) -> None:
        self._file_path = Path(file_path)
        self._lock = threading.Lock()
        self._ensure_file()

    def _ensure_file(self) -> None:
        self._file_path.parent.mkdir(parents=True, exist_ok=True)
        if not self._file_path.exists():
            self._file_path.write_text("[]", encoding="utf-8")

    def _read_requests(self) -> list[dict[str, object]]:
        payload = json.loads(self._file_path.read_text(encoding="utf-8"))
        if not isinstance(payload, list):
            raise ValueError("pending registrations catalog must be a list")
        return payload

    def _write_requests(self, data: list[dict[str, object]]) -> None:
        self._file_path.write_text(
            json.dumps(data, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )

    def list_pending_registrations(self) -> list[PendingTelegramRegistration]:
        registrations = [PendingTelegramRegistration.from_dict(item) for item in self._read_requests()]
        pending_only = [item for item in registrations if item.status == "pending"]
        return sorted(pending_only, key=lambda item: item.created_at, reverse=True)

    def upsert_pending_registration(self, registration: PendingTelegramRegistration) -> PendingTelegramRegistration:
        with self._lock:
            data = self._read_requests()
            filtered = [
                item
                for item in data
                if not self._matches_registration(item, registration)
            ]
            filtered.append(registration.to_dict())
            self._write_requests(filtered)
        return registration

    def get_pending_registration(
        self,
        *,
        normalized_phone: str,
        telegram_user_id: str,
    ) -> PendingTelegramRegistration | None:
        normalized_phone = str(normalized_phone).strip()
        telegram_user_id = str(telegram_user_id).strip()

        for item in self._read_requests():
            if item.get("status") != "pending":
                continue

            if self._matches_identifiers(
                item,
                normalized_phone=normalized_phone,
                telegram_user_id=telegram_user_id,
            ):
                return PendingTelegramRegistration.from_dict(item)
        return None

    def get_pending_registration_by_telegram_user_id(self, telegram_user_id: str) -> PendingTelegramRegistration | None:
        normalized_user_id = str(telegram_user_id).strip()
        if not normalized_user_id:
            return None

        for item in self._read_requests():
            if item.get("status") != "pending":
                continue
            if str(item.get("telegram_user_id") or "") == normalized_user_id:
                return PendingTelegramRegistration.from_dict(item)
        return None

    def resolve_pending_registration(
        self,
        *,
        normalized_phone: str,
        telegram_user_id: str,
        status: str = "resolved",
    ) -> PendingTelegramRegistration:
        normalized_phone = str(normalized_phone).strip()
        telegram_user_id = str(telegram_user_id).strip()

        with self._lock:
            data = self._read_requests()
            for index, item in enumerate(data):
                if item.get("status") != "pending":
                    continue

                if self._matches_identifiers(
                    item,
                    normalized_phone=normalized_phone,
                    telegram_user_id=telegram_user_id,
                ):
                    updated = dict(item)
                    updated["status"] = status
                    data[index] = updated
                    self._write_requests(data)
                    return PendingTelegramRegistration.from_dict(updated)

        raise ValueError("Pending Telegram registration not found")

    @staticmethod
    def _matches_registration(item: dict[str, object], registration: PendingTelegramRegistration) -> bool:
        if item.get("status") != "pending":
            return False

        normalized_phone = str(item.get("normalized_phone") or "")
        telegram_user_id = str(item.get("telegram_user_id") or "")
        return (
            normalized_phone == registration.normalized_phone
            or telegram_user_id == registration.telegram_user_id
        )

    @staticmethod
    def _matches_identifiers(
        item: dict[str, object],
        *,
        normalized_phone: str,
        telegram_user_id: str,
    ) -> bool:
        item_phone = str(item.get("normalized_phone") or "")
        item_telegram_user_id = str(item.get("telegram_user_id") or "")
        return item_phone == normalized_phone and item_telegram_user_id == telegram_user_id
