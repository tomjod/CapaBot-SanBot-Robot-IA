from __future__ import annotations

import unittest
from datetime import datetime, timezone

from backend.app.domain.contact import Contact
from backend.app.domain.message_builder import TemplateMessageBuilder
from backend.app.domain.notification import NotificationRequest
from backend.app.domain.notification_service import NotificationService
from backend.app.infra.providers.email_provider import FakeEmailProvider
from backend.app.infra.providers.telegram_provider import FakeTelegramProvider


class InMemoryContactRepository:
    def __init__(self, contacts: list[Contact]) -> None:
        self._contacts = {contact.id: contact for contact in contacts}

    def get_contact(self, contact_id: str) -> Contact | None:
        return self._contacts.get(contact_id)


class NotificationServiceTest(unittest.TestCase):
    def _request(self) -> NotificationRequest:
        return NotificationRequest(
            contact_id="ventas-1",
            device_id="sanbot-01",
            requested_at=datetime(2026, 4, 9, 18, 0, tzinfo=timezone.utc),
            visitor_name="Ada",
            location="Recepción",
        )

    def test_returns_delivered_or_queued_when_telegram_succeeds(self) -> None:
        repository = InMemoryContactRepository(
            [Contact(id="ventas-1", display_name="Ventas", telegram_chat_id="chat-1")]
        )
        service = NotificationService(
            contact_repository=repository,
            telegram_provider=FakeTelegramProvider(status="sent"),
            email_provider=FakeEmailProvider(status="sent"),
            message_builder=TemplateMessageBuilder(),
        )

        outcome = service.submit(self._request())

        self.assertEqual(outcome.status, "delivered_or_queued")
        self.assertEqual(outcome.to_dict()["channels"], {"telegram": "sent", "email": "skipped"})
        self.assertEqual(outcome.to_dict()["detail"], "Listo, avisamos a Ventas por Telegram.")
        self.assertFalse(outcome.retryable)

    def test_returns_unavailable_without_false_success_when_contact_has_no_channels(self) -> None:
        repository = InMemoryContactRepository(
            [Contact(id="ventas-1", display_name="Ventas", telegram_chat_id=None, email=None, email_enabled=False)]
        )
        service = NotificationService(
            contact_repository=repository,
            telegram_provider=FakeTelegramProvider(status="sent"),
            message_builder=TemplateMessageBuilder(),
        )

        outcome = service.submit(self._request())

        self.assertEqual(outcome.status, "unavailable")
        self.assertEqual(outcome.to_dict()["channels"], {"telegram": "unavailable", "email": "unavailable"})
        self.assertIn("no tiene Telegram disponible", outcome.to_dict()["detail"])
        self.assertFalse(outcome.retryable)

    def test_returns_unavailable_when_contact_is_manually_disabled_even_with_channels(self) -> None:
        repository = InMemoryContactRepository(
            [
                Contact(
                    id="ventas-1",
                    display_name="Ventas",
                    enabled=False,
                    telegram_chat_id="chat-1",
                    email="ventas@example.com",
                    email_enabled=True,
                )
            ]
        )
        service = NotificationService(
            contact_repository=repository,
            telegram_provider=FakeTelegramProvider(status="sent"),
            email_provider=FakeEmailProvider(status="sent"),
            message_builder=TemplateMessageBuilder(),
        )

        outcome = service.submit(self._request())

        self.assertEqual(outcome.status, "unavailable")
        self.assertEqual(outcome.to_dict()["channels"], {"telegram": "unavailable", "email": "unavailable"})
        self.assertFalse(outcome.retryable)

    def test_uses_template_fallback_when_no_rewriter_is_configured(self) -> None:
        repository = InMemoryContactRepository(
            [Contact(id="ventas-1", display_name="Ventas", telegram_chat_id="chat-1")]
        )
        telegram_provider = FakeTelegramProvider(status="accepted")
        service = NotificationService(
            contact_repository=repository,
            telegram_provider=telegram_provider,
            message_builder=TemplateMessageBuilder(),
        )

        outcome = service.submit(self._request())

        self.assertEqual(outcome.status, "accepted")
        sent_message = telegram_provider.sent_messages[0][2]
        self.assertIn("🔔 Solicitud para hablar con una persona", sent_message)
        self.assertIn("Para: Ventas", sent_message)
        self.assertIn("Visitante: Ada", sent_message)
        self.assertIn("Ubicación: Recepción", sent_message)
        self.assertIn("Dispositivo: sanbot-01", sent_message)
        self.assertIn("Hora: 09 abr 2026, 14:00 (Chile)", sent_message)
        self.assertIn("Acción sugerida: Acercarse a recepción o responder a la visita.", sent_message)

    def test_leave_message_flow_includes_reason_and_visitor_message_in_dispatched_text(self) -> None:
        repository = InMemoryContactRepository(
            [Contact(id="ventas-1", display_name="Ventas", telegram_chat_id="chat-1")]
        )
        telegram_provider = FakeTelegramProvider(status="accepted")
        service = NotificationService(
            contact_repository=repository,
            telegram_provider=telegram_provider,
            message_builder=TemplateMessageBuilder(),
        )

        outcome = service.submit(
            NotificationRequest(
                contact_id="ventas-1",
                device_id="sanbot-01",
                requested_at=datetime(2026, 4, 9, 18, 0, tzinfo=timezone.utc),
                visitor_name="Ada",
                location="Recepción",
                reason="leave_message",
                message="Necesito que me contacten mañana.",
            )
        )

        self.assertEqual(outcome.status, "accepted")
        sent_message = telegram_provider.sent_messages[0][2]
        self.assertIn("📝 Nuevo mensaje de visita", sent_message)
        self.assertIn("Para: Ventas", sent_message)
        self.assertIn("Visitante: Ada", sent_message)
        self.assertIn("Ubicación: Recepción", sent_message)
        self.assertIn("Mensaje: Necesito que me contacten mañana.", sent_message)
        self.assertIn("Dispositivo: sanbot-01", sent_message)
        self.assertIn("Hora: 09 abr 2026, 14:00 (Chile)", sent_message)
        self.assertIn("Acción sugerida: Responder a la visita cuando le sea posible.", sent_message)

    def test_marks_request_retryable_when_all_attempted_channels_fail(self) -> None:
        repository = InMemoryContactRepository(
            [
                Contact(
                    id="ventas-1",
                    display_name="Ventas",
                    telegram_chat_id="chat-1",
                    email="ventas@example.com",
                    email_enabled=True,
                )
            ]
        )
        service = NotificationService(
            contact_repository=repository,
            telegram_provider=FakeTelegramProvider(status="failed"),
            email_provider=FakeEmailProvider(status="failed"),
            message_builder=TemplateMessageBuilder(),
        )

        outcome = service.submit(self._request())

        self.assertEqual(outcome.status, "failed")
        self.assertTrue(outcome.retryable)
        self.assertEqual(outcome.to_dict()["channels"], {"telegram": "failed", "email": "failed"})
        self.assertEqual(outcome.to_dict()["detail"], "No pudimos avisar a Ventas por Telegram ni email.")

    def test_reports_telegram_and_email_statuses_when_both_channels_are_attempted(self) -> None:
        repository = InMemoryContactRepository(
            [
                Contact(
                    id="ventas-1",
                    display_name="Ventas",
                    telegram_chat_id="chat-1",
                    email="ventas@example.com",
                    email_enabled=True,
                )
            ]
        )
        service = NotificationService(
            contact_repository=repository,
            telegram_provider=FakeTelegramProvider(status="sent"),
            email_provider=FakeEmailProvider(status="accepted"),
            message_builder=TemplateMessageBuilder(),
        )

        outcome = service.submit(self._request())

        self.assertEqual(outcome.status, "delivered_or_queued")
        self.assertFalse(outcome.retryable)
        self.assertEqual(outcome.to_dict()["channels"], {"telegram": "sent", "email": "accepted"})
        self.assertEqual(outcome.to_dict()["detail"], "Listo, avisamos a Ventas por Telegram y email.")


if __name__ == "__main__":
    unittest.main()
