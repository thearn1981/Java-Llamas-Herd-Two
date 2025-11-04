import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * Sales and Customer Management
 * --------------------------
 * Manages customer relationships and sales transactions.
 * Handles customer records, loyalty points tracking,
 * and invoice generation for purchases.
 */
public class SalesManager {
    static ArrayList<Customer> customers = new ArrayList<>();
    static ArrayList<Invoice> invoices = new ArrayList<>();
    static double currentTaxRate = 0.065;  // Current tax rate (6.5%)

    /**
     * Sales Data Models
     * ---------------
     * Core data structures for customer information
     * and sales records. Supports customer loyalty
     * program and invoice tracking.
     */
    static class Customer {
        String id;      // stable unique id (<=5 digits)
        String phone;   // editable & searchable
        String name;
        String email;
        int points;

        Customer(String id, String phone, String name, String email, int points) {
            this.id = id;
            this.phone = phone;
            this.name = name;
            this.email = email;
            this.points = points;
        }

        String toCSV() {
            return Main.escape(id) + "," + Main.escape(phone) + "," +
                   Main.escape(name) + "," + Main.escape(email) + "," + points;
        }

        static Customer fromCSV(String line) {
            String[] p = line.split(",", -1);
            // Back-compat: old format phone,name,email,points
            if (p.length == 4) {
                String generatedId = genCustomerId();
                return new Customer(generatedId, Main.unescape(p[0]),
                    Main.unescape(p[1]), Main.unescape(p[2]), Main.parseInt(p[3]));
            } else if (p.length >= 5) {
                return new Customer(Main.unescape(p[0]), Main.unescape(p[1]),
                    Main.unescape(p[2]), Main.unescape(p[3]), Main.parseInt(p[4]));
            }
            return null;
        }
    }

    static class Invoice {
        String invoiceId;
        String customerId;   // stable reference
        String dateMDY;      // MM-DD-YYYY
        double subTotal;
        double tax;
        double total;

        Invoice(String invoiceId, String customerId, String dateMDY,
               double subTotal, double tax, double total) {
            this.invoiceId = invoiceId;
            this.customerId = customerId;
            this.dateMDY = dateMDY;
            this.subTotal = subTotal;
            this.tax = tax;
            this.total = total;
        }

        String toCSV() {
            return Main.escape(invoiceId) + "," + Main.escape(customerId) + "," +
                   Main.escape(dateMDY) + "," + subTotal + "," + tax + "," + total;
        }

        static Invoice fromCSV(String line) {
            String[] p = line.split(",", -1);
            if (p.length < 6) return null;

            String token = Main.unescape(p[1]).trim();
            String customerId = token;

            // Prefer interpreting as an ID if it matches a known customer
            Customer byId = findCustomerById(token);
            if (byId != null) {
                customerId = byId.id;
            } else {
                // Back-compat: maybe it's an old phone value
                Customer byPhone = findCustomerByPhone(token);
                if (byPhone != null) customerId = byPhone.id;
            }

            // Parse: InvoiceNumber,CustomerID,Date,Subtotal,Tax,Total
            // Note: Old CSV format had "Discount" instead of "Tax" but we treat it as tax
            return new Invoice(Main.unescape(p[0]), customerId, Main.unescape(p[2]),
                Main.parseDouble(p[3]), Main.parseDouble(p[4]), Main.parseDouble(p[5]));
        }
    }

    /**
     * File Operations
     * --------------
     * Handles persistence of customer and invoice data.
     * Manages CSV file storage with backward compatibility
     * for legacy data formats.
     */
    static void loadCustomers() {
        customers.clear();
        for (String line : Main.Store.readDataLines(Main.Store.CUST_FILE)) {
            Customer c = Customer.fromCSV(line);
            if (c != null) customers.add(c);
        }
    }

    static void saveCustomers() {
        ArrayList<String> lines = new ArrayList<>();
        for (Customer c : customers) lines.add(c.toCSV());
        Main.Store.writeDataLines(Main.Store.CUST_FILE, Main.Store.CUSTOMERS_HEADER, lines);
    }

    static void loadInvoices() {
        invoices.clear();
        for (String line : Main.Store.readDataLines(Main.Store.INV2_FILE)) {
            Invoice inv = Invoice.fromCSV(line);
            if (inv != null) invoices.add(inv);
        }
    }

    static void saveInvoices() {
        ArrayList<String> lines = new ArrayList<>();
        for (Invoice inv : invoices) lines.add(inv.toCSV());
        Main.Store.writeDataLines(Main.Store.INV2_FILE, Main.Store.INVOICES_HEADER, lines);
    }

    /**
     * Customer Helper Functions
     * ----------------------
     * Utility functions for customer management.
     * Handles ID generation, customer lookup,
     * and data validation operations.
     */
    static String genCustomerId() {
        return genShortNumericId();
    }

    static String genShortNumericId() {
        java.util.Random r = new java.util.Random();
        for (int attempts = 0; attempts < 1000; attempts++) {
            int n = r.nextInt(100000);
            String id = Integer.toString(n);
            if (!customerIdExists(id)) return id;
        }
        String id = Long.toString(System.nanoTime() % 100000);
        if (!customerIdExists(id)) return id;
        return "0";
    }

    static boolean customerIdExists(String id) {
        for (Customer c : customers) if (c.id.equals(id)) return true;
        return false;
    }

    static Customer findCustomerByPhone(String phone) {
        for (Customer c : customers) if (c.phone.equalsIgnoreCase(phone)) return c;
        return null;
    }

    static Customer findCustomerById(String id) {
        for (Customer c : customers) if (c.id.equals(id)) return c;
        return null;
    }

    static Customer findCustomerByName(String name) {
        for (Customer c : customers) {
            if (c.name.equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    static Customer createQuickCustomer(String name, ImageIcon logo) {
        try {
            String phone = Main.clean(Main.prompt("Phone number for " + name + ":", logo));
            if (phone == null) return null;
            
            String email = Main.clean(Main.prompt("Email for " + name + " (optional):", logo));
            if (email == null) email = "";
            
            // Check if phone already exists
            if (findCustomerByPhone(phone) != null) {
                Main.error("A customer with that phone number already exists.", logo);
                return null;
            }
            
            Customer newCustomer = new Customer(genCustomerId(), phone, name, email, 0);
            customers.add(newCustomer);
            Main.info("Customer account created successfully!\nID: " + newCustomer.id, logo);
            return newCustomer;
            
        } catch (Exception e) {
            Main.error("Error creating customer: " + e.getMessage(), logo);
            return null;
        }
    }

    static Customer selectCustomerForEditing(ImageIcon logo) {
        if (customers.size() > 15) {
            // For many customers, offer search option
            String[] options = {"Search for customer", "Browse all customers (" + customers.size() + " total)"};
            int choice = Main.selectFromDropdown("How would you like to find the customer?", options, logo);
            if (choice == -1) return null; // User cancelled
            
            if (choice == 0) {
                // Search mode
                String customerName = UI.searchAndSelectCustomer(logo);
                if (customerName == null) return null;
                return findCustomerByName(customerName);
            }
        }
        
        // Browse all customers (default for <= 15 customers or when requested)
        String[] customerOptions = new String[customers.size()];
        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);
            customerOptions[i] = c.name + " (ID: " + c.id + " - Phone: " + c.phone + ")";
        }
        
        int selectedIndex = Main.selectFromDropdown("Select customer to edit:", customerOptions, logo);
        if (selectedIndex == -1) return null; // User cancelled
        
        return customers.get(selectedIndex);
    }

    static String genInvoiceId() {
        java.util.Random r = new java.util.Random();
        for (int attempts = 0; attempts < 1000; attempts++) {
            int n = r.nextInt(100000); // 0 to 99999 (up to 5 digits)
            String id = "INV-" + n;
            if (!invoiceIdExists(id)) return id;
        }
        // Fallback if somehow all numbers are taken
        String id = "INV-" + (System.nanoTime() % 100000);
        if (!invoiceIdExists(id)) return id;
        return "INV-0"; // Last resort
    }

    static boolean invoiceIdExists(String invoiceId) {
        for (Invoice inv : invoices) if (inv.invoiceId.equals(invoiceId)) return true;
        return false;
    }

    
    static void customersMenu(ImageIcon logo) {
        while (true) {

            
            String choice = UI.showCustomersMenu(logo);
            if (choice == null || "BACK".equals(choice)) break;

            try {
                switch (choice) {
                    case "ADD" -> addCustomer(logo);
                    case "EDIT" -> editCustomer(logo);
                    case "REM" -> removeCustomer(logo);
                    case "LIST" -> listCustomers(logo);
                    case "POINTS" -> manageLoyaltyPoints(logo);
                }
            } catch (Exception e) {
                UI.error("Error: " + e.getMessage(), logo);
            }
        }
    }


    /**

            JLabel instructionLabel = new JLabel("Select:");
            instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            instructionLabel.setHorizontalAlignment(JLabel.CENTER);



            // Uniform sizes
            JButton[] all = { bAdd, bEdit, bRem, bList, bPoints, bBack };
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

            // Row 1: Add, Edit, Remove (CENTER aligned)
            JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
            row1.add(bAdd); row1.add(bEdit); row1.add(bRem);
            row1.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Row 2: List, Points, Back (CENTER aligned)
            JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
            row2.add(bList); row2.add(bPoints); row2.add(bBack);
            row2.setAlignmentX(Component.CENTER_ALIGNMENT);

            // ===== Root vertical content =====
            JPanel root = new JPanel();
            root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.add(logoLabel);
            root.add(Box.createVerticalStrut(6));
            root.add(titleLabel);
            root.add(Box.createVerticalStrut(6));
            root.add(sep);
            root.add(Box.createVerticalStrut(10));
            root.add(instructionLabel);
            root.add(Box.createVerticalStrut(10));
            root.add(row1);
            root.add(Box.createVerticalStrut(8));
            root.add(row2);

            // ===== JOptionPane with custom content =====
            JOptionPane pane = new JOptionPane(
                    root,
                    JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.DEFAULT_OPTION,
                    null,             // no default icon here; we show the logo at top instead
                    new Object[]{},   // no default one-line buttons
                    null
            );
            JDialog dlg = pane.createDialog(null, "Customers Menu");
            dlg.setModal(true);
            dlg.setResizable(false);

            // Actions: set pane value then close
            Runnable close = dlg::dispose;

        }
    }


    /**
     * Customer Management Actions
     * ------------------------
     * Core operations for customer data.
     * Handles adding new customers, editing details,
     * and managing customer records with validation.
     */
    static void addCustomer(ImageIcon logo) {
        try {
            String phone = Main.clean(Main.prompt("Customer phone (unique):", logo));
            if (phone == null || phone.isEmpty()) return;
            
            if (findCustomerByPhone(phone) != null) {
                Main.error("Phone already exists.", logo);
                return;
            }
            
            String name = Main.clean(Main.prompt("Customer name:", logo));
            if (name == null) return;
            
            String email = Main.clean(Main.prompt("Customer email:", logo));
            if (email == null) return;
            
            customers.add(new Customer(genCustomerId(), phone, name, email, 0));
            Main.info("Customer added.", logo);
        } catch (Exception e) {
            Main.error("Could not add customer: " + e.getMessage(), logo);
        }
    }

    static void editCustomer(ImageIcon logo) {
        if (customers.isEmpty()) {
            Main.info("No customers yet.", logo);
            return;
        }
        
        Customer c = selectCustomerForEditing(logo);
        if (c == null) return; // User cancelled

        String newName = Main.promptDefault("Name:", c.name, logo);
        if (newName == null) return;
        
        String newPhone = Main.promptDefault("Phone:", c.phone, logo);
        if (newPhone == null) return;
        
        String newEmail = Main.promptDefault("Email:", c.email, logo);
        if (newEmail == null) return;

        // Loyalty Points Management
        String pointsChoice = (String) JOptionPane.showInputDialog(
            null,
            "Current Loyalty Points: " + c.points + "\n\nChoose points action:",
            "Farm Life Inc. - Loyalty Points",
            JOptionPane.PLAIN_MESSAGE,
            logo,
            new String[]{"Keep current points", "Set new total", "Add points", "Subtract points"},
            "Keep current points"
        );
        
        if (pointsChoice == null) return; // User cancelled
        
        int newPoints = switch (pointsChoice) {
            case "Set new total" -> {
                String totalStr = Main.promptDefault("Set total points to:", String.valueOf(c.points), logo);
                if (totalStr == null) yield c.points;
                yield Math.max(0, Main.parseInt(totalStr.trim()));
            }
            case "Add points" -> {
                int addPoints = Main.askInt("Points to add:", logo);
                if (addPoints > 0) {
                    Main.info("Added " + addPoints + " points.", logo);
                    yield c.points + addPoints;
                }
                yield c.points;
            }
            case "Subtract points" -> {
                int subtractPoints = Main.askInt("Points to subtract:", logo);
                if (subtractPoints > 0) {
                    int newVal = Math.max(0, c.points - subtractPoints);
                    int actualSubtracted = c.points - newVal;
                    Main.info("Subtracted " + actualSubtracted + " points.", logo);
                    yield newVal;
                }
                yield c.points;
            }
            default -> c.points; // "Keep current points"
        };

        newName = Main.clean(newName);
        newPhone = Main.clean(newPhone);
        newEmail = Main.clean(newEmail);

        if (!newPhone.equalsIgnoreCase(c.phone) && findCustomerByPhone(newPhone) != null) {
            Main.error("Another customer already has that phone.", logo);
            return;
        }

        c.name = newName;
        c.phone = newPhone;
        c.email = newEmail;
        c.points = newPoints;

        Main.info("""
                Customer updated successfully!
                ID: %s
                Name: %s
                Phone: %s
                Email: %s
                Loyalty Points: %d""".formatted(c.id, c.name, c.phone, c.email, c.points), logo);
    }

    static void manageLoyaltyPoints(ImageIcon logo) {
        if (customers.isEmpty()) {
            Main.info("No customers yet.", logo);
            return;
        }
        
        // Create dropdown options array with customer names, IDs, and current points
        String[] customerOptions = new String[customers.size()];
        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);
            customerOptions[i] = c.name + " (ID: " + c.id + " - Current Points: " + c.points + ")";
        }
        
        int selectedIndex = Main.selectFromDropdown("Select customer to manage points:", customerOptions, logo);
        if (selectedIndex == -1) return; // User cancelled
        
        Customer c = customers.get(selectedIndex);
        
        // Points Management Options
        String pointsAction = (String) JOptionPane.showInputDialog(
            null,
            "Customer: " + c.name + "\n" +
            "Current Loyalty Points: " + c.points + "\n\n" +
            "Choose action:",
            "Farm Life Inc. - Loyalty Points Management",
            JOptionPane.PLAIN_MESSAGE,
            logo,
            new String[]{"Add points", "Subtract points", "Set total points", "View history", "Cancel"},
            "Add points"
        );
        
        if (pointsAction == null || "Cancel".equals(pointsAction)) return;
        
        int originalPoints = c.points;
        
        switch (pointsAction) {
            case "Add points" -> {
                int addPoints = Main.askInt("Points to add to " + c.name + ":", logo);
                if (addPoints > 0) {
                    c.points += addPoints;
                    Main.info("Added " + addPoints + " points to " + c.name + ".\n" +
                             "Previous: " + originalPoints + " points\n" +
                             "Current: " + c.points + " points", logo);
                }
            }
            case "Subtract points" -> {
                int subtractPoints = Main.askInt("Points to subtract from " + c.name + ":", logo);
                if (subtractPoints > 0) {
                    int newPoints = Math.max(0, c.points - subtractPoints);
                    int actualSubtracted = c.points - newPoints;
                    c.points = newPoints;
                    Main.info("Subtracted " + actualSubtracted + " points from " + c.name + ".\n" +
                             "Previous: " + originalPoints + " points\n" +
                             "Current: " + c.points + " points", logo);
                }
            }
            case "Set total points" -> {
                String totalStr = Main.promptDefault("Set total points for " + c.name + ":", String.valueOf(c.points), logo);
                if (totalStr != null) {
                    int newTotal = Math.max(0, Main.parseInt(totalStr.trim()));
                    c.points = newTotal;
                    Main.info("Set " + c.name + "'s points to " + newTotal + ".\n" +
                             "Previous: " + originalPoints + " points\n" +
                             "Current: " + c.points + " points", logo);
                }
            }
            case "View history" -> {
                StringBuilder sb = new StringBuilder();
                sb.append("=== Loyalty Points Summary ===\n");
                sb.append("Customer: ").append(c.name).append("\n");
                sb.append("ID: ").append(c.id).append("\n");
                sb.append("Phone: ").append(c.phone).append("\n");
                sb.append("Email: ").append(c.email).append("\n");
                sb.append("Current Points: ").append(c.points).append("\n\n");
                sb.append("Points can be earned through:\n");
                sb.append("• Purchases (automatic)\n");
                sb.append("• Service appointments\n");
                sb.append("• Special promotions\n");
                sb.append("• Manual adjustments\n");
                Main.showLarge(sb.toString(), "Loyalty Points - " + c.name, logo);
            }
        }
    }

    static void removeCustomer(ImageIcon logo) {
        if (customers.isEmpty()) {
            Main.info("No customers yet.", logo);
            return;
        }
        
        // Create dropdown options array with customer names and IDs
        String[] customerOptions = new String[customers.size()];
        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);
            customerOptions[i] = c.name + " (ID: " + c.id + " - Phone: " + c.phone + ")";
        }
        
        int selectedIndex = Main.selectFromDropdown("Select customer to remove:", customerOptions, logo);
        if (selectedIndex == -1) return; // User cancelled
        
        Customer c = customers.get(selectedIndex);
        
        // Confirm removal
        boolean confirm = Main.askYesNo("Are you sure you want to remove customer:\n" + 
                                      c.name + " (ID: " + c.id + ")?", logo);
        if (!confirm) return;
        
        customers.remove(c);
        Main.info("Customer removed.", logo);
    }

    static void listCustomers(ImageIcon logo) {
        if (customers.isEmpty()) {
            Main.info("No customers recorded.", logo);
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-16s %-16s %-20s %-24s %-8s%n",
            "ID", "Phone", "Name", "Email", "Points"));
        sb.append("--------------------------------------------------------------------------\n");
        
        for (Customer c : customers) {
            // Truncate fields if too long
            String id = c.id.length() > 16 ? c.id.substring(0, 13) + "..." : c.id;
            String phone = c.phone.length() > 16 ? c.phone.substring(0, 13) + "..." : c.phone;
            String name = c.name.length() > 20 ? c.name.substring(0, 17) + "..." : c.name;
            String email = c.email.length() > 24 ? c.email.substring(0, 21) + "..." : c.email;
            
            sb.append(String.format("%-16s %-16s %-20s %-24s %-8d%n",
                id, phone, name, email, c.points));
        }
        
        Main.showLarge(sb.toString(), "Customers", logo);
    }

    /**
     * Invoice Management Interface
     * -------------------------
     * Manages sales transactions and invoicing.
     * Provides options to create new invoices
     * and view transaction history.
     */
    static void invoicesMenu(ImageIcon logo) {
        while (true) {
            String choice = UI.showInvoicesMenu(logo);
        if (choice == null || "BACK".equals(choice)) break;

        try {
            switch (choice) {
                case "NEW" -> newInvoice(logo);
                case "LIST" -> listInvoices(logo);
                case "TAX" -> setTaxRate(logo);
            }
        } catch (Exception e) {
            UI.error("Error: " + e.getMessage(), logo);
        }
    }
    }


    /**
     * Invoice Processing Actions
     * -----------------------
     * Core sales transaction functions.
     * Handles invoice creation, item selection,
     * pricing calculations, and loyalty points.
     */
    static void newInvoice(ImageIcon logo) {
        if (InventoryManager.inventory.isEmpty()) {
            Main.info("Add inventory items first.", logo);
            return;
        }
        
        // Use enhanced customer selection
        String customerName = Main.selectCustomerName("invoice", logo);
        if (customerName == null) return; // User cancelled
        
        // Find or create customer
        Customer cust = findCustomerByName(customerName);
        if (cust == null) {
            // New customer - ask if they want to create account
            boolean createAccount = Main.askYesNo("Customer '" + customerName + 
                                                "' is not in our system.\n" +
                                                "Create customer account for future visits?", logo);
            if (createAccount) {
                cust = createQuickCustomer(customerName, logo);
                if (cust == null) return; // Failed to create
            } else {
                // Create temporary customer for this invoice only
                cust = new Customer(genCustomerId(), "N/A", customerName, "N/A", 0);
            }
        }

        String date = Main.promptDateMDY(logo);

        
        List<int[]> selections = new ArrayList<>();
        int itemsCount = 0;
        
        while (true) {
            int idx = Main.pickIndex("Select inventory ID to add (or cancel to stop):",
                InventoryManager.formatInventoryTable(),
                InventoryManager.inventory.size(), logo);
            if (idx < 0) break;
            
            InventoryManager.InventoryItem it = InventoryManager.inventory.get(idx);
            int qty = Main.askInt("Quantity to add for '" + it.name + "':", logo);
            
            if (qty <= 0) {
                Main.error("Quantity must be > 0.", logo);
                continue;
            }
            
            if (qty > it.qtyOnHand) {
                Main.error("Not enough stock. On hand: " + it.qtyOnHand, logo);
                continue;
            }
            
            selections.add(new int[]{idx, qty});
            itemsCount++;
            
            if (!Main.askYesNo("Add another line item?", logo)) break;
        }
        
        if (selections.isEmpty()) {
            Main.info("No items added. Cancelled invoice.", logo);
            return;
        }

        // Totals & optional stock deduction
        double sub = 0;
        for (int[] pair : selections) {
            InventoryManager.InventoryItem it = InventoryManager.inventory.get(pair[0]);
            int qty = pair[1];
            sub += qty * it.salePrice;
        }
        
        double tax = sub * currentTaxRate;
        double total = sub + tax;

        // Loyalty Points Redemption
        double pointsDiscount = 0.0;
        int pointsUsed = 0;
        if (cust.points > 0) {
            double maxPointsValue = cust.points * 0.10; // Each point = $0.10
            double maxDiscount = Math.min(maxPointsValue, total); // Can't exceed total
            
            if (maxDiscount > 0) {
                String msg = String.format("""
                    Customer has %d loyalty points (worth $%.2f).
                    Maximum discount available: $%.2f
                    
                    Use points for discount?""", 
                    cust.points, maxPointsValue, maxDiscount);
                
                if (Main.askYesNo(msg, logo)) {
                    int maxPointsToUse = (int)(maxDiscount / 0.10);
                    pointsUsed = Main.askInt("How many points to use (max " + maxPointsToUse + "):", logo);
                    
                    if (pointsUsed > 0) {
                        pointsUsed = Math.min(pointsUsed, Math.min(cust.points, maxPointsToUse));
                        pointsDiscount = pointsUsed * 0.10;
                        total = total - pointsDiscount;
                        cust.points -= pointsUsed;
                        
                        Main.info(String.format("Applied %d points for $%.2f discount. New total: $%.2f", 
                                pointsUsed, pointsDiscount, total), logo);
                    }
                }
            }
        }

        boolean deduct = Main.askYesNo("Deduct these quantities from inventory now?", logo);
        if (deduct) {
            for (int[] pair : selections) {
                InventoryManager.InventoryItem it = InventoryManager.inventory.get(pair[0]);
                int qty = pair[1];
                it.qtyOnHand -= qty;
            }
        }

        String invoiceId = genInvoiceId();
        Invoice inv = new Invoice(invoiceId, cust.id, date, sub, tax, total);
        invoices.add(inv);

        // Loyalty: 1 point per $10 of final total (after discount)
        int earned = (int)Math.floor(total / 10.0);
        cust.points += earned;

        // Build summary message
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Invoice saved.\nInvoice ID: %s\nCustomer: %s\nItems: %d\nSubtotal: $%.2f\nTax: $%.2f",
            invoiceId, cust.name, itemsCount, sub, tax));
        
        if (pointsDiscount > 0) {
            summary.append(String.format("\nPoints used: %d (Discount: $%.2f)", pointsUsed, pointsDiscount));
        }
        
        summary.append(String.format("\nFinal Total: $%.2f\nPoints earned: %d\nNew points balance: %d",
            total, earned, cust.points));

        Main.info(summary.toString(), logo);
    }

    static void listInvoices(ImageIcon logo) {
        if (invoices.isEmpty()) {
            Main.info("No invoices recorded.", logo);
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-18s %-12s %-20s %-16s %-12s %-10s %-8s %-10s%n",
            "Invoice ID", "CustomerID", "Customer Name", "Phone",
            "Date(M-D-Y)", "Subtotal", "Tax", "Total"));
        sb.append("------------------------------------------------------------------------------------------------\n");
        
        for (Invoice inv : invoices) {
            Customer c = findCustomerById(inv.customerId);
            String cname = (c != null ? c.name : "(unknown)");
            String cphone = (c != null ? c.phone : "(unlinked)");
            
            // Truncate fields if too long
            String invId = inv.invoiceId.length() > 18 ? inv.invoiceId.substring(0, 15) + "..." : inv.invoiceId;
            String custId = inv.customerId.length() > 12 ? inv.customerId.substring(0, 9) + "..." : inv.customerId;
            String custName = cname.length() > 20 ? cname.substring(0, 17) + "..." : cname;
            String custPhone = cphone.length() > 16 ? cphone.substring(0, 13) + "..." : cphone;
            
            sb.append(String.format("%-18s %-12s %-20s %-16s %-12s $%-9.2f $%-7.2f $%-9.2f%n",
                invId, custId, custName, custPhone, inv.dateMDY,
                inv.subTotal, inv.tax, inv.total));
        }
        
        Main.showLarge(sb.toString(), "Invoices", logo);
    }

    /**
     * Tax Rate Management
     * ------------------
     * Allows updating the current tax rate used for invoice calculations.
     * Displays current rate and allows setting a new rate as percentage.
     */
    static void setTaxRate(ImageIcon logo) {
        double currentPercent = currentTaxRate * 100.0;
        
        String msg = String.format("Current tax rate: %.2f%%\n\nEnter new tax rate as percentage (e.g., 7.5 for 7.5%%):", currentPercent);
        String input = Main.prompt(msg, logo);
        
        if (input == null || input.trim().isEmpty()) {
            return; // User cancelled
        }
        
        try {
            double newPercent = Double.parseDouble(input.trim());
            
            if (newPercent < 0 || newPercent > 100) {
                Main.error("Tax rate must be between 0% and 100%.", logo);
                return;
            }
            
            currentTaxRate = newPercent / 100.0;
            
            Main.info(String.format("Tax rate updated to %.2f%%\n\nThis will apply to new invoices.", newPercent), logo);
            
        } catch (NumberFormatException e) {
            Main.error("Invalid number format. Please enter a valid percentage.", logo);
        }
    }
}