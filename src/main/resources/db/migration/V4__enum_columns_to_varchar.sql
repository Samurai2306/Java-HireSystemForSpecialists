-- V4: приводим ENUM-колонки к VARCHAR, чтобы типы в БД совпадали
-- с JPA-маппингом @Enumerated(EnumType.STRING) и запросы/вставки
-- работали без ошибок "column is of type ... but expression is of type character varying".
--
-- Нюансы PostgreSQL: частичные индексы и DEFAULT-значения держат ссылку на enum-тип,
-- поэтому их снимаем перед ALTER TYPE и создаём заново после.

DROP INDEX IF EXISTS idx_vacancies_salary_min;
DROP INDEX IF EXISTS uq_candidate_active_application;

ALTER TABLE users ALTER COLUMN role DROP DEFAULT;
ALTER TABLE vacancies ALTER COLUMN currency DROP DEFAULT;
ALTER TABLE vacancies ALTER COLUMN employment_type DROP DEFAULT;
ALTER TABLE vacancies ALTER COLUMN source_type DROP DEFAULT;
ALTER TABLE vacancies ALTER COLUMN status DROP DEFAULT;
ALTER TABLE applications ALTER COLUMN status DROP DEFAULT;

ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(50) USING role::text;
ALTER TABLE parsing_sources ALTER COLUMN source_type TYPE VARCHAR(50) USING source_type::text;
ALTER TABLE vacancies ALTER COLUMN currency TYPE VARCHAR(10) USING currency::text;
ALTER TABLE vacancies ALTER COLUMN employment_type TYPE VARCHAR(50) USING employment_type::text;
ALTER TABLE vacancies ALTER COLUMN source_type TYPE VARCHAR(50) USING source_type::text;
ALTER TABLE vacancies ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE applications ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE users ALTER COLUMN role SET DEFAULT 'CANDIDATE';
ALTER TABLE vacancies ALTER COLUMN currency SET DEFAULT 'RUB';
ALTER TABLE vacancies ALTER COLUMN employment_type SET DEFAULT 'REMOTE';
ALTER TABLE vacancies ALTER COLUMN source_type SET DEFAULT 'MANUAL';
ALTER TABLE vacancies ALTER COLUMN status SET DEFAULT 'ACTIVE';
ALTER TABLE applications ALTER COLUMN status SET DEFAULT 'APPLIED';

CREATE INDEX idx_vacancies_salary_min ON vacancies(salary_min) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uq_candidate_active_application
    ON applications(vacancy_id, candidate_id)
    WHERE status IN ('APPLIED', 'REVIEWING');

DROP TYPE IF EXISTS user_role_enum;
DROP TYPE IF EXISTS source_type_enum;
DROP TYPE IF EXISTS vacancy_status_enum;
DROP TYPE IF EXISTS application_status_enum;
DROP TYPE IF EXISTS currency_enum;
DROP TYPE IF EXISTS employment_type_enum;
