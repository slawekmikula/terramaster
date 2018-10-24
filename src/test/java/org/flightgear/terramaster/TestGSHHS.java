package org.flightgear.terramaster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.*;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flightgear.terramaster.gshhs.GshhsReader;
import org.flightgear.terramaster.gshhs.MapPoly;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Test;

public class TestGSHHS {

  private static final String BORDERS = "maps/wdb_borders_i.b";
  private static final String CONTINENTS = "maps/gshhs_l.b";

  @Test
  public void test() {
    GshhsReader h = new GshhsReader();
    List<MapPoly> newPolyList = h.newPolyList(CONTINENTS);
    assertEquals(10717, newPolyList.size());
    Comparator<MapPoly> byLevel = (MapPoly m1, MapPoly m2) -> Byte.compare(m1.level, m2.level);
    Collections.sort(newPolyList, byLevel);
    System.out.println(newPolyList);
    List<MapPoly> filter = newPolyList.stream().filter(p -> p.level < 2 || p.level >= 5).collect(Collectors.toList());
    for (MapPoly mapPoly : filter) {
      System.out.println(mapPoly.level + "\t" + mapPoly.npoints);
    }
  }

  @Test
  public void test2() {
    GshhsReader h = new GshhsReader();
    List<MapPoly> newPolyList = h.newPolyList(BORDERS);
    assertEquals(1125, newPolyList.size());
    Comparator<MapPoly> byLevel = (MapPoly m1, MapPoly m2) -> Byte.compare(m1.level, m2.level);
    Collections.sort(newPolyList, byLevel);
    System.out.println(newPolyList);
    List<MapPoly> filter = newPolyList.stream().filter(p -> p.level < 2 || p.level >= 5).collect(Collectors.toList());
    for (MapPoly mapPoly : filter) {
      System.out.println(mapPoly.level + "\t" + mapPoly.npoints);
    }
  }

  @Test
  public void test3() {
    GshhsReader h = new GshhsReader();
    List<MapPoly> newPolyList = h.newPolyList(BORDERS);
    assertEquals(1125, newPolyList.size());
    Comparator<MapPoly> byLevel = (MapPoly m1, MapPoly m2) -> Byte.compare(m1.level, m2.level);
    Collections.sort(newPolyList, byLevel);
    Rectangle maxBounds = new Rectangle(-180000, -90000, 360000, 180000);
    for (MapPoly mapPoly : newPolyList) {
      assertEquals(mapPoly.getGshhsHeader().getNumPoints(), mapPoly.getNumPoints(), 0);
      assertTrue(maxBounds.x < mapPoly.getBounds().x);
      assertTrue(maxBounds.y < mapPoly.getBounds().y);
      assertTrue(maxBounds.x + maxBounds.width > mapPoly.getBounds().x + mapPoly.getBounds().width);
      assertTrue(maxBounds.y + maxBounds.height > mapPoly.getBounds().y + mapPoly.getBounds().height);
      assertTrue(maxBounds.y < mapPoly.getBounds().y);
      assertTrue(maxBounds.x < mapPoly.getBounds().x);
//      if(!maxBounds.contains(mapPoly.getBounds()))
//      {
//        System.out.println(maxBounds.intersection(mapPoly.getBounds()));
//      }
//      assertTrue(mapPoly.getBounds().toString(), maxBounds.contains(mapPoly.getBounds()));
    }
    List<MapPoly> filter = newPolyList.stream().filter(p -> p.level < 2 || p.level >= 5).collect(Collectors.toList());
    for (MapPoly mapPoly : filter) {
      System.out.println(mapPoly.level + "\t" + mapPoly.npoints);
    }
  }

  @Test
  public void test4() {
    GshhsReader h = new GshhsReader();
    List<MapPoly> newPolyList = h.newPolyList(CONTINENTS);
    assertEquals(10717, newPolyList.size());
    Comparator<MapPoly> byLevel = (MapPoly m1, MapPoly m2) -> Byte.compare(m1.level, m2.level);
    Collections.sort(newPolyList, byLevel);
    System.out.println(newPolyList);
    Rectangle maxBounds = new Rectangle(180, 90, 360, 180);
    for (MapPoly mapPoly : newPolyList) {
      assertTrue(mapPoly.getBounds().toString(), maxBounds.contains(mapPoly.getBounds()));
    }
    List<MapPoly> filter = newPolyList.stream().filter(p -> p.level < 2 || p.level >= 5).collect(Collectors.toList());
    for (MapPoly mapPoly : filter) {
      System.out.println(mapPoly.level + "\t" + mapPoly.npoints);
    }
  }
}
