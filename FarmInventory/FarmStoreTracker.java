import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

// Item class to represent products in the farm store
class FarmItem {
    private final String name;
    private double price;
    private int quantity;
    private final String category;
    
    public FarmItem(String name, double price, int quantity, String category) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
    }
    
    public String getName() {
        return name;
    }
    
    public double getPrice() {
        return price;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public void addQuantity(int amount) {
        this.quantity += amount;
    }
    
    public void removeQuantity(int amount) {
        this.quantity -= amount;
    }
    
    public double getTotalValue() {
        return price * quantity;
    }
    
    // Convert item to string for file storage
    public String toFileString() {
        return name + "|" + price + "|" + quantity + "|" + category;
    }
    
    // Create item from file string
    public static FarmItem fromFileString(String line) {
        String[] parts = line.split("\\|");
        if (parts.length == 4) {
            String name = parts[0];
            double price = Double.parseDouble(parts[1]);
            int quantity = Integer.parseInt(parts[2]);
            String category = parts[3];
            return new FarmItem(name, price, quantity, category);
        }
        return null;
    }
    
    @Override
    public String toString() {
        return name + " | $" + String.format("%.2f", price) + 
               " | Qty: " + quantity + " | " + category;
    }
}

// Main store management class
class FarmStore {
    private final List<FarmItem> inventory;
    private final DecimalFormat df;
    private static final String FILENAME = "farm_store_inventory.txt";
    
    public FarmStore() {
        inventory = new ArrayList<>();
        df = new DecimalFormat("#.00");
        loadInventory();
    }
    
    // Load inventory from file
    private void loadInventory() {
        File file = new File(FILENAME);
        
        // Decision structure: check if file exists
        if (!file.exists()) {
            // File doesn't exist, create it with default inventory
            initializeInventory();
            saveInventory();
            JOptionPane.showMessageDialog(null,
                "New inventory file created: " + FILENAME,
                "File Created",
                JOptionPane.INFORMATION_MESSAGE);
        } else {
            // File exists, load data from it
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    FarmItem item = FarmItem.fromFileString(line);
                    if (item != null) {
                        inventory.add(item);
                    }
                }
                
                // Decision structure: check if file was empty
                if (inventory.isEmpty()) {
                    initializeInventory();
                    saveInventory();
                }
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                    "Error loading inventory file: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                initializeInventory();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null,
                    "Error reading inventory data. Creating new inventory.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                inventory.clear();
                initializeInventory();
                saveInventory();
            }
        }
    }
    
    // Save inventory to file
    public void saveInventory() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILENAME))) {
            for (FarmItem item : inventory) {
                writer.write(item.toFileString());
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                "Error saving inventory: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void initializeInventory() {
        // Pre-stock some common farm store items
        inventory.add(new FarmItem("Fresh Eggs (dozen)", 4.50, 24, "Dairy"));
        inventory.add(new FarmItem("Milk (gallon)", 5.99, 15, "Dairy"));
        inventory.add(new FarmItem("Honey (16oz)", 8.99, 12, "Pantry"));
        inventory.add(new FarmItem("Apples (lb)", 2.49, 50, "Produce"));
        inventory.add(new FarmItem("Tomatoes (lb)", 3.29, 30, "Produce"));
        inventory.add(new FarmItem("Sweet Corn (ear)", 0.75, 100, "Produce"));
        inventory.add(new FarmItem("Beef (lb)", 8.99, 20, "Meat"));
        inventory.add(new FarmItem("Chicken (lb)", 5.99, 25, "Meat"));
    }
    
    public String getInventoryList() {
        if (inventory.isEmpty()) {
            return "No items in inventory.";
        }
        
        StringBuilder list = new StringBuilder("Farm Store Inventory:\n\n");
        for (int i = 0; i < inventory.size(); i++) {
            list.append((i + 1)).append(". ").append(inventory.get(i)).append("\n");
        }
        return list.toString();
    }
    
    public String getInventorySummary() {
        if (inventory.isEmpty()) {
            return "No items in inventory.";
        }
        
        StringBuilder summary = new StringBuilder("Inventory Summary:\n\n");
        double totalValue = 0;
        int totalItems = 0;
        
        for (FarmItem item : inventory) {
            totalValue += item.getTotalValue();
            totalItems += item.getQuantity();
        }
        
        summary.append("Total Items: ").append(totalItems).append("\n");
        summary.append("Total Inventory Value: $").append(df.format(totalValue)).append("\n\n");
        summary.append("Items by Category:\n");
        summary.append(getItemsByCategory());
        
        return summary.toString();
    }
    
    private String getItemsByCategory() {
        StringBuilder result = new StringBuilder();
        String[] categories = {"Dairy", "Produce", "Meat", "Pantry", "Other"};
        
        for (String category : categories) {
            int count = 0;
            for (FarmItem item : inventory) {
                if (item.getCategory().equals(category)) {
                    count++;
                }
            }
            if (count > 0) {
                result.append("  ").append(category).append(": ").append(count).append(" items\n");
            }
        }
        
        return result.toString();
    }
    
    public FarmItem getItem(int index) {
        if (index >= 0 && index < inventory.size()) {
            return inventory.get(index);
        }
        return null;
    }
    
    public void addItem(FarmItem item) {
        inventory.add(item);
        saveInventory();
    }
    
    public void removeItem(int index) {
        if (index >= 0 && index < inventory.size()) {
            inventory.remove(index);
            saveInventory();
        }
    }
    
    public int getInventorySize() {
        return inventory.size();
    }
}

// Main application class
public class FarmStoreTracker {
    private static final FarmStore store = new FarmStore(); // changed: make store final (and static)
    
    public static void main(String[] args) {
        boolean running = true;
        
        while (running) {
            String[] options = {"Add New Item", "Update Item", "Remove Item", 
                               "View Inventory", "Inventory Summary", "Exit"};
            int choice = JOptionPane.showOptionDialog(null,
                "What would you like to do?",
                "Farm Store Menu",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
            
            // Decision structure for menu handling
            if (choice == 0) {
                addNewItem();
            } else if (choice == 1) {
                updateItem();
            } else if (choice == 2) {
                removeItem();
            } else if (choice == 3) {
                viewInventory();
            } else if (choice == 4) {
                viewInventorySummary();
            } else if (choice == 5 || choice == JOptionPane.CLOSED_OPTION) {
                running = confirmExit();
            }
        }
    }
    
    private static void addNewItem() {
        // Get item name
        String name = JOptionPane.showInputDialog(null,
            "Enter item name:",
            "Add New Item",
            JOptionPane.QUESTION_MESSAGE);
        
        if (name == null || name.trim().isEmpty()) {
            return; // User cancelled
        }
        
        try {
            // Get price
            String priceInput = JOptionPane.showInputDialog(null,
                "Enter price for " + name + ":",
                "Enter Price",
                JOptionPane.QUESTION_MESSAGE);
            
            if (priceInput == null || priceInput.trim().isEmpty()) {
                return;
            }
            
            double price = Double.parseDouble(priceInput.trim());
            
            // Decision structure for price validation
            if (price < 0) {
                JOptionPane.showMessageDialog(null,
                    "Price cannot be negative.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Get quantity
            String qtyInput = JOptionPane.showInputDialog(null,
                "Enter quantity for " + name + ":",
                "Enter Quantity",
                JOptionPane.QUESTION_MESSAGE);
            
            if (qtyInput == null || qtyInput.trim().isEmpty()) {
                return;
            }
            
            int quantity = Integer.parseInt(qtyInput.trim());
            
            // Decision structure for quantity validation
            if (quantity < 0) {
                JOptionPane.showMessageDialog(null,
                    "Quantity cannot be negative.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Get category
            String[] categories = {"Dairy", "Produce", "Meat", "Pantry", "Other"};
            String category = (String) JOptionPane.showInputDialog(null,
                "Select category for " + name + ":",
                "Select Category",
                JOptionPane.QUESTION_MESSAGE,
                null,
                categories,
                categories[0]);
            
            if (category == null) {
                return;
            }
            
            // Create and add item
            FarmItem newItem = new FarmItem(name, price, quantity, category);
            store.addItem(newItem);
            
            JOptionPane.showMessageDialog(null,
                "Item Added Successfully!\n\n" + newItem.toString(),
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null,
                "Please enter valid numbers for price and quantity.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void updateItem() {
        // Show inventory and get item selection
        String inventoryList = store.getInventoryList();
        String input = JOptionPane.showInputDialog(null,
            inventoryList + "\nEnter item number to update:",
            "Select Item",
            JOptionPane.QUESTION_MESSAGE);
        
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        
        try {
            int itemNum = Integer.parseInt(input.trim());
            
            // Decision structure for item validation
            if (itemNum < 1 || itemNum > store.getInventorySize()) {
                JOptionPane.showMessageDialog(null,
                    "Invalid item number.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            FarmItem selectedItem = store.getItem(itemNum - 1);
            
            // Choose what to update
            String[] updateOptions = {"Update Price", "Update Quantity", "Cancel"};
            int updateChoice = JOptionPane.showOptionDialog(null,
                "Current: " + selectedItem.toString() + "\n\nWhat would you like to update?",
                "Update Item",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                updateOptions,
                updateOptions[0]);
            
            // Decision structure for update type
            if (updateChoice == 0) {
                updatePrice(selectedItem);
            } else if (updateChoice == 1) {
                updateQuantity(selectedItem);
            }
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null,
                "Please enter a valid number.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void updatePrice(FarmItem item) {
        String priceInput = JOptionPane.showInputDialog(null,
            "Current price: $" + String.format("%.2f", item.getPrice()) + "\n\nEnter new price:",
            "Update Price",
            JOptionPane.QUESTION_MESSAGE);
        
        if (priceInput == null || priceInput.trim().isEmpty()) {
            return;
        }
        
        try {
            double newPrice = Double.parseDouble(priceInput.trim());
            
            if (newPrice < 0) {
                JOptionPane.showMessageDialog(null,
                    "Price cannot be negative.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            item.setPrice(newPrice);
            store.saveInventory();
            JOptionPane.showMessageDialog(null,
                "Price updated successfully!\n\n" + item.toString(),
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null,
                "Please enter a valid number.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void updateQuantity(FarmItem item) {
        String qtyInput = JOptionPane.showInputDialog(null,
            "Current quantity: " + item.getQuantity() + "\n\nEnter new quantity:",
            "Update Quantity",
            JOptionPane.QUESTION_MESSAGE);
        
        if (qtyInput == null || qtyInput.trim().isEmpty()) {
            return;
        }
        
        try {
            int newQuantity = Integer.parseInt(qtyInput.trim());
            
            if (newQuantity < 0) {
                JOptionPane.showMessageDialog(null,
                    "Quantity cannot be negative.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            item.setQuantity(newQuantity);
            store.saveInventory();
            JOptionPane.showMessageDialog(null,
                "Quantity updated successfully!\n\n" + item.toString(),
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null,
                "Please enter a valid number.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void removeItem() {
        String inventoryList = store.getInventoryList();
        String input = JOptionPane.showInputDialog(null,
            inventoryList + "\nEnter item number to remove:",
            "Remove Item",
            JOptionPane.QUESTION_MESSAGE);
        
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        
        try {
            int itemNum = Integer.parseInt(input.trim());
            
            if (itemNum < 1 || itemNum > store.getInventorySize()) {
                JOptionPane.showMessageDialog(null,
                    "Invalid item number.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            FarmItem itemToRemove = store.getItem(itemNum - 1);
            
            int confirm = JOptionPane.showConfirmDialog(null,
                "Are you sure you want to remove:\n" + itemToRemove.toString(),
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                store.removeItem(itemNum - 1);
                JOptionPane.showMessageDialog(null,
                    "Item removed successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null,
                "Please enter a valid number.",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private static void viewInventory() {
        String inventory = store.getInventoryList();
        JOptionPane.showMessageDialog(null,
            inventory,
            "Current Inventory",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private static void viewInventorySummary() {
        String summary = store.getInventorySummary();
        JOptionPane.showMessageDialog(null,
            summary,
            "Inventory Summary",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private static boolean confirmExit() {
        int choice = JOptionPane.showConfirmDialog(null,
            "Are you sure you want to exit?",
            "Confirm Exit",
            JOptionPane.YES_NO_OPTION);
        
        if (choice == JOptionPane.YES_OPTION) {
            return false;
        }
        return true;
    }
}