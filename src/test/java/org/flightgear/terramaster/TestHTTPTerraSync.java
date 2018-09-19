package org.flightgear.terramaster;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JProgressBar;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestHTTPTerraSync {

  @Mock
  TerraMaster tm;
  private JProgressBar mockProgress;
  private HTTPTerraSync ts;

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
    ts = new HTTPTerraSync(tm);
    ts.start();
  }


  @Test
  public void testModels() throws InterruptedException {

    Collection<Syncable> m = new ArrayList<>();
    m.add(new ModelsSync());
    ts.sync(m, false);
    verify(tm.frame.progressBar, timeout(400000).times(1)).setVisible(false);
    assertEquals(0, ts.getSyncList().size());
    ts.quit();
    Thread.sleep(1000);
    assertEquals(false, ts.isAlive());
    File f = new File("Models/Aircraft/a310-tnt.xml");
    assertEquals(true, f.exists());
  }

  @Ignore
  @Test
  public void testAirports() throws InterruptedException {
    Collection<Syncable> m = new ArrayList<>();
    m.add(new AirportsSync());
    ts.sync(m, false);
    verify(tm.frame.progressBar, timeout(400000).times(1)).setVisible(false);
    assertEquals(0, ts.getSyncList().size());
    ts.quit();
    Thread.sleep(1000);
    assertEquals(false, ts.isAlive());
  }

  @Test
  public void testTile() throws InterruptedException {
    Collection<Syncable> m = new ArrayList<>();
    m.add(new TileName("w006n56"));
    ts.sync(m, false);
    // there must be exactly 1 call
    verify(tm.frame.progressBar, timeout(100000).times(1)).setVisible(false);
    assertEquals(0, ts.getSyncList().size());
    ts.quit();
    Thread.sleep(1000);
    assertEquals(false, ts.isAlive());
    
    File f = new File("Terrain/w010n50/w006n56/2860184.stg");
    assertEquals(true, f.exists());
  }
}
