from __future__ import annotations

from typing import Any, Protocol

from backend.app.api.routes._contact_models import ContactCreate, ContactUpdate
from backend.app.domain.contact import Contact
from backend.app.domain.company_catalog import list_company_catalog


class ContactListRepository(Protocol):
    def list_contacts(self) -> list[Any]:
        ...


class ContactRepository(ContactListRepository, Protocol):
    def get_contact(self, contact_id: str) -> Contact | None:
        ...

    def add_contact(self, contact: Contact) -> Contact:
        ...

    def update_contact(self, contact: Contact) -> Contact:
        ...

    def delete_contact(self, contact_id: str) -> bool:
        ...


def list_contacts_response(repository: ContactListRepository) -> list[dict[str, Any]]:
    return [contact.to_summary() for contact in repository.list_contacts()]


def full_contact_response(contact: Contact) -> dict[str, Any]:
    return {
        "id": contact.id,
        "display_name": contact.display_name,
        "job_title": contact.job_title,
        "cargo": contact.job_title,
        "company": contact.company,
        "empresa": contact.company,
        "company_label": contact.company_label,
        "enabled": contact.enabled,
        "phone": contact.phone,
        "telefono": contact.phone,
        "telegram_chat_id": contact.telegram_chat_id,
        "telegram_user_id": contact.telegram_user_id,
        "telegram_username": contact.telegram_username,
        "email": contact.email,
        "email_enabled": contact.email_enabled,
        "channels": {
            "telegram": contact.telegram_available,
            "email": contact.email_available,
        },
        "available": contact.available,
    }


def register_contacts_routes(app: Any, repository: ContactRepository) -> None:
    from fastapi import APIRouter, HTTPException

    router = APIRouter()

    @router.get("/contacts")
    def get_contacts() -> list[dict[str, Any]]:
        return list_contacts_response(repository)

    @router.get("/contacts/companies")
    def get_contact_companies() -> list[dict[str, str]]:
        return [{"id": entry.id, "label": entry.label} for entry in list_company_catalog()]

    @router.get("/contacts/{contact_id}")
    def get_contact(contact_id: str) -> dict[str, Any]:
        contact = repository.get_contact(contact_id)
        if contact is None:
            raise HTTPException(status_code=404, detail=f"Contact '{contact_id}' not found")
        return full_contact_response(contact)

    @router.post("/contacts", status_code=201)
    def create_contact(data: ContactCreate) -> dict[str, Any]:
        try:
            contact = Contact(
                id=data.id,
                display_name=data.display_name,
                job_title=data.job_title,
                company=data.company,
                enabled=data.enabled,
                phone=data.phone,
                telegram_chat_id=data.telegram_chat_id,
                telegram_user_id=data.telegram_user_id,
                telegram_username=data.telegram_username,
                email=data.email,
                email_enabled=data.email_enabled,
            )
            repository.add_contact(contact)
            return full_contact_response(contact)
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))

    @router.put("/contacts/{contact_id}")
    def update_contact(contact_id: str, data: ContactUpdate) -> dict[str, Any]:
        existing = repository.get_contact(contact_id)
        if existing is None:
            raise HTTPException(status_code=404, detail=f"Contact '{contact_id}' not found")
        try:
            contact = Contact(
                id=contact_id,
                display_name=data.display_name,
                job_title=data.job_title,
                company=data.company,
                enabled=data.enabled,
                phone=data.phone,
                telegram_chat_id=data.telegram_chat_id,
                telegram_user_id=data.telegram_user_id,
                telegram_username=data.telegram_username,
                email=data.email,
                email_enabled=data.email_enabled,
            )
            repository.update_contact(contact)
            return full_contact_response(contact)
        except ValueError as e:
            raise HTTPException(status_code=400, detail=str(e))

    @router.delete("/contacts/{contact_id}", status_code=204)
    def delete_contact(contact_id: str) -> None:
        if not repository.delete_contact(contact_id):
            raise HTTPException(status_code=404, detail=f"Contact '{contact_id}' not found")

    app.include_router(router)
