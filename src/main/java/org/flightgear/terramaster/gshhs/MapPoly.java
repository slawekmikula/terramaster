package org.flightgear.terramaster.gshhs;

import java.awt.Polygon;
import java.io.DataInput;
import java.io.IOException;

public class MapPoly extends Polygon {

  private transient GshhsHeader gshhsHeader;

  public synchronized GshhsHeader getGshhsHeader() {
    return gshhsHeader;
  }

  public synchronized void setGshhsHeader(GshhsHeader gshhsHeader) {
    this.gshhsHeader = gshhsHeader;
  }

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
      float x = (float) s.readInt() / 10000;
      float y = -(float) s.readInt() / 10000;
      if ((h.isGreenwich() && x > 27000) || h.getWest() > 180000000)
        x -= 36000;
      if (x != 0 && y != 0) {
        addPoint((int) x, (int) y);
      }
      else {
        System.out.println("Ignored");
      }
    }
  }

  public double getNumPoints() {
    return gshhsHeader.getNumPoints();
  }

  public double getArea() {
    return gshhsHeader.area;
  }

}
