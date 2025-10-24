import java.awt.Component;
import java.awt.Font;
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
        for (String line : Main.Store.readLines(Main.Store.CUST_FILE)) {
            Customer c = Customer.fromCSV(line);
            if (c != null) customers.add(c);
        }
    }

    static void saveCustomers() {
        ArrayList<String> lines = new ArrayList<>();
        for (Customer c : customers) lines.add(c.toCSV());
        Main.Store.writeLines(Main.Store.CUST_FILE, lines);
    }

    static void loadInvoices() {
        invoices.clear();
        for (String line : Main.Store.readLines(Main.Store.INV2_FILE)) {
            Invoice inv = Invoice.fromCSV(line);
            if (inv != null) invoices.add(inv);
        }
    }

    static void saveInvoices() {
        ArrayList<String> lines = new ArrayList<>();
        for (Invoice inv : invoices) lines.add(inv.toCSV());
        Main.Store.writeLines(Main.Store.INV2_FILE, lines);
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

    /**
     * Customer Management Interface
     * --------------------------
     * Main menu for customer operations.
     * Provides functions to add, edit, remove,
     * and list customer information.
     */
    static void customersMenu(ImageIcon logo) {
        String[] opts = {
            "Add customer",
            "Edit customer",
            "Remove customer",
            "List customers",
            "Back"
        };

        while (true) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            
            JLabel titleLabel = new JLabel("===== Customers =====");
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            panel.add(titleLabel);
            panel.add(Box.createVerticalStrut(10));
            
            JLabel instructionLabel = new JLabel("Select:");
            instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(instructionLabel);

            int result = JOptionPane.showOptionDialog(
                null,
                panel,
                "Customers Menu",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                logo,
                opts,
                opts[0]
            );

            if (result == JOptionPane.CLOSED_OPTION || result == 4) break;

            try {
                switch (result) {
                    case 0 -> addCustomer(logo);
                    case 1 -> editCustomer(logo);
                    case 2 -> removeCustomer(logo);
                    case 3 -> listCustomers(logo);
                }
            } catch (Exception e) {
                Main.error("Error: " + e.getMessage(), logo);
            }
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
        
        listCustomers(logo);
        String phone = Main.clean(Main.prompt("Enter current phone of customer to edit:", logo));
        if (phone == null) return;
        
        Customer c = findCustomerByPhone(phone);
        if (c == null) {
            Main.error("Customer not found.", logo);
            return;
        }

        String newName = Main.promptDefault("Name:", c.name, logo);
        if (newName == null) return;
        
        String newPhone = Main.promptDefault("Phone:", c.phone, logo);
        if (newPhone == null) return;
        
        String newEmail = Main.promptDefault("Email:", c.email, logo);
        if (newEmail == null) return;

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

        Main.info("Customer updated.\nID stays the same: " + c.id, logo);
    }

    static void removeCustomer(ImageIcon logo) {
        if (customers.isEmpty()) {
            Main.info("No customers yet.", logo);
            return;
        }
        
        listCustomers(logo);
        String phone = Main.clean(Main.prompt("Enter phone of customer to remove:", logo));
        if (phone == null) return;
        
        Customer c = findCustomerByPhone(phone);
        if (c == null) {
            Main.error("Customer not found.", logo);
            return;
        }
        
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
        String[] opts = {
            "Create new invoice",
            "List invoices",
            "Back"
        };

        while (true) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            
            JLabel titleLabel = new JLabel("===== Invoices =====");
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            panel.add(titleLabel);
            panel.add(Box.createVerticalStrut(10));
            
            JLabel instructionLabel = new JLabel("Select:");
            instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(instructionLabel);

            int result = JOptionPane.showOptionDialog(
                null,
                panel,
                "Invoices Menu",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                logo,
                opts,
                opts[0]
            );

            if (result == JOptionPane.CLOSED_OPTION || result == 2) break;

            try {
                switch (result) {
                    case 0 -> newInvoice(logo);
                    case 1 -> listInvoices(logo);
                }
            } catch (Exception e) {
                Main.error("Error: " + e.getMessage(), logo);
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
        
        if (customers.isEmpty()) {
            if (Main.askYesNo("No customers yet. Add one now?", logo)) {
                addCustomer(logo);
            }
            if (customers.isEmpty()) return;
        }

        listCustomers(logo);
        String phone = Main.clean(Main.prompt("Enter existing customer phone:", logo));
        if (phone == null) return;
        
        Customer cust = findCustomerByPhone(phone);
        if (cust == null) {
            Main.error("Customer not found.", logo);
            return;
        }

        String date = Main.promptDateMDY(logo);

        // Build selections (no persistent lines): keep (idx, qty) pairs
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
        
        double taxRate = 0.0;  // change if you need tax (e.g., 0.07)
        double tax = sub * taxRate;
        double total = sub + tax;

        boolean deduct = Main.askYesNo("Deduct these quantities from inventory now?", logo);
        if (deduct) {
            for (int[] pair : selections) {
                InventoryManager.InventoryItem it = InventoryManager.inventory.get(pair[0]);
                int qty = pair[1];
                it.qtyOnHand -= qty;
            }
        }

        String invoiceId = "INV-" + System.currentTimeMillis();
        Invoice inv = new Invoice(invoiceId, cust.id, date, sub, tax, total);
        invoices.add(inv);

        // Loyalty: 1 point per $10 (floor)
        int earned = (int)Math.floor(total / 10.0);
        cust.points += earned;

        Main.info(String.format("""
            Invoice saved.
            Invoice ID: %s
            Customer: %s
            Items: %d
            Subtotal: $%.2f
            Tax: $%.2f
            Total: $%.2f
            Points earned: %d (New balance: %d)""",
            invoiceId, cust.name, itemsCount, sub, tax, total, earned, cust.points
        ), logo);
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
}