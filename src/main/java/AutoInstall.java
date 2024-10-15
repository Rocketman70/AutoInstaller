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
import java.util.Map.Entry;

//TO-DO: Add functionality to run button to execute commands.

public class AutoInstall {

    //GLOBAL VARIABLES JUST FOR FUN
    private JPanel checkBoxPanel;
    private JScrollPane scrollPane;
    private JPanel bottom;
    private JFrame app;
    private static File[] files;
    private static HashMap<String, Boolean> checklist;

    public void gui() {
        //Create frame for GUI.
        app = new JFrame("AutoInstall - Java");
        app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        app.setSize(600,600);
        app.setLayout(new BorderLayout());

        //Panels for the top and bottom of the window
        JPanel top = new JPanel();
        bottom = new JPanel();

        //Set dimension proportions for panels.
        top.setLayout(new FlowLayout());
        bottom.setLayout(new BorderLayout());

        //Top area Browse button and corresponding text field
        JButton browseButton = new JButton("Select Directory");
        JTextField directoryPathText = new JTextField(20);
        directoryPathText.setEditable(false);

        //Create panel to hold checkboxes.
        checkBoxPanel = new JPanel();
        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

        //Create scroll pane for checkboxes.
        scrollPane = new JScrollPane(checkBoxPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        //Open file dialog upon button press.
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                //Create file chooser dialog for directory selection.
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                //Open file dialog and get user selection.
                int option = fileChooser.showOpenDialog(app);
                if(option == JFileChooser.APPROVE_OPTION) {
                    File dir = fileChooser.getSelectedFile();
                    directoryPathText.setText(dir.getAbsolutePath());
                    //Create file array to create optional checkboxes for user.
                    files = dir.listFiles();
                    checkBoxPanel.removeAll();

                    checklist = new HashMap<String, Boolean>();
                    checklist.clear();
                    

                    // Add default checkboxes for common applications available through WinGet.
                    JCheckBox webEx = new JCheckBox("Cisco Webex");
                    JCheckBox teams = new JCheckBox("Microsoft Teams");
                    JCheckBox mEdge = new JCheckBox("Microsoft Edge");
                    checkBoxPanel.add(webEx);
                    checkBoxPanel.add(teams);
                    checkBoxPanel.add(mEdge);

                    checklist.put("Microsoft Teams", false);
                    checklist.put("Cisco Webex", false);
                    checklist.put("Microsoft Edge", false);

                    //Add checkboxes for each file in the directory.
                    //Add checkboxes for each file in the directory.
                    for (File file : files) {
                        JCheckBox checkBox = new JCheckBox(file.getName());
                        checklist.put(file.getName(), false);

                        checkBox.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                // Update the checklist directly based on the checkbox state.
                                checklist.put(checkBox.getText(), checkBox.isSelected());
                            }
                        });

                        checkBoxPanel.add(checkBox);
                    }
                    //Refresh the GUI to display checkboxes.
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

        //Create run button to execute installation.
        JButton runButton = new JButton("Run");
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Virtual thread to execute commands. 
                Thread.ofVirtual().start(() -> executeCommands());
            }
        });

        //Add components to the top panel.
        top.add(browseButton);
        top.add(directoryPathText);
        top.add(runButton);

        //Add scroll pane to bottom panel
        bottom.add(scrollPane, BorderLayout.CENTER);

        //Add panels to the frame.
        app.add(top, BorderLayout.NORTH);
        app.add(bottom, BorderLayout.CENTER);

        //Make visible to user.
        app.setVisible(true);
    }

    public static void executeCommands() {
        try {
            // Install WinGet
            ProcessBuilder installWinget = new ProcessBuilder(
                "powershell.exe", "-Command",
                "Invoke-WebRequest -Uri https://github.com/microsoft/winget-cli/releases/latest/download/Microsoft.DesktopAppInstaller_8wekyb3d8bbwe.msixbundle -OutFile winget.msixbundle; " +
                "Add-AppxPackage .\\winget.msixbundle"
            );
            installWinget.start().waitFor();  // Wait for WinGet installation to complete
    
            // Accept source agreements
            ProcessBuilder acceptAgreements = new ProcessBuilder(
                "winget", "install", "--accept-source-agreements"
            );
            acceptAgreements.start().waitFor();  // Wait for agreement acceptance to complete
    
            for (Entry<String, Boolean> entry : checklist.entrySet()) {
                if (entry.getValue()) {
                    // Handle cases for Winget Applications. Otherwise install selected MSI files.
                    switch (entry.getKey()) {
                        case "Microsoft Teams":
                            ProcessBuilder teams = new ProcessBuilder(
                                "winget", "install", "Microsoft.Teams", 
                                "--accept-package-agreements", "--accept-source-agreements"
                            );
                            teams.start();
                            break;
                        case "Cisco Webex":
                            ProcessBuilder webex = new ProcessBuilder(
                                "winget", "install", "Cisco.Webex", 
                                "--accept-package-agreements", "--accept-source-agreements"
                            );
                            webex.start();
                            break;
                        case "Microsoft Edge":
                            ProcessBuilder edge = new ProcessBuilder(
                                "winget", "install", "Microsoft.Edge", 
                                "--accept-package-agreements", "--accept-source-agreements"
                            );
                            edge.start();
                            break;
                        default:
                            // Check if the entry key ends with .msi
                            if (entry.getKey().toLowerCase().endsWith(".msi")) {
                                String msiPath = new File(files[0].getParent(), entry.getKey()).getAbsolutePath();
                                ProcessBuilder msiInstaller = new ProcessBuilder(
                                    "powershell.exe", "-Command",
                                    "Start-Process msiexec.exe -ArgumentList '/i \"" + msiPath + "\" /qn' -Wait -NoNewWindow"
                                );
                                msiInstaller.start();
                            }
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isAdmin() {
        // Choose a protected system directory
        String systemPath = System.getenv("SystemRoot") + File.separator + "System32";
        Path testPath = Paths.get(systemPath, "test_write_access.tmp");

        try {
            // Try to create a temporary file in the system directory
            Files.createFile(testPath);
            // If successful, delete the file and return true
            Files.delete(testPath);
            return true;
        } catch (IOException e) {
            // If an exception is thrown, the program doesn't have elevated rights
            return false;
        }
    }
    public static void main(String[] args) {
        if (isAdmin()) {
            AutoInstall autoInstall = new AutoInstall();
            autoInstall.gui();
        } else {
            JOptionPane.showOptionDialog(
                null, 
                "Please run this program as an administrator.", 
                "Error",
                JOptionPane.DEFAULT_OPTION, 
                JOptionPane.ERROR_MESSAGE,
                null,
                new Object[] {"Exit"},
                "Exit");
        }
        
        
    }
}