# ToDoList · Backend

Spring Boot 4.1.1, Java 25 und PostgreSQL. Die Vue-Anwendung verwendet die bestehende Route `/todos`. Jede Abfrage, auch für einzelne Aufgaben, verwendet die interne UUID aus der bestätigten Sitzung. Hibernate ergänzt die Kontotabellen und den optionalen Fremdschlüssel `task.owner_id`. Bestehende Aufgaben ohne Besitzer bleiben erhalten und sind über die API unsichtbar.

## Lokal starten

Eine eigene PostgreSQL-Entwicklungsdatenbank und einen Benutzer mit Rechten für das Anwendungsschema anlegen. Im Backend-Verzeichnis die lokale Run-Konfiguration in IntelliJ oder die Umgebung des Terminalprozesses setzen. Beispielwerte ersetzen.

| Variable | Beispiel oder Bedeutung |
|---|---|
| JDBC_DATABASE_URL | jdbc:postgresql://localhost:5432/todolist |
| JDBC_DATABASE_USERNAME | todo_app |
| JDBC_DATABASE_PASSWORD | Platzhalter für das Datenbankpasswort |
| SPRING_PROFILES_ACTIVE | dev für lokale Entwicklung über HTTP. Im Produktivbetrieb weglassen. |
| FRONTEND_URL | Exakte öffentliche Frontend-Origin ohne abschließenden Slash. Lokal im dev-Profil standardmäßig http://localhost:5173 |
| RESEND_FROM | ToDoList <konto@mail.example.org> (Adresse auf der verifizierten Domain) |
| RESEND_API_KEY | Bereits als Backend-Secret hinterlegt. Für lokalen echten Versand im Backend-Prozess bereitstellen. |
| PORT | Optional, standardmäßig 8080 |
| VITE_BACKEND_BASE_URL | Nur im Frontend. Lokales Beispiel http://localhost:8080, im Deployment öffentliche HTTPS-API-Origin oder gleicher Origin mittels Reverse Proxy. |

Mit Java 25 im Backend-Verzeichnis starten:

```sh
./gradlew bootRun
```

Im Frontend-Verzeichnis mit Node 26 `npm ci` und `npm run dev` ausführen. Im Browser http://localhost:5173 öffnen, registrieren, die Bestätigungsmail öffnen, den Button betätigen und sich anmelden.

## Resend einrichten

1. In Resend unter Domains eine eigene Absenderdomain oder Subdomain wie `mail.example.org` hinzufügen.
2. Beim DNS-Anbieter die von Resend für die gewählte Region angezeigten DKIM- und SPF-Einträge einschließlich des angezeigten MX-Eintrags für den Return-Path exakt übernehmen. Die DNS-Werte aus dem eigenen Dashboard verwenden.
3. In Resend die Verifizierung starten und auf den Status „Verified“ warten. Für diese Transaktionsmails Klicktracking deaktiviert lassen, damit die Bestätigungslinks direkt auf die Anwendung führen.
4. Eine Adresse dieser Domain als `RESEND_FROM` setzen. Die Versandberechtigung des vorhandenen Backend-Schlüssels muss diese Domain einschließen. Ausgehendes HTTPS zu api.resend.com erlauben.

Der Provider erhält die Zieladresse und den Mailinhalt. Das Backend erzeugt 32 zufällige Bytes und setzt den URL-sicheren Token in das Fragment des Links zur konfigurierten Frontend-Origin. Beim Öffnen entfernt das Frontend das Fragment aus der Adressleiste. Erst der Button sendet den Token im POST-Body. Die Seite lädt nur eigene Ressourcen und setzt die Referrer-Policy auf no-referrer. Die Bestätigung führt zur Anmeldung.

Bei fehlendem Schlüssel, fehlendem Absender, Timeout oder abgelehntem Versand meldet die API HTTP 503. Das Konto besteht weiter. Ein neuer Versuch ist nach 60 Sekunden möglich. Jeder neue Versandversuch widerruft vorherige Links. Auch ein erfolgreicher API-Versand bedeutet, dass der Anbieter die Mail angenommen hat. Die Zustellung ins Postfach muss im Deployment geprüft werden.

[Resend-Domainverifizierung](https://resend.com/docs/dashboard/domains/introduction) · [Resend-Versand-API](https://resend.com/docs/api-reference/emails/send-email)

## Schnittstellen und Sitzung

| Methode und Pfad | Verhalten |
|---|---|
| GET /auth/csrf | Gibt den CSRF-Wert der aktuellen Sitzung zurück. Schreibanfragen senden ihn als X-CSRF-TOKEN mit dem Sitzungscookie. |
| POST /auth/register | E-Mail, displayName und password. Generische Annahme auch bei wiederholter Registrierung. Bestehende Zugangsdaten bleiben erhalten. |
| POST /auth/resend | E-Mail im Feld email. Frühestens nach 60 Sekunden und maximal fünf Versandversuche je Konto und Stunde. Maximal 20 Registrierungs-/Versandanfragen je IP und Stunde. |
| POST /auth/verify | Bestätigt einen einmaligen, 24 Stunden gültigen Token. Konsum und Änderung des Kontos liegen in einer Transaktion mit Kontosperre. |
| POST /auth/login | email und password. Bei korrektem Passwort entweder pending oder neue Sitzung mit authenticated. |
| GET /auth/session | Aktueller Anmeldestatus und ggf. Anzeigename. |
| PUT /auth/profile | Ändert displayName des angemeldeten Kontos. |
| POST /auth/logout | Invalidiert Sitzung und Cookie. |
| GET, POST /todos | Eigene Liste lesen oder eine Aufgabe anlegen. |
| GET, PUT, DELETE /todos/{id} | Eigene Aufgabe lesen, bearbeiten oder löschen. Fehlende und fremde Aufgaben ergeben HTTP 404. |

Der erste Schreibaufruf benötigt ebenfalls CSRF. Das Frontend holt den Wert vor jedem Schreibaufruf neu. Nach erfolgreicher Anmeldung wird die alte Sitzung invalidiert. Der Browser erhält ein hostgebundenes HttpOnly-Cookie mit SameSite=Lax und Pfad /. Produktiv ist Secure gesetzt. Kontositzungen enden nach zwei Stunden Inaktivität oder nach insgesamt 24 Stunden. Pro zehn Minuten sind acht Loginversuche je IP/Adresse, 40 je IP und 80 je Adresse möglich. IP-Limits zählen die vom Servlet gemeldete Gegenstelle.

## Speicherung und Betrieb

- E-Mail-Adressen werden an den Rändern bereinigt, mit Locale.ROOT kleingeschrieben und per Unique-Constraint geschützt. Punkte und Plus-Zusätze bleiben erhalten. Anzeigenamen sind änderbar und dürfen mehrfach vorkommen. Beide Felder und die Taskinhalte sind in der Datenbank lesbar.
- Das Passwort wird unverändert mit Argon2id verarbeitet (19 MiB, zwei Iterationen, Parallelität eins, eigener zufälliger 16-Byte-Salt, 32-Byte-Hash). Die Länge wird in Unicode-Codepoints geprüft. Für Bestätigungstokens werden nur SHA-256-Digest, Kontoreferenz, Zweck, Ablauf und Verbrauchsstatus gespeichert.
- HTTPS für beide Dienste sowie TLS für entfernte Datenbankverbindungen mit Zertifikatsprüfung einrichten. Datenbankzugriff auf das Backend beschränken. Datenträger und Backups über die Infrastruktur verschlüsseln, Zugriff und Wiederherstellung prüfen. Vor der ersten Schemaaktualisierung ein Backup erstellen. Request-Bodies, Autorisierungsheader, Cookies und SQL-Bindewerte aus Betriebslogs heraushalten.
- Frontend und API unter derselben Site betreiben, beispielsweise unter app.example.org und api.example.org, oder die API auf der Frontend-Origin unter /auth und /todos weiterleiten. Separate Hosting-Standarddomains können für den Browser unterschiedliche Sites sein. Eine CORS-Freigabe allein genügt dann für das Lax-Cookie nicht.
- Sitzungen und IP-Zähler liegen in einer Backend-Instanz. Ein Neustart beendet Sitzungen. Hinter einem Proxy den Zugriff auf vertrauenswürdige Proxys begrenzen und die Client-IP dort verbindlich setzen. Ohne passende Proxykonfiguration können mehrere Nutzer ein gemeinsames IP-Limit treffen.

## Prüfungen

`./gradlew test bootJar` führt die Integrationssuite mit Spring Security, MockMvc, H2 im PostgreSQL-Modus und gemocktem Mailer aus. Sie prüft unter anderem parallele Registrierung und Verifizierung, Passwortgrenzen, Erneuerung und Ablauf von Tokens, Versandfehler, Loginlimits, Sitzungswechsel, beide Sitzungsfristen, Logout, CSRF sowie fremde und besitzerlose Tasks. Es wird keine echte Mail verschickt.

Mit JDK 25 wurden alle 15 Java-Quelldateien syntaktisch geprüft. Zehn Backend-Quelldateien, darunter Datenmodell, Repositories, Versand und Verifizierung, ließen sich mit den lokal vorhandenen Bibliotheken separat kompilieren.

Der lokale Gradle-Versuch für diese Änderung wurde vor der Kompilierung durch eine Socket-Sperre beendet. Auch Maven Central war lokal nicht auflösbar. Backendtests, JAR-Build und der gemeinsame Browsertest sind deshalb noch in einer Umgebung mit lokalen Sockets und verfügbaren Abhängigkeiten auszuführen. Die Frontend-CI bereitet dafür einen separaten PostgreSQL-Testdienst vor. Nach erfolgreichem CI-Lauf bleiben die Einrichtung der Produktionsadressen, die Absenderverifizierung und ein echter Registrierungsdurchlauf mit einem Testpostfach.
