package src.main.java;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class AutoInstall {

    private JPanel checkBoxPanel;
    private JScrollPane scrollPane;
    private JPanel bottom;
    private JFrame app;
    private static File[] files;
    private static HashMap<String, Boolean> checklist;
    private JButton runButton;

    public void gui() {
        app = new JFrame("AutoInstall - Java");
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        app.setSize(600,600);
        app.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        bottom = new JPanel();

        top.setLayout(new FlowLayout());
        bottom.setLayout(new BorderLayout());

        JButton browseButton = new JButton("Select Directory");
        JTextField directoryPathText = new JTextField(20);
        directoryPathText.setEditable(false);

        checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(checkBoxPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                int option = fileChooser.showOpenDialog(app);
                if(option == JFileChooser.APPROVE_OPTION) {
                    File dir = fileChooser.getSelectedFile();
                    directoryPathText.setText(dir.getAbsolutePath());
                    files = dir.listFiles();
                    checkBoxPanel.removeAll();

                    checklist = new HashMap<String, Boolean>();

                    // Add default checkboxes for common applications available through WinGet.
                    addCheckbox("Cisco Webex");
                    addCheckbox("Microsoft Teams");
                    addCheckbox("Microsoft Edge");

                    // Add checkboxes for each file in the directory.
                    for (File file : files) {
                        addCheckbox(file.getName());
                    }

                    // Refresh the GUI to display checkboxes.
                    SwingUtilities.invokeLater(() -> {
                        checkBoxPanel.revalidate();
                        checkBoxPanel.repaint();
                        scrollPane.revalidate();
                        scrollPane.repaint();
                        bottom.revalidate();
                        bottom.repaint();
                        app.revalidate();
                        app.repaint();
                    });
                }
            }
        });

        runButton = new JButton("Run");
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Print the state of the checklist before executing commands
                for (Map.Entry<String, Boolean> entry : checklist.entrySet()) {
                    System.out.println("Before execution: " + entry.getKey() + " selected: " + entry.getValue());
                }
                
                // Disable the run button while installation is in progress
                runButton.setEnabled(false);
                
                // Virtual thread to execute commands. 
                Thread.ofVirtual().start(() -> {
                    executeCommands();
                    // Re-enable the run button and show completion dialog on the EDT
                    SwingUtilities.invokeLater(() -> {
                        runButton.setEnabled(true);
                        showCompletionDialog();
                    });
                });
            }
        });

        top.add(browseButton);
        top.add(directoryPathText);
        top.add(runButton);

        bottom.add(scrollPane, BorderLayout.CENTER);

        app.add(top, BorderLayout.NORTH);
        app.add(bottom, BorderLayout.CENTER);

        app.setVisible(true);
    }

    private void addCheckbox(String name) {
        JCheckBox checkBox = new JCheckBox(name);
        checklist.put(name, false);

        checkBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checklist.put(name, checkBox.isSelected());
                System.out.println(name + " selected: " + checkBox.isSelected()); // Debug output
            }
        });

        checkBoxPanel.add(checkBox);
    }

    public static void executeCommands() {
        try {
            int totalSteps = checklist.size() + 2; // +2 for Winget check and source agreement
            int currentStep = 0;

            System.out.println("Starting execution with " + totalSteps + " total steps.");

            // Check if Winget is installed
            System.out.println("Checking if Winget is installed...");
            if (!isWingetInstalled()) {
                System.out.println("Winget not found. Installing Winget...");
                installWinget();
            } else {
                System.out.println("Winget is already installed.");
            }
            updateProgress(++currentStep, totalSteps);

            System.out.println("Checklist contains " + checklist.size() + " items.");
            for (Map.Entry<String, Boolean> entry : checklist.entrySet()) {
                System.out.println("Checking item: " + entry.getKey() + ", Selected: " + entry.getValue());
                if (entry.getValue()) {
                    System.out.println("Attempting to install " + entry.getKey() + "...");
                    ProcessBuilder processBuilder;
                    switch (entry.getKey()) {
                        case "Microsoft Teams":
                            processBuilder = new ProcessBuilder(
                                "winget", "install", "Microsoft.Teams", 
                                "--accept-package-agreements", "--accept-source-agreements"
                            );
                            break;
                        case "Cisco Webex":
                            processBuilder = new ProcessBuilder(
                                "winget", "install", "Cisco.Webex", 
                                "--accept-package-agreements", "--accept-source-agreements"
                            );
                            break;
                        case "Microsoft Edge":
                            processBuilder = new ProcessBuilder(
                                "winget", "install", "Microsoft.Edge", 
                                "--accept-package-agreements", "--accept-source-agreements"
                            );
                            break;
                        default:
                            String filePath = new File(files[0].getParent(), entry.getKey()).getAbsolutePath();
                            if (entry.getKey().toLowerCase().endsWith(".msi")) {
                                processBuilder = new ProcessBuilder(
                                    "powershell.exe", "-Command",
                                    "Start-Process msiexec.exe -ArgumentList '/i \"" + filePath + "\" /qn' -Wait -NoNewWindow"
                                );
                            } else if (entry.getKey().toLowerCase().endsWith(".exe")) {
                                processBuilder = new ProcessBuilder(
                                    "powershell.exe", "-Command",
                                    "Start-Process \"" + filePath + "\" -ArgumentList '/S' -Wait -NoNewWindow"
                                );
                            } else {
                                System.out.println("Skipping unknown item: " + entry.getKey());
                                continue;
                            }
                    }
                    
                    System.out.println("Executing command: " + String.join(" ", processBuilder.command()));
                    Process process = processBuilder.start();
                    
                    // Capture and print the output
                    new Thread(() -> {
                        try (java.util.Scanner s = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A")) {
                            while (s.hasNext()) {
                                System.out.println(s.next());
                            }
                        }
                    }).start();

                    // Capture and print any errors
                    new Thread(() -> {
                        try (java.util.Scanner s = new java.util.Scanner(process.getErrorStream()).useDelimiter("\\A")) {
                            while (s.hasNext()) {
                                System.err.println(s.next());
                            }
                        }
                    }).start();

                    // Wait for the process to complete
                    int exitCode = process.waitFor();
                    System.out.println(entry.getKey() + " installation completed with exit code: " + exitCode);
                    
                    updateProgress(++currentStep, totalSteps);
                }
            }
            System.out.println("Installation process completed.");
        } catch (Exception e) {
            System.err.println("An error occurred during execution:");
            e.printStackTrace();
        }
    }

    private static boolean isWingetInstalled() throws IOException, InterruptedException {
        ProcessBuilder checkWinget = new ProcessBuilder("where", "winget");
        Process process = checkWinget.start();
        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    private static void installWinget() throws IOException, InterruptedException {
        ProcessBuilder installWinget = new ProcessBuilder(
            "powershell.exe", "-Command",
            "Invoke-WebRequest -Uri https://github.com/microsoft/winget-cli/releases/latest/download/Microsoft.DesktopAppInstaller_8wekyb3d8bbwe.msixbundle -OutFile winget.msixbundle; " +
            "Add-AppxPackage .\\winget.msixbundle"
        );
        installWinget.start().waitFor();
    }

    private static void updateProgress(int currentStep, int totalSteps) {
        int progressPercentage = (int) ((double) currentStep / totalSteps * 100);
        System.out.println("Progress: " + progressPercentage + "%");
    }

    private void showCompletionDialog() {
        JOptionPane.showMessageDialog(
            app,
            "Installation process completed.",
            "Process Complete",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static boolean isAdmin() {
        String systemPath = System.getenv("SystemRoot") + File.separator + "System32";
        Path testPath = Paths.get(systemPath, "test_write_access.tmp");

        try {
            Files.createFile(testPath);
            Files.delete(testPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        if (isAdmin()) {
            SwingUtilities.invokeLater(() -> {
                AutoInstall autoInstall = new AutoInstall();
                autoInstall.gui();
            });
        } else {
            JOptionPane.showMessageDialog(
                null, 
                "Please run this program as an administrator.", 
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}