import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/**
 * Farm Life Inc. - Simple GUI Tracker
 * 
 * Purpose: A farm management system that tracks inventory, services, 
 * animals, sales, and customer information.
 *created by  Mattea Isley, Demetrius Johnson, and Teresa Hearn
 *Assisted by ChatGPT and Claude AI
 * 
 * Main Features:
 * 1. Inventory - Track supplies and equipment
 * 2. Services - Manage farm services and schedules
 * 3. Animals - Monitor livestock and sales
 * 4. Customers - Manage customer data and loyalty
 * 5. Invoices - Handle sales and transactions
 * 
 * Technical Notes:
 * - Uses CSV files for data storage
 * - Date format: MM-DD-YYYY
 * - GUI-based interface
 */
public class Main {

    /**
     * CSV Store Implementation
     * ----------------------
     * Handles all file operations for saving and loading data.
     * Each type of data (inventory, services, animals, etc.)
     * has its own CSV file. Includes basic file operations
     * for reading and writing data safely.
     */
    // ---------- Simple CSV Store ----------
    static class Store {
        static final String INV_FILE  = "inventory.csv";
        static final String SVC_FILE  = "services.csv";
        static final String ANM_FILE  = "animals.csv";
        static final String CUST_FILE = "customers.csv";
        static final String INV2_FILE = "invoices.csv";

        static void loadAll() {
            InventoryManager.loadInventory();
            ServicesManager.loadServices();
            AnimalsManager.loadAnimals();
            SalesManager.loadCustomers();
            SalesManager.loadInvoices();
        }

        static void saveAll() {
            InventoryManager.saveInventory();
            ServicesManager.saveServices();
            AnimalsManager.saveAnimals();
            SalesManager.saveCustomers();
            SalesManager.saveInvoices();
        }


        // Header-aware CSV methods
        static java.util.List<String> readDataLines(String file) {
            try { 
                java.util.List<String> allLines = Files.readAllLines(Paths.get(file));
                // Skip the first line (header) if file has content
                if (allLines.size() > 1) {
                    return allLines.subList(1, allLines.size());
                } else {
                    return new ArrayList<>();
                }
            }
            catch (IOException e) { return new ArrayList<>(); }
        }

        static void writeDataLines(String file, String header, java.util.List<String> dataLines) {
            try { 
                java.util.List<String> allLines = new ArrayList<>();
                allLines.add(header);  // Add header as first line
                allLines.addAll(dataLines);  // Add all data lines
                Files.write(Paths.get(file), allLines); 
            }
            catch (IOException e) { 
                System.err.println("Error writing to file " + file + ": " + e.getMessage());
            }
        }

        // CSV Headers for each file type
        static final String ANIMALS_HEADER = "Name,Species,Gender,Age,Value,Breeder";
        static final String SERVICES_HEADER = "Service,CustomerName,AnimalName,Date,Time,Fee,Status";
        static final String INVENTORY_HEADER = "Item,CostPrice,SalePrice,Quantity";
        static final String CUSTOMERS_HEADER = "CustomerID,Phone,Name,Email,LoyaltyPoints";
        static final String INVOICES_HEADER = "InvoiceNumber,CustomerID,Date,Subtotal,Tax,Total";
    }

    /**
     * Shared Utilities
     * ---------------
     * Common helper functions used across all modules.
     * Includes tools for data safety (CSV formatting),
     * string cleaning, and number parsing with error handling.
     */
    // ---------- Shared Utilities ----------
    static String escape(String s) {
        if (s == null) return "";
        return s.replace(",", ";");
    }

    static String unescape(String s) {
        return (s == null) ? "" : s;
    }

    static double parseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return 0.0; }
    }

    static int parseInt(String s) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return 0; }
    }

    static String clean(String s) {
        return (s == null) ? "" : escape(s.trim());
    }

    /**
     * UI Method Delegates
     * ------------------
     * Static methods that delegate to the UI class for backward compatibility.
     * This allows existing code to continue working while using the new UI class.
     */
    // ---------- UI Delegates ----------
    static String prompt(String msg, ImageIcon logo) { return UI.prompt(msg, logo); }
    static String promptDefault(String msg, String def, ImageIcon logo) { return UI.promptDefault(msg, def, logo); }
    static void info(String msg, ImageIcon logo) { UI.info(msg, logo); }
    static void error(String msg, ImageIcon logo) { UI.error(msg, logo); }
    static boolean askYesNo(String msg, ImageIcon logo) { return UI.askYesNo(msg, logo); }

    static void showLarge(String text, String title, ImageIcon logo) { UI.showLarge(text, title, logo); }
    static int askInt(String msg, ImageIcon logo) { return UI.askInt(msg, logo); }
    static double askDouble(String msg, ImageIcon logo) { return UI.askDouble(msg, logo); }
    static int pickIndex(String msg, String listText, int max, ImageIcon logo) { return UI.pickIndex(msg, listText, max, logo); }
    static String promptDateMDY(ImageIcon logo) { return UI.promptDateMDY(logo); }

    static int selectFromDropdown(String msg, String[] options, ImageIcon logo) { return UI.selectFromDropdown(msg, options, logo); }

    // Customer selection delegates
    static String selectCustomerName(String purpose, ImageIcon logo) { return UI.selectCustomerName(purpose, logo); }


    // Logo delegates
    static ImageIcon createLogo() { return UI.createLogo(); }

    /**
     * Main Menu
     * ---------
     * Central menu system that provides access to all 
     * farm management functions. Handles data loading 
     * on startup, user navigation, and saving on exit.
     * Includes basic error recovery and clean shutdown.
     */
    // ---------- Main Menu ----------
    public static void main(String[] args) {
        Store.loadAll();
        ImageIcon logo = createLogo();

        while (true) {
            // Calculate responsive dimensions based on screen size
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int menuWidth = Math.min(500, (int)(screenSize.width * 0.4));
            int menuHeight = Math.min(400, (int)(screenSize.height * 0.5));
            
            // Create main menu panel using UI class
            JPanel root = UI.createMainMenuPanel(menuWidth, menuHeight);
            
            // Get buttons from the panel
            JButton[] buttons = (JButton[]) root.getClientProperty("buttons");
            JButton bInv = buttons[0], bSvc = buttons[1], bAni = buttons[2];
            JButton bCust = buttons[3], bInvs = buttons[4], bExit = buttons[5];
            
            // Create dialog
            JOptionPane pane = new JOptionPane(
                    root,
                    JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.DEFAULT_OPTION,
                    null,
                    new Object[]{}, 
                    null
            );
            JDialog dlg = pane.createDialog(null, "Main Menu");
            dlg.setModal(true);
            dlg.setResizable(false);

            // Record choice, then close
            Runnable close = dlg::dispose;
            bInv.addActionListener(_  -> { pane.setValue("INV");  close.run(); });
            bSvc.addActionListener(_  -> { pane.setValue("SVC");  close.run(); });
            bAni.addActionListener(_  -> { pane.setValue("ANI");  close.run(); });
            bCust.addActionListener(_ -> { pane.setValue("CUST"); close.run(); });
            bInvs.addActionListener(_ -> { pane.setValue("INVS"); close.run(); });
            bExit.addActionListener(_ -> { pane.setValue("EXIT"); close.run(); });

            dlg.setLocationRelativeTo(null);
            dlg.setVisible(true);

            // Handle results
            Object choice = pane.getValue();

        if (choice == null) { // window X
            if (askYesNo("Exit without saving?", logo)) break;
            else continue;
        }

        try {
            switch (String.valueOf(choice)) {
                case "INV"  -> InventoryManager.inventoryMenu(logo);
                case "SVC"  -> ServicesManager.servicesMenu(logo);
                case "ANI"  -> AnimalsManager.animalsMenu(logo);
                case "CUST" -> SalesManager.customersMenu(logo);
                case "INVS" -> SalesManager.invoicesMenu(logo);
                case "EXIT" -> {
                    Store.saveAll();
                    info("Data saved. Goodbye!", logo);
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            error("Error: " + e.getMessage(), logo);
            System.err.println("Error occurred: " + e.getMessage());
        }
    }
}
}