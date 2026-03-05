-- Добавляем колонку, если её вдруг нет (безопасный скрипт для Postgres)
ALTER TABLE users ADD COLUMN IF NOT EXISTS telegram_chat_id BIGINT;

-- Сначала удаляем старое ограничение, чтобы не было конфликта
ALTER TABLE internship DROP CONSTRAINT IF EXISTS internship_status_check;

-- Добавляем актуальное ограничение
ALTER TABLE internship ADD CONSTRAINT internship_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CLOSED'));

-- Если статус — это просто строка с проверкой
ALTER TABLE application DROP CONSTRAINT IF EXISTS application_status_check;

-- Добавляем новую проверку, включающую APPROVED
ALTER TABLE application ADD CONSTRAINT application_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));

ALTER TABLE internships ADD COLUMN study_materials TEXT;

ALTER TABLE internships ADD COLUMN is_job BOOLEAN DEFAULT FALSE;
ALTER TABLE internships ADD COLUMN study_materials TEXT;

-- 1. Ескі шектеуді өшіреміз (аты қатеде көрсетілген: application_status_check)
ALTER TABLE application DROP CONSTRAINT application_status_check;

-- 2. Жаңа тізіммен шектеуді қайта қосамыз (барлық статустарыңды тізіп шық)
ALTER TABLE application ADD CONSTRAINT application_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'ENROLLED', 'VERIFIED'));

ALTER TABLE application DROP CONSTRAINT IF EXISTS application_status_check;

-- 1. Алдымен ескі шектеуді өшіреміз
ALTER TABLE application DROP CONSTRAINT IF EXISTS application_status_check;

-- 2. Жаңа рұқсат етілген статустар тізімін қосамыз (ACCEPTED-ті қоса алғанда)
ALTER TABLE application ADD CONSTRAINT application_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'VERIFIED', 'ENROLLED', 'ACCEPTED', 'COMPLETED'));