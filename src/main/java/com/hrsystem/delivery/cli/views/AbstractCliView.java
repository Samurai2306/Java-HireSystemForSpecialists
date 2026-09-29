package com.hrsystem.delivery.cli.views;

import com.hrsystem.delivery.cli.CliSessionContext;
import com.hrsystem.delivery.cli.utils.AnsiColor;
import com.hrsystem.delivery.cli.utils.InputValidator;

import java.io.PrintStream;

/**
 * Базовый класс для всех CLI-экранов: общий ввод/вывод, заголовки и безопасный
 * запуск действий (ошибка сервиса не «выпадает» в консоль стек-трейсом).
 */
public abstract class AbstractCliView {

    protected static final String LINE = "------------------------------------------------------------";
    protected static final String DOUBLE_LINE = "============================================================";

    protected final CliSessionContext sessionContext;
    protected final InputValidator in;
    protected final PrintStream out;

    protected AbstractCliView(CliSessionContext sessionContext, InputValidator in, PrintStream out) {
        this.sessionContext = sessionContext;
        this.in = in;
        this.out = out;
    }

    protected void header(String title) {
        header(title, AnsiColor.CYAN);
    }

    protected void header(String title, String color) {
        out.println("\n" + AnsiColor.colorize(DOUBLE_LINE, color));
        out.println(AnsiColor.colorize("   " + title, AnsiColor.BOLD + color));
        out.println(AnsiColor.colorize(DOUBLE_LINE, color));
    }

    protected void ok(String message) {
        out.println(AnsiColor.success(message));
    }

    protected void err(String message) {
        out.println(AnsiColor.error(message));
    }

    protected void warn(String message) {
        out.println(AnsiColor.warning(message));
    }

    protected void info(String message) {
        out.println(AnsiColor.info(message));
    }

    protected void pause() {
        in.readOptionalString("\nНажмите Enter для продолжения > ");
    }

    /** Выполняет действие и превращает любую ошибку в короткое понятное сообщение вместо падения CLI. */
    protected void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            err(rootMessage(e));
        }
    }

    protected static String rootMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message == null || message.isBlank() ? root.getClass().getSimpleName() : message.split("\n")[0];
    }
}
