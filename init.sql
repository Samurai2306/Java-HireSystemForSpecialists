-- SQL-скрипт создания базы данных для КР1 (HR Система)

-- Удаление таблиц, если они существуют (для чистого запуска)
DROP TABLE IF EXISTS vacancies;
DROP TABLE IF EXISTS users;

-- 1. Создание таблицы пользователей (Связанная сущность)
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Создание таблицы вакансий (Основная сущность)
CREATE TABLE vacancies (
    id SERIAL PRIMARY KEY,
    employer_id INT NOT NULL, -- FOREIGN KEY
    title VARCHAR(255) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    salary_min DECIMAL(10, 2) CONSTRAINT check_salary_min_positive CHECK (salary_min >= 0),
    salary_max DECIMAL(10, 2) CONSTRAINT check_salary_max_positive CHECK (salary_max >= 0),
    status VARCHAR(50) NOT NULL,
    is_parsed BOOLEAN DEFAULT false,
    source_type VARCHAR(50) DEFAULT 'MANUAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Ограничение бизнес-логики на уровне БД
    CONSTRAINT check_salary_range CHECK (salary_max IS NULL OR salary_min IS NULL OR salary_max >= salary_min),
    
    -- Связь с таблицей пользователей
    CONSTRAINT fk_employer FOREIGN KEY (employer_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Наполнение тестовыми данными (Требование КР1: минимум 5 пользователей, 10 записей основной сущности)
INSERT INTO users (email, password_hash, role) VALUES 
('admin@hr.ru', 'hash1', 'ADMIN'),
('apple@corp.com', 'hash2', 'EMPLOYER'),
('yandex@corp.ru', 'hash3', 'EMPLOYER'),
('google@corp.com', 'hash4', 'EMPLOYER'),
('candidate@mail.ru', 'hash5', 'CANDIDATE');

INSERT INTO vacancies (employer_id, title, company_name, salary_min, salary_max, status, source_type) VALUES
(2, 'Senior Java Developer', 'Apple', 300000.00, 450000.00, 'ACTIVE', 'MANUAL'),
(2, 'iOS Engineer', 'Apple', 250000.00, 350000.00, 'ACTIVE', 'MANUAL'),
(3, 'Middle Backend Developer', 'Yandex', 150000.00, 220000.00, 'ACTIVE', 'MANUAL'),
(3, 'Data Scientist', 'Yandex', 200000.00, 300000.00, 'ARCHIVED', 'MANUAL'),
(3, 'QA Automation', 'Yandex', 120000.00, 180000.00, 'ACTIVE', 'MANUAL'),
(4, 'DevOps Engineer', 'Google', 350000.00, 500000.00, 'ACTIVE', 'MANUAL'),
(4, 'Frontend React Developer', 'Google', 200000.00, 280000.00, 'ACTIVE', 'MANUAL'),
(2, 'Product Manager', 'Apple', 250000.00, NULL, 'REJECTED', 'MANUAL'),
(4, 'System Analyst', 'Google', 180000.00, 250000.00, 'ACTIVE', 'MANUAL'),
(3, 'HR Manager', 'Yandex', 80000.00, 120000.00, 'ARCHIVED', 'MANUAL');
