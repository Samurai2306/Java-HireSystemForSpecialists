package com.hrsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hr.scraper")
public class ScraperProperties {

    private String userAgent = "Mozilla/5.0";
    private int connectTimeoutMs = 10_000;
    private int readTimeoutMs = 10_000;
    private long requestDelayMs = 500;
    private int maxPages = 5;
    private int maxRetries = 3;

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public long getRequestDelayMs() {
        return requestDelayMs;
    }

    public void setRequestDelayMs(long requestDelayMs) {
        this.requestDelayMs = requestDelayMs;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
