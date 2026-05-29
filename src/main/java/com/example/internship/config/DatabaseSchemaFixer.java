package com.example.internship.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Исправляет схему после обновлений (колонки оценки в lesson_progress и application).
 * Hibernate ddl-auto=update не всегда корректно добавляет NOT NULL к существующим строкам.
 */
@Component
public class DatabaseSchemaFixer {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaFixer.class);

    private final JdbcTemplate jdbc;

    public DatabaseSchemaFixer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void fixSchema() {
        fixLessonProgressColumns();
        fixApplicationGradeColumns();
        fixApplicationCertificateToken();
    }

    private void fixLessonProgressColumns() {
        if (!tableExists("lesson_progress")) {
            return;
        }
        addIntColumnWithDefault("lesson_progress", "attempts", 1);
        addIntColumnWithDefault("lesson_progress", "score_percent", 100);
    }

    private void fixApplicationGradeColumns() {
        if (!tableExists("application")) {
            return;
        }
        addNullableIntColumn("application", "final_grade_percent");
        addNullableVarcharColumn("application", "grade_letter", 32);
        addNullableBooleanColumn("application", "quiz_passed");
        addNullableIntColumn("application", "quiz_score_percent");
        addNullableTimestampColumn("application", "completed_at");
    }

    private void fixApplicationCertificateToken() {
        if (!tableExists("application")) {
            return;
        }
        addNullableVarcharColumn("application", "certificate_token", 12);
        try {
            if (columnExists("application", "certificate_token")) {
                jdbc.execute("""
                        CREATE UNIQUE INDEX IF NOT EXISTS uq_application_certificate_token
                        ON application (certificate_token)
                        WHERE certificate_token IS NOT NULL
                        """);
            }
        } catch (Exception e) {
            log.warn("Индекс certificate_token: {}", e.getMessage());
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = ?
                """,
                Integer.class,
                table);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """,
                Integer.class,
                table,
                column);
        return count != null && count > 0;
    }

    private void addIntColumnWithDefault(String table, String column, int defaultValue) {
        try {
            if (!columnExists(table, column)) {
                jdbc.execute(String.format(
                        "ALTER TABLE %s ADD COLUMN %s integer NOT NULL DEFAULT %d",
                        table, column, defaultValue));
                log.info("Добавлена колонка {}.{}", table, column);
                return;
            }
            jdbc.update(String.format(
                    "UPDATE %s SET %s = ? WHERE %s IS NULL",
                    table, column, column), defaultValue);
            jdbc.execute(String.format("ALTER TABLE %s ALTER COLUMN %s SET DEFAULT %d", table, column, defaultValue));
            jdbc.execute(String.format("ALTER TABLE %s ALTER COLUMN %s SET NOT NULL", table, column));
        } catch (Exception e) {
            log.warn("Не удалось обновить колонку {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void addNullableIntColumn(String table, String column) {
        if (!columnExists(table, column)) {
            try {
                jdbc.execute(String.format("ALTER TABLE %s ADD COLUMN %s integer", table, column));
                log.info("Добавлена колонка {}.{}", table, column);
            } catch (Exception e) {
                log.warn("Колонка {}.{}: {}", table, column, e.getMessage());
            }
        }
    }

    private void addNullableVarcharColumn(String table, String column, int length) {
        if (!columnExists(table, column)) {
            try {
                jdbc.execute(String.format(
                        "ALTER TABLE %s ADD COLUMN %s varchar(%d)", table, column, length));
                log.info("Добавлена колонка {}.{}", table, column);
            } catch (Exception e) {
                log.warn("Колонка {}.{}: {}", table, column, e.getMessage());
            }
        }
    }

    private void addNullableBooleanColumn(String table, String column) {
        if (!columnExists(table, column)) {
            try {
                jdbc.execute(String.format("ALTER TABLE %s ADD COLUMN %s boolean", table, column));
                log.info("Добавлена колонка {}.{}", table, column);
            } catch (Exception e) {
                log.warn("Колонка {}.{}: {}", table, column, e.getMessage());
            }
        }
    }

    private void addNullableTimestampColumn(String table, String column) {
        if (!columnExists(table, column)) {
            try {
                jdbc.execute(String.format("ALTER TABLE %s ADD COLUMN %s timestamp(6)", table, column));
                log.info("Добавлена колонка {}.{}", table, column);
            } catch (Exception e) {
                log.warn("Колонка {}.{}: {}", table, column, e.getMessage());
            }
        }
    }
}
