package org.flightgear.terramaster;

public class Airport {
	String name = "";
	String code = "";
	String tilename = "";
	double lat;
	double lon;
	private double maxLat = 0;
	private double maxLon = 0;
	private double minLon = Double.MAX_VALUE;
	private double minLat = Double.MAX_VALUE;

	public Airport(String code, String name) {
		this.code = code;
		this.name = name;
	}

	public String toString() {
		return String.format("%s %s (%g,%g) %s", code, name, lat, lon, tilename);
	}

	public String getTileName() {
		return tilename;
	}

	/**
	 * Performs the finding of a center of an airport from all the runway ends. 
	 * @param sLat
	 * @param sLon
	 */
	public void updatePosition(String sLat, String sLon) {
		double newLat = Double.parseDouble(sLat);
		double newLon = Double.parseDouble(sLon);

		maxLat = maxLat!=0?Math.max(maxLat, newLat):newLat;
		maxLon = maxLon!=0?Math.max(maxLon, newLon):newLon;

		minLat = Math.min(minLat, newLat);
		minLon = Math.min(minLon, newLon);
		this.lat = (maxLat - minLat)/2 + minLat; 
		this.lon = (maxLon - minLon)/2 + minLon; 
		java.awt.geom.Point2D.Double p = new java.awt.geom.Point2D.Double(lon, -lat);
    tilename = TileName.computeTileName(p );
	}
}
