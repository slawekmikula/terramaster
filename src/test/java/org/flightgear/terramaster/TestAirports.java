package org.flightgear.terramaster;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestAirports {

  @Test
  public void test() {
    Airport a = new Airport("ICAO", "Test Airport");
    a.updatePosition("55.1", "12.0");
    assertEquals( 55.1, a.lat, 0);
    a.updatePosition("55.5", "12.4");
    assertEquals( 55.3, a.lat, 0);
    a.updatePosition("55.1", "12.2");
    assertEquals( 12.2, a.lon, 0);
    assertEquals( "e012n55", a.getTileName());
  }

}
