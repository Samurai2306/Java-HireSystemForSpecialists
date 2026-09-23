package com.hrsystem.scraper;

import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * Контракт загрузчика HTML-страниц. Скраперы зависят от интерфейса, а не от конкретного
 * {@link HttpFetcher}: это позволяет подменять загрузчик в тестах (мок интерфейса) и
 * добавлять другие реализации (например, с кэшем или прокси) без правки парсеров.
 */
public interface HtmlFetcher {

    /** Загружает страницу по URL с ретраями и возвращает разобранный документ. */
    Document fetch(String url) throws IOException;

    /** Пауза между запросами, чтобы не злить источник. */
    void pause();
}
