package org.flightgear.terramaster;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
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

public class TestMapFrame {

  @Mock
  TerraMaster tm;
  private JProgressBar mockProgress;
  private FGMap fgmap = null;
  private Graphics g;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    tm.frame = mock(MapFrame.class);
    mockProgress = mock(JProgressBar.class);
    tm.frame.progressBar = mockProgress;
    tm.frame.butStop = mock(JButton.class);
    tm.getProps().setProperty(TerraMasterProperties.DNS_GOOGLE, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.DNS_GCA, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.LOG_LEVEL, Level.ALL.toString());
    fgmap = new FGMap(tm);
    doReturn(fgmap).when(tm).getFgmap();
    tm.log =   Logger.getAnonymousLogger();
    BufferedImage offScreen = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);

    g = offScreen.getGraphics();
  }
  @Test
  public void test() {
    MapFrame mf = new MapFrame(tm, "");
    mf.setSize(100, 100);
    mf.storeSettings();
    mf.setSize(110, 110);
    mf.restoreSettings();
    assertEquals(100, mf.getSize().getWidth(), 0);
  }

}
