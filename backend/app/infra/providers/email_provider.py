from __future__ import annotations

import smtplib
from email.message import EmailMessage
from typing import Callable, Protocol

from backend.app.domain.contact import Contact
from backend.app.domain.notification import NotificationRequest
from backend.app.domain.providers import ChannelDelivery, ChannelStatus


class SupportsSmtpClient(Protocol):
    def sendmail(self, from_address: str, to_addresses: list[str], message: str) -> None:
        ...

    def quit(self) -> None:
        ...


ClientFactory = Callable[[str, int, float], SupportsSmtpClient]


def _default_client_factory(host: str, port: int, timeout: float) -> SupportsSmtpClient:
    return smtplib.SMTP(host=host, port=port, timeout=timeout)


class SmtpEmailProvider:
    def __init__(
        self,
        host: str,
        from_address: str,
        port: int = 25,
        timeout: float = 10.0,
        client_factory: ClientFactory | None = None,
    ) -> None:
        self._host = host.strip()
        self._from_address = from_address.strip()
        self._port = port
        self._timeout = timeout
        self._client_factory = client_factory or _default_client_factory

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        if not contact.email:
            return ChannelDelivery(status="unavailable")

        email = EmailMessage()
        email["Subject"] = "Notificación de visita"
        email["From"] = self._from_address
        email["To"] = contact.email
        email.set_content(message)

        client = self._client_factory(self._host, self._port, self._timeout)
        try:
            client.sendmail(self._from_address, [contact.email], email.as_string())
        finally:
            client.quit()

        return ChannelDelivery(status="sent")


class StubEmailProvider:
    def __init__(self, status: ChannelStatus = "accepted") -> None:
        self._status = status
        self.sent_messages: list[tuple[str, str, str]] = []

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        self.sent_messages.append((contact.id, request.device_id, message))
        return ChannelDelivery(status=self._status)


FakeEmailProvider = StubEmailProvider
