from __future__ import annotations


def normalize_phone_number(phone_number: str | None) -> str | None:
    if phone_number is None:
        return None

    digits = "".join(char for char in phone_number if char.isdigit())
    if not digits:
        return None

    if digits.startswith("00"):
        digits = digits[2:]

    return digits or None
