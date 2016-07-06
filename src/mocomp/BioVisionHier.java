//
//			  BioVisionHier.java
//
//	       Copyright (C) 1999-2009 Takashi Yukawa
//
//        This java source file conforms
//     GNU GENERAL PUBLIC LICENSE Version 2.
//
// Author:  Takashi Yukawa <yukawa@fukushima-nct.ac.jp>
// Created: Aug.14, 1999
// Revised: Aug.17, 1999
// Revised: Dec.23, 2008
// Revised: Jan.29, 2009
//
// Change Log:
// Jan.29, 2009
//   舞踊符同士が離れている場合，その間を補間するようにした．
//   HumanoidRootは線形補間，その他のJointの角度はQuaternionによる補間(Slerp)
//   を行う．

package mocomp;

import java.awt.Point;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.*;

public class BioVisionHier {
    static final double DEG2RAD = Math.PI/180.0;
    static final double RAD2DEG = 180.0/Math.PI;
    private int channellen; // joints内のジョイントに含まれるチャネル数の合計
    private Joint root;
    private Motion motion;
    private ArrayList<Joint> joints;
    private HashMap<String, Point> jntpos;
  
    public BioVisionHier() {
        joints = new ArrayList<>();
        motion = new Motion();
// XXX : ファイルから読み込んで設定すること
        jntpos = new HashMap<>();
        jntpos.put("HumanoidRoot", new Point(0, 5));
        jntpos.put("sacroiliac", new Point(6,8));
        jntpos.put("l_hip", new Point(9, 11));
        jntpos.put("l_knee", new Point(12, 14));
        jntpos.put("l_ankle", new Point(15, 17));
        jntpos.put("r_hip", new Point(18, 20));
        jntpos.put("r_knee", new Point(21, 23));
        jntpos.put("r_ankle", new Point(24, 26));
        jntpos.put("vl5", new Point(27, 29));
        jntpos.put("l_shoulder", new Point(30, 32));
        jntpos.put("l_elbow", new Point(33, 35));
        jntpos.put("l_wrist", new Point(36, 38));
        jntpos.put("r_shoulder", new Point(39, 41));
        jntpos.put("r_elbow", new Point(42, 44));
        jntpos.put("r_wrist", new Point(45,47));
        jntpos.put("skullbase", new Point(48,50));
    }

    public void saveData(File savefile, double[][] motiondata) {
        int i, j;
        PrintWriter out = null;
        Hierarchy h = new Hierarchy();
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(savefile)));
            out.print(h.getHier());
            out.println("MOTION");
            out.println("Frames: " + motiondata.length);
            out.println("Frame Time: 0.033333");
            for (i = 0; i < motiondata.length; i++) {
                for (j = 0; j < motiondata[0].length - 1; j++) {
                    out.print(motiondata[i][j] + " ");
                }
                out.println(motiondata[i][j]-1);
            }
        } catch (IOException ex) {
            Logger.getLogger(BioVisionHier.class.getName()).log(Level.SEVERE, null, ex);
        }
        out.close();
    }

    public final void readData(double[][] framedata, URL bvhurl, int srcbeginframe, int srcendframe, int tgtstartindex, int tgtframelength, String[] jointnames, int prevLastFrame) {
        Reader reader = new Reader(framedata, bvhurl, srcbeginframe, srcendframe, tgtstartindex, tgtframelength, prevLastFrame);
        reader.run(jointnames); // データを読み込むジョイント名の配列
    }

    class Reader {
        private int sourceBeginFrame = 0; // もとのbvhファイル中の開始フレーム
        private int sourceEndFrame = -1;  // もとのbvhファイル中の終了フレーム
        private int startIndex = 0;       // 舞踊譜での開始フレーム
        private int frameLength = -1;     // 舞踊符の長さ
        private int prevLastFrame;        // 1つ前の舞踊符の終了フレーム
        private LineNumberReader reader = null;
        private String line = null;
        private double[][] framedata;

        public Reader(double[][] framedata, URL bvhurl, int startframe, int endframe, int startindex, int framelength, int prevLastFrame) {
            this.sourceBeginFrame = startframe;
            this.sourceEndFrame = endframe;
            this.startIndex = startindex;
            this.frameLength = framelength;
            this.framedata = framedata;
            this.prevLastFrame = prevLastFrame;

            HttpURLConnection urlconn = null;
            try {
                System.out.println("url=" + bvhurl.toString());
                urlconn = (HttpURLConnection) bvhurl.openConnection();
                urlconn.setRequestMethod("GET");
                urlconn.connect();
                reader = new LineNumberReader(new BufferedReader(new InputStreamReader(urlconn.getInputStream())));
            } catch (ProtocolException ex) {
                Logger.getLogger(BioVisionHier.class.getName()).log(Level.SEVERE, null, ex);        
            } catch (IOException ex) {
                Logger.getLogger(BioVisionHier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // 指定されたパラメータが示すフレームの範囲にしたがってURLからデータの読み込みを行う
        public void run(String[] joints) {
            try {
                readHierachy();
                readMotionHeader();
                readMotion(joints);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // BioVisionHierachyのHIERACHYからMOTIONの前の行までを読み込む
        private final void readHierachy() throws IOException {
            Joint joint = null;
            Joint j = null;
            channellen = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                line = line.replace('\t', ' ');
                String[] ss = line.split("\\s+");
                if (ss[0].equals("ROOT")) {
                    root = new Joint(ss[1], null);
                    joint = root;
                    joints.add(joint);
                } else if (ss[0].equals("JOINT")) {
                    j = new Joint(ss[1], joint);
                    joint.addChild(j);
                    joint = j;
                    joints.add(joint);
                } else if (ss[0].equals("End")) { // end effector
                    if (ss[1].equals("Site")) {
                        j = new Joint("End Site", joint);
                        joint.addChild(j);
                        joint = j;
                    }
                } else if (ss[0].equals("OFFSET")) {
                    double x = Double.parseDouble(ss[1]);
                    double y = Double.parseDouble(ss[2]);
                    double z = Double.parseDouble(ss[3]);
                    joint.setOffset(x, y, z);
                } else if (ss[0].equals("CHANNELS")) {
                    int nchannels = Integer.parseInt(ss[1]);
                    channellen += nchannels;
                    String[] channelname = new String[nchannels];
                    for (int i = 0; i < nchannels; i++) {
                        channelname[i] = ss[i+2];
                    }
                    joint.setChannelName(channelname);
                } else if (ss[0].equals("MOTION")) {
                    return;		// おしまい
                } else if (ss[0].equals("}")) {
                    joint = joint.getParent();
                }
            } // while
        } // readHierachy()

    // BioVisionHierachyのMOTIONのフレーム数とフレームタイムを読み込む
    // BioVisionHier.motionのframesとframetimeを設定する
    private void readMotionHeader() throws IOException {
      String[] st = line.split("\\s+");
      if (st[0].equals("MOTION")) {
        line = reader.readLine();
        st = line.split("\\s+");
        if (st[0].equals("Frames:")) {
          motion.setFrames(Integer.parseInt(st[1]));
        }
        line = reader.readLine();
        st = line.split("\\s+");
        if (st[0].equals("Frame") && st[1].equals("Time:")) {
          motion.setFrameTime(Double.parseDouble(st[2]));
        }
//        startMotionDataLineNo = reader.getLineNumber() + 1; // 次の行からデータが始まる
      }
    }

    class Euler {
      public double heading;
      public double attitude;
      public double bank;
     public Euler() {
       heading = 0.0;
       attitude = 0.0;
       bank = 0.0;
     } 
      public Euler(Quat4d q1) {
        double test = q1.x * q1.y + q1.z * q1.w;
        if (test > 0.499) { // singularity at north pole
          heading = 2 * Math.atan2(q1.x, q1.w);
          attitude = Math.PI / 2;
          bank = 0;
        } else if (test < -0.499) { // singularity at south pole
          heading = -2 * Math.atan2(q1.x, q1.w);
          attitude = -Math.PI / 2;
          bank = 0;
        } else {
          double sqx = q1.x * q1.x;
          double sqy = q1.y * q1.y;
          double sqz = q1.z * q1.z;
          heading = Math.atan2(2 * q1.y * q1.w - 2 * q1.x * q1.z, 1 - 2 * sqy - 2 * sqz);
          attitude = Math.asin(2 * test);
          bank = Math.atan2(2 * q1.x * q1.w - 2 * q1.y * q1.z, 1 - 2 * sqx - 2 * sqz);
        }
        heading *= RAD2DEG;
        attitude *= RAD2DEG;
        bank *= RAD2DEG;
      }
    }
    public final void euler2qtrn(Quat4d q, double[] e, int k) { //double bank, double heading, double attitude) {
      // Assuming the angles are in radians.
      double heading = e[k + 1] * DEG2RAD;
      double attitude = e[k + 2] * DEG2RAD;
      double bank = e[k] * DEG2RAD;
      double c1 = Math.cos(heading / 2);
      double s1 = Math.sin(heading / 2);
      double c2 = Math.cos(attitude / 2);
      double s2 = Math.sin(attitude / 2);
      double c3 = Math.cos(bank / 2);
      double s3 = Math.sin(bank / 2);
      double c1c2 = c1 * c2;
      double s1s2 = s1 * s2;
      q.w = c1c2 * c3 - s1s2 * s3;
      q.x = c1c2 * s3 + s1s2 * c3;
      q.y = s1 * c2 * c3 + c1 * s2 * s3;
      q.z = c1 * s2 * c3 - s1 * c2 * s3;
    }

    public final void readMotion(String[] joints) throws IOException {
      int i;
      int duration = sourceEndFrame - sourceBeginFrame + 1;
      double[][] indata = new double[duration][51];
      int lineno = 0;

      for (lineno=1; lineno<sourceBeginFrame; lineno++){
        reader.readLine();
      }
      i = 0;
      for (; lineno<=sourceEndFrame; lineno++) {
        line = reader.readLine();
        String[] s = line.split("\\s+");
        for (int j = 0; j < s.length; j++) {
          indata[i][j] = Double.parseDouble(s[j]);
        }
        i++;
      }
      // 必要なデータはindataに読み込み完了

      // ここで補間を実行 ------------------------------------------------------
 /*     // linear interpolation
      if (prevLastFrame + 1 < startIndex) {
        for (i = prevLastFrame + 1; i < startIndex ; i++) {
          for (String jnt : joints) {
            Point p = jntpos.get(jnt);
            for (int k = p.x; k <= p.y; k++) {
              framedata[i][k] = framedata[prevLastFrame][k];
            }
          }
        }
      }
   */
      // slerp
      if (prevLastFrame + 1 < startIndex) {
        Point3d p1, p2, pp;
        p1 = null;
        p2 = null;
        pp = null;
        Quat4d q1 = new Quat4d(); // 前の舞踊符の最終フレームの角度
        Quat4d q2 = new Quat4d(); // この舞踊符の開始フレームの角度
        Quat4d qq = new Quat4d(); // 補間に使用
        for (String jnt : joints) {
          Point p = jntpos.get(jnt);
          for (int k = p.x; k <= p.y; k += 3) {
            if (k == 0) {
              p1 = new Point3d(framedata[prevLastFrame]);
              p2 = new Point3d(indata[0]);
              pp = new Point3d();
            } else if (k > 0) {
              euler2qtrn(q1, framedata[prevLastFrame], k);
              euler2qtrn(q2, indata[0], k);
            }
            for (i = prevLastFrame + 1; i < startIndex; i++) {
              double n = (double) (i - prevLastFrame) / (double) (startIndex - 1 - prevLastFrame);
              if (k == 0) { // HumanoidRootなので位置データ(線形補間する)
                pp.interpolate(p1, p2, n);
                framedata[i][k] = pp.x;
                framedata[i][k + 1] = pp.y;
                framedata[i][k + 2] = pp.z;
              } else { // 回転データ(Slerpにより補間する)
                qq.interpolate(q1, q2, n);
                Euler e = new Euler(qq);
                framedata[i][k] = (float)e.bank;
                framedata[i][k + 1] = (float)e.heading;
                framedata[i][k + 2] = (float)e.attitude;
              }
            }
          }
        }
      }
      // 補間終了 --------------------------------------------------------------
      for (i = startIndex; i < startIndex + frameLength; i++) {
        int pos = (int) (((double) duration / (double) frameLength) * (double) (i - startIndex));
        for (String jnt : joints) {
          Point p = jntpos.get(jnt);
          for (int k = p.x; k <= p.y; k++) {
            framedata[i][k] = indata[pos][k];
          }
        }
      }
    } // readMotion()

    public final void readMotion2(String[] joints) throws IOException {
      int duration = sourceEndFrame - sourceBeginFrame + 1;
      double[][] indata = new double[duration][51];
      int lineno = 0;
      for (lineno=1; lineno<sourceBeginFrame; lineno++){
        reader.readLine();
      }
      int i = 0;
      for (; lineno<=sourceEndFrame; lineno++) {
        line = reader.readLine();
        String[] s = line.split("\\s+");
        for (int j = 0; j < s.length; j++) {
          indata[i][j] = Double.parseDouble(s[j]);
        }
        i++;
      }
      // 必要なデータはindataに読み込み完了
      
      for (i = startIndex; i < startIndex + frameLength; i++) {
        int pos = (int) (((double) duration / (double) frameLength) * (double) (i - startIndex));
        for (String jnt : joints) {
          Point p = jntpos.get(jnt);
          for (int k = p.x; k <= p.y; k++) {
            framedata[i][k] = indata[pos][k];
          }
        }
      }
    } // readMotion()
  } // class Reader

  public void printChildren() {
    root.printChildren();
  }

  class Joint {
    private String name;
    private double[] offset;
    private String[] channelname;
    private Joint parent;
    private ArrayList<Joint> children;
    private ArrayList<Double[]> frame; // motion
    int XpositionIndex, YpositionIndex, ZpositionIndex;
    int XrotationIndex, YrotationIndex, ZrotationIndex;

    public Joint() {
      offset = new double[3];
      setOffset(0.0, 0.0, 0.0);
      XpositionIndex = YpositionIndex = ZpositionIndex = -1;
      XrotationIndex = YrotationIndex = ZrotationIndex = -1;
      frame = new ArrayList<>();
      children = new ArrayList<>();
    }

    public Joint(String name, Joint parent) {
      this();
      setName(name);
      setParent(parent);
    }
    public final void setParent(Joint parent) {  this.parent = parent; }
    public final Joint getParent() { return parent;  }
    
    public final void setOffset(double x, double y, double z) {
      offset[0] = x;
      offset[1] = y;
      offset[2] = z;
    }

    public final void setName(String name) {  this.name = name;   }
    public final String getName() {  return name; }

    public final int getChannelLen() { return channelname.length; }
    public final void addChild(Joint joint) {  children.add(joint); }

    private final void setChannelName(String[] names) throws InvalidChannelNameException{
      channelname = names;
      for (int i = 0; i < getChannelLen(); i++) {
        if (channelname[i].equals("Xposition")) {
          XpositionIndex = i;
        } else if (channelname[i].equals("Yposition")) {
          YpositionIndex = i;
        } else if (channelname[i].equals("Zposition")) {
          ZpositionIndex = i;
        } else if (channelname[i].equals("Zrotation")) {
          ZrotationIndex = i;
        } else if (channelname[i].equals("Xrotation")) {
          XrotationIndex = i;
        } else if (channelname[i].equals("Yrotation")) {
          YrotationIndex = i;
        } else {
          throw new InvalidChannelNameException(channelname[i]);
        }
      }
    }

    public final void printChildren() {
      if (getParent() == null) {
        System.out.println("HIERARCHY\nROOT " + getName() + "\n{");
      } else {
        System.out.println((getName().equals("End Site") ? getName() : "JOINT " + getName()) + "\n{");
      }
      System.out.println("OFFSET\t" + offset[0] + "\t" + offset[1] + "\t" + offset[2]);
      if (channellen > 0) {
        System.out.print("CHANNELS " + channellen);
        for (int i = 0; i < channellen; i++) {
          System.out.print(" " + channelname[i]);
        }
        System.out.print("\n");
      }
      for (Joint j : children) {
        j.printChildren();
      }
      System.out.println("}");
    }
  } // end of class Site
  
  class Motion {
    int frames;
    double frametime;

    public Motion() {
      //      frame =  new Vector();
    }

    public Motion(int frames, double frametime) {
      this();
      this.frames = frames;
      this.frametime = frametime;
    }

    public final void setFrames(int frames) {  this.frames = frames; }
    public final int getFrames() {  return this.frames;  }
    public final void setFrameTime(double frametime) { this.frametime = frametime;  }
    public final double getFrameTime() {  return this.frametime;  }
  } // end of class Motion

  public static void main(String argv[]) {
    BioVisionHier bvh = new BioVisionHier();

    URL url = null;
    try {
      url = new URL("http://127.0.0.1/cgi-bin/msv.pl?nm=akita-ondo&amp;type=bvh");
    } catch (MalformedURLException ex) {
      Logger.getLogger(BioVisionAnim.class.getName()).log(Level.SEVERE, null, ex);
    }

    String[] acceptlist = {"l_upperarm", "l_forearm", "l_hand"};
//    bvh.readData(url, 0, 100, 20, 80, acceptlist);
//    File newfile = new File("BioVisionHier_test.bvh");
//    bvh.saveData(newfile);
  }

  class Hierarchy {
    public String getHier() {
      return "HIERARCHY\nROOT HumanoidRoot\n{\n" +
              "  OFFSET 0.0000 0.0000 0.0000\n" +
"  CHANNELS 6 Xposition Yposition Zposition Zrotation Xrotation Yrotation\n"+
"  JOINT sacroiliac\n  {\n    OFFSET 0.0000 9.0900 -2.6100\n"+
"    CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"    JOINT l_hip\n    {\n      OFFSET 9.6100 -0.2500 -0.1700\n"+
"      CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"      JOINT l_knee\n      {\n        OFFSET 0.7900 -42.5700 3.0900\n"+
"        CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"        JOINT l_ankle\n        {\n          OFFSET 0.6100 -42.1100 -10.4400\n"+
"          CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"          End Site\n          {\n"+
"            OFFSET 2.5300 -6.4000 22.1200\n          }\n        }\n"+
"      }\n    }\n"+"" +
"    JOINT r_hip\n    {\n      OFFSET -9.5000 0.2200 0.1300\n"+
"      CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"      JOINT r_knee\n      {\n        OFFSET 0.8300 -42.5800 2.8900\n"+
"        CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"        JOINT r_ankle\n        {\n"+
"          OFFSET 0.6600 -42.0100 -10.8400\n"+
"          CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"          End Site\n          {\n"+
"            OFFSET -2.4200 -9.3900 22.1600\n"+
"          }\n        }\n      }\n    }\n  }\n"+
"  JOINT vl5\n  {\n    OFFSET 0.2800 14.1900 -7.9200\n"+
"    CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"    JOINT l_shoulder\n    {\n      OFFSET 20.0100 38.0800 3.8900\n"+
"      CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"      JOINT l_elbow\n      {\n        OFFSET -0.1500 -30.1900 -2.9500\n"+
"        CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"        JOINT l_wrist\n        {\n          OFFSET -0.3000 -26.9400 0.9900\n"+
"          CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"          End Site\n          {\n"+
"            OFFSET 0.9600 -19.3200 0.9200\n          }\n        }\n"+
"      }\n    }\n"+
"    JOINT r_shoulder\n    {\n      OFFSET -19.3500 38.3900 4.5100\n"+
"      CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"      JOINT r_elbow\n      {\n        OFFSET -0.4200 -30.1900 -2.9500\n"+
"        CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"        JOINT r_wrist\n        {\n          OFFSET -0.1000 -26.9400 0.9900\n"+
"          CHANNELS 3 Zrotation Xrotation Yrotation\n"+
"          End Site\n          {\n"+
"            OFFSET -0.1000 -19.3600 0.9400\n          }\n"+
"        }\n      }\n    }\n"+
"    JOINT skullbase\n    {\n      OFFSET 0.1600 56.4100 10.1200\n"+
"      CHANNELS 3 Zrotation Xrotation Yrotation\n      End Site\n      {\n"+
"        OFFSET 0.0600 12.9500 -1.8100\n      }\n    }\n  }\n}\n";
    }
  }
} // end of class BioVisionHier

class InvalidChannelNameException extends IllegalArgumentException {
  public InvalidChannelNameException() {
    super();
  }
  public InvalidChannelNameException(String s) {
    super(s);
  }
}

