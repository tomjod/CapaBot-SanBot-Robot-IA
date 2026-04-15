from backend.app.bot.service import TelegramBotFlowService

__all__ = ["create_telegram_onboarding_application", "TelegramBotFlowService"]


def __getattr__(name: str):
    if name == "create_telegram_onboarding_application":
        from backend.app.bot.application import create_telegram_onboarding_application

        return create_telegram_onboarding_application
    raise AttributeError(name)
