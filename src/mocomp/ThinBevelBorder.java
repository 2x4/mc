package mocomp;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Component;
import javax.swing.border.*;

/**
 * 縁の細いBevelBorder
 */
public class ThinBevelBorder extends BevelBorder {
  //  public static final int RAISED  = 0;    /** Raised bevel type. */
  //  public static final int LOWERED = 1;    /** Lowered bevel type. */

  //  protected int bevelType;
  //  protected Color highlightOuter;
  //  protected Color highlightInner;
  //  protected Color shadowInner;
  //  protected Color shadowOuter;

  public ThinBevelBorder(int bevelType) {
    super(bevelType);
  }

  //  public ThinBevelBorder(int bevelType, Color highlight, Color shadow) {
  //    this(bevelType, highlight.darker(), highlight, shadow, shadow.brighter());
  //  }

  //  public ThinBevelBorder(int bevelType, Color highlightOuter, Color highlightInner, Color shadowOuter, Color shadowInner) {
  //    super(bevelType, highlightOuter, highlightInner, shadowOuter, shadowInner);
  //  }

  //  public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
  //    if (bevelType == RAISED) {
  //      paintRaisedBevel(c, g, x, y, width, height);
  //    } else if (bevelType == LOWERED) {
  //      paintLoweredBevel(c, g, x, y, width, height);
  //    }
  //  }

  public Insets getBorderInsets(Component c) {
    return new Insets(1, 1, 1, 1);
  }

  //  public Color getHighlightOuterColor(Component c) {
  //    return highlightOuter != null? highlightOuter : 
  //      c.getBackground().brighter().brighter();
  //  }

  //  public Color getHighlightInnerColor(Component c) {
  //    return highlightInner != null? highlightInner :
  //      c.getBackground().brighter();
  //  }

  //  public Color getShadowInnerColor(Component c) {
  //    return shadowInner != null? shadowInner :
  //      c.getBackground().darker();
  //  }

  //  public Color getShadowOuterColor(Component c) {
  //    return shadowOuter != null? shadowOuter :
  //      c.getBackground().darker().darker();
  //  }

  //  public int getBevelType() { return bevelType; }

  //  public boolean isBorderOpaque() { return true; }

  protected void paintRaisedBevel(Component c, Graphics g, int x, int y,
				  int width, int height) {
    Color oldColor = g.getColor();
    int h = height;
    int w = width;

    g.translate(x, y);

    g.setColor(getHighlightOuterColor(c));
    g.drawLine(0, 0, 0, h-1);
    g.drawLine(1, 0, w-1, 0);

    //    g.setColor(getHighlightInnerColor(c));
    //    g.drawLine(1, 1, 1, h-2);
    //    g.drawLine(2, 1, w-2, 1);

    g.setColor(getShadowOuterColor(c));
    g.drawLine(1, h-1, w-1, h-1);
    g.drawLine(w-1, 1, w-1, h-2);

    //    g.setColor(getShadowInnerColor(c));
    //    g.drawLine(2, h-2, w-2, h-2);
    //    g.drawLine(w-2, 2, w-2, h-3);

    g.translate(-x, -y);
    g.setColor(oldColor);

  }

  protected void paintLoweredBevel(Component c, Graphics g, int x, int y,
				   int width, int height)  {
    Color oldColor = g.getColor();
    int h = height;
    int w = width;

    g.translate(x, y);

    g.setColor(getShadowInnerColor(c));
    g.drawLine(0, 0, 0, h-1);
    g.drawLine(1, 0, w-1, 0);

    //    g.setColor(getShadowOuterColor(c));
    //    g.drawLine(1, 1, 1, h-2);
    //    g.drawLine(2, 1, w-2, 1);

    g.setColor(getHighlightOuterColor(c));
    g.drawLine(1, h-1, w-1, h-1);
    g.drawLine(w-1, 1, w-1, h-2);

    //    g.setColor(getHighlightInnerColor(c));
    //    g.drawLine(2, h-2, w-2, h-2);
    //    g.drawLine(w-2, 2, w-2, h-3);

    g.translate(-x, -y);
    g.setColor(oldColor);
  }
}
