package org.flightgear.terramaster.gshhs;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.flightgear.terramaster.TerraMaster;

public class GshhsReader {
  Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);

  private int readGshhsHeader(DataInput s, GshhsHeader h) {
    int fl;
    try {
      h.id = s.readInt();
      h.setNumPoints(s.readInt()); // npoints
      fl = s.readInt();
      h.setGreenwich((fl & 1 << 16) > 0 ? true : false);
      h.setLevel((byte) (fl & 0xff));
      h.setWest(s.readInt());
      h.setEast(s.readInt());
      h.setSouth(s.readInt());
      h.setNorth(s.readInt());
      h.area = s.readInt();
      h.areaFull = s.readInt();
      h.container = s.readInt();
      h.ancestor = s.readInt();
      return h.getNumPoints();
    } catch (EOFException e) {
      return -1;
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error reading GsshhsHeader", e);
      return -1;
    }
  }

  /**
   * reads in GSHHS and builds ArrayList of polys
   * 
   * @param filename
   * @return
   */

  public List<MapPoly> newPolyList(String filename) {

    ArrayList<MapPoly> poly = new ArrayList<>();

    try {
      DataInput s = new DataInputStream(getClass().getClassLoader().getResourceAsStream(filename));
      int n = 0;
      do {
        GshhsHeader h = new GshhsHeader();
        n = readGshhsHeader(s, h);
        if (n > 0)
          poly.add(new MapPoly(s, h));
      } while (n > 0);
    } catch (Exception e) {
      log.log(Level.SEVERE, filename, e);
    }

    return poly;
  }

}
