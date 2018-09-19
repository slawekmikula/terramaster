package org.flightgear.terramaster;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestMapPanel {

  @Test
  public void test() {
    MapPanel mp = new MapPanel(null);
//    assertEquals(mp.getBox(5, 56).getBounds().getMaxX(),mp.getBox(6, 56).getBounds().getMaxX(),0.1);
    
    System.out.println(mp.getBox(5, 56).getBounds());
    System.out.println(mp.getBox(6, 56).getBounds());
  }

}
