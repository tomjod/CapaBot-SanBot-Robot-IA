from __future__ import annotations

import asyncio
import json
from threading import Thread
from typing import Any, Callable, Protocol
from urllib import request as urllib_request

from backend.app.domain.contact import Contact
from backend.app.domain.notification import NotificationRequest
from backend.app.domain.providers import ChannelDelivery, ChannelStatus

Transport = Callable[[str, bytes, dict[str, str]], int]
BotFactory = Callable[..., "SupportsTelegramBot"]


class SupportsTelegramBot(Protocol):
    async def send_message(self, chat_id: int | str, text: str, **kwargs: Any) -> Any:
        ...


def _default_transport(url: str, data: bytes, headers: dict[str, str]) -> int:
    http_request = urllib_request.Request(url=url, data=data, headers=headers, method="POST")
    timeout = float(headers.pop("X-Timeout-Seconds", "10"))
    with urllib_request.urlopen(http_request, timeout=timeout) as response:
        return response.getcode()


def _normalize_bot_base_url(api_base_url: str) -> str:
    normalized = api_base_url.rstrip("/")
    if normalized.endswith("/bot"):
        return normalized
    return f"{normalized}/bot"


def _run_async(coroutine: Any) -> Any:
    try:
        asyncio.get_running_loop()
    except RuntimeError:
        return asyncio.run(coroutine)

    state: dict[str, Any] = {}

    def _runner() -> None:
        loop = asyncio.new_event_loop()
        try:
            state["result"] = loop.run_until_complete(coroutine)
        except BaseException as error:  # pragma: no cover
            state["error"] = error
        finally:
            loop.close()

    thread = Thread(target=_runner, daemon=True)
    thread.start()
    thread.join()

    error = state.get("error")
    if error is not None:
        raise error
    return state.get("result")


class TelegramBotProvider:
    def __init__(
        self,
        bot_token: str,
        transport: Transport | None = None,
        api_base_url: str = "https://api.telegram.org",
        timeout_seconds: float = 10.0,
        bot_factory: BotFactory | None = None,
        bot: SupportsTelegramBot | None = None,
    ) -> None:
        self._bot_token = bot_token.strip()
        self._transport = transport or _default_transport
        self._api_base_url = api_base_url.rstrip("/")
        self._timeout_seconds = timeout_seconds
        self._use_library_client = bot is not None or transport is None
        self._bot_factory = bot_factory or _default_bot_factory
        self._bot = bot

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        if not contact.telegram_chat_id:
            return ChannelDelivery(status="unavailable")

        if self._use_library_client:
            try:
                return self._send_with_telegram_bot(contact.telegram_chat_id, message)
            except TimeoutError:
                raise
            except Exception as error:
                return ChannelDelivery(status="failed", detail=str(error))

        payload = json.dumps({"chat_id": contact.telegram_chat_id, "text": message}).encode("utf-8")
        status_code = self._transport(
            f"{self._api_base_url}/bot{self._bot_token}/sendMessage",
            payload,
            {"Content-Type": "application/json", "X-Timeout-Seconds": str(self._timeout_seconds)},
        )
        if 200 <= status_code < 300:
            return ChannelDelivery(status="sent")
        return ChannelDelivery(status="failed")

    def _send_with_telegram_bot(self, chat_id: str, message: str) -> ChannelDelivery:
        bot = self._bot or self._bot_factory(
            token=self._bot_token,
            base_url=_normalize_bot_base_url(self._api_base_url),
        )
        _run_async(
            bot.send_message(
                chat_id=chat_id,
                text=message,
                read_timeout=self._timeout_seconds,
                write_timeout=self._timeout_seconds,
                connect_timeout=self._timeout_seconds,
                pool_timeout=self._timeout_seconds,
            )
        )
        return ChannelDelivery(status="sent")


class StubTelegramProvider:
    def __init__(self, status: ChannelStatus = "accepted") -> None:
        self._status = status
        self.sent_messages: list[tuple[str, str, str]] = []

    def send(self, contact: Contact, message: str, request: NotificationRequest) -> ChannelDelivery:
        self.sent_messages.append((contact.id, request.device_id, message))
        return ChannelDelivery(status=self._status)


FakeTelegramProvider = StubTelegramProvider


def _default_bot_factory(**kwargs: Any) -> SupportsTelegramBot:
    from telegram import Bot

    return Bot(**kwargs)
