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
     * Main interface for inventory management.
     * Uses centralized UI from UI.java for consistent presentation.
     */
    static void inventoryMenu(ImageIcon logo) {
        while (true) {
            String choice = UI.showInventoryMenu(logo);
            if (choice == null || "BACK".equals(choice)) break;

            try {
                switch (choice) {
                    case "ADD" -> addItem(logo);
                    case "EDIT" -> editItem(logo);
                    case "REM" -> removeItem(logo);
                    case "LIST" -> listItems(logo);
                }
            } catch (Exception e) {
                UI.error("Error: " + e.getMessage(), logo);
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