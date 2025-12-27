package gui.dialogs;

import gui.theme.*;
import gui.components.ComponentFactory; 
import gui.components.StyledButton; 
import gui.components.StyledTextField; 
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList; 
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;      


public class FileExplorerDialog extends BaseDialog { 

    private final StyledTextField searchField;
    private final JComboBox<String> sortBox;
    private final DefaultListModel<File> listModel;
    private final JList<File> fileList;
    private File currentDirectory;
    private final JLabel pathLabel;

    private File selectedFileResult = null; 

    public FileExplorerDialog(Frame owner, File startDir) {
        super((JFrame) owner, "Explorateur de fichiers", 700, 480); 

        currentDirectory = startDir != null ? startDir : new File(System.getProperty("user.home"));

        searchField = StyledTextField.forInput(20); 
        sortBox = new JComboBox<>(new String[]{"Nom", "Taille", "Date"});
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        pathLabel = new JLabel(currentDirectory.getAbsolutePath());

        initContent();
        initFooter();
        // NOUVEVEAU: Assurer que les panneaux sont bien validés et repeints
        SwingUtilities.invokeLater(() -> {
            rootPanel.revalidate();
            rootPanel.repaint();
        });
    }
    
    @Override
    protected void initContent() {
        contentPanel.setLayout(new BorderLayout(12, 12));
        contentPanel.setBorder(new EmptyBorder(0, 0, 0, 0)); 

        JPanel controls = new JPanel(new BorderLayout(8, 8));
        controls.setOpaque(false);

        JPanel leftControls = new JPanel(new BorderLayout(6, 6));
        leftControls.setOpaque(false);
        pathLabel.setFont(Theme.fontMono(Font.PLAIN, 11));
        pathLabel.setForeground(Theme.TEXTE);

        StyledButton upBtn = new StyledButton("↑", Theme.BTN_NEUTRE, Color.WHITE); 
        upBtn.setToolTipText("Remonter d'un dossier");
        upBtn.setPreferredSize(new Dimension(30, 30));
        upBtn.setRadius(8); 
        upBtn.addActionListener(e -> {
            File parent = currentDirectory.getParentFile();
            if (parent != null && parent.exists() && parent.canRead()) { 
                currentDirectory = parent;
                refreshFileList();
            } else if (parent == null) { 
            } else {
                DialogFactory.showError((JFrame) getOwner(), "Erreur d'accès", "Impossible d'accéder au dossier parent."); 
            }
        });

        leftControls.add(upBtn, BorderLayout.WEST);
        leftControls.add(pathLabel, BorderLayout.CENTER);

        searchField.setToolTipText("Rechercher un fichier...");
        searchField.setHorizontalAlignment(JTextField.LEFT); 
        searchField.setRadius(10); 
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                refreshFileList();
            }
        });

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightControls.setOpaque(false);

        sortBox.setToolTipText("Trier");
        sortBox.addActionListener(e -> refreshFileList());

        StyledButton newFolderBtn = new StyledButton("Nouveau dossier", Theme.BTN_NEUTRE, Color.WHITE); 
        newFolderBtn.setToolTipText("Créer un nouveau dossier dans le répertoire courant");
        newFolderBtn.addActionListener(e -> createNewFolder());

        rightControls.add(sortBox);
        rightControls.add(newFolderBtn);

        contentPanel.add(controls, BorderLayout.NORTH);
        
        JPanel searchAndSortPanel = new JPanel(new BorderLayout(8,8));
        searchAndSortPanel.setOpaque(false);
        searchAndSortPanel.add(searchField, BorderLayout.CENTER);
        searchAndSortPanel.add(rightControls, BorderLayout.EAST);

        controls.add(leftControls, BorderLayout.WEST);
        controls.add(searchAndSortPanel, BorderLayout.CENTER); 

        fileList.setCellRenderer(new FileCellRenderer());
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setFixedCellHeight(28);
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { 
                    File sel = fileList.getSelectedValue();
                    if (sel != null && sel.isDirectory()) { 
                        currentDirectory = sel;
                        refreshFileList();
                    } else if (sel != null && sel.isFile()) { 
                        selectedFileResult = sel;
                        dispose();
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(new RoundedBorder(Theme.BORDURE, 1, Theme.RAYON_ARRONDI, Theme.INSETS_NORMAL));
        contentPanel.add(scrollPane, BorderLayout.CENTER); 

        refreshFileList();
    }

    @Override
    protected void initFooter() {
        addButton("Ouvrir", Theme.BTN_PRIMAIRE, () -> {
            File sel = fileList.getSelectedValue();
            if (sel != null && sel.isFile()) { 
                selectedFileResult = sel;
                dispose();
            } else if (sel != null && sel.isDirectory()) {
                DialogFactory.showError((JFrame) getOwner(), "Sélection Invalide", "Veuillez sélectionner un fichier, pas un dossier."); 
            } else {
                DialogFactory.showError((JFrame) getOwner(), "Sélection Invalide", "Veuillez sélectionner un fichier."); 
            }
        });

        addButton("Annuler", Theme.BTN_NEUTRE, this::dispose);
    }

    private void refreshFileList() {
        listModel.clear();
        File[] files = currentDirectory.listFiles();
        
        pathLabel.setText(currentDirectory.getAbsolutePath()); 
        
        if (files == null) {
            DialogFactory.showError((JFrame) getOwner(), "Erreur d'accès", "Impossible de lire le contenu du dossier: " + currentDirectory.getAbsolutePath()); 
            return;
        }

        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";

        List<File> directories = new ArrayList<>();
        List<File> regularFiles = new ArrayList<>();

        for (File f : files) {
            if (!f.isHidden() && f.canRead() && (query.isEmpty() || f.getName().toLowerCase().contains(query))) {
                if (f.isDirectory()) {
                    directories.add(f);
                } else {
                    regularFiles.add(f);
                }
            }
        }

        Comparator<File> fileComparator;
        String sort = (String) sortBox.getSelectedItem();
        if ("Taille".equals(sort)) {
            fileComparator = Comparator.comparingLong(File::length);
        } else if ("Date".equals(sort)) {
            fileComparator = Comparator.comparingLong(File::lastModified);
        } else { 
            fileComparator = Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER);
        }

        directories.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER)); 
        regularFiles.sort(fileComparator);

        directories.forEach(listModel::addElement);
        regularFiles.forEach(listModel::addElement);
    }

    private void createNewFolder() {
        String name = JOptionPane.showInputDialog(this, "Nom du nouveau dossier :", "Nouveau dossier", JOptionPane.PLAIN_MESSAGE);
        if (name != null) {
            name = name.trim();
            if (!name.isEmpty()) {
                File newDir = new File(currentDirectory, name);
                if (newDir.exists()) {
                    DialogFactory.showInfo((JFrame) getOwner(), "Dossier existant", "Le dossier existe déjà."); 
                } else {
                    boolean ok = newDir.mkdir();
                    if (!ok) {
                        DialogFactory.showError((JFrame) getOwner(), "Erreur de création", "Impossible de créer le dossier."); 
                    } else {
                        refreshFileList();
                    }
                }
            }
        }
    }

    private static class FileCellRenderer extends DefaultListCellRenderer {
        private final FileSystemView fsv = FileSystemView.getFileSystemView();
        private final Font mono = Theme.fontMono(Font.PLAIN, 12);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof File) {
                File file = (File) value;
                Icon icon = fsv.getSystemIcon(file);
                label.setIcon(icon);
                label.setText(file.getName());
                label.setFont(mono);
                label.setForeground(Theme.TEXTE);
                label.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                
                if (file.isDirectory()) {
                    label.setText(file.getName() + File.separator); 
                } else {
                }
            }
            return label;
        }
    }

    public File getSelectedFile() {
        return selectedFileResult;
    }
}
