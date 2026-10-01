package com.hrsystem;

import com.hrsystem.repository.VacancyRepository;
import com.hrsystem.service.VacancyService;

import java.util.Scanner;

public class Main {
    
    // Собираем всё руками, Spring тут не нужен
    private static final VacancyRepository repository = new VacancyRepository();
    private static final VacancyService service = new VacancyService(repository);
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== HR-система — управление вакансиями ===");
        
        boolean running = true;
        while (running) {
            printMenu();
            String command = scanner.nextLine().trim();
            
            try {
                switch (command) {
                    case "1" -> addVacancy();
                    case "2" -> service.printAllVacancies();
                    case "3" -> searchVacancy();
                    case "4" -> archiveVacancy();
                    case "5" -> service.printStatistics();
                    case "6" -> service.exportToCsv();
                    case "0" -> {
                        System.out.println("Выходим...");
                        running = false;
                    }
                    default -> System.out.println("Нет такой команды, попробуйте ещё раз.");
                }
            } catch (Exception e) {
                // ловим ошибки, чтобы программа не падала
                System.err.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.print("""
                
                ========================================
                            ГЛАВНОЕ МЕНЮ
                ========================================
                1. Новая вакансия
                2. Все вакансии
                3. Поиск по названию
                4. В архив
                5. Статистика
                6. Выгрузить в CSV
                0. Выход
                > \s""");
    }

    private static void addVacancy() {
        System.out.print("Название вакансии: ");
        String title = scanner.nextLine().trim();
        
        System.out.print("Компания: ");
        String company = scanner.nextLine().trim();
        
        System.out.print("Зарплата от: ");
        String minStr = scanner.nextLine().trim();
        java.math.BigDecimal min = minStr.isEmpty() ? null : new java.math.BigDecimal(minStr);
        
        System.out.print("Зарплата до: ");
        String maxStr = scanner.nextLine().trim();
        java.math.BigDecimal max = maxStr.isEmpty() ? null : new java.math.BigDecimal(maxStr);
        
        service.addVacancy(title, company, min, max);
        System.out.println("Готово, вакансия добавлена.");
    }

    private static void searchVacancy() {
        System.out.print("Что ищем: ");
        String keyword = scanner.nextLine().trim();
        service.searchByTitle(keyword);
    }
    
    private static void archiveVacancy() {
        System.out.print("ID вакансии: ");
        // если ввели буквы вместо цифр — ругаемся
        try {
            Long id = Long.parseLong(scanner.nextLine().trim());
            service.archiveVacancy(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID — это число, а не текст.");
        }
    }
}
