from __future__ import annotations

import json
from pathlib import Path

from backend.app.domain.contact import Contact


class JsonContactRepository:
    def __init__(self, file_path: str | Path) -> None:
        self._file_path = Path(file_path)

    def list_contacts(self) -> list[Contact]:
        payload = json.loads(self._file_path.read_text(encoding="utf-8"))
        if not isinstance(payload, list):
            raise ValueError("contacts catalog must be a list")

        return [Contact.from_dict(item) for item in payload]

    def get_contact(self, contact_id: str) -> Contact | None:
        for contact in self.list_contacts():
            if contact.id == contact_id:
                return contact
        return None
