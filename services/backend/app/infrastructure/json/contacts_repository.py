from __future__ import annotations

import json
import threading
from pathlib import Path

from backend.app.domain.contact import Contact
from backend.app.domain.phone_numbers import normalize_phone_number


class JsonContactRepository:
    def __init__(self, file_path: str | Path) -> None:
        self._file_path = Path(file_path)
        self._lock = threading.Lock()

    def _read_contacts(self) -> list[dict]:
        return json.loads(self._file_path.read_text(encoding="utf-8"))

    def _write_contacts(self, data: list[dict]) -> None:
        self._file_path.write_text(
            json.dumps(data, indent=2, ensure_ascii=False),
            encoding="utf-8",
        )

    def list_contacts(self) -> list[Contact]:
        payload = self._read_contacts()
        if not isinstance(payload, list):
            raise ValueError("contacts catalog must be a list")

        return [Contact.from_dict(item) for item in payload]

    def get_contact(self, contact_id: str) -> Contact | None:
        for contact in self.list_contacts():
            if contact.id == contact_id:
                return contact
        return None

    def find_contact_by_phone(self, phone_number: str) -> Contact | None:
        normalized_phone = normalize_phone_number(phone_number)
        if normalized_phone is None:
            return None

        for contact in self.list_contacts():
            if contact.normalized_phone == normalized_phone:
                return contact
        return None

    def find_contact_by_telegram_user_id(self, telegram_user_id: str) -> Contact | None:
        normalized_user_id = str(telegram_user_id).strip()
        if not normalized_user_id:
            return None

        for contact in self.list_contacts():
            if contact.telegram_user_id == normalized_user_id:
                return contact
        return None

    def add_contact(self, contact: Contact) -> Contact:
        with self._lock:
            data = self._read_contacts()
            for item in data:
                if item.get("id") == contact.id:
                    raise ValueError(f"Contact with id '{contact.id}' already exists")
            data.append(self._contact_to_dict(contact))
            self._write_contacts(data)
        return contact

    def update_contact(self, contact: Contact) -> Contact:
        with self._lock:
            data = self._read_contacts()
            for index, item in enumerate(data):
                if item.get("id") == contact.id:
                    data[index] = self._contact_to_dict(contact)
                    self._write_contacts(data)
                    return contact
            raise ValueError(f"Contact with id '{contact.id}' not found")

    def bind_contact_telegram(
        self,
        *,
        contact_id: str,
        telegram_chat_id: str,
        telegram_user_id: str,
        telegram_username: str | None = None,
    ) -> Contact:
        existing = self.get_contact(contact_id)
        if existing is None:
            raise ValueError(f"Contact with id '{contact_id}' not found")

        bound_contact = existing.with_telegram_binding(
            telegram_chat_id=telegram_chat_id,
            telegram_user_id=telegram_user_id,
            telegram_username=telegram_username,
        )
        return self.update_contact(bound_contact)

    def delete_contact(self, contact_id: str) -> bool:
        with self._lock:
            data = self._read_contacts()
            new_data = [item for item in data if item.get("id") != contact_id]
            if len(new_data) == len(data):
                return False
            self._write_contacts(new_data)
            return True

    @staticmethod
    def _contact_to_dict(contact: Contact) -> dict:
        return {
            "id": contact.id,
            "display_name": contact.display_name,
            "job_title": contact.job_title,
            "company": contact.company,
            "enabled": contact.enabled,
            "phone": contact.phone,
            "telegram_chat_id": contact.telegram_chat_id,
            "telegram_user_id": contact.telegram_user_id,
            "telegram_username": contact.telegram_username,
            "email": contact.email,
            "email_enabled": contact.email_enabled,
        }
