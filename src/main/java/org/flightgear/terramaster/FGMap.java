package org.flightgear.terramaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.logging.Logger;

/**
 * this class handles webqueries and returns results it keeps a per-session
 * HashMap of known airports It queries the multiplayer map.
 * {@link http://mpmap02.flightgear.org/fg_nav_xml_proxy.cgi?sstr=wbks&apt_code}
 */

public class FGMap extends Observable implements AirportResult {
  Logger LOG = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
  private HashMap<String, Airport> map;
  private List<Airport> searchResult = new ArrayList<>();

  public FGMap() {
    map = new HashMap<String, Airport>();
  }

  public void addAirport(Airport apt) {
    if (apt.code != null) {
      // first add the current airport to the HashMap
      map.put(apt.code, apt);

      // and to the current query's result
      searchResult.add(apt);
    }
  }

  public HashMap<String, Airport> getAirportMap() {
    return (HashMap<String, Airport>) map.clone();
  }

  public synchronized List<Airport> getSearchResult() {
    return searchResult;
  }

  public void clearAirports() {
    map.clear();
  }

  @Override
  public void done() {
    setChanged();
    notifyObservers();
    TerraMaster.frame.repaint();
  }

  @Override
  public void clearLastResult() {
    searchResult.clear();
  }
}
