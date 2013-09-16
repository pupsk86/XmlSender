/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mypack;

import com.alee.laf.WebLookAndFeel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.io.*;
import java.util.Locale;
import java.util.Properties;
import javax.swing.JFormattedTextField;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

/**
 *
 * @author gridasov
 */
public class XmlSenderGui extends javax.swing.JFrame {

    /**
     * Creates new form XmlSenderGui
     */
    
    private String VERSION() {
        Properties prop = new Properties();
     	try {
            prop.load( getClass().getResourceAsStream("config.properties") );
    	} catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        return prop.getProperty("VERSION");
    }
    
    private final String   WAIT_NOTIFY = "Send XML. Please wait...";
    private final Settings SETTINGS = new Settings();
    private final Color    GREEN = new java.awt.Color(0,100,0);
    private final Color    RED = new java.awt.Color(180,0,0);
    private final Color    GRAY = new java.awt.Color(224,224,224);
    private final Color    WHITE = new java.awt.Color(255,255,255);
    
    private Thread thSender = null;
    private Thread thNotify = null;

    public XmlSenderGui() {
        Locale.setDefault(Locale.ENGLISH);
        initComponents();
        
        //Startup settings
        hostField.setText(SETTINGS.params.host);
        fileChooser.setCurrentDirectory(new java.io.File(SETTINGS.params.root));
        highlightMenuItem.setSelected(SETTINGS.params.isHighlightEnabled);
        linenumbersMenuItem.setSelected(SETTINGS.params.isLineNumbersEnabled);
        setBounds(SETTINGS.params.getFrameBounds());
        splitPane.setDividerLocation(SETTINGS.params.dividerLocation);
        splitPane.setLastDividerLocation(SETTINGS.params.lastDividerLocation);


        SyntaxScheme mySyntaxScheme = new SyntaxScheme(true);
        
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_ATTRIBUTE, new Style(GREEN));
        mySyntaxScheme.setStyle(TokenTypes.OPERATOR, new Style(GREEN));
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE, new Style(RED));
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_DELIMITER, new Style(Style.DEFAULT_FOREGROUND,Style.DEFAULT_BACKGROUND, rsTextArea.getFont().deriveFont(java.awt.Font.BOLD)));
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_NAME, new Style(Style.DEFAULT_FOREGROUND,Style.DEFAULT_BACKGROUND, rsTextArea.getFont().deriveFont(java.awt.Font.BOLD)));
        
        rqTextArea.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_XML );
        rqTextArea.setCurrentLineHighlightColor(GRAY);
        rqTextArea.setSyntaxScheme(mySyntaxScheme);
        
        rsTextArea.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_XML );
        rsTextArea.setCurrentLineHighlightColor(WHITE);
        rsTextArea.setSyntaxScheme(mySyntaxScheme);

        rsScrollPane.setLineNumbersEnabled(linenumbersMenuItem.isSelected());
        rqScrollPane.setLineNumbersEnabled(linenumbersMenuItem.isSelected());
        
        rqTextArea.setSyntaxEditingStyle(highlightMenuItem.isSelected() ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_NONE);
        rsTextArea.setSyntaxEditingStyle(highlightMenuItem.isSelected() ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_NONE);
        
        NumberEditor numEditor = null;
        for (Component component : gotoSpinner.getComponents()){
            if(component instanceof NumberEditor){
                numEditor = (NumberEditor)component;
                gotoField = (numEditor.getComponent(0) instanceof JFormattedTextField) ? (JFormattedTextField)numEditor.getComponent(0) : null;
            }
        }

        gotoSpinner.setValue(1);
        
        gotoField.addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyReleased(java.awt.event.KeyEvent evt) {
            char a = evt.getKeyChar();
            if((int)a == 6 && evt.isControlDown()){
                if(rqTextArea.getSelectedText() != null)
                    searchField.setText(rqTextArea.getSelectedText());
                gotoPanel.setVisible(false);
                searchPanel.setVisible(true);
                searchField.selectAll();
                searchField.requestFocus();
            }
            if ((int)a == 10){
                try {
                    gotoSpinner.commitEdit();
                    gotoButtonActionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED,"GOTO"));
                } catch (Exception ex){
                    
                }
            }
            if((int)a == 27){
                gotoPanel.setVisible(false);
            }
        }    
        public void keyTyped(java.awt.event.KeyEvent evt) {
            char a = evt.getKeyChar();
            if (!Character.isDigit(a) )
                evt.consume();
            }
        });
        
        searchField.getDocument().addDocumentListener(new DocumentListener(){
            
            public void insertUpdate(DocumentEvent e) {
                if (searchPanel.isVisible())
                    searchActionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, "FindNextTyped"));
            }

            public void removeUpdate(DocumentEvent e) {
                if (searchPanel.isVisible())
                    searchActionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, "FindPrevTyped"));
            }

            public void changedUpdate(DocumentEvent e) {
                if (searchPanel.isVisible())
                    searchActionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, "FindNextTyped"));
            }
            
        });
        
        searchPanel.setVisible(false);
        gotoPanel.setVisible(false);
        
        gotoButton.setRolloverDecoratedOnly(true);
        gotoButton.setMoveIconOnPress ( false );
        searchPrevButton.setRolloverDecoratedOnly(true);
        searchPrevButton.setMoveIconOnPress ( false );
        searchNextButton.setRolloverDecoratedOnly(true);
        searchNextButton.setMoveIconOnPress ( false );

        gotoCloseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));;
        gotoCloseButton.setUndecorated(true);
        searchCloseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));;
        searchCloseButton.setUndecorated(true);
        
        //undoMenuItem.setAction(rqTextArea.getAction(RTextArea.REDO_ACTION));
        //undoMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-undo.png")));

        for(Component comp  : rqTextArea.getPopupMenu().getComponents()){
            if (comp instanceof JMenuItem) {
                JMenuItem itemPopMenu = (JMenuItem)comp;
                switch (itemPopMenu.getActionCommand()){
                    case "Undo":
                        itemPopMenu.setIcon(undoMenuItem.getIcon());
                        break;
                    case "Redo":
                        itemPopMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-redo.png")));
                        break;
                    case "Cut":
                        itemPopMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-cut.png")));
                        break;
                    case "Copy":
                        itemPopMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-copy.png")));
                        break;
                    case "Paste":
                        itemPopMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-paste.png")));
                        break;
                }

                //System.out.println(itemPopMenu.getText() + " " + itemPopMenu.getActionCommand() + " " + itemPopMenu.getUIClassID() + " " + itemPopMenu.getLocale());
                 
            }
        }

        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        sendButton = new javax.swing.JButton();
        statusTextField = new javax.swing.JTextField();
        cancelButton = new javax.swing.JButton();
        splitPane = new javax.swing.JSplitPane();
        rsScrollPane = new org.fife.ui.rtextarea.RTextScrollPane();
        rsTextArea = new org.fife.ui.rsyntaxtextarea.RSyntaxTextArea();
        rqScrollPane = new org.fife.ui.rtextarea.RTextScrollPane();
        rqTextArea = new org.fife.ui.rsyntaxtextarea.RSyntaxTextArea();
        searchPanel = new javax.swing.JPanel();
        searchCloseButton = new com.alee.laf.button.WebButton();
        searchNextButton = new com.alee.laf.button.WebButton();
        searchPrevButton = new com.alee.laf.button.WebButton();
        searchField = new com.alee.laf.text.WebTextField();
        gotoPanel = new javax.swing.JPanel();
        gotoSpinner = new javax.swing.JSpinner();
        gotoCloseButton = new com.alee.laf.button.WebButton();
        gotoButton = new com.alee.laf.button.WebButton();
        hostField = new com.alee.laf.text.WebTextField();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        undoMenuItem = new javax.swing.JMenuItem();
        redoMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        cutMenuItem = new javax.swing.JMenuItem();
        copyMenuItem = new javax.swing.JMenuItem();
        pasteMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        findMenuItem = new javax.swing.JMenuItem();
        gotoMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        highlightMenuItem = new javax.swing.JCheckBoxMenuItem();
        linenumbersMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        fileChooser.setCurrentDirectory(new java.io.File("/"));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("XML SENDER");
        setBounds(new java.awt.Rectangle(300, 300, 0, 0));
        setMinimumSize(new java.awt.Dimension(600, 450));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        sendButton.setFont(sendButton.getFont().deriveFont(sendButton.getFont().getStyle() & ~java.awt.Font.BOLD));
        sendButton.setText("Send");
        sendButton.setFocusPainted(false);
        sendButton.setFocusable(false);
        sendButton.setPreferredSize(new java.awt.Dimension(60, 29));
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        statusTextField.setEditable(false);
        statusTextField.setFont(statusTextField.getFont().deriveFont(statusTextField.getFont().getStyle() & ~java.awt.Font.BOLD));
        statusTextField.setToolTipText("Result line");
        statusTextField.setPreferredSize(new java.awt.Dimension(6, 28));

        cancelButton.setFont(cancelButton.getFont().deriveFont(cancelButton.getFont().getStyle() & ~java.awt.Font.BOLD));
        cancelButton.setText("Cancel");
        cancelButton.setEnabled(false);
        cancelButton.setFocusPainted(false);
        cancelButton.setFocusable(false);
        cancelButton.setPreferredSize(new java.awt.Dimension(80, 29));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        splitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        splitPane.setDividerLocation(200);
        splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);

        rsScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rsScrollPane.setLineNumbersEnabled(true);

        rsTextArea.setColumns(20);
        rsTextArea.setEditable(false);
        rsTextArea.setRows(5);
        rsTextArea.setToolTipText("");
        rsScrollPane.setViewportView(rsTextArea);

        splitPane.setBottomComponent(rsScrollPane);

        rqScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rqScrollPane.setLineNumbersEnabled(true);

        rqTextArea.setColumns(20);
        rqTextArea.setRows(5);
        rqTextArea.setToolTipText("");
        rqTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                rqTextAreaKeyPressed(evt);
            }
        });
        rqScrollPane.setViewportView(rqTextArea);

        splitPane.setLeftComponent(rqScrollPane);

        searchPanel.setMaximumSize(new java.awt.Dimension(100, 50));
        searchPanel.setMinimumSize(new java.awt.Dimension(100, 50));
        searchPanel.setPreferredSize(new java.awt.Dimension(100, 50));

        searchCloseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/window-close.png"))); // NOI18N
        searchCloseButton.setFocusable(false);
        searchCloseButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/list-remove-user.png"))); // NOI18N
        searchCloseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchCloseButtonActionPerformed(evt);
            }
        });

        searchNextButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/forward.png"))); // NOI18N
        searchNextButton.setActionCommand("FindNext");
        searchNextButton.setFocusable(false);
        searchNextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchActionPerformed(evt);
            }
        });

        searchPrevButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/backward.png"))); // NOI18N
        searchPrevButton.setActionCommand("FindPrev");
        searchPrevButton.setFocusable(false);
        searchPrevButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchActionPerformed(evt);
            }
        });

        searchField.setHideInputPromptOnFocus(false);
        searchField.setInputPrompt("Search");
        searchField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchFieldActionPerformed(evt);
            }
        });
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                searchFieldKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout searchPanelLayout = new javax.swing.GroupLayout(searchPanel);
        searchPanel.setLayout(searchPanelLayout);
        searchPanelLayout.setHorizontalGroup(
            searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, searchPanelLayout.createSequentialGroup()
                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchNextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchPrevButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 321, Short.MAX_VALUE)
                .addComponent(searchCloseButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        searchPanelLayout.setVerticalGroup(
            searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchPanelLayout.createSequentialGroup()
                .addGroup(searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(searchNextButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchPrevButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                    .addComponent(searchField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchCloseButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        gotoPanel.setMaximumSize(new java.awt.Dimension(100, 50));
        gotoPanel.setMinimumSize(new java.awt.Dimension(100, 50));
        gotoPanel.setPreferredSize(new java.awt.Dimension(100, 50));

        gotoCloseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/window-close.png"))); // NOI18N
        gotoCloseButton.setFocusable(false);
        gotoCloseButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/list-remove-user.png"))); // NOI18N
        gotoCloseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gotoCloseButtonActionPerformed(evt);
            }
        });

        gotoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/forward.png"))); // NOI18N
        gotoButton.setFocusable(false);
        gotoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gotoButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout gotoPanelLayout = new javax.swing.GroupLayout(gotoPanel);
        gotoPanel.setLayout(gotoPanelLayout);
        gotoPanelLayout.setHorizontalGroup(
            gotoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, gotoPanelLayout.createSequentialGroup()
                .addComponent(gotoSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gotoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(gotoCloseButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        gotoPanelLayout.setVerticalGroup(
            gotoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(gotoPanelLayout.createSequentialGroup()
                .addGroup(gotoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(gotoButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(gotoSpinner)
                    .addComponent(gotoCloseButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        hostField.setToolTipText("Host");
        hostField.setHideInputPromptOnFocus(false);
        hostField.setInputPrompt("Host");
        hostField.setPreferredSize(new java.awt.Dimension(11, 28));

        fileMenu.setText("File");

        openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/document-open.png"))); // NOI18N
        openMenuItem.setText("Open...");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/document-save-as.png"))); // NOI18N
        saveMenuItem.setText("Save...");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);
        fileMenu.add(jSeparator1);

        exitMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/system-shutdown-restart-panel.png"))); // NOI18N
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText("Edit");

        undoMenuItem.setAction(rqTextArea.getAction(RTextArea.UNDO_ACTION));
        undoMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-undo.png"))); // NOI18N
        editMenu.add(undoMenuItem);

        redoMenuItem.setAction(rqTextArea.getAction(RTextArea.REDO_ACTION));
        redoMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        redoMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-redo.png"))); // NOI18N
        redoMenuItem.setText("Redo");
        editMenu.add(redoMenuItem);
        editMenu.add(jSeparator2);

        cutMenuItem.setAction(rqTextArea.getAction(RTextArea.CUT_ACTION));
        cutMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        cutMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-cut.png"))); // NOI18N
        editMenu.add(cutMenuItem);

        copyMenuItem.setAction(rqTextArea.getAction(RTextArea.COPY_ACTION));
        copyMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-copy.png"))); // NOI18N
        copyMenuItem.setText("Copy");
        editMenu.add(copyMenuItem);

        pasteMenuItem.setAction(rqTextArea.getAction(RTextArea.PASTE_ACTION));
        pasteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        pasteMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-paste.png"))); // NOI18N
        pasteMenuItem.setText("Paste");
        editMenu.add(pasteMenuItem);
        editMenu.add(jSeparator3);

        findMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        findMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-find-user.png"))); // NOI18N
        findMenuItem.setText("Find");
        findMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(findMenuItem);

        gotoMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        gotoMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/document-revert.png"))); // NOI18N
        gotoMenuItem.setText("Goto");
        gotoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gotoMenuItemActionPerformed(evt);
            }
        });
        editMenu.add(gotoMenuItem);

        menuBar.add(editMenu);

        viewMenu.setText("View");

        highlightMenuItem.setSelected(true);
        highlightMenuItem.setText("Highlight");
        highlightMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                highlightMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(highlightMenuItem);

        linenumbersMenuItem.setSelected(true);
        linenumbersMenuItem.setText("Line numbers");
        linenumbersMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linenumbersMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(linenumbersMenuItem);

        menuBar.add(viewMenu);

        helpMenu.setText("Help");

        aboutMenuItem.setText("About...");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);
        helpMenu.getAccessibleContext().setAccessibleName("About");

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(searchPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 671, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sendButton, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statusTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(gotoPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 671, Short.MAX_VALUE)
                    .addComponent(hostField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addComponent(splitPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(splitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gotoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hostField, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(statusTextField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(sendButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButtonActionPerformed
        statusTextField.setText(WAIT_NOTIFY);
        sendButton.setEnabled(false);
        cancelButton.setEnabled(true);
        rsTextArea.setText("");
        try {
            final HTTPRequest httprequest = new HTTPRequest(rqTextArea.getText(),hostField.getText());
            thSender = new Thread(httprequest);
            thSender.start();
            
            thNotify = new Thread(new Runnable(){
                @Override
                public void run(){
                    int seconds = 0;
                    try {
                        while(!Thread.interrupted() && thSender.isAlive()) {
                            thSender.join(1000);
                            seconds += 1;
                            statusTextField.setText(WAIT_NOTIFY + seconds + "s");
                        }
                    } catch (Exception e) {
                    }
                    
                    if (thSender.isAlive()){
                        statusTextField.setText("Canceled");
                    } else {
                        statusTextField.setText(httprequest.getResult());
                        rsTextArea.setText(httprequest.getResultEntity());
                        System.out.println(httprequest.getResultEntity());    
                    }
                    
                    sendButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                }
            });
            thNotify.start();
        }catch (Exception e) {
            statusTextField.setText(e.getMessage());
            sendButton.setEnabled(true);
            cancelButton.setEnabled(false);
        }
       
    }//GEN-LAST:event_sendButtonActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        SETTINGS.params.host = hostField.getText();
        SETTINGS.params.root = fileChooser.getCurrentDirectory().getAbsolutePath();
        SETTINGS.params.isHighlightEnabled = highlightMenuItem.isSelected();
        SETTINGS.params.isLineNumbersEnabled = linenumbersMenuItem.isSelected();
        SETTINGS.params.setFrameBounds(getBounds());
        SETTINGS.params.dividerLocation = splitPane.getDividerLocation();
        SETTINGS.params.lastDividerLocation = splitPane.getLastDividerLocation();
        SETTINGS.exportXml();
    }//GEN-LAST:event_formWindowClosing

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        if (thNotify != null && thNotify.isAlive()) {
            thNotify.interrupt();
            cancelButton.setEnabled(false);
        }
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
        int returnVal = fileChooser.showOpenDialog(XmlSenderGui.this);
        if (returnVal == fileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                FileInputStream in = new FileInputStream(file);
                FileReader reader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line = null;
                StringBuffer lines = new StringBuffer();
                while ( (line = bufferedReader.readLine()) != null) {
                    lines.append(line + "\n");
                }
                
                rqTextArea.setText(lines.toString());
                rqTextArea.setCaretPosition(0);
        	in.close();
            }
            catch (Exception e) {
                System.out.println(e.getMessage());                                    
            }
        }
    }//GEN-LAST:event_openMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        int returnVal = fileChooser.showSaveDialog(XmlSenderGui.this);
        if (returnVal == fileChooser.APPROVE_OPTION) {
            try {
                File save_file = fileChooser.getSelectedFile();
                FileWriter writer = new FileWriter(save_file);
                BufferedWriter buffered = new BufferedWriter(writer);
                buffered.write(rqTextArea.getText());
                buffered.flush();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
       formWindowClosing(new java.awt.event.WindowEvent(this, java.awt.event.WindowEvent.WINDOW_CLOSING));
       System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        JOptionPane.showMessageDialog(null, "XmlSender® \nversion: " + VERSION());
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void linenumbersMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linenumbersMenuItemActionPerformed
        rsScrollPane.setLineNumbersEnabled(linenumbersMenuItem.isSelected());
        rqScrollPane.setLineNumbersEnabled(linenumbersMenuItem.isSelected());
    }//GEN-LAST:event_linenumbersMenuItemActionPerformed

    private void highlightMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_highlightMenuItemActionPerformed
        rqTextArea.setSyntaxEditingStyle(highlightMenuItem.isSelected() ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_NONE);
        rsTextArea.setSyntaxEditingStyle(highlightMenuItem.isSelected() ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_NONE);
    }//GEN-LAST:event_highlightMenuItemActionPerformed

    private void rqTextAreaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_rqTextAreaKeyPressed
        if(evt.getKeyCode() == 114){
             searchActionPerformed(
                     new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, evt.isShiftDown()?"FindPrev":"FindNext"));
        }
        if(evt.getKeyCode() == 27){
             searchPanel.setVisible(false);
             gotoPanel.setVisible(false);
        }
        
    }//GEN-LAST:event_rqTextAreaKeyPressed

    private void gotoCloseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gotoCloseButtonActionPerformed
        gotoPanel.setVisible(false);
    }//GEN-LAST:event_gotoCloseButtonActionPerformed

    private void searchCloseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchCloseButtonActionPerformed
        searchPanel.setVisible(false);
    }//GEN-LAST:event_searchCloseButtonActionPerformed

    private void gotoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gotoButtonActionPerformed
         try {
            rqTextArea.setCaretPosition(
                    rqTextArea.getDocument().getDefaultRootElement().getElement((int) gotoSpinner.getValue() - 1).getStartOffset());
        } catch (Exception ex) {
        }
        gotoPanel.setVisible(false);
    }//GEN-LAST:event_gotoButtonActionPerformed

    private void searchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchActionPerformed
         // "FindNext" => search forward, "FindPrev" => search backward
        String command = evt.getActionCommand();
        boolean forward = "FindNext".equals(command) || "FindNextTyped".equals(command);
      
        SearchContext context = new SearchContext();
        String text = searchField.getText();
        if (text.length() == 0) {
           return;
        }
        context.setSearchFor(text);
        context.setMatchCase(false);
        context.setRegularExpression(false);
        context.setSearchForward(forward);
        context.setWholeWord(false);
        context.setSearchSelectionOnly(false);

        if (rqTextArea.getSelectedText() != null && command.contains("Typed")){
            int maxCaretPosition = rqTextArea.getDocument().getLength();
            int caretPosition = rqTextArea.getCaretPosition() - (forward ? rqTextArea.getSelectedText().length() : 0);
            if (caretPosition >= 0 && caretPosition <= maxCaretPosition)
                rqTextArea.setCaretPosition(caretPosition);
        }
        
        boolean found = SearchEngine.find(rqTextArea, context);
        if (!found) {
            rqTextArea.setCaretPosition(0);
            context.setSearchForward(true);
            found = SearchEngine.find(rqTextArea, context);
        }
        if (!found) {
           searchField.setForeground(RED);
        } else {
           searchField.setForeground(GREEN);
        }
    }//GEN-LAST:event_searchActionPerformed

    private void searchFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchFieldActionPerformed
        searchActionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, "FindNext"));
    }//GEN-LAST:event_searchFieldActionPerformed

    private void searchFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchFieldKeyPressed
        if(evt.getKeyCode() == 27){
             searchPanel.setVisible(false);
        }
        if(evt.getKeyCode() == 114){
             searchActionPerformed(
                     new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, evt.isShiftDown()?"FindPrev":"FindNext"));
        }
        if(evt.getKeyCode() == 71 && evt.isControlDown()){
            gotoPanel.setVisible(true);
            searchPanel.setVisible(false);
            gotoField.requestFocus();
            gotoField.setText(gotoField.getText());
            gotoField.selectAll();
        }
    }//GEN-LAST:event_searchFieldKeyPressed

    private void findMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findMenuItemActionPerformed
        searchField.setText(rqTextArea.getSelectedText());
        gotoPanel.setVisible(false);
        searchPanel.setVisible(true);
        searchField.selectAll();
        searchField.requestFocus();
    }//GEN-LAST:event_findMenuItemActionPerformed

    private void gotoMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gotoMenuItemActionPerformed
        gotoPanel.setVisible(true);
        searchPanel.setVisible(false);
        gotoField.requestFocus();
        gotoField.setText(gotoField.getText());
        gotoField.selectAll();
    }//GEN-LAST:event_gotoMenuItemActionPerformed
 


    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        WebLookAndFeel.install ();
        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new XmlSenderGui().setVisible(true);
            }
        });
        
    }

    private javax.swing.JFormattedTextField gotoField;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton cancelButton;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JMenuItem cutMenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem findMenuItem;
    private com.alee.laf.button.WebButton gotoButton;
    private com.alee.laf.button.WebButton gotoCloseButton;
    private javax.swing.JMenuItem gotoMenuItem;
    private javax.swing.JPanel gotoPanel;
    private javax.swing.JSpinner gotoSpinner;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JCheckBoxMenuItem highlightMenuItem;
    private com.alee.laf.text.WebTextField hostField;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JCheckBoxMenuItem linenumbersMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem pasteMenuItem;
    private javax.swing.JMenuItem redoMenuItem;
    private org.fife.ui.rtextarea.RTextScrollPane rqScrollPane;
    private org.fife.ui.rsyntaxtextarea.RSyntaxTextArea rqTextArea;
    private org.fife.ui.rtextarea.RTextScrollPane rsScrollPane;
    private org.fife.ui.rsyntaxtextarea.RSyntaxTextArea rsTextArea;
    private javax.swing.JMenuItem saveMenuItem;
    private com.alee.laf.button.WebButton searchCloseButton;
    private com.alee.laf.text.WebTextField searchField;
    private com.alee.laf.button.WebButton searchNextButton;
    private javax.swing.JPanel searchPanel;
    private com.alee.laf.button.WebButton searchPrevButton;
    private javax.swing.JButton sendButton;
    private javax.swing.JSplitPane splitPane;
    private javax.swing.JTextField statusTextField;
    private javax.swing.JMenuItem undoMenuItem;
    private javax.swing.JMenu viewMenu;
    // End of variables declaration//GEN-END:variables
}
