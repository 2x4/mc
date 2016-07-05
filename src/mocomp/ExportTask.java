//			    ExportTask.java
//
//	       Copyright (C) 2009 Takashi Yukawa
//
//		    This java source file conforms
//		GNU GENERAL PUBLIC LICENSE Version 2.
//
// Author:  Takashi Yukawa <yukawalab@gmail.com>
// Created: Aug.30, 2009
//
// Change Log
// Aug.30, 2009  Exportからクラス名を変更
// Jun.02, 1999  MotionCodeTableをMcmlに変更
// Jan.17, 1999
//  doubleで表していた座標や回転をfloatで表すように変更した．
// Dec.14
//  VRML97形式での書き出しを追加
//  ExportBVAからExportに名前を変更
// Nov.19
//  TransMat4d.java を別ファイルにした
//  transform()のアルゴリズムが間違っていたので，正しいものに修正
//                        special thanks to isamu@akeihou-u.ac.jp
// Nov.17
//  Polhemusのクラスライブラリを使わないように修正
// Nov.15
//  ActualTask.ActualTask()のfdsの扱いに関するバグをfix
//  (fds.set(segindex, fe); を追加)
// Nov.14
//  DanceComp.java から独立

package mocomp;

import static mocomp.MotionCompApp.mp7mgr;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import org.w3c.dom.Node;
import vecmath.*;
/**
 *
 * @author yukawa
 */
public class ExportTask extends javax.swing.SwingWorker<Void, Void> {
  public static String note; // ProgressMonitorへのメッセージの受け渡しに使用
  private File file;

  ExportTask(File f) {
    file = f;
  }

  @Override
  protected Void doInBackground() {
    BioVisionAnim newbva = null;
    if (file.getName().endsWith(".bvh") || file.getName().endsWith(".BVH")) {
      exportBVH(file);
    } else { // 拡張子が bva or wrl
      newbva = exportBVA();
    }
    if (file.getName().endsWith(".bva") || file.getName().endsWith(".BVA")) {
      newbva.saveData(file);
    } else if (file.getName().endsWith(".wrl") || file.getName().endsWith(".WRL")) {
      newbva.saveDataAsVRML97(file);
    }
    return null;
  }

  BioVisionHier exportBVH(File savefile) {
    HashMap<String, String[]> pcode2jntnames = new HashMap<>();
    pcode2jntnames.put("LG", new String[]{"HumanoidRoot", "sacroiliac", "l_hip", "l_knee", "l_ankle", "r_hip", "r_knee", "r_ankle"});
    pcode2jntnames.put("RAM", new String[]{"r_shoulder", "r_elbow", "r_wrist"});
    pcode2jntnames.put("LAM", new String[]{"l_shoulder", "l_elbow", "l_wrist"});
    pcode2jntnames.put("BD", new String[]{"vl5"});
    pcode2jntnames.put("HD", new String[]{"skullbase"});

    int i;
    int partslen = mp7mgr.getPartSize();
    int maxframe = -1;
    BioVisionHier bvh = new BioVisionHier();
    note = "必要なデータの準備をしています．";
    for (i = 0; i < partslen; i++) { // 舞踊譜全体の長さを求める
      ScorePanel sp = MotionCompApp.sharedInstance().getScorePanel(i);
      if (maxframe < sp.getMaxFrame()) {
        maxframe = sp.getMaxFrame();
      }
    }
    double[][] framedata = new double[maxframe][51]; // XXX
    for (i = 0; i < partslen; i++) {
      ScorePanel sp = MotionCompApp.sharedInstance().getScorePanel(i);
      String[] jointnames = pcode2jntnames.get(sp.getPartCode());
      int prevLastFrame = 0;
      for (MotionCodePanel mcp : sp.getMotionCodeList()) {
        String motioncode = mcp.getMotionCode();
        URL url = null;
        String urlstr = mp7mgr.getMediaLocator(motioncode, "bvh");
          try {
            url = new URL(urlstr);
          } catch (MalformedURLException ex) {
            Logger.getLogger(ExportTask.class.getName()).log(Level.SEVERE, null, ex);
          }
//          Node motion = mp7mgr.getSegment(motioncode);
          int startframe = mp7mgr.getStartFrame(motioncode);
          String durationtxt[] =mp7mgr.getSegmentDuration(motioncode).split("[TN]");
          int duration = Integer.parseInt(durationtxt[1]);
          bvh.readData(framedata, url, startframe, startframe + duration - 1, mcp.getFrameIndex(), mcp.getLength(), jointnames, prevLastFrame);
          prevLastFrame = mcp.getFrameIndex() + mcp.getLength() - 1;
        }
      }
      // bvhデータ準備完了
      bvh.saveData(savefile, framedata);
      return bvh;
    } // end of exportBVH()

  BioVisionAnim makeNewBVA(ScorePanel sp, String statMessage) {
    BioVisionAnim bva = new BioVisionAnim();
    String[] requiments = mp7mgr.getPartSegments(sp.getPartIndex());
    for (MotionCodePanel mcp : sp.getMotionCodeList()) {// スコアパネルに並べられたモーションコードのリストを順に処理
      String motioncode = mcp.getMotionCode();
//      Node motion = mp7mgr.getSegment(motioncode);
      String url = mp7mgr.getMediaLocator(motioncode, "BVA");
      statMessage = url + "からデータを読み込みます．";
      int startframe = mp7mgr.getStartFrame(motioncode);
      
      String durationtxt[] =mp7mgr.getSegmentDuration(motioncode).split("[TN]");
      int duration = Integer.parseInt(durationtxt[1]);
      bva.readData(url, startframe, startframe + duration - 1, mcp.getFrameIndex(), mcp.getLength(), requiments);
    }
    return bva;
  }
    
  BioVisionAnim exportBVA() {
    int i, j, maxframes = 0;
    int partslen = mp7mgr.getPartSize();
    // 各パートに必要なbvaデータを用意する
    ArrayList<BioVisionAnim> bvaVector = new ArrayList<>();
    for (i = 0; i < partslen; i++) {
      note = "データを準備しています（" + mp7mgr.getPartCode(i) + "）";
      System.out.println(note);
      setProgress((i) * 70 / partslen);
      ScorePanel sp = MotionCompApp.sharedInstance().getScorePanel(i);
      bvaVector.add(makeNewBVA(sp, note)); //bvaVector.add(sp.makeNewBVA(note));
      j = ((BioVisionAnim)bvaVector.get(i)).getMaxFrames();
      if (maxframes < j)
        maxframes = j;
    }
    // bvaデータ準備完了
    // 新しいbvaを記録
    BioVisionAnim newbva = new BioVisionAnim();
    // legsのデータを保存（変換不要）
    // 動作を一つ前の動作の終了位置から始まるようにするために，腰の終了位置を記録しておくためのベクタ
    ArrayList<Point2f> transVec = new ArrayList<>(100);
    transVec.add(0, new Point2f(0.0F, 0.0F));
    Point2f trans = null;
    note = "データを変換しています（" + mp7mgr.getPartCode(3) + "）";
    System.out.println(note);
    BioVisionAnim bva = (BioVisionAnim)bvaVector.get(partslen - 1);
//処理中のパートの対象segment
     String[] al = mp7mgr.getPartSegments(partslen - 1);
    int n = 0;
    for (String segname: al) {
      setProgress(70 + n * 20 / al.length);
      newbva.addNewSegment(segname);
      BioVisionAnim.Segment seg = bva.get(segname);
      float ft = seg.getFrameTime();
      newbva.get(segname).setFrameTime(ft);
      for (i = 0; i < 9; i++) {
        newbva.get(segname).setParamUnit(i, seg.getParamUnit(i));
      }
      // legsのデータはそのまま出力
      BioVisionAnim.FrameData fd = seg.get(0); //getFirstData();
      BioVisionAnim.FrameData fd2;
      boolean endOneSegment = false;
      int transindex = 0;
      for (i = 0; i < seg.size(); i++) {
        if ((fd2 = seg.getFrame(i)) != null) {
          if (endOneSegment) { // 新しい舞踊符の最初のフレーム
            transindex++;
            if (segname.equals("Hip")) { // ||segname.equals("sacrum")||) {
              trans = new Point2f(fd2.trans.x - fd.trans.x, fd2.trans.z - fd.trans.z);
              transVec.add(transindex, trans);
            } else {
              trans = (Point2f)transVec.get(transindex);
            }
            endOneSegment = false;
          }
          if (transindex != 0) {
            fd2.trans.x -= trans.x;
            fd2.trans.z -= trans.y;
          }
          fd = fd2;
        } else {
          endOneSegment = true;
        }
        newbva.get(segname).addFrameData(fd);
      }
      for (i = seg.size(); i < maxframes; i++) {
        newbva.get(segname).addFrameData(fd);
      }
      n++;
    }
// remaining part
//      String linkedSegment[] = { "Body", "Body", "Hip", null };
    String linkedSegment[] = new String[mp7mgr.getPartSize()];
    for (i = 0; i < mp7mgr.getPartSize(); i++) {
      String[] sl = mp7mgr.getPartSegments(i);
      linkedSegment[i] = sl[0];
    }
    for (int partindex = partslen - 2; partindex >= 0; partindex--) {
      note = "データを変換しています（" + mp7mgr.getPartCode(partindex) + "）";
      System.out.println(note);
      setProgress(80 + (((partslen - 2) - partindex) * 20) / (partslen - 2));
      bva = (BioVisionAnim)bvaVector.get(partindex);
      String refsegname = linkedSegment[partindex];
      BioVisionAnim.Segment seg = bva.get(refsegname);
      BioVisionAnim.Segment refseg = newbva.get(refsegname);
      al = mp7mgr.getPartSegments(partindex);
      ArrayList<BioVisionAnim.FrameData> fds = new ArrayList<>();
      for (String cursegname : al) {
        fds.add(bva.get(cursegname).get(0));
        if (cursegname.equals(refsegname) == false) {
          newbva.addNewSegment(cursegname);
          for (i = 0; i < 9; i++)
            newbva.get(cursegname).setParamUnit(i,seg.getParamUnit(i));
          newbva.get(cursegname).setFrameTime(seg.getFrameTime());
        }
      }
      BioVisionAnim.FrameData fd = seg.get(0);
      BioVisionAnim.FrameData reffd = refseg.get(0);
      BioVisionAnim.FrameData fd2, reffd2;
      for (j = 0; j < maxframes; j++) {
        try {
          if ((fd2 = seg.getFrame(j)) != null) {
            fd = fd2;
          }
        } catch (ArrayIndexOutOfBoundsException e) {}
        try {
          if ((reffd2 = refseg.getFrame(j)) != null) {
            reffd = reffd2;
          }
        } catch (ArrayIndexOutOfBoundsException e) {}
        // 座標および回転の変換の準備
        Vector3f po = new Vector3f(reffd.trans);
        Euler ro = new Euler(reffd.rot);
        Vector3f p1 = new Vector3f(fd.trans);
        Euler r1 = new Euler(fd.rot);
          //
        BioVisionAnim.FrameData fe, fe2;
        int segindex = 0;
        for (String cursegname : al) {
          if (cursegname.equals(refsegname) == false) {
            BioVisionAnim.Segment curseg = bva.get(cursegname);
            fe = (BioVisionAnim.FrameData)fds.get(segindex);
            try {
              if ((fe2 = bva.get(cursegname).getFrame(j)) != null) {
                fe = fe2;
                fds.set(segindex, fe);
              }
            } catch (ArrayIndexOutOfBoundsException ex) {}
            Vector3f p2 = new Vector3f(fe.trans);
            Euler r2 = new Euler(fe.rot);
            transform(po, ro, p1, r1, p2, r2);
            BioVisionAnim.FrameData ff = bva. new FrameData();
            ff.trans.set(p2);
            ff.rot.set(r2);
            newbva.get(cursegname).addFrameData(ff);
          }
        } // for each segment
      } // for each frames
    } // for each part
    setProgress(100);
    return newbva;
  }

  /**
   * ベースとなる部位の共有セグメントと接合する部位の共有セグメントの位置，
   * 角度から，接合する部位の他のセグメントの位置，角度を決定する
   * @param p0     ベースとなる部位の共有セグメントの位置
   * @param r0     ベースとなる部位の共有セグメントの角度
   * @param p1     接合する部位の共有セグメントの位置
   * @param r1     接合する部位の共有セグメントの角度
   * @param p2     接合する部位のその他のセグメントの位置
   * @param r2     接合する部位のその他のセグメントの角度
  */
  private final void transform(final Vector3f p0, final Euler r0,
			       final Vector3f p1, final Euler r1,
			       Vector3f p2, Euler r2) {
    // p2をp1中心に回転して新しい位置に移動
    TransMat4f t1 = new TransMat4f();  // t1 = r1^-1
    t1.setRevRotation(r1);
    TransMat4f t2 = new TransMat4f();  // t2 = r0
    t2.setRotation(r0);
    t2.mul(t1);			       // t2 = r0 * r1^-1
    p2.sub(p1);    t2.transform(p2);    p2.add(p0);
    // r2の回転を新しい回転に変換
    TransMat4f t3 = new TransMat4f();
    t3.setRotation(r2);
    t2.mul(t3);
    Euler e = t2.getEuler();
    r2.set(e);
  }
}
