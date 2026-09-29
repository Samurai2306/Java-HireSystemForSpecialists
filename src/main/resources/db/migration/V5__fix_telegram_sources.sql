-- V5: заменяем несуществующие Telegram-каналы на рабочие.
--
-- Причина: у каналов java_jobs и it_vacancies нет публичного веб-превью, поэтому Telegram
-- отвечает редиректом на t.me/<имя> и страницей-заглушкой с кодом 200. Jsoup считал такую
-- страницу успешно загруженной, селектор .tgme_widget_message_wrap ничего не находил,
-- и парсинг молча возвращал 0 карточек без единой ошибки в отчёте.
--
-- Рабочие каналы с публичным превью (проверено: t.me/s/java_jobs_ru, t.me/s/devjobs, t.me/s/vacancies_it).

-- 1. Если запись с рабочим каналом уже существует — убираем дубликат-заглушку,
--    чтобы не нарушить UNIQUE(base_url).
DELETE FROM parsing_sources
 WHERE base_url IN ('https://t.me/s/java_jobs', 'https://t.me/java_jobs', 'https://t.me/s/javajobs')
   AND EXISTS (SELECT 1 FROM parsing_sources p WHERE p.base_url = 'https://t.me/s/java_jobs_ru');

DELETE FROM parsing_sources
 WHERE base_url IN ('https://t.me/s/it_vacancies', 'https://t.me/it_vacancies')
   AND EXISTS (SELECT 1 FROM parsing_sources p WHERE p.base_url = 'https://t.me/s/devjobs');

-- 2. Мёртвые каналы переводим на рабочие.
UPDATE parsing_sources
   SET name = 'Telegram Java Jobs RU',
       base_url = 'https://t.me/s/java_jobs_ru',
       is_active = TRUE
 WHERE base_url IN ('https://t.me/s/java_jobs', 'https://t.me/java_jobs', 'https://t.me/s/javajobs');

UPDATE parsing_sources
   SET name = 'Telegram Dev Jobs',
       base_url = 'https://t.me/s/devjobs',
       is_active = TRUE
 WHERE base_url IN ('https://t.me/s/it_vacancies', 'https://t.me/it_vacancies');

-- 3. Добавляем третий рабочий канал, если его ещё нет.
INSERT INTO parsing_sources (name, source_type, base_url, is_active)
SELECT 'Telegram IT Vacancies', 'TELEGRAM', 'https://t.me/s/vacancies_it', TRUE
 WHERE NOT EXISTS (SELECT 1 FROM parsing_sources WHERE base_url = 'https://t.me/s/vacancies_it');
