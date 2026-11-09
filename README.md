# Studienaufgaben – Backend

Spring-Boot-Backend der Aufgabenverwaltung für Studierende (WebTech-Kurs).
Gesamtdokumentation, Architektur und Bewertungsmatrix liegen im Projektordner (`README.md`, `docs/`).

## Start

Voraussetzungen: JDK 25, laufende PostgreSQL-Instanz.

```bash
export JDBC_DATABASE_URL=jdbc:postgresql://localhost:5432/tasklist
export JDBC_DATABASE_USERNAME=tasklist
export JDBC_DATABASE_PASSWORD=<lokales-passwort>
./gradlew bootRun
```

Alle Variablen mit Platzhaltern: `.env.example`. Demo-Daten (nur bei leerer Tabelle):
`SPRING_PROFILES_ACTIVE=demo`.

## API (Kurzfassung)

| Methode | Pfad                 | Zweck                        |
|---------|----------------------|------------------------------|
| GET     | `/todos`             | alle Aufgaben, neueste zuerst |
| GET     | `/todos/{id}`        | eine Aufgabe                 |
| POST    | `/todos`             | erstellen (201 + Location)   |
| PUT     | `/todos/{id}`        | bearbeiten                   |
| PATCH   | `/todos/{id}/status` | `{"status":"OPEN"\|"DONE"}`  |
| DELETE  | `/todos/{id}`        | löschen (204)                |
| GET     | `/actuator/health`   | Health-Check                 |

Fehler im Format RFC 9457 (Problem Details) mit deutschen Texten und Feldfehlern unter `errors`.

## Tests

```bash
./gradlew test
```

Benötigt Docker: Testcontainers startet eine isolierte PostgreSQL-Instanz. Enthalten sind
API-Integrationstests (CRUD, Status, Validierung, Grenzen, 400/404, CORS, Zeitstempel) und ein
Migrationstest, der eine Datenbank der Vorversion übernimmt.

## Schema

Flyway-Migrationen unter `src/main/resources/db/migration`. Bestehende Datenbanken aus der
Vorversion (ohne Flyway-Historie) werden auf Version 0 gesetzt und datenerhaltend migriert.
Hibernate läuft mit `ddl-auto=validate`.

## Deployment

Docker-Image (`Dockerfile`) auf Render, Health Check `/actuator/health`. Umgebungsvariablen:
`JDBC_DATABASE_URL`, `JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD`, `FRONTEND_URL`.
CI: `.github/workflows/ci.yml`.
