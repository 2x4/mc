//
//			   ScorePanel.java
//
//	       Copyright (C) 1998, 1999 Takashi Yukawa
//
//		    This java source file conforms
//		GNU GENERAL PUBLIC LICENSE Version 2.
//
// Author:  Takashi Yukawa <yukawalab@gmail.com>
// Created: Sep.16, 1998
// Revised: Jan.02, 1999
//
// Change Log
// Jan.02 1999
// * MotionCodeをSS-MM-PPからCC-SS-MM-PPの形式に変更した．
// Jan.01 1999
// * MotionCodePanelをこのパネルに配置するときの位置とMotionCodePanelを
//   移動した後の位置が2ポイントずれていたのを修正した．
// * XYLayoutを使うのを止めて，Layout Managerを使わない(Absolute Positining)
//   で内部のコンポーネントを配置するように変更した．

/*
 *　スコアパネル
 *  五線譜に音符を配置するようにこのパネルの上に動作を表す「MotionCodePanel」を配置する
 */

package mocomp;

import static mocomp.MotionCompApp.mp7mgr;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * モーションコードパネルを貼りつけるためのパネル
 * このパネルの上でクリックすると舞踊符選択ダイアログを開き、その中で選択された
 * 「MotionCodePanel」を貼り付ける
 *
 * @version 1.00 09/16/1998
 * @author Takashi Yukawa
 * @see MotionCodePanel
 */
public class ScorePanel extends JPanel {
    // どの部位に対応するパネルかを表す
    private final int partIndex;

    // モーションコードパネルのベクタ
    // @see MotionCodePanel
    private final ArrayList<MotionCodePanel> mcpList;

    private Point startPoint, endPoint, currentPoint;

    // 最後にクリックした位置の座標．この位置に，編集 -> ペースト
    // した舞踊符パネルが貼りつけられる．
    private transient Point lastClickedPoint;

    // jdk1.2beta4のバグ??対策のフラグ．
    // パネルをクリックしたときに，その下に重なっているパネルにもイベント
    // (確かMouseClicked)が起きるバグを防ぐ．
    private transient boolean mousereleased;

    private final Insets borderInsets;

  // スコアパネルを構築する
    public ScorePanel(int partindex) {
        partIndex = partindex;
        startPoint = endPoint = currentPoint = null;
        mcpList = new ArrayList<>();
        setBorder(new ThinBevelBorder(BevelBorder.RAISED));
        borderInsets = getBorder().getBorderInsets(null);
        setLayout(null);
        enableEvents(AWTEvent.KEY_EVENT_MASK);
        mousereleased = false;
        addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            ComposePanel cp = (ComposePanel)getParent().getParent().getParent().getParent();
            cp.setScorePanelColor(partIndex);

            if (mousereleased && SwingUtilities.isLeftMouseButton(e)) {
                Frame f = MotionCompApp.sharedInstance(); //.getFrame();
                MotionCodeChooser mcc = MotionCompApp.getMotionCodeChooser();
                Dimension fs = f.getSize();
                Dimension ds = mcc.getSize();
                if (ds.height > fs.height)
                    ds.height = fs.height;
                if (ds.width > fs.width)
                    ds.width = fs.width;
                Point p = f.getLocation();
                mcc.setLocation(p.x + (fs.width - ds.width) / 2, p.y + (fs.height - ds.height) / 2);
                mcc.setPart(partIndex);  // どの部位の舞踊符を用意するのか
                // 舞踊符選択ダイアログを開き、舞踊符を選択できるようにする
                mcc.setVisible(true);
                if (mcc.isSelected()) { // OK button was pressed on MCC
                    addNewCodePanel(mcc.getSelectedMotionCode(), e.getX() /* *MotionCompApp.FPS/MotionCompApp.PPS */);
                    MotionCompApp.sharedInstance().setSaveMenuItemEnabled(true);
                    MotionCompApp.sharedInstance().setCutAndCopyEnabled(true);
                    MotionCompApp.isChanged = true;
                }
            }
            lastClickedPoint = e.getPoint();

            mousereleased = false;
        ////////////////////////////////////////
            MotionCompApp.sharedInstance().setCutAndCopyEnabled(getSelectedPanelCounts() > 0);
            MotionCompApp.sharedInstance().setSelectAllEnabled(getPanelCounts() > 0);
        } // mouseClicked()

        @Override
        public void mousePressed(MouseEvent e) {
            ScorePanel.this.requestFocus();
            startPoint = e.getPoint();
            currentPoint = startPoint;
            if (!e.isControlDown()) {
                selectAll(false);
            }
            mousereleased = false;
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            int sx, sy, ex, ey;
            endPoint = me.getPoint();
            if (startPoint.x < endPoint.x) {
                sx = startPoint.x;
                ex = endPoint.x;
            } else {
                sx = endPoint.x;
                ex = startPoint.x;
            }
            if (startPoint.y < endPoint.y) {
                sy = startPoint.y;
                ey = endPoint.y;
            } else {
                sy = endPoint.y;
                ey = startPoint.y;
            }
/*
            if (sy < 0) {
                sy = 0;
            }
            Dimension d = getSize();
            if (ey > d.height) {
                ey = d.height;
            }
*/
            for (int i = sx; i <= ex; i++) {
                Component c = findComponentAt(i, 2);
                if (!(c instanceof MotionCodePanel)) {
                    continue;
                }
                MotionCodePanel mcp = (MotionCodePanel) c;
                MotionCompApp.sharedInstance().setCutAndCopyEnabled(true);
                mcp.markSelect(true);
            }
            startPoint = currentPoint = null;
            repaint();
            MotionCompApp.sharedInstance().setSelectedScorePanel(ScorePanel.this);
            mousereleased = true;
        }
    });
    this.addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        currentPoint = e.getPoint();
        repaint();
      }
    });

//    MotionCompApp.sharedInstance().setSaveMenuItemEnabled(true);
  } // public ScorePanel()

  @Override
  public void removeAll() {
    super.removeAll();
    mcpList.clear();
  }

  // スコアパネルを描画する．startPointに値が設定されていると，
  // startPointとendPointを対角とする矩形を描く．
  @Override
  public void paint(Graphics g) {
    super.paint(g);
    if (startPoint != null && !startPoint.equals(currentPoint)) {
      Dimension d = getSize();
      g.setColor(Color.black);
      int sx, sy, width, height;
      if (startPoint.x < currentPoint.x) {
        sx = startPoint.x;
        width = currentPoint.x - startPoint.x;
      } else {
        sx = currentPoint.x;
        width = startPoint.x - currentPoint.x;
      }
      if (startPoint.y < currentPoint.y) {
        sy = startPoint.y;
        if (currentPoint.y > d.height)
          currentPoint.y = d.height - 2;
        height = currentPoint.y - startPoint.y;
      } else {
        if (currentPoint.y < 0)
          currentPoint.y = 1;
        sy = currentPoint.y;
        height = startPoint.y - currentPoint.y;
      }
      g.drawRect(sx, sy, width, height);
    }
  } // paint

  /**
   * 最後に右クリックされた位置の座標を返す
   * @return 最後に右クリックされた位置の座標
   * @see MotionCompMenu#EditMenu#editPasteMenuItemActionPerformed
   */
  public final Point getLastPoint() {
    return lastClickedPoint;
  }

  // このパネルに含まれるすべてのモーションコードパネルを選択または非選択状態にする．
  // @param b trueのとき選択，falseのとき非選択にする．
  public final void selectAll(boolean b) {
    for (MotionCodePanel mcp : mcpList) {
      mcp.markSelect(b);
    }
  }

  // 選択されているモーションコードパネルのベクタを作成して返す．
  // @return 選択されているモーションコードパネルのリスト
  public ArrayList<MotionCodePanel> getSelectedMotionCodePanel() {
    ArrayList<MotionCodePanel> mcl = new ArrayList<>();
    for (MotionCodePanel mcp : mcpList) {
      if (mcp.isSelected())
        mcl.add(mcp);
    }
    return mcl;
  }

  /**
   * 新しいモーションコードパネルを貼りつける．
   * @param motioncode モーションコード (CC-SS-MM-PPの形式)
   * @param startindex 新しいモーションコードの開始位置
   * @see DDSReader
   */
  public final void addNewCodePanel(String motioncode, int startindex) {
    addCodePanel(motioncode, startindex, -1);
  }

  public final void addNewCodePanel(String motioncode, int startindex, int framelength) {
    addCodePanel(motioncode, startindex, framelength);
  }

  public final void addCodePanel(String motioncode, int startindex, int framelength) {
      MotionCodePanel mcp = new MotionCodePanel(this);
      String ss[] = motioncode.split("-"); // genre-cwho-cwhen-cwhat-cshow-times   1.6.24-0115683884-0892522800-0705896909-0686741873-0001
      String creationid = ss[1] + "-" + ss[2] + "-" + ss[3]; // who-when-what           1492319154-0892522800-0705896909
//      Node creation = mp7mgr.getCreation(creationid);
      String title = mp7mgr.getTitle(creationid);
      String givenname = mp7mgr.getGivenName(creationid);
      String familyname = mp7mgr.getFamilyName(creationid);
      mcp.setMotionCode(motioncode);
      String description = mp7mgr.getDescription(motioncode);
      String segtitle = mp7mgr.getSegmentTitle(motioncode);
      mcp.add("Center", new JLabel(segtitle+"@"+title, SwingConstants.CENTER));
      mcp.setToolTipText(segtitle+"("+description+"),"+title+","+givenname+ " "+familyname);
      mcp.setAbstract(description);
      mcp.setTitle(segtitle);
      if (framelength < 0) {
        String str[] = mp7mgr.getSegmentDuration(motioncode).split("[TN]");
        framelength = Integer.parseInt(str[1]);
      }
      this.add(mcp);
      mcp.setBounds(startindex, borderInsets.top, framelength, this.getHeight() - (borderInsets.top + borderInsets.bottom));
      mcp.setPreferredSize(new Dimension(framelength, this.getHeight() - (borderInsets.top + borderInsets.bottom)));
      this.validate();
      mcpList.add(mcp);
      MotionCompApp.sharedInstance().setSelectAllEnabled(true);
  }

  /**
   * このパネルが担当している部位のコードを返す．
   * このメソッドはMotionCompを介してDDSReaderから呼ばれる．
   * @return このパネルが担当している部位のコード
   * @see DDSReader
   */
  public final String getPartCode() {
    return mp7mgr.getPartCode(partIndex);
  }

  public final int getPartIndex() {
    return partIndex;
  }

  /**
   * 右隣にあるモーションコードパネルの開始フレーム数を返す
   * @param	here 現在のフレーム数
   * @return 	右隣にあるモーションコードパネルの開始フレーム数
   */
  public final int getUpperLimit(final int here) {
    int min = MotionCompApp.MAX_TIME * MotionCompApp.FPS;
    int start;
    for (MotionCodePanel mcp : mcpList) {
      start = mcp.getFrameIndex();
      // hereのすぐ後にあるモーションコードパネルの開始位置を探す
      if (start >= here && min > start)
        min = start;
    }
    return min - 1;
  }

  /**
   * 左隣にあるモーションコードパネルの終了フレーム数を返す
   * @param	here 基準になるモーションコードパネルの開始フレーム数
   * @return	左隣にあるモーションコードパネルの終了フレーム数
   */
  public final int getLowerLimit(final int here) {
    int max = 0;
    int end;
    for (MotionCodePanel mcp : mcpList) {
      end = mcp.getEndFrames();
      // hereの前にあるモーションコードパネルの終了位置を探す
      if (end <= here && max < end)
        max = end;
    }
    return max;
  }

  /**
   * このパネルが保持しているモーションコードのベクタを返す．
   * @return	このパネルが保持しているモーションコードのベクタ
   * @see	ComposePabel#getScoreList
   */
  public ArrayList<MotionCodePanel> getMotionCodeList() {
    ArrayList<MotionCodePanel> mcl = new ArrayList<MotionCodePanel>();
    int numComponent = this.getComponentCount();
    Component[] components = this.getComponents();
    for (int i = 0; i < numComponent; i++) {
      mcl.add((MotionCodePanel) components[i]);
    }
    Collections.sort(mcl);    //      mcl.sort();
    return mcl;
  }

  /*
   * 選択されているモーションコードパネルの数を返す
   */
  public int getSelectedPanelCounts() {
    int ns = 0;
    for (int i = 0; i < mcpList.size(); i++) {
      if (((MotionCodePanel)mcpList.get(i)).isSelected()) {
        ns++;
      }
    }
    return ns;
  }

  /*
   * スコアパネルにあるモーションコードパネルの数を返す
   */
  public final int getPanelCounts() {
    return mcpList.size();
  }

  // 選択されているモーションコードパネルを削除する．
  public final void removeSelected() {

    for (int i = mcpList.size()-1; i>=0; i--) {
      MotionCodePanel mcp = mcpList.get(i);
      if (mcp.isSelected()) {
        mcpList.remove(i);
        remove(mcp);
      }
    }
    repaint();
    MotionCompApp.sharedInstance().setCutAndCopyEnabled(false);
  }

  // このスコアパネルの文字列表現を返す．
  // @return このスコアパネルの文字列表現．
  @Override
  public String toString() {
    String psegs = String.join(",", mp7mgr.getPartSegments(partIndex));
    return (mp7mgr.getPartCode(partIndex) + ": " + psegs);
  }

  // このスコアパネルの舞踊符の最終フレーム数を返す
  public int getMaxFrame() {
    int max = -1;
    for (MotionCodePanel mcp : getMotionCodeList()) {
      if (max < mcp.getEndFrames()) {
        max = mcp.getEndFrames();
      }
    }
    return max;
  }

  /**
   * このパネルが保持しているモーションコードをもとに作成した新しい
   * BVAデータを返す．
   * @return このパネルが保持しているモーションコードをもとに作成した
   *         新しいBVAデータ．
   * @see MotionComp#export
   */
  /*
  public final BioVisionAnim makeNewBVA(String statMessage) {
    BioVisionAnim bva = new BioVisionAnim();
    String[] requiments = mp7mgr.getPartSegments(partIndex);
    ArrayList<MotionCodePanel> mcl = getMotionCodeList(); // このパネルに並べられたモーションコードのリスト
//System.out.println("ScorePanel.makeNewBVA() getMotionCodeList()="+mcl.toString() );
    for (MotionCodePanel mcp : mcl) {
      //      System.out.println("ScorePanel.makeNewBVA.mcp.toString(): "+mcp.toString());
      String motioncode = mcp.getMotionCode();
      Node motion = mp7mgr.getSegment(motioncode);
      String url = mp7mgr.getMediaLocator(motioncode, "BVA");
      statMessage = url + "からデータを読み込みます．";
//      System.err.println(this.getPartCode() + ":" + statMessage + ":" + mcp.toString());
      int startframe = mp7mgr.getStartFrame(motion);
      bva.readData(url, startframe, startframe + mp7mgr.getDuration(motion) - 1, mcp.getFrameIndex(), mcp.getLength(), requiments);
    }
//    System.err.println("ScorePanel.makeNewBVA() => " + bva.toString());
//    System.out.println("ScorePanel.makeNewBVA().bva.size() => " + bva.size());
//    System.out.println(bva.keySet().toString());
    return bva;
  }
*/
  /**
   * このパネルが保持しているモーションコードをもとに作成した新しい
   * BVHデータを返す．
   * @return このパネルが保持しているモーションコードをもとに作成した
   *         新しいBVHデータ．
   * @see MotionComp#export
   */
  /*
  // TODO: ハードコードしているセグメント名の定義をMPEG-7ドキュメントから読み込むように
   public final BioVisionHier makeNewBVH(double[][] framedata, String statMessage) {
    BioVisionHier bvh = new BioVisionHier();
    HashMap<String, String[]> pcode2jntnames = new HashMap<String, String[]>();
    String[] lg_jnts = {"HumanoidRoot", "sacroiliac", "l_hip", "l_knee", "l_ankle", "r_hip", "r_knee", "r_ankle"};
    pcode2jntnames.put("LG", lg_jnts);
    String[] ram_jnts = {"r_shoulder", "r_elbow", "r_wrist"};
    pcode2jntnames.put("RAM", ram_jnts);
    String[] lam_jnts = {"l_shoulder", "l_elbow", "l_wrist"};
    pcode2jntnames.put("LAM", lam_jnts);
    String[] bd_jnts = {"vl5"};
    pcode2jntnames.put("BD", bd_jnts);
    String[] hd_jnts = {"skullbase"};
    pcode2jntnames.put("HD", hd_jnts);
        
    String[] jointnames = pcode2jntnames.get(getPartCode());

    // このパネルに並べられたモーションコードのリストを１つずつ読み込む
    for (MotionCodePanel mcp : getMotionCodeList()) {
      String motioncode = mcp.getMotionCode();
      URL url = null;
      try {
        String urlstr = MotionComp.mcml.getMediaLocator(motioncode).replaceAll("bva", "bvh");
        url = new URL(urlstr);
      } catch (MalformedURLException ex) {
        Logger.getLogger(ScorePanel.class.getName()).log(Level.SEVERE, null, ex);
      }
      statMessage = url.toString() + "からデータを読み込みます．";
      System.err.println(this.getPartCode() + ":" + statMessage + ":" + mcp.toString());
      Node motion = MotionComp.mcml.getSegment(motioncode);
      int startframe = MotionComp.mcml.getStartFrame(motion);

//      System.out.println("startframe=" + startframe+",duration="+ MotionComp.mcml.getDuration(motion)+", "+ mcp.getFrameIndex()+","+mcp.getFrames()+","+ org.apache.commons.lang.StringUtils.join(jointnames, ","));
      bvh.readData(framedata, url, startframe, startframe + MotionComp.mcml.getDuration(motion) - 1, mcp.getFrameIndex(), mcp.getFrames(), jointnames);
    }
    return bvh;
  }
  */
}
