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

    public ThinBevelBorder(int bevelType) {
        super(bevelType);
    }

    public Insets getBorderInsets(Component c) {
        return new Insets(1, 1, 1, 1);
    }

    protected void paintRaisedBevel(Component c, Graphics g, int x, int y,
				  int width, int height) {
        Color oldColor = g.getColor();

        g.translate(x, y);

        g.setColor(getHighlightOuterColor(c));
        g.drawLine(0, 0, 0, height-1);
        g.drawLine(1, 0, width-1, 0);

        g.setColor(getShadowOuterColor(c));
        g.drawLine(1, height-1, width-1, height-1);
        g.drawLine(width-1, 1, width-1, height-2);

        g.translate(-x, -y);
        g.setColor(oldColor);
    }

    protected void paintLoweredBevel(Component c, Graphics g, int x, int y,
				   int width, int height)  {
        Color oldColor = g.getColor();

        g.translate(x, y);

        g.setColor(getShadowInnerColor(c));
        g.drawLine(0, 0, 0, height-1);
        g.drawLine(1, 0, width-1, 0);

        g.setColor(getHighlightOuterColor(c));
        g.drawLine(1, height-1, width-1, height-1);
        g.drawLine(width-1, 1, width-1, height-2);

        g.translate(-x, -y);
        g.setColor(oldColor);
    }
}
