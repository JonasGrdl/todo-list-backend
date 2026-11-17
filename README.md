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
| MAILJET_FROM | Einzelne in Mailjet bestätigte Absenderadresse, z. B. jonas.groedl@student.htw-berlin.de (ohne Anzeigename/Klammern) |
| MAILJET_API_KEY | Öffentlicher API-Key aus Mailjet, ausschließlich im Backend hinterlegen. |
| MAILJET_SECRET_KEY | Zugehöriger geheimer API-Key, ausschließlich als Backend-Secret hinterlegen. |
| PORT | Optional, standardmäßig 8080 |
| VITE_BACKEND_BASE_URL | Nur im Frontend. Lokales Beispiel http://localhost:8080, im Deployment öffentliche HTTPS-API-Origin oder gleicher Origin mittels Reverse Proxy. |

Mit Java 25 im Backend-Verzeichnis starten:

```sh
./gradlew bootRun
```

Im Frontend-Verzeichnis mit Node 26 `npm ci` und `npm run dev` ausführen. Im Browser http://localhost:5173 öffnen, registrieren, die Bestätigungsmail öffnen, den Button betätigen und sich anmelden.

## Mailjet einrichten

1. Im kostenlosen Mailjet-Konto unter „Domains and Sender Addresses“ eine einzelne Absenderadresse hinzufügen und den Bestätigungslink im Postfach öffnen. Der Status muss „Active“ sein. Alternativ eine eigene Absenderdomain verifizieren.
2. Unter „API Key Management“ den vorhandenen API-Key und zugehörigen Secret Key verwenden.
3. Beim Render-Backend `MAILJET_FROM`, `MAILJET_API_KEY` und `MAILJET_SECRET_KEY` hinterlegen und den Backend-Code neu deployen. `RESEND_FROM` und `RESEND_API_KEY` werden nicht mehr verwendet.
4. `FRONTEND_URL=https://todo-list-frontend-165i.onrender.com` beibehalten. Das Frontend verwendet diese gemeinsame Origin auch als `VITE_BACKEND_BASE_URL`. Die Render-Rewrites `/auth/*`, `/todos` und `/todos/*` müssen vor dem SPA-Fallback auf das Backend zeigen.
5. Über „Link erneut senden“ den Versand für ein vorhandenes unbestätigtes Konto testen. In Mailjet die Annahme und Zustellung und anschließend im Postfach den Bestätigungslink prüfen. Der API-Key muss zum aktiven Absender gehören. Für Hochschul-/Freemail-Absender ohne Domainauthentifizierung können Empfängerserver Nachrichten als Spam behandeln oder ablehnen; die Zustellung an verschiedene Anbieter muss separat getestet werden.

Der Versand läuft per HTTPS über `https://api.mailjet.com/v3.1/send` mit Basic-Authentifizierung aus API-Key und Secret. Es werden weder SMTP-Ports noch zusätzliche Bibliotheken benötigt. Klick- und Öffnungstracking sind deaktiviert. Erfolg setzt sowohl einen HTTP-Erfolg als auch den Nachrichtenstatus `success` voraus; eine Provider-Annahme garantiert noch keine Zustellung.

Der Provider erhält die Zieladresse und den Mailinhalt. Das Backend erzeugt 32 zufällige Bytes und setzt den URL-sicheren Token in das Fragment des Links zur konfigurierten Frontend-Origin. Beim Öffnen entfernt das Frontend das Fragment aus der Adressleiste. Erst der Button sendet den Token im POST-Body. Die Seite lädt nur eigene Ressourcen und setzt die Referrer-Policy auf no-referrer. Die Bestätigung führt zur Anmeldung.

Bei fehlendem Schlüssel, fehlendem Absender, Timeout oder abgelehntem Versand meldet die API HTTP 503. Das Konto besteht weiter. Ein neuer Versuch ist nach 60 Sekunden möglich. Jeder neue Versandversuch widerruft vorherige Links. Auch ein erfolgreicher API-Versand bedeutet, dass der Anbieter die Mail angenommen hat. Die Zustellung ins Postfach muss im Deployment geprüft werden.

[Mailjet-Absender verifizieren](https://documentation.mailjet.com/hc/en-us/articles/360042759253-How-to-add-a-sender-address) · [Mailjet Send API](https://dev.mailjet.com/docs/email-api/send-api-v31/send-basic-email)

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

Die Mailjet-Anbindung wurde mit JDK 25 direkt gegen die lokal vorhandenen Bibliotheken kompiliert und mit drei JUnit-Tests geprüft: korrekte Authentifizierung/Empfänger/Bestätigungslink, Providerfehler einschließlich Nachrichtenfehler bei HTTP 200 sowie fehlende Zugangsdaten ohne Versand. Alle drei Tests sind erfolgreich; es wurde dabei keine echte Mail verschickt.

Der vollständige Gradle-Lauf (`test bootJar`) ist lokal durch eine Socket-Sperre des Gradle-Daemons blockiert. Für den separaten Lauf der bestehenden Spring-Security-Integrationssuite fehlen lokal einige Abhängigkeiten. Vollständiger Build und Integrationssuite müssen deshalb noch in CI beziehungsweise einer Umgebung mit verfügbaren Abhängigkeiten laufen. Der echte Versandtest folgt nach Hinterlegung der Mailjet-Zugangsdaten in Render.
