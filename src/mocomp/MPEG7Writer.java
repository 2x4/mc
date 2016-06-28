/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/*
 * 以下の例外が起きるのを防ぐために、
 * Exception in thread "AWT-EventQueue-0" java.lang.AbstractMethodError: org.apache.xml.serialize.DOMSerializerImpl.getDomConfig()Lorg/w3c/dom/DOMConfiguration;
 *        at mocomp.MPEG7Writer.savedata(MPEG7Writer.java:110)
 *        at mocomp.MotionCompMenu$SaveAction.actionPerformed(MotionCompMenu.java:393)
 * 実行するときにXercesのライブラリをクラスパスの前の方に（他のライブラリより先に見つけられるように）指定する。
 */
package mocomp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

/**
 *
 * @author yukawa
 */
public class MPEG7Writer {

  public void savedata(File savefile) {

    String title = MotionCompApp.sharedInstance().getScoreTitle();
    String author = MotionCompApp.sharedInstance().getScoreAuthor();
    DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.JAPANESE);
//        String creationdate = formatter.format(new Date());
    
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = null;
    try {
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException ex) {
      Logger.getLogger(MPEG7Writer.class.getName()).log(Level.SEVERE, null, ex);
    }
    Document doc = db.newDocument();
    
    DOMImplementationRegistry registry = null;  
    try {
          registry = DOMImplementationRegistry.newInstance();
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
          Logger.getLogger(MPEG7Writer.class.getName()).log(Level.SEVERE, null, ex);
      }
    
    DOMImplementationLS domImplLS = (DOMImplementationLS)registry.getDOMImplementation("LS");

//    DOMImplementation domImpl = db.getDOMImplementation();
//    DOMImplementationLS domImplLS = (DOMImplementationLS) domImpl.getFeature("LS", "3.0");

//    Document doc = domImpl.createDocument("urn:mpeg:mpeg7:schema:2001", "Mpeg7", null);
    Element root = doc.createElementNS(null, "Mpeg7");
    root.setAttribute("xmlns", "urn:mpeg:mpeg7:schema:2001");
    root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
    root.setAttribute("xmlns:mpeg7", "urn:mpeg:mpeg7:schema:2001");
    root.setAttribute("xmlns:xml", "http://www.w3.org/XML/1998/namespace");
    root.setAttribute("xsi:schemaLocation", "urn:mpeg:mpeg7:schema:2001 Mpeg7-2001.xsd");
    doc.appendChild(root);
    root.appendChild(createMediaDescription(doc));
    Element ce = doc.createElement("Description");
    ce.setAttribute("xsi:type", "ContentEntityType");
    root.appendChild(ce);
    Element mc = doc.createElement("MultimediaContent");
    mc.setAttribute("xsi:type", "VideoType");
    ce.appendChild(mc);
    Element video = doc.createElement("Video"); //Mpeg7/Description/MultimediaContent/Video
    mc.appendChild(video);
    String agn = null;
    String afn = null;
    if (author.length() > 0) {
      String[] an = author.split("\\s");
      if (an.length > 0) {
        afn = an[0];
      }
      if (an.length > 1) {
        agn = an[1];
      }
    }
    Element bci = createBuyofuCreationInformation(doc, null, agn, afn, null, title, title);
    video.appendChild(bci);
    Element td = doc.createElement("TemporalDecomposition"); // outer
    video.appendChild(td);

    for (ScorePanel sp : MotionCompApp.sharedInstance().getScoreList()) {
      Element vseg = doc.createElement("VideoSegment");
      String pcode = sp.getPartCode();
      vseg.setAttribute("id", pcode);
      td.appendChild(vseg);
      Element td2 = doc.createElement("TemporalDecomposition"); // inner
      td2.setAttribute("id", pcode);
      vseg.appendChild(td2);
      // 舞踊符の定義
      for (MotionCodePanel mcp : sp.getMotionCodeList()) {
        td2.appendChild(createBuyofuSegment(doc, mcp, 30));
      }
    }
    LSOutput out = domImplLS.createLSOutput();
    LSSerializer ser = domImplLS.createLSSerializer();
    System.out.println("lsSer="+ser.toString());
    //out.setEncoding("Shift_JIS");
    //
    try {
      out.setByteStream(new FileOutputStream(savefile)); //        lsOutput.setByteStream(System.out);
    } catch (FileNotFoundException ex) {
      Logger.getLogger(MPEG7Writer.class.getName()).log(Level.SEVERE, null, ex);
    }
    ser.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
    ser.write(doc, out);

  }
  private Element createMediaDescription(Document document) {
    Element desc = document.createElement("Description");
    desc.setAttribute("xsi:type", "MediaDescriptionType");
    Element mi = document.createElement("MediaInformation");
    desc.appendChild(mi);
    Element header = document.createElement("Header");
    mi.appendChild(header);
    Element creator = document.createElement("Creator");
    header.appendChild(creator);
    Element role = document.createElement("Role");
    creator.appendChild(role);
    Element name = document.createElement("Name");
    role.setAttribute("href", "creatorCS");
    role.appendChild(name);
    name.setTextContent("ContentsDescriptor");
    creator.appendChild(role);
    Element agent = document.createElement("Agent");
    creator.appendChild(agent);

    name = document.createElement("Name");
//    Element gn = document.createElement("GivenName");
    Element fn = document.createElement("FamilyName");
//    name.appendChild(gn);
//    gn.setTextContent("Takaaki");
    name.appendChild(fn);
    fn.setTextContent(MotionCompApp.sharedInstance().getScoreAuthor());
    agent.appendChild(name);
    creator.appendChild(agent);
    Element inst = document.createElement("Instrument");
    creator.appendChild(inst);
    Element tool = document.createElement("Tool");
    name = document.createElement("Name");
    name.setTextContent("Motion Composer");
    tool.appendChild(name);
    inst.appendChild(tool);
    creator.appendChild(inst);
    Element setting = document.createElement("Setting");
    setting.setAttribute("name", "rate");
    setting.setAttribute("value", "30");
    inst.appendChild(setting);
    setting = document.createElement("Setting");
    setting.setAttribute("name", "sensors");
    setting.setAttribute("value", "15");
    inst.appendChild(setting);

    return desc;
  }

  private Element createBuyofuCreationInformation(Document document, String agentname, String agname, String afname, String email, String title, String abstrct) {
    Element cinfo = document.createElement("CreationInformation");
    Element header = document.createElement("Header");
    header.setAttribute("xsi:type", "DescriptionMetadataType");
    Element creation = document.createElement("Creation");
    cinfo.appendChild(creation);
    Element creator = document.createElement("Creator");
    creation.appendChild(creator);
    Element role = document.createElement("Role");
    creator.appendChild(role);

    role.setAttribute("href", "urn:ricoh:mmVISION:RoleCS:6");
    Element name = document.createElement("Name");
    role.appendChild(name);
    name.setTextContent("ContentsDescriptor");
//    if (agentname != null || afname != null || agname != null || email != null) {
    Element agent = document.createElement("Agent");
    creator.appendChild(agent);
//      if (agname != null || afname != null || email != null) {
    Element contact = document.createElement("Contact");
    agent.appendChild(contact);
    contact.setAttribute("xsi:type", "PersonType");
//        if (agname != null || afname != null) {
    name = document.createElement("Name");
    contact.appendChild(name);
//          if (agname != null) {
    Element gn = document.createElement("GivenName");
    name.appendChild(gn);
    gn.setTextContent(agname);
//          }
//          if (afname != null) {
    Element fn = document.createElement("FamilyName");
    name.appendChild(fn);
    fn.setTextContent(afname);
//          }
//          if (title != null) {
    Element btitle = document.createElement("Title");
    creation.appendChild(btitle);
    btitle.setTextContent(MotionCompApp.sharedInstance().getScoreTitle());
//          }
//        }
//      }
//    }
    return cinfo;
  }

  public Element createBuyofuSegment(Document doc, String id, String tstr, String abs, int timepoint, int duration, int srate) {
    Element seg = doc.createElement("VideoSegment");
    seg.setAttribute("id", id);
    Element ci = doc.createElement("CreationInformation");
    seg.appendChild(ci);
    Element creation = doc.createElement("Creation");
    ci.appendChild(creation);
    Element title = doc.createElement("Title");
    title.setTextContent(tstr);
    creation.appendChild(title);
    Element abstrct = doc.createElement("Abstract");
    creation.appendChild(abstrct);
    Element fta = doc.createElement("FreeTextAnnotation");
    fta.setTextContent(abs);
    abstrct.appendChild(fta);
    Element mt = doc.createElement("MediaTime");
    seg.appendChild(mt);
    Element mtp = doc.createElement("MediaTimePoint");
    mtp.setTextContent(String.format("%dF%d", timepoint, srate));
    mt.appendChild(mtp);
    Element md = doc.createElement("MediaDuration");
    md.setTextContent(String.format("PT%dN%dF", duration, srate));
    mt.appendChild(md);
    return seg;
  }
  
    public Element createBuyofuSegment(Document doc, MotionCodePanel mcp, int srate) {
        String mcode = mcp.getMotionCode();
        String tstr = mcp.getTitle();
        String abstrct = mcp.getAbstract();
        int bp = mcp.getFrameIndex();
        int len = mcp.getLength();
        return createBuyofuSegment(doc, mcode, tstr, abstrct, bp, len, srate);
    }
}
