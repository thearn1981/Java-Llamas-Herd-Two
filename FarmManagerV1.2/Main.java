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

        static java.util.List<String> readLines(String file) {
            try { return Files.readAllLines(Paths.get(file)); }
            catch (IOException e) { return new ArrayList<>(); }
        }

        static void writeLines(String file, java.util.List<String> lines) {
            try { Files.write(Paths.get(file), lines); }
            catch (IOException e) { 
                System.err.println("Error writing to file " + file + ": " + e.getMessage());
            }
        }
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

    // ---------- Logo ----------
/*     static ImageIcon createLogo() {
        int size = 64;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(34, 139, 34));
        g.fillOval(0, 0, size, size);

        g.setColor(Color.WHITE);
        Font f = new Font("SansSerif", Font.BOLD, 28);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics(f);
        String txt = "FL";
        int w = fm.stringWidth(txt);
        int h = fm.getAscent();
        g.drawString(txt, (size - w) / 2, (size + h) / 2 - 4);
        g.dispose();

        return new ImageIcon(img);
    } */
    static ImageIcon createLogo() {
        // Load the image file (place it in your project resources folder or same directory)
        String imagePath = "farmLifeIcon.png"; // <-- rename to match your actual image file

        // Load the image
        ImageIcon icon = new ImageIcon(imagePath);

        // Optionally scale it down (64x64 for JOptionPane or menu icon)
        Image scaled = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);

        return new ImageIcon(scaled);
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
        JLabel logoLabel = new JLabel(logo);
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(" +=========== Farm Life Inc. ===========+ ");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSeparator sep = new JSeparator();
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2)); 

        JLabel instructionLabel = new JLabel("Select an option:");
        instructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

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

        // Row 1: Inventory, Services, Animals  (LEFT aligned)
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row1.add(bInv); row1.add(bSvc); row1.add(bAni);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 2: Customers, Invoices, Exit     (LEFT aligned)
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row2.add(bCust); row2.add(bInvs); row2.add(bExit);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);

        
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
        // If using the optional indent block above, replace the two adds with:
        // root.add(rows);

        // ===== 3) JOptionPane with custom content (no default one-line options) =====
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