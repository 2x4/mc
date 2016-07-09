//
// BioVisionAnim.java
//
// Copyright (C) 1998-2015 Takashi Yukawa
// This java source file confirms the MIT license
// https://github.com/2x4/mc/blob/master/LICENSE
//
package mocomp;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import vecmath.Euler;
import vecmath.Vector3f;

/**
 * BioVisionAnimのデータを保持するためのクラス
 *
 * @version	9/4/2015
 * @author	Takashi Yukawa
 */
public class BioVisionAnim extends HashMap<String, BioVisionAnim.Segment> {

    public static final HashMap<String, String> rsegname;

    static {
        rsegname = new HashMap<>();
        rsegname.put("Hip", "sacrum");
        rsegname.put("Body", "l5");
        rsegname.put("L-UpperArm", "l_upperarm");
        rsegname.put("L-ForeArm", "l_forearm");
        rsegname.put("L-Hand", "l_hand");
        rsegname.put("R-UpperArm", "r_upperarm");
        rsegname.put("R-ForeArm", "r_forearm");
        rsegname.put("R-Hand", "r_hand");
        rsegname.put("Head", "skull");
        rsegname.put("R-Thigh", "r_thigh");
        rsegname.put("R-Shin", "r_calf");
        rsegname.put("R-Foot", "r_hindfoot");
        rsegname.put("L-Thigh", "l_thigh");
        rsegname.put("L-Shin", "l_calf");
        rsegname.put("L-Foot", "l_hindfoot");
    }

    public BioVisionAnim() {

    }

    /**
     * ソースモーションファイルbvafileのstartframeからendframeまでのaccept-
     * listに含まれるセグメントのフレームデータを，startindexから始まる 位置にframelengthの長さに納まるように読み込む．
     *
     * @param bvafile	ソースモーションデータファイル
     * @param startframe	元データの開始フレーム（0から数える）
     * @param endframe	元データの終了フレーム
     * @param startindex	読み込み開始フレーム
     * @param framelength	読み込むフレームをこの長さに納める
     * @param requiments	読み込むセグメント名の配列
     */
    public final void readData(String bvafile, int startframe, int endframe, int startindex, int framelength, String[] requiments) {

        Reader reader = new Reader(bvafile, startframe, endframe, startindex, framelength, requiments);
        reader.run();

    }

    /**
     * 新しいセグメントを追加する． すでにnameというセグメントが存在するときは何もしない
     *
     * @param name	追加するセグメント名
     */
    public Segment addNewSegment(String name) {
        if (!containsKey(name)) {
//      System.out.println("BioVisionAnim.addNewSegment(\""+name+"\")");
            Segment s = new Segment(name);
            put(name, s);
            return s;
        } else {
            return get(name);
        }
    }

    /**
     * このBVAが持つセグメントのフレーム数のうち，最大のものを返す
     */
    public final int getMaxFrames() {
        int max = 0;
        for (Segment segment : values()) {
//      System.out.println("  " + segment.getDataSize());
            if (max < segment.size()) {
                max = segment.size();
            }

        }
//    System.out.println("BioVIsionAnim.getMaxFrames()=" + max);
        return max;
    }

    /**
     * このBioVisionAnimが持つデータをファイルに保存する．
     *
     * @param savefile データを保存するファイル名
     */
    public void saveData(File savefile) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(savefile)));
            for (BioVisionAnim.Segment segm : values()) {
                out.println("Segment: " + segm.getName());
                out.println("Frames: " + segm.size());
                out.println("Frame Time: " + segm.getFrameTime());
                out.println("XTRAN   YTRAN   ZTRAN   XROT    YROT    ZROT    XSCALE  YSCALE  ZSCALE");
                out.println(String.join("\t", segm.getParamUnits()));
                for (FrameData fd : segm) {   // XXX:要検証
                    out.println(fd.toString());
                }
            }
        } catch (IOException e) {
            System.err.println("データの書き出し中に例外が発生しました.");
        }
        out.close();
    }

    /**
     * このBioVisionAnimが持つデータをVRML97形式でファイルに保存する．
     *
     * @param savefile データを保存するファイル名
     */
    public void saveDataAsVRML97(File savefile) {
        StandardModel model = new StandardModel();
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(savefile)));
            out.println("#VRML V2.0 utf8\n");
            out.println("WorldInfo {");
            out.println("  info \"MotionComp: MotionComposer V2.0");
            out.println("  Programing by Takashi Yukawa(yukawa@fukushima-nct.ac.jp)");
            out.println("  Copyright(c) 1998-2015 Takashi YUKAWA\"");
            out.println("}\n");
            out.println("NavigationInfo {");
            out.println("  avatarSize\t1.0");
            out.println("  headlight\tTRUE");
            out.println("  speed\t1.0");
            out.println("  type [\"WALK\",\"EXAMINE\"]");
            out.println("}\n");
            out.println("DEF Cameras Group {");
            out.println("children [");
            out.println("  DEF cam_int1 Viewpoint {");
            out.println("    position\t7.524 42.06 109.2");
            out.println("    orientation\t1 0 0 6.184");
            out.println("    fieldOfView\t0.725");
            out.println("    description\t\"cam_int1\"");
            out.println("  }");
            out.println("]");
            out.println("}\n");
            out.println("DEF BVA2SOFT__work_rundir_d4 Transform {");
            out.println("  children [");

            for (String name : keySet()) {
                Segment segm = get(name);
                String vrmlname = rsegname.get(name);
                out.println("    DEF " + vrmlname + " Transform {");
                out.println("      rotation " + model.getRotation(vrmlname));
                if (vrmlname.equals("sacrum")) {
                    out.println("    scale 1 1 1");
                }
                out.println("      translation " + model.getTranslation(vrmlname));
                out.println("      children [");
                out.println("        DEF O_" + vrmlname + " OrientationInterpolator {");
                out.println("          key [");
                for (int i = 0; i < segm.size(); i++) {
                    out.printf("%4.4f, ", 1.0 / segm.size() * i);
                }
                out.println("\n          ]");
                out.println("	    keyValue [");

                for (FrameData fd : segm) {

                    float sy = (float) Math.sin(fd.rot.getYawRad() * 0.5);
                    float sp = (float) Math.sin(fd.rot.getPitchRad() * 0.5);
                    float sr = (float) Math.sin(fd.rot.getRollRad() * 0.5);
                    float cy = (float) Math.cos(fd.rot.getYawRad() * 0.5);
                    float cp = (float) Math.cos(fd.rot.getPitchRad() * 0.5);
                    float cr = (float) Math.cos(fd.rot.getRollRad() * 0.5);

                    float pw = cy * cp * cr + sy * sp * sr;
                    float px = cy * cp * sr - sy * sp * cr;
                    float py = cy * sp * cr + sy * cp * sr;
                    float pz = sy * cp * cr - cy * sp * sr;

                    float n = (float) Math.sqrt(px * px + py * py + pz * pz);
                    float t = (float) Math.acos(pw * pw - px * px - py * py - pz * pz);

                    out.printf("%4.4f %4.4f %4.4f %4.4f, ", -px / n, -py / n, -pz / n, 2.0F * (float) Math.PI - t);

                }
                out.println("\n	    ]");
                out.println("	}");
                out.println("	DEF P_" + vrmlname + " PositionInterpolator {");
                out.println("	    key [");
                for (int i = 0; i < segm.size(); i++) {
                    out.print(String.format("%4.4f, ", 1.0 / segm.size() * i));
                }
                out.println("\n	    ]");
                out.println("	    keyValue [");
                for (FrameData fd : segm) {
                    if (fd == null) {
                        out.println("null");
                    } else {
                        out.print(fd.trans.toString() + ", ");
                    }
                }
                out.println("\n	    ]");
                out.println("	}");
                out.println("    ]");
                out.println("    }");
            }
            out.println("\n]");
            out.println("}");
            out.println("DEF sacrum Transform {");
            out.println("rotation " + model.getRotation("sacrum"));
            out.println("scale 0.7874 0.7874 0.7874");
            out.println("translation " + model.getTranslation("sacrum"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-9 -5 -5,");
            out.println("		-9 -5 5,");
            out.println("		-6.5 5 -5,");
            out.println("		-6.5 5 5,");
            out.println("		9 -5 -5,");
            out.println("		9 -5 5,");
            out.println("		6.5 5 -5,");
            out.println("		6.5 5 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 1.046667");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	0 1 0");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF l5 Transform {");
            out.println("rotation " + model.getRotation("l5"));
            out.println("scale 1.575 1.575 0.7874");
            out.println("translation " + model.getTranslation("l5"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-3.25 0 -5,");
            out.println("		-3.25 0 5,");
            out.println("		-5 8.75 -5,");
            out.println("		-5 8.75 5,");
            out.println("		3.25 0 -5,");
            out.println("		3.25 0 5,");
            out.println("		5 8.75 -5,");
            out.println("		5 8.75 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 1.046667");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	0 1 0");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF l_upperarm Transform {");
            out.println("rotation " + model.getRotation("l_upperarm"));
            out.println("scale 0.3937 1.378 0.3937");
            out.println("translation " + model.getTranslation("l_upperarm"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-5 -10.71 -5,");
            out.println("		-5 -10.71 5,");
            out.println("		-5 -0.7142 -5,");
            out.println("		-5 -0.7142 5,");
            out.println("		5 -10.71 -5,");
            out.println("		5 -10.71 5,");
            out.println("		5 -0.7142 -5,");
            out.println("		5 -0.7142 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 0.0");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	1 1 0");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF l_forearm Transform {");
            out.println("rotation " + model.getRotation("l_forearm"));
            out.println("scale 0.3937 0.9843 0.3937");
            out.println("translation " + model.getTranslation("l_forearm"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-5 -11 -5,");
            out.println("		-5 -11 5,");
            out.println("		-5 -1 -5,");
            out.println("		-5 -1 5,");
            out.println("		2 -11 -5,");
            out.println("		2 -11 5,");
            out.println("		5 -1 -5,");
            out.println("		5 -1 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 1.046667");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	1 1 0");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF l_hand Transform {");
            out.println("rotation " + model.getRotation("l_hand"));
            out.println("scale 0.3937 0.3937 0.3937");
            out.println("translation " + model.getTranslation("l_hand"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-3 -15 -5,");
            out.println("		-3 -15 5,");
            out.println("		-3 0 -5,");
            out.println("		-3 0 5,");
            out.println("		4 -10 -5,");
            out.println("		4 -10 5,");
            out.println("		4 0 -5,");
            out.println("		4 0 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 1.046667");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	1 1 0");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF r_upperarm Transform {");
            out.println("rotation " + model.getRotation("r_upperarm"));
            out.println("scale 0.3937 1.378 0.3937");
            out.println("translation " + model.getTranslation("r_upperarm"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-5 -10.71 -5,");
            out.println("		-5 -10.71 5,");
            out.println("		-5 -0.7142 -5,");
            out.println("		-5 -0.7142 5,");
            out.println("		5 -10.71 -5,");
            out.println("		5 -10.71 5,");
            out.println("		5 -0.7142 -5,");
            out.println("		5 -0.7142 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 0.0");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	1 1 0");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF r_forearm Transform {");
            out.println("rotation " + model.getRotation("r_forearm"));
            out.println("scale 0.3937 0.9843 0.3937");
            out.println("translation " + model.getTranslation("r_forearm"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-2 -11 -5,");
            out.println("		-2 -11 5,");
            out.println("		-5 -1 -5,");
            out.println("		-5 -1 5,");
            out.println("		5 -11 -5,");
            out.println("		5 -11 5,");
            out.println("		5 -1 -5,");
            out.println("		5 -1 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 1.046667");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	1 1 0");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF r_hand Transform {");
            out.println("  rotation " + model.getRotation("r_hand"));
            out.println("  scale 0.3937 0.3937 0.3937");
            out.println("  translation " + model.getTranslation("r_hand"));
            out.println("  children [");
            out.println("    Shape {");
            out.println("      geometry IndexedFaceSet {");
            out.println("        coord Coordinate { point [");
            out.println("          -3 -15 -5,");
            out.println("          -3 -15 5,");
            out.println("          -3 0 -5,");
            out.println("          -3 0 5,");
            out.println("           4 -10 -5,");
            out.println("           4 -10 5,");
            out.println("           4 0 -5,");
            out.println("           4 0 5,");
            out.println("        ]}");
            out.println("        coordIndex [");
            out.println("          0, 1, 3, 2, -1,");
            out.println("          1, 5, 7, 3, -1,");
            out.println("          5, 4, 6, 7, -1,");
            out.println("          4, 0, 2, 6, -1,");
            out.println("          4, 5, 1, 0, -1,");
            out.println("          2, 3, 7, 6, -1,");
            out.println("        ]");
            out.println("        convex FALSE");
            out.println("        creaseAngle 1.046667");
            out.println("      }");
            out.println("      appearance Appearance {");
            out.println("        material Material {");
            out.println("          diffuseColor     1 1 0");
            out.println("          ambientIntensity 0.5");
            out.println("          specularColor    0.314 0.314 0.314");
            out.println("        }");
            out.println("      }");
            out.println("    }");
            out.println("  ]");
            out.println("}");
            out.println("DEF skull Transform {");
            out.println("  rotation " + model.getRotation("skull"));
            out.println("  scale 0.7874 0.9843 0.7874");
            out.println("  translation " + model.getTranslation("skull"));
            out.println("  children [");
            out.println("    Shape {");
            out.println("      geometry IndexedFaceSet {");
            out.println("        coord Coordinate { point [");
            out.println("           0 3.867 6,");
            out.println("          -2 3.867 4,");
            out.println("           2 3.867 4,");
            out.println("           0 7.867 4,");
            out.println("          -5 11 -5,");
            out.println("          -5 11 4,");
            out.println("          5 11 4,");
            out.println("          5 11 -5,");
            out.println("          5 1 -5,");
            out.println("          5 1 4,");
            out.println("          -5 1 4,");
            out.println("          -5 1 -5,");
            out.println("        ]}");
            out.println("        coordIndex [");
            out.println("          0, 1, 2, -1,");
            out.println("          1, 0, 3, -1,");
            out.println("          0, 2, 3, -1,");
            out.println("          4, 5, 6, 7, -1,");
            out.println("          8, 9, 10, 11, -1,");
            out.println("          10, 9, 6, 5, -1,");
            out.println("          9, 8, 7, 6, -1,");
            out.println("          8, 11, 4, 7, -1,");
            out.println("          11, 10, 5, 4, -1,");
            out.println("        ]");
            out.println("        convex FALSE");
            out.println("        creaseAngle 0.0");
            out.println("      }");
            out.println("      appearance Appearance {");
            out.println("        material Material {");
            out.println("          diffuseColor     1 0 0");
            out.println("          ambientIntensity 0.5");
            out.println("          specularColor    0.314 0.314 0.314");
            out.println("        }");
            out.println("      }");
            out.println("    }");
            out.println("  ]");
            out.println("}");
            out.println("DEF r_thigh Transform {");
            out.println("rotation " + model.getRotation("r_thigh"));
            out.println("scale 0.5906 1.575 0.5906");
            out.println("translation " + model.getTranslation("r_thigh"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-5 -10 -5,");
            out.println("		-5 -10 5,");
            out.println("		-5 0 -5,");
            out.println("		-5 0 5,");
            out.println("		5 -10 -5,");
            out.println("		5 -10 5,");
            out.println("		5 0 -5,");
            out.println("		5 0 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 0.0");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	0 0.0355 1");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF r_calf Transform {");
            out.println("rotation " + model.getRotation("r_calf"));
            out.println("scale 0.5906 1.181 0.5906");
            out.println("translation " + model.getTranslation("r_calf"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-3.667 -10 -1.667,");
            out.println("		-3.667 -10 5,");
            out.println("		-5 5.722e-006 -5,");
            out.println("		-5 5.722e-006 5,");
            out.println("		3.667 -10 -1.667,");
            out.println("		3.667 -10 5,");
            out.println("		5 5.722e-006 -5,");
            out.println("		5 5.722e-006 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 1.046667");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	0 0.0355 1");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF r_hindfoot Transform {");
            out.println("rotation " + model.getRotation("r_hindfoot"));
            out.println("scale 0.3937 0.3937 0.9843");
            out.println("translation " + model.getTranslation("r_hindfoot"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-5 -5 -5,");
            out.println("		-5 -5 5,");
            out.println("		-5 5 -5,");
            out.println("		-5 0 5,");
            out.println("		5 -5 -5,");
            out.println("		5 -5 5,");
            out.println("		5 5 -5,");
            out.println("		5 0 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 1.046667");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	0 0.0355 1");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF l_thigh Transform {");
            out.println("rotation " + model.getRotation("l_thigh"));
            out.println("scale 0.5906 1.575 0.5906");
            out.println("translation " + model.getTranslation("l_thigh"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-5 -10 -5,");
            out.println("		-5 -10 5,");
            out.println("		-5 0 -5,");
            out.println("		-5 0 5,");
            out.println("		5 -10 -5,");
            out.println("		5 -10 5,");
            out.println("		5 0 -5,");
            out.println("		5 0 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 0.0");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	0 0.0355 1");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF l_calf Transform {");
            out.println("rotation " + model.getRotation("l_calf"));
            out.println("scale 0.5906 1.181 0.5906");
            out.println("translation " + model.getTranslation("l_calf"));
            out.println("children [");
            out.println("    Shape {");
            out.println("	geometry IndexedFaceSet {");
            out.println("	    coord Coordinate { point [");
            out.println("		-3.667 -10 -1.667,");
            out.println("		-3.667 -10 5,");
            out.println("		-5 5.245e-006 -5,");
            out.println("		-5 5.245e-006 5,");
            out.println("		3.667 -10 -1.667,");
            out.println("		3.667 -10 5,");
            out.println("		5 5.245e-006 -5,");
            out.println("		5 5.245e-006 5,");
            out.println("	    ]}");
            out.println("	    coordIndex [");
            out.println("		0, 1, 3, 2, -1,");
            out.println("		1, 5, 7, 3, -1,");
            out.println("		5, 4, 6, 7, -1,");
            out.println("		4, 0, 2, 6, -1,");
            out.println("		4, 5, 1, 0, -1,");
            out.println("		2, 3, 7, 6, -1,");
            out.println("	    ]");
            out.println("	    convex FALSE");
            out.println("	    creaseAngle 1.046667");
            out.println("	}");
            out.println("	appearance Appearance {");
            out.println("	    material Material {");
            out.println("		diffuseColor	0 0.0355 1");
            out.println("		ambientIntensity	0.5");
            out.println("		specularColor	0.314 0.314 0.314");
            out.println("	    }");
            out.println("	}");
            out.println("    }");
            out.println("]");
            out.println("}");
            out.println("DEF l_hindfoot Transform {");
            out.println("  rotation " + model.getRotation("l_hindfoot"));
            out.println("  scale 0.3937 0.3937 0.9842");
            out.println("  translation " + model.getTranslation("l_hindfoot"));
            out.println("  children [");
            out.println("    Shape {");
            out.println("      geometry IndexedFaceSet {");
            out.println("        coord Coordinate { point [");
            out.println("          -5 -5 -5,");
            out.println("          -5 -5 5,");
            out.println("          -5 5 -5,");
            out.println("          -5 0 5,");
            out.println("          5 -5 -5,");
            out.println("          5 -5 5,");
            out.println("          5 5 -5,");
            out.println("          5 0 5,");
            out.println("        ]}");
            out.println("        coordIndex [");
            out.println("          0, 1, 3, 2, -1,");
            out.println("          1, 5, 7, 3, -1,");
            out.println("          5, 4, 6, 7, -1,");
            out.println("          4, 0, 2, 6, -1,");
            out.println("          4, 5, 1, 0, -1,");
            out.println("          2, 3, 7, 6, -1,");
            out.println("        ]");
            out.println("        convex FALSE");
            out.println("        creaseAngle 1.046667");
            out.println("      }");
            out.println("      appearance Appearance {");
            out.println("        material Material {");
            out.println("          diffuseColor	0 0.0355 1");
            out.println("          ambientIntensity	0.5");
            out.println("          specularColor	0.314 0.314 0.314");
            out.println("        }");
            out.println("      }");
            out.println("    }");
            out.println("  ]");
            out.println("}");
            out.println("DEF TIMER TimeSensor {");
            out.println("  loop TRUE");
            out.println("  enabled TRUE");
            out.println("  cycleInterval " + getMaxFrames() / 30.0);
            out.println("  stopTime -1");
            out.println("}");
            out.println("ROUTE TIMER.fraction_changed TO O_sacrum.set_fraction");
            out.println("ROUTE O_sacrum.value_changed TO sacrum.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_sacrum.set_fraction");
            out.println("ROUTE P_sacrum.value_changed TO sacrum.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_r_thigh.set_fraction");
            out.println("ROUTE O_r_thigh.value_changed TO r_thigh.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_r_thigh.set_fraction");
            out.println("ROUTE P_r_thigh.value_changed TO r_thigh.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_r_calf.set_fraction");
            out.println("ROUTE O_r_calf.value_changed TO r_calf.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_r_calf.set_fraction");
            out.println("ROUTE P_r_calf.value_changed TO r_calf.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_r_hindfoot.set_fraction");
            out.println("ROUTE O_r_hindfoot.value_changed TO r_hindfoot.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_r_hindfoot.set_fraction");
            out.println("ROUTE P_r_hindfoot.value_changed TO r_hindfoot.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_l_thigh.set_fraction");
            out.println("ROUTE O_l_thigh.value_changed TO l_thigh.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_l_thigh.set_fraction");
            out.println("ROUTE P_l_thigh.value_changed TO l_thigh.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_l_calf.set_fraction");
            out.println("ROUTE O_l_calf.value_changed TO l_calf.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_l_calf.set_fraction");
            out.println("ROUTE P_l_calf.value_changed TO l_calf.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_l_hindfoot.set_fraction");
            out.println("ROUTE O_l_hindfoot.value_changed TO l_hindfoot.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_l_hindfoot.set_fraction");
            out.println("ROUTE P_l_hindfoot.value_changed TO l_hindfoot.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_l5.set_fraction");
            out.println("ROUTE O_l5.value_changed TO l5.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_l5.set_fraction");
            out.println("ROUTE P_l5.value_changed TO l5.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_l_upperarm.set_fraction");
            out.println("ROUTE O_l_upperarm.value_changed TO l_upperarm.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_l_upperarm.set_fraction");
            out.println("ROUTE P_l_upperarm.value_changed TO l_upperarm.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_l_forearm.set_fraction");
            out.println("ROUTE O_l_forearm.value_changed TO l_forearm.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_l_forearm.set_fraction");
            out.println("ROUTE P_l_forearm.value_changed TO l_forearm.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_l_hand.set_fraction");
            out.println("ROUTE O_l_hand.value_changed TO l_hand.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_l_hand.set_fraction");
            out.println("ROUTE P_l_hand.value_changed TO l_hand.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_r_upperarm.set_fraction");
            out.println("ROUTE O_r_upperarm.value_changed TO r_upperarm.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_r_upperarm.set_fraction");
            out.println("ROUTE P_r_upperarm.value_changed TO r_upperarm.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_r_forearm.set_fraction");
            out.println("ROUTE O_r_forearm.value_changed TO r_forearm.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_r_forearm.set_fraction");
            out.println("ROUTE P_r_forearm.value_changed TO r_forearm.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_r_hand.set_fraction");
            out.println("ROUTE O_r_hand.value_changed TO r_hand.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_r_hand.set_fraction");
            out.println("ROUTE P_r_hand.value_changed TO r_hand.set_translation");
            out.println("ROUTE TIMER.fraction_changed TO O_skull.set_fraction");
            out.println("ROUTE O_skull.value_changed TO skull.set_rotation");
            out.println("ROUTE TIMER.fraction_changed TO P_skull.set_fraction");
            out.println("ROUTE P_skull.value_changed TO skull.set_translation");
        } catch (IOException e) {
            System.err.println("データの書き出し中に例外が発生しました.");
        }
        out.close();
    }

    /**
     * ファイルからBioVisionAnimにモーションデータを読み込むためのクラス
     */
    class Reader { // extends Thread {

        private int startFrame = 0;
        private int endFrame = -1;
        private int startIndex = 0;
        private int frameLength = -1;
        private URL url = null;
        private LineNumberReader reader = null;
        private String[] requiments = null;
        private HttpURLConnection urlconn = null;
        private HashMap<String, String> segname;

        /**
         * ソースモーションファイルbvafileのstartframeからendframeまでのaccept-
         * listに含まれるセグメントのフレームデータを，startindexから始まる 位置にframelengthの長さに納まるように読み込む．
         *
         * @param bvafile	ソースモーションデータファイルのURLの文字列表記
         * @param startframe	元データの開始フレーム（0から）
         * @param endframe	元データの終了フレーム（0から）
         * @param startindex	読み込み開始フレーム（0から）
         * @param framelength	読み込むフレームをこの長さに納める（配置した舞踊符の長さ）
         * @param requiments	サーバに要求するセグメント名の配列
         */
        public Reader(String bvafile, int startframe, int endframe, int startindex, int framelength, String[] requiments) {
            this.segname = new HashMap<>();
            this.startFrame = startframe;
            this.endFrame = endframe;
            this.startIndex = startindex;
            this.frameLength = framelength;
            this.requiments = requiments;
            // mpeg7での部位名をbvaでの部位名に変換
            segname.put("sacrum", "Hip");
            segname.put("l5", "Body");
            segname.put("l_upperarm", "L-UpperArm");
            segname.put("l_forearm", "L-ForeArm");
            segname.put("l_hand", "L-Hand");
            segname.put("r_upperarm", "R-UpperArm");
            segname.put("r_forearm", "R-ForeArm");
            segname.put("r_hand", "R-Hand");
            segname.put("skull", "Head");
            segname.put("r_thigh", "R-Thigh");
            segname.put("r_calf", "R-Shin");
            segname.put("r_hindfoot", "R-Foot");
            segname.put("l_thigh", "L-Thigh");
            segname.put("l_calf", "L-Shin");
            segname.put("l_hindfoot", "L-Foot");

            for (int i = 0; i < requiments.length; i++) {
                if (segname.get(requiments[i]) != null) {
                    requiments[i] = segname.get(requiments[i]);
                }
            }

            try {
                // モーションデータサーバではフレームを1から数える（舞踊符も）         
                url = new URL(bvafile + "&sp=" + startframe + "&ep=" + endframe + "&part=" + String.join(",", requiments));
                System.out.println(url.toString() + "からデータを読み込みます");
                urlconn = (HttpURLConnection) url.openConnection();
                urlconn.setRequestMethod("GET");
                urlconn.connect();
                reader = new LineNumberReader(new BufferedReader(new InputStreamReader(urlconn.getInputStream())));
            } catch (ProtocolException ex) {
                Logger.getLogger(BioVisionAnim.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(BioVisionAnim.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void run() {
            int i;
            String line;
            String t;
            String segmentname;
            BioVisionAnim.Segment segment;
            int frames = 0;
            if (frameLength <= 0) {
                frameLength = endFrame - startFrame + 1;
            }
            try {
                while ((line = reader.readLine()) != null) {
                    line = line.replace('\t', ' ');
                    if (line.startsWith("#")) {
                        continue;
                    }  // skip comment line
                    if (line.startsWith("Segment:")) { // Segment name
                        segmentname = line.substring(line.lastIndexOf(" ") + 1, line.length());
//            System.out.println("Segment: " +segmentname);
                        for (i = 0; i < requiments.length; i++) {
                            if (requiments[i].equals(segmentname)) {
                                break;
                            }
                        }
                        if (i == requiments.length) {
                            System.err.println("セグメント " + String.join(",", requiments) + "が見つかりません");
                            continue;                      // not match
                        }
                        segment = addNewSegment(segmentname);
                        while ((line = reader.readLine()) != null) {//read remaining header
                            if (line.startsWith("#")) {
                                continue;  // skip comment line
                            }
                            line = line.replace('\t', ' ');
                            if (line.startsWith("Frames:")) {
                                t = line.substring(line.lastIndexOf(" ") + 1, line.length());
                                frames = Integer.parseInt(t);
                            } else if (line.startsWith("Frame Time:")) {
                                t = line.substring(line.lastIndexOf(" ") + 1, line.length());
                                segment.setFrameTime(Float.parseFloat(t));
                            } else if (line.startsWith("XTRAN")) {
                                line = reader.readLine();
                                String st[] = line.split("\\s");
                                for (i = 0; i < st.length; i++) {
                                    segment.setParamUnit(i, st[i]);
                                }
                                break;
                            } // if
                        } // end of reading segment header
                        // starts reading segment frame data
                        if (endFrame < 0) {
                            endFrame = frames;
                        }
                        int lastindex = startIndex + frameLength - 1;
                        if (segment.size() < lastindex) {
                            int size = segment.size();
                            for (i = 0; i < lastindex - size + 1; i++) {
                                segment.add(new FrameData());
                            }
//                System.out.println("change segment size ("+segment.getName()+") "+size+ " -> " + segment.size() + ", lastindex="+lastindex);
                        }
//System.out.printf("sf=%d,ef=%d,si=%d,fl=%d\n", startFrame, endFrame, startIndex, frameLength);
                        for (i = 0; i < endFrame - startFrame + 1; i++) {
                            line = reader.readLine();
                            int n = (i * frameLength) / (endFrame - startFrame + 1);
                            int diff = (int) Math.ceil((double) frameLength / (double) (endFrame - startFrame + 1));
                            // System.out.println("segment("+segment.getName()+").addFramedata("+(startIndex+n)+",line["+i+"]);"+"    n="+n+", diff="+diff+"  (line number="+reader.getLineNumber()+")");
                            FrameData fd = segment.setFrameData(startIndex + n, line);
                            for (int j = n + 1; j < n + diff; j++) {
                                segment.set(startIndex + j, fd);
// System.out.println("segment("+segment.getName()+").addFramedata("+(startIndex+j)+",line["+i+"]); *    (line number="+reader.getLineNumber()+")");
                            }
                        } // for
//	    segment.setDataSize(lastindex);
                    } // end of reading one segment
                } // while
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(BioVisionAnim.class.getName()).log(Level.SEVERE, null, ex);
            }

        } // run()

    } // class Reader

    class Segment extends ArrayList<FrameData> {

        private String name;    // セグメントの名前はBVAファイルで使っている形式（Head, Body, R-UpperArm）
        private float frametime;
        private String[] unit;

        public Segment(String name) {
            unit = new String[9];

            this.name = name;
        }

        public final void setFrameTime(float ft) {
            this.frametime = ft;
        }

        public final void setParamUnit(int i, String unit) {
            this.unit[i] = unit;
        }

        public final void addFrameData(FrameData fd) {
            add(fd);
        }

        // lineから1フレームのデータを読み込みセットする
        public final FrameData setFrameData(int i, String line) {
            FrameData fd = new FrameData(line);
            set(i, fd);
            return fd;
        }

        // セグメント名を返す
        public final String getName() {
            return name;
        }

        public final String getNameAsVRML() {
            return BioVisionAnim.rsegname.get(name);
        }

        public final FrameData getFrame(int index) {
            FrameData fd = null;
            try {
                fd = get(index);
            } catch (IndexOutOfBoundsException e) {
                fd = null;
            }
            return fd;
        }

        public final float getFrameTime() {
            return frametime;
        }

        public final String getParamUnit(int i) {
            return unit[i];
        }

        public final String[] getParamUnits() {
            return unit;
        }
    } // end of class Segment

    // 1フレームのデータを表すクラス
    class FrameData {

        public Vector3f trans;	// translation
        public Euler rot;	// rotation (degry)
        public Vector3f scale;	// scale

        public FrameData() {
            trans = new Vector3f();
            rot = new Euler();	// deg.
            scale = new Vector3f(100.0F, 100.0F, 100.0F);
        }

        public FrameData(String line) {
            String st[] = line.split("\\s");
            trans = new Vector3f(Float.parseFloat(st[0]), Float.parseFloat(st[1]), Float.parseFloat(st[2]));
            rot = new Euler(Float.parseFloat(st[4]), Float.parseFloat(st[3]), Float.parseFloat(st[5]));
            scale = new Vector3f(Float.parseFloat(st[6]), Float.parseFloat(st[7]), Float.parseFloat(st[8]));
        }

        // このフレームデータの文字列表現を返す(BVA形式)．
        @Override
        public final String toString() {
            return String.format("%4.4f %4.4f %4.4f %4.4f %4.4f %4.4f %4.4f %4.4f %4.4f",
                        trans.x, trans.y, trans.z,
                        rot.getRollDeg(), rot.getPitchDeg(), rot.getYawDeg(),
                        scale.x, scale.y, scale.z);
        }
    } // end of class FrameData

    public static void main(String args[]) {
        String filename1 = "C:\\cygwin\\home\\yukawa\\test.bva";
        String filename2 = null;
        int startframe = 0;
        int endframe = -1;
        int startindex = 0;
        int framelength = -1;
        /*
         String[] acceptlist = null;
         int len = args.length;

         if (len == 7) {
         acceptlist = ((String)args[6]).split(",");
         }
         if (len >= 6) {
         framelength = Integer.parseInt(args[5]);
         }
         if (len >= 5) {
         startindex = Integer.parseInt(args[4]);
         }
         if (len >= 4) {
         endframe = Integer.parseInt(args[3]);
         }
         if (len >= 3) {
         startframe = Integer.parseInt(args[2]);
         }
         if (len >= 2) {
         filename2 = new String(args[1]);
         }
         if (len >= 1) {
         filename1 = new String(args[0]);
         } else {
         System.err.println("usage: BioVisionAnim filename1 filename2 startframe endframe [startindex] [framelength] [acceptlist]");
         System.err.println("\t filename1: 新しく作成されるファイル名");
         System.err.println("\t filename2: 既存のBVAファイル名");
         System.err.println("\t startframe: 読み込み開始フレーム数");
         System.err.println("\t endframe: 読み込み終了フレーム数");
         System.err.println("\t startindex: 何フレーム目に読み込むか");
         System.err.println("\t framelength: 何フレームの間に読み込むか");
         System.err.println("\t acceptlist: 読み込むセグメント名を,でつなげたもの");
         System.exit(0);
         }
         URL bvaurl = null;
         */
        BioVisionAnim bva = new BioVisionAnim();
        String bvaurl = null;
        bvaurl = "http://msv/cgi-bin/msv.pl?nm=akita-ondo&amp;type=bva";
//    bva.readData(bvaurl, startframe, endframe, startindex, framelength, acceptlist);
        String[] requiments = {"l_upperarm", "l_forearm", "l_hand"};
        bva.readData(bvaurl, 0, 100, 20, 80, requiments);

        File newfile = new File(filename1);
        bva.saveData(newfile);
    } // end of main
} // end of class BioVisionAnim

class StandardModel {

    public static final HashMap<String, String> rotation;
    public static final HashMap<String, String> translation;

    static {
        rotation = new HashMap<>();
        translation = new HashMap<>();
        rotation.put("sacrum", "0.608 0.7852 0.1173 0.2882");
        rotation.put("r_thigh", "0.7721 -0.1626 0.6143 5.613");
        rotation.put("r_calf", "0.7214 -0.1502 -0.676 0.8541");
        rotation.put("r_hindfoot", "0.9691 -0.2438 -0.03819 0.9838");
        rotation.put("l_thigh", "0.9691 -0.2438 -0.03819 0.9838");
        rotation.put("l_calf", "0.5707 0.8201 0.0418 0.6756");
        rotation.put("l_hindfoot", "0.09811 0.9951 0.008866 0.63");
        rotation.put("l5", "0.7682 0.6398 0.02264 0.2897");
        rotation.put("l_upperarm", "-0.3612 0.5651 0.7417 1.458");
        rotation.put("l_forearm", "0.6835 -0.5539 -0.4754 4.601");
        rotation.put("l_hand", "0.6569 -0.5596 -0.5053 3.947");
        rotation.put("r_upperarm", "0.5891 0.5137 0.6238 5.014");
        rotation.put("r_forearm", "0.7262 0.3519 0.5906 4.821");
        rotation.put("r_hand", "-0.5792 -0.3998 0.7105 3.847");
        rotation.put("skull", "0.7566 0.651 -0.06097 0.3531");
        translation.put("sacrum", "5.976 34.2 3.314");
        translation.put("r_thigh", "2.524 30.07 3.401");
        translation.put("r_calf", "-3.149 17.45 11.42");
        translation.put("r_hindfoot", "-10.89 6.106 6.575");
        translation.put("l_thigh", "9.537 30.46 1.815");
        translation.put("l_calf", "14.39 16.76 8.517");
        translation.put("l_hindfoot", "15.39 2.372 6.538");
        translation.put("l5", "5.921 38.14 4.021");
        translation.put("l_upperarm", "16.03 52.46 5.354");
        translation.put("l_forearm", "29.05 46.84 5.167");
        translation.put("l_hand", "38.55 44.28 10.15");
        translation.put("r_upperarm", "-3.785 51.91 9.025");
        translation.put("r_forearm", "-15.25 45.07 13.81");
        translation.put("r_hand", "-23.75 42.38 20.29");
        translation.put("skull", "6.146 53.75 7.542");
    }

    public final String getRotation(String part) {
        return rotation.get(part);
    }

    public final String getTranslation(String part) {
        return translation.get(part);
    }
} // end of class StandardModel
