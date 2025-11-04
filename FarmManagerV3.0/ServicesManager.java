import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * Farm Services Management
 * ---------------------
 * Manages scheduling and tracking of farm services.
 * Handles appointments, service records, and scheduling
 * for various farm-related services.
 */
public class ServicesManager {
    static ArrayList<ServiceAppointment> services = new ArrayList<>();

    /**
     * Service Appointment Data
     * ---------------------
     * Tracks service details including scheduling,
     * customer information, and pricing. Supports
     * date/time tracking for appointments.
     */
    static class ServiceAppointment {
        String serviceName;
        String customerName;
        String petName;
        String date; // "MM-DD-YYYY"
        String time;
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
            return Main.escape(serviceName) + "," + Main.escape(customerName) + "," +
                   Main.escape(petName) + "," + Main.escape(date) + "," +
                   Main.escape(time) + "," + price + "," + (paid ? "PAID" : "UNPAID");
        }

        static ServiceAppointment fromCSV(String line) {
            String[] p = line.split(",", -1);
            if (p.length < 7) return null;
            return new ServiceAppointment(
                Main.unescape(p[0]),
                Main.unescape(p[1]),
                Main.unescape(p[2]),
                Main.unescape(p[3]),
                Main.unescape(p[4]),
                Main.parseDouble(p[5]),
                "PAID".equalsIgnoreCase(p[6])
            );
        }

        @Override
        public String toString() {
            return String.format("%-16s | %s (%s) | %s @ %s | $%.2f | %s",
                serviceName, customerName, petName, date, time, price,
                (paid ? "PAID" : "UNPAID"));
        }
    }

    /**
     * File Operations
     * -------------
     * Handles service appointment data persistence.
     * Manages CSV storage for appointment records
     * with basic error handling.
     */
    static void loadServices() {
        services.clear();
        for (String line : Main.Store.readDataLines(Main.Store.SVC_FILE)) {
            ServiceAppointment s = ServiceAppointment.fromCSV(line);
            if (s != null) services.add(s);
        }
    }

    static void saveServices() {
        ArrayList<String> lines = new ArrayList<>();
        for (ServiceAppointment s : services) lines.add(s.toCSV());
        Main.Store.writeDataLines(Main.Store.SVC_FILE, Main.Store.SERVICES_HEADER, lines);
    }

    /**
     * Services Menu Interface
     * --------------------
     * Main interface for service management.
     * Provides options to schedule, edit, and
     * track farm service appointments.
     */
    static void servicesMenu(ImageIcon logo) {
    while (true) {
        // Calculate responsive dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int menuWidth = Math.min(500, (int)(screenSize.width * 0.4));
        int menuHeight = Math.min(400, (int)(screenSize.height * 0.5));
        
        // Header with responsive logo
        JLabel logoLabel = Main.createResponsiveLogo(menuWidth, menuHeight);

        JLabel titleLabel = new JLabel("========== Services ==========");
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
        JButton bPaid  = new JButton("Paid / Unpaid");
        JButton bBack  = new JButton("Back");

        // Uniform sizes (use the largest preferred size among buttons)
        JButton[] all = { bAdd, bEdit, bRem, bList, bPaid, bBack };
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

        // Row 2: List, Paid/Unpaid, Back (CENTER aligned)
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row2.add(bList); row2.add(bPaid); row2.add(bBack);
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
        JDialog dlg = pane.createDialog(null, "Services Menu");
        dlg.setModal(true);
        dlg.setResizable(false);

        // Actions
        Runnable close = dlg::dispose;
        bAdd.addActionListener(_  -> { pane.setValue("ADD");  close.run(); });
        bEdit.addActionListener(_ -> { pane.setValue("EDIT"); close.run(); });
        bRem.addActionListener(_  -> { pane.setValue("REM");  close.run(); });
        bList.addActionListener(_ -> { pane.setValue("LIST"); close.run(); });
        bPaid.addActionListener(_ -> { pane.setValue("PAID"); close.run(); });
        bBack.addActionListener(_ -> { pane.setValue("BACK"); close.run(); });

        dlg.setLocationRelativeTo(null);
        dlg.setVisible(true);

        Object val = pane.getValue();
        if (val == null || "BACK".equals(val)) break;

        try {
            switch (String.valueOf(val)) {
                case "ADD"  -> addService(logo);
                case "EDIT" -> editService(logo);
                case "REM"  -> removeService(logo);
                case "LIST" -> listServices(logo);
                case "PAID" -> markPaid(logo);      // your paid/unpaid handler
            }
        } catch (Exception e) {
            Main.error("Error: " + e.getMessage(), logo);
        }
    }
}


    /**
     * Service Management Actions
     * ----------------------
     * Core functions for managing services.
     * Handles scheduling, modification, and
     * tracking of service appointments.
     */
    static void addService(ImageIcon logo) {
        String svc = Main.clean(Main.prompt("Service name:", logo));
        if (svc == null || svc.isEmpty()) return;
        
        // Use enhanced customer selection
        String cust = Main.selectCustomerName("service appointment", logo);
        if (cust == null) return;
        
        String pet = Main.clean(Main.prompt("Pet name:", logo));
        if (pet == null) return;
        
        String date = Main.promptDateMDY(logo);
        String time = Main.clean(Main.prompt("Time (e.g., 2:00 PM):", logo));
        if (time == null) return;
        
        double price = Main.askDouble("Price:", logo);
        boolean paid = Main.askYesNo("Already paid?", logo);
        
        services.add(new ServiceAppointment(svc, cust, pet, date, time, price, paid));
        Main.info("Service appointment added.", logo);
    }

    static void editService(ImageIcon logo) {
        if (services.isEmpty()) {
            Main.info("No services yet.", logo);
            return;
        }
        
        int idx = Main.pickIndex("Select service ID to edit:", formatServicesTable(), services.size(), logo);
        if (idx < 0) return;
        
        ServiceAppointment s = services.get(idx);
        
        String svc = Main.promptDefault("Service name:", s.serviceName, logo);
        if (svc == null) return;
        
        String cust;
        // Check if there are existing customers to choose from
        if (!SalesManager.customers.isEmpty()) {
            boolean useExisting = Main.askYesNo("Select from existing customers?\n(No = enter manually)", logo);
            if (useExisting) {
                String[] customerOptions = new String[SalesManager.customers.size() + 1];
                // Add existing customers
                for (int i = 0; i < SalesManager.customers.size(); i++) {
                    SalesManager.Customer c = SalesManager.customers.get(i);
                    customerOptions[i] = c.name + " (ID: " + c.id + " - Phone: " + c.phone + ")";
                }
                // Add option to enter manually (keeping current value as default)
                customerOptions[SalesManager.customers.size()] = "Keep current: " + s.customerName;
                
                int selectedIndex = Main.selectFromDropdown("Select customer:", customerOptions, logo);
                if (selectedIndex == -1) return; // User cancelled
                
                if (selectedIndex < SalesManager.customers.size()) {
                    // Selected existing customer
                    cust = SalesManager.customers.get(selectedIndex).name;
                } else {
                    // Keep current value
                    cust = s.customerName;
                }
            } else {
                cust = Main.promptDefault("Customer name:", s.customerName, logo);
            }
        } else {
            cust = Main.promptDefault("Customer name:", s.customerName, logo);
        }
        if (cust == null) return;
        
        String pet = Main.promptDefault("Pet name:", s.petName, logo);
        if (pet == null) return;
        
        String date = Main.promptDefault("Date (MM-DD-YYYY):", s.date, logo);
        if (date == null) return;
        
        String time = Main.promptDefault("Time:", s.time, logo);
        if (time == null) return;
        
        String prStr = Main.promptDefault("Price:", String.valueOf(s.price), logo);
        if (prStr == null) return;
        
        s.serviceName = Main.clean(svc);
        s.customerName = Main.clean(cust);
        s.petName = Main.clean(pet);
        s.date = Main.clean(date);
        s.time = Main.clean(time);
        s.price = Main.parseDouble(prStr.trim());
        
        Main.info("Service updated.", logo);
    }

    static void removeService(ImageIcon logo) {
        if (services.isEmpty()) {
            Main.info("No services yet.", logo);
            return;
        }
        
        int idx = Main.pickIndex("Select service ID to remove:", formatServicesTable(), services.size(), logo);
        if (idx < 0) return;
        
        services.remove(idx);
        Main.info("Service removed.", logo);
    }

    static void listServices(ImageIcon logo) {
        if (services.isEmpty()) {
            Main.info("No services recorded.", logo);
            return;
        }
        Main.showLarge(formatServicesTable(), "Service Appointments", logo);
    }

    static void markPaid(ImageIcon logo) {
        if (services.isEmpty()) {
            Main.info("No services yet.", logo);
            return;
        }
        
        int idx = Main.pickIndex("Select service ID to mark paid/unpaid:", formatServicesTable(), services.size(), logo);
        if (idx < 0) return;
        
        ServiceAppointment s = services.get(idx);
        s.paid = Main.askYesNo("Mark as PAID?", logo);
        
        Main.info("Status updated to " + (s.paid ? "PAID" : "UNPAID"), logo);
    }

    static String formatServicesTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s %-16s %-14s %-14s %-12s %-10s %-8s %-8s%n",
            "ID", "Service", "Customer", "Pet", "Date", "Time", "Price", "Status"));
        sb.append("--------------------------------------------------------------------------------\n");
        
        for (int i = 0; i < services.size(); i++) {
            ServiceAppointment s = services.get(i);
            // Truncate fields if too long
            String svcName = s.serviceName.length() > 16 ? s.serviceName.substring(0, 13) + "..." : s.serviceName;
            String custName = s.customerName.length() > 14 ? s.customerName.substring(0, 11) + "..." : s.customerName;
            String petName = s.petName.length() > 14 ? s.petName.substring(0, 11) + "..." : s.petName;
            
            sb.append(String.format("%-4d %-16s %-14s %-14s %-12s %-10s $%-7.2f %-8s%n",
                i, svcName, custName, petName, s.date, s.time,
                s.price, (s.paid ? "PAID" : "UNPAID")));
        }
        
        return sb.toString();
    }
}