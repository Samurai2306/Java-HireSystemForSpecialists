package com.hrsystem.scraper;

import com.hrsystem.config.ScraperProperties;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;

@Component
public class HttpFetcher {

    private final ScraperProperties properties;

    public HttpFetcher(ScraperProperties properties) {
        this.properties = properties;
    }

    public Document fetch(String url) throws IOException {
        IOException last = null;
        int retries = Math.max(1, properties.getMaxRetries());
        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                Connection.Response response = Jsoup.connect(url)
                        .userAgent(properties.getUserAgent())
                        .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
                        .timeout(properties.getReadTimeoutMs())
                        .followRedirects(true)
                        .ignoreHttpErrors(true)
                        .execute();
                int status = response.statusCode();
                if (status == 429 || status == 403 || status >= 500) {
                    last = new IOException("HTTP " + status + " для " + url);
                    backoff(attempt);
                    continue;
                }
                if (status >= 400) {
                    throw new IOException("HTTP " + status + " для " + url);
                }
                return response.parse();
            } catch (SocketTimeoutException e) {
                last = e;
                backoff(attempt);
            } catch (IOException e) {
                last = e;
                backoff(attempt);
            }
        }
        throw last == null ? new IOException("Не удалось загрузить " + url) : last;
    }

    public void pause() {
        try {
            Thread.sleep(properties.getRequestDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(300L * (1L << Math.min(attempt, 4)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
