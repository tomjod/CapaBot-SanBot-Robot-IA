from __future__ import annotations

import unittest
from datetime import datetime, timezone

from backend.app.domain.contact import Contact
from backend.app.domain.message_builder import TemplateMessageBuilder
from backend.app.domain.notification import NotificationRequest


class TemplateMessageBuilderTest(unittest.TestCase):
    def setUp(self) -> None:
        self.builder = TemplateMessageBuilder()
        self.contact = Contact(id="ventas-1", display_name="Ventas", telegram_chat_id="chat-1")

    def test_builds_operational_message_for_talk_to_person_flow(self) -> None:
        message = self.builder.build(
            self.contact,
            NotificationRequest(
                contact_id="ventas-1",
                device_id="sanbot-01",
                requested_at=datetime(2026, 4, 9, 18, 0, tzinfo=timezone.utc),
                visitor_name="Ada",
                location="Recepción",
            ),
        )

        self.assertEqual(
            message,
            "\n".join(
                [
                    "🔔 Solicitud para hablar con una persona",
                    "Para: Ventas",
                    "Visitante: Ada",
                    "Ubicación: Recepción",
                    "Dispositivo: sanbot-01",
                    "Hora: 09 abr 2026, 14:00 (Chile)",
                    "Acción sugerida: Acercarse a recepción o responder a la visita.",
                ]
            ),
        )

    def test_builds_operational_message_for_leave_message_flow(self) -> None:
        message = self.builder.build(
            self.contact,
            NotificationRequest(
                contact_id="ventas-1",
                device_id="sanbot-01",
                requested_at=datetime(2026, 4, 9, 18, 0, tzinfo=timezone.utc),
                visitor_name="Ada",
                location="Recepción",
                reason="leave_message",
                message="Necesito que me contacten mañana.",
            ),
        )

        self.assertEqual(
            message,
            "\n".join(
                [
                    "📝 Nuevo mensaje de visita",
                    "Para: Ventas",
                    "Visitante: Ada",
                    "Ubicación: Recepción",
                    "Mensaje: Necesito que me contacten mañana.",
                    "Dispositivo: sanbot-01",
                    "Hora: 09 abr 2026, 14:00 (Chile)",
                    "Acción sugerida: Responder a la visita cuando le sea posible.",
                ]
            ),
        )

    def test_falls_back_to_default_labels_when_optional_data_is_missing(self) -> None:
        message = self.builder.build(
            self.contact,
            NotificationRequest(
                contact_id="ventas-1",
                device_id="sanbot-01",
                requested_at=datetime(2026, 4, 9, 18, 0, tzinfo=timezone.utc),
            ),
        )

        self.assertIn("Visitante: Visitante no identificado", message)
        self.assertIn("Ubicación: Sin especificar", message)


if __name__ == "__main__":
    unittest.main()
