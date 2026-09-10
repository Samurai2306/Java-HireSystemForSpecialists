package com.hrsystem.delivery.cli.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleTableFormatterTest {

    @Test
    @DisplayName("Отрисовка таблицы с заголовками и данными")
    void renderTable_withData() {
        ConsoleTableFormatter table = new ConsoleTableFormatter("ID", "Должность", "Компания");
        table.addRow("1", "Java Dev", "Яндекс");
        table.addRow("2", "Python Dev", "Сбер");

        String rendered = table.render();

        assertThat(rendered).contains("ID");
        assertThat(rendered).contains("Должность");
        assertThat(rendered).contains("Компания");
        assertThat(rendered).contains("Java Dev");
        assertThat(rendered).contains("Яндекс");
        assertThat(rendered).contains("Python Dev");
        assertThat(rendered).contains("Сбер");
    }

    @Test
    @DisplayName("Корректный расчет длины с ANSI-кодами цвета")
    void visibleLength_shouldIgnoreAnsiEscapes() {
        String colored = AnsiColor.colorize("OFFER", AnsiColor.GREEN);
        assertThat(ConsoleTableFormatter.visibleLength(colored)).isEqualTo(5);
    }
}
