//
//			  MotionCompMenu.java
//
//		Copyright (C) 1998-2009 Takashi Yukawa
//
//		   This java source file conforms
//		GNU GENERAL PUBLIC LICENSE Version 2.
//
// Author:  Takashi Yukawa <yukawalab@gmail.com>
//
// Created: Sep.16, 1998
// Revised: Oct.03, 1998
// Revised: Aug.07, 2009
// Revised: Aug.09, 2015
package mocomp;

import static mocomp.MotionCompApp.workingDirectory;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.File;
import java.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.undo.*;
import javax.xml.parsers.ParserConfigurationException;
//import org.xml.sax.SAXException;

/**
 * 舞踊コンポーザーのメニューバー．
 * ファイルメニュー，編集メニューとヘルプメニューを配置する．
 * @version 1.00 09/16/98
 * @author Takashi Yukawa
 */
public class MotionCompMenu extends JMenuBar {
  private String savefilename = "untitled";
  private Font defaultFont;
  private ArrayList<MotionCodePanel> mcl; // clip board

  //private JTextComponent editor;
  private HashMap<String, Action> commands;
  private HashMap<String, JMenuItem> menuItems;
  private JComponent status;
  //  private JFrame elementTreeFrame;
  //  protected ElementTreePanel elementTreePanel;
//  protected FileDialog fileDialog;

  // Listener for the edits on the current document.
  protected UndoableEditListener undoHandler = new UndoHandler();
  // UndoManager that we add edits to.
  protected UndoManager undo = new UndoManager();
  // Suffix applied to the key used in resource file lookups for an image.
  public static final String imageSuffix = "Image";
  // Suffix applied to the key used in resource file lookups for a label.
  public static final String labelSuffix = "Label";
  // Suffix applied to the key used in resource file lookups for an action.
  public static final String actionSuffix = "Action";
  // Suffix applied to the key used in resource file lookups for a Accelerator
  public static final String acceleratorSuffix = "Accel";
  // Suffix applied to the key used in resource file lookups for tooltip text.
  public static final String tipSuffix = "Tooltip";
  public static final String newAction  = "new";
  public static final String openAction = "open";
  public static final String saveAction = "save";
  public static final String saveAsAction = "saveas";
  public static final String exportAction = "export";
  public static final String exitAction = "exit";
  public static final String cutAction = "cut";
  public static final String copyAction = "copy";
  public static final String pasteAction = "paste";
  public static final String selectallAction = "selectall";
  public static final String browserAction = "browser";
  public static final String aboutAction = "about";

  // --- action implementations -----------------------------------
  private UndoAction undoAction = new UndoAction();
  private RedoAction redoAction = new RedoAction();

  private Action[] defaultActions = {
    new NewAction(),
    new OpenAction(),
    new SaveAction(),
    new SaveAsAction(),
    new ExportAction(),
    new ExitAction(),
    new SelectAllAction(),
    new CutAction(),
    new CopyAction(),
    new PasteAction(),
    new BrowserAction(),
    new AboutAction(),
    undoAction,
    redoAction
  };

  public MotionCompMenu() {
//    super();
    defaultFont = new Font("Dialog", Font.PLAIN, MotionCompApp.getResourceValue("menuBar.fontsize"));
    this.setBorder(new ThinBevelBorder(BevelBorder.RAISED));
    // install the command table
    commands = new HashMap<>();

    for (Action a : defaultActions) { //    for (Action a: getActions()) {
      commands.put((String) a.getValue(Action.NAME), a);
    }

    menuItems = new HashMap<>();

    for (String key: MotionCompApp.getResourceString("menubar").split("\\s")) {
      JMenu menu = createMenu(key);
      if (menu != null) {
        menu.setFont(defaultFont);
        this.add(menu);
      }
    }

    setSaveMenuItemEnabled(false);
    getAction(cutAction).setEnabled(false);
    getAction(copyAction).setEnabled(false);
    getAction(pasteAction).setEnabled(false);
    getAction(selectallAction).setEnabled(false);

  } // MotionCompMenu()

  /**
   * Create a menu for the app.  By default this pulls the
   * definition of the menu from the associated resource file.
   */
  protected JMenu createMenu(String key) {
    JMenu menu = new JMenu(MotionCompApp.getResourceString(key + labelSuffix));
    for (String itemkey:MotionCompApp.getResourceString(key).split("\\s")) {
      if (itemkey.equals("-")) {
        menu.addSeparator();
      } else {
        JMenuItem mi = createMenuItem(itemkey);
        mi.setFont(defaultFont);
        menu.add(mi);
      }
    }
    return menu;
  }

  /**
   * This is the hook through which all menu items are
   * created.  It registers the result with the menuitem
   * hashtable so that it can be fetched with getMenuItem().
   * @param cmd
   * @see #getMenuItem
   */
  protected JMenuItem createMenuItem(String cmd) {
    JMenuItem mi = null;
    if (cmd.endsWith(".cb")) {
      String[] ss = cmd.split("\\.");
      cmd = ss[0];
      mi = new javax.swing.JCheckBoxMenuItem(MotionCompApp.getResourceString(cmd + labelSuffix));
    } else {
      mi = new JMenuItem(MotionCompApp.getResourceString(cmd + labelSuffix));
    }
 //   URL url = getResource(cmd + imageSuffix);
 //   if (url != null) {
 //     mi.setHorizontalTextPosition(JButton.RIGHT);
 //     mi.setIcon(new ImageIcon(url));
 //   }
    String astr = MotionCompApp.getResourceString(cmd + actionSuffix);
    if (astr == null) {
      astr = cmd;
    }
    mi.setActionCommand(astr);
    Action a = getAction(astr);
    if (a != null) {
      mi.addActionListener(a);
      a.addPropertyChangeListener(new ActionChangedListener(mi));
      mi.setEnabled(a.isEnabled());
    } else {
      mi.setEnabled(false);
    }
    String s = MotionCompApp.getResourceString(cmd + acceleratorSuffix);
    if (s != null) {
      mi.setAccelerator(KeyStroke.getKeyStroke(s));
    }
    menuItems.put(cmd, mi);
    return mi;
  }

  protected Action getAction(String cmd) {
    return (Action) commands.get(cmd);
  }

  // Yarked from JMenu, ideally this would be public.
  private class ActionChangedListener implements PropertyChangeListener {
    JMenuItem menuItem;

    ActionChangedListener(JMenuItem mi) {
      super();
      this.menuItem = mi;
    }
    @Override
    public void propertyChange(PropertyChangeEvent e) {
      String propertyName = e.getPropertyName();
      if (e.getPropertyName().equals(Action.NAME)) {
        String text = (String) e.getNewValue();
        menuItem.setText(text);
      } else if (propertyName.equals("enabled")) {
        Boolean enabledState = (Boolean) e.getNewValue();
        menuItem.setEnabled(enabledState.booleanValue());
      }
    }
  }

  class AboutAction extends AbstractAction {

    AboutAction() {
      super(aboutAction);
    }

    AboutAction(String nm) {
      super(nm);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      AboutDialog ad = new AboutDialog(getFrame(), false);
      ad.setVisible(true);
    }
  }

  class NewAction extends AbstractAction {

    NewAction() {
      super(newAction);
    }

    NewAction(String nm) {
      super(nm);
    }

    public void actionPerformed(ActionEvent e) {
      CheckIfChanged();
      MotionCompApp.sharedInstance().removeAllCodePanel();
      MotionCompApp.sharedInstance().repaint();
//    ddsfile = null;
      String openfilename = "untitled";
      MotionCompApp.sharedInstance().setTitle(openfilename);
      setSaveMenuItemEnabled(false);
      MotionCompApp.sharedInstance().clearInfoPanel();
/*
      Document oldDoc = getEditor().getDocument();
      if(oldDoc != null)
	oldDoc.removeUndoableEditListener(undoHandler);
      getEditor().setDocument(new PlainDocument());
      getEditor().getDocument().addUndoableEditListener(undoHandler);
      revalidate();
*/
    }
  } // class NewAction

  class UndoAction extends AbstractAction {
    public UndoAction() {
      super("Undo");
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      try {
        undo.undo();
      } catch (CannotUndoException ex) {
        System.err.println("Unable to undo: " + ex);
        ex.printStackTrace();
      }
      update();
      redoAction.update();
    }

    protected void update() {
      if(undo.canUndo()) {
        setEnabled(true);
        putValue(Action.NAME, undo.getUndoPresentationName());
      } else {
        setEnabled(false);
        putValue(Action.NAME, "Undo");
      }
    }
  } // UndoAction

  class RedoAction extends AbstractAction {
    public RedoAction() {
      super("Redo");
      setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      try {
        undo.redo();
      } catch (CannotRedoException ex) {
        System.out.println("Unable to redo: " + ex);
        ex.printStackTrace();
      }
      update();
      undoAction.update();
    }

    protected void update() {
      if (undo.canRedo()) {
        setEnabled(true);
        putValue(Action.NAME, undo.getRedoPresentationName());
      } else {
        setEnabled(false);
        putValue(Action.NAME, "Redo");
      }
    }
  } // class RedoAction

  class OpenAction extends NewAction {

    OpenAction() {
      super(openAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      File openfile = null;
      MPEG7Reader mp7reader = null;
      JFileChooser filechooser = new JFileChooser(workingDirectory);
      filechooser.setDialogTitle(MotionCompApp.getResourceString("fileChooserOpenTitle"));
      FileFilter filter = new FileNameExtensionFilter("Multimedia Content Description Interface", "mp7");
      filechooser.addChoosableFileFilter(filter);
      filechooser.setFileFilter(filter);
//      if (filechooser.showOpenDialog(MotionCompApp.sharedInstance().getFrame()) == JFileChooser.APPROVE_OPTION) {
      if (filechooser.showOpenDialog(MotionCompApp.sharedInstance()) == JFileChooser.APPROVE_OPTION) {
        openfile = filechooser.getSelectedFile();
        if (openfile.exists()) {
          MotionCompApp.sharedInstance().removeAllCodePanel();
          mp7reader = new MPEG7Reader(openfile);
          MotionCompApp.sharedInstance().setTitle(openfile.getName());
          workingDirectory = filechooser.getCurrentDirectory().toString();
          MotionCompApp.isChanged = true;
        } else {
          System.err.println("Can't find file: " + openfile.toString());
          openfile = null;
          return;
        }
      } else { // No file was chosen or an error occured
        return;
      }
      setSaveMenuItemEnabled(true);
//      setSelectAllEnabled(true);
      getAction(cutAction).setEnabled(false);
      getAction(copyAction).setEnabled(false);
      MotionCompApp.sharedInstance().setScoreAuthor(mp7reader.getAuthor());
      MotionCompApp.sharedInstance().setScoreTitle(mp7reader.getTitle());
    }
  } // class OpenAction

  class SaveAction extends SaveAsAction {

    SaveAction() {
      super(saveAction);
    }

    public void actionPerformed(ActionEvent e) {
      if (savefile == null) {
        super.actionPerformed(e);
        return;
      }
      MPEG7Writer mp7writer = new MPEG7Writer();
      mp7writer.savedata(savefile);
    }
  } // class SaveAction
 
  class SaveAsAction extends NewAction {
    protected File savefile = null;

    SaveAsAction() {
      super(saveAsAction);
    }

    SaveAsAction(String nm) {
      super(nm);
    }

    public void actionPerformed(ActionEvent e) {
      JFileChooser filechooser = new JFileChooser(workingDirectory);
      filechooser.setDialogTitle("名前を付けて保存");
      FileFilter filter = new FileNameExtensionFilter("Multimedia Content Description Interface", "mp7");
      filechooser.addChoosableFileFilter(filter);
      filechooser.setFileFilter(filter);
//      if (filechooser.showSaveDialog(MotionCompApp.sharedInstance().getFrame()) == JFileChooser.APPROVE_OPTION) {
      if (filechooser.showSaveDialog(MotionCompApp.sharedInstance()) == JFileChooser.APPROVE_OPTION) {
        savefile = filechooser.getSelectedFile();
        String fn = savefile.toString();
        if (!fn.endsWith(".mp7")) {// .dds
          savefile = new File(fn + ".mp7"); // .dds
        }
        if (savefile.exists()) {
          if (JOptionPane.showConfirmDialog(MotionCompApp.sharedInstance(), //.getFrame(),
                  savefile.toString() + "は既に存在します。\n上書きしますか?",
                  "名前を付けて保存",
                  JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != 0)
            return;             // yes = 0, no = 1, window closing = -1
//          savefilename = savefile.getName();
        }
        MPEG7Writer mp7writer = new MPEG7Writer();
        mp7writer.savedata(savefile);
        workingDirectory = filechooser.getCurrentDirectory().toString();
      } // actionPerformed()
    }
  } // class SaveAsAction

  class ExportAction extends NewAction {
    ExportAction() {
      super(exportAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Frame frame = MotionCompApp.sharedInstance(); //.getFrame();
      JFileChooser filechooser = new JFileChooser(workingDirectory);
      filechooser.setDialogTitle(MotionCompApp.sharedInstance().getResourceString("fileChooserExportTitle"));
      FileFilter vpmfilter = new FileNameExtensionFilter("Biovision animation motion data", "bva", "vpm");
      filechooser.addChoosableFileFilter(vpmfilter);
      filechooser.addChoosableFileFilter(new FileNameExtensionFilter("Biovision hierarchical motion data", "bvh"));
      filechooser.addChoosableFileFilter(new FileNameExtensionFilter("VRML file","wrl"));
      filechooser.setFileFilter(vpmfilter);
      if (filechooser.showSaveDialog(frame)==JFileChooser.APPROVE_OPTION) {
        workingDirectory = filechooser.getCurrentDirectory().toString();
        File f = filechooser.getSelectedFile();
        // 拡張子がついていなかったら自動的につける
        FileFilter ff = filechooser.getFileFilter();
        if (ff.getDescription().startsWith("Biovision animation motion data")) {
          if (!(f.toString().endsWith(".bva") || f.toString().endsWith(".BVA")))
            f = new File(f.toString() + ".bva");
        } else if (ff.getDescription().startsWith("Biovision hierarchical motion data")) {
          if (!(f.toString().endsWith(".bvh") || f.toString().endsWith(".BVH")))
            f = new File(f.toString() + ".bvh");
        } else if (ff.getDescription().startsWith("VRML file")) {
          if (!(f.toString().endsWith(".wrl") || f.toString().endsWith(".WRL")))
            f = new File(f.toString() + ".wrl");
        } else {
          System.err.println("Can't determine file type.");          /* XXXX: error  */
        }
        if (f.exists()) {
          String dialogtitle = "新しいモーションデータの書き出し";
          if (f.getName().endsWith(".wrl")) {
            dialogtitle = "VRML97ファイルの書き出し";
          }
          if (JOptionPane.showConfirmDialog(MotionCompApp.sharedInstance(), //.getFrame(),
                  f.toString() + "は既に存在します。\n上書きしますか?",
                  dialogtitle,
                  JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != 0)
            return;		// yes = 0, no = 1, window closing = -1
        } // ddsfile.exists()
//	MotionComp.sharedInstance().setCursor(new Cursor(Cursor.WAIT_CURSOR));
        MotionCompApp.sharedInstance().export2(f);
//	MotionComp.sharedInstance().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      } // showSaveDialog()
    } // actionPerformed()
  } // class ExportAction

  class BrowserAction extends AbstractAction {

    BrowserAction() {
      super(browserAction);
    }
    @Override
    public void actionPerformed(ActionEvent e) {
      MotionCompApp.sharedInstance().showBrowser(((JCheckBoxMenuItem)menuItems.get("browser")).isSelected());
    }

  }

  /**
   * Really lame implementation of an exit command
   */
  class ExitAction extends AbstractAction {

    ExitAction() {
      super(exitAction);
    }

    public void actionPerformed(ActionEvent e) {
      CheckIfChanged();
      System.exit(0);
    }
  } // class ExitAction

  class SelectAllAction extends NewAction {
    SelectAllAction() {
      super(selectallAction);
    }

    public void actionPerformed(ActionEvent e) {
        if (MotionCompApp.sharedInstance().getSelectedScorePanel() != null) {
            MotionCompApp.sharedInstance().getSelectedScorePanel().selectAll(true);
            getAction(cutAction).setEnabled(true);
            getAction(copyAction).setEnabled(true);
        }
    }
  }
  
  /**
   * Fetch the list of actions supported by this
   * editor.  It is implemented to return the list
   * of actions supported by the embedded JTextComponent
   * augmented with the actions defined locally.
   */
  public Action[] getActions() {
//  return TextAction.augmentList(editor.getActions(), defaultActions);
    return defaultActions;
  }

  class CutAction extends CopyAction {
    public CutAction() {
      super(cutAction);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      super.actionPerformed(e);
      MotionCompApp.sharedInstance().getSelectedScorePanel().removeSelected();
    }
  }

  class CopyAction extends AbstractAction {
    public CopyAction() {
      super(copyAction);
    }
    public CopyAction(String nm) {
      super(nm);
    }
    @Override
    public void actionPerformed(ActionEvent e) {
      mcl = MotionCompApp.sharedInstance().getSelectedScorePanel().getSelectedMotionCodePanel();
      System.out.println("mcl=" + mcl.toString());
      getAction(pasteAction).setEnabled(true);
    }
  }

  class PasteAction extends CopyAction {
    public PasteAction() {
      super(pasteAction);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
      MotionCodePanel mcp;
      Collections.sort(mcl); // mcl.sort();
      int baseindex = MotionCompApp.sharedInstance().getSelectedScorePanel().getLastPoint().x - ((MotionCodePanel) mcl.get(0)).getLocation().x;
      for (Iterator e = mcl.iterator(); e.hasNext();) {
        mcp = (MotionCodePanel) e.next();
        int xpos = baseindex + mcp.getLocation().x;
        MotionCompApp.sharedInstance().getSelectedScorePanel().addNewCodePanel(mcp.getMotionCode(), xpos * MotionCompApp.FPS / MotionCompApp.PPS);
      }
    }
  }

  /**
   * Find the hosting frame, for the file-chooser dialog.
   */
  protected Frame getFrame() {
    for (Container p = getParent(); p != null; p = p.getParent()) {
      if (p instanceof Frame) {
        return (Frame) p;
      }
    }
    return null;
  }

  //
  public void CheckIfChanged() {
    Frame frame = MotionCompApp.sharedInstance(); //.getFrame();
    if (MotionCompApp.isChanged) {
      if (savefilename.equals("untitled")) {
	//if (JOptionPane.showConfirmDialog(frame ,
	//	"データは変更されています。\n保存しますか?",
	//	"舞踊譜コンポーザ",
	//	JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)==0)
	//  filemenu.saveAsActionPerformed();
      }// else if (JOptionPane.showConfirmDialog(frame ,
      //		    ddsfilename + "は変更されています。\n保存しますか?",
      //	    	    "舞踊譜コンポーザ",
      //		    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)==0)
	//	filemenu.saveActionPerformed();
    }
  }

  /**
   * ファイルメニューの保存，名前を付けて保存，書き出しの項目の選択可能
   * ／不可能を設定する
   * @param b trueだったら選択可能，falseだったら選択不可能
   * @see MotionCompMenu#setSaveMenuItemEnabled(boolean)
   */
  public final void setSaveMenuItemEnabled(boolean b) {
    getAction(saveAction).setEnabled(b);
    getAction(saveAsAction).setEnabled(b);
    getAction(exportAction).setEnabled(b);
  }

  /**
   * 編集メニューの切り取りとコピーの項目の選択可能／不可能を設定する
   * @param b trueだったら選択可能，falseだったら選択不可能
   */
  public final void setCutAndCopyEnabled(boolean b) {
    getAction(cutAction).setEnabled(b);
    getAction(copyAction).setEnabled(b);
  }

  /**
   * 編集メニューのすべて選択の項目の選択可能／不可能を設定する
   * @param b trueだったら選択可能，falseだったら選択不可能
   */
  public final void setSelectAllEnabled(boolean b) {
    getAction(selectallAction).setEnabled(b);
  }

  class UndoHandler implements UndoableEditListener {

    /**
     * Messaged when the Document has created an edit, the edit is
     * added to <code>undo</code>, an instance of UndoManager.
     */
    public void undoableEditHappened(UndoableEditEvent e) {
      undo.addEdit(e.getEdit());
      undoAction.update();
      redoAction.update();
    }
  }

  public static void main(String args[]) {
    JFrame f = new JFrame("MotionCompMenu Test");
    MotionCompMenu m = new MotionCompMenu();
//    f.getContentPane().add(m);
//    f.setSize(320, 80);
//    f.setVisible(true);
//    m.setSaveMenuItemEnabled(true);
  }

} // end of class MotionCompMenu
