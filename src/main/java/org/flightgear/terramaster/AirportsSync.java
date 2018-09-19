package org.flightgear.terramaster;

public class AirportsSync implements Syncable {

  @Override
  public String buildPath() {
    return "";
  }

  @Override
  public String getName() {
    return "Airports";
  }

  @Override
  public TerraSyncDirectoryTypes[] getTypes() {
    return new TerraSyncDirectoryTypes[]{TerraSyncDirectoryTypes.AIRPORTS};
  }

}
