package org.flightgear.terramaster;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JProgressBar;

import org.flightgear.terramaster.MapPanel.SortablePoint;
import org.flightgear.terramaster.gshhs.GshhsReader;
import org.flightgear.terramaster.gshhs.MapPoly;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestMapPanel {

  @Mock
  TerraMaster tm;
  private JProgressBar mockProgress;
  private Graphics g;
  private Properties props = new Properties();

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    tm.frame = mock(MapFrame.class);
    mockProgress = mock(JProgressBar.class);
    tm.frame.progressBar = mockProgress;
    tm.frame.butStop = mock(JButton.class);
    doReturn(props ).when(tm).getProps();
    tm.getProps().setProperty(TerraMasterProperties.DNS_GOOGLE, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.DNS_GCA, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.LOG_LEVEL, Level.ALL.toString());
    FGMap map = new FGMap(tm);
    doReturn(map ).when(tm).getFgmap();
    tm.log =   Logger.getAnonymousLogger();
    BufferedImage offScreen = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

    g = offScreen.getGraphics();
  }
  
  @Test
  public void testMap() {
    MapPanel mp = new MapPanel(tm);
    HashMap<MapPanel.SortablePoint, String> m = new HashMap<>();
    m.put(mp.new SortablePoint( new Point(1, 1), 2), "D");
    m.put(mp.new SortablePoint( new Point(1, 2), 2), "D");
    SortablePoint k1 = mp.new SortablePoint( new Point(1, 1), 2);
    m.put(k1, "D");
    m.put(k1, "D");
    
    assertEquals(2, m.size());
  }


  @Test
  public void testList() {
    MapPanel mp = new MapPanel(tm);
    ArrayList<MapPanel.SortablePoint> m = new ArrayList<>();
    m.add(mp.new SortablePoint( new Point(1, 1), 2));
    m.add(mp.new SortablePoint( new Point(1, 2), 2));
    m.add(mp.new SortablePoint( new Point(1, 1), 2));
    
    String s = mp.new SortablePoint( new Point(1, 1), 2).toString();
    
    assertTrue(s.matches("[0-9]* java\\.awt\\.Point\\[x=[0-9]*,y=[0-9]*\\]"));
    
    assertEquals(3, m.size());
    
    Collections.sort(m);
  }

  @Test
  public void testList2() {
    MapPanel mp = new MapPanel(tm);
    SortablePoint s1 = mp.new SortablePoint( new Point(1, 1), 2);
    SortablePoint s2 = mp.new SortablePoint( new Point(1, 1), 3);
    SortablePoint s3 = mp.new SortablePoint( new Point(1, 2), 2);
    SortablePoint s4 = mp.new SortablePoint( null, 2);
        
    assertFalse(s1.equals(""));
    assertFalse(s1.equals(null));
    assertFalse(s1.equals(s2));
    assertFalse(s1.equals(s3));
    assertFalse(s1.equals(s4));
    assertFalse(s4.equals(s1));
    assertTrue(s1.equals(s1));
    
  }

  @Test
  public void test() {
    MapPanel mp = new MapPanel(tm);
    mp.passFrame(new MapFrame(tm, ""));
    mp.setSize(100, 100);
    ArrayList<MapPoly> newPolyList = new GshhsReader().newPolyList("maps/gshhs_l.b");
    assertThat(newPolyList.size(), is(not(0)));
    mp.passPolys(newPolyList);
    mp.toggleProj();

    Double screen2geo = mp.screen2geo(new Point(20, 20));
    mp.toggleProj();

    Double screen2geo2 = mp.screen2geo(new Point(20, 20));
    mp.toggleProj();
    Double screen2geo3 = mp.screen2geo(new Point(20, 20));
    assertEquals(screen2geo, screen2geo3);
  }

  @Test
  public void test2() {
    MapPanel mp = new MapPanel(tm);
    mp.passFrame(tm.frame);
    mp.setSize(100, 100);
    ArrayList<MapPoly> newPolyList = new GshhsReader().newPolyList("maps/gshhs_l.b");
    assertThat(newPolyList.size(), is(not(0)));
    ArrayList<MapPoly> borders = new GshhsReader().newPolyList("maps/wdb_borders_l.b");
    assertThat(newPolyList.size(), is(not(0)));
    mp.passPolys(newPolyList);
    mp.passBorders(borders);
    mp.reset();
    mp.paintComponent(g);
  }
}
