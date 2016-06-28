package mocomp;

//			  ComposePanel.java
//
//	     Copyright (C) 1998,1999,2000,2009 Takashi Yukawa
//
//		    This java source file conforms
//		GNU GENERAL PUBLIC LICENSE Version 2.
//
// Author:  Takashi Yukawa <yukawalab@gmail.com>
// Created: Sep.19, 1998
// Revised: Jan.30, 2000
//
// Change Log
// Jan.30 2000
//  パッケージを止めた
// Jan.01 1999
//  XYLayoutを使うのを止めて，Layout Managerを使わない(Absolute Positining)
//  で内部のコンポーネントを配置するように変更した．

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

/**
 * スコアパネルや，タイムスケールなどを保持するコンテナクラス．
 * @version 1.00 09/20/98
 * @author Takashi Yukawa
 * @see ScorePanel
 */
public class ComposePanel extends JPanel {

  private Color spbgcol;
  private JPanel panel;

  public ComposePanel() {
    this.setLayout(new BorderLayout());
    JScrollPane scrollPane = new JScrollPane();
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    this.add("North", scrollPane);
    int partlen = MotionCompApp.mp7mgr.getPartSize();
    panel = new JPanel();
    panel.setBackground(Color.white);
    scrollPane.getViewport().add(panel);
    panel.setLayout(null);
    TimeScale timescale = new TimeScale();
    panel.add(timescale);
    int sh = MotionCompApp.getResourceValue("scorePanel.height");
    for (int i = 0; i < partlen; i++) {
      ScorePanel sp = new ScorePanel(i);
      spbgcol = sp.getBackground();
      panel.add(sp);
      sp.setBounds(0, timescale.getHeight() + i * sh, timescale.getWidth(), sh);
    }
    panel.setPreferredSize(new Dimension(timescale.getWidth(), sh * partlen + timescale.getHeight() + 13)); // XXX

    this.registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int numComponent = panel.getComponentCount();
        for (int i = 1; i < numComponent; i++) {
          ScorePanel sp = (ScorePanel) panel.getComponent(i);
          sp.removeSelected();
          sp.validate();
        }
        validate();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
  }

  /**
   * すべての舞踊符を削除する
   * @see MotionComp#removeAllCodePanel
   */
  public void removeAllCodePanel() {
    int numComponent = panel.getComponentCount();
    for (int i = numComponent - 1; i > 0 ; i--) {
      ScorePanel sp = (ScorePanel)panel.getComponent(i);
      sp.removeAll();
      sp.revalidate();
      sp.repaint();
    }
    validate();
  }

  /**
   * このコンポーズパネルが保持しているスコアパネルのリストを返す．
   * @return このコンポーズパネルが保持しているスコアパネル
   */
  public ArrayList<ScorePanel> getScorePanelList() {
    ArrayList<ScorePanel> sl = new ArrayList<>();
    int numComponent = panel.getComponentCount();
    for (int i = 1; i < numComponent ; i++) {
      ScorePanel sp = (ScorePanel)panel.getComponent(i);
      sl.add(sp);
    }
    return sl;
  }

  /**
   * partCodeを持つscorepanelを探して返す
   * @param partCode 探すスコアパネルの部位コード
   * @return 部位コードがpartCodeであるスコアパネルが見つかったら，
   *         そのスコアパネル，見つからなかったらnull
   */
  public ScorePanel getScorePanel(String partcode) {
    ScorePanel sp;
    int numComponent = panel.getComponentCount();
    for (int i = 1; i < numComponent ; i++) {
      sp = (ScorePanel)panel.getComponent(i);
      if (partcode.equals(sp.getPartCode())) {
        return sp;
      }
    }
    return null;
  }

  public final ScorePanel getScorePanel(int index) {
    return (ScorePanel)panel.getComponent(index+1);
  }

  public void setScorePanelColor(int partindex) {
    ScorePanel sp;
    int numComponent = panel.getComponentCount();
    JPanel labelpanel = MotionCompApp.sharedInstance().getLabelPanel();
    for (int i = 1; i < numComponent ; i++) {
      sp = (ScorePanel)panel.getComponent(i);
      if (i == partindex+1) {
        Color c = new Color(135, 206, 235);
        sp.setBackground(c);
        labelpanel.getComponent(i-1).setBackground(c);
      } else {
        sp.setBackground(spbgcol);
        labelpanel.getComponent(i-1).setBackground(spbgcol);
      }
    }
  }

  /**
   * コンポーズパネルの上部に表示される時間の目盛り．時間の単位はフレーム．
   * @see MotionComp#MAX_TIME
   * @see MotionComp#FPS
   * @see MotionComp#PPS
   */
  //  public static final int WIDTH = MotionComp.MAX_TIME * MotionComp.PPS;
  class TimeScale extends JPanel {
    private Font f = new Font("TimesRoman", Font.PLAIN, 9);
    private FontMetrics fm = getFontMetrics(f);

    /**
     * タイムスケールを構築する．
     */
    public TimeScale() {
      this.setBackground(Color.white);
      this.setSize(MotionCompApp.MAX_TIME * MotionCompApp.PPS, 15);
    }
    
    /**
     * タイムスケールを描画する．
     */
    public void paint(Graphics g) {
      int i, j;
      int FPP = MotionCompApp.FPS / MotionCompApp.PPS; // Frames/Pixel
      Dimension d = getSize();
      g.setColor(Color.black);
      g.setFont(f);
      for (i = 0; i <= d.width; i += FPP * 10) { // 10フレーム毎に目盛りを描画
        if (i % (FPP * 100) == 0) {
          j = 0;
          g.drawString(String.valueOf(i / FPP), i + 2, fm.getAscent());
        } else if (i % (FPP * 50) == 0) {
          j = d.height * 1 / 3;
        } else {
          j = d.height * 2 / 3;
        }
        g.drawLine(i, j, i, d.height);
      }
    }
  }

  public static void main(String args[]) {
    JFrame f = new JFrame("ComposePanel Test");
    ComposePanel c = new ComposePanel();
    f.getContentPane().add(c, BorderLayout.CENTER);
    f.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });
    f.setSize(640, 480);
    f.pack();
    f.setVisible(true);
  }
}
