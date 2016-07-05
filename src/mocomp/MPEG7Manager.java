//
//			      MPEG7Manager.java
//
//		Copyright (C) 1999-2009 Takashi Yukawa
//
//		    This java source file conforms
//		GNU GENERAL PUBLIC LICENSE Version 2.
//
// Author:  Takashi Yukawa <yukawalab@gmail.com>
// Created: May.28, 1999
// Revised: Jun.13, 2001
// Revised: Dec.13, 2008
// Revised: Aug.21, 2009
// Revised: Jul.05, 2016
//
// Sep.01, 2009
// * クラス名を変更
// Aug.21, 2009
// * リソースファイルのXMLDBUriに複数のサーバを記述できるようにした。
// Dec.13, 2008
// * 舞踊符を独自ファイル（motion2.xml）で定義するのをやめ，mpeg-7を用いた記述に
//   変更した．それに伴い，舞踊符をxmlサーバ(xindice)から読み込むように変更した．
// Jul.05, 2016
// * XMLデータベースとして使っていたApache Xindiceのプロジェクトが終了したので，
//   BaseXを利用するように修正した．

package mocomp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.Authenticator;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MPEG7Manager {
    static final String sep = "-";
    private HttpURLConnection conn = null;
  
    private ArrayList<String> parts = new ArrayList<>();
    private ArrayList<String> genres = new ArrayList<>();// 動作の種類 genre
    private HashMap<String, String[]> segments = new HashMap<>();
    private HashMap<String, String> partnames = new HashMap<>();
    private ArrayList<String> partstrs = new ArrayList<>();
    private HashMap<String, String> titlename2code = null;
    private HashMap<String, String> titlecode2name = null;
    private HashMap<String, String> genrecode2name = null;
    private HashMap<String, String> genrename2code = null;
//    private ArrayList<String> selectedTitle = null;
    private URL url = null;

  public MPEG7Manager() {
    String basex_user = MotionCompApp.getResourceString("BaseXUsername");
    String basex_passwd = MotionCompApp.getResourceString("BaseXPassword");
     
    Authenticator.setDefault(new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(basex_user, basex_passwd.toCharArray());
        }
    });
    
    try {
        url = new URL(MotionCompApp.getResourceString("BaseXURL"));
    } catch (MalformedURLException ex) {
        Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    } 
    
    try {
        conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("Content-Type", "application/xml;charset=UTF-8");         
    } catch (IOException ex) {
        Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    }  

    // setup parts, partnames, segments
    String query = "<query xmlns='http://basex.org/rest'><text>//Mpeg7/Description/MultimediaContent/Video/CreationInformation/Header/Comment/FreeTextAnnotation/text()</text></query>";
       
    try {    
        try (OutputStream out = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.write(query);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line = null;
    aaa:
            while ((line = reader.readLine()) != null) {
                String[] st = line.split(":");
                String pcode = st[0]; // part code
                for (String ss : parts) {
                    if (ss.equals(pcode)) {
                        break aaa;
                    }
                }
                parts.add(pcode);
                partnames.put(pcode, st[1]);    // part name
                partstrs.add(st[1]);
                String psegs = st[2];           // segments
                String[] st2 = psegs.split(",");
                segments.put(pcode, st2);
            }
        }
    } catch (IOException e) {
        Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, e);
    } finally {
        if (conn != null) {
            conn.disconnect();
        }
    }
    
    genrecode2name = new HashMap<>();
    genrename2code = new HashMap<>();
    // setup classes
    try {
        conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("Content-Type", "application/xml;charset=UTF-8");         
    } catch (IOException ex) {
        Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    }
    try {    
        query = "<query xmlns='http://basex.org/rest'><text>//Mpeg7/Description/CreationInformation/Classification/Genre/Name/text()</text></query>";//POSTするデータ
        try (OutputStream out = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            writer.write(query);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line = null;
l1:
            while ((line = reader.readLine()) != null) {
                for (String ss : genres) {
                    if (ss.equals(line)) {
                        continue l1;
                    }
                }
                genres.add(line);
                genrename2code.put("Dance sport", "1.6.24");
                genrename2code.put("Karate Kata", "1.6.40");
                genrecode2name.put("1.6.24", "Dance sport");
                genrecode2name.put("1.6.40", "Karate Kata");   
            }
        }
    } catch (IOException ex) {
        Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    } finally {
        if (conn != null) {
            conn.disconnect();
        }
    }    
//    changeGenre(0);  // 必要かどうか分からない???
  } // MPEG7Manager

    private String xQuery(String xpath) {
        try {
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Content-Type", "application/xml;charset=UTF-8");         
        } catch (IOException ex) {
            Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
        }
        String query = "<query xmlns='http://basex.org/rest'><text>"+xpath+"</text></query>";
        try {
            try (OutputStream out = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                writer.write(query);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.readLine();
            }    
        } catch (IOException ex) {
            Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }
  
    public final String getUri() {
        return "getUri() has not been implemented. "+MPEG7Manager.class.getName();    // uri;
    }

    public final int getStartFrame(String motioncode) {
//    String motioncode = "1.6.24-1159248584-0900126000-0247825860-1187363539-0001";
        String xpath = "//VideoSegment[@id='"+motioncode+"']/MediaTime/MediaTimePoint/text()";
        String mtp = xQuery(xpath);
        return Integer.parseInt(mtp.substring(0,mtp.indexOf("F")));
    }
  
    public final float getTime(String motioncode) {
        String duration = getSegmentDuration(motioncode);
        String cols[] = duration.split("[TNF]");
        return Float.parseFloat(cols[1]) / Float.parseFloat(cols[2]);
    }

    public final String getDescription(String motioncode) //1.6.24-011568-0705896909-0686741873-0001
    {
//    String motioncode = "1.6.24-1159248584-0900126000-0247825860-1187363539-0001";
        String xpath = "//VideoSegment[@id='"+motioncode+"']/CreationInformation/Creation/Abstract/FreeTextAnnotation/text()";
        return xQuery(xpath);
    }
  
    public final int getPartSize() {
        return parts.size();
    }

    public final String getPartName(int index) {
        return partnames.get(parts.get(index));
    }

    public final String[] getPartNames() {
        return (String[])(partstrs.toArray(new String[0]));
    }

    public String getPartCode(int index) {
        return parts.get(index);
    }

    public final String[] getPartSegments(int index) { // 部位に含まれるセグメントのベクタを作成する
        HashMap<String, String> segname = new HashMap<>();
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

        String xpath = "//Mpeg7/Description[@xsi:type='ContentEntityType'][1]/MultimediaContent/Video/CreationInformation/Header/Comment/FreeTextAnnotation["+(index+1)+"]/text()";
        String line = xQuery(xpath);
        String st[] = line.split(":");
        String[] result = st[2].split(",");
        for (int i=0; i<result.length; i++) {
            if (segname.get(result[i]) != null) {
                result[i] = segname.get(result[i]);
            }
        }
        return result;
    }

// i番目のクラス（ジャンル）に属する演目名のベクタを返す
    public final ArrayList<String> getShowNames(int cindex) {
        titlename2code = new HashMap<>();
        titlecode2name = new HashMap<>();    
        ArrayList<String> sv = new ArrayList<>();
        String genre = genres.get(cindex); // genre name
        String xpath = "//Mpeg7/Description/CreationInformation[Classification/Genre/Name='"+genre+"']/Creation/[@id,Title/text()]";
        String query = "<query xmlns='http://basex.org/rest'><text>"+xpath+"</text></query>";
        try {
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Content-Type", "application/xml;charset=UTF-8");         
        } catch (IOException ex) {
            Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            try (OutputStream out = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                writer.write(query);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    String[] result = line.substring(5, line.length()-1).split("\", ");
                    if (result[1].startsWith("(")) {
                        String[] result2 = result[1].substring(1,result[1].length()-1).split(", ");
                        result[1] = result2[0]+"."+result2[1];
                    }
                    sv.add(result[1]);  //name
                    titlename2code.put(result[1], result[0]);
                    titlecode2name.put(result[0], result[1]);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }    
        return sv;
    }

    // i番目のクラス（ジャンル）に属する演目のnodeのベクタを返す
/*
  public final ArrayList<Node> changeGenre(int cindex) {
        selectedTitle = new ArrayList<String>();
        String genre = genrecode2name.get(genres.get(cindex)); // genre name
        String xpath = "/Mpeg7/Description/CreationInformation/Classification/Genre/Name[text()='"+genre+"']/../../../../following-sibling::*";
        String query = "<query xmlns='http://basex.org/rest'><text>"+xpath+"</text></query>";
        try {
            try (OutputStream out = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                writer.write(query);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
             
      
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
        }    


    try {
      ResourceSet resultSet = service.query(xpath);
      ResourceIterator results = resultSet.getIterator();
      while (results.hasMoreResources()) {
        Resource res = results.nextResource();
        if (res.getResourceType().equals("XMLResource")) {
          XMLResource xmlres = (XMLResource) res;
          Node node = xmlres.getContentAsDOM();
          selectedTitle.add(node);
        }
      }
    } catch (XMLDBException e) {}
      return selectedTitle;
  }
*/

// gindex : ジャンルのインデクス
// sindex : 演目のインデクス
// pindex : 部位のインデクス
// gindex番目のジャンルのsindex番目の演目で、pindex用に定義されている舞踊符のベクタ
// を作成する
    public final ArrayList<String> getSegments(int gindex, int tindex, int pindex) {
//    System.out.printf("getSegments(%d,%d,%d)\n", gindex, tindex, pindex);
        ArrayList<String> ids = new ArrayList<>();
    //ArrayList<Node> shows = changeGenre(gindex);
    //boolean b = false;
        String genreName = genres.get(gindex);
//    System.out.printf("getGenre(%d)=%s\n", gindex, genreName);
        ArrayList<String> shownames = getShowNames(gindex);
        String tname = shownames.get(tindex); // title
        String tcode = titlename2code.get(tname);
        String[] tt = tcode.split(sep);
        String partCode = getPartCode(pindex);
        String xpath = "//Mpeg7/Description/CreationInformation[@id='"+tt[2]+"']/Classification/Genre/Name[text()='" + genreName + "']/../../../../../Description/MultimediaContent/Video/TemporalDecomposition/VideoSegment[contains(@id,'" + partCode + "')]/TemporalDecomposition/VideoSegment/@id";
        String query = "<query xmlns='http://basex.org/rest'><text>"+xpath+"</text></query>";
        try {
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Content-Type", "application/xml;charset=UTF-8");         
        } catch (IOException ex) {
            Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            try (OutputStream out = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                    writer.write(query);
            }
            try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    ids.add(line.substring(4,line.length()-1));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return ids;
    }
      
  // 基本動作を得る
    public final String getSegment(int gindex, int tindex, int sindex, int pindex) {
//    System.out.printf("getSegment(%d,%d,%d,%d)\n", gindex, tindex, sindex, pindex);
        ArrayList<String> nv = getSegments(gindex, tindex, pindex);
        return nv.get(sindex);
    }

  
    public final ArrayList<String> getGenres() {
        return genres;
    }

    public final String getMediaLocator(String motioncode, String format) {
//    String format = "BVH";
//    System.out.println("getMediaLocator("+motioncode+")");
        String xpath = "//Mpeg7[Description/MultimediaContent/Video/TemporalDecomposition/VideoSegment/TemporalDecomposition/VideoSegment/@id='" + motioncode + "']/Description/MediaInformation/MediaProfile[MediaFormat/FileFormat/Name='" + format + "']/MediaInstance/MediaLocator/MediaUri/text()";
        return xQuery(xpath);
    }

  /*
  public final Node getSegment(String gcode, String tcode, String scode, String pcode) {
//    System.out.printf("getSegment(%s,%s,%s,%s)\n", gcode, tcode, scode, pcode);
    String gname = genrecode2name.get(gcode);
//    System.out.println("scode="+scode);
    String xpath = "//Mpeg7/Description/CreationInformation[@id='"+tcode+"']/Classification/Genre/Name[text()='" + gname + "']/../../../../../Description/MultimediaContent/Video/TemporalDecomposition/VideoSegment[contains(@id,'" + pcode + "')]/TemporalDecomposition/VideoSegment[@id='"+scode+"']";
//    System.out.println("getSegment: xpath = ["+xpath+"]");
    try {
      ResourceSet resultSet = service.query(xpath);
      Resource res = resultSet.getResource(0);
      if (res.getResourceType().equals("XMLResource")) {
        XMLResource xmlres = (XMLResource) res;
        Node segment = xmlres.getContentAsDOM();
        return segment.getFirstChild();
      }
    } catch (XMLDBException e) {
    }
    return null;
  }
*/
  
  /*
    public final Node getSegment(String segcode) {
//      System.out.printf("getSegment(\"%s\")\n", segcode);
      String xpath = "/Mpeg7/Description/MultimediaContent/Video/TemporalDecomposition/VideoSegment/TemporalDecomposition/VideoSegment[@id='" + segcode + "']";
//      System.out.println("getSegment: xpath = [" + xpath + "]");
      try {
        ResourceSet resultSet = service.query(xpath);
        Resource res = resultSet.getResource(0);
        if (res.getResourceType().equals("XMLResource")) {
          XMLResource xmlres = (XMLResource) res;
          Node segment = xmlres.getContentAsDOM();
          return segment.getFirstChild();
        }
      } catch (XMLDBException e) {
      }
      return null;
  }
  */
  /*
  public final int getDuration(Node segment) {
    String str[] = null;
    try {
      Node node = XPathAPI.selectSingleNode(segment, "MediaTime/MediaDuration");
      str = node.getFirstChild().getNodeValue().split("[TN]");
    } catch (javax.xml.transform.TransformerException e) {
    }
    return Integer.parseInt(str[1]); //Integer.parseInt(beg);
}*/
  
//  public final String getSegmentDuration(Node segment) {
//    String duration = null;
//    try {
//      duration = XPathAPI.selectSingleNode(segment, "MediaTime/MediaDuration").getFirstChild().getNodeValue();
//    } catch (TransformerException ex) {
//      Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
//    }
//    return duration;
//  }

    public final String getSegmentDuration(String motioncode) {
//    String motioncode = "1.6.24-1159248584-0900126000-0247825860-1187363539-0001";
        String xpath = "//VideoSegment[@id='"+motioncode+"']/MediaTime/MediaDuration/text()";
        return xQuery(xpath);
    }

  
 /*
  public final String getSegmentTitle(Node segment) {
    String segtitle = null;
    try {
      segtitle = XPathAPI.selectSingleNode(segment, "CreationInformation/Creation/Title").getTextContent();
    } catch (TransformerException ex) {
      Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    }
    return segtitle;
  }
  */
  
    public final String getSegmentTitle(String motioncode) {//1.6.24-011568-0705896909-0686741873-0001
//    String motioncode = "1.6.24-1159248584-0900126000-0247825860-1187363539-0001";
        String xpath = "//VideoSegment[@id='"+motioncode+"']/CreationInformation/Creation/Title/text()";
        return xQuery(xpath);
    }

/*
    public final Node getCreation(String creationid) {
//    System.out.printf("getCreation(\"%s\")\n", creationid);
    String xpath = "/Mpeg7/Description/CreationInformation/Creation[@id='"+creationid+"']";
//    System.out.println("getCreation: xpath = ["+xpath+"]");
    try {
      ResourceSet resultSet = service.query(xpath);
//      System.out.println("resultSet="+resultSet.toString());
      Resource res = resultSet.getResource(0);
      if (res.getResourceType().equals("XMLResource")) {
        XMLResource xmlres = (XMLResource) res;
        Node segment = xmlres.getContentAsDOM();
        return segment.getFirstChild();
      }
    } catch (XMLDBException e) {}
    return null;
  }
*/
/*
    public final String getTitle(Node creation) {
  String title = null;
  try {
    title = XPathAPI.selectSingleNode(creation, "Title").getTextContent();
  } catch (TransformerException ex) {
    Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
  }
  return title;
}
*/
    
    public final String getTitle(String creationid)
    {
        String xpath = "//Mpeg7/Description/CreationInformation/Creation[@id='"+creationid+"']/[Title/text()]";
        String query = "<query xmlns='http://basex.org/rest'><text>"+xpath+"</text></query>";
        try {
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Content-Type", "application/xml;charset=UTF-8");         
        } catch (IOException ex) {
            Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            try (OutputStream out = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                writer.write(query);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    line = line.substring(1,line.length()-1);
                    if (line.startsWith("(")) {
                        String str[] = line.substring(1,line.length()-1).split(", ");
                        line = str[0]+"."+str[1];
                    }
                    return line;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;    
    }


/*
public final String getGivenName(Node creation) {
  String gname = null;
  try {
    gname = XPathAPI.selectSingleNode(creation, "Creator/Agent/Name/GivenName").getTextContent();
  } catch (TransformerException ex) {
    Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
  }
  return gname;
}
*/


    public final String getGivenName(String creationid)
    {
        String xpath = "//Mpeg7/Description/CreationInformation/Creation[@id='"+creationid+"']//GivenName[@xml:lang='ja']/text()";
        return xQuery(xpath);    
    }

/*
public final String getFamilyName(Node creation) {
  String fname = null;
  try {
    fname = XPathAPI.selectSingleNode(creation, "Creator/Agent/Name/FamilyName").getTextContent();
  } catch (TransformerException ex) {
    Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
  }
  return fname;
}
*/

    public final String getFamilyName(String creationid)
    {
        String xpath = "//Mpeg7/Description/CreationInformation/Creation[@id='"+creationid+"']//FamilyName[@xml:lang='ja']/text()";
        return xQuery(xpath);   
    }

    public void partTest() {
        for (int i=0; i < getPartSize(); i++) {
        System.out.print(getPartName(i) + ":" + getPartCode(i) + ":");
        String[] sl = getPartSegments(i);
            for (String sl1 : sl) {
                System.out.print(sl1 + " ");
            }
        System.out.print("\n");
        }
    }

    public void classTest() {
        System.out.println("classTest()");
/*
    Node seg1 = getSegment(0, 0, 0, 0);
    System.out.println("seg1=");
    System.out.println(seg1.toString());
    Node seg2 = getSegment(1, 0, 0, 0);
    System.out.println("seg2=");
    System.out.println(seg2.toString());
        */
    }
 
    public static void main(String argv []) {
        MPEG7Manager mgr = new MPEG7Manager();
        mgr.classTest();
//  mgr.partTest();
//  mgr.test();
    }
}
