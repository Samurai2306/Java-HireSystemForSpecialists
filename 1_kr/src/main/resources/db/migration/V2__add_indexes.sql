-- V2: индексы, частичный уникальный индекс откликов, полнотекстовый поиск

CREATE INDEX idx_vacancies_status_pub_date ON vacancies(status, published_at DESC);
CREATE INDEX idx_vacancies_salary_min ON vacancies(salary_min) WHERE status = 'ACTIVE';
CREATE INDEX idx_vacancies_source_type ON vacancies(source_type);
CREATE INDEX idx_vacancies_employer_id ON vacancies(employer_id);

CREATE INDEX idx_applications_vacancy_id ON applications(vacancy_id);
CREATE INDEX idx_applications_candidate_id ON applications(candidate_id);
CREATE INDEX idx_applications_status ON applications(status);

CREATE UNIQUE INDEX uq_candidate_active_application
    ON applications(vacancy_id, candidate_id)
    WHERE status IN ('APPLIED', 'REVIEWING');

CREATE OR REPLACE FUNCTION update_vacancy_search_vector() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('russian', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('russian', coalesce(NEW.company_name, '')), 'B') ||
        setweight(to_tsvector('russian', coalesce(NEW.requirements_stack, '')), 'B') ||
        setweight(to_tsvector('russian', coalesce(NEW.description, '')), 'C');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_vacancies_search_vector_update
    BEFORE INSERT OR UPDATE ON vacancies
    FOR EACH ROW EXECUTE FUNCTION update_vacancy_search_vector();

CREATE INDEX idx_vacancies_search_vector ON vacancies USING GIN(search_vector);
