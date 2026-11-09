#!/usr/bin/env bash
# Despliega en AWS el backend de Batalla Naval:
#   - Rol IAM de la Lambda (logs y permiso para publicar en el tópico MQTT del robot)
#   - Función Lambda (Java 21, SnapStart) con el alias "live": su ARN es el endpoint de la skill
#   - Thing de AWS IoT, política y certificados del robot EV3 (se guardan en deploy/ev3-certs/)
#
# Uso (desde la carpeta del proyecto):
#   mvn clean package
#   ./deploy/deploy.sh                                  # primera vez, antes de crear la skill
#   SKILL_ID=amzn1.ask.skill.xxxx ./deploy/deploy.sh    # con la skill creada: la Lambda solo le responde a ella
#
# Se puede ejecutar varias veces: actualiza lo que ya existe.
set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"   # us-east-1 = región de Alexa para América
NAME="battleship-alexa"
ROLE="${NAME}-role"
TOPIC="battleship/ev3"
THING="battleship-ev3"
JAR="target/alexa-batlla-naval-2.0.0.jar"
HANDLER="jose.coronel.cl.battleship.BatallaNavalAlexaSkill::handleRequest"
CERTS="deploy/ev3-certs"
SKILL_ID="${SKILL_ID:-}"

aws_() { aws --region "$REGION" "$@"; }

[ -f "$JAR" ] || { echo "Falta $JAR. Ejecuta: mvn clean package"; exit 1; }

ACCOUNT=$(aws_ sts get-caller-identity --query Account --output text)
IOT_ENDPOINT=$(aws_ iot describe-endpoint --endpoint-type iot:Data-ATS --query endpointAddress --output text)
TOPIC_ARN="arn:aws:iot:${REGION}:${ACCOUNT}:topic/${TOPIC}"
echo "Cuenta ${ACCOUNT}, región ${REGION}, endpoint IoT ${IOT_ENDPOINT}"

# ---------- Rol IAM de la Lambda ----------
if ! aws_ iam get-role --role-name "$ROLE" >/dev/null 2>&1; then
  echo "Creando el rol IAM ${ROLE}"
  aws_ iam create-role --role-name "$ROLE" --assume-role-policy-document '{
    "Version":"2012-10-17",
    "Statement":[{"Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":"sts:AssumeRole"}]}' >/dev/null
  aws_ iam attach-role-policy --role-name "$ROLE" \
    --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
  sleep 10   # IAM tarda unos segundos antes de que Lambda pueda usar un rol nuevo
fi
aws_ iam put-role-policy --role-name "$ROLE" --policy-name publish-to-robot --policy-document "{
  \"Version\":\"2012-10-17\",
  \"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"iot:Publish\",\"Resource\":\"${TOPIC_ARN}\"}]}"
ROLE_ARN=$(aws_ iam get-role --role-name "$ROLE" --query Role.Arn --output text)

# ---------- Lambda ----------
ENV="Variables={IOT_ENDPOINT=${IOT_ENDPOINT},ROBOT_TOPIC=${TOPIC}"
[ -n "$SKILL_ID" ] && ENV="${ENV},SKILL_ID=${SKILL_ID}"
ENV="${ENV}}"

if aws_ lambda get-function --function-name "$NAME" >/dev/null 2>&1; then
  echo "Actualizando la Lambda ${NAME}"
  aws_ lambda update-function-code --function-name "$NAME" --zip-file "fileb://${JAR}" >/dev/null
  aws_ lambda wait function-updated-v2 --function-name "$NAME"
  aws_ lambda update-function-configuration --function-name "$NAME" --environment "$ENV" >/dev/null
  aws_ lambda wait function-updated-v2 --function-name "$NAME"
else
  echo "Creando la Lambda ${NAME}"
  aws_ lambda create-function --function-name "$NAME" \
    --runtime java21 --architectures arm64 --handler "$HANDLER" \
    --role "$ROLE_ARN" --zip-file "fileb://${JAR}" \
    --memory-size 1024 --timeout 10 \
    --snap-start ApplyOn=PublishedVersions \
    --environment "$ENV" >/dev/null
  aws_ lambda wait function-active-v2 --function-name "$NAME"
fi

echo "Publicando la versión (SnapStart toma la foto de memoria, tarda cerca de un minuto)"
VERSION=$(aws_ lambda publish-version --function-name "$NAME" --query Version --output text)
aws_ lambda wait published-version-active --function-name "$NAME" --qualifier "$VERSION"
if aws_ lambda get-alias --function-name "$NAME" --name live >/dev/null 2>&1; then
  aws_ lambda update-alias --function-name "$NAME" --name live --function-version "$VERSION" >/dev/null
else
  aws_ lambda create-alias --function-name "$NAME" --name live --function-version "$VERSION" >/dev/null
fi
ALIAS_ARN=$(aws_ lambda get-alias --function-name "$NAME" --name live --query AliasArn --output text)

# Disparador de Alexa: solo nuestra skill puede invocar la Lambda
aws_ lambda remove-permission --function-name "$NAME" --qualifier live --statement-id alexa-skill >/dev/null 2>&1 || true
if [ -n "$SKILL_ID" ]; then
  aws_ lambda add-permission --function-name "$NAME" --qualifier live --statement-id alexa-skill \
    --action lambda:InvokeFunction --principal alexa-appkit.amazon.com \
    --event-source-token "$SKILL_ID" >/dev/null
  echo "Disparador de Alexa agregado para la skill ${SKILL_ID}"
else
  # Temporal: Alexa lo necesita para aceptar el endpoint. Vuelve a ejecutar con SKILL_ID para limitarlo.
  aws_ lambda add-permission --function-name "$NAME" --qualifier live --statement-id alexa-skill \
    --action lambda:InvokeFunction --principal alexa-appkit.amazon.com >/dev/null
  echo "Disparador de Alexa temporal (cualquier skill). Vuelve a ejecutar con SKILL_ID para limitarlo."
fi

# ---------- AWS IoT: el robot EV3 ----------
aws_ iot describe-thing --thing-name "$THING" >/dev/null 2>&1 || aws_ iot create-thing --thing-name "$THING" >/dev/null
POLICY_DOC="{
  \"Version\":\"2012-10-17\",
  \"Statement\":[
    {\"Effect\":\"Allow\",\"Action\":\"iot:Connect\",\"Resource\":\"arn:aws:iot:${REGION}:${ACCOUNT}:client/${THING}\"},
    {\"Effect\":\"Allow\",\"Action\":\"iot:Subscribe\",\"Resource\":\"arn:aws:iot:${REGION}:${ACCOUNT}:topicfilter/${TOPIC}\"},
    {\"Effect\":\"Allow\",\"Action\":\"iot:Receive\",\"Resource\":\"${TOPIC_ARN}\"}]}"
aws_ iot get-policy --policy-name "${THING}-policy" >/dev/null 2>&1 || \
  aws_ iot create-policy --policy-name "${THING}-policy" --policy-document "$POLICY_DOC" >/dev/null

if [ ! -f "${CERTS}/certificate.pem.crt" ]; then
  echo "Creando los certificados del EV3 en ${CERTS}/"
  mkdir -p "$CERTS"
  CERT_ARN=$(aws_ iot create-keys-and-certificate --set-as-active \
    --certificate-pem-outfile "${CERTS}/certificate.pem.crt" \
    --private-key-outfile "${CERTS}/private.pem.key" \
    --query certificateArn --output text)
  chmod 600 "${CERTS}/private.pem.key"
  aws_ iot attach-policy --policy-name "${THING}-policy" --target "$CERT_ARN"
  aws_ iot attach-thing-principal --thing-name "$THING" --principal "$CERT_ARN"
  curl -sf -o "${CERTS}/AmazonRootCA1.pem" https://www.amazontrust.com/repository/AmazonRootCA1.pem
fi
printf 'IOT_ENDPOINT=%s\nTOPIC=%s\nCLIENT_ID=%s\n' "$IOT_ENDPOINT" "$TOPIC" "$THING" > "${CERTS}/robot.env"

echo
echo "Listo."
echo "  Endpoint de la skill (Default Region): ${ALIAS_ARN}"
echo "  Archivos del EV3:                       ${CERTS}/ (install.sh los copia al robot)"
[ -z "$SKILL_ID" ] && echo "  Siguiente paso: ./deploy/skill.sh (crea la skill y vuelve a ejecutar este script con su ID)"
exit 0
