package org.flightgear.terramaster;

public interface Syncable {

  String buildPath();

  String getName();

  TerraSyncDirectoryTypes[] getTypes();

}
