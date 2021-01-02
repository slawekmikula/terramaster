package org.flightgear.terramaster;

/**
 * The subdirectories that TerraMaster supports in the directory. 
 * @author keith.paterson
 *
 */
public enum TerraSyncDirectoryTypes {

  TERRAIN("Terrain", 0, true),
  OBJECTS("Objects", 1, true),
  MODELS("Models",2, false),
  AIRPORTS("Airports", 3, false),
  BUILDINGS("Buildings", 4, false),
  PYLONS("Pylons", 4, true),
  ROADS("Roads", 4, true),
  ORTHOPHOTOS("Orthophotos", 4, true);

  private String dirname = null;
  private final boolean tile;
  
  public synchronized boolean isTile() {
    return tile;
  }

  TerraSyncDirectoryTypes(String name, int index, boolean tile) {
    this.setDirname(name + "/");
    this.tile = tile;
  }

  public String getDirname() {
    return dirname;
  }

  public void setDirname(String dirname) {
    this.dirname = dirname;
  }

}
