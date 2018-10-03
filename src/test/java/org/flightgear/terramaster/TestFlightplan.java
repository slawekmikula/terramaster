package org.flightgear.terramaster;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JProgressBar;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestFlightplan {
  @Mock
  TerraMaster tm;
  private JProgressBar mockProgress;
  private HTTPTerraSync ts;
  File scnDir = null;
  private Map<TileName, TileData> mapScenery = new HashMap<>();
  private Properties props = new Properties();

  @Before
  public void initMocks() throws IOException {
    MockitoAnnotations.initMocks(this);
    tm.frame = mock(MapFrame.class);
    mockProgress = mock(JProgressBar.class);
    tm.frame.progressBar = mockProgress;
    tm.frame.butStop = mock(JButton.class);
    doReturn(props).when(tm).getProps();
    tm.getProps().setProperty(TerraMasterProperties.DNS_GOOGLE, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.DNS_GCA, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.LOG_LEVEL, Level.ALL.toString());
    doReturn(mapScenery).when(tm).getMapScenery();
    System.out.println(tm.getMapScenery().getClass().getName());
    tm.log = Logger.getAnonymousLogger();
    scnDir = Files.createTempDirectory("").toFile();

    ts = new HTTPTerraSync(tm);
    ts.start();
    ts.setScnPath(scnDir);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        scnDir.delete();
      }
    });
  }

  @Test
  public void test() {
    FlightPlan f = new FlightPlan(tm);
    // f.setVisible(true);

  }

}
