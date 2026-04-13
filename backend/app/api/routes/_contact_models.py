from __future__ import annotations

from typing import Any

from pydantic import AliasChoices, BaseModel, Field, field_validator


class ContactCreate(BaseModel):
    id: str
    display_name: str
    job_title: str = Field(validation_alias=AliasChoices("job_title", "cargo"))
    company: str = Field(validation_alias=AliasChoices("company", "empresa"))
    enabled: bool = True
    phone: str = Field(validation_alias=AliasChoices("phone", "telefono"))
    telegram_chat_id: str | None = None
    telegram_user_id: str | None = None
    telegram_username: str | None = None
    email: str | None = None
    email_enabled: bool = False

    @field_validator("id", "display_name", "job_title", "company", "phone", mode="before")
    @classmethod
    def strip_string(cls, v: Any) -> Any:
        if isinstance(v, str):
            return v.strip() or None
        return v


class ContactUpdate(BaseModel):
    display_name: str
    job_title: str = Field(validation_alias=AliasChoices("job_title", "cargo"))
    company: str = Field(validation_alias=AliasChoices("company", "empresa"))
    enabled: bool = True
    phone: str = Field(validation_alias=AliasChoices("phone", "telefono"))
    telegram_chat_id: str | None = None
    telegram_user_id: str | None = None
    telegram_username: str | None = None
    email: str | None = None
    email_enabled: bool = False

    @field_validator("display_name", "job_title", "company", "phone", mode="before")
    @classmethod
    def strip_display_name(cls, v: Any) -> Any:
        if isinstance(v, str):
            return v.strip() or None
        return v
