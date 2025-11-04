import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * Inventory Management System
 * -------------------------
 * Manages farm inventory items including supplies, equipment,
 * and stock. Tracks quantities, costs, and sales prices.
 * Provides functions for stock management and reporting.
 */
public class InventoryManager {
    static ArrayList<InventoryItem> inventory = new ArrayList<>();

    /**
     * Inventory Data Model
     * ------------------
     * Represents individual inventory items with pricing
     * and quantity tracking. Each item tracks both purchase
     * cost and sale price for profit calculations.
     */
    static class InventoryItem {
        String name;
        double unitPrice;  // purchase cost (optional)
        double salePrice;  // sale price
        int qtyOnHand;

        InventoryItem(String name, double unitPrice, double salePrice, int qtyOnHand) {
            this.name = name;
            this.unitPrice = unitPrice;
            this.salePrice = salePrice;
            this.qtyOnHand = qtyOnHand;
        }

        String toCSV() {
            return Main.escape(name) + "," + unitPrice + "," + salePrice + "," + qtyOnHand;
        }

        static InventoryItem fromCSV(String line) {
            String[] p = line.split(",", -1);
            if (p.length < 4) return null;
            return new InventoryItem(
                Main.unescape(p[0]),
                Main.parseDouble(p[1]),
                Main.parseDouble(p[2]),
                Main.parseInt(p[3])
            );
        }

        @Override
        public String toString() {
            return String.format("%-22s | on-hand=%-5d | price/each=$%.2f (unit $%.2f)",
                name, qtyOnHand, salePrice, unitPrice);
        }
    }

    /**
     * File Operations
     * --------------
     * Handles loading and saving inventory data to CSV.
     * Provides basic persistence with error handling.
     */
    static void loadInventory() {
        inventory.clear();
        for (String line : Main.Store.readDataLines(Main.Store.INV_FILE)) {
            InventoryItem it = InventoryItem.fromCSV(line);
            if (it != null) inventory.add(it);
        }
    }

    static void saveInventory() {
        ArrayList<String> lines = new ArrayList<>();
        for (InventoryItem it : inventory) lines.add(it.toCSV());
        Main.Store.writeDataLines(Main.Store.INV_FILE, Main.Store.INVENTORY_HEADER, lines);
    }

    /**
     * Inventory Menu
     * -------------
     * Main interface for inventory management. Provides
     * options to add, edit, remove and list items.
     * Includes reporting features.
     */
    static void inventoryMenu(ImageIcon logo) {
    while (true) {
        // Calculate responsive dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int menuWidth = Math.min(500, (int)(screenSize.width * 0.4));
        int menuHeight = Math.min(400, (int)(screenSize.height * 0.5));
        
        // Header
        JLabel logoLabel = Main.createResponsiveLogo(menuWidth, menuHeight);

        JLabel titleLabel = new JLabel("+========== Inventory ==========+");
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
        JButton bAdd = new JButton("Add");
        JButton bEdit = new JButton("Edit");
        JButton bRemove = new JButton("Remove");
        JButton bList = new JButton("List");
        JButton bBack = new JButton("Back");

        // Uniform sizes
        JButton[] all = { bAdd, bEdit, bRemove, bList, bBack };
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
        row1.add(bAdd); row1.add(bEdit); row1.add(bRemove);
        row1.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Row 2: List, Back (CENTER aligned)
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row2.add(bList); row2.add(bBack);
        row2.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Root content (vertical)
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
        JDialog dlg = pane.createDialog(null, "Inventory Menu");
        dlg.setModal(true);
        dlg.setResizable(false);

        // Actions
        Runnable close = dlg::dispose;
        bAdd.addActionListener(e -> { e.getActionCommand(); pane.setValue("ADD"); close.run(); });
        bEdit.addActionListener(e -> { e.getActionCommand(); pane.setValue("EDIT"); close.run(); });
        bRemove.addActionListener(e -> { e.getActionCommand(); pane.setValue("REMOVE"); close.run(); });
        bList.addActionListener(e -> { e.getActionCommand(); pane.setValue("LIST"); close.run(); });
        bBack.addActionListener(e -> { e.getActionCommand(); pane.setValue("BACK"); close.run(); });

        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);

        Object val = pane.getValue();
        if (val == null || "BACK".equals(val)) break;

        try {
            switch (String.valueOf(val)) {
                case "ADD"    -> addItem(logo);
                case "EDIT"   -> editItem(logo);
                case "REMOVE" -> removeItem(logo);
                case "LIST"   -> listItems(logo);
            }
        } catch (Exception e) {
            Main.error("Error: " + e.getMessage(), logo);
        }
    }
    }

    /**
     * Inventory Actions
     * ---------------
     * Core functions for managing inventory items.
     * Handles adding, editing, removing and listing items
     * with input validation. Edit includes quantity changes.
     */
    static void addItem(ImageIcon logo) {
        String name = Main.clean(Main.prompt("Item name:", logo));
        if (name == null || name.isEmpty()) return;
        
        double unitPrice = Main.askDouble("Unit price (cost):", logo);
        double salePrice = Main.askDouble("Sale price:", logo);
        int qty = Main.askInt("Quantity on hand:", logo);
        
        inventory.add(new InventoryItem(name, unitPrice, salePrice, qty));
        Main.info("Item added.", logo);
    }

    static void editItem(ImageIcon logo) {
        if (inventory.isEmpty()) {
            Main.info("No items yet.", logo);
            return;
        }
        
        int idx = Main.pickIndex("Select item ID to edit:", formatInventoryTable(), inventory.size(), logo);
        if (idx < 0) return;
        
        InventoryItem it = inventory.get(idx);
        String name = Main.promptDefault("Name:", it.name, logo);
        if (name == null) return;
        
        String upStr = Main.promptDefault("Unit price:", String.valueOf(it.unitPrice), logo);
        if (upStr == null) return;
        
        String spStr = Main.promptDefault("Sale price:", String.valueOf(it.salePrice), logo);
        if (spStr == null) return;
        
        String qStr = Main.promptDefault("Qty on hand:", String.valueOf(it.qtyOnHand), logo);
        if (qStr == null) return;
        
        it.name = Main.clean(name);
        it.unitPrice = Main.parseDouble(upStr.trim());
        it.salePrice = Main.parseDouble(spStr.trim());
        it.qtyOnHand = Main.parseInt(qStr.trim());
        
        Main.info("Item updated.", logo);
    }

    static void removeItem(ImageIcon logo) {
        if (inventory.isEmpty()) {
            Main.info("No items yet.", logo);
            return;
        }
        
        int idx = Main.pickIndex("Select item ID to remove:", formatInventoryTable(), inventory.size(), logo);
        if (idx < 0) return;
        
        inventory.remove(idx);
        Main.info("Item removed.", logo);
    }

    static void listItems(ImageIcon logo) {
        if (inventory.isEmpty()) {
            Main.info("No items in inventory.", logo);
            return;
        }
        Main.showLarge(formatInventoryTable(), "Inventory Items", logo);
    }

    static String formatInventoryTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s %-22s %-10s %-10s %-10s%n",
            "ID", "Name", "On Hand", "Sale $", "Unit $"));
        sb.append("------------------------------------------------------------\n");
        
        for (int i = 0; i < inventory.size(); i++) {
            InventoryItem it = inventory.get(i);
            // Truncate name if too long
            String displayName = it.name;
            if (displayName.length() > 22) {
                displayName = displayName.substring(0, 19) + "...";
            }
            sb.append(String.format("%-4d %-22s %-10d $%-9.2f $%-9.2f%n",
                i, displayName, it.qtyOnHand, it.salePrice, it.unitPrice));
        }
        
        return sb.toString();
    }
}