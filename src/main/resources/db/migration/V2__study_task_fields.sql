-- V2: Erweiterung für die Aufgabenverwaltung (Titel, Status, Priorität, Fälligkeit, Zeitstempel).
-- Alle vorhandenen Zeilen bleiben erhalten; es wird nichts gelöscht.
--
-- Die Migration funktioniert für zwei mögliche Ausgangszustände:
--  a) Schema der ersten Version (id, name, description varchar(255), done)
--  b) Schema aus dem Branch "backend-test" (schema.sql): zusätzlich priority mit
--     deutschen Werten (niedrig/mittel/hoch), due_date, created_at, updated_at,
--     description varchar(2000) und die Constraints task_title_valid/task_priority_valid.
-- Deshalb werden Spalten nur angelegt bzw. umbenannt, wenn sie (noch) fehlen.

-- 0. Constraints aus Zustand b) entfernen; sie werden unten in neuer Form angelegt
ALTER TABLE task DROP CONSTRAINT IF EXISTS task_title_valid;
ALTER TABLE task DROP CONSTRAINT IF EXISTS task_priority_valid;

-- 1. name -> title (nur wenn noch nicht geschehen)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'task' AND column_name = 'name')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'task' AND column_name = 'title') THEN
        ALTER TABLE task RENAME COLUMN name TO title;
    END IF;
END $$;
UPDATE task SET title = btrim(title) WHERE title IS NOT NULL;
UPDATE task SET title = 'Ohne Titel' WHERE title IS NULL OR title = '';

-- 2. Beschreibung darf 2000 Zeichen lang sein; leerer Text bedeutet "keine Beschreibung"
ALTER TABLE task ALTER COLUMN description TYPE varchar(2000);
UPDATE task SET description = NULL WHERE description IS NOT NULL AND btrim(description) = '';

-- 3. Titel über 120 Zeichen (früher bis 255 erlaubt): voller Text wandert
--    an den Anfang der Beschreibung, erst danach wird der Titel gekürzt.
UPDATE task
SET description = left('Ursprünglicher Titel: ' || title
                  || CASE WHEN description IS NULL THEN '' ELSE E'\n\n' || description END, 2000)
WHERE length(title) > 120;
UPDATE task SET title = left(title, 120) WHERE length(title) > 120;
ALTER TABLE task ALTER COLUMN title TYPE varchar(120);
ALTER TABLE task ALTER COLUMN title SET NOT NULL;
ALTER TABLE task ADD CONSTRAINT task_title_not_blank CHECK (length(btrim(title)) > 0);

-- 4. done (boolean) -> status (OPEN/DONE)
ALTER TABLE task ADD COLUMN IF NOT EXISTS status varchar(10);
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'task' AND column_name = 'done') THEN
        UPDATE task SET status = CASE WHEN done THEN 'DONE' ELSE 'OPEN' END WHERE status IS NULL;
        ALTER TABLE task DROP COLUMN done;
    END IF;
END $$;
UPDATE task SET status = 'OPEN' WHERE status IS NULL;
ALTER TABLE task ALTER COLUMN status SET NOT NULL;
ALTER TABLE task ADD CONSTRAINT task_status_valid CHECK (status IN ('OPEN', 'DONE'));

-- 5. Priorität: anlegen oder vorhandene deutsche Werte übernehmen
ALTER TABLE task ADD COLUMN IF NOT EXISTS priority varchar(10);
ALTER TABLE task ALTER COLUMN priority DROP DEFAULT;
ALTER TABLE task ALTER COLUMN priority TYPE varchar(10);
UPDATE task SET priority = CASE lower(btrim(coalesce(priority, '')))
                               WHEN 'niedrig' THEN 'LOW'
                               WHEN 'low'     THEN 'LOW'
                               WHEN 'hoch'    THEN 'HIGH'
                               WHEN 'high'    THEN 'HIGH'
                               ELSE 'MEDIUM'
                           END;
ALTER TABLE task ALTER COLUMN priority SET DEFAULT 'MEDIUM';
ALTER TABLE task ALTER COLUMN priority SET NOT NULL;
ALTER TABLE task ADD CONSTRAINT task_priority_valid CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));

-- 6. Fälligkeit und Zeitstempel (vorhandene Werte bleiben erhalten)
ALTER TABLE task ADD COLUMN IF NOT EXISTS due_date date;
ALTER TABLE task ADD COLUMN IF NOT EXISTS created_at timestamp(6) with time zone;
ALTER TABLE task ADD COLUMN IF NOT EXISTS updated_at timestamp(6) with time zone;
ALTER TABLE task ALTER COLUMN created_at TYPE timestamp(6) with time zone;
ALTER TABLE task ALTER COLUMN updated_at TYPE timestamp(6) with time zone;
UPDATE task SET created_at = now() WHERE created_at IS NULL;
UPDATE task SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE task ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE task ALTER COLUMN updated_at DROP DEFAULT;
ALTER TABLE task ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE task ALTER COLUMN updated_at SET NOT NULL;
