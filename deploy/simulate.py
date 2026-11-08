#!/usr/bin/env python3
"""Plays phrases against the real skill using the Alexa simulator (ask smapi simulate-skill).

Usage:
  python3 deploy/simulate.py es-US "abre batalla naval" "comenzar juego" "modo de juego avanzado" "C cinco"
"""
import json
import re
import subprocess
import sys
import time

SKILL_ID = "amzn1.ask.skill.fe055462-2886-4526-9e85-44dd8cb8c5d4"


def ask(*args):
    out = subprocess.run(["ask", "smapi", *args], capture_output=True, text=True)
    if out.returncode != 0:
        raise SystemExit(out.stderr or out.stdout)
    return json.loads(out.stdout)


def say(locale, text, new_session):
    started = ask("simulate-skill", "--skill-id", SKILL_ID, "--input-content", text,
                  "--device-locale", locale,
                  "--session-mode", "FORCE_NEW_SESSION" if new_session else "DEFAULT")
    sim_id = started["id"]
    for _ in range(60):
        time.sleep(1)
        result = ask("get-skill-simulation", "--skill-id", SKILL_ID, "--simulation-id", sim_id)
        if result.get("status") != "IN_PROGRESS":
            return result
    raise SystemExit("simulation timed out")


def main():
    locale, phrases = sys.argv[1], sys.argv[2:]
    for i, phrase in enumerate(phrases):
        result = say(locale, phrase, new_session=(i == 0))
        info = result.get("result", {}).get("skillExecutionInfo") or {}
        request = (info.get("invocations") or [{}])[0].get("invocationRequest", {}).get("body", {}).get("request", {})
        response = (info.get("invocations") or [{}])[0].get("invocationResponse", {}).get("body", {}).get("response", {})
        intent = request.get("intent", {})
        slots = {k: v.get("value") for k, v in (intent.get("slots") or {}).items() if v.get("value")}
        speech = response.get("outputSpeech", {}).get("ssml", "")
        speech = re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", speech)).strip()
        error = result.get("result", {}).get("error", {}).get("message")
        print(f"\n> {phrase}")
        print(f"  [{request.get('type', '-')} {intent.get('name', '')} {slots if slots else ''}]")
        print(f"  Alexa: {speech or error or '(sin respuesta)'}")


if __name__ == "__main__":
    main()
