package org.flightgear.terramaster;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Polygon;

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
  private File dirTerrain = null;
  private File dirObjects = null;
  private File dirBuildings = null;

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
  }

  private void deltree(File d) {
    if (!d.exists())
      return;
    for (File f : d.listFiles()) {
      if (f.isDirectory())
        deltree(f);
      try {
        f.delete();
      } catch (SecurityException x) {
        log.log(Level.WARNING, "Deltree", x);
      }
    }
    try {
      d.delete();
    } catch (SecurityException x) {
      log.log(Level.WARNING, "Deltree", x);
    }
  }

  public synchronized boolean isTerrain() {
    return terrain;
  }

  public synchronized boolean isObjects() {
    return objects;
  }

  public synchronized boolean isBuildings() {
    return buildings;
  }

  public void setDirTerrain(File i) {
    terrain = i!= null && i.exists();
    dirTerrain = i;
  }

  public File getDirTerrain() {
    return dirTerrain;
  }

  public void setDirObjects(File i) {
    objects = i!= null && i.exists();
    dirObjects = i;
  }

  public File getDirObjects() {
    return dirObjects;
  }


  public void setDirBuildings(File i) {
    buildings = i!= null && i.exists();
    dirBuildings = i;
  }

  public File getDirBuildings() {
    return dirBuildings;
  }


}
