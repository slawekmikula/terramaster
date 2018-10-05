package org.flightgear.terramaster;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TestWebWorker{

  @Test
  public void testNoResult() {
    
    AirportResult cally = mock(AirportResult.class);
    WebWorker w = new WebWorker("Leibzig", cally);
    w.execute();
    // there must be exactly 1 call
    verify(cally, timeout(100000).times(1)).done();
    verify(cally, times(0)).addAirport(any());
    
  }

  @Test
  public void test1Result() {
    ArgumentCaptor<Airport> airportCaptor = ArgumentCaptor.forClass(Airport.class);
    
    AirportResult cally = mock(AirportResult.class);
    WebWorker w = new WebWorker("Leipzig", cally);
    w.execute();
    // there must be exactly 1 call
    verify(cally, timeout(100000).times(1)).done();
    
    verify(cally, times(1)).addAirport(airportCaptor.capture());

    List<Airport> capturedAirports = airportCaptor.getAllValues();
    assertEquals("e012n51", capturedAirports.get(0).getTileName());
    
  }

  @Test
  public void testFindResult() {
    ArgumentCaptor<Airport> airportCaptor = ArgumentCaptor.forClass(Airport.class);
    
    AirportResult cally = mock(AirportResult.class);
    
    MapFrame m = mock(MapFrame.class);
    doReturn(m).when(cally).getMapFrame();
    ArrayList<TileName> l = new ArrayList<>();
    l.add(TileName.getTile("e012n51"));
    WebWorker w = new WebWorker(l, cally);
    w.execute();
    // there must be exactly 1 call
    verify(cally, timeout(100000).times(1)).done();
    
    verify(cally, times(3)).addAirport(airportCaptor.capture());

    List<Airport> capturedAirports = airportCaptor.getAllValues();
    for (Iterator iterator = capturedAirports.iterator(); iterator.hasNext();) {
      Airport airport = (Airport) iterator.next();
      assertEquals("e012n51", airport.getTileName());      
      System.out.println(airport.name);
    }
    
  }
}
