package com.openclassrooms.p2dfsbea.ui;

import com.openclassrooms.p2dfsbea.model.CarPurchaseSimulation;
import com.openclassrooms.p2dfsbea.model.CategoryStatistics;
import com.openclassrooms.p2dfsbea.model.Expense;
import com.openclassrooms.p2dfsbea.model.ExpenseStatistics;
import com.openclassrooms.p2dfsbea.model.MortgageSimulation;
import com.openclassrooms.p2dfsbea.repository.ExpenseRepository;
import com.openclassrooms.p2dfsbea.service.ExpenseService;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Console menu of the application.
 *
 * <p>TODO technical debt: only the menu loop and the Statistics option are covered by console-level
 * tests (see ConsoleMenuStatisticsTest). The other console parts, "Show history" and "Add expense"
 * (prompts, input parsing and error messages), still have to be test-covered.
 */
public class ConsoleMenu {
    private final Scanner scanner;
    private final ExpenseService service;

    public ConsoleMenu() {
        this(System.in, new ExpenseService(new ExpenseRepository()));
    }

    /** Lets tests drive the menu with scripted input and a stubbed service. */
    ConsoleMenu(InputStream input, ExpenseService service) {
        this.scanner = new Scanner(input);
        this.service = service;
    }

    public void start() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> showHistory();
                case "2" -> addExpense();
                case "3" -> showStatistics();
                case "4" -> simulateCarPurchase();
                case "5" -> simulateMortgage();
                case "6" -> {
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
        System.out.println("4) Simulation achat d'une voiture");
        System.out.println("5) Simulation prêt immobilier");
        System.out.println("6) Quit");
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

    private void simulateCarPurchase() {
        System.out.println("\n=== Simulation achat d'une voiture ===");
        BigDecimal income = readPositiveAmount("Revenu mensuel net (€) : ", "Le revenu mensuel");
        if (income == null) return;
        BigDecimal price = readPositiveAmount("Prix de la voiture (€) : ", "Le prix de la voiture");
        if (price == null) return;

        try {
            printCarPurchaseSimulation(service.simulateCarPurchase(income, price));
        } catch (ArithmeticException e) {
            System.out.println("Calcul impossible : la durée estimée est trop grande. Vérifiez les montants saisis.");
        }
    }

    private void simulateMortgage() {
        System.out.println("\n=== Simulation prêt immobilier ===");
        BigDecimal income = readPositiveAmount("Revenu mensuel net (€) : ", "Le revenu mensuel");
        if (income == null) return;
        BigDecimal rate = readRatePercent();
        if (rate == null) return;
        Integer years = readDurationYears();
        if (years == null) return;

        printMortgageSimulation(service.simulateMortgage(income, rate, years));
    }

    /** Reads an annual rate in percent, zero allowed; prints a French error and returns null if invalid. */
    private BigDecimal readRatePercent() {
        System.out.print("Taux d'intérêt annuel (%) : ");
        String text = scanner.nextLine().trim().replace(',', '.');
        try {
            BigDecimal rate = new BigDecimal(text);
            if (rate.signum() < 0) {
                System.out.println("Le taux d'intérêt ne peut pas être négatif.");
                return null;
            }
            if (rate.compareTo(ExpenseService.MAX_ANNUAL_RATE_PERCENT) > 0) {
                System.out.println("Le taux d'intérêt ne peut pas dépasser "
                        + ExpenseService.MAX_ANNUAL_RATE_PERCENT.toPlainString() + " %.");
                return null;
            }
            return rate;
        } catch (NumberFormatException e) {
            System.out.println("Taux invalide : un nombre décimal est attendu (ex. 3.5).");
            return null;
        }
    }

    /** Reads a whole number of years greater than zero; prints a French error and returns null if invalid. */
    private Integer readDurationYears() {
        System.out.print("Durée du prêt (années) : ");
        String text = scanner.nextLine().trim();
        try {
            int years = Integer.parseInt(text);
            if (years <= 0) {
                System.out.println("La durée doit être strictement supérieure à zéro.");
                return null;
            }
            if (years > ExpenseService.MAX_LOAN_DURATION_YEARS) {
                System.out.println("La durée ne peut pas dépasser " + ExpenseService.MAX_LOAN_DURATION_YEARS + " ans.");
                return null;
            }
            return years;
        } catch (NumberFormatException e) {
            System.out.println("Durée invalide : un nombre entier d'années est attendu (ex. 20).");
            return null;
        }
    }

    private void printMortgageSimulation(MortgageSimulation r) {
        System.out.println();
        System.out.println("Revenu mensuel net : " + formatAmount(r.monthlyIncome()) + " €");
        System.out.println("Dépenses mensuelles moyennes : " + formatAmount(r.averageMonthlyExpenses()) + " €");
        if (!r.hasExpenseHistory()) {
            System.out.println("(Aucun historique de dépenses : dépenses mensuelles considérées comme nulles.)");
        }
        System.out.println("Dépense logement mensuelle : " + formatAmount(r.monthlyHousingExpense()) + " €");
        System.out.println("Épargne mensuelle disponible : " + formatAmount(r.monthlySavings()) + " €");
        if (r.monthlySavings().signum() < 0) {
            System.out.println("(Épargne négative : ramenée à 0. La capacité affichée peut être surestimée.)");
        }
        System.out.println();
        System.out.println("Mensualité souhaitée : " + formatAmount(r.desiredMonthlyPayment()) + " €");
        System.out.println("Plafond d'endettement (35 % du revenu) : " + formatAmount(r.debtCeiling()) + " €");
        if (r.capped()) {
            System.out.println("(Plafond d'endettement atteint : la mensualité est limitée à 35 % du revenu.)");
        }
        System.out.println("Mensualité retenue : " + formatAmount(r.monthlyPayment()) + " €");
        System.out.println("Taux : " + r.annualRatePercent().toPlainString() + " % sur " + r.durationYears() + " ans");
        System.out.println();
        if (r.monthlyPayment().signum() == 0) {
            System.out.println("Aucune mensualité possible : le montant empruntable est de 0.00 €.");
        } else {
            System.out.println("Montant maximum empruntable : " + formatAmount(r.borrowingCapacity()) + " €");
        }
    }

    /** Reads an amount greater than zero; prints a French error and returns null if it is invalid. */
    private BigDecimal readPositiveAmount(String prompt, String label) {
        System.out.print(prompt);
        String text = scanner.nextLine().trim().replace(',', '.');
        try {
            BigDecimal amount = new BigDecimal(text);
            if (amount.signum() <= 0) {
                System.out.println(label + " doit être strictement supérieur à zéro.");
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            System.out.println("Montant invalide : un nombre décimal est attendu (ex. 3000.50).");
            return null;
        }
    }

    private void printCarPurchaseSimulation(CarPurchaseSimulation r) {
        System.out.println();
        System.out.println("Revenu mensuel net : " + formatAmount(r.monthlyIncome()) + " €");
        System.out.println("Dépenses mensuelles moyennes : " + formatAmount(r.averageMonthlyExpenses()) + " €");
        if (!r.hasExpenseHistory()) {
            System.out.println("(Aucun historique de dépenses : dépenses mensuelles considérées comme nulles.)");
        }
        System.out.println("Épargne mensuelle disponible : " + formatAmount(r.monthlySavings()) + " €");
        System.out.println();
        System.out.println("Prix de la voiture : " + formatAmount(r.carPrice()) + " €");
        System.out.println();
        r.monthsRequired().ifPresentOrElse(
                months -> System.out.println("Durée estimée : " + months + " mois"),
                () -> System.out.println("Achat impossible par l'épargne : vos dépenses moyennes atteignent ou dépassent votre revenu."));
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
