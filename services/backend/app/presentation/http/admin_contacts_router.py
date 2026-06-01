from __future__ import annotations

from pathlib import Path
from typing import Any
from urllib.parse import urlencode

from fastapi import APIRouter, Form, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates

from backend.app.domain.company_catalog import list_company_catalog
from backend.app.domain.contact import Contact
from backend.app.domain.contact_ids import generate_contact_id


def _build_admin_contacts_redirect(**params: str) -> str:
    filtered_params = {key: value for key, value in params.items() if value}
    if not filtered_params:
        return "/admin/contacts"
    return f"/admin/contacts?{urlencode(filtered_params)}"


def _checkbox_value(raw_value: str | None) -> bool:
    return raw_value == "true"


def _admin_form_state(request: Any, prefix: str = "") -> dict[str, Any]:
    query_params = request.query_params
    return {
        "id": query_params.get(f"{prefix}id", ""),
        "display_name": query_params.get(f"{prefix}display_name", ""),
        "job_title": query_params.get(f"{prefix}job_title", ""),
        "company": query_params.get(f"{prefix}company", ""),
        "enabled": query_params.get(f"{prefix}enabled", "true") != "false",
        "phone": query_params.get(f"{prefix}phone", ""),
        "email": query_params.get(f"{prefix}email", ""),
        "email_enabled": _checkbox_value(query_params.get(f"{prefix}email_enabled")),
    }


def _admin_approval_form_state(request: Any, pending_registration_repository: Any | None = None) -> dict[str, Any]:
    query_params = request.query_params
    normalized_phone = query_params.get("approve_normalized_phone", "")
    telegram_user_id = query_params.get("approve_telegram_user_id", "")
    pending_registration = None
    if pending_registration_repository is not None and normalized_phone and telegram_user_id:
        pending_registration = pending_registration_repository.get_pending_registration(
            normalized_phone=normalized_phone,
            telegram_user_id=telegram_user_id,
        )

    first_name = pending_registration.first_name if pending_registration is not None and pending_registration.first_name else ""
    telegram_username = (
        pending_registration.telegram_username
        if pending_registration is not None and pending_registration.telegram_username
        else ""
    )
    suggested_name = first_name or telegram_username
    job_title = query_params.get(
        "approve_job_title",
        pending_registration.job_title if pending_registration is not None and pending_registration.job_title else "",
    )
    company = query_params.get(
        "approve_company",
        pending_registration.company if pending_registration is not None and pending_registration.company else "",
    )
    return {
        "normalized_phone": normalized_phone,
        "telegram_user_id": telegram_user_id,
        "telegram_chat_id": pending_registration.telegram_chat_id if pending_registration is not None else "",
        "telegram_username": telegram_username,
        "first_name": first_name,
        "display_name": query_params.get("approve_display_name", suggested_name),
        "job_title": job_title,
        "company": company,
        "flow_state": pending_registration.flow_state if pending_registration is not None else "",
        "enabled": query_params.get("approve_enabled", "true") != "false",
        "phone": query_params.get(
            "approve_phone",
            pending_registration.phone_number if pending_registration is not None else "",
        ),
        "email": query_params.get("approve_email", ""),
        "email_enabled": _checkbox_value(query_params.get("approve_email_enabled")),
    }


def register_admin_contacts_routes(app: Any, repository: Any, pending_registration_repository: Any | None = None) -> None:
    router = APIRouter()

    templates_dir = Path(__file__).resolve().parent.parent.parent / "templates"
    templates = Jinja2Templates(directory=str(templates_dir))

    @router.get("/admin/contacts", response_class=HTMLResponse)
    async def admin_contacts_page(request: Request) -> Any:
        contacts = repository.list_contacts()
        pending_registrations = (
            pending_registration_repository.list_pending_registrations()
            if pending_registration_repository is not None
            else []
        )
        return templates.TemplateResponse(
            request,
            "admin/contacts.html",
            {
                "contacts": contacts,
                "pending_registrations": pending_registrations,
                "error_message": request.query_params.get("error"),
                "error_mode": request.query_params.get("error_mode"),
                "create_form": _admin_form_state(request),
                "update_form": _admin_form_state(request, prefix="update_"),
                "approval_form": _admin_approval_form_state(request, pending_registration_repository),
                "companies": list_company_catalog(),
            },
        )

    @router.post("/admin/contacts", response_class=RedirectResponse)
    async def admin_create_contact(
        id: str = Form(),
        display_name: str = Form(),
        job_title: str = Form(),
        company: str = Form(),
        enabled: bool = Form(False),
        phone: str = Form(),
        email: str = Form(""),
        email_enabled: bool = Form(False),
    ) -> Any:
        try:
            contact = Contact(
                id=id.strip(),
                display_name=display_name.strip(),
                job_title=job_title.strip(),
                company=company.strip(),
                enabled=enabled,
                phone=phone.strip(),
                email=email.strip() or None,
                email_enabled=email_enabled,
            )
            repository.add_contact(contact)
        except ValueError as error:
            return RedirectResponse(
                url=_build_admin_contacts_redirect(
                    error=str(error),
                    error_mode="create",
                    id=id.strip(),
                    display_name=display_name.strip(),
                    job_title=job_title.strip(),
                    company=company.strip(),
                    enabled="true" if enabled else "false",
                    phone=phone.strip(),
                    email=email.strip(),
                    email_enabled="true" if email_enabled else "",
                ),
                status_code=303,
            )
        return RedirectResponse(url="/admin/contacts", status_code=303)

    @router.post("/admin/contacts/{contact_id}/update", response_class=RedirectResponse)
    async def admin_update_contact(
        contact_id: str,
        display_name: str = Form(),
        job_title: str = Form(),
        company: str = Form(),
        enabled: bool = Form(False),
        phone: str = Form(),
        email: str = Form(""),
        email_enabled: bool = Form(False),
    ) -> Any:
        try:
            existing = repository.get_contact(contact_id)
            if existing is None:
                raise ValueError(f"Contact with id '{contact_id}' not found")
            contact = Contact(
                id=contact_id,
                display_name=display_name.strip(),
                job_title=job_title.strip(),
                company=company.strip(),
                enabled=enabled,
                phone=phone.strip(),
                telegram_chat_id=existing.telegram_chat_id,
                telegram_user_id=existing.telegram_user_id,
                telegram_username=existing.telegram_username,
                email=email.strip() or None,
                email_enabled=email_enabled,
            )
            repository.update_contact(contact)
        except ValueError as error:
            return RedirectResponse(
                url=_build_admin_contacts_redirect(
                    error=str(error),
                    error_mode="update",
                    update_id=contact_id,
                    update_display_name=display_name.strip(),
                    update_job_title=job_title.strip(),
                    update_company=company.strip(),
                    update_enabled="true" if enabled else "false",
                    update_phone=phone.strip(),
                    update_email=email.strip(),
                    update_email_enabled="true" if email_enabled else "",
                ),
                status_code=303,
            )
        return RedirectResponse(url="/admin/contacts", status_code=303)

    @router.post("/admin/contacts/{contact_id}/delete", response_class=RedirectResponse)
    async def admin_delete_contact(contact_id: str) -> Any:
        repository.delete_contact(contact_id)
        return RedirectResponse(url="/admin/contacts", status_code=303)

    @router.post("/admin/pending-registrations/approve", response_class=RedirectResponse)
    async def admin_approve_pending_registration(
        normalized_phone: str = Form(),
        telegram_user_id: str = Form(),
        display_name: str = Form(),
        job_title: str = Form(),
        company: str = Form(),
        enabled: bool = Form(False),
        phone: str = Form(),
        email: str = Form(""),
        email_enabled: bool = Form(False),
    ) -> Any:
        if pending_registration_repository is None:
            return RedirectResponse(
                url=_build_admin_contacts_redirect(
                    error="Pending Telegram registrations are not configured",
                    error_mode="approve",
                ),
                status_code=303,
            )

        try:
            pending_registration = pending_registration_repository.get_pending_registration(
                normalized_phone=normalized_phone,
                telegram_user_id=telegram_user_id,
            )
            if pending_registration is None:
                raise ValueError("Pending Telegram registration not found")
            if pending_registration.company is None or pending_registration.job_title is None:
                raise ValueError("Pending Telegram registration is incomplete and cannot be approved yet")

            generated_id = generate_contact_id(
                company_id=company.strip(),
                job_title=job_title.strip(),
                existing_ids=[contact.id for contact in repository.list_contacts()],
            )

            contact = Contact(
                id=generated_id,
                display_name=display_name.strip(),
                job_title=job_title.strip(),
                company=company.strip(),
                enabled=enabled,
                phone=phone.strip(),
                telegram_chat_id=pending_registration.telegram_chat_id,
                telegram_user_id=pending_registration.telegram_user_id,
                telegram_username=pending_registration.telegram_username,
                email=email.strip() or None,
                email_enabled=email_enabled,
            )
            repository.add_contact(contact)
            pending_registration_repository.resolve_pending_registration(
                normalized_phone=pending_registration.normalized_phone,
                telegram_user_id=pending_registration.telegram_user_id,
                status="approved",
            )
        except ValueError as error:
            return RedirectResponse(
                url=_build_admin_contacts_redirect(
                    error=str(error),
                    error_mode="approve",
                    approve_normalized_phone=normalized_phone.strip(),
                    approve_telegram_user_id=telegram_user_id.strip(),
                    approve_display_name=display_name.strip(),
                    approve_job_title=job_title.strip(),
                    approve_company=company.strip(),
                    approve_enabled="true" if enabled else "false",
                    approve_phone=phone.strip(),
                    approve_email=email.strip(),
                    approve_email_enabled="true" if email_enabled else "",
                ),
                status_code=303,
            )

        return RedirectResponse(url="/admin/contacts", status_code=303)

    @router.post("/admin/pending-registrations/resolve", response_class=RedirectResponse)
    async def admin_resolve_pending_registration(
        normalized_phone: str = Form(),
        telegram_user_id: str = Form(),
    ) -> Any:
        if pending_registration_repository is None:
            return RedirectResponse(url="/admin/contacts", status_code=303)

        try:
            pending_registration_repository.resolve_pending_registration(
                normalized_phone=normalized_phone,
                telegram_user_id=telegram_user_id,
                status="resolved",
            )
        except ValueError:
            pass
        return RedirectResponse(url="/admin/contacts", status_code=303)

    app.include_router(router)
