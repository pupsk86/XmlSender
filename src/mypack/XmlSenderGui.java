/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mypack;

import com.alee.extended.painter.DefaultPainter;
import com.alee.laf.WebLookAndFeel;
import java.awt.*;
import java.io.*;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
        hostField.setText(SETTINGS.params.host);
        fileChooser.setCurrentDirectory(new java.io.File(SETTINGS.params.root));
        highlightMenuItem.setSelected(SETTINGS.params.isHighlightEnabled);
        linenumbersMenuItem.setSelected(SETTINGS.params.isLineNumbersEnabled);
        setBounds(SETTINGS.params.getFrameBounds());
        splitPane.setDividerLocation(SETTINGS.params.dividerLocation);

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
        gotoCloseButton.setPainter(new FlatButtonPainter ());
        searchCloseButton.setPainter(new FlatButtonPainter ());
        gotoButton.setPainter(new FlatButtonPainter ());
        searchPrevButton.setPainter(new FlatButtonPainter ());
        searchNextButton.setPainter(new FlatButtonPainter ());
        gotoCloseButton.setMoveIconOnPress ( false );
        searchCloseButton.setMoveIconOnPress ( false );
        gotoButton.setMoveIconOnPress ( false );
        searchNextButton.setMoveIconOnPress ( false );
        searchPrevButton.setMoveIconOnPress ( false );
    }

    public static class FlatButtonPainter extends DefaultPainter<AbstractButton>
    {
        // Border colors
        private Color rolloverBorder = new Color ( 172, 172, 172 );
        private Color pressedBorder = new Color ( 162, 162, 162 );
        // Background colors
        private Color rolloverBg = new Color(242,242,242);
        private Color pressedBg = new Color(248,248,248);

        private final int round = 8;
        private float[] borderFractions = { 0f, 1f };
        
        // Margin
        private Insets margin = new Insets ( 5, 5, 5, 5 );

        public FlatButtonPainter ()
        {
            super ();
        }

        @Override
        public Insets getMargin ( AbstractButton c )
        {
            return margin;
        }

        @Override
        public void paint ( Graphics2D g2d, Rectangle bounds, AbstractButton c )
        {
            ButtonModel buttonModel = c.getModel ();

            if (buttonModel.isRollover() || buttonModel.isPressed()) {
                // Background
                g2d.setPaint (buttonModel.isPressed()? pressedBg : rolloverBg);
                g2d.fillRoundRect ( 2, 2, c.getWidth () - 4, c.getHeight () - 4, 7, 7 );
                
                // Border                
                g2d.setPaint (buttonModel.isPressed()? pressedBorder : rolloverBorder);
                g2d.drawRoundRect(2, 2, c.getWidth () - 5, c.getHeight () - 5, round, round );
                if (buttonModel.isPressed()) {
                    Color[] colors  =  new Color[]{ new Color(196,196,196), new Color(237,237,237) };
                    g2d.setPaint (new LinearGradientPaint ( 0, 2, 0, c.getHeight () - 2, borderFractions, colors ));
                    g2d.drawRoundRect(3, 3, c.getWidth () - 7, c.getHeight () - 7, round, round );
                    
                    colors  =  new Color[]{ new Color(226,226,226), new Color(237,237,237) };
                    g2d.setPaint (new LinearGradientPaint ( 0, 2, 0, c.getHeight () - 2, borderFractions, colors ));
                    g2d.drawRoundRect(4, 4, c.getWidth () - 9, c.getHeight () - 8, round, round );
                } else {
                    g2d.setPaint (new Color(230,230,230));
                    g2d.drawRoundRect(1, 1, c.getWidth () - 3, c.getHeight () - 3, round, round );
                }
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
        statusTextField.setToolTipText("Response");
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

        splitPane.setDividerLocation(200);
        splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPane.setContinuousLayout(true);

        rsScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rsScrollPane.setLineNumbersEnabled(true);

        rsTextArea.setEditable(false);
        rsTextArea.setColumns(20);
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

        searchCloseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/close-button.png"))); // NOI18N
        searchCloseButton.setFocusable(false);
        searchCloseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchCloseButtonActionPerformed(evt);
            }
        });

        searchNextButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/arrow_down.png"))); // NOI18N
        searchNextButton.setActionCommand("FindNext");
        searchNextButton.setFocusable(false);
        searchNextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchActionPerformed(evt);
            }
        });

        searchPrevButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/arrow_up.png"))); // NOI18N
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 229, Short.MAX_VALUE)
                .addComponent(searchCloseButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        searchPanelLayout.setVerticalGroup(
            searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchPanelLayout.createSequentialGroup()
                .addGroup(searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(searchNextButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                    .addComponent(searchPrevButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                    .addComponent(searchCloseButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        gotoPanel.setMaximumSize(new java.awt.Dimension(100, 50));
        gotoPanel.setMinimumSize(new java.awt.Dimension(100, 50));
        gotoPanel.setPreferredSize(new java.awt.Dimension(100, 50));

        gotoCloseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/close-button.png"))); // NOI18N
        gotoCloseButton.setFocusable(false);
        gotoCloseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gotoCloseButtonActionPerformed(evt);
            }
        });

        gotoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/arrow_right.png"))); // NOI18N
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
                .addComponent(gotoCloseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
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

        hostField.setToolTipText("");
        hostField.setHideInputPromptOnFocus(false);
        hostField.setInputPrompt("Host");
        hostField.setPreferredSize(new java.awt.Dimension(11, 28));

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
                    .addComponent(searchPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(sendButton, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statusTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(splitPane, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(gotoPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 576, Short.MAX_VALUE)
                    .addComponent(hostField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(splitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 324, Short.MAX_VALUE)
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
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JMenu fileMenu;
    private com.alee.laf.button.WebButton gotoButton;
    private com.alee.laf.button.WebButton gotoCloseButton;
    private javax.swing.JPanel gotoPanel;
    private javax.swing.JSpinner gotoSpinner;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JCheckBoxMenuItem highlightMenuItem;
    private com.alee.laf.text.WebTextField hostField;
    private javax.swing.JCheckBoxMenuItem linenumbersMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openMenuItem;
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
    private javax.swing.JMenu settingsMenu;
    private javax.swing.JSplitPane splitPane;
    private javax.swing.JTextField statusTextField;
    // End of variables declaration//GEN-END:variables
}
