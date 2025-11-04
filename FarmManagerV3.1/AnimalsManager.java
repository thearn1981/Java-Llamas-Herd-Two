import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * Animal Sales Management
 * ---------------------
 * Tracks and manages animal sales and acquisitions.
 * Handles animal records, pricing, and breeder information.
 * Supports both breeder-sourced and direct sales.
 */
public class AnimalsManager {
    static ArrayList<AnimalSale> animals = new ArrayList<>();

    /**
     * Animal Sales Data
     * ----------------
     * Records details of animals available for sale,
     * including breed info, pricing, and source details.
     */
    static class AnimalSale {
        String name;  // animal name/identifier
        String breed;
        String gender;
        String age;
        double price;
        String breederName; // empty if not from local breeder

        AnimalSale(String name, String breed, String gender, String age,
                  double price, String breederName) {
            this.name = name;
            this.breed = breed;
            this.gender = gender;
            this.age = age;
            this.price = price;
            this.breederName = breederName;
        }

        String toCSV() {
            return Main.escape(name) + "," + Main.escape(breed) + "," +
                   Main.escape(gender) + "," + Main.escape(age) + "," +
                   price + "," + Main.escape(breederName);
        }

        static AnimalSale fromCSV(String line) {
            String[] p = line.split(",", -1);
            if (p.length < 6) return null;
            return new AnimalSale(
                Main.unescape(p[0]),
                Main.unescape(p[1]),
                Main.unescape(p[2]),
                Main.unescape(p[3]),
                Main.parseDouble(p[4]),
                Main.unescape(p[5])
            );
        }

        @Override
        public String toString() {
            String breeder = (breederName == null || breederName.isBlank()) ? "—" : breederName;
            return String.format("%-14s | %-12s | %-6s | age=%-6s | $%.2f | breeder: %s",
                name, breed, gender, age, price, breeder);
        }
    }

    /**
     * File Operations
     * --------------
     * Manages animal records in CSV storage.
     * Handles loading and saving of animal data
     * with basic error handling.
     */
    static void loadAnimals() {
        animals.clear();
        for (String line : Main.Store.readDataLines(Main.Store.ANM_FILE)) {
            AnimalSale a = AnimalSale.fromCSV(line);
            if (a != null) animals.add(a);
        }
    }

    static void saveAnimals() {
        ArrayList<String> lines = new ArrayList<>();
        for (AnimalSale a : animals) lines.add(a.toCSV());
        Main.Store.writeDataLines(Main.Store.ANM_FILE, Main.Store.ANIMALS_HEADER, lines);
    }

    /**
     * Animals Menu
     * -----------
     * Main interface for animal management.
     * Provides options to add, edit, remove animals,
     * and manage breeder information.
     */
    static void animalsMenu(ImageIcon logo) {
    while (true) {
        // Calculate responsive dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int menuWidth = Math.min(500, (int)(screenSize.width * 0.4));
        int menuHeight = Math.min(400, (int)(screenSize.height * 0.5));
        
        // Header
        JLabel logoLabel = Main.createResponsiveLogo(menuWidth, menuHeight);

        JLabel titleLabel = new JLabel("+========== Animals ==========+");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        JSeparator sep = new JSeparator();
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        sep.setMaximumSize(new Dimension(menuWidth - 40, 2));

        JLabel instructionLabel = new JLabel("Select:");
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionLabel.setHorizontalAlignment(JLabel.CENTER);

        // Buttons
        JButton bAdd   = new JButton("Add");
        JButton bEdit  = new JButton("Edit");
        JButton bRem   = new JButton("Remove");
        JButton bList  = new JButton("List");
        JButton bBack  = new JButton("Back");

        // Uniform sizes (use the largest preferred size among buttons)
        JButton[] all = { bAdd, bEdit, bRem, bList, bBack };
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

        // Row 2: List, Back (CENTER aligned)
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row2.add(bList); row2.add(bBack);
        row2.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Root vertical content
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

        // JOptionPane with custom content
        JOptionPane pane = new JOptionPane(
                root,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                new Object[]{},
                null
        );
        JDialog dlg = pane.createDialog(null, "Animals Menu");
        dlg.setModal(true);
        dlg.setResizable(false);

        // Actions → set value then close
        Runnable close = dlg::dispose;
        bAdd.addActionListener(_  -> { pane.setValue("ADD");  close.run(); });
        bEdit.addActionListener(_ -> { pane.setValue("EDIT"); close.run(); });
        bRem.addActionListener(_  -> { pane.setValue("REM");  close.run(); });
        bList.addActionListener(_ -> { pane.setValue("LIST"); close.run(); });
        bBack.addActionListener(_ -> { pane.setValue("BACK"); close.run(); });

        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);

        Object val = pane.getValue();
        if (val == null || "BACK".equals(val)) break;

        try {
            switch (String.valueOf(val)) {
                case "ADD"  -> addAnimal(logo);
                case "EDIT" -> editAnimal(logo);
                case "REM"  -> removeAnimal(logo);
                case "LIST" -> listAnimals(logo);
            }
        } catch (Exception e) {
            Main.error("Error: " + e.getMessage(), logo);
        }
    }
}

    /**
     * Animal Management Actions
     * ----------------------
     * Core functions for managing animal records.
     * Includes adding new animals, updating details,
     * and handling animal sales transactions.
     */
    static void addAnimal(ImageIcon logo) {
        String name = Main.clean(Main.prompt("Animal name/ID:", logo));
        if (name == null || name.isEmpty()) return;
        
        String breed = Main.clean(Main.prompt("Breed:", logo));
        if (breed == null) return;
        
        String gender = Main.clean(Main.prompt("Gender:", logo));
        if (gender == null) return;
        
        String age = Main.clean(Main.prompt("Age:", logo));
        if (age == null) return;
        
        double price = Main.askDouble("Price:", logo);
        
        String breeder = Main.clean(Main.prompt("Breeder name (optional):", logo));
        if (breeder == null) breeder = "";
        
        animals.add(new AnimalSale(name, breed, gender, age, price, breeder));
        Main.info("Animal sale added.", logo);
    }

    static void editAnimal(ImageIcon logo) {
        if (animals.isEmpty()) {
            Main.info("No animals yet.", logo);
            return;
        }
        
        int idx = Main.pickIndex("Select animal ID to edit:", formatAnimalsTable(), animals.size(), logo);
        if (idx < 0) return;
        
        AnimalSale a = animals.get(idx);
        
        String name = Main.promptDefault("Name/ID:", a.name, logo);
        if (name == null) return;
        
        String breed = Main.promptDefault("Breed:", a.breed, logo);
        if (breed == null) return;
        
        String gender = Main.promptDefault("Gender:", a.gender, logo);
        if (gender == null) return;
        
        String age = Main.promptDefault("Age:", a.age, logo);
        if (age == null) return;
        
        String prStr = Main.promptDefault("Price:", String.valueOf(a.price), logo);
        if (prStr == null) return;
        
        String breeder = Main.promptDefault("Breeder:", a.breederName, logo);
        if (breeder == null) return;
        
        a.name = Main.clean(name);
        a.breed = Main.clean(breed);
        a.gender = Main.clean(gender);
        a.age = Main.clean(age);
        a.price = Main.parseDouble(prStr.trim());
        a.breederName = Main.clean(breeder);
        
        Main.info("Animal updated.", logo);
    }

    static void removeAnimal(ImageIcon logo) {
        if (animals.isEmpty()) {
            Main.info("No animals yet.", logo);
            return;
        }
        
        int idx = Main.pickIndex("Select animal ID to remove:", formatAnimalsTable(), animals.size(), logo);
        if (idx < 0) return;
        
        animals.remove(idx);
        Main.info("Animal removed.", logo);
    }

    static void listAnimals(ImageIcon logo) {
        if (animals.isEmpty()) {
            Main.info("No animals recorded.", logo);
            return;
        }
        Main.showLarge(formatAnimalsTable(), "Animal Sales", logo);
    }

    static String formatAnimalsTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s %-14s %-12s %-8s %-8s %-10s %-20s%n",
            "ID", "Name", "Breed", "Gender", "Age", "Price", "Breeder"));
        sb.append("--------------------------------------------------------------------------------\n");
        
        for (int i = 0; i < animals.size(); i++) {
            AnimalSale a = animals.get(i);
            String breeder = (a.breederName == null || a.breederName.isBlank()) ? "—" : a.breederName;
            
            // Truncate fields if too long
            String name = a.name.length() > 14 ? a.name.substring(0, 11) + "..." : a.name;
            String breed = a.breed.length() > 12 ? a.breed.substring(0, 9) + "..." : a.breed;
            String gender = a.gender.length() > 8 ? a.gender.substring(0, 5) + "..." : a.gender;
            String age = a.age.length() > 8 ? a.age.substring(0, 5) + "..." : a.age;
            String breederDisplay = breeder.length() > 20 ? breeder.substring(0, 17) + "..." : breeder;
            
            sb.append(String.format("%-4d %-14s %-12s %-8s %-8s $%-9.2f %-20s%n",
                i, name, breed, gender, age, a.price, breederDisplay));
        }
        
        return sb.toString();
    }
}