-- V3: стартовые источники сбора. Пользователи и демо-вакансии создаёт SeedDataGenerator.

INSERT INTO parsing_sources (name, source_type, base_url, is_active)
VALUES
    ('Habr Career', 'WEBSITE', 'https://career.habr.com/vacancies?type=all', TRUE),
    ('Telegram Java Jobs Mirror', 'TELEGRAM', 'https://t.me/s/java_jobs', TRUE),
    ('Telegram IT Vacancies Mirror', 'TELEGRAM', 'https://t.me/s/it_vacancies', TRUE);
