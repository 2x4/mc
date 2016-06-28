package mocomp;

import java.awt.*;

public class GridBagConstraints2 extends GridBagConstraints {
  public GridBagConstraints2(int gridx, int gridy,
			     int gridwidth, int gridheight,
                             double weightx, double weighty,
			     int anchor, int fill,
                             Insets insets, int ipadx, int ipady) {
    this.gridx = gridx;
    this.gridy = gridy;
    this.gridwidth = gridwidth;
    this.gridheight = gridheight;
    this.fill = fill;
    this.ipadx = ipadx;
    this.ipady = ipady;
    this.insets = insets;
    this.anchor  = anchor;
    this.weightx = weightx;
    this.weighty = weighty;
  }

  public GridBagConstraints2(int gridx, int gridy,
			     int gridwidth, int gridheight,
                             double weightx, double weighty,
			     int anchor, int fill) {
    this(gridx, gridy, gridwidth, gridheight, weightx, weighty, anchor, fill,
	 new Insets(0, 0, 0, 0), 0, 0);
  }

  public String toString() {
    return  ": " + gridx + "," + gridy+ "," + gridwidth + "," + gridheight;    //NORES
  }
}
