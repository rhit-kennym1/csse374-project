package example;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * GUI for selecting linters and files/packages to analyze.
 * Updates LinterConfig and optionally runs the analysis.
 */
public class LinterGUI extends JFrame {

    private static final String CONFIG_PATH = "src/main/java/example/LinterConfig";
    private static final String DEFAULT_TEST_PATH = "src/test/resources/testclasses";

    // Available linters
    private static final String[] SINGLE_CLASS_LINTERS = {
            "EqualsHashCode", "DeadCode", "UnusedVariables",
            "OpenClosedPrinciple", "DecoratorPattern", "DemeterPrinciple",
            "ObserverPattern", "FeatureEnvy", "AdapterPattern"
    };

    private static final String[] PACKAGE_LINTERS = {
            "CyclicDependency"
    };

    // GUI Components
    private JList<String> linterList;
    private JTextArea selectedFilesArea;
    private JTextArea configPreviewArea;
    private JButton selectFilesButton;
    private JButton selectPackageButton;
    private JButton clearButton;
    private JButton saveConfigButton;
    private JButton runAnalysisButton;
    private JCheckBox appendModeCheckBox;

    // Data
    private Map<String, List<String>> linterToTargets;

    public LinterGUI() {
        super("Linter Configuration Tool");
        linterToTargets = new LinkedHashMap<>();
        initializeGUI();
    }

    private void initializeGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLayout(new BorderLayout(10, 10));

        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left panel - Linter selection
        JPanel leftPanel = createLinterPanel();

        // Center panel - File selection and preview
        JPanel centerPanel = createCenterPanel();

        // Bottom panel - Action buttons
        JPanel bottomPanel = createBottomPanel();

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setLocationRelativeTo(null); // Center on screen
    }

    private JPanel createLinterPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Select Linter"));
        panel.setPreferredSize(new Dimension(200, 0));

        // Combine all linters
        List<String> allLinters = new ArrayList<>();
        allLinters.addAll(Arrays.asList(SINGLE_CLASS_LINTERS));
        allLinters.addAll(Arrays.asList(PACKAGE_LINTERS));

        linterList = new JList<>(allLinters.toArray(new String[0]));
        linterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(linterList);

        panel.add(new JLabel("Available Linters:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 10, 10));

        // Top - File selection
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.setBorder(BorderFactory.createTitledBorder("Selected Files/Packages"));

        selectedFilesArea = new JTextArea();
        selectedFilesArea.setEditable(false);
        selectedFilesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane fileScroll = new JScrollPane(selectedFilesArea);

        JPanel fileButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectFilesButton = new JButton("Select Class Files");
        selectPackageButton = new JButton("Select Package");
        clearButton = new JButton("Clear Selection");

        selectFilesButton.addActionListener(e -> selectClassFiles());
        selectPackageButton.addActionListener(e -> selectPackage());
        clearButton.addActionListener(e -> clearSelection());

        fileButtonPanel.add(selectFilesButton);
        fileButtonPanel.add(selectPackageButton);
        fileButtonPanel.add(clearButton);

        filePanel.add(fileButtonPanel, BorderLayout.NORTH);
        filePanel.add(fileScroll, BorderLayout.CENTER);

        // Bottom - Config preview
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));
        previewPanel.setBorder(BorderFactory.createTitledBorder("LinterConfig Preview"));

        configPreviewArea = new JTextArea();
        configPreviewArea.setEditable(false);
        configPreviewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane previewScroll = new JScrollPane(configPreviewArea);

        previewPanel.add(previewScroll, BorderLayout.CENTER);

        panel.add(filePanel);
        panel.add(previewPanel);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        appendModeCheckBox = new JCheckBox("Append to existing config", false);
        saveConfigButton = new JButton("Save to LinterConfig");
        runAnalysisButton = new JButton("Save & Run Analysis");

        saveConfigButton.addActionListener(e -> saveConfiguration(false));
        runAnalysisButton.addActionListener(e -> saveConfiguration(true));

        panel.add(appendModeCheckBox);
        panel.add(saveConfigButton);
        panel.add(runAnalysisButton);

        return panel;
    }

    private void selectClassFiles() {
        String selectedLinter = linterList.getSelectedValue();
        if (selectedLinter == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a linter first",
                    "No Linter Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check if this is a package linter
        if (Arrays.asList(PACKAGE_LINTERS).contains(selectedLinter)) {
            JOptionPane.showMessageDialog(this,
                    selectedLinter + " requires package selection.\nPlease use 'Select Package' button.",
                    "Package Linter",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser(DEFAULT_TEST_PATH);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Java Class Files (*.class)", "class"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            List<String> classNames = new ArrayList<>();

            for (File file : selectedFiles) {
                String className = fileToClassName(file);
                if (className != null) {
                    classNames.add(className);
                }
            }

            if (!classNames.isEmpty()) {
                linterToTargets.put(selectedLinter, classNames);
                updateDisplays();
            }
        }
    }

    private void selectPackage() {
        String selectedLinter = linterList.getSelectedValue();
        if (selectedLinter == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a linter first",
                    "No Linter Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser(DEFAULT_TEST_PATH);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            String packageName = dirToPackageName(selectedDir);

            if (packageName != null) {
                linterToTargets.put(selectedLinter, Arrays.asList("PACKAGE:" + packageName));
                updateDisplays();
            }
        }
    }

    private void clearSelection() {
        linterToTargets.clear();
        updateDisplays();
    }

    private void updateDisplays() {
        // Update selected files area
        StringBuilder filesText = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : linterToTargets.entrySet()) {
            filesText.append(entry.getKey()).append(":\n");
            for (String target : entry.getValue()) {
                filesText.append("  - ").append(target).append("\n");
            }
            filesText.append("\n");
        }
        selectedFilesArea.setText(filesText.toString());

        // Update config preview
        configPreviewArea.setText(generateConfigContent());
    }

    private String generateConfigContent() {
        StringBuilder config = new StringBuilder();
        config.append("#Format for config is Linter: class1, class2 etc\n");
        config.append("#Generated by LinterGUI\n\n");

        for (Map.Entry<String, List<String>> entry : linterToTargets.entrySet()) {
            String linter = entry.getKey();
            List<String> targets = entry.getValue();

            config.append(linter).append(": ");
            config.append(String.join(", ", targets));
            config.append("\n");
        }

        return config.toString();
    }

    private void saveConfiguration(boolean runAnalysis) {
        if (linterToTargets.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No linters configured. Please select files/packages first.",
                    "Nothing to Save",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String configContent = generateConfigContent();

            if (appendModeCheckBox.isSelected()) {
                // Read existing config and append
                String existingContent = "";
                try {
                    existingContent = new String(Files.readAllBytes(Paths.get(CONFIG_PATH)));
                } catch (IOException e) {
                    // File doesn't exist, that's okay
                }
                configContent = existingContent + "\n" + configContent;
            }

            Files.write(Paths.get(CONFIG_PATH), configContent.getBytes());

            JOptionPane.showMessageDialog(this,
                    "Configuration saved to " + CONFIG_PATH,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            if (runAnalysis) {
                runASMMain();
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error saving configuration: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runASMMain() {
        try {
            // Run ASMMain in a separate thread to avoid blocking GUI
            new Thread(() -> {
                try {
                    ASMMain.main(new String[0]);
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Analysis complete! Check console for results.",
                            "Analysis Complete",
                            JOptionPane.INFORMATION_MESSAGE));
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                            "Error running analysis: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE));
                }
            }).start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error starting analysis: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Convert file path to fully qualified class name
     */
    private String fileToClassName(File file) {
        try {
            String path = file.getAbsolutePath();

            // Find "testclasses" in the path
            int testclassesIndex = path.indexOf("testclasses");
            if (testclassesIndex == -1) {
                return null;
            }

            // Extract from testclasses onwards
            String relativePath = path.substring(testclassesIndex);

            // Remove .class extension and convert to package notation
            relativePath = relativePath.replace(".class", "");
            relativePath = relativePath.replace(File.separatorChar, '.');

            return relativePath;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert directory path to package name
     */
    private String dirToPackageName(File dir) {
        try {
            String path = dir.getAbsolutePath();

            // Find "testclasses" in the path
            int testclassesIndex = path.indexOf("testclasses");
            if (testclassesIndex == -1) {
                return null;
            }

            // Extract from testclasses onwards
            String relativePath = path.substring(testclassesIndex);

            // Convert to package notation
            relativePath = relativePath.replace(File.separatorChar, '.');

            return relativePath;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LinterGUI gui = new LinterGUI();
            gui.setVisible(true);
        });
    }
}