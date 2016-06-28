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
//
// Sep.01, 2009
// * クラス名を変更
// Aug.21, 2009
// * リソースファイルのXMLDBUriに複数のサーバを記述できるようにした。
// Dec.13, 2008
// * 舞踊符を独自ファイル（motion2.xml）で定義するのをやめ，mpeg-7を用いた記述に
//   変更した．それに伴い，舞踊符をxmlサーバ(xindice)から読み込むように変更した．

package mocomp;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import org.w3c.dom.*;
import org.apache.xpath.XPathAPI;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.xmldb.api.*;

public class MPEG7Manager {
  static final String sep = "-";
  
  private org.xmldb.api.base.Collection col = null;
  private XPathQueryService service = null;
  
  private ArrayList<String> parts = new ArrayList<>();
  private ArrayList<String> genres = new ArrayList<>();// 動作の種類 genre
  private HashMap<String, String[]> segments = new HashMap<>();
  private HashMap<String, String> partnames = new HashMap<>();
  private ArrayList<String> partstrs = new ArrayList<>();
  private HashMap<String, String> titlename2code = null;
  private HashMap<String, String> titlecode2name = null;
  private HashMap<String, String> genrecode2name = null;
  private HashMap<String, String> genrename2code = null;
  private ArrayList<Node> selectedTitle = null;
  private String uri = null;

  public MPEG7Manager() {

    String driver = "org.apache.xindice.client.xmldb.DatabaseImpl";
    String uris = MotionCompApp.getResourceString("XMLDBUri"); // "uri1,uri2,...";

    Class c = null;
    try {
      c = Class.forName(driver);
    } catch (ClassNotFoundException ex) {
      Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    }
    Database database = null;
    try {
      database = (Database) c.newInstance();
    } catch (InstantiationException | IllegalAccessException ex) {
      Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    }
    String dburi[] = uris.split(",");
    for (int i = 0; i < dburi.length && col == null; i++) {
      try {
        DatabaseManager.registerDatabase(database);
        col = DatabaseManager.getCollection(dburi[i]);
        service = (XPathQueryService) col.getService("XPathQueryService", "1.0");
        uri = dburi[i];
      } catch (XMLDBException ex) {
//      Logger.getLogger(Mcml.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    // setup parts, partnames, segments
    String xpath = "/Mpeg7/Description/MultimediaContent/Video/CreationInformation/Header/Comment/FreeTextAnnotation";
    ResourceSet resultSet;
    ResourceIterator results;
    try {
      resultSet = service.query(xpath);
      results = resultSet.getIterator();
      aaa:
      while (results.hasMoreResources()) {
        Resource res = results.nextResource();
        if (res.getResourceType().equals("XMLResource")) {
          Node node = ((XMLResource) res).getContentAsDOM();
          String[] st = node.getFirstChild().getTextContent().split(":");
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
    } catch (XMLDBException ex) {
      Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    }

    genrecode2name = new HashMap<>();
    genrename2code = new HashMap<>();
    // setup classes
    xpath = "/Mpeg7/Description/CreationInformation/Classification/Genre/Name/text()";
    try {
      resultSet = service.query(xpath);
      results = resultSet.getIterator();
      l1:
      while (results.hasMoreResources()) {
        Resource res = results.nextResource();
        if (res.getResourceType().equals("XMLResource")) {
          XMLResource xmlres = (XMLResource) res;
          Node node = xmlres.getContentAsDOM();
          String genrename = (String) node.getFirstChild().getTextContent();
          for (String ss : genres) {
            if (ss.equals(genrename)) {
              continue l1;
            }
          }
          genres.add(genrename);
        }
        genrename2code.put("Dance sport", "1.6.24");
        genrename2code.put("Karate Kata", "1.6.40");
        genrecode2name.put("1.6.24", "Dance sport");
        genrecode2name.put("1.6.40", "Karate Kata");
      } // while
    } catch (XMLDBException ex) {
      Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    }
    changeGenre(0);
  } // MPEG7Manager

  public final String getUri() {
    return uri;
  }

  public final int getStartFrame(Node segment) {
    String str[] = null;
    try {
      Node node = XPathAPI.selectSingleNode(segment, "MediaTime/MediaTimePoint");
      str = node.getFirstChild().getNodeValue().split("F");
    } catch (javax.xml.transform.TransformerException e) {}
    return Integer.parseInt(str[0]); //Integer.parseInt(beg);
  }

  public final int getDuration(Node segment) {
    String str[] = null;
    try {
      Node node = XPathAPI.selectSingleNode(segment, "MediaTime/MediaDuration");
      str = node.getFirstChild().getNodeValue().split("[TN]");
    } catch (javax.xml.transform.TransformerException e) {
    }
    return Integer.parseInt(str[1]); //Integer.parseInt(beg);
}

  public final float getTime(Node segment) {
    float rate = 0.0f;
    String str[] = null;
    try {
      Node node = XPathAPI.selectSingleNode(segment, "MediaTime/MediaTimePoint");
      str = node.getFirstChild().getNodeValue().split("F");
      rate = Float.parseFloat(str[1]);
      node = XPathAPI.selectSingleNode(segment, "MediaTime/MediaDuration");
      str = node.getFirstChild().getNodeValue().split("[TN]");
    } catch (javax.xml.transform.TransformerException e) {}
    return Float.parseFloat(str[1]) / rate;
  }

  public final String getDesc(Node segment) {
    Node desc = null;
    try {
      desc = XPathAPI.selectSingleNode(segment, "CreationInformation/Creation/Abstract/FreeTextAnnotation");
    } catch (javax.xml.transform.TransformerException te) {}
    return desc.getFirstChild().toString();
  }

  public final String getDescription(Node segment) {
    String description = null;
    try {
      description = XPathAPI.selectSingleNode(segment, "CreationInformation/Creation/Abstract/FreeTextAnnotation").getTextContent();
    } catch (TransformerException ex) {
      Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    }
    return description;
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
    try {
      ResourceSet resultSet = service.query("/Mpeg7/Description/MultimediaContent/Video/CreationInformation/Header/Comment/FreeTextAnnotation");
      Resource res = resultSet.getResource(index);
      if (res.getResourceType().equals("XMLResource")) {
        Node node = ((XMLResource) res).getContentAsDOM();
        String st[] = node.getFirstChild().getTextContent().split(":");
        String[] result = st[2].split(",");
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
      
        for (int i=0; i<result.length; i++) {
          if (segname.get(result[i]) != null) {
              result[i] = segname.get(result[i]);
          }
        }
        return result;
      }
    } catch (XMLDBException e) {}
    return null;
  }

// i番目のクラス（ジャンル）に属する演目名のベクタを返す
  public final ArrayList<String> getShowNames(int cindex) {
    titlename2code = new HashMap<>();
    titlecode2name = new HashMap<>();    
    ArrayList<String> sv = new ArrayList<>();
    String genre = genres.get(cindex); // genre name
    String xpath = "/Mpeg7/Description/CreationInformation/Classification/Genre/Name[text()='" + genre + "']/../../../Creation";
    try {
      ResourceSet resultSet = service.query(xpath);
      ResourceIterator results = resultSet.getIterator();
      while (results.hasMoreResources()) {
        Resource res = results.nextResource();
        if (res.getResourceType().equals("XMLResource")) {
          XMLResource xmlres = (XMLResource) res;
          Node node = xmlres.getContentAsDOM();
          String id = ((Element) node.getFirstChild()).getAttribute("id");
          NodeList nl = ((Element) node.getFirstChild()).getElementsByTagName("Title");
          if (nl.getLength() == 1) {
            sv.add(nl.item(0).getTextContent());
            titlename2code.put(nl.item(0).getTextContent(), id);
            titlecode2name.put(id, nl.item(0).getTextContent());
          } else if (nl.getLength() == 2) {
            String nm = nl.item(0).getTextContent() + " " + nl.item(1).getTextContent();
            sv.add(nm);
            titlename2code.put(nm, id);
            titlecode2name.put(id, nm);
          } else {
//
          }
        }
      }
    } catch (XMLDBException e) {
    }
    return sv;
  }

 // i番目のクラス（ジャンル）に属する演目のnodeのベクタを返す
  public final ArrayList<Node> changeGenre(int cindex) {
    selectedTitle = new ArrayList<Node>();
    String genre = genrecode2name.get(genres.get(cindex)); // genre name
    String xpath = "/Mpeg7/Description/CreationInformation/Classification/Genre/Name[text()='"+genre+"']/../../../../following-sibling::*";
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

  // cindex : ジャンルのインデクス
  // sindex : 演目のインデクス
  // pindex : 部位のインデクス
// cindex番目のジャンルのsindex番目の演目で、pindex用に定義されている舞踊符のベクタ
// を作成する
  public final ArrayList<Node> getSegments(int gindex, int tindex, int pindex) {
//    System.out.printf("getSegments(%d,%d,%d)\n", gindex, tindex, pindex);
    ArrayList<Node> nv = new ArrayList<>();
    ArrayList<Node> shows = changeGenre(gindex);
    boolean b = false;
    String genreName = genres.get(gindex);
//    System.out.printf("getGenre(%d)=%s\n", gindex, genreName);
    ArrayList<String> shownames = getShowNames(gindex);
    String tname = shownames.get(tindex); // title
    String tcode = titlename2code.get(tname);
    String[] tt = tcode.split(sep);
    String partCode = getPartCode(pindex);
    try {
      String xpath = "/Mpeg7/Description/CreationInformation[@id='"+tt[2]+"']/Classification/Genre/Name[text()='" + genreName + "']/../../../../../Description/MultimediaContent/Video/TemporalDecomposition/VideoSegment[contains(@id,'" + partCode + "')]/TemporalDecomposition/VideoSegment";
//      System.out.println("getSegments: xpath = [" + xpath + "]");
      ResourceSet resultSet = service.query(xpath);
      ResourceIterator results = resultSet.getIterator();
      while (results.hasMoreResources()) {
        Resource res = results.nextResource();
        if (res.getResourceType().equals("XMLResource")) {
          XMLResource xmlres = (XMLResource) res;
          Node node = xmlres.getContentAsDOM();
          nv.add(node.getFirstChild());
        }
      }
    } catch (XMLDBException e) {}
    return nv;
  }

  // 基本動作を得る
  public final Node getSegment(int gindex, int tindex, int sindex, int pindex) {
//    System.out.printf("getSegment(%d,%d,%d,%d)\n", gindex, tindex, sindex, pindex);
    ArrayList<Node> nv = getSegments(gindex, tindex, pindex);
    Node segment = nv.get(sindex);
    return segment;
  }

  public final ArrayList<String> getGenres() {
    return genres;
  }

  public final String getMediaLocator(String motioncode, String format) {
//    String format = "BVH";
//    System.out.println("getMediaLocator("+motioncode+")");
    String xpath = "/Mpeg7/Description/MultimediaContent/Video/TemporalDecomposition/VideoSegment/TemporalDecomposition/VideoSegment[@id='" + motioncode + "']/../../../../../../../Description/MediaInformation/MediaProfile/MediaFormat/FileFormat/Name[text()='" + format + "']/../../../MediaInstance/MediaLocator/MediaUri/text()";
    try {
      ResourceSet resultSet = service.query(xpath);
      Resource res = resultSet.getResource(0);
      if (res.getResourceType().equals("XMLResource")) {
        XMLResource xmlres = (XMLResource) res;
        Node mediaUri = xmlres.getContentAsDOM().getFirstChild();
//        System.out.println("uri= " + mediaUri.getTextContent());
        return mediaUri.getTextContent();
      }
    } catch (XMLDBException e) {
    }
    return null;
  }
  
  public final Node getSegment(String gcode, String tcode, String scode, String pcode) {
//    System.out.printf("getSegment(%s,%s,%s,%s)\n", gcode, tcode, scode, pcode);
    String gname = genrecode2name.get(gcode);
//    System.out.println("scode="+scode);
    String xpath = "/Mpeg7/Description/CreationInformation[@id='"+tcode+"']/Classification/Genre/Name[text()='" + gname + "']/../../../../../Description/MultimediaContent/Video/TemporalDecomposition/VideoSegment[contains(@id,'" + pcode + "')]/TemporalDecomposition/VideoSegment[@id='"+scode+"']";
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

  public final String getSegmentDuration(Node segment) {
    String duration = null;
    try {
      duration = XPathAPI.selectSingleNode(segment, "MediaTime/MediaDuration").getFirstChild().getNodeValue();
    } catch (TransformerException ex) {
      Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    }
    return duration;
  }

  public final String getSegmentTitle(Node segment) {
    String segtitle = null;
    try {
      segtitle = XPathAPI.selectSingleNode(segment, "CreationInformation/Creation/Title").getTextContent();
    } catch (TransformerException ex) {
      Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
    }
    return segtitle;
  }

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

public final String getTitle(Node creation) {
  String title = null;
  try {
    title = XPathAPI.selectSingleNode(creation, "Title").getTextContent();
  } catch (TransformerException ex) {
    Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
  }
  return title;
}

public final String getGivenName(Node creation) {
  String gname = null;
  try {
    gname = XPathAPI.selectSingleNode(creation, "Creator/Agent/Name/GivenName").getTextContent();
  } catch (TransformerException ex) {
    Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
  }
  return gname;
}

public final String getFamilyName(Node creation) {
  String fname = null;
  try {
    fname = XPathAPI.selectSingleNode(creation, "Creator/Agent/Name/FamilyName").getTextContent();
  } catch (TransformerException ex) {
    Logger.getLogger(MPEG7Manager.class.getName()).log(Level.SEVERE, null, ex);
  }
  return fname;
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

    Node seg1 = getSegment(0, 0, 0, 0);
    System.out.println("seg1=");
    System.out.println(seg1.toString());
    Node seg2 = getSegment(1, 0, 0, 0);
    System.out.println("seg2=");
    System.out.println(seg2.toString());
  }
 
  public static void main(String argv []) {
    MPEG7Manager mgr = new MPEG7Manager();
    mgr.classTest();
//  mgr.partTest();
//  mgr.test();
  }
}

