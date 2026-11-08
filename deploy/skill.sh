#!/usr/bin/env bash
# Creates or updates the Alexa skill with the ASK CLI and connects it to the Lambda.
# Needs: `ask configure` done once, and deploy/deploy.sh run once (so the Lambda alias exists).
#
# Usage (from the battleship-alexa-smart-mode folder):
#   ./deploy/skill.sh
set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"
NAME="battleship-alexa"
PKG="deploy/skill-package"

ALIAS_ARN=$(aws --region "$REGION" lambda get-alias --function-name "$NAME" --name live --query AliasArn --output text)
echo "Lambda endpoint: ${ALIAS_ARN}"

# ---------- Build the skill package from alexa-skill/*.json ----------
rm -rf "$PKG/interactionModels"
mkdir -p "$PKG/interactionModels/custom"
for locale in es-US es-MX es-ES; do
  cp alexa-skill/Battleship_es.json "$PKG/interactionModels/custom/${locale}.json"
done
cp alexa-skill/Battleship_en.json "$PKG/interactionModels/custom/en-US.json"

python3 - "$ALIAS_ARN" "$PKG/skill.json" <<'EOF'
import json, sys
arn, out = sys.argv[1], sys.argv[2]
es = {
    "name": "Batalla Naval",
    "summary": "Juego de batalla naval por voz con un robot LEGO EV3.",
    "description": "Juega batalla naval contra Alexa. Un robot LEGO EV3 marca tus disparos en el tablero.",
    "examplePhrases": ["Alexa, abre batalla naval", "comenzar juego", "modo de juego avanzado"],
    "keywords": ["batalla naval", "lego", "ev3"],
}
en = {
    "name": "Robot Battleship",
    "summary": "Voice battleship game with a LEGO EV3 robot.",
    "description": "Play battleship against Alexa. A LEGO EV3 robot marks your shots on the board.",
    "examplePhrases": ["Alexa, open robot battleship", "start game", "game mode advanced"],
    "keywords": ["battleship", "lego", "ev3"],
}
manifest = {"manifest": {
    "manifestVersion": "1.0",
    "publishingInformation": {
        "locales": {"es-US": es, "es-MX": es, "es-ES": es, "en-US": en},
        "isAvailableWorldwide": True,
        "testingInstructions": "Development only.",
        "category": "GAMES",
        "distributionCountries": [],
    },
    "privacyAndCompliance": {
        "allowsPurchases": False, "usesPersonalInfo": False, "isChildDirected": False,
        "isExportCompliant": True, "containsAds": False,
        "locales": {l: {} for l in ["es-US", "es-MX", "es-ES", "en-US"]},
    },
    "apis": {"custom": {"endpoint": {"uri": arn}}},
}}
json.dump(manifest, open(out, "w"), ensure_ascii=False, indent=2)
EOF

cat > deploy/ask-resources.json <<EOF
{
  "askcliResourcesVersion": "2020-03-31",
  "profiles": {
    "default": {
      "skillMetadata": { "src": "./skill-package" }
    }
  }
}
EOF

# Reuse an existing skill (e.g. the 2020 one) instead of creating a new one
EXISTING_SKILL_ID="${EXISTING_SKILL_ID:-amzn1.ask.skill.fe055462-2886-4526-9e85-44dd8cb8c5d4}"
if [ ! -f deploy/.ask/ask-states.json ] && [ -n "$EXISTING_SKILL_ID" ]; then
  mkdir -p deploy/.ask
  printf '{"askcliStatesVersion":"2020-03-31","profiles":{"default":{"skillId":"%s"}}}\n' "$EXISTING_SKILL_ID" \
    > deploy/.ask/ask-states.json
fi

# ---------- Deploy with the ASK CLI (skill metadata only; the Lambda is managed by deploy.sh) ----------
(cd deploy && ask deploy --target skill-metadata)

SKILL_ID=$(python3 -c "import json;print(json.load(open('deploy/.ask/ask-states.json'))['profiles']['default']['skillId'])")
echo "Skill ID: ${SKILL_ID}"

# Enable the skill for testing on your account (Echo + simulator)
ask smapi set-skill-enablement --skill-id "$SKILL_ID" --stage development >/dev/null 2>&1 || true

# Lock the Lambda to this skill
SKILL_ID="$SKILL_ID" ./deploy/deploy.sh

echo
echo "Skill ready. Try: ask dialog --skill-id ${SKILL_ID} --locale es-US"
