-- V2: Erweiterung für die Aufgabenverwaltung (Titel, Status, Priorität, Fälligkeit, Zeitstempel).
-- Alle vorhandenen Zeilen bleiben erhalten; es wird nichts gelöscht.

-- 1. name -> title, Leerzeichen am Rand entfernen, leere Titel ersetzen
ALTER TABLE task RENAME COLUMN name TO title;
UPDATE task SET title = btrim(title) WHERE title IS NOT NULL;
UPDATE task SET title = 'Ohne Titel' WHERE title IS NULL OR title = '';

-- 2. Beschreibung darf jetzt 2000 Zeichen lang sein
ALTER TABLE task ALTER COLUMN description TYPE varchar(2000);

-- 3. Titel über 120 Zeichen (früher bis 255 erlaubt): voller Text wandert
--    an den Anfang der Beschreibung, erst danach wird der Titel gekürzt.
--    Platz reicht immer: höchstens 255 + 255 Zeichen + Präfix < 2000.
UPDATE task
SET description = 'Ursprünglicher Titel: ' || title
                  || CASE WHEN description IS NULL THEN '' ELSE E'\n\n' || description END
WHERE length(title) > 120;
UPDATE task SET title = left(title, 120) WHERE length(title) > 120;
ALTER TABLE task ALTER COLUMN title TYPE varchar(120);
ALTER TABLE task ALTER COLUMN title SET NOT NULL;
ALTER TABLE task ADD CONSTRAINT task_title_not_blank CHECK (length(btrim(title)) > 0);

-- 4. done (boolean) -> status (OPEN/DONE)
ALTER TABLE task ADD COLUMN status varchar(10);
UPDATE task SET status = CASE WHEN done THEN 'DONE' ELSE 'OPEN' END;
ALTER TABLE task ALTER COLUMN status SET NOT NULL;
ALTER TABLE task ADD CONSTRAINT task_status_valid CHECK (status IN ('OPEN', 'DONE'));
ALTER TABLE task DROP COLUMN done;

-- 5. Neue Felder
ALTER TABLE task ADD COLUMN priority varchar(10) NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE task ADD CONSTRAINT task_priority_valid CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));
ALTER TABLE task ADD COLUMN due_date date;
-- Vorhandene Zeilen bekommen den Migrationszeitpunkt; neue Zeilen setzt das Backend.
ALTER TABLE task ADD COLUMN created_at timestamp(6) with time zone NOT NULL DEFAULT now();
ALTER TABLE task ADD COLUMN updated_at timestamp(6) with time zone NOT NULL DEFAULT now();
ALTER TABLE task ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE task ALTER COLUMN updated_at DROP DEFAULT;
