package org.flightgear.terramaster;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.flightgear.terramaster.gshhs.GshhsReader;
import org.flightgear.terramaster.gshhs.MapPoly;
import org.junit.Test;

public class TestGSHHS {

  @Test
  public void test() {
    GshhsReader h = new GshhsReader();
    ArrayList<MapPoly> newPolyList = h.newPolyList("maps/gshhs_l.b");
    assertEquals(10717,newPolyList.size());
  }

}
