package org.flightgear.terramaster;

import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The data associated with a tile.
 * 
 * @author keith.paterson
 *
 */

public class TileData {
  private Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
  /** The square drawn on the map. */
  public Polygon poly;
  /** Flags indicating what the tiles contain. Used for the mouse over. */
  private boolean terrain = false;
  private boolean objects = false;
  private boolean buildings = false;
  private boolean roads = false;

  private boolean pylons = false;
  private File dirTerrain = null;
  private File dirObjects = null;
  private File dirBuildings = null;
  private File dirRoads = null;
  private File dirPylons = null;

  public TileData() {
  }

  public void delete() {
    if (terrain) {
      deltree(dirTerrain);
    }

    if (objects) {
      deltree(dirObjects);
    }
    if (buildings) {
      deltree(dirBuildings);
    }
    if (pylons) {
      deltree(dirPylons);
    }
    if (roads) {
      deltree(dirRoads);
    }
  }

  private void deltree(File dir) {
    if (!dir.exists())
      return;
    for (File f : dir.listFiles()) {
      if (f.isDirectory())
        deltree(f);
      try {        
        Files.delete(f.toPath());
      } catch (SecurityException | IOException x) {
        log.log(Level.WARNING, "Deltree", x);
      }
    }
    try {
      Files.delete(dir.toPath());
    } catch (SecurityException | IOException x) {
      log.log(Level.WARNING, "Deltree", x);
    }
  }

  public synchronized boolean isBuildings() {
    return buildings;
  }

  public synchronized boolean isObjects() {
    return objects;
  }

  public synchronized boolean isRoads() {
    return roads;
  }

  public synchronized boolean isTerrain() {
    return terrain;
  }

  public synchronized boolean isPylons() {
    return pylons;
  }

  public synchronized void setDirRoads(File i) {
    roads = i!= null && i.exists();
    this.dirRoads = i;
  }

  public synchronized void setDirPylons(File i) {
    pylons = i!= null && i.exists();
    this.dirPylons = i;
  }

  public void setDirTerrain(File i) {
    terrain = i!= null && i.exists();
    dirTerrain = i;
  }

  public void setDirObjects(File i) {
    objects = i!= null && i.exists();
    dirObjects = i;
  }

  public void setDirBuildings(File i) {
    buildings = i!= null && i.exists();
    dirBuildings = i;
  }

  public File getDirTerrain() {
    return dirTerrain;
  }


}
