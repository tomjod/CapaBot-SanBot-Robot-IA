from __future__ import annotations

from typing import Protocol

from backend.app.domain.contact import Contact
from backend.app.domain.notification import NotificationRequest


class MessageRewriter(Protocol):
    def rewrite(self, draft: str, contact: Contact, request: NotificationRequest) -> str:
        ...


class TemplateMessageBuilder:
    def __init__(self, rewriter: MessageRewriter | None = None) -> None:
        self._rewriter = rewriter

    def build(self, contact: Contact, request: NotificationRequest) -> str:
        location = f" en {request.location}" if request.location else ""
        visitor = request.visitor_name or "Visitante"
        draft = self._build_base_draft(visitor, contact, request, location)

        if self._rewriter is None:
            return draft

        rewritten = self._rewriter.rewrite(draft, contact, request).strip()
        return rewritten or draft

    @staticmethod
    def _build_base_draft(
        visitor: str,
        contact: Contact,
        request: NotificationRequest,
        location: str,
    ) -> str:
        if request.reason == "leave_message" and request.message:
            return (
                f"{visitor} dejó un mensaje para {contact.display_name}{location}. "
                f"Mensaje: {request.message}. "
                f"Dispositivo: {request.device_id}. "
                f"Hora: {request.requested_at.isoformat()}."
            )

        return (
            f"{visitor} solicita comunicarse con {contact.display_name}{location}. "
            f"Dispositivo: {request.device_id}. "
            f"Hora: {request.requested_at.isoformat()}."
        )
