package com.hrsystem.delivery.cli;

import com.hrsystem.delivery.cli.utils.AnsiColor;
import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.delivery.cli.views.AdminCliView;
import com.hrsystem.delivery.cli.views.AuthCliView;
import com.hrsystem.delivery.cli.views.CandidateCliView;
import com.hrsystem.delivery.cli.views.EmployerCliView;
import com.hrsystem.domain.entity.EmployerProfileEntity;
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
    private final InputValidator inputValidator = new InputValidator();

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
    }

    @Override
    public void run(String... args) {
        if (cliDisabled(args)) {
            return;
        }
        System.out.println(AnsiColor.colorize(
                "HR-SYSTEM  ·  Agile Recruitment Platform  ·  CLI v1.0",
                AnsiColor.BOLD + AnsiColor.CYAN));

        while (true) {
            if (!sessionContext.isAuthorized()) {
                boolean stayInProgram = authCliView.showMainMenu();
                if (!stayInProgram) {
                    return;
                }
            }

            if (sessionContext.isCandidate()) {
                candidateCliView.showCandidateDashboard();
            } else if (sessionContext.isGuest()) {
                candidateCliView.showVacancyCatalog();
                sessionContext.logout();
            } else if (sessionContext.isEmployer()) {
                EmployerProfileEntity profile = sessionContext.getCurrentEmployerProfile();
                if (profile != null) {
                    employerCliView.show(profile.getId(), sessionContext.getCurrentUserId(), inputValidator, System.out);
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

    private static boolean cliDisabled(String... args) {
        for (int i = 0; i < args.length; i++) {
            if ("--no-cli".equalsIgnoreCase(args[i])) {
                return true;
            }
        }
        return "true".equalsIgnoreCase(System.getProperty("hrsystem.cli.disabled"));
    }
}
