import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AnimalsManager {
    private final List<AnimalSale> animals = new ArrayList<>();

    /** ==================== INNER CLASS ==================== */
    static class AnimalSale {
        String name, breed, gender, age, breederName;
        double price;

        AnimalSale(String name, String breed, String gender, String age, double price, String breederName) {
            this.name = name;
            this.breed = breed;
            this.gender = gender;
            this.age = age;
            this.price = price;
            this.breederName = breederName;
        }

        String toCSV() {
            return String.join(",",
                    Main.escape(name),
                    Main.escape(breed),
                    Main.escape(gender),
                    Main.escape(age),
                    String.valueOf(price),
                    Main.escape(breederName));
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

    /** ==================== FILE OPS ==================== */
    public void loadAnimals() {
        animals.clear();
        for (String line : Main.Store.readLines(Main.Store.ANM_FILE)) {
            AnimalSale a = AnimalSale.fromCSV(line);
            if (a != null) animals.add(a);
        }
    }

    public void saveAnimals() {
        List<String> lines = animals.stream().map(AnimalSale::toCSV).toList();
        Main.Store.writeLines(Main.Store.ANM_FILE, lines);
    }

    /** ==================== MENU ==================== */
    public void showMenu(ImageIcon logo) {
        while (true) {
            Object choice = createMenuDialog(logo);
            if (choice == null || choice.equals("BACK")) break;

            try {
                switch (String.valueOf(choice)) {
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

    private Object createMenuDialog(ImageIcon logo) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("      +========== Animals ==========+ ");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton bAdd = new JButton("Add");
        JButton bEdit = new JButton("Edit");
        JButton bRem = new JButton("Remove");
        JButton bList = new JButton("List");
        JButton bBack = new JButton("Back");

        JPanel buttons = new JPanel(new GridLayout(2, 3, 8, 8));
        buttons.add(bAdd);
        buttons.add(bEdit);
        buttons.add(bRem);
        buttons.add(bList);
        buttons.add(bBack);

        root.add(title);
        root.add(Box.createVerticalStrut(10));
        root.add(buttons);

        JOptionPane pane = new JOptionPane(
                root, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, new Object[]{}, null
        );
        JDialog dlg = pane.createDialog(null, "Animals Menu");

        Runnable close = dlg::dispose;
        bAdd.addActionListener(_ -> { pane.setValue("ADD"); close.run(); });
        bEdit.addActionListener(_ -> { pane.setValue("EDIT"); close.run(); });
        bRem.addActionListener(_ -> { pane.setValue("REM"); close.run(); });
        bList.addActionListener(_ -> { pane.setValue("LIST"); close.run(); });
        bBack.addActionListener(_ -> { pane.setValue("BACK"); close.run(); });

        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);
        return pane.getValue();
    }

    /** ==================== CRUD ACTIONS ==================== */
    private void addAnimal(ImageIcon logo) {
        String name = promptField("Animal name/ID:", null, logo);
        if (name == null) return;

        String breed = promptField("Breed:", null, logo);
        if (breed == null) return;

        String gender = promptField("Gender:", null, logo);
        if (gender == null) return;

        String age = promptField("Age:", null, logo);
        if (age == null) return;

        double price = Main.askDouble("Price:", logo);
        String breeder = promptField("Breeder name (optional):", "", logo);

        animals.add(new AnimalSale(name, breed, gender, age, price, breeder));
        Main.info("Animal sale added.", logo);
    }

    private void editAnimal(ImageIcon logo) {
        if (animals.isEmpty()) {
            Main.info("No animals yet.", logo);
            return;
        }

        int idx = Main.pickIndex("Select animal to edit:", formatAnimalsTable(), animals.size(), logo);
        if (idx < 0) return;

        AnimalSale a = animals.get(idx);
        a.name = promptField("Name/ID:", a.name, logo);
        a.breed = promptField("Breed:", a.breed, logo);
        a.gender = promptField("Gender:", a.gender, logo);
        a.age = promptField("Age:", a.age, logo);
        a.price = Main.parseDouble(promptField("Price:", String.valueOf(a.price), logo));
        a.breederName = promptField("Breeder:", a.breederName, logo);

        Main.info("Animal updated.", logo);
    }

    private void removeAnimal(ImageIcon logo) {
        if (animals.isEmpty()) {
            Main.info("No animals yet.", logo);
            return;
        }
        int idx = Main.pickIndex("Select animal ID to remove:", formatAnimalsTable(), animals.size(), logo);
        if (idx < 0) return;
        animals.remove(idx);
        Main.info("Animal removed.", logo);
    }

    private void listAnimals(ImageIcon logo) {
        if (animals.isEmpty()) {
            Main.info("No animals recorded.", logo);
            return;
        }
        Main.showLarge(formatAnimalsTable(), "Animal Sales", logo);
    }

    private String formatAnimalsTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s %-14s %-12s %-8s %-8s %-10s %-20s%n",
                "ID", "Name", "Breed", "Gender", "Age", "Price", "Breeder"));
        sb.append("-".repeat(80)).append("\n");

        for (int i = 0; i < animals.size(); i++) {
            AnimalSale a = animals.get(i);
            sb.append(String.format("%-4d %-14s %-12s %-8s %-8s $%-9.2f %-20s%n",
                    i,
                    truncate(a.name, 14),
                    truncate(a.breed, 12),
                    truncate(a.gender, 8),
                    truncate(a.age, 8),
                    a.price,
                    truncate(a.breederName == null || a.breederName.isBlank() ? "—" : a.breederName, 20)
            ));
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max - 3) + "..." : s;
    }

    private static String promptField(String label, String def, ImageIcon logo) {
        String input = (def == null)
                ? Main.prompt(label, logo)
                : Main.promptDefault(label, def, logo);
        return (input == null) ? null : Main.clean(input);
    }
}
