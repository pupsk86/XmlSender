/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mypack;

import com.alee.extended.image.WebImage;
import com.alee.laf.StyleConstants;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBoxUI;
import com.alee.managers.language.LanguageManager;
import com.alee.managers.proxy.WebProxyAuthenticator;
import com.alee.utils.LafUtils;
import com.alee.utils.SwingUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Authenticator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.*;
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
    
    private String Host(){
        return ((JTextField)hostList.getEditor().getEditorComponent()).getText();
    }
    
    private final String   WAIT_NOTIFY = "Send XML. Please wait...";
    private final Settings SETTINGS = new Settings();
    private final Color    GREEN = new java.awt.Color(0,100,0);
    private final Color    RED = new java.awt.Color(180,0,0);
    private final Color    GRAY = new java.awt.Color(224,224,224);
    private final Color    WHITE = new java.awt.Color(255,255,255);
    private final Color    BLUE = new java.awt.Color(0,0,120);
    
    private Thread thSender = null;
    private Thread thNotify = null;

    public XmlSenderGui() {
        Locale.setDefault(Locale.ENGLISH);
        initComponents();

        //Startup settings
        fileChooser.setCurrentDirectory(new java.io.File(SETTINGS.params.root));
        highlightMenuItem.setSelected(SETTINGS.params.isHighlightEnabled);
        linewrapMenuItem.setSelected(SETTINGS.params.isLineWrapEnabled);
        linenumbersMenuItem.setSelected(SETTINGS.params.isLineNumbersEnabled);
        setBounds(SETTINGS.params.getFrameBounds());
        splitPane.setDividerLocation(SETTINGS.params.dividerLocation);
        splitPane.setLastDividerLocation(SETTINGS.params.lastDividerLocation);
        for(String host : SETTINGS.params.hostList){
            hostList.addItem(host);
        }
        try{
            hostList.setSelectedIndex(SETTINGS.params.hostIdx);
            if(SETTINGS.params.hostIdx == -1) {
                hostList.setSelectedItem(SETTINGS.params.hostCurrent);
            }
        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }
        //Startup settings

        SyntaxScheme mySyntaxScheme = new SyntaxScheme(true);
        
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_ATTRIBUTE, new Style(GREEN));
        mySyntaxScheme.setStyle(TokenTypes.OPERATOR, new Style(GREEN));
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE, new Style(RED));
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_DELIMITER, new Style(Color.BLACK));
        mySyntaxScheme.setStyle(TokenTypes.MARKUP_TAG_NAME, new Style(BLUE,Style.DEFAULT_BACKGROUND,rqTextArea.getFont().deriveFont(java.awt.Font.BOLD)));
        
        rqTextArea.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_XML );
        rqTextArea.setCurrentLineHighlightColor(GRAY);
        rqTextArea.setSyntaxScheme(mySyntaxScheme);
        rqTextArea.setLineWrap(linewrapMenuItem.isSelected());
        
        rsTextArea.setSyntaxEditingStyle( SyntaxConstants.SYNTAX_STYLE_XML );
        rsTextArea.setCurrentLineHighlightColor(WHITE);
        rsTextArea.setSyntaxScheme(mySyntaxScheme);
        rsTextArea.setLineWrap(linewrapMenuItem.isSelected());

        rsScrollPane.setLineNumbersEnabled(linenumbersMenuItem.isSelected());
        rqScrollPane.setLineNumbersEnabled(linenumbersMenuItem.isSelected());
        
        rqTextArea.setSyntaxEditingStyle(highlightMenuItem.isSelected() ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_NONE);
        rsTextArea.setSyntaxEditingStyle(highlightMenuItem.isSelected() ? SyntaxConstants.SYNTAX_STYLE_XML : SyntaxConstants.SYNTAX_STYLE_NONE);
        
        for (Component component : gotoSpinner.getComponents()){
            if(component instanceof NumberEditor){
                NumberEditor numEditor = (NumberEditor)component;
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
        
        searchField.setTrailingComponent ( new WebImage (  new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/search.png")) ) );
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

        boolean isPopupEnd = false;
        for(Component comp : rqTextArea.getPopupMenu().getComponents()){
            if (isPopupEnd) comp.setVisible(false);
            if (comp instanceof JMenuItem) {
                JMenuItem itemPopMenu = (JMenuItem)comp;
                switch (itemPopMenu.getActionCommand()){
                    case "Undo":
                        itemPopMenu.setIcon(undoMenuItem.getIcon());
                        break;
                    case "Redo":
                        itemPopMenu.setIcon(redoMenuItem.getIcon());
                        break;
                    case "Cut":
                        itemPopMenu.setIcon(cutMenuItem.getIcon());
                        break;
                    case "Copy":
                        itemPopMenu.setIcon(copyMenuItem.getIcon());
                        break;
                    case "Paste":
                        itemPopMenu.setIcon(pasteMenuItem.getIcon());
                        break;
                    case "Delete":
                        itemPopMenu.setIcon(deleteMenuItem.getIcon());
                        break;
                    case "Select All":
                        itemPopMenu.setIcon(selectAllMenuItem.getIcon());
                        isPopupEnd = true;
                        break;
                }
            }
        }

        hostList.setUI(new WebComboBoxUI(){
            @Override
            public void paintCurrentValueBackground ( Graphics g, Rectangle bounds, boolean hasFocus )
            {
                Graphics2D g2d = ( Graphics2D ) g;
                comboBox.setBackground ( Color.WHITE );
                LafUtils.drawWebStyle ( g2d, comboBox, SwingUtils.hasFocusOwner ( comboBox ) ? StyleConstants.fieldFocusColor : StyleConstants.shadeColor,
                                        this.getShadeWidth(), this.getRound(), true, false );
            }
        });
        
        mainButton = new WebButton("Send",iconStart);
        mainButton.setRound(9);
        progressOverlay.setComponent(mainButton);
        mainButton.addActionListener ( new ActionListener () {
            @Override
            public void actionPerformed ( ActionEvent e )
            {
                boolean showLoad = !progressOverlay.isShowLoad ();
                progressOverlay.setShowLoad ( showLoad );
                mainButton.setText ( showLoad ? "Cancel" : "Send" );
                mainButton.setIcon ( showLoad ? iconStop : iconStart );
                if(showLoad) runSendXml();
                else cancelSendXml();
            }
        });
        
        menuBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F10"), "none"); 

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
        splitPane = new com.alee.laf.splitpane.WebSplitPane();
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
        hostList = new com.alee.laf.combobox.WebComboBox();
        progressOverlay = new com.alee.extended.progress.WebProgressOverlay();
        statusTextField = new com.alee.laf.text.WebTextField();
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
        deleteMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        selectAllMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        findMenuItem = new javax.swing.JMenuItem();
        gotoMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        highlightMenuItem = new javax.swing.JCheckBoxMenuItem();
        linewrapMenuItem = new javax.swing.JCheckBoxMenuItem();
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

        splitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        splitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);

        rsScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rsScrollPane.setLineNumbersEnabled(true);

        rsTextArea.setColumns(20);
        rsTextArea.setEditable(false);
        rsTextArea.setRows(5);
        rsTextArea.setToolTipText("");
        rsTextArea.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        rsScrollPane.setViewportView(rsTextArea);

        splitPane.setBottomComponent(rsScrollPane);

        rqScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        rqScrollPane.setLineNumbersEnabled(true);

        rqTextArea.setColumns(20);
        rqTextArea.setRows(5);
        rqTextArea.setToolTipText("");
        rqTextArea.setCloseMarkupTags(false);
        rqTextArea.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        rqTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                rqTextAreaKeyPressed(evt);
            }
        });
        rqScrollPane.setViewportView(rqTextArea);

        splitPane.setLeftComponent(rqScrollPane);

        searchPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 12, 5, 12));
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
        searchField.setMargin(new java.awt.Insets(0, 0, 0, 2));
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
                .addComponent(searchPrevButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchNextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 321, Short.MAX_VALUE)
                .addComponent(searchCloseButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        searchPanelLayout.setVerticalGroup(
            searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchPanelLayout.createSequentialGroup()
                .addGroup(searchPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(searchNextButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchPrevButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(searchField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                    .addComponent(searchCloseButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        gotoPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 12, 5, 12));
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
                .addGroup(gotoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, gotoPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(gotoSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(gotoPanelLayout.createSequentialGroup()
                        .addGroup(gotoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(gotoButton, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gotoCloseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        hostList.setBackground(new java.awt.Color(254, 254, 254));
        hostList.setEditable(true);

        progressOverlay.setConsumeEvents(false);

        statusTextField.setBackground(new java.awt.Color(212, 212, 212));
        statusTextField.setEditable(false);
        statusTextField.setMargin(new java.awt.Insets(0, 5, 0, 5));
        statusTextField.setRound(9);

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
        copyMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        copyMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-copy.png"))); // NOI18N
        copyMenuItem.setText("Copy");
        editMenu.add(copyMenuItem);

        pasteMenuItem.setAction(rqTextArea.getAction(RTextArea.PASTE_ACTION));
        pasteMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        pasteMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-paste.png"))); // NOI18N
        pasteMenuItem.setText("Paste");
        editMenu.add(pasteMenuItem);

        deleteMenuItem.setAction(rqTextArea.getAction(RTextArea.DELETE_ACTION));
        deleteMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-clear.png"))); // NOI18N
        editMenu.add(deleteMenuItem);
        editMenu.add(jSeparator4);

        selectAllMenuItem.setAction(rqTextArea.getAction(RTextArea.SELECT_ALL_ACTION));
        selectAllMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        selectAllMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/edit-select-all.png"))); // NOI18N
        selectAllMenuItem.setText("Select All");
        editMenu.add(selectAllMenuItem);
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

        linewrapMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F10, 0));
        linewrapMenuItem.setSelected(true);
        linewrapMenuItem.setText("Line wrap");
        linewrapMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                linewrapMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(linewrapMenuItem);

        linenumbersMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F11, 0));
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

        aboutMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/help-about.png"))); // NOI18N
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
            .addComponent(splitPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(hostList, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(progressOverlay, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(statusTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
            .addComponent(gotoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 695, Short.MAX_VALUE)
            .addComponent(searchPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 695, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(splitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gotoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hostList, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(progressOverlay, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(statusTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {gotoPanel, searchPanel});

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        List<String> lst = new ArrayList<String>();
        for(int i=0; i<hostList.getItemCount(); i++){
            lst.add((String)hostList.getItemAt(i));
        }
        SETTINGS.params.hostList = lst;
        SETTINGS.params.hostIdx = hostList.getSelectedIndex();
        if(SETTINGS.params.hostIdx == -1) {
            SETTINGS.params.hostCurrent = Host();
        }
        SETTINGS.params.root = fileChooser.getCurrentDirectory().getAbsolutePath();
        SETTINGS.params.isHighlightEnabled = highlightMenuItem.isSelected();
        SETTINGS.params.isLineWrapEnabled = linewrapMenuItem.isSelected();
        SETTINGS.params.isLineNumbersEnabled = linenumbersMenuItem.isSelected();
        SETTINGS.params.setFrameBounds(getBounds());
        SETTINGS.params.dividerLocation = splitPane.getDividerLocation();
        SETTINGS.params.lastDividerLocation = splitPane.getLastDividerLocation();
        SETTINGS.exportXml();
    }//GEN-LAST:event_formWindowClosing

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
        searchPanel.setVisible(false);
    }//GEN-LAST:event_gotoCloseButtonActionPerformed

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

    private void searchCloseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchCloseButtonActionPerformed
        searchPanel.setVisible(false);
    }//GEN-LAST:event_searchCloseButtonActionPerformed

    private void linewrapMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_linewrapMenuItemActionPerformed
        rqTextArea.setLineWrap(linewrapMenuItem.isSelected());
        rsTextArea.setLineWrap(linewrapMenuItem.isSelected());
    }//GEN-LAST:event_linewrapMenuItemActionPerformed
    private void cancelSendXml(){
        if (thNotify != null && thNotify.isAlive()) {
            thNotify.interrupt();
        }
    }
    private void runSendXml(){
        statusTextField.setText(WAIT_NOTIFY);
        rsTextArea.setText("");
        try {
            final HTTPRequest httprequest = new HTTPRequest(rqTextArea.getText(),Host());
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
                        thSender.interrupt();
                    } catch (Exception e) {
                    }
                    
                    if (thSender.isAlive()){
                        statusTextField.setText("Canceled");
                    } else {
                        statusTextField.setText(httprequest.getResult());
                        rsTextArea.setText(httprequest.getResultEntity());
                        System.out.println(httprequest.getResultEntity());
                        if(httprequest.getResultCode() == 200 ){
                            boolean isFind = false;
                            for(int i =0; i < hostList.getItemCount(); i ++){
                                if( hostList.getItemAt(i).equals(Host())){
                                    isFind = true;
                                    break;
                                }
                            }
                            if(!isFind){
                                hostList.addItem(Host());
                                hostList.setSelectedIndex(hostList.getItemCount() - 1);
                            }
                        }
                    }
                    
                    progressOverlay.setShowLoad(false);
                    mainButton.setText (  "Send" );
                    mainButton.setIcon (  iconStart );
                }
            });
            thNotify.start();
        }catch (Exception e) {
            statusTextField.setText(e.getMessage());
            progressOverlay.setShowLoad(false);
            mainButton.setText (  "Send" );
            mainButton.setIcon (  iconStart );
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        LanguageManager.DEFAULT = LanguageManager.ENGLISH;
        WebLookAndFeel.install ();
        System.setProperty ( "java.net.useSystemProxies", "false" );
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
    private WebButton mainButton;
    private ImageIcon iconStart = new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/media-playback-start.png"));
    private ImageIcon iconStop = new javax.swing.ImageIcon(getClass().getResource("/mypack/icons/media-playback-stop.png"));
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenuItem copyMenuItem;
    private javax.swing.JMenuItem cutMenuItem;
    private javax.swing.JMenuItem deleteMenuItem;
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
    private com.alee.laf.combobox.WebComboBox hostList;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JCheckBoxMenuItem linenumbersMenuItem;
    private javax.swing.JCheckBoxMenuItem linewrapMenuItem;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JMenuItem pasteMenuItem;
    private com.alee.extended.progress.WebProgressOverlay progressOverlay;
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
    private javax.swing.JMenuItem selectAllMenuItem;
    private com.alee.laf.splitpane.WebSplitPane splitPane;
    private com.alee.laf.text.WebTextField statusTextField;
    private javax.swing.JMenuItem undoMenuItem;
    private javax.swing.JMenu viewMenu;
    // End of variables declaration//GEN-END:variables
}
