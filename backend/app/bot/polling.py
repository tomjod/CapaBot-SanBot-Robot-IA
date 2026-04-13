from __future__ import annotations

from backend.app.bot.application import create_telegram_onboarding_application
from backend.app.runtime import build_runtime


def run() -> None:
    application = create_telegram_onboarding_application(build_runtime())
    application.run_polling(drop_pending_updates=False)


if __name__ == "__main__":
    run()
