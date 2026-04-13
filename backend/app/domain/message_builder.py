from __future__ import annotations

from datetime import UTC
from datetime import datetime
from typing import Protocol
from zoneinfo import ZoneInfo

from backend.app.domain.contact import Contact
from backend.app.domain.notification import NotificationRequest


class MessageRewriter(Protocol):
    def rewrite(self, draft: str, contact: Contact, request: NotificationRequest) -> str:
        ...


class TemplateMessageBuilder:
    def __init__(self, rewriter: MessageRewriter | None = None, timezone_name: str = "America/Santiago") -> None:
        self._rewriter = rewriter
        self._timezone_name = timezone_name or "America/Santiago"

    def build(self, contact: Contact, request: NotificationRequest) -> str:
        draft = self._build_base_draft(contact, request, self._timezone_name)

        if self._rewriter is None:
            return draft

        rewritten = self._rewriter.rewrite(draft, contact, request).strip()
        return rewritten or draft

    @staticmethod
    def _build_base_draft(
        contact: Contact,
        request: NotificationRequest,
        timezone_name: str,
    ) -> str:
        visitor = request.visitor_name or "Visitante no identificado"
        location = request.location or "Sin especificar"
        timestamp = _format_timestamp(request.requested_at, timezone_name)

        if request.reason == "leave_message" and request.message:
            return "\n".join(
                [
                    "📝 Nuevo mensaje de visita",
                    f"Para: {contact.display_name}",
                    f"Visitante: {visitor}",
                    f"Ubicación: {location}",
                    f"Mensaje: {request.message}",
                    f"Dispositivo: {request.device_id}",
                    f"Hora: {timestamp}",
                    "Acción sugerida: Responder a la visita cuando le sea posible.",
                ]
            )

        return "\n".join(
            [
                "🔔 Solicitud para hablar con una persona",
                f"Para: {contact.display_name}",
                f"Visitante: {visitor}",
                f"Ubicación: {location}",
                f"Dispositivo: {request.device_id}",
                f"Hora: {timestamp}",
                "Acción sugerida: Acercarse a recepción o responder a la visita.",
            ]
        )


def _format_timestamp(value: datetime, timezone_name: str) -> str:
    try:
        target_zone = ZoneInfo(timezone_name)
    except Exception:
        target_zone = UTC
        timezone_name = "UTC"

    normalized = value.astimezone(target_zone)
    month_names = {
        1: "ene",
        2: "feb",
        3: "mar",
        4: "abr",
        5: "may",
        6: "jun",
        7: "jul",
        8: "ago",
        9: "sep",
        10: "oct",
        11: "nov",
        12: "dic",
    }
    month = month_names.get(normalized.month, f"{normalized.month:02d}")
    zone_label = "Chile" if timezone_name == "America/Santiago" else timezone_name
    return f"{normalized.day:02d} {month} {normalized.year}, {normalized:%H:%M} ({zone_label})"
