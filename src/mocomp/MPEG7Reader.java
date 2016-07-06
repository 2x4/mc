//			    MP7Reader.java
//
//	       Copyright (C) 2009 Takashi Yukawa
//
//		    This java source file conforms
//		GNU GENERAL PUBLIC LICENSE Version 2.
//
// Author:  Takashi Yukawa <yukawa@fukushima-nct.ac.jp>
// Created: May.25, 2009

package mocomp;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Mpeg7形式で保存されている舞踊譜を読み込むためのクラス．
 * @version 1.00 05/25/2009
 * @author Takashi Yukawa
 * @see MotionCompMenu#openActionPerformed
 */
public class MPEG7Reader extends DefaultHandler {
  private int level = 0;
  private String partName = null;
  private String motioncode = null;
  private String lastqname = null;
  private int mediatime;
  private int duration;
  private Boolean inMediaTimePoint = false;
  private Boolean inMediaDuration = false;
  private Boolean inTitle = false;
  private Boolean inName = false;
  private Boolean inFamilyName = false;
  private Boolean inGivenName = false;
  private Boolean inCreationInformation = false;
  private ScorePanel sp;
  private String givenName;
  private String familyName;
  /**
   * 舞踊譜のタイトル
   */
  private String title;

  /**
   * 舞踊譜の作者名
   */
  private String author;

  /**
   * 引数で与えたMpeg7ファイルから舞踊符を読み込み，スコアパネルに貼りつける．
   * @param file Mpeg7ファイルのファイル名
   */
    public MPEG7Reader(File file) {
        BufferedInputStream input = null;
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(true);
            input = new BufferedInputStream(new FileInputStream(file));
            SAXParser saxParser = saxParserFactory.newSAXParser();
            InputSource inputSource = new InputSource(input);
            saxParser.parse(inputSource, this);
        } catch (IOException | SAXException | ParserConfigurationException ex) {
            Logger.getLogger(MPEG7Reader.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                input.close();
            } catch (IOException ex) {
                Logger.getLogger(MPEG7Reader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
/*
  public void startDocument() {
    System.out.println("startDocument");
  }
*/
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
//    System.out.println("startElement : " + qName + "("+level+")");
        if (qName.equals("VideoSegment")) {
            level++;
            if (level == 1) {
                partName = atts.getValue("id");
                sp = MotionCompApp.sharedInstance().getScorePanel(partName);
            } else if (level == 2) {
                motioncode = atts.getValue("id");
            }
        } else if (qName.equals("MediaTimePoint")) {
            inMediaTimePoint = true;
        } else if (qName.equals("MediaDuration")) {
            inMediaDuration = true;
        } else if (qName.equals("Name")) {
            inName = true;
        } else if (qName.equals("FamilyName")) {
            inFamilyName = true;
        } else if (qName.equals("GivenName")) {
            inGivenName = true;
        } else if (qName.equals("Title")) {
            inTitle = true;
        } else if (qName.equals("CreationInformation")) {
            inCreationInformation = true;
        }
        lastqname = qName;
    }

    public void characters(char[] ch, int start, int length) {
        String str = new String(ch, start, length);
        if (inMediaTimePoint && lastqname.equals("MediaTimePoint")) {
            String[] ss = str.split("F");
//      System.out.println(ss[0]);
            mediatime = Integer.parseInt(ss[0]);
        } else if (inMediaDuration && lastqname.equals("MediaDuration")) {
            String[] ss = str.split("[TN]");
//System.out.println("str="+str + ", ss[1]="+ss[1]);
            duration = Integer.parseInt(ss[1]);
        } else if (inName) {
            if (inGivenName) {
                givenName = str;
            } else if (inFamilyName) {
                familyName = str;
            }
        } else if (inCreationInformation && inTitle) {
            title = str;
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) {
//    System.out.println("endElement : " + qName);
        if (qName.equals("VideoSegment")) {
            level--;
            if (level == 1) {
//          System.out.println("addNewCodePanel("+partName+","+motioncode+","+mediatime+","+duration+")");
                sp.addNewCodePanel(motioncode, mediatime, duration);
            }
        } else if (qName.equals("CreationInformation")) {
            inCreationInformation = false;
        } else if (qName.equals("MediaTimePoint")) {
            inMediaTimePoint = false;
        } else if (qName.equals("MediaDuration")) {
            inMediaDuration = false;
        } else if (qName.equals("Name")) {
            inName = false;
            if (givenName != null && familyName != null && givenName.length()>0 && familyName.length()>0) {
                author = givenName + " " + familyName;
            } else if (givenName != null && givenName.length() > 0) {
                author = givenName;
            } else if (familyName != null && familyName.length() > 0) {
                author = familyName;
            }
        } else if (qName.equals("FamilyName")) {
            inFamilyName = false;
        } else if (qName.equals("GivenName")) {
            inGivenName = false;
        } else if (qName.equals("Title")) {
            inTitle = false;
        }
    }
/*
  public void endDocument() {
    System.out.println("endDocument");
  }
 */
  /**
   * 引数で与えた文字列が部位コードに含まれているかどうかを返す
   * @param 部位コードかどうか調べる文字列
   * @return 引数は部位コードのときはtrue，それ以外だったらfalse
    private final boolean isPartCode(String t) {
    for (int i = 0; i < MotionComp.mcml.getPartSize(); i++) {
      if (t.equals(MotionComp.mcml.getPartCode(i))) return true;
    }
    return false;
  }
 */
  /**
   * 読み込んだDDSファイルのヘッダに記述されている作者名を返す
   * @return 読み込んだDDSファイルのヘッダに記述されている作者名
   */
    public final String getAuthor() {
        return author;
    }

  /**
   * 読み込んだDDSファイルのヘッダに記述されているタイトルを返す
   * @return 読み込んだDDSファイルのヘッダに記述されているタイトル
   */
    public final String getTitle() {
        return title;
    }
}
