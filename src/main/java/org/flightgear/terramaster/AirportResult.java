package org.flightgear.terramaster;

import java.awt.Frame;

/**
 * Callback for the WebWorker
 * {@link WebWorker}
 * @author keith.paterson
 *
 */
public interface AirportResult {

  void clearLastResult();

	void addAirport(Airport result);

	void done();
  
	MapFrame getMapFrame(); 

}