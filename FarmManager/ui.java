import javax.swing.JOptionPane;

public class UI {
    private static final FarmStore store = new FarmStore();

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

            switch (choice) {
                case 0 -> addNewItem();
                case 1 -> updateItem();
                case 2 -> removeItem();
                case 3 -> viewInventory();
                case 4 -> viewSummary();
                default -> running = confirmExit();
            }
        }
    }

    private static void addNewItem() {
        String name = JOptionPane.showInputDialog("Enter item name:");
        if (name == null || name.trim().isEmpty()) return;

        try {
            double price = Double.parseDouble(JOptionPane.showInputDialog("Enter price for " + name + ":"));
            int quantity = Integer.parseInt(JOptionPane.showInputDialog("Enter quantity for " + name + ":"));
            String[] categories = {"Dairy", "Produce", "Meat", "Pantry", "Other"};
            String category = (String) JOptionPane.showInputDialog(null,
                    "Select category:", "Category",
                    JOptionPane.QUESTION_MESSAGE, null, categories, categories[0]);

            if (category == null) return;

            store.addItem(new FarmItem(name, price, quantity, category));
            JOptionPane.showMessageDialog(null, "Item added successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Invalid input. Please enter valid numbers.");
        }
    }

    private static void updateItem() {
        String inventoryList = store.getInventoryList();
        String input = JOptionPane.showInputDialog(inventoryList + "\nEnter item number to update:");
        if (input == null) return;

        try {
            int index = Integer.parseInt(input) - 1;
            FarmItem item = store.getItem(index);
            if (item == null) {
                JOptionPane.showMessageDialog(null, "Invalid item number.");
                return;
            }

            String[] updateOptions = {"Update Price", "Update Quantity", "Cancel"};
            int choice = JOptionPane.showOptionDialog(null,
                    "Selected: " + item + "\nWhat would you like to update?",
                    "Update Item", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, updateOptions, updateOptions[0]);

            if (choice == 0) {
                double newPrice = Double.parseDouble(JOptionPane.showInputDialog("Enter new price:"));
                item.setPrice(newPrice);
                store.saveInventory();
            } else if (choice == 1) {
                int newQty = Integer.parseInt(JOptionPane.showInputDialog("Enter new quantity:"));
                item.setQuantity(newQty);
                store.saveInventory();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Invalid input.");
        }
    }

    private static void removeItem() {
        String inventoryList = store.getInventoryList();
        String input = JOptionPane.showInputDialog(inventoryList + "\nEnter item number to remove:");
        if (input == null) return;

        try {
            int index = Integer.parseInt(input) - 1;
            FarmItem item = store.getItem(index);
            if (item == null) return;

            int confirm = JOptionPane.showConfirmDialog(null,
                    "Remove: " + item + "?", "Confirm Removal", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                store.removeItem(index);
                JOptionPane.showMessageDialog(null, "Item removed successfully!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Invalid input.");
        }
    }

    private static void viewInventory() {
        JOptionPane.showMessageDialog(null, store.getInventoryList(), "Inventory", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void viewSummary() {
        JOptionPane.showMessageDialog(null, store.getInventorySummary(), "Summary", JOptionPane.INFORMATION_MESSAGE);
    }

    private static boolean confirmExit() {
        int choice = JOptionPane.showConfirmDialog(null,
                "Exit application?", "Confirm Exit", JOptionPane.YES_NO_OPTION);
        return choice != JOptionPane.YES_OPTION;
    }
}
