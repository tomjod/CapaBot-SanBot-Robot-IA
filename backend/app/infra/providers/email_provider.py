from __future__ import annotations

import smtplib
from email.message import EmailMessage
from typing import Callable, Mapping, Protocol

from mailtrap import Address, Mail, MailtrapClient

from backend.app.domain.contact import Contact
from backend.app.domain.notification import NotificationRequest
from backend.app.domain.providers import ChannelDelivery, ChannelStatus


class SupportsSmtpClient(Protocol):
    def sendmail(self, from_address: str, to_addresses: list[str], message: str) -> None:
        ...

    def quit(self) -> None:
        ...


ClientFactory = Callable[[str, int, float], SupportsSmtpClient]
MailtrapClientFactory = Callable[[str], "SupportsMailtrapClient"]


def _default_client_factory(host: str, port: int, timeout: float) -> SupportsSmtpClient:
    return smtplib.SMTP(host=host, port=port, timeout=timeout)


class SupportsMailtrapClient(Protocol):
    def send(self, mail: Mail) -> Mapping[str, object]:
        ...


def _default_mailtrap_client_factory(token: str) -> SupportsMailtrapClient:
    return MailtrapClient(token=token)


class MailtrapEmailProvider:
    def __init__(
        self,
        token: str,
        from_address: str,
        from_name: str | None = None,
        client_factory: MailtrapClientFactory | None = None,
    ) -> None:
        self._token = token.strip()
        self._from_address = from_address.strip()
        self._from_name = from_name.strip() if from_name and from_name.strip() else None
        self._client_factory = client_factory or _default_mailtrap_client_factory

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        if not contact.email:
            return ChannelDelivery(status="unavailable")

        sender = Address(email=self._from_address, name=self._from_name)
        email = Mail(
            sender=sender,
            to=[Address(email=contact.email, name=contact.display_name)],
            subject="Notificación de visita",
            text=message,
        )

        try:
            response = self._client_factory(self._token).send(email)
        except TimeoutError:
            raise
        except Exception as exc:
            return ChannelDelivery(status="failed", detail=str(exc))

        if response.get("success") is False:
            detail = response.get("errors")
            return ChannelDelivery(status="failed", detail=str(detail) if detail else None)

        return ChannelDelivery(status="sent")


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
