package org.flightgear.terramaster;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.awt.AWTException;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
  private Properties props = new Properties();
  private Map<TileName, TileData> mapScenery = new HashMap<>();
  private Robot r;

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    tm.frame = mock(MapFrame.class);
    mockProgress = mock(JProgressBar.class);
    tm.frame.progressBar = mockProgress;
    tm.frame.butStop = mock(JButton.class);
    HTTPTerraSync sync = new HTTPTerraSync(tm);
    mapScenery = sync.newScnMap(new File(".").getAbsolutePath());
    sync.start();
    doReturn(sync ).when(tm).getTileService();
    doReturn(props ).when(tm).getProps();
    tm.getProps().setProperty(TerraMasterProperties.DNS_GOOGLE, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.DNS_GCA, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.LOG_LEVEL, Level.ALL.toString());    
    fgmap = new FGMap(tm);
    fgmap.addAirport(new Airport("BLA", "BLABLA Airport"));
    doReturn(fgmap).when(tm).getFgmap();
    doReturn(mapScenery).when(tm).getMapScenery();
    tm.log =   Logger.getAnonymousLogger();    
  }
  @Test
  public void testSettings() {
    MapFrame mf = new MapFrame(tm, "");
    mf.setSize(100, 100);
    mf.storeSettings();
    mf.setSize(110, 110);
    mf.restoreSettings();
    assertEquals(100, mf.getSize().getWidth(), 0);
  }

  
  @Test
  public void testShowing() throws AWTException {
    MapFrame mf = new MapFrame(tm, "");
    mf.setSize(1000, 500);
    mf.setVisible(true);
    mf.map.setProjection(false);
    r = new Robot();
    r.setAutoDelay(1000);
    mouseMove(400, 300);
    r.mousePress(InputEvent.BUTTON3_DOWN_MASK);
    mouseMove(310, 320);
    mouseMove(300, 300);
    mouseMove(350, 320);
    r.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
    r.mouseWheel(-6);
    mouseMove(490, 320);
    r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    mouseMove(520, 320);
    r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    Collection<TileName> selection = mf.map.getSelection();
    List<Syncable> synch = new ArrayList<>();
    synch.addAll(selection);
    assertEquals(3, selection.size());
    tm.getTileService().sync(synch, true);
    for (int i = 500; i < 540; i+=4) {
      mouseMove(i, 320);      
    }
    for (int i = 540; i > 500; i-=4) {
      mouseMove(i, 320);      
    }
    r.delay(4000);
    mouseMove(520, 320);
    r.keyPress(KeyEvent.VK_ADD);
    r.keyPress(KeyEvent.VK_PLUS);
    r.keyPress(KeyEvent.VK_MINUS);
    r.keyPress(KeyEvent.VK_SUBTRACT);
    r.keyPress(KeyEvent.VK_LEFT);
    r.keyPress(KeyEvent.VK_RIGHT);
    r.keyPress(KeyEvent.VK_UP);
    r.keyPress(KeyEvent.VK_DOWN);
    r.mouseWheel(6);
    mf.setVisible(false);
  }


  private void mouseMove(int x, int y) {
    for (int count = 0; (MouseInfo.getPointerInfo().getLocation().getX() != x
        || MouseInfo.getPointerInfo().getLocation().getY() != y) && count < 100; count++) {
      r.mouseMove(x, y);
    }
  }
}
