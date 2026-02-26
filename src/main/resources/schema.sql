-- Добавляем колонку, если её вдруг нет (безопасный скрипт для Postgres)
ALTER TABLE users ADD COLUMN IF NOT EXISTS telegram_chat_id BIGINT;

-- Сначала удаляем старое ограничение, чтобы не было конфликта
ALTER TABLE internship DROP CONSTRAINT IF EXISTS internship_status_check;

-- Добавляем актуальное ограничение
ALTER TABLE internship ADD CONSTRAINT internship_status_check
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CLOSED'));