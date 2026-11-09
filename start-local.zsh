#!/bin/zsh
set -eu
cd "${0:A:h}"
: "${JDBC_DATABASE_URL:?Bitte die JDBC-Adresse der Entwicklungsdatenbank setzen}"
: "${JDBC_DATABASE_USERNAME:?Bitte den Entwicklungsbenutzer setzen}"
if [[ -z "${JDBC_DATABASE_PASSWORD:-}" ]]; then
  read -rs 'JDBC_DATABASE_PASSWORD?Passwort der Entwicklungsdatenbank: '
  print
fi
export JDBC_DATABASE_PASSWORD
exec ./gradlew bootRun
