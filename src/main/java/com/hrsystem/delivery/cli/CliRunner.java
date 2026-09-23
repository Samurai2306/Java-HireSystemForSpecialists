package com.hrsystem.delivery.cli;

import com.hrsystem.delivery.cli.utils.AnsiColor;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.delivery.cli.views.AbstractCliView;
import com.hrsystem.delivery.cli.views.AuthCliView;
import com.hrsystem.delivery.cli.views.RoleMenu;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Точка входа CLI: показывает экран аутентификации, затем полиморфно открывает меню роли. */
@Component
@Order(100)
public class CliRunner extends AbstractCliView implements CommandLineRunner {

    private final AuthCliView authCliView;
    private final List<RoleMenu> roleMenus;

    public CliRunner(AuthCliView authCliView, List<RoleMenu> roleMenus,
                     CliSessionContext sessionContext, InputValidator input) {
        super(sessionContext, input, System.out);
        this.authCliView = authCliView;
        this.roleMenus = roleMenus;
    }

    @Override
    public void run(String... args) {
        if (cliDisabled(args)) {
            return;
        }
        out.println(AnsiColor.colorize(
                "HR-SYSTEM  ·  Agile Recruitment Platform  ·  CLI v1.0",
                AnsiColor.BOLD + AnsiColor.CYAN));

        try {
            while (authCliView.showMainMenu()) {
                roleMenus.stream()
                        .filter(menu -> menu.supports(sessionContext.getCurrentRole()))
                        .findFirst()
                        .ifPresent(menu -> safely(menu::open));
                sessionContext.logout();
            }
        } catch (RuntimeException e) {
            err("Критическая ошибка: " + rootMessage(e));
        }
    }

    private static boolean cliDisabled(String... args) {
        for (String arg : args) {
            if ("--no-cli".equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return "true".equalsIgnoreCase(System.getProperty("hrsystem.cli.disabled"));
    }
}
