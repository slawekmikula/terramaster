package org.flightgear.terramaster;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JProgressBar;

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

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    tm.frame = mock(MapFrame.class);
    mockProgress = mock(JProgressBar.class);
    tm.frame.progressBar = mockProgress;
    tm.frame.butStop = mock(JButton.class);
    tm.props.setProperty(TerraMasterProperties.DNS_GOOGLE, Boolean.TRUE.toString());
    tm.props.setProperty(TerraMasterProperties.DNS_GCA, Boolean.TRUE.toString());
    tm.props.setProperty(TerraMasterProperties.LOG_LEVEL, Level.ALL.toString());
    tm.fgmap = new FGMap(tm);
    tm.log =   Logger.getAnonymousLogger();
    BufferedImage offScreen = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

    g = offScreen.getGraphics();
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
