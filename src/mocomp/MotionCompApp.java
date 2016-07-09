//
// MotionCompApp.java
//
// Copyright (C) 2009 Takashi Yukawa
// This source is licenced under the MIT license
// https://github.com/2x4/mc/blob/master/LICENSE
//
package mocomp;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.web3d.vrml.sav.InputSource;

// メニューバーの下にモーションスコアの作者とタイトルを表示するためのパネル
/**
 *
 * @author Takashi Yukawa (yukawalab@gmail.com)
 */
public class MotionCompApp extends javax.swing.JFrame {
  public static final String VERSION = "MotionCompApp  2016/07/09";

  // 舞踊譜を編集するためのおおもとになるパネル
  // @see ComposePanel
  private final ComposePanel composepanel;

  private final LabelPanel labelpanel;

    // 最後に操作したScorePanel
  // @see ScorePanel
  private ScorePanel selectedScorePanel;

  // キャプチャデータの1秒あたりのフレーム数
  public static final int FPS = 30;

  /**
   * コンポーザで舞踊符を表示するときの1秒あたりのピクセル数を表す定数．
   * @see ComposePanel
   * @see MotionCodePanel
   * @see ScorePanel
   */
  public static final int PPS = FPS;

  // 舞踊符パネルを表示するときに使う1フレームあたりのピクセル数
  public static final float FPP = (float)FPS / (float)PPS;

  // 舞踊譜が編集されたかどうかを表すフラグ
  public static boolean isChanged = false;

  static MotionCompApp instance;
  public static MotionCompApp sharedInstance() {
    return instance;
  }

  private static ResourceBundle resources;

  static {
    try {
      resources = ResourceBundle.getBundle("resources.MotionComp", Locale.getDefault());
    } catch (MissingResourceException mre) {
      System.err.println("resources/MotionComp.properties not found");
      System.exit(1);
    }
  }

  static String getResourceString(String nm) {
    String str;
    try {
      str = resources.getString(nm);
    } catch (MissingResourceException mre) {
      str = null;
    }
    return str;
  }

  static int getResourceValue(String nm) {
    return Integer.parseInt(getResourceString(nm));
  }

  // 舞踊符インデックス(MPEG7)から舞踊符を読み込みParse Treeを作成
  public static final MPEG7Manager mp7mgr = new MPEG7Manager();

  public static int MAX_TIME = getResourceValue("maxtime");
  private static MotionCodeChooser mcc;
  private File openfile = null;
  private InfoPanel infopanel = null;
  public final String getScoreTitle() { return infopanel.getTitle(); }
  public final void setScoreTitle(String title) { infopanel.setTitle(title); }
  public final String getScoreAuthor() { return infopanel.getAuthor(); }
  public final void setScoreAuthor(String author) { infopanel.setAuthor(author);  }
  public final void clearInfoPanel() { infopanel.clear(); }
//  private OGLBrowser browser;
  private X3dBrowser browser;
  private MotionCompMenu menubar;
  
  public static String workingDirectory;
  static {
    String osname = System.getProperty("os.name");
    workingDirectory = System.getenv("HOMEPATH");
    if (osname.startsWith("Windows")) {
      workingDirectory = workingDirectory + "/Documents"; // for windows os
    } else {
      workingDirectory = System.getenv("HOME");
    }
  }

  /** Creates new form MotionComp2 */
  public MotionCompApp() {
    instance = this;
    initComponents();
    menubar = new MotionCompMenu();
    this.setJMenuBar(menubar);
    setFrameTitle(getResourceString("newfilename"));

    mcc = new MotionCodeChooser();
    getContentPane().setBackground(Color.WHITE);
    infopanel = new InfoPanel();
    jPanel1.add(infopanel);
    composepanel = new ComposePanel();
    jScrollPane1.setViewportView(composepanel);
    labelpanel = new LabelPanel(mp7mgr.getPartNames());
    jPanel2.add(labelpanel, java.awt.BorderLayout.CENTER);
    jPanel2.setPreferredSize(new Dimension(getResourceValue("labelPanel.width"), jPanel2.getSize().height));

    browser = new X3dBrowser();
    jPanel4.add(browser, BorderLayout.CENTER);
    jPanel4.setSize(400, 400);
    jPanel4.setVisible(false);
    pack();
  }

  public final LabelPanel getLabelPanel() {
    return labelpanel;
  }

  public static final MotionCodeChooser getMotionCodeChooser() {
    return mcc;
  }

  // @see ComposePanel#getScorePanelList
  public final ArrayList<ScorePanel> getScoreList() {
    return composepanel.getScorePanelList();
  }

  /**
   * 引数で与える部位コードに対応するスコアパネルを探して返す．
   * @see ComposePanel#getScorePanel(String)
   * @param partcode 探すスコアパネルの部位コード
   * @return 部位コードがpartcodeであるスコアパネル
   */
  public final ScorePanel getScorePanel(String partcode) {
    return composepanel.getScorePanel(partcode);
  }
  public final ScorePanel getScorePanel(int index) {
    return composepanel.getScorePanel(index);
  }

  // スコアパネルにあるすべてのモーションコードパネルを削除する．
  // @see ComposePanel#removeAllCodePanel
  public final void removeAllCodePanel() {
    composepanel.removeAllCodePanel();
  }

  /**
   * このアプリケーションのフレームタイトルを設定する
   * @param title このアプリケーションのフレームタイトル
   */
  public final void setFrameTitle(String title) {
    setTitle(getResourceString("Title") + ": " + title);
  }

  /**
   * ファイルメニューの保存の項目の選択可能／不可能を設定する
   * @param b trueだったら選択可能，falseだったら選択不可能
   * @see MotionCompMenu#setSaveMenuItemEnabled(boolean)
   */
  public final void setSaveMenuItemEnabled(boolean b) {
    menubar.setSaveMenuItemEnabled(b);
  }

  /**
   * 編集メニューの切り取りとコピーの項目の選択可能／不可能を設定する
   * @param b trueだったら選択可能，falseだったら選択不可能
   */
  public final void setCutAndCopyEnabled(boolean b) {
    menubar.setCutAndCopyEnabled(b);
  }

  /**
   * 編集メニューの切り取りとコピーの項目の選択可能／不可能を設定する
   * ScorePanelを調べて，操作可能なメニューを設定する。
   */
  public final void setCutAndCopyEnabled() {
    int ns = 0;
    ns = composepanel.getScorePanelList().stream().map((l1) -> (ScorePanel) l1).map((sp) -> sp.getSelectedPanelCounts()).reduce(ns, Integer::sum);
    menubar.setCutAndCopyEnabled(ns > 0);
  }

  /**
   * 編集メニューのすべて選択の項目の選択可能／不可能を設定する
   * @param b trueだったら選択可能，falseだったら選択不可能
   */

  public final void setSelectAllEnabled(boolean b) {
    menubar.setSelectAllEnabled(b);
  }

  public ExportTask export2(File savefile) {
    final ProgressMonitor pm = new ProgressMonitor(MotionCompApp.this, "Exporting motion data", "", 0, 100);
    final ExportTask task2 = new ExportTask(savefile);
    task2.addPropertyChangeListener(
    new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
          pm.setProgress((Integer) evt.getNewValue());
          pm.setNote(ExportTask.note);
        }
      }
            });
    task2.execute();
    System.out.println("task2.execute() done.");
    return task2;
 }

  // 最後に操作したスコアパネルを得る
  // @see ScorePanel
  public final ScorePanel getSelectedScorePanel() {
    return selectedScorePanel;
  }

  /**
   * 最後に操作したスコアパネルを設定する
   * @param sp    設定するスコアパネル
   * @see ScorePanel
   */
  public final void setSelectedScorePanel(ScorePanel sp) {
    selectedScorePanel = sp;
  }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jPanel1 = new javax.swing.JPanel();
    jPanel6 = new javax.swing.JPanel();
    jPanel5 = new javax.swing.JPanel();
    jPanel2 = new javax.swing.JPanel();
    jScrollPane1 = new javax.swing.JScrollPane();
    jPanel4 = new javax.swing.JPanel();
    jPanel3 = new javax.swing.JPanel();
    jButton2 = new javax.swing.JButton();
    jButton3 = new javax.swing.JButton();
    jToggleButton1 = new javax.swing.JToggleButton();
    jButton4 = new javax.swing.JButton();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    setBackground(new java.awt.Color(255, 255, 255));
    getContentPane().setLayout(new java.awt.BorderLayout(1, 1));

    jPanel1.setBackground(new java.awt.Color(255, 102, 51));
    jPanel1.setMinimumSize(new java.awt.Dimension(100, 30));
    jPanel1.setPreferredSize(new java.awt.Dimension(100, 30));
    jPanel1.setLayout(new java.awt.BorderLayout());
    getContentPane().add(jPanel1, java.awt.BorderLayout.NORTH);

    jPanel6.setLayout(new java.awt.BorderLayout());

    jPanel5.setBackground(new java.awt.Color(255, 255, 255));
    jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
    jPanel5.setMinimumSize(new java.awt.Dimension(225, 40));
    jPanel5.setPreferredSize(new java.awt.Dimension(636, 250));
    jPanel5.setLayout(new java.awt.BorderLayout(10, 10));

    jPanel2.setBackground(new java.awt.Color(255, 255, 255));
    jPanel2.setPreferredSize(new java.awt.Dimension(50, 50));
    jPanel2.setLayout(new java.awt.BorderLayout());
    jPanel5.add(jPanel2, java.awt.BorderLayout.WEST);

    jScrollPane1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    jScrollPane1.setMaximumSize(new java.awt.Dimension(500, 500));
    jScrollPane1.setMinimumSize(new java.awt.Dimension(100, 100));
    jScrollPane1.setPreferredSize(new java.awt.Dimension(400, 200));
    jPanel5.add(jScrollPane1, java.awt.BorderLayout.CENTER);

    jPanel6.add(jPanel5, java.awt.BorderLayout.PAGE_START);

    jPanel4.setBackground(new java.awt.Color(255, 255, 255));
    jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 10, 10, 10));
    jPanel4.setRequestFocusEnabled(false);
    jPanel4.setLayout(new java.awt.BorderLayout());

    jPanel3.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    jPanel3.setPreferredSize(new java.awt.Dimension(608, 50));

    jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/118.png"))); // NOI18N
    jButton2.setMargin(new java.awt.Insets(1, 1, 1, 1));
    jPanel3.add(jButton2);

    jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/113.png"))); // NOI18N
    jButton3.setMargin(new java.awt.Insets(1, 1, 1, 1));
    jButton3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jButton3ActionPerformed(evt);
      }
    });
    jPanel3.add(jButton3);

    jToggleButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/110.png"))); // NOI18N
    jToggleButton1.setIconTextGap(0);
    jToggleButton1.setMargin(new java.awt.Insets(1, 1, 1, 1));
    jToggleButton1.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/116.png"))); // NOI18N
    jToggleButton1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jToggleButton1ActionPerformed(evt);
      }
    });
    jPanel3.add(jToggleButton1);

    jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/114.png"))); // NOI18N
    jButton4.setMargin(new java.awt.Insets(1, 1, 1, 1));
    jPanel3.add(jButton4);

    jPanel4.add(jPanel3, java.awt.BorderLayout.PAGE_END);

    jPanel6.add(jPanel4, java.awt.BorderLayout.CENTER);

    getContentPane().add(jPanel6, java.awt.BorderLayout.CENTER);

    pack();
  }// </editor-fold>//GEN-END:initComponents

    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed

      if (jToggleButton1.isSelected()) {
        // VRMLファイルをエクスポート
// ここで待ち時間をとらないと、アイコンが変更されない

        if (isChanged) {
          String filename = "motioncomp_preview.wrl";
          File file = new File(filename);
          ExportTask task = export2(file);
          try {
            task.get(); // バックグラウンドで実行されるexport2の終了を待つ
          } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(MotionCompApp.class.getName()).log(Level.SEVERE, null, ex);
          }

          // Xj3Dからファイルをオープン
          System.out.println("trying to load file:/" + file.getAbsolutePath());
            InputSource is = new InputSource(file);
            if (browser.load(is) == true) {
              System.out.println("file loaded successfully.");
            }
          isChanged = false;
        }
        browser.timerEnabled(true);
        // 再生を開始
      } else {
        browser.timerEnabled(false);
      }
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
      // TODO add your handling code here:
    }//GEN-LAST:event_jButton3ActionPerformed

    public void showBrowser(Boolean enable) {
      jPanel4.setVisible(enable);
      pack();
    }

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MotionCompApp().setVisible(true);
            }
        });
    }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton jButton2;
  private javax.swing.JButton jButton3;
  private javax.swing.JButton jButton4;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JPanel jPanel4;
  private javax.swing.JPanel jPanel5;
  private javax.swing.JPanel jPanel6;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JToggleButton jToggleButton1;
  // End of variables declaration//GEN-END:variables

// フレームの左側に部位名を表示するためのパネル．
  class LabelPanel extends JPanel {
   /**
     * ラベルパネルを構築する．このコンストラクタはMotionComp.mcmlから
     * 部位名を読み込むので，このコンストラクタが呼び出される前に，
     * MotionCompApp.mp7mgrが設定されていなくてはならない．
     * @param parts 部位名の配列
     * @see MPEG7Manager
     */
    public LabelPanel(String[] parts) {
      setLayout(null);
      setBackground(Color.white);
      int th = getResourceValue("timeScale.height");
      int lw = getResourceValue("labelPanel.width");
      int sh = getResourceValue("scorePanel.height");
      for (int i = 0; i < parts.length; i++) {
        IndexedJPanel panel = new IndexedJPanel(i);
        panel.setLayout(new BorderLayout());
        JLabel label = new JLabel(parts[i], JLabel.CENTER);
        label.setFont(new Font("Dialog", 0, getResourceValue("labelPanel.fontsize")));
        panel.add("Center", label);
        panel.setBorder(new ThinBevelBorder(BevelBorder.RAISED));
        panel.setBounds(0, th + 3 + sh * i, lw, sh);
        panel.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            int i = ((IndexedJPanel) e.getSource()).getIndex();
            setSelectedScorePanel(getScorePanel(i));
            composepanel.setScorePanelColor(i);
          }
        });
          this.add(panel);
      }
    }
  } // end of class LabelPanel
  class IndexedJPanel extends JPanel {
    private int index;
    IndexedJPanel(int i) {
      super();
      index=i;
    }
    public int getIndex() {
      return index;
    }
  }
} // end of class MotionComp

class InfoPanel extends JPanel {
  private JPanel panel1 = new JPanel();
  private JLabel label1 = new JLabel(MotionCompApp.getResourceString("labelAuthor"));
  private JTextField authorLabel = new JTextField(); //"湯川　崇");
  private JPanel panel2 = new JPanel();
  private JLabel label2 = new JLabel(" " + MotionCompApp.getResourceString("labelTitle"));
  private JTextField titleLabel = new JTextField(); //"みかぐら＋そうらん節");

  private final Cursor TEXT_CURSOR  = new Cursor(Cursor.TEXT_CURSOR);
  private final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

  public InfoPanel() {
    Font defaultfont = new Font("Dialog", 0, MotionCompApp.getResourceValue("infoPanel.fontsize"));
    label1.setFont(defaultfont);
    label2.setFont(defaultfont);
    this.setLayout(new GridLayout());
    this.setBorder(new ThinBevelBorder(BevelBorder.RAISED));
    panel1.setLayout(new GridBagLayout());
    panel2.setLayout(new GridBagLayout());
    panel1.add(label1, new GridBagConstraints2(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    //    authorLabel.disableEvents(AWTEvent.KEY_EVENT_MASK);
    //    titleLabel.disableEvents(AWTEvent.KEY_EVENT_MASK);
    authorLabel.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        authorLabel.requestFocus();
      }
    });
    titleLabel.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        titleLabel.requestFocus();
      }
    });
    authorLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
    authorLabel.setOpaque(true);
    authorLabel.setBackground(Color.white);
    panel1.add(authorLabel, new GridBagConstraints2(1, 0, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
    panel2.add(label2, new GridBagConstraints2(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    titleLabel.setBorder(new BevelBorder(BevelBorder.LOWERED));
    titleLabel.setOpaque(true);
    titleLabel.setBackground(Color.white);
    panel2.add(titleLabel, new GridBagConstraints2(2, 0, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
    this.add(panel1); // authorLabel
    this.add(panel2); // titleLabel
    this.add(new JPanel());
    authorLabel.addMouseListener(new MouseAdapter() {
      public void mouseEntered(MouseEvent e) {
        authorLabel.setCursor(TEXT_CURSOR);
      }
      public void mouseExited(MouseEvent e) {
        authorLabel.setCursor(DEFAULT_CURSOR);
      }
    });
    titleLabel.addMouseListener(new MouseAdapter() {
      public void mouseEntered(MouseEvent e) {
        titleLabel.setCursor(TEXT_CURSOR);
      }
      public void mouseExited(MouseEvent e) {
        titleLabel.setCursor(DEFAULT_CURSOR);
      }
    });
  }
  public final void setAuthor(String author) {
    authorLabel.setText(author);
  }
  public final void setTitle(String title) {
    titleLabel.setText(title);
  }
  public void clear() {
    authorLabel.setText(null);
    titleLabel.setText(null);
  }
  public final String getAuthor() {
    return authorLabel.getText();
  }
  public final String getTitle() {
    return titleLabel.getText();
  }
}
