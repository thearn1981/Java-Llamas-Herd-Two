import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Farm Life Inc. - Simple GUI Tracker
 * Tracks: Inventory items, Services (with schedule & paid/unpaid), Animal sales/resales.
 * Storage: CSV files in working directory (inventory.csv, services.csv, animals.csv). 
 */
public class FarmLifeApp {

    // ---------- Data Models ----------
    static class InventoryItem {
        String name;
        double unitPrice;   // purchase cost (if you use it)
        double salePrice;   // sale price
        int qtyOnHand;

        InventoryItem(String name, double unitPrice, double salePrice, int qtyOnHand) {
            this.name = name;
            this.unitPrice = unitPrice;
            this.salePrice = salePrice;
            this.qtyOnHand = qtyOnHand;
        }

        String toCSV() {
            return escape(name) + "," + unitPrice + "," + salePrice + "," + qtyOnHand;
        }

        static InventoryItem fromCSV(String line) {
            String[] p = line.split(",", -1);
            if (p.length < 4) return null;
            return new InventoryItem(unescape(p[0]), parseDouble(p[1]), parseDouble(p[2]), parseInt(p[3]));
        }

        @Override public String toString() {
            return String.format("%-22s | on-hand=%-5d | price/each=$%.2f (unit $%.2f)",
                    name, qtyOnHand, salePrice, unitPrice);
        }
    }

    static class ServiceAppointment {
        String serviceName;
        String customerName;
        String petName;
        String date;     // simple string "YYYY-MM-DD"
        String time;     // simple string like "2:00 PM" or "14:00"
        double price;
        boolean paid;

        ServiceAppointment(String serviceName, String customerName, String petName,
                           String date, String time, double price, boolean paid) {
            this.serviceName = serviceName;
            this.customerName = customerName;
            this.petName = petName;
            this.date = date;
            this.time = time;
            this.price = price;
            this.paid = paid;
        }

        String toCSV() {
            return escape(serviceName)+","+escape(customerName)+","+escape(petName)+","+
                   escape(date)+","+escape(time)+","+price+","+(paid?"PAID":"UNPAID");
        }

        static ServiceAppointment fromCSV(String line) {
            String[] p = line.split(",", -1);
            if (p.length < 7) return null;
            return new ServiceAppointment(unescape(p[0]), unescape(p[1]), unescape(p[2]),
                    unescape(p[3]), unescape(p[4]), parseDouble(p[5]), "PAID".equalsIgnoreCase(p[6]));
        }

        @Override public String toString() {
            return String.format("%-16s | %s (%s) | %s @ %s | $%.2f | %s",
                    serviceName, customerName, petName, date, time, price, (paid ? "PAID" : "UNPAID"));
        }
    }

    static class AnimalSale {
        String name;      // animal name/identifier
        String breed;
        String gender;
        String age;       
        double price;
        String breederName; // empty if not from local breeder

        AnimalSale(String name, String breed, String gender, String age, double price, String breederName) {
            this.name = name;
            this.breed = breed;
            this.gender = gender;
            this.age = age;
            this.price = price;
            this.breederName = breederName;
        }

        String toCSV() {
            return escape(name)+","+escape(breed)+","+escape(gender)+","+escape(age)+","+price+","+escape(breederName);
        }

        static AnimalSale fromCSV(String line) {
            String[] p = line.split(",", -1);
            if (p.length < 6) return null;
            return new AnimalSale(unescape(p[0]), unescape(p[1]), unescape(p[2]),
                    unescape(p[3]), parseDouble(p[4]), unescape(p[5]));
        }

        @Override public String toString() {
            String breeder = (breederName == null || breederName.isBlank()) ? "—" : breederName;
            return String.format("%-14s | %-12s | %-6s | age=%-6s | $%.2f | breeder: %s",
                    name, breed, gender, age, price, breeder);
        }
    }

    // ---------- Simple CSV Store ----------
    static class Store {
        static final String INV_FILE = "inventory.csv";
        static final String SVC_FILE = "services.csv";
        static final String ANM_FILE = "animals.csv";

        static void saveInventory(List<InventoryItem> items) throws IOException {
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(INV_FILE)))) {
                pw.println("name,unitPrice,salePrice,qtyOnHand");
                for (InventoryItem it : items) pw.println(it.toCSV());
            }
        }
        static List<InventoryItem> loadInventory() {
            List<InventoryItem> list = new ArrayList<>();
            Path p = Paths.get(INV_FILE);
            if (!Files.exists(p)) return list;
            try (BufferedReader br = Files.newBufferedReader(p)) {
                String line; boolean header = true;
                while ((line = br.readLine()) != null) {
                    if (header) { header = false; continue; }
                    InventoryItem it = InventoryItem.fromCSV(line);
                    if (it != null) list.add(it);
                }
            } catch (IOException ignored) {}
            return list;
        }

        static void saveServices(List<ServiceAppointment> items) throws IOException {
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(SVC_FILE)))) {
                pw.println("serviceName,customerName,petName,date,time,price,paid");
                for (ServiceAppointment it : items) pw.println(it.toCSV());
            }
        }
        static List<ServiceAppointment> loadServices() {
            List<ServiceAppointment> list = new ArrayList<>();
            Path p = Paths.get(SVC_FILE);
            if (!Files.exists(p)) return list;
            try (BufferedReader br = Files.newBufferedReader(p)) {
                String line; boolean header = true;
                while ((line = br.readLine()) != null) {
                    if (header) { header = false; continue; }
                    ServiceAppointment it = ServiceAppointment.fromCSV(line);
                    if (it != null) list.add(it);
                }
            } catch (IOException ignored) {}
            return list;
        }

        static void saveAnimals(List<AnimalSale> items) throws IOException {
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(ANM_FILE)))) {
                pw.println("name,breed,gender,age,price,breederName");
                for (AnimalSale it : items) pw.println(it.toCSV());
            }
        }
        static List<AnimalSale> loadAnimals() {
            List<AnimalSale> list = new ArrayList<>();
            Path p = Paths.get(ANM_FILE);
            if (!Files.exists(p)) return list;
            try (BufferedReader br = Files.newBufferedReader(p)) {
                String line; boolean header = true;
                while ((line = br.readLine()) != null) {
                    if (header) { header = false; continue; }
                    AnimalSale it = AnimalSale.fromCSV(line);
                    if (it != null) list.add(it);
                }
            } catch (IOException ignored) {}
            return list;
        }
    }

    // ---------- App State ----------
    static final List<InventoryItem> inventory = new ArrayList<>();
    static final List<ServiceAppointment> services = new ArrayList<>();
    static final List<AnimalSale> animals = new ArrayList<>();

    // ---------- Main ----------
    public static void main(String[] args) {
        // Load existing data (if any)
        inventory.addAll(Store.loadInventory());
        services.addAll(Store.loadServices());
        animals.addAll(Store.loadAnimals());

        ImageIcon logo = makeLogo("Farm Life Inc.");

        while (true) {
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Welcome to Farm Life Inc.\nChoose an option:",
                    "Farm Life Inc. — Main Menu",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    logo,
                    new Object[]{"Inventory", "Services", "Animal Sales", "Save All", "Exit"},
                    "Inventory"
            );
            if (choice == 0) inventoryMenu(logo);
            else if (choice == 1) servicesMenu(logo);
            else if (choice == 2) animalsMenu(logo);
            else if (choice == 3) saveAll(logo);
            else break; // Exit or closed
        }
        // On exit, offer to save
        int save = JOptionPane.showConfirmDialog(null, "Save all changes before exit?", "Save",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, logo);
        if (save == JOptionPane.YES_OPTION) saveAll(logo);
    }

    // ---------- Menus ----------
    static void inventoryMenu(ImageIcon logo) {
        while (true) {
            int c = JOptionPane.showOptionDialog(
                    null,
                    "Inventory Options",
                    "Farm Life Inc. — Inventory",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, logo,
                    new Object[]{"Add Item", "Edit Item/Qty", "List Items", "Back"},
                    "Add Item"
            );
            if (c == 0) addInventory(logo);
            else if (c == 1) editInventory(logo);
            else if (c == 2) listInventory(logo);
            else break;
        }
    }
    static void servicesMenu(ImageIcon logo) {
        while (true) {
            int c = JOptionPane.showOptionDialog(
                    null,
                    "Service Appointments",
                    "Farm Life Inc. — Services",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, logo,
                    new Object[]{"Schedule/Add", "Edit Date/Time/Paid", "List Services", "Back"},
                    "Schedule/Add"
            );
            if (c == 0) addService(logo);
            else if (c == 1) editService(logo);
            else if (c == 2) listServices(logo);
            else break;
        }
    }
    static void animalsMenu(ImageIcon logo) {
        while (true) {
            int c = JOptionPane.showOptionDialog(
                    null,
                    "Animal Sales & Resales",
                    "Farm Life Inc. — Animals",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE, logo,
                    new Object[]{"Add Animal", "Edit Animal", "List Animals", "Back"},
                    "Add Animal"
            );
            if (c == 0) addAnimal(logo);
            else if (c == 1) editAnimal(logo);
            else if (c == 2) listAnimals(logo);
            else break;
        }
    }

    // ---------- Inventory actions ----------
    static void addInventory(ImageIcon logo) {
        try {
            String name = clean(prompt("Item name:", logo));
            if (name == null) return;
            double unit = askDouble("Unit price (cost):", logo);
            double sale = askDouble("Sale price:", logo);
            int qty = askInt("Quantity on hand:", logo);
            inventory.add(new InventoryItem(name, unit, sale, qty));
            info("Item added.", logo);
        } catch (Exception e) {
            error("Could not add item: " + e.getMessage(), logo);
        }
    }

    static void editInventory(ImageIcon logo) {
        if (inventory.isEmpty()) { info("No items yet.", logo); return; }
        int idx = pickIndex("Select item to edit (ID):", formatInventoryTable(), inventory.size(), logo);
        if (idx < 0) return;

        InventoryItem it = inventory.get(idx);
        try {
            String name = clean(promptDefault("Item name:", it.name, logo));
            if (name == null) return;
            double unit = askDoubleDefault("Unit price (cost):", it.unitPrice, logo);
            double sale = askDoubleDefault("Sale price:", it.salePrice, logo);
            int qty = askIntDefault("Quantity on hand:", it.qtyOnHand, logo);

            it.name = name; it.unitPrice = unit; it.salePrice = sale; it.qtyOnHand = qty;
            info("Item updated.", logo);
        } catch (Exception e) {
            error("Could not update item: " + e.getMessage(), logo);
        }
    }

    static void listInventory(ImageIcon logo) {
        if (inventory.isEmpty()) { info("No inventory items.", logo); return; }
        StringBuilder sb = formatInventoryTable();
        double totalValue = 0;
        for (InventoryItem it : inventory) totalValue += it.salePrice * it.qtyOnHand;
        sb.append("\nTotal on-hand retail value: $").append(String.format("%.2f", totalValue));
        showLarge(sb.toString(), "Inventory List", logo);
    }

    static StringBuilder formatInventoryTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s %-24s %-12s %-12s %-8s%n", "ID", "Item Name", "Unit Price", "Sale Price", "Qty"));
        sb.append("--------------------------------------------------------------------\n");
        for (int i = 0; i < inventory.size(); i++) {
            InventoryItem it = inventory.get(i);
            sb.append(String.format("%-4d %-24s $%-11.2f $%-11.2f %-8d%n",
                    i, it.name, it.unitPrice, it.salePrice, it.qtyOnHand));
        }
        return sb;
    }

    // ---------- Services actions ----------
    static void addService(ImageIcon logo) {
        try {
            String sName = clean(prompt("Service name:", logo));
            if (sName == null) return;
            String cust = clean(prompt("Customer name:", logo)); if (cust == null) return;
            String pet = clean(prompt("Pet name:", logo)); if (pet == null) return;
            String date = clean(prompt("Date (YYYY-MM-DD):", logo)); if (date == null) return;
            String time = clean(prompt("Time (e.g., 2:00 PM or 14:00):", logo)); if (time == null) return;
            double price = askDouble("Price:", logo);
            boolean paid = askYesNo("Mark as PAID?", logo);

            services.add(new ServiceAppointment(sName, cust, pet, date, time, price, paid));
            info("Service scheduled.", logo);
        } catch (Exception e) {
            error("Could not add service: " + e.getMessage(), logo);
        }
    }

    static void editService(ImageIcon logo) {
        if (services.isEmpty()) { info("No services yet.", logo); return; }
        int idx = pickIndex("Select service to edit (ID):", formatServicesTable(), services.size(), logo);
        if (idx < 0) return;

        ServiceAppointment s = services.get(idx);
        try {
            String date = promptDefault("New date (YYYY-MM-DD):", s.date, logo);
            if (date == null) return;
            String time = promptDefault("New time (e.g., 2:00 PM or 14:00):", s.time, logo);
            if (time == null) return;
            boolean paid = askYesNoDefault("Is it PAID now?", s.paid, logo);

            s.date = clean(date); s.time = clean(time); s.paid = paid;
            info("Service updated.", logo);
        } catch (Exception e) {
            error("Could not update service: " + e.getMessage(), logo);
        }
    }

    static void listServices(ImageIcon logo) {
        if (services.isEmpty()) { info("No service appointments.", logo); return; }
        StringBuilder sb = formatServicesTable();
        double totalDue = services.stream().filter(s -> !s.paid).mapToDouble(s -> s.price).sum();
        sb.append("\nUnpaid balance total: $").append(String.format("%.2f", totalDue));
        showLarge(sb.toString(), "Service Appointments", logo);
    }

    static StringBuilder formatServicesTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s %-16s %-16s %-10s %-10s %-9s %-6s%n",
                "ID","Service","Customer","Date","Time","Price","Paid"));
        sb.append("--------------------------------------------------------------------------\n");
        for (int i = 0; i < services.size(); i++) {
            ServiceAppointment s = services.get(i);
            sb.append(String.format("%-4d %-16s %-16s %-10s %-10s $%-8.2f %-6s%n",
                    i, s.serviceName, s.customerName, s.date, s.time, s.price, (s.paid?"YES":"NO")));
        }
        return sb;
    }

    // ---------- Animals actions ----------
    static void addAnimal(ImageIcon logo) {
        try {
            String name = clean(prompt("Animal name/ID:", logo)); if (name == null) return;
            String breed = clean(prompt("Breed:", logo)); if (breed == null) return;
            String gender = clean(prompt("Gender (M/F/Other):", logo)); if (gender == null) return;
            String age = clean(prompt("Age (e.g., '6 mo'):", logo)); if (age == null) return;
            double price = askDouble("Price:", logo);
            String breeder = clean(prompt("Breeder name (optional, blank if none):", logo));
            if (breeder == null) breeder = "";
            animals.add(new AnimalSale(name, breed, gender, age, price, breeder));
            info("Animal added.", logo);
        } catch (Exception e) {
            error("Could not add animal: " + e.getMessage(), logo);
        }
    }

    static void editAnimal(ImageIcon logo) {
        if (animals.isEmpty()) { info("No animals yet.", logo); return; }
        int idx = pickIndex("Select animal to edit (ID):", formatAnimalsTable(), animals.size(), logo);
        if (idx < 0) return;

        AnimalSale a = animals.get(idx);
        try {
            String name = promptDefault("Animal name/ID:", a.name, logo); if (name == null) return;
            String breed = promptDefault("Breed:", a.breed, logo); if (breed == null) return;
            String gender = promptDefault("Gender:", a.gender, logo); if (gender == null) return;
            String age = promptDefault("Age:", a.age, logo); if (age == null) return;
            double price = askDoubleDefault("Price:", a.price, logo);
            String breeder = promptDefault("Breeder (blank if none):", a.breederName, logo);
            if (breeder == null) breeder = a.breederName;

            a.name = clean(name);
            a.breed = clean(breed);
            a.gender = clean(gender);
            a.age = clean(age);
            a.price = price;
            a.breederName = clean(breeder);
            info("Animal updated.", logo);
        } catch (Exception e) {
            error("Could not update animal: " + e.getMessage(), logo);
        }
    }

    static void listAnimals(ImageIcon logo) {
        if (animals.isEmpty()) { info("No animals recorded.", logo); return; }
        StringBuilder sb = formatAnimalsTable();
        showLarge(sb.toString(), "Animals", logo);
    }

    static StringBuilder formatAnimalsTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s %-14s %-12s %-6s %-8s %-9s %-16s%n",
                "ID","Name","Breed","Gender","Age","Price","Breeder"));
        sb.append("----------------------------------------------------------------------------\n");
        for (int i = 0; i < animals.size(); i++) {
            AnimalSale a = animals.get(i);
            String breeder = (a.breederName == null || a.breederName.isBlank()) ? "—" : a.breederName;
            sb.append(String.format("%-4d %-14s %-12s %-6s %-8s $%-8.2f %-16s%n",
                    i, a.name, a.breed, a.gender, a.age, a.price, breeder));
        }
        return sb;
    }

    // ---------- Save ----------
    static void saveAll(ImageIcon logo) {
        try {
            Store.saveInventory(inventory);
            Store.saveServices(services);
            Store.saveAnimals(animals);
            info("All data saved to CSV files.\n(inventory.csv, services.csv, animals.csv)", logo);
        } catch (IOException e) {
            error("Failed to save files: " + e.getMessage(), logo);
        }
    }

    // ---------- UI Helpers ----------
    static ImageIcon makeLogo(String text) {
        int w = 260, h = 60;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(240, 250, 240));
        g.fillRoundRect(0, 0, w, h, 16, 16);
        g.setColor(new Color(34, 139, 34));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString(text, 12, 36);
        g.dispose();
        return new ImageIcon(img);
    }

    static String prompt(String message, ImageIcon logo) {
        return JOptionPane.showInputDialog(null, message, "Farm Life Inc.", JOptionPane.QUESTION_MESSAGE);
    }
    static String promptDefault(String message, Object defaultVal, ImageIcon logo) {
        JTextField tf = new JTextField(String.valueOf(defaultVal), 20);
        int res = JOptionPane.showConfirmDialog(null, tf, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        return res == JOptionPane.OK_OPTION ? tf.getText() : null;
    }
    static void info(String msg, ImageIcon logo) {
        JOptionPane.showMessageDialog(null, msg, "Farm Life Inc.", JOptionPane.INFORMATION_MESSAGE, logo);
    }
    static void error(String msg, ImageIcon logo) {
        JOptionPane.showMessageDialog(null, msg, "Farm Life Inc. — Error", JOptionPane.ERROR_MESSAGE, logo);
    }
    static boolean askYesNo(String question, ImageIcon logo) {
        int r = JOptionPane.showConfirmDialog(null, question, "Farm Life Inc.", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, logo);
        return r == JOptionPane.YES_OPTION;
    }
    static boolean askYesNoDefault(String question, boolean current, ImageIcon logo) {
        Object[] opts = {"Yes", "No"};
        int r = JOptionPane.showOptionDialog(null, question + " (current: " + (current?"Yes":"No") + ")",
                "Farm Life Inc.", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, current ? "Yes" : "No");
        return r == 0;
    }

    static void showLarge(String text, String title, ImageIcon logo) {
        JTextArea ta = new JTextArea(text, 20, 70);
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        JOptionPane.showMessageDialog(null, sp, title, JOptionPane.PLAIN_MESSAGE, logo);
    }

    static int pickIndex(String prompt, StringBuilder table, int size, ImageIcon logo) {
        showLarge(table.toString(), "Pick by ID", logo);
        try {
            int idx = askInt(prompt, logo);
            if (idx < 0 || idx >= size) { error("Invalid ID.", logo); return -1; }
            return idx;
        } catch (Exception e) {
            error("Invalid input.", logo);
            return -1;
        }
    }

    // ---------- Parsing / Sanitizing ----------
    static String clean(String s) {
        if (s == null) return null;
        return s.trim().replace(",", ";");
    }
    static int askInt(String label, ImageIcon logo) {
        while (true) {
            String in = prompt(label, logo);
            if (in == null) throw new RuntimeException("Cancelled");
            try { return Integer.parseInt(in.trim()); }
            catch (NumberFormatException e) { error("Please enter a whole number.", logo); }
        }
    }
    static int askIntDefault(String label, int current, ImageIcon logo) {
        while (true) {
            String in = promptDefault(label, current, logo);
            if (in == null) throw new RuntimeException("Cancelled");
            try { return Integer.parseInt(in.trim()); }
            catch (NumberFormatException e) { error("Please enter a whole number.", logo); }
        }
    }
    static double askDouble(String label, ImageIcon logo) {
        while (true) {
            String in = prompt(label, logo);
            if (in == null) throw new RuntimeException("Cancelled");
            try { return Double.parseDouble(in.trim()); }
            catch (NumberFormatException e) { error("Please enter a valid number (e.g., 12.34).", logo); }
        }
    }
    static double askDoubleDefault(String label, double current, ImageIcon logo) {
        while (true) {
            String in = promptDefault(label, current, logo);
            if (in == null) throw new RuntimeException("Cancelled");
            try { return Double.parseDouble(in.trim()); }
            catch (NumberFormatException e) { error("Please enter a valid number (e.g., 12.34).", logo); }
        }
    }
    static double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException | NullPointerException e) { return 0.0; }
    }
    static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException | NullPointerException e) { return 0; }
    }
    static String escape(String s) {
        return (s == null) ? "" : s.replace(",", ";");
    }
    static String unescape(String s) { return s; }
}
