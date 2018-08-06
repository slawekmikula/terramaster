package org.flightgear.terramaster;

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


}