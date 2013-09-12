/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mypack;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
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
      
        initComponents();
        
        //Startup settings
        hostTextField.setText(SETTINGS.params.host);
        fileChooser.setCurrentDirectory(new java.io.File(SETTINGS.params.root));
        highlightMenuItem.setSelected(SETTINGS.params.isHighlightEnabled);
        linenumbersMenuItem.setSelected(SETTINGS.params.isLineNumbersEnabled);
        setBounds(SETTINGS.params.getFrameBounds());
        splitPane.setDividerLocation(SETTINGS.params.dividerLocation);

        Font myFont     = sendButton.getFont().deriveFont(~java.awt.Font.BOLD);
        Font myBoldFont = sendButton.getFont().deriveFont(java.awt.Font.BOLD);

        SyntaxScheme mySyntaxScheme = new SyntaxScheme(true);
        
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_ATTRIBUTE, new Style(GREEN, Style.DEFAULT_BACKGROUND, myFont));
        mySyntaxScheme.setStyle(TokenTypes.OPERATOR, new Style(GREEN, Style.DEFAULT_BACKGROUND, myFont));
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE, new Style(RED, Style.DEFAULT_BACKGROUND, myFont));
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_DELIMITER, new Style(Style.DEFAULT_FOREGROUND,Style.DEFAULT_BACKGROUND, myBoldFont));
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_NAME, new Style(Style.DEFAULT_FOREGROUND,Style.DEFAULT_BACKGROUND, myBoldFont));
        
        rqTextArea.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_XML );
        rqTextArea.setFont(myFont);
        rqTextArea.setCurrentLineHighlightColor(GRAY);
        rqTextArea.setSyntaxScheme(mySyntaxScheme);
        rqTextArea.setFont(hostLabel.getFont());
        
        rsTextArea.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_XML );
        rsTextArea.setFont(myFont);
        rsTextArea.setCurrentLineHighlightColor(WHITE);
        rsTextArea.setSyntaxScheme(mySyntaxScheme);
        rsTextArea.setFont(hostLabel.getFont());

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


        gotoField.addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyTyped(java.awt.event.KeyEvent evt) {

            char a = evt.getKeyChar();
            if((int)a == 27){
                gotoPanel.setVisible(false);
            }
            if((int)a == 6 && evt.isControlDown()){
                if(rqTextArea.getSelectedText() != null)
                    searchField.setText(rqTextArea.getSelectedText());
                gotoPanel.setVisible(false);
                searchPanel.setVisible(true);
                searchField.selectAll();
                searchField.requestFocus();
            }
            if ((int)a == 10)
                gotoButtonActionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, ""));
            if (!Character.isDigit(a) )
                evt.consume();
            }
        });
        
        gotoSpinner.setValue(1);
        
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
        
        /*gotoButton1.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {}

            @Override
            public void mousePressed(MouseEvent e) {
                gotoButton1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                gotoButton1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                gotoButton1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                gotoButton1.setBorder(null);
            }
        });*/
        
        
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
        hostTextField = new javax.swing.JTextField();
        sendButton = new javax.swing.JButton();
        hostLabel = new javax.swing.JLabel();
        statusTextField = new javax.swing.JTextField();
        cancelButton = new javax.swing.JButton();
        splitPane = new javax.swing.JSplitPane();
        rsScrollPane = new org.fife.ui.rtextarea.RTextScrollPane();
        rsTextArea = new org.fife.ui.rsyntaxtextarea.RSyntaxTextArea();
        rqScrollPane = new org.fife.ui.rtextarea.RTextScrollPane();
        rqTextArea = new org.fife.ui.rsyntaxtextarea.RSyntaxTextArea();
        searchPanel = new javax.swing.JPanel();
        searchNextButton = new javax.swing.JButton();
        searchPrevButton = new javax.swing.JButton();
        searchField = new javax.swing.JTextField();
        searchCloseButton = new javax.swing.JButton();
        gotoPanel = new javax.swing.JPanel();
        gotoCloseButton = new javax.swing.JButton();
        gotoSpinner = new javax.swing.JSpinner();
        gotoButton = new javax.swing.JButton();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        settingsMenu = new javax.swing.JMenu();
        highlightMenuItem = new javax.swing.JCheckBoxMenuItem();
        linenumbersMenuItem = new javax.swing.JCheckBoxMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        fileChooser.setCurrentDirectory(new java.io.File("/"));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("XML SENDER");
        setBounds(new java.awt.Rectangle(300, 300, 0, 0));
        setMinimumSize(new java.awt.Dimension(600, 450));
        setPreferredSize(new java.awt.Dimension(600, 450));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        hostTextField.setFont(hostTextField.getFont().deriveFont(hostTextField.getFont().getStyle() & ~java.awt.Font.BOLD));
        hostTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hostTextFieldActionPerformed(evt);
            }
        });

        sendButton.setFont(sendButton.getFont().deriveFont(sendButton.getFont().getStyle() & ~java.awt.Font.BOLD));
        sendButton.setText("Send");
        sendButton.setFocusPainted(false);
        sendButton.setPreferredSize(new java.awt.Dimension(60, 29));
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        hostLabel.setFont(hostLabel.getFont().deriveFont(hostLabel.getFont().getStyle() & ~java.awt.Font.BOLD));
        hostLabel.setText("Host:");

        statusTextField.setEditable(false);
        statusTextField.setFont(statusTextField.getFont().deriveFont(statusTextField.getFont().getStyle() & ~java.awt.Font.BOLD));
        statusTextField.setToolTipText("Response");

        cancelButton.setFont(cancelButton.getFont().deriveFont(cancelButton.getFont().getStyle() & ~java.awt.Font.BOLD));
        cancelButton.setText("Cancel");
        cancelButton.setEnabled(false);
        cancelButton.setFocusPainted(false);
        cancelButton.setPreferredSize(new java.awt.Dimension(80, 29));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        splitPane.setDividerLocation(200);
        splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPane.setContinuousLayout(true);

        rsScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rsScrollPane.setLineNumbersEnabled(true);

        rsTextArea.setColumns(20);
        rsTextArea.setEditable(false);
        rsTextArea.setRows(5);
        rsScrollPane.setViewportView(rsTextArea);

        splitPane.setBottomComponent(rsScrollPane);

        rqScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rqScrollPane.setLineNumbersEnabled(true);

        rqTextArea.setColumns(20);
        rqTextArea.setRows(5);
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

        searchNextButton.setFont(searchNextButton.getFont().deriveFont(searchNextButton.getFont().getStyle() & ~java.awt.Font.BOLD));
        searchNextButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/arrow_down.png"))); // NOI18N
        searchNextButton.setActionCommand("FindNext");
        searchNextButton.setBorderPainted(false);
        searchNextButton.setDefaultCapable(false);
        searchNextButton.setFocusPainted(false);
        searchNextButton.setMaximumSize(new java.awt.Dimension(70, 30));
        searchNextButton.setMinimumSize(new java.awt.Dimension(70, 30));
        searchNextButton.setPreferredSize(new java.awt.Dimension(70, 30));
        searchNextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchActionPerformed(evt);
            }
        });

        searchPrevButton.setFont(searchPrevButton.getFont().deriveFont(searchPrevButton.getFont().getStyle() & ~java.awt.Font.BOLD));
        searchPrevButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/arrow_up.png"))); // NOI18N
        searchPrevButton.setActionCommand("FindPrev");
        searchPrevButton.setBorderPainted(false);
        searchPrevButton.setDefaultCapable(false);
        searchPrevButton.setFocusPainted(false);
        searchPrevButton.setMaximumSize(new java.awt.Dimension(70, 30));
        searchPrevButton.setMinimumSize(new java.awt.Dimension(70, 30));
        searchPrevButton.setPreferredSize(new java.awt.Dimension(70, 30));
        searchPrevButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchActionPerformed(evt);
            }
        });

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

        searchCloseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/close-button.png"))); // NOI18N
        searchCloseButton.setBorderPainted(false);
        searchCloseButton.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        searchCloseButton.setDefaultCapable(false);
        searchCloseButton.setFocusPainted(false);
        searchCloseButton.setMaximumSize(new java.awt.Dimension(70, 30));
        searchCloseButton.setMinimumSize(new java.awt.Dimension(70, 30));
        searchCloseButton.setPreferredSize(new java.awt.Dimension(70, 30));
        searchCloseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchCloseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout searchPanelLayout = new javax.swing.GroupLayout(searchPanel);
        searchPanel.setLayout(searchPanelLayout);
        searchPanelLayout.setHorizontalGroup(
            searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, searchPanelLayout.createSequentialGroup()
                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchNextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchPrevButton, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(searchCloseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        searchPanelLayout.setVerticalGroup(
            searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchPanelLayout.createSequentialGroup()
                .addGroup(searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(searchNextButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addComponent(searchPrevButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                        .addComponent(searchField, javax.swing.GroupLayout.Alignment.LEADING))
                    .addComponent(searchCloseButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        gotoPanel.setMaximumSize(new java.awt.Dimension(100, 50));
        gotoPanel.setMinimumSize(new java.awt.Dimension(100, 50));
        gotoPanel.setPreferredSize(new java.awt.Dimension(100, 50));

        gotoCloseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/close-button.png"))); // NOI18N
        gotoCloseButton.setBorderPainted(false);
        gotoCloseButton.setDefaultCapable(false);
        gotoCloseButton.setFocusPainted(false);
        gotoCloseButton.setMaximumSize(new java.awt.Dimension(70, 30));
        gotoCloseButton.setMinimumSize(new java.awt.Dimension(70, 30));
        gotoCloseButton.setPreferredSize(new java.awt.Dimension(70, 30));
        gotoCloseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gotoCloseButtonActionPerformed(evt);
            }
        });

        gotoButton.setBackground(javax.swing.UIManager.getDefaults().getColor("Label.background"));
        gotoButton.setFont(gotoButton.getFont().deriveFont(gotoButton.getFont().getStyle() & ~java.awt.Font.BOLD));
        gotoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/arrow_right.png"))); // NOI18N
        gotoButton.setBorder(null);
        gotoButton.setBorderPainted(false);
        gotoButton.setDefaultCapable(false);
        gotoButton.setFocusPainted(false);
        gotoButton.setMaximumSize(new java.awt.Dimension(70, 30));
        gotoButton.setMinimumSize(new java.awt.Dimension(70, 30));
        gotoButton.setPreferredSize(new java.awt.Dimension(70, 30));
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
                .addComponent(gotoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(gotoCloseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        gotoPanelLayout.setVerticalGroup(
            gotoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(gotoPanelLayout.createSequentialGroup()
                .addGroup(gotoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(gotoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE, false)
                        .addComponent(gotoSpinner)
                        .addComponent(gotoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                    .addComponent(gotoCloseButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fileMenu.setText("File");

        openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenuItem.setText("Open...");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setText("Save...");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        settingsMenu.setText("Settings");

        highlightMenuItem.setSelected(true);
        highlightMenuItem.setText("Highlight");
        highlightMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                highlightMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(highlightMenuItem);

        linenumbersMenuItem.setSelected(true);
        linenumbersMenuItem.setText("Line numbers");
        linenumbersMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linenumbersMenuItemActionPerformed(evt);
            }
        });
        settingsMenu.add(linenumbersMenuItem);

        menuBar.add(settingsMenu);

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
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(searchPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sendButton, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(statusTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(hostLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(hostTextField))
                    .addComponent(splitPane, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(gotoPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(splitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 309, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gotoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(hostTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hostLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sendButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(statusTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendButtonActionPerformed
        // TODO add your handling code here:
        statusTextField.setText(WAIT_NOTIFY);
        sendButton.setEnabled(false);
        cancelButton.setEnabled(true);
        rsTextArea.setText("");
        try {
            final HTTPRequest httprequest = new HTTPRequest(rqTextArea.getText(),hostTextField.getText());
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
        SETTINGS.params.host = hostTextField.getText();
        SETTINGS.params.root = fileChooser.getCurrentDirectory().getAbsolutePath();
        SETTINGS.params.isHighlightEnabled = highlightMenuItem.isSelected();
        SETTINGS.params.isLineNumbersEnabled = linenumbersMenuItem.isSelected();
        SETTINGS.params.setFrameBounds(getBounds());
        SETTINGS.params.dividerLocation = splitPane.getDividerLocation();
        SETTINGS.exportXml();
    }//GEN-LAST:event_formWindowClosing

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        // TODO add your handling code here:
        if (thNotify != null && thNotify.isAlive()) {
            thNotify.interrupt();
            cancelButton.setEnabled(false);
        }
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
        // TODO add your handling code here:
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
        // TODO add your handling code here:
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
        // TODO add your handling code here:
       formWindowClosing(new java.awt.event.WindowEvent(this, java.awt.event.WindowEvent.WINDOW_CLOSING));
       System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        // TODO add your handling code here:
        JOptionPane.showMessageDialog(null, "XmlSender® \nversion: " + VERSION());
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void hostTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hostTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_hostTextFieldActionPerformed

    private void linenumbersMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linenumbersMenuItemActionPerformed
        // TODO add your handling code here:
        rsScrollPane.setLineNumbersEnabled(linenumbersMenuItem.isSelected());
        rqScrollPane.setLineNumbersEnabled(linenumbersMenuItem.isSelected());
    }//GEN-LAST:event_linenumbersMenuItemActionPerformed

    private void highlightMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_highlightMenuItemActionPerformed
        // TODO add your handling code here:
        rqTextArea.setSyntaxEditingStyle(highlightMenuItem.isSelected() ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_NONE);
        rsTextArea.setSyntaxEditingStyle(highlightMenuItem.isSelected() ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_NONE);
    }//GEN-LAST:event_highlightMenuItemActionPerformed

    private void rqTextAreaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_rqTextAreaKeyPressed
        // TODO add your handling code here:
        if(evt.getKeyCode() == 114){
             searchActionPerformed(
                     new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, evt.isShiftDown()?"FindPrev":"FindNext"));
        }
        if(evt.getKeyCode() == 70 && evt.isControlDown()){
            if(rqTextArea.getSelectedText() != null)
                searchField.setText(rqTextArea.getSelectedText());
            gotoPanel.setVisible(false);
            searchPanel.setVisible(true);
            searchField.selectAll();
            searchField.requestFocus();
        }
        if(evt.getKeyCode() == 71 && evt.isControlDown()){
            gotoPanel.setVisible(true);
            searchPanel.setVisible(false);
            gotoField.requestFocus();
            gotoField.setText(gotoField.getText());
            gotoField.selectAll();
        }
        
    }//GEN-LAST:event_rqTextAreaKeyPressed

    private void searchCloseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchCloseButtonActionPerformed
        // TODO add your handling code here:
        searchPanel.setVisible(false);
    }//GEN-LAST:event_searchCloseButtonActionPerformed

    private void gotoCloseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gotoCloseButtonActionPerformed
        // TODO add your handling code here:
        gotoPanel.setVisible(false);
    }//GEN-LAST:event_gotoCloseButtonActionPerformed

    private void searchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchActionPerformed
        // TODO add your handling code here:
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
        // TODO add your handling code here:
        searchActionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, "FindNext"));
    }//GEN-LAST:event_searchFieldActionPerformed

    private void searchFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchFieldKeyPressed
        // TODO add your handling code here:
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

    private void gotoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gotoButtonActionPerformed
        // TODO add your handling code here:
        try {
            rqTextArea.setCaretPosition(
                    rqTextArea.getDocument().getDefaultRootElement().getElement((int) gotoSpinner.getValue() - 1).getStartOffset());
        } catch (Exception ex) {
        }
        gotoPanel.setVisible(false);
    }//GEN-LAST:event_gotoButtonActionPerformed
 


    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(XmlSenderGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(XmlSenderGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(XmlSenderGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(XmlSenderGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        try {
            UIManager.setLookAndFeel(
            UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (InstantiationException e) {
            System.out.println(e.getMessage());
        } catch (IllegalAccessException e) {
            System.out.println(e.getMessage());
        } catch (UnsupportedLookAndFeelException e) {
            System.out.println(e.getMessage());
        }

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
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JButton gotoButton;
    private javax.swing.JButton gotoCloseButton;
    private javax.swing.JPanel gotoPanel;
    private javax.swing.JSpinner gotoSpinner;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JCheckBoxMenuItem highlightMenuItem;
    private javax.swing.JLabel hostLabel;
    private javax.swing.JTextField hostTextField;
    private javax.swing.JCheckBoxMenuItem linenumbersMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openMenuItem;
    private org.fife.ui.rtextarea.RTextScrollPane rqScrollPane;
    private org.fife.ui.rsyntaxtextarea.RSyntaxTextArea rqTextArea;
    private org.fife.ui.rtextarea.RTextScrollPane rsScrollPane;
    private org.fife.ui.rsyntaxtextarea.RSyntaxTextArea rsTextArea;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JButton searchCloseButton;
    private javax.swing.JTextField searchField;
    private javax.swing.JButton searchNextButton;
    private javax.swing.JPanel searchPanel;
    private javax.swing.JButton searchPrevButton;
    private javax.swing.JButton sendButton;
    private javax.swing.JMenu settingsMenu;
    private javax.swing.JSplitPane splitPane;
    private javax.swing.JTextField statusTextField;
    // End of variables declaration//GEN-END:variables
}
