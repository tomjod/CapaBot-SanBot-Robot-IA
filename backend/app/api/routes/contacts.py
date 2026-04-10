from __future__ import annotations

from typing import Any, Protocol


class ContactListRepository(Protocol):
    def list_contacts(self) -> list[Any]:
        ...


def list_contacts_response(repository: ContactListRepository) -> list[dict[str, Any]]:
    return [contact.to_summary() for contact in repository.list_contacts()]


def register_contacts_routes(app: Any, repository: ContactListRepository) -> None:
    from fastapi import APIRouter

    router = APIRouter()

    @router.get("/contacts")
    def get_contacts() -> list[dict[str, Any]]:
        return list_contacts_response(repository)

    app.include_router(router)
