#!/usr/bin/env bash
# Inicia el simulador web de Batalla Naval en http://localhost:8080
#
# Uso (desde la carpeta del proyecto):
#   ./deploy/web.sh
#
# Para jugar contra la Lambda en AWS o conectar el robot virtual a AWS IoT, primero inicia
# sesión con "aws login" y ejecuta deploy/deploy.sh (crea la Lambda y los certificados del robot).
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Compilando la skill (la usa el simulador como librería)..."
mvn -q -B install -DskipTests

echo "Iniciando el simulador en http://localhost:8080 (Ctrl+C para detenerlo)"
mvn -q -B -f simulador/pom.xml spring-boot:run
