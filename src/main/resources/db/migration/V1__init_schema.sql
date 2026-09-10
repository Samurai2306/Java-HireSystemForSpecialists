-- V1: базовая схема HR-системы (ENUM, таблицы, FK, CHECK)

CREATE TYPE user_role_enum AS ENUM ('CANDIDATE', 'EMPLOYER', 'ADMIN');
CREATE TYPE source_type_enum AS ENUM ('WEBSITE', 'TELEGRAM', 'MANUAL');
CREATE TYPE vacancy_status_enum AS ENUM ('ACTIVE', 'ARCHIVED', 'MODERATION', 'REJECTED');
CREATE TYPE application_status_enum AS ENUM ('APPLIED', 'REVIEWING', 'OFFER', 'REJECTED', 'WITHDRAWN');
CREATE TYPE currency_enum AS ENUM ('RUB', 'USD', 'EUR', 'KZT');
CREATE TYPE employment_type_enum AS ENUM ('REMOTE', 'OFFICE', 'HYBRID', 'FLEXIBLE');

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role user_role_enum NOT NULL DEFAULT 'CANDIDATE',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE candidate_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    target_title VARCHAR(255),
    skills TEXT,
    phone VARCHAR(100),
    telegram VARCHAR(100),
    portfolio_links TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employer_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    website_url VARCHAR(255),
    description TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE parsing_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    source_type source_type_enum NOT NULL,
    base_url VARCHAR(500) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_scraped_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vacancies (
    id BIGSERIAL PRIMARY KEY,
    employer_id BIGINT REFERENCES employer_profiles(id) ON DELETE SET NULL,
    source_id BIGINT REFERENCES parsing_sources(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    salary_min INTEGER CHECK (salary_min >= 0),
    salary_max INTEGER CHECK (salary_max >= salary_min OR salary_max IS NULL),
    currency currency_enum NOT NULL DEFAULT 'RUB',
    description TEXT NOT NULL,
    requirements_stack TEXT,
    location VARCHAR(100) DEFAULT 'Не указано',
    employment_type employment_type_enum DEFAULT 'REMOTE',
    source_type source_type_enum NOT NULL DEFAULT 'MANUAL',
    source_url VARCHAR(1000),
    content_hash VARCHAR(64) UNIQUE,
    is_parsed BOOLEAN NOT NULL DEFAULT FALSE,
    status vacancy_status_enum NOT NULL DEFAULT 'ACTIVE',
    search_vector TSVECTOR,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    vacancy_id BIGINT NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    candidate_id BIGINT NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    cover_letter TEXT,
    status application_status_enum NOT NULL DEFAULT 'APPLIED',
    status_comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE parsing_logs (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT REFERENCES parsing_sources(id) ON DELETE SET NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP WITH TIME ZONE,
    items_found INTEGER NOT NULL DEFAULT 0,
    items_saved INTEGER NOT NULL DEFAULT 0,
    duplicates_skipped INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT
);
