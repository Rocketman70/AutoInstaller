package src.main.java;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.*;
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
                    for (File file : files) {
                        JCheckBox checkBox = new JCheckBox(file.getName());
                        checklist.put(file.getName(), false);
                        checkBox.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                checkBox.setSelected(checkBox.isSelected());
                                if (checkBox.isSelected()) {
                                    for (Entry<String, Boolean> entry : checklist.entrySet()) {
                                        if (entry.getKey().equals(checkBox.getText())) {
                                            entry.setValue(true);
                                        }
                                    }
                                } else {
                                    for (Entry<String, Boolean> entry : checklist.entrySet()) {
                                        if (entry.getKey().equals(checkBox.getText())) {
                                            entry.setValue(false);
                                        }
                                    }
                                }
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
                executeCommands();
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
            // Install WinGet (unchanged)
            ProcessBuilder windowsCom = new ProcessBuilder(
                "powershell.exe", "-Command", 
                "Add-AppxPackage -RegisterByFamilyName -MainPackage Microsoft.DesktopAppInstaller_8wekyb3d8bbwe");
            windowsCom.start();
    
            for (Entry<String, Boolean> entry : checklist.entrySet()) {
                if (entry.getValue()) {
                    switch (entry.getKey()) {
                        case "Microsoft Teams":
                            ProcessBuilder teams = new ProcessBuilder(
                                "winget", "install", "Microsoft.Teams");
                            teams.start();
                            break;
                        case "Cisco Webex":
                            ProcessBuilder webex = new ProcessBuilder(
                                "winget", "install", "Cisco.Webex");
                            webex.start();
                            break;
                        case "Microsoft Edge":
                            ProcessBuilder edge = new ProcessBuilder(
                                "winget", "install", "Microsoft.Edge");
                            edge.start();
                            break;
                        default:
                            // Check if the entry key ends with .msi
                            if (entry.getKey().toLowerCase().endsWith(".msi")) {
                                String msiPath = new File(files[0].getParent(), entry.getKey()).getAbsolutePath();
                                ProcessBuilder msiInstaller = new ProcessBuilder(
                                    "powershell.exe", "-Command",
                                    "Start-Process msiexec.exe -ArgumentList '/i \"" + msiPath + "\" /qn' -Wait -NoNewWindow");
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
    public static void main(String[] args) {
        //Create instance of GUI.
        AutoInstall autoInstall = new AutoInstall();
        autoInstall.gui();
        
    }
}