package org.flightgear.terramaster;
import java.io.File;

/**
 * The subdirectories that TerraMaster supports in the directory. 
 * @author keith.paterson
 *
 */

public enum TerraSyncDirectoryTypes {

  TERRAIN("Terrain", 0, true), OBJECTS("Objects", 1, true), MODELS("Models",
      2, true), AIRPORTS("Airports", 3, false), BUILDINGS("Buildings", 4, false), PYLONS("Pylons", 4, true), ROADS("Roads", 4, true);

  String dirname = null;
  private boolean tile;
  
  public synchronized boolean isTile() {
    return tile;
  }

  TerraSyncDirectoryTypes(String name, int index, boolean tile) {
    this.dirname = name + File.separator;
    this.tile = tile;
  }

}
