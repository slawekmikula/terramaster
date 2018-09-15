package org.flightgear.terramaster.gshhs;

import java.awt.Polygon;
import java.io.DataInput;
import java.io.IOException;

public class MapPoly extends Polygon {

  public transient GshhsHeader gshhsHeader;
  public byte level;

  public MapPoly() {
  }

  /**
   * reads raw GSHHS format
   * 
   * @param s
   * @param h
   * @throws IOException 
   * @throws Exception
   */

  public MapPoly(DataInput s, GshhsHeader h) throws IOException {
    gshhsHeader = h;
    level = h.getLevel();

    for (int i = 0; i < h.getNumPoints(); ++i) {
      float x = (float)s.readInt() / 10000;
      float y = -(float)s.readInt() / 10000;
      if ((h.isGreenwich() && x > 27000) || h.getWest() > 180000000)
        x -= 36000;
      addPoint((int) x, (int) y);
    }
  }

}
