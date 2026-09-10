package com.hrsystem.delivery.cli.utils;

import com.hrsystem.domain.enums.ApplicationStatus;

public class AnsiColor {
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String GRAY = "\u001B[90m";

    public static String colorize(String text, String color) {
        if (text == null) return "";
        return color + text + RESET;
    }

    public static String colorizeStatus(ApplicationStatus status) {
        if (status == null) return "-";
        switch (status) {
            case OFFER:
                return GREEN + BOLD + "[ " + status.name() + " ]" + RESET;
            case REVIEWING:
                return YELLOW + BOLD + "[ " + status.name() + " ]" + RESET;
            case REJECTED:
                return RED + "[ " + status.name() + " ]" + RESET;
            case APPLIED:
                return CYAN + "[ " + status.name() + " ]" + RESET;
            case WITHDRAWN:
                return GRAY + "[ " + status.name() + " ]" + RESET;
            default:
                return "[ " + status.name() + " ]";
        }
    }

    public static String success(String msg) {
        return GREEN + "✔ " + msg + RESET;
    }

    public static String error(String msg) {
        return RED + "✖ " + msg + RESET;
    }

    public static String info(String msg) {
        return CYAN + "ℹ " + msg + RESET;
    }

    public static String warning(String msg) {
        return YELLOW + "⚠ " + msg + RESET;
    }
}
