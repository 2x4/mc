//
//			 MotionCodePanel.java
//
//	       Copyright (C) 1998-2009 Takashi Yukawa
//
//		    This java source file conforms
//		GNU GENERAL PUBLIC LICENSE Version 2.
//
// Author:  Takashi Yukawa <yukawa@nau.ac.jp>
// Created: Sep 16, 1998
// Revised: Feb.07, 1999
//
// Change Log
// Feb.07, 1999
// *パネル上にマウスカーソルを位置したときとボタンを押した状態の，カーソルアイコンを
//  変更した．
// Jan.01 1999
// *このパネルを伸縮できるようにした．
// *このパネルをScorePanelに配置するときの位置と移動した後の位置が2ポイント
//  ずれていたのを修正した．
// *XYLayoutを使うのを止めて，Layout Managerを使わない(Absolute Positining)
//  で内部のコンポーネントを配置するように変更した．

package mocomp;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * The panel expresses one motion-code on the ScorePanel.
 * @version 1.00 09/16/1998
 * @author Takashi Yukawa
 */
public class MotionCodePanel extends JPanel implements Comparable<MotionCodePanel> {
  // height of this panel. calculated from ScorePanel.height
  public final int HEIGHT;

  // ToolTip for showing starting frame of motion-code
  private transient ToolTipWindow ttw;

  // マウスが押されたときのこのパネルの座標系でのx座標
  private transient int mx;

  private transient int iniWidth;
  private transient int iniEnds;

  // 左に移動することができる限度をスコアパネルの座標で表現したもの
  private transient int lowerLimit;
  // 右に移動することができる限度をスコアパネルの座標で表現したもの
  private transient int upperLimit;

  // このパネルのスコアパネル内でのロケーション
  private transient Point sp;

  // このパネルが選択されているかどうかを表すフラグ
  private transient boolean selected = false;

  // バグ対策のフラグ
  private transient boolean mousereleased;

  private boolean resizableAtWest;
  private boolean resizableAtEast;

  // このパネルが表すモーションコード (cc-ss-mm-pp)
  private String motioncode;
  private String title;
  private String mabstract;
  private String timepoint;
  private String duration;
  public void setTitle(String str) {
    title = str;
  }
  public String getTitle() {
    return title;
  }
  public void setAbstract(String abs) {
    mabstract = abs;
  }
  public String getAbstract() {
    return mabstract;
  }
  public void setTimePoint(String tp) {
    timepoint = tp;
  }
  public String getTimePoint() {
    return timepoint;
  }
  public void setDuration(String dur) {
    duration = dur;
  }
  public String getDuration() {
    return duration;
  }
  // マウスカーソル
  private final byte[] openhanddata = {
       71,   73,   70,   56,   57,   97,   32,    0,   32,    0,
      -77,    0,    0,    0,    0,    0,  -64,  -64,  -64,   -1,
       -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
       -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
       -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
       -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,
       -1,   33,   -7,    4,    1,    0,    0,    1,    0,   44,
        0,    0,    0,    0,   32,    0,   32,    0,    0,    4,
       88,   48,  -56,   25,    0,  -96,   56,  107,  106,  -85,
       16,  -42,  -75, -115,  -36,  103, -103,   38,  -87,    2,
       38,   11,  -90,  -94,  -86,  -71,  116,   11,  -54,   51,
       -6,  -18,   49,   94,   93,  -82, -113, -112,  -26, -109,
      -48, -126,  -62,  100,   79,   22,   68,   38,  111,   69,
      -29,  115,   10, -115,  122,  -88,   67,  -85,   20,   91,
      -43,   58, -107,  -38,  -46,  116,   25,   -2, -110,  -67,
      -29,  -80,  122,  -51,  110,  -69,  -33,  -16,  -72,  124,
       78,  -81,  -37,  -17,   -8,  -68,   94,   31,    1,    0,
       59
  };
  private final byte[] closehanddata = {
      71,   73,   70,   56,   57,   97,   32,    0,   32,    0,
     -77,    0,    0,    0,    0,    0, -128,    0,    0,    0,
    -128,    0, -128, -128,    0,    0,    0, -128, -128,    0,
    -128,    0, -128, -128,  -64,  -64,  -64, -128, -128, -128,
      -1,    0,    0,    0,   -1,    0,   -1,   -1,    0,    0,
       0,   -1,   -1,    0,   -1,    0,   -1,   -1,   -1,   -1,
      -1,   33,   -7,    4,    1,    0,    0,    7,    0,   44,
       0,    0,    0,    0,   32,    0,   32,    0,    0,    4,
      74,  -16,  -56,   73,  -85,  -67,   56,  -21,  -51,  -69,
      -1,   28,    0,   28,   34,   57, -126,   22,  -16,   60,
      42,  -69, -106,  -24,  -44,  -82,  -76,  123,  -58,  100,
     -83,  -33,  -88,  -88,  -41,  -68,  -49,  -20,   -9,  -62,
      13, -119,   65,  -49,  113, -121, -109,   44,  105,   73,
     -48,   51,  -38,   -5,   81,  -91,  -42,  -90,  118,  -53,
     -19,  122,  -65,  -32,  -80,  120,   76,   46, -101,  -49,
     -24,  -76,   58,   22,    1,    0,   59
  };
  private final Toolkit toolkit = Toolkit.getDefaultToolkit();
  private final ImageIcon openhandicon = new ImageIcon(openhanddata, "OPEN_HAND");
  private final ImageIcon closehandicon = new ImageIcon(closehanddata, "CLOSE_HAND");
  private final Cursor OPENHAND_CURSOR = toolkit.createCustomCursor(openhandicon.getImage(), new Point(8, 8), "OPEN_HAND");
  private final Cursor CLOSEHAND_CURSOR = toolkit.createCustomCursor(closehandicon.getImage(), new Point(8, 8), "CLOSE_HAND");
  private final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
  private final Cursor E_RESIZE_CURSOR = new Cursor(Cursor.E_RESIZE_CURSOR);
  private final Cursor W_RESIZE_CURSOR = new Cursor(Cursor.W_RESIZE_CURSOR);

  private final int MAXFRAMES = MotionCompApp.MAX_TIME * MotionCompApp.FPS;
//  private ScorePanel scorepanel;
  private Insets scorePanelBorderInsets;

  /**
   * scorepanelを親とするモーションコードパネルを構築する．
   * @param scorepanel 親になるスコアパネル
   * @see ScorePanel
   */
  public MotionCodePanel(final ScorePanel scorepanel) {
    scorePanelBorderInsets = scorepanel.getBorder().getBorderInsets(null);
    HEIGHT = scorepanel.getHeight() - (scorePanelBorderInsets.top + scorePanelBorderInsets.bottom);
    try {
      this.setLayout(new BorderLayout());
      this.setBorder(BorderFactory.createEtchedBorder());
      this.setBackground(Color.orange);
      this.mousereleased = false;
      this.resizableAtWest = false;
      this.resizableAtEast = false;
      this.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          if (SwingUtilities.isLeftMouseButton(e)) {
            Frame f = MotionCompApp.sharedInstance(); //.getFrame();
            Point fp = f.getLocation();
            ttw = new ToolTipWindow(f);
            ttw.setLocation(fp.x + 120, fp.y + 8);
            mx = e.getX();
            if (mx >= 0 && mx < 3) {
              resizableAtWest = true;
              ttw.setTipText(getLocation().x);
            } else if (mx > getWidth() - 4 && mx < getWidth()) {
              resizableAtEast = true;
              ttw.setTipText(getLocation().x + getSize().width - 1);
            } else {
              resizableAtWest = resizableAtEast = false;
              setCursor(CLOSEHAND_CURSOR);
              ttw.setTipText(getLocation().x);
            }
            sp = getLocation();
            iniWidth = getSize().width;
            iniEnds = sp.x + iniWidth;
            lowerLimit = scorepanel.getLowerLimit(sp.x);
            upperLimit = scorepanel.getUpperLimit(sp.x + getWidth());
            ttw.setVisible(true);
          }
          // ここでScorePanelに色を変えるように指示を出す。
          setScorePanelColor(scorepanel.getPartIndex());
          mousereleased = false;
        } // mousePressed()

        public void mouseReleased(MouseEvent e) {
          if (SwingUtilities.isLeftMouseButton(e)) {
            setCursor(OPENHAND_CURSOR);
            if (!resizableAtEast && !resizableAtWest) {
              int sx2 = getLocation().x;
              int nx = sx2 + e.getX() - mx;
              if (nx < lowerLimit && !e.isControlDown() || nx < 0) {
                nx = lowerLimit;
              } else if (nx + getWidth() > upperLimit && !e.isControlDown() || nx + getWidth() > MAXFRAMES) {
                nx = upperLimit - getWidth() + 1;
              }
              setBounds(nx, scorePanelBorderInsets.top, getWidth(), HEIGHT);
              validate();
              MotionCompApp.isChanged = true;
            }
          }
          ttw.dispose();
          mousereleased = true;
          resizableAtEast = resizableAtWest = false;
        } // mousereleased()

        public void mouseClicked(MouseEvent e) {
          if (mousereleased) {
            if (!e.isControlDown()) {
              scorepanel.selectAll(false);
            }
            markSelect(!selected);
          }
          mousereleased = false;
          MotionCompApp.sharedInstance().setCutAndCopyEnabled(true);
          // ここでScorePanelに色を変えるように指示を出す。
          setScorePanelColor(scorepanel.getPartIndex());

        } // mouseClicked()

        public void mouseEntered(MouseEvent e) {
          int pos = e.getX();
          if (pos >= 0 && pos < 3) {
            setCursor(W_RESIZE_CURSOR);
          } else if (pos > getWidth() - 4 && pos < getWidth()) {
            setCursor(E_RESIZE_CURSOR);
          } else {
            if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
              setCursor(OPENHAND_CURSOR);
            } else {
              setCursor(CLOSEHAND_CURSOR);
            }
          }
        } // mouseEntered()

        public void mouseExited(MouseEvent e) {
          setCursor(DEFAULT_CURSOR);
        }
      });

      this.addMouseMotionListener(new MouseMotionAdapter() {
        public void mouseDragged(MouseEvent e) {
          if (SwingUtilities.isLeftMouseButton(e)) {
            if (resizableAtWest) { // left side
              int nx = getLocation().x + e.getX() - mx;
              if (nx < lowerLimit && !e.isControlDown()/* || nx < 0 */) {
                nx = lowerLimit;
              }
              int width = iniEnds - nx;
              if (width > 0) {
                setBounds(nx, scorePanelBorderInsets.top, width, HEIGHT);
                setPreferredSize(new Dimension(width, HEIGHT));
                validate();
                scorepanel.validate();
                ttw.setTipText(getLocation().x);
              }
            } else if (resizableAtEast) { // right side
              int newwidth = iniWidth + e.getX() - mx;
              int panelend = getLocation().x + newwidth - 1;
              if (newwidth > 0 && !(panelend > upperLimit/* ||panelend > MAXFRAMES */)) {
                setSize(newwidth, HEIGHT);
                setPreferredSize(new Dimension(newwidth, HEIGHT));
                validate();
                ttw.setTipText(panelend);
              }
            } else {
              int nx = getLocation().x + e.getX() - mx;
              if (nx < lowerLimit && !e.isControlDown() || nx < 0) {
                nx = lowerLimit;
              } else if (nx + getWidth() > upperLimit && !e.isControlDown() ||
                      nx + getWidth() > MAXFRAMES) {
                nx = upperLimit - getWidth() + 1;
              }
              setLocation(nx, scorePanelBorderInsets.top);
              validate();
              ttw.setTipText(getLocation().x);
            }
            MotionCompApp.isChanged = true;
          } // isLeftMouseButton()
        } // mouseDragged()

        public void mouseMoved(MouseEvent e) {
          // just change mouse cursor
          int pos = e.getX();
          if (pos >= 0 && pos < 3) {
            setCursor(W_RESIZE_CURSOR);
          } else if (pos > getWidth() - 4 && pos < getWidth()) {
            setCursor(E_RESIZE_CURSOR);
          } else {
            setCursor(OPENHAND_CURSOR);
          }
        } // mouseMoved()
      }); // addMouseMotionListener()
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void setScorePanelColor(int index) {
    for (Container p = getParent(); p != null; p = p.getParent()) {
      if (p instanceof ComposePanel) {
        ((ComposePanel) p).setScorePanelColor(index);
        return;
      }
    }
  }

  // このパネルが表すモーションコードの終了フレーム数を返す．
  // @return このパネルが表すモーションコードの終了フレーム数
  public final int getEndFrames() {
    return (getLocation().x + getWidth()); /* * MotionComp.FPP */
  }

  // XXX : スケールを考慮する必要あり
  public final int getLength() {
    return getWidth();
  }

  // このパネルが表すモーションコードを返す．
  // @return このパネルが表すモーションコード
  public final String getMotionCode() {
    return motioncode;
    //return ((JLabel)getComponent(0)).getText();
  }

  // このモーションコードパネルの文字列表現を返す．
  // @return このモーションコードパネルの文字列表現
  public final String toString() {
    return (getFrameIndex() + ":" + getSize().width + ":" + getMotionCode());
  }

  // このモーションコードが何フレーム目から始まるかを返す．
  // @return このモーションコードの開始フレーム
  // @see ScorePanel#setupBVA
  public final int getFrameIndex() {
    return getLocation().x /* * MotionComp.FPS / MotionComp.PPS */ ;
  }
  
  // このモーションコードパネルが選択されているかどうかを返す．
  // @return 選択されているならtrue，選択されていないならfalse
  public final boolean isSelected() {
    return selected;
  }

  // このモーションコードパネルを選択または非選択する．
  // @param select trueなら選択，falseなら非選択にする．
  public final void markSelect(boolean select) {
    if (select) {
      selected = true;
      setBackground(Color.blue);
    } else {
      selected = false;
      setBackground(Color.orange);
    }
    repaint();
  }

  public int compareTo(MotionCodePanel o) {
    return (getFrameIndex()-o.getFrameIndex());
  }

  public void setMotionCode(String code) {
    motioncode = code;
  }

  // モーションコードパネルの移動中に開始フレーム数や終了フレーム数を
  // 表示するための枠なしウィンドウ．
  class ToolTipWindow extends Window {
    private JPanel panel = new JPanel();
    private JLabel tipLabel = new JLabel();
    ToolTipWindow(Frame f) {
      super(f);
      try {
        this.setBackground(Color.white);
        this.setSize(40, 12);
        this.setLayout(new BorderLayout());
        tipLabel.setBackground(Color.white);
        panel.setLayout(new BorderLayout());
        panel.add("Center", tipLabel);
        this.add(panel);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    private final void setTipText(int t) {
      tipLabel.setText(Integer.toString(t));
    }
  } // end of class ToolTipWindow


  public static void main(String args[]) {
    JFrame f = new JFrame("MotionCodePanel Test");
    MotionCodePanel m = new MotionCodePanel(null);
    f.getContentPane().add(m, BorderLayout.CENTER);
    f.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });
    f.setSize(640, 480);
    f.setVisible(true);
  }

} // class MotionCodePanel
