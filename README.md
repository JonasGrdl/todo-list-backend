# Studienplaner · Backend

Spring Boot verarbeitet die gemeinsame Aufgabenliste. PostgreSQL speichert eine Zeile pro Aufgabe. Das Frontend-Repository heißt To-Do-Listen-FrontEnd. Es enthält die Bewertungsmatrix, den Demoablauf und die Screenshots.

## Lokal starten

Benötigt werden Java 25, der enthaltene Gradle-Wrapper und eine eigene PostgreSQL-Entwicklungsdatenbank. Eine kurzlebige Testinstanz lässt sich mit Docker starten:

    docker compose -f compose.test.yml up -d

Sie ist nur auf dem Loopback-Port 55439 erreichbar. Ihre Daten liegen im Arbeitsspeicher und verschwinden beim Stoppen. Für dauerhafte Entwicklungsdaten eine separate Datenbank verwenden.

In IntelliJ die Startkonfiguration für DemoApplication öffnen und die folgenden Variablen nur dort eintragen. Die Konfiguration lokal belassen. Für die Compose-Instanz:

| Variable | Lokaler Wert |
|---|---|
| JDBC_DATABASE_URL | jdbc:postgresql://127.0.0.1:55439/postgres |
| JDBC_DATABASE_USERNAME | studium |
| JDBC_DATABASE_PASSWORD | leer für die kurzlebige Testinstanz |
| FRONTEND_URL | http://localhost:5173 |
| PORT | 8080 |

Die Werte können alternativ nur für einen Prozess gelten:

    JDBC_DATABASE_URL=jdbc:postgresql://127.0.0.1:55439/postgres JDBC_DATABASE_USERNAME=studium JDBC_DATABASE_PASSWORD='' ./gradlew bootRun

## Schnittstelle

| Methode | Pfad | Antwort |
|---|---|---|
| GET | /todos | 200, Liste aller Aufgaben |
| POST | /todos | 201, gespeicherte Aufgabe |
| PUT | /todos/{id} | 200, ersetzt alle bearbeitbaren Felder |
| DELETE | /todos/{id} | 204, kein Antworttext |

Beispiel für POST und PUT:

    {"name":"Lesetext annotieren","description":"Kapitel 2","status":"offen","priority":"mittel","dueDate":"2026-11-12"}

Die Antwort enthält zusätzlich id, done, createdAt und updatedAt. name bleibt aus Kompatibilitätsgründen der JSON-Schlüssel für den Titel. Frühere Clients können done als Boolean senden. Wenn beide Statusangaben vorhanden sind, müssen sie übereinstimmen. Gültige Statuswerte sind offen und erledigt, gültige Prioritäten niedrig, mittel und hoch. Ohne Angaben gelten offen und mittel. Ein fehlender Termin oder null bedeutet keine Fälligkeit. Das Datum enthält keine Uhrzeit. Titel werden getrimmt und auf 1–120 Zeichen geprüft. Beschreibungen dürfen 0–2000 Zeichen enthalten.

Bei ungültigen Feldern oder IDs antwortet die API mit 400, bei fehlenden Aufgaben mit 404. Beide Antworten enthalten ein JSON-Objekt mit message. ID und Zeitstempel schreibt ausschließlich das Backend. Statuswechsel nutzen PUT mit allen aktuellen Feldern. Gleichzeitige Bearbeitungen derselben Aufgabe verwenden den zuletzt gespeicherten Stand.

## Architektur und Schema

TodoController übersetzt HTTP in Aufrufe des TaskService. TaskInput prüft den Eingabevertrag. Der Service speichert über TaskRepository in einer Transaktion. TaskView bildet die persistierten Daten auf die JSON-Antwort ab. Task ist die JPA-Entity.

schema.sql ergänzt die vorhandene Tabelle, erweitert die Beschreibung, vergibt Bestandsaufgaben die Priorität mittel und behält IDs, Namen und Status. Für historische Aufgaben ohne bekannte Zeitstempel gilt der Migrationszeitpunkt. Neue Schreibvorgänge unterliegen Titel- und Prioritätsprüfungen. Hibernate validiert das Ergebnis. SchemaTests führt die Migration zweimal auf einem alten Tabellenstand aus und prüft die erhaltene Zeile.

## Tests

    docker compose -f compose.test.yml up -d
    ./gradlew build
    docker compose -f compose.test.yml down

Tests greifen nur auf Loopback zu. Der Test-Port ist mit TEST_PG_PORT, Datenbank und Anmeldung mit TEST_PG_DB, TEST_PG_USER und TEST_PG_PASSWORD konfigurierbar. Die HTTP-Integrationstests starten den echten Server auf einem freien Port. Die GitHub Action startet PostgreSQL 17 und führt denselben Build aus.

## Render

Den vorhandenen Docker-Web-Service und die bestehende PostgreSQL-Datenbank verwenden. Das Dockerfile baut mit ./gradlew bootJar und startet das JAR mit java -jar. Im Backend-Service JDBC_DATABASE_URL als jdbc:postgresql://<host>:5432/<datenbank>, JDBC_DATABASE_USERNAME und JDBC_DATABASE_PASSWORD getrennt eintragen. FRONTEND_URL ist die öffentliche HTTPS-Adresse des Frontends ohne abschließenden Schrägstrich. Der Server berücksichtigt PORT. Die Datenbankrolle benötigt Rechte zur Schemaaktualisierung. Vor dem ersten Update auf Bestandsdaten eine Sicherung vorsehen.

Nach Freigabe in beiden bestehenden Services den gewünschten Branch und Auto-Deploy „After CI Checks Pass“ wählen. Zuerst den Backend-Stand verfügbar machen, dann das Frontend gegen diesen Stand prüfen. Nach dem Deployment /todos und den Demoablauf im Frontend prüfen. Das Hosting für diese Version wurde vorbereitet, aber nicht veröffentlicht. [Render-Dokumentation](https://render.com/docs/deploys)
