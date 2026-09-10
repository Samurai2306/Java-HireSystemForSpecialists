package com.hrsystem.delivery.cli;

import com.hrsystem.delivery.cli.utils.InputValidator;
import com.hrsystem.delivery.cli.views.AuthCliView;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Component
public class CliRunner {

    private final AuthCliView authCliView;

    public CliRunner(AuthCliView authCliView) {
        this.authCliView = authCliView;
    }

    public void start() {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            InputValidator input = new InputValidator(scanner, out);
            printBanner(out);
            boolean running = true;
            while (running) {
                try {
                    running = authCliView.showStartMenu(input, out);
                } catch (RuntimeException ex) {
                    out.println("[Необработанная ошибка] " + ex.getMessage());
                }
            }
            out.println("До свидания.");
        }
    }

    private void printBanner(PrintStream out) {
        try (InputStream stream = new ClassPathResource("banner.txt").getInputStream()) {
            out.println(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            out.println("HR-SYSTEM: AGGREGATOR & RECRUITING");
        }
    }
}
