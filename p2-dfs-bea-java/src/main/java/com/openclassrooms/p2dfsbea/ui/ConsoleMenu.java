package com.openclassrooms.p2dfsbea.ui;

import com.openclassrooms.p2dfsbea.model.CategoryStatistics;
import com.openclassrooms.p2dfsbea.model.Expense;
import com.openclassrooms.p2dfsbea.model.ExpenseStatistics;
import com.openclassrooms.p2dfsbea.repository.ExpenseRepository;
import com.openclassrooms.p2dfsbea.service.ExpenseService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class ConsoleMenu {
    private final Scanner scanner = new Scanner(System.in);
    private final ExpenseRepository repository = new ExpenseRepository();
    private final ExpenseService service = new ExpenseService(repository);

    public void start() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> showHistory();
                case "2" -> addExpense();
                case "3" -> showStatistics();
                case "4" -> {
                    System.out.println("Goodbye");
                    running = false;
                }
                default -> System.out.println("Unknown option, please try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- Personal Finance Tracker ---");
        System.out.println("1) Show history");
        System.out.println("2) Add expense");
        System.out.println("3) Statistics");
        System.out.println("4) Quit");
        System.out.print("Choose an option: ");
    }

    private void showHistory() {
        List<Expense> list = service.loadAllExpenses();
        System.out.println("\nHistory (most recent first):");
        list.stream().sorted((a, b) -> b.getDate().compareTo(a.getDate())).forEach(e -> System.out.println(e.toCsvLine()));
    }

    private void showStatistics() {
        ExpenseStatistics stats = service.computeStatistics();
        if (stats.categories().isEmpty()) {
            System.out.println("\nNo expenses recorded yet.");
            return;
        }
        System.out.println("\nOverall total: " + formatAmount(stats.overallTotal()));
        System.out.println("Months covered: " + stats.monthsCovered());
        System.out.println();
        printCategoryTable(stats.categories());
    }

    private void printCategoryTable(List<CategoryStatistics> rows) {
        String categoryHeader = "Category";
        String totalHeader = "Total";
        String averageHeader = "Monthly average";

        int categoryWidth = categoryHeader.length();
        int totalWidth = totalHeader.length();
        int averageWidth = averageHeader.length();
        for (CategoryStatistics row : rows) {
            categoryWidth = Math.max(categoryWidth, row.category().length());
            totalWidth = Math.max(totalWidth, formatAmount(row.total()).length());
            averageWidth = Math.max(averageWidth, formatAmount(row.monthlyAverage()).length());
        }

        String separator = "+-" + "-".repeat(categoryWidth) + "-+-" + "-".repeat(totalWidth)
                + "-+-" + "-".repeat(averageWidth) + "-+";
        String rowFormat = "| %-" + categoryWidth + "s | %" + totalWidth + "s | %" + averageWidth + "s |%n";

        System.out.println(separator);
        System.out.printf(rowFormat, categoryHeader, totalHeader, averageHeader);
        System.out.println(separator);
        for (CategoryStatistics row : rows) {
            System.out.printf(rowFormat, row.category(), formatAmount(row.total()), formatAmount(row.monthlyAverage()));
        }
        System.out.println(separator);
    }

    private static String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void addExpense() {
        try {
            System.out.print("Date (dd/MM/yyyy): ");
            String dateS = scanner.nextLine().trim();
            LocalDate date = LocalDate.parse(dateS, Expense.FORMATTER);

            System.out.print("Category: ");
            String category = scanner.nextLine().trim();

            System.out.print("Description: ");
            String description = scanner.nextLine().trim();

            System.out.print("Amount (e.g. 12.50): ");
            String amountS = scanner.nextLine().trim().replace(',', '.');
            BigDecimal amount = new BigDecimal(amountS);

            Expense e = new Expense(date, category, description, amount);
            service.addExpense(e);
            System.out.println("Expense added.");
        } catch (DateTimeParseException dtpe) {
            System.out.println("Invalid date format. Use dd/MM/yyyy.");
        } catch (IOException io) {
            System.out.println("Unable to save expense: " + io.getMessage());
        } catch(NumberFormatException ex) {
        	System.out.println("Invalid amount format. Decimal is expected.");
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }
}
