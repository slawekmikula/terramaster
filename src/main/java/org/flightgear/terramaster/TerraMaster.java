package org.flightgear.terramaster;
// WinkelTriple, Azimuthal Orthographic (globe)

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.flightgear.terramaster.dns.FlightgearNAPTRQuery;
import org.flightgear.terramaster.dns.FlightgearNAPTRQuery.HealthStats;
import org.flightgear.terramaster.dns.WeightedUrl;

public class TerraMaster {
  public static final String LOGGER_CATEGORY = "org.flightgear";
  Logger log = Logger.getLogger(LOGGER_CATEGORY);

  MapFrame frame;

  private Map<TileName, TileData> mapScenery;

  /** The service getting the tiles */
  private TileService tileService;

  private FGMap fgmap;
  private Properties props = new Properties();
  private Logger staticLogger = Logger.getLogger(TerraMaster.class.getCanonicalName());

  public TerraMaster() {
    setFgmap(new FGMap(this)); // handles webqueries
  }

  void createAndShowGUI() {
    // find our jar
    java.net.URL url = getClass().getClassLoader().getResource("maps/gshhs_l.b");
    log.log(Level.FINE, "getResource: {0}", url);
    if (url == null) {
      JOptionPane.showMessageDialog(null, "Couldn\'t load resources", "ERROR", JOptionPane.ERROR_MESSAGE);
      return;
    }

    String path = getProps().getProperty(TerraMasterProperties.SCENERY_PATH);
    if (path != null) {
      tileService.setScnPath(new File(path));
      setMapScenery(tileService.newScnMap(path));
    } else {
      setMapScenery(new HashMap<TileName, TileData>());
    }

    frame = new MapFrame(this, "TerraMaster");
    frame.restoreSettings();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
    frame.toFront();
    new VersionChecker(this).run();
  }

  public static void main(String[] args) {

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          InputStream resourceAsStream = TerraMaster.class.getClassLoader()
              .getResourceAsStream("terramaster.logging.properties");
          if (resourceAsStream != null) {
            LogManager.getLogManager().readConfiguration(resourceAsStream);
            Logger.getLogger("java.awt").setLevel(Level.OFF);
            Logger.getLogger("sun.awt").setLevel(Level.OFF);
            Logger.getLogger("javax.swing").setLevel(Level.OFF);
            Logger.getGlobal().info("Successfully configured logging");
          }
        } catch (SecurityException | IOException e1) {
          e1.printStackTrace();
        }
        TerraMaster tm = new TerraMaster();
        tm.loadVersion();
        tm.readMetaINF();
        tm.startUp();
        tm.setTileService();
        tm.initLoggers();

        tm.createAndShowGUI();
      }
    });

  }

  protected void initLoggers() {
    if (getProps().getProperty(TerraMasterProperties.LOG_LEVEL) != null) {

      Level newLevel = Level.parse(getProps().getProperty(TerraMasterProperties.LOG_LEVEL));
      staticLogger.getParent().setLevel(newLevel);
      LogManager manager = LogManager.getLogManager();
      Enumeration<String> loggers = manager.getLoggerNames();
      while (loggers.hasMoreElements()) {
        String logger = loggers.nextElement();
        Logger logger2 = manager.getLogger(logger);
        if (logger2 != null && logger2.getLevel() != null) {
          if (logger.contains("awt"))
            logger2.setLevel(Level.WARNING);
          else
            logger2.setLevel(newLevel);
        }
      }

    }
  }

  protected void startUp() {
    try {
      getProps().load(new FileReader("terramaster.properties"));
      if (getProps().getProperty(TerraMasterProperties.LOG_LEVEL) != null) {
        Logger.getGlobal().getParent().setLevel(Level.INFO);
        Logger.getLogger(TerraMaster.LOGGER_CATEGORY)
            .setLevel(Level.parse(getProps().getProperty(TerraMasterProperties.LOG_LEVEL)));
        Logger.getGlobal().getParent().setLevel(Level.INFO);
      } else {
        Logger.getGlobal().getParent().setLevel(Level.INFO);
        Logger.getLogger(TerraMaster.LOGGER_CATEGORY).setLevel(Level.INFO);
        Logger.getGlobal().getParent().setLevel(Level.INFO);
      }
    } catch (IOException e) {
      staticLogger.log(Level.WARNING, "Couldn't load properties : " + e.toString(), e);
    }
    staticLogger.info("Starting TerraMaster " + getProps().getProperty("version"));
  }

  private void readMetaINF() {
    try {
      Enumeration<URL> resources = TerraMaster.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
      while (resources.hasMoreElements()) {
        readManifest(resources.nextElement());
      }
    } catch (IOException e) {
      staticLogger.log(Level.SEVERE, e.toString(), e);
    }
  }

  public void readManifest(URL resource) {
    try {
      Manifest manifest = new Manifest(resource.openStream());
      // check that this is your manifest and do what you need or
      // get the next one
      if ("TerraMasterLauncher".equals(manifest.getMainAttributes().getValue("Main-Class"))) {
        for (Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
          staticLogger.finest(entry.getKey() + "\t:\t" + entry.getValue());
        }
      }
    } catch (IOException e) {
      staticLogger.log(Level.WARNING, e.toString(), e);
    }
  }

  public void setTileService() {
    if (tileService == null) {
      tileService = new HTTPTerraSync(this);
      tileService.start();
    }
    tileService.restoreSettings();
  }

  public synchronized TileService getTileService() {
    return tileService;
  }

  public FGMap getFgmap() {
    return fgmap;
  }

  public void setFgmap(FGMap fgmap) {
    this.fgmap = fgmap;
  }

  public Properties getProps() {
    return props;
  }

  public void setProps(Properties props) {
    this.props = props;
  }

  private void loadVersion() {
    try (InputStream is = TerraMaster.class
        .getResourceAsStream("/META-INF/maven/org.flightgear/terramaster/pom.properties")) {
      getProps().load(is);
    } catch (IOException e) {
      staticLogger.log(Level.WARNING, "Couldn't load properties : " + e.toString(), e);
    } catch (Exception e) {
      staticLogger.log(Level.WARNING, e.toString(), e);
    }
  }

  public Map<TileName, TileData> getMapScenery() {
    return mapScenery;
  }

  public void setMapScenery(Map<TileName, TileData> mapScenery) {
    this.mapScenery = mapScenery;
  }

  void showDnsStats(FlightgearNAPTRQuery flightgearNAPTRQuery) {
    int errors = 0;
    for (Entry<String, HealthStats> entry : flightgearNAPTRQuery.getStats().entrySet()) {
      HealthStats stats = entry.getValue();
      log.fine(() -> stats.toString());
      errors += stats.errors;
    }
    if (errors > 0) {
      JOptionPane.showMessageDialog(null,
          "There where errors in DNS queries. Consider enabling 8.8.8.8 or 9.9.9.9 in settings", "DNS Error",
          JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * @param completeStats
   * 
   */
  public void showStats(Map<WeightedUrl, TileResult> completeStats) {
    try {

      new DownloadResultDialog(completeStats).setVisible(true);
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error showing stats ", e);
    }
  }

}
