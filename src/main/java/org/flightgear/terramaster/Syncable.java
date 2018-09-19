package org.flightgear.terramaster;

import java.io.File;

public interface Syncable {

  String buildPath();

  String getName();

  TerraSyncDirectoryTypes[] getTypes();

}
