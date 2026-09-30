package com.hrsystem;

import com.hrsystem.repository.VacancyRepository;
import com.hrsystem.service.VacancyService;

import java.util.Scanner;

public class Main {
    
    // Инициализируем зависимости вручную (как требовалось в КР1, без Spring)
    private static final VacancyRepository repository = new VacancyRepository();
    private static final VacancyService service = new VacancyService(repository);
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== ИНФОРМАЦИОННАЯ СИСТЕМА HR (Режим КР1) ===");
        
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
                        System.out.println("Завершение работы...");
                        running = false;
                    }
                    default -> System.out.println("Ошибка: Неизвестная команда.");
                }
            } catch (Exception e) {
                // Обработка бизнес-исключений (Требование КР1: Программа не должна падать)
                System.err.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.print("""
                
                ========================================
                          МЕНЮ УПРАВЛЕНИЯ (КР 1)
                ========================================
                1. Добавить вакансию (Создание)
                2. Вывести все вакансии (Сортировка)
                3. Поиск вакансий по названию (Фильтрация)
                4. Убрать вакансию в архив (Изменение)
                5. Статистика (5 показателей)
                6. Экспорт данных в CSV
                0. Выход
                Выберите действие:\s""");
    }

    private static void addVacancy() {
        System.out.print("Введите название вакансии: ");
        String title = scanner.nextLine().trim();
        
        System.out.print("Введите название компании: ");
        String company = scanner.nextLine().trim();
        
        System.out.print("Зарплата От (оставьте пустым если нет): ");
        String minStr = scanner.nextLine().trim();
        java.math.BigDecimal min = minStr.isEmpty() ? null : new java.math.BigDecimal(minStr);
        
        System.out.print("Зарплата До (оставьте пустым если нет): ");
        String maxStr = scanner.nextLine().trim();
        java.math.BigDecimal max = maxStr.isEmpty() ? null : new java.math.BigDecimal(maxStr);
        
        service.addVacancy(title, company, min, max);
        System.out.println("Вакансия успешно добавлена!");
    }

    private static void searchVacancy() {
        System.out.print("Введите ключевое слово для поиска: ");
        String keyword = scanner.nextLine().trim();
        service.searchByTitle(keyword);
    }
    
    private static void archiveVacancy() {
        System.out.print("Введите ID вакансии для архивации: ");
        // Обработка неверного ввода (буквы вместо цифр)
        try {
            Long id = Long.parseLong(scanner.nextLine().trim());
            service.archiveVacancy(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID должен быть целым числом.");
        }
    }
}
