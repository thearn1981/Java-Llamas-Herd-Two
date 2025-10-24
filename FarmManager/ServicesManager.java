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
        for (String line : Main.Store.readLines(Main.Store.SVC_FILE)) {
            ServiceAppointment s = ServiceAppointment.fromCSV(line);
            if (s != null) services.add(s);
        }
    }

    static void saveServices() {
        ArrayList<String> lines = new ArrayList<>();
        for (ServiceAppointment s : services) lines.add(s.toCSV());
        Main.Store.writeLines(Main.Store.SVC_FILE, lines);
    }

    /**
     * Services Menu Interface
     * --------------------
     * Main interface for service management.
     * Provides options to schedule, edit, and
     * track farm service appointments.
     */
    static void servicesMenu(ImageIcon logo) {
        String[] opts = {
            "Add service appointment",
            "Edit service",
            "Remove service",
            "List services",
            "Mark paid/unpaid",
            "Back"
        };

        while (true) {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            
            JLabel titleLabel = new JLabel("===== Services =====");
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
                "Services Menu",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                logo,
                opts,
                opts[0]
            );

            if (result == JOptionPane.CLOSED_OPTION || result == 5) break;

            try {
                switch (result) {
                    case 0 -> addService(logo);
                    case 1 -> editService(logo);
                    case 2 -> removeService(logo);
                    case 3 -> listServices(logo);
                    case 4 -> markPaid(logo);
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
        
        String cust = Main.clean(Main.prompt("Customer name:", logo));
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
        
        String cust = Main.promptDefault("Customer name:", s.customerName, logo);
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