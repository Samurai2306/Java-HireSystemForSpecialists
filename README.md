# HR System (Агрегатор вакансий) - Контрольная работа №1

Консольная информационная система управления вакансиями (HR System), разработанная на чистой Java (Pure Java) с использованием JDBC и PostgreSQL.

Проект полностью соответствует требованиям к КР1:
- Разделение на слои (UI, Service, Repository)
- Использование ООП, интерфейсов, полиморфизма
- Работа с базой данных через JDBC (`PreparedStatement`, `Connection`, `ResultSet`)
- Управление зависимостями вручную (без Spring)
- Использование Java Collections Framework и Stream API
- Консольное меню на основе `switch` выражений
- Безопасная обработка исключений (система не падает при ошибках ввода)

---

##  Архитектура проекта

Проект построен по классической многослойной архитектуре.

```mermaid
flowchart TD
    UI["Console UI\n(Main.java)"]
    Service["Business Logic\n(VacancyService.java)"]
    Repo["Data Access\n(VacancyRepository.java)"]
    DB[("PostgreSQL")]

    UI -- Ввод/Вывод --> Service
    Service -- Проверки & Бизнес-логика --> Repo
    Repo -- SQL & JDBC --> DB
```

---

##  Схема базы данных (ER Diagram)

База данных состоит из двух связанных сущностей: `users` (дополнительная сущность) и `vacancies` (основная сущность).

```mermaid
erDiagram
    users {
        int id PK
        varchar email UK "NOT NULL"
        varchar password_hash "NOT NULL"
        varchar role "NOT NULL"
        boolean is_active
        timestamp created_at
    }
    
    vacancies {
        int id PK
        int employer_id FK "NOT NULL"
        varchar title "NOT NULL"
        varchar company_name "NOT NULL"
        decimal salary_min "CHECK (>= 0)"
        decimal salary_max "CHECK (>= 0)"
        varchar status "NOT NULL"
        boolean is_parsed
        varchar source_type
        timestamp created_at
        timestamp updated_at
    }

    users ||--o{ vacancies : "создает (employer_id)"
```

**Ограничения на уровне БД:**
- Каскадное удаление (ON DELETE CASCADE)
- Проверка валидности зарплаты: `CHECK (salary_max >= salary_min)`

---

##  Диаграмма классов (Class Diagram)

```mermaid
classDiagram
    class Main {
        -VacancyRepository repository$
        -VacancyService service$
        -Scanner scanner$
        +main(String[] args)$
        -printMenu()$
        -addVacancy()$
        -searchVacancy()$
        -archiveVacancy()$
    }

    class VacancyService {
        -VacancyRepository repository
        +VacancyService(VacancyRepository repo)
        +addVacancy(String title, String company, BigDecimal min, BigDecimal max)
        +printAllVacancies()
        +searchByTitle(String keyword)
        +archiveVacancy(Long id)
        +printStatistics()
        +exportToCsv()
    }

    class CrudRepository~T, ID~ {
        <<interface>>
        +save(T entity)
        +findById(ID id) T
        +findAll() List~T~
        +update(T entity)
        +deleteById(ID id)
    }

    class VacancyRepository {
        +save(Vacancy vacancy)
        +findById(Long id) Vacancy
        +findAll() List~Vacancy~
        +update(Vacancy vacancy)
        +deleteById(Long id)
    }

    class Vacancy {
        -Long id
        -Long employerId
        -String title
        -String companyName
        -BigDecimal salaryMin
        -BigDecimal salaryMax
        -VacancyStatus status
        +printFormatted()
    }

    class VacancyStatus {
        <<enumeration>>
        ACTIVE
        ARCHIVED
        REJECTED
    }

    class BusinessException {
        +BusinessException(String message)
    }

    class DatabaseManager {
        -String URL$
        -String USER$
        -String PASS$
        +getConnection()$ Connection
    }

    Main --> VacancyService : uses
    VacancyService --> VacancyRepository : depends on
    VacancyRepository ..|> CrudRepository : implements
    VacancyRepository --> DatabaseManager : gets connection
    VacancyRepository --> Vacancy : manages
    Vacancy --> VacancyStatus : has state
    VacancyService ..> BusinessException : throws
```

---

##  Пользовательские сценарии (User Story Map)

```mermaid
journey
    title Путь пользователя (Employer) в консольной системе
    section Запуск и просмотр
      Запуск приложения: 5: Main
      Просмотр списка всех вакансий: 4: VacancyService, VacancyRepository
    section Управление вакансиями
      Ввод данных новой вакансии: 4: Main
      Проверка бизнес-правил: 5: VacancyService
      Сохранение в БД: 5: VacancyRepository
    section Поиск и фильтрация
      Поиск по названию: 4: VacancyService
      Сортировка через Stream API: 4: VacancyService
    section Аналитика
      Вывод статистики (5 показателей): 5: VacancyService
      Экспорт данных в CSV: 4: VacancyService
```

---

##  Как запустить проект

### 1. Поднять базу данных

В проекте есть `docker-compose.yml`, который запускает PostgreSQL и pgAdmin:

```bash
docker-compose up -d
```

Это создаст:
- PostgreSQL на порту `5432` (база `hr_system_db`, пользователь `hr_user`, пароль `hr_password`)
- pgAdmin на порту `5050` (логин `admin@hrsystem.local`, пароль `admin123`)

> Если PostgreSQL уже стоит локально, docker не нужен — главное чтобы база, пользователь и пароль совпадали с тем, что в `DatabaseManager.java`.

### 2. Создать таблицы и заполнить тестовыми данными

```bash
psql -h localhost -U hr_user -d hr_system_db -f init.sql
```

Или через pgAdmin: открыть `http://localhost:5050`, подключиться к серверу, открыть Query Tool и вставить содержимое `init.sql`.

Скрипт создаст таблицы `users` и `vacancies`, добавит 5 пользователей и 10 вакансий.

### 3. Собрать и запустить

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.hrsystem.Main"
```

##  Реализованные бизнес-правила (согласно требованиям)

1. Нельзя создать запись без названия.
2. Зарплата не может быть отрицательной.
3. Максимальная зарплата не может быть меньше минимальной.
4. Отсутствие записи с указанным ID обрабатывается без падения программы.
5. Запрещенный переход статусов (нельзя перевести в архив вакансию, которая уже в архиве).
