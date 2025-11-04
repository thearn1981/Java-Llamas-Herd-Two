import java.awt.*;
import java.util.*;
import javax.swing.*;

/**
 * UI - User Interface Components
 * 
 * Purpose: Handles all GUI-related functionality for the Farm Life Inc. system.
 * This class contains all dialog boxes, input prompts, message displays,
 * and visual components used throughout the application.
 * 
 * Features:
 * - Text wrapping for long messages
 * - Consistent styling and branding
 * - Input validation and error handling
 * - Responsive logo and layout design
 * - Customer selection workflows
 */
public class UI {
    
    /**
     * GUI Dialogs
     * -----------
     * Collection of standard dialog boxes for user interaction.
     * Includes input prompts, messages, confirmations, and 
     * data entry with validation. All dialogs use consistent
     * styling and error handling with automatic text wrapping.
     */
    public static String prompt(String msg, ImageIcon logo) {
        // Create a wrapped message panel for long text
        JPanel messagePanel = createWrappedMessagePanel(msg, 400);
        
        String result = (String) JOptionPane.showInputDialog(
            null, 
            messagePanel, 
            "Farm Life Inc.", 
            JOptionPane.PLAIN_MESSAGE, 
            logo, 
            null, 
            ""
        );
        return result;
    }

    public static String promptDefault(String msg, String def, ImageIcon logo) {
        // Create a wrapped message panel for long text
        JPanel messagePanel = createWrappedMessagePanel(msg, 400);
        
        String result = (String) JOptionPane.showInputDialog(
            null, 
            messagePanel, 
            "Farm Life Inc.", 
            JOptionPane.PLAIN_MESSAGE, 
            logo, 
            null, 
            def
        );
        return (result == null) ? null : result;
    }

    public static void info(String msg, ImageIcon logo) {
        JPanel messagePanel = createWrappedMessagePanel(msg, 400);
        JOptionPane.showMessageDialog(null, messagePanel, "Farm Life Inc.", JOptionPane.INFORMATION_MESSAGE, logo);
    }

    public static void error(String msg, ImageIcon logo) {
        JPanel messagePanel = createWrappedMessagePanel(msg, 400);
        JOptionPane.showMessageDialog(null, messagePanel, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean askYesNo(String msg, ImageIcon logo) {
        JPanel messagePanel = createWrappedMessagePanel(msg, 400);
        int res = JOptionPane.showConfirmDialog(null, messagePanel, "Farm Life Inc.", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, logo);
        return (res == JOptionPane.YES_OPTION);
    }

    public static void showLarge(String text, String title, ImageIcon logo) {
        JTextArea ta = new JTextArea(text, 18, 65);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(ta);
        JOptionPane.showMessageDialog(null, sp, title, JOptionPane.PLAIN_MESSAGE, logo);
    }

    public static int askInt(String msg, ImageIcon logo) {
        String in = prompt(msg, logo);
        if (in == null || in.isBlank()) return 0;
        return Main.parseInt(in.trim());
    }

    public static double askDouble(String msg, ImageIcon logo) {
        String in = prompt(msg, logo);
        if (in == null || in.isBlank()) return 0.0;
        return Main.parseDouble(in.trim());
    }

    public static int pickIndex(String msg, String listText, int max, ImageIcon logo) {
        if (max == 0) { error("No items available.", logo); return -1; }
        showLarge(listText, "Selection", logo);
        int idx = askInt(msg + " (0-based ID):", logo);
        if (idx < 0 || idx >= max) { error("Invalid ID.", logo); return -1; }
        return idx;
    }

    public static String promptDateMDY(ImageIcon logo) {
        String last = "";
        while (true) {
            String in = promptDefault("New date (MM-DD-YYYY):", last, logo);
            if (in == null) throw new RuntimeException("Cancelled");
            in = Main.clean(in);
            if (in.matches("\\d{2}-\\d{2}-\\d{4}")) return in;
            error("Please use Month-Day-Year as MM-DD-YYYY (e.g., 10-24-2025).", logo);
            last = in;
        }
    }

    public static int selectFromDropdown(String msg, String[] options, ImageIcon logo) {
        if (options == null || options.length == 0) {
            error("No options available.", logo);
            return -1;
        }

        Object selected = JOptionPane.showInputDialog(
            null,
            msg,
            "Farm Life Inc. - Selection",
            JOptionPane.PLAIN_MESSAGE,
            logo,
            options,
            options[0]
        );

        if (selected == null) return -1; // User cancelled
        
        // Find the index of the selected option
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(selected)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Customer Selection Workflows
     * ---------------------------
     * Enhanced customer selection with search, browse, and new customer options.
     * Provides a user-friendly way to find and select customers for various operations.
     */
    public static String selectCustomerName(String purpose, ImageIcon logo) {
        if (SalesManager.customers.isEmpty()) {
            String name = Main.clean(prompt("Customer name (new customer):", logo));
            return (name == null || name.isEmpty()) ? null : name;
        }

        // Initial choice: Search, Browse All, or New Customer
        String[] mainOptions = {
            "Search for existing customer", 
            "Browse all customers (" + SalesManager.customers.size() + " total)", 
            "Enter new customer name"
        };
        
        int choice = selectFromDropdown("How would you like to select customer for " + purpose + "?", 
                                       mainOptions, logo);
        if (choice == -1) return null; // User cancelled
        
        return switch (choice) {
            case 0 -> // Search
                searchAndSelectCustomer(logo);
            case 1 -> // Browse all
                browseAllCustomers(logo);
            case 2 -> { // New customer
                String name = Main.clean(prompt("Enter new customer name:", logo));
                yield (name == null || name.isEmpty()) ? null : name;
            }
            default -> null;
        };
    }

    public static String searchAndSelectCustomer(ImageIcon logo) {
        while (true) {
            String searchTerm = Main.clean(prompt("Enter customer name or phone to search:", logo));
            if (searchTerm == null) return null; // User cancelled
            
            if (searchTerm.isEmpty()) {
                error("Please enter a search term.", logo);
                continue;
            }
            
            // Find matching customers
            java.util.List<SalesManager.Customer> matches = new ArrayList<>();
            String searchLower = searchTerm.toLowerCase();
            
            for (SalesManager.Customer c : SalesManager.customers) {
                if (c.name.toLowerCase().contains(searchLower) || 
                    c.phone.contains(searchTerm) ||
                    c.id.contains(searchTerm)) {
                    matches.add(c);
                }
            }
            
            if (matches.isEmpty()) {
                boolean tryAgain = askYesNo("No customers found matching '" + searchTerm + 
                                          "'.\nTry a different search?", logo);
                if (!tryAgain) {
                    // Offer to create new customer
                    boolean createNew = askYesNo("Create new customer with name '" + searchTerm + "'?", logo);
                    return createNew ? searchTerm : null;
                }
                continue;
            }
            
            if (matches.size() == 1) {
                // Only one match, confirm selection
                SalesManager.Customer c = matches.get(0);
                boolean confirm = askYesNo("Select customer: " + c.name + 
                                         " (ID: " + c.id + ", Phone: " + c.phone + ")?", logo);
                return confirm ? c.name : null;
            }
            
            // Multiple matches, let user choose
            String[] matchOptions = new String[matches.size() + 1];
            for (int i = 0; i < matches.size(); i++) {
                SalesManager.Customer c = matches.get(i);
                matchOptions[i] = c.name + " (ID: " + c.id + " - Phone: " + c.phone + ")";
            }
            matchOptions[matches.size()] = "Search again";
            
            int selection = selectFromDropdown("Found " + matches.size() + " customers:", 
                                             matchOptions, logo);
            if (selection == -1) return null; // User cancelled
            
            if (selection < matches.size()) {
                return matches.get(selection).name;
            }
            // Otherwise, search again (continue loop)
        }
    }

    public static String browseAllCustomers(ImageIcon logo) {
        if (SalesManager.customers.size() > 20) {
            boolean proceed = askYesNo("You have " + SalesManager.customers.size() + 
                                     " customers. This may take time to browse.\n" +
                                     "Consider using search instead. Continue browsing?", logo);
            if (!proceed) return null;
        }
        
        String[] customerOptions = new String[SalesManager.customers.size() + 1];
        for (int i = 0; i < SalesManager.customers.size(); i++) {
            SalesManager.Customer c = SalesManager.customers.get(i);
            customerOptions[i] = c.name + " (ID: " + c.id + " - Phone: " + c.phone + ")";
        }
        customerOptions[SalesManager.customers.size()] = "Enter new customer name instead";
        
        int selectedIndex = selectFromDropdown("Select customer:", customerOptions, logo);
        if (selectedIndex == -1) return null; // User cancelled
        
        if (selectedIndex < SalesManager.customers.size()) {
            return SalesManager.customers.get(selectedIndex).name;
        } else {
            // Enter new customer
            String name = Main.clean(prompt("Enter new customer name:", logo));
            return (name == null || name.isEmpty()) ? null : name;
        }
    }

    /**
     * Logo and Branding
     * -----------------
     * Methods for creating and managing the Farm Life Inc. logo and branding
     * elements throughout the application interface.
     */
    public static ImageIcon createLogo() {
        // Load the image file (place it in your project resources folder or same directory)
        String imagePath = "farmLifeIcon.png"; // <-- rename to match your actual image file

        // Load the image
        ImageIcon icon = new ImageIcon(imagePath);

        // Optionally scale it down (64x64 for JOptionPane or menu icon)
        Image scaled = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);

        return new ImageIcon(scaled);
    }

    public static JLabel createResponsiveLogo(int maxWidth, int maxHeight) {
        try {
            // Load the image file
            String imagePath = "farmLifeIcon.png";
            ImageIcon icon = new ImageIcon(imagePath);
            
            // If image doesn't exist, create a placeholder
            if (icon.getIconWidth() <= 0) {
                // Create a simple colored rectangle as placeholder
                java.awt.image.BufferedImage placeholder = new java.awt.image.BufferedImage(
                    maxWidth, maxHeight/3, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2d = placeholder.createGraphics();
                g2d.setColor(new java.awt.Color(34, 139, 34)); // Forest green
                g2d.fillRect(0, 0, maxWidth, maxHeight/3);
                g2d.setColor(java.awt.Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, maxWidth/20)));
                String text = "FARM LIFE";
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (maxWidth - fm.stringWidth(text)) / 2;
                int textY = ((maxHeight/3) + fm.getAscent()) / 2;
                g2d.drawString(text, textX, textY);
                g2d.dispose();
                
                icon = new ImageIcon(placeholder);
            } else {
                // Scale existing image to fit within maxWidth and maxHeight/3
                int logoHeight = Math.min(maxHeight / 3, 120); // Max 120px height
                int logoWidth = (icon.getIconWidth() * logoHeight) / icon.getIconHeight();
                if (logoWidth > maxWidth) {
                    logoWidth = maxWidth;
                    logoHeight = (icon.getIconHeight() * logoWidth) / icon.getIconWidth();
                }
                
                Image scaled = icon.getImage().getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaled);
            }
            
            JLabel logoLabel = new JLabel(icon);
            logoLabel.setHorizontalAlignment(JLabel.CENTER);
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            return logoLabel;
            
        } catch (Exception e) {
            // Fallback to text label if image loading fails
            JLabel textLogo = new JLabel("🚜 FARM LIFE INC 🌾");
            textLogo.setFont(new Font("SansSerif", Font.BOLD, Math.max(16, maxWidth/25)));
            textLogo.setHorizontalAlignment(JLabel.CENTER);
            textLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
            textLogo.setForeground(new java.awt.Color(34, 139, 34));
            return textLogo;
        }
    }

    /**
     * Text Wrapping Utilities
     * -----------------------
     * Helper methods for creating wrapped text panels that prevent
     * horizontal scrolling in dialog boxes.
     */
    // Helper method to create wrapped text panels
    public static JPanel createWrappedMessagePanel(String message, int maxWidth) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Split long messages into multiple lines if needed
        String[] lines = wrapText(message, maxWidth);
        
        for (String line : lines) {
            JLabel label = new JLabel(line);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(label);
        }
        
        return panel;
    }

    // Helper method to wrap text based on character count and natural breaks
    public static String[] wrapText(String text, int maxWidth) {
        if (text == null || text.length() <= 60) {
            return new String[]{text};
        }
        
        java.util.List<String> lines = new ArrayList<>();
        String[] sentences = text.split("\\. |\\? |! ");
        StringBuilder currentLine = new StringBuilder();
        
        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];
            if (i < sentences.length - 1) {
                // Add back the punctuation (except for the last sentence)
                char lastChar = text.charAt(text.indexOf(sentence) + sentence.length());
                sentence += lastChar + " ";
            }
            
            // If adding this sentence would make the line too long, start a new line
            if (currentLine.length() + sentence.length() > 60 && currentLine.length() > 0) {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder(sentence);
            } else {
                currentLine.append(sentence);
            }
        }
        
        // Add the last line
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }
        
        return lines.toArray(String[]::new);
    }

    /**
     * Main Menu Creation
     * -----------------
     * Creates and displays the main application menu with responsive design
     * and proper button layout.
     */
    public static JPanel createMainMenuPanel(int menuWidth, int menuHeight) {
        // Create responsive logo
        JLabel logoLabel = createResponsiveLogo(menuWidth, menuHeight);

        JLabel titleLabel = new JLabel(" +=========== Farm Life Inc. ===========+ ");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        JSeparator sep = new JSeparator();
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        sep.setMaximumSize(new Dimension(menuWidth - 40, 2)); 

        JLabel instructionLabel = new JLabel("Select an option:");
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionLabel.setHorizontalAlignment(JLabel.CENTER);

        // Create buttons
        JButton bInv   = new JButton("Inventory");
        JButton bSvc   = new JButton("Services");
        JButton bAni   = new JButton("Animals");
        JButton bCust  = new JButton("Customers");
        JButton bInvs  = new JButton("Invoices");
        JButton bExit  = new JButton("Exit");

        // Make all buttons the same size
        JButton[] allButtons = { bInv, bSvc, bAni, bCust, bInvs, bExit };
        standardizeButtonSizes(allButtons);

        // Create button rows
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row1.add(bInv); row1.add(bSvc); row1.add(bAni);
        row1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row2.add(bCust); row2.add(bInvs); row2.add(bExit);
        row2.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create root panel
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

        // Store buttons in the panel for access by Main class
        root.putClientProperty("buttons", allButtons);
        
        return root;
    }

    // Helper method to make all buttons the same size
    private static void standardizeButtonSizes(JButton[] buttons) {
        int maxW = 0, maxH = 0;
        for (JButton b : buttons) {
            Dimension d = b.getPreferredSize(); 
            maxW = Math.max(maxW, d.width);
            maxH = Math.max(maxH, d.height);
        }
        Dimension uniformSize = new Dimension(maxW, maxH);
        for (JButton b : buttons) {
            b.setPreferredSize(uniformSize);
            b.setMinimumSize(uniformSize);
            b.setMaximumSize(uniformSize);
        }
    }
}