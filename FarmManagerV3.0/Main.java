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
     * GUI Dialogs
     * -----------
     * Collection of standard dialog boxes for user interaction.
     * Includes input prompts, messages, confirmations, and 
     * data entry with validation. All dialogs use consistent
     * styling and error handling.
     */
    // ---------- GUI Dialogs ----------
    static String prompt(String msg, ImageIcon logo) {
        return (String) JOptionPane.showInputDialog(null, msg, "Farm Life Inc.", JOptionPane.PLAIN_MESSAGE, logo, null, "");
    }

    static String promptDefault(String msg, String def, ImageIcon logo) {
        String in = (String) JOptionPane.showInputDialog(null, msg, "Farm Life Inc.", JOptionPane.PLAIN_MESSAGE, logo, null, def);
        return (in == null) ? null : in;
    }

    static void info(String msg, ImageIcon logo) {
        JOptionPane.showMessageDialog(null, msg, "Farm Life Inc.", JOptionPane.INFORMATION_MESSAGE, logo);
    }

    static void error(String msg, ImageIcon logo) {
        JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static boolean askYesNo(String msg, ImageIcon logo) {
        int res = JOptionPane.showConfirmDialog(null, msg, "Farm Life Inc.", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, logo);
        return (res == JOptionPane.YES_OPTION);
    }

    static void showLarge(String text, String title, ImageIcon logo) {
        JTextArea ta = new JTextArea(text, 18, 65);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(ta);
        JOptionPane.showMessageDialog(null, sp, title, JOptionPane.PLAIN_MESSAGE, logo);
    }

    static int askInt(String msg, ImageIcon logo) {
        String in = prompt(msg, logo);
        if (in == null || in.isBlank()) return 0;
        return parseInt(in.trim());
    }

    static double askDouble(String msg, ImageIcon logo) {
        String in = prompt(msg, logo);
        if (in == null || in.isBlank()) return 0.0;
        return parseDouble(in.trim());
    }

    static int pickIndex(String msg, String listText, int max, ImageIcon logo) {
        if (max == 0) { error("No items available.", logo); return -1; }
        showLarge(listText, "Selection", logo);
        int idx = askInt(msg + " (0-based ID):", logo);
        if (idx < 0 || idx >= max) { error("Invalid ID.", logo); return -1; }
        return idx;
    }

    static String promptDateMDY(ImageIcon logo) {
        String last = "";
        while (true) {
            String in = promptDefault("New date (MM-DD-YYYY):", last, logo);
            if (in == null) throw new RuntimeException("Cancelled");
            in = clean(in);
            if (in.matches("\\d{2}-\\d{2}-\\d{4}")) return in;
            error("Please use Month-Day-Year as MM-DD-YYYY (e.g., 10-24-2025).", logo);
            last = in;
        }
    }

    static int selectFromDropdown(String msg, String[] options, ImageIcon logo) {
        if (options == null || options.length == 0) {
            error("No options available.", logo);
            return -1;
        }

        Object selected = JOptionPane.showInputDialog(
            null,
            msg,
            "Farm Life Inc. - Selection",
            JOptionPane.PLAIN_MESSAGE,
            logo,
            options,
            options[0]
        );

        if (selected == null) return -1; // User cancelled
        
        // Find the index of the selected option
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(selected)) {
                return i;
            }
        }
        return -1;
    }

    // Enhanced customer selection with search and new customer options
    static String selectCustomerName(String purpose, ImageIcon logo) {
        if (SalesManager.customers.isEmpty()) {
            String name = clean(prompt("Customer name (new customer):", logo));
            return (name == null || name.isEmpty()) ? null : name;
        }

        // Initial choice: Search, Browse All, or New Customer
        String[] mainOptions = {
            "Search for existing customer", 
            "Browse all customers (" + SalesManager.customers.size() + " total)", 
            "Enter new customer name"
        };
        
        int choice = selectFromDropdown("How would you like to select customer for " + purpose + "?", 
                                       mainOptions, logo);
        if (choice == -1) return null; // User cancelled
        
        return switch (choice) {
            case 0 -> // Search
                searchAndSelectCustomer(logo);
            case 1 -> // Browse all
                browseAllCustomers(logo);
            case 2 -> { // New customer
                String name = clean(prompt("Enter new customer name:", logo));
                yield (name == null || name.isEmpty()) ? null : name;
            }
            default -> null;
        };
    }

    static String searchAndSelectCustomer(ImageIcon logo) {
        while (true) {
            String searchTerm = clean(prompt("Enter customer name or phone to search:", logo));
            if (searchTerm == null) return null; // User cancelled
            
            if (searchTerm.isEmpty()) {
                error("Please enter a search term.", logo);
                continue;
            }
            
            // Find matching customers
            java.util.List<SalesManager.Customer> matches = new ArrayList<>();
            String searchLower = searchTerm.toLowerCase();
            
            for (SalesManager.Customer c : SalesManager.customers) {
                if (c.name.toLowerCase().contains(searchLower) || 
                    c.phone.contains(searchTerm) ||
                    c.id.contains(searchTerm)) {
                    matches.add(c);
                }
            }
            
            if (matches.isEmpty()) {
                boolean tryAgain = askYesNo("No customers found matching '" + searchTerm + 
                                          "'.\nTry a different search?", logo);
                if (!tryAgain) {
                    // Offer to create new customer
                    boolean createNew = askYesNo("Create new customer with name '" + searchTerm + "'?", logo);
                    return createNew ? searchTerm : null;
                }
                continue;
            }
            
            if (matches.size() == 1) {
                // Only one match, confirm selection
                SalesManager.Customer c = matches.get(0);
                boolean confirm = askYesNo("Select customer: " + c.name + 
                                         " (ID: " + c.id + ", Phone: " + c.phone + ")?", logo);
                return confirm ? c.name : null;
            }
            
            // Multiple matches, let user choose
            String[] matchOptions = new String[matches.size() + 1];
            for (int i = 0; i < matches.size(); i++) {
                SalesManager.Customer c = matches.get(i);
                matchOptions[i] = c.name + " (ID: " + c.id + " - Phone: " + c.phone + ")";
            }
            matchOptions[matches.size()] = "Search again";
            
            int selection = selectFromDropdown("Found " + matches.size() + " customers:", 
                                             matchOptions, logo);
            if (selection == -1) return null; // User cancelled
            
            if (selection < matches.size()) {
                return matches.get(selection).name;
            }
            // Otherwise, search again (continue loop)
        }
    }

    static String browseAllCustomers(ImageIcon logo) {
        if (SalesManager.customers.size() > 20) {
            boolean proceed = askYesNo("You have " + SalesManager.customers.size() + 
                                     " customers. This may take time to browse.\n" +
                                     "Consider using search instead. Continue browsing?", logo);
            if (!proceed) return null;
        }
        
        String[] customerOptions = new String[SalesManager.customers.size() + 1];
        for (int i = 0; i < SalesManager.customers.size(); i++) {
            SalesManager.Customer c = SalesManager.customers.get(i);
            customerOptions[i] = c.name + " (ID: " + c.id + " - Phone: " + c.phone + ")";
        }
        customerOptions[SalesManager.customers.size()] = "Enter new customer name instead";
        
        int selectedIndex = selectFromDropdown("Select customer:", customerOptions, logo);
        if (selectedIndex == -1) return null; // User cancelled
        
        if (selectedIndex < SalesManager.customers.size()) {
            return SalesManager.customers.get(selectedIndex).name;
        } else {
            // Enter new customer
            String name = clean(prompt("Enter new customer name:", logo));
            return (name == null || name.isEmpty()) ? null : name;
        }
    }


    static ImageIcon createLogo() {
        // Load the image file (place it in your project resources folder or same directory)
        String imagePath = "farmLifeIcon.png"; // <-- rename to match your actual image file

        // Load the image
        ImageIcon icon = new ImageIcon(imagePath);

        // Optionally scale it down (64x64 for JOptionPane or menu icon)
        Image scaled = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);

        return new ImageIcon(scaled);
    }

    static JLabel createResponsiveLogo(int maxWidth, int maxHeight) {
        try {
            // Load the image file
            String imagePath = "farmLifeIcon.png";
            ImageIcon icon = new ImageIcon(imagePath);
            
            // If image doesn't exist, create a placeholder
            if (icon.getIconWidth() <= 0) {
                // Create a simple colored rectangle as placeholder
                java.awt.image.BufferedImage placeholder = new java.awt.image.BufferedImage(
                    maxWidth, maxHeight/3, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2d = placeholder.createGraphics();
                g2d.setColor(new java.awt.Color(34, 139, 34)); // Forest green
                g2d.fillRect(0, 0, maxWidth, maxHeight/3);
                g2d.setColor(java.awt.Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, maxWidth/20)));
                String text = "FARM LIFE";
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (maxWidth - fm.stringWidth(text)) / 2;
                int textY = ((maxHeight/3) + fm.getAscent()) / 2;
                g2d.drawString(text, textX, textY);
                g2d.dispose();
                
                icon = new ImageIcon(placeholder);
            } else {
                // Scale existing image to fit within maxWidth and maxHeight/3
                int logoHeight = Math.min(maxHeight / 3, 120); // Max 120px height
                int logoWidth = (icon.getIconWidth() * logoHeight) / icon.getIconHeight();
                if (logoWidth > maxWidth) {
                    logoWidth = maxWidth;
                    logoHeight = (icon.getIconHeight() * logoWidth) / icon.getIconWidth();
                }
                
                Image scaled = icon.getImage().getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaled);
            }
            
            JLabel logoLabel = new JLabel(icon);
            logoLabel.setHorizontalAlignment(JLabel.CENTER);
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            return logoLabel;
            
        } catch (Exception e) {
            // Fallback to text label if image loading fails
            JLabel textLogo = new JLabel("🚜 FARM LIFE INC 🌾");
            textLogo.setFont(new Font("SansSerif", Font.BOLD, Math.max(16, maxWidth/25)));
            textLogo.setHorizontalAlignment(JLabel.CENTER);
            textLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
            textLogo.setForeground(new java.awt.Color(34, 139, 34));
            return textLogo;
        }
    }

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
        // ===== 1) Build vertical content =====
        // Calculate responsive dimensions based on screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int menuWidth = Math.min(500, (int)(screenSize.width * 0.4));
        int menuHeight = Math.min(400, (int)(screenSize.height * 0.5));
        
        JLabel logoLabel = createResponsiveLogo(menuWidth, menuHeight);

        JLabel titleLabel = new JLabel(" +=========== Farm Life Inc. ===========+ ");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        JSeparator sep = new JSeparator();
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        sep.setMaximumSize(new Dimension(menuWidth - 40, 2)); 

        JLabel instructionLabel = new JLabel("Select an option:");
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionLabel.setHorizontalAlignment(JLabel.CENTER);

        // ===== 2) Buttons (two rows) =====
        JButton bInv   = new JButton("Inventory");
        JButton bSvc   = new JButton("Services");
        JButton bAni   = new JButton("Animals");
        JButton bCust  = new JButton("Customers");
        JButton bInvs  = new JButton("Invoices");
        JButton bExit  = new JButton("Exit");

        // Make all buttons the same size using their natural preferred sizes
        JButton[] all = { bInv, bSvc, bAni, bCust, bInvs, bExit };
        int maxW = 0, maxH = 0;
        for (JButton b : all) {
            Dimension d = b.getPreferredSize(); 
            maxW = Math.max(maxW, d.width);
            maxH = Math.max(maxH, d.height);
        }
        Dimension uni = new Dimension(maxW, maxH);
        for (JButton b : all) {
            b.setPreferredSize(uni);
            b.setMinimumSize(uni);
            b.setMaximumSize(uni);
        }

        // Row 1: Inventory, Services, Animals  (CENTER aligned)
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row1.add(bInv); row1.add(bSvc); row1.add(bAni);
        row1.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Row 2: Customers, Invoices, Exit     (CENTER aligned)
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row2.add(bCust); row2.add(bInvs); row2.add(bExit);
        row2.setAlignmentX(Component.CENTER_ALIGNMENT);

        
        // ===== Root vertical container =====
        JPanel root = new JPanel();
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.add(logoLabel);                 // icon first (left)
        root.add(Box.createVerticalStrut(6));
        root.add(titleLabel);                // then title (left)
        root.add(Box.createVerticalStrut(6));
        root.add(sep);                       // separator
        root.add(Box.createVerticalStrut(10));
        root.add(instructionLabel);          // "Select an option:"
        root.add(Box.createVerticalStrut(10));
        root.add(row1);                      // buttons row 1
        root.add(Box.createVerticalStrut(8));
        root.add(row2);                      // buttons row 2
        
        JOptionPane pane = new JOptionPane(
                root,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                new Object[]{}, // no default buttons
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

        // ===== 4) Handle results exactly like your previous switch =====
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