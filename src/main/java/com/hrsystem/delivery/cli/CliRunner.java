package com.hrsystem.delivery.cli;

import com.hrsystem.delivery.cli.utils.AnsiColor;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.delivery.cli.views.AdminCliView;
import com.hrsystem.delivery.cli.views.AuthCliView;
import com.hrsystem.delivery.cli.views.CandidateCliView;
import com.hrsystem.delivery.cli.views.EmployerCliView;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class CliRunner implements CommandLineRunner {

    private final AuthCliView authCliView;
    private final CandidateCliView candidateCliView;
    private final EmployerCliView employerCliView;
    private final AdminCliView adminCliView;
    private final CliSessionContext sessionContext;
    private final InputValidator inputValidator;

    public CliRunner(AuthCliView authCliView,
                     CandidateCliView candidateCliView,
                     EmployerCliView employerCliView,
                     AdminCliView adminCliView,
                     CliSessionContext sessionContext) {
        this.authCliView = authCliView;
        this.candidateCliView = candidateCliView;
        this.employerCliView = employerCliView;
        this.adminCliView = adminCliView;
        this.sessionContext = sessionContext;
        this.inputValidator = new InputValidator();
    }

    @Override
    public void run(String... args) {
        for (String arg : args) {
            if ("--no-cli".equalsIgnoreCase(arg)) {
                return;
            }
        }
        if ("true".equalsIgnoreCase(System.getProperty("hrsystem.cli.disabled"))) {
            return;
        }

        printBanner();

        boolean running = true;
        while (running) {
            if (!sessionContext.isAuthorized()) {
                boolean proceed = authCliView.showMainMenu();
                if (!proceed) {
                    running = false;
                    break;
                }
            }

            if (sessionContext.isCandidate()) {
                candidateCliView.showCandidateDashboard();
            } else if (sessionContext.isGuest()) {
                // Guest catalog browsing
                candidateCliView.showVacancyCatalog();
                sessionContext.logout();
            } else if (sessionContext.isEmployer()) {
                Long employerId = sessionContext.getCurrentEmployerProfile() != null
                        ? sessionContext.getCurrentEmployerProfile().getId()
                        : null;
                Long userId = sessionContext.getCurrentUserId();
                if (employerId != null) {
                    employerCliView.show(employerId, userId, inputValidator, System.out);
                } else {
                    System.out.println(AnsiColor.error("Профиль работодателя не найден."));
                }
                sessionContext.logout();
            } else if (sessionContext.isAdmin()) {
                adminCliView.show(inputValidator, System.out);
                sessionContext.logout();
            }
        }
    }

    private void printBanner() {
        System.out.println(AnsiColor.colorize(
                "  _    _ _____     _______     _______ _______ ______ __  __ \n" +
                " | |  | |  __ \\   / ____\\ \\   / / ____|__   __|  ____|  \\/  |\n" +
                " | |__| | |__) | | (___  \\ \\_/ / (___    | |  | |__  | \\  / |\n" +
                " |  __  |  _  /   \\___ \\  \\   / \\___ \\   | |  |  __| | |\\/| |\n" +
                " | |  | | | \\ \\   ____) |  | |  ____) |  | |  | |____| |  | |\n" +
                " |_|  |_|_|  \\_\\ |_____/   |_| |_____/   |_|  |______|_|  |_|\n" +
                "               AGILE RECRUITMENT PLATFORM (CLI v1.0)       \n",
                AnsiColor.BOLD + AnsiColor.CYAN
        ));
    }
}
