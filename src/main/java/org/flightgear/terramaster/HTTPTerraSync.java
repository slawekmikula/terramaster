package org.flightgear.terramaster;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.flightgear.terramaster.dns.FlightgearNAPTRQuery;
import org.flightgear.terramaster.dns.FlightgearNAPTRQuery.HealthStats;
import org.flightgear.terramaster.dns.WeightedUrl;

/**
 * Implementation of the new TerraSync Version
 * 
 * @author keith.paterson
 */

public class HTTPTerraSync extends Thread implements TileService {
  private static final String DIRINDEX_FILENAME = ".dirindex";

  private static final int DIR_SIZE = 400;

  private static final int AIRPORT_MAX = 30000;

  private Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);

  private static final int RESET = 1;
  private static final int UPDATE = 2;
  private static final int EXTEND = 3;
  private LinkedList<TileName> syncList = new LinkedList<>();
  private boolean cancelFlag = false;
  private boolean noquit = true;

  private List<WeightedUrl> urls = new ArrayList<>();
  Random rand = new Random();
  private File localBaseDir;

  private HttpURLConnection httpConn;

  private boolean ageCheck;

  private boolean terrain;

  private boolean objects;

  private boolean buildings;

  private long maxAge;

  private Object mutex = new Object();

 
  private HashMap<WeightedUrl, TileResult> downloadStats = new HashMap<>(); 
  private HashMap<WeightedUrl, TileResult> badUrls = new HashMap<>(); 

  public HTTPTerraSync() {
    super("HTTPTerraSync");
  }

  @Override
  public void setScnPath(File file) {
    localBaseDir = file;
  }

  @Override
  public void sync(Collection<TileName> set, boolean ageCheck) {

    this.ageCheck = ageCheck;
    for (Iterator<TileName> iterator = set.iterator(); iterator.hasNext();) {
      TileName tileName = (TileName) iterator.next();
      if (tileName == null)
        continue;
      synchronized (syncList) {
        syncList.add(tileName);
        cancelFlag = false;
        syncList.sort((TileName o1, TileName o2) -> o1.getName().compareTo(o2.getName()));
      }
      log.finest("Added " + tileName.getName() + " to queue");
    }
    wakeUp();
  }

  public void wakeUp() {
    synchronized (mutex) {
      mutex.notifyAll();
    }
  }

  @Override
  public Collection<TileName> getSyncList() {
    return syncList;
  }

  @Override
  public void quit() {
    cancelFlag = true;
    synchronized (syncList) {
      syncList.clear();
    }
    (new Thread() {
      @Override
      public void run() {
        try {
          if (httpConn != null && httpConn.getInputStream() != null) {
            httpConn.getInputStream().close();
          }
        } catch (IOException e) {
          // Expecting to throw error
        }
      }
    }).start();
  }

  @Override
  public void cancel() {
    cancelFlag = true;
    synchronized (syncList) {
      syncList.clear();
    }
    (new Thread("Http Cancel Thread") {
      @Override
      public void run() {
        try {
          if (httpConn != null && httpConn.getInputStream() != null) {
            httpConn.getInputStream().close();
          }
        } catch (IOException e) {
          // Expecting to throw error
        }
      }
    }).start();
  }

  @Override
  public void delete(Collection<TileName> selection) {
    for (TileName n : selection) {
      TileData d = TerraMaster.mapScenery.remove(n);
      if (d == null)
        continue;
      d.delete();

      synchronized (syncList) {
        syncList.remove(n);
      }
    }
  }


  @Override
  public void run() {
    try {
      while (noquit) {
        synchronized (mutex) {
          mutex.wait();
        }
        //Woke up
        sync();
      }
      log.fine("HTTP TerraSync ended gracefully");
    } catch (Exception e) {
      e.printStackTrace();
      log.log(Level.SEVERE, "HTTP Crashed ", e);
    }
  }

  private void sync() {
    HashSet<String> apt = new HashSet<String>();
    int tilesize = (terrain ? DIR_SIZE : 0) + (objects ? DIR_SIZE : 0) + (buildings ? 2000 : 0);
    // update progressbar
    invokeLater(EXTEND, syncList.size() * tilesize + AIRPORT_MAX); // update
    FlightgearNAPTRQuery flightgearNAPTRQuery = new FlightgearNAPTRQuery();
    while (!syncList.isEmpty()) {
      urls = flightgearNAPTRQuery
          .queryDNSServer(TerraMaster.props.getProperty(TerraMasterProperties.SCENERY_VERSION, "ws20"));
      downloadStats.clear();
      badUrls.clear();
      urls.forEach(element-> downloadStats.put(element, new TileResult(element)));
      final TileName n;
      synchronized (syncList) {
        if (syncList.isEmpty())
          continue;
        n = syncList.getFirst();
      }

      String name = n.getName();
      if (name.startsWith("MODELS")) {
        int i = name.indexOf('-');
        if (i > -1)
          syncDirectory(name.substring(i + 1), false, TerraSyncDirectoryTypes.MODELS);
        else
          syncModels();
      } else {
        String path = n.buildPath();
        if (path != null) {
          // Updating Terrain/Objects/Buildings
          apt.addAll(syncTile(path));
        }
      }

      synchronized (syncList) {
        syncList.remove(n);
      }
    }
    if (apt != null) {
      syncAirports(apt.toArray(new String[0]));
    }

    showDnsStats(flightgearNAPTRQuery);
    showStats();
    // syncList is now empty
    invokeLater(RESET, 0); // reset progressBar
  }

  private void showDnsStats(FlightgearNAPTRQuery flightgearNAPTRQuery) {
    int errors = 0;
    for (Entry<String, HealthStats> entry : flightgearNAPTRQuery.getStats().entrySet()) {
      HealthStats stats = entry.getValue();
      log.fine(stats.toString());
      errors += stats.errors;
    }
    if (errors > 0) {
      JOptionPane.showMessageDialog(null,
          "There where errors in DNS queries. Consider enabling 8.8.8.8 or 9.9.9.9 in settings", "DNS Error",
          JOptionPane.WARNING_MESSAGE);
    }
  }

  /**
   * 
   */
  private void showStats() {
    try {
      
      HashMap<WeightedUrl, TileResult> completeStats = new HashMap<>();
      completeStats.putAll(downloadStats);
      completeStats.putAll(badUrls);
      new DownloadResultDialog(completeStats).setVisible(true);
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error showing stats ", e);
      e.printStackTrace();
    }
  }

  /**
   * 
   * @param path
   * @return
   * @throws IOException
   */

  private HashSet<String> syncTile(String path) {
    try {
      log.fine(() -> "Syncing " + path);
      if (terrain) {
        int updates = syncDirectory(TerraSyncDirectoryTypes.TERRAIN.dirname + path, false,
            TerraSyncDirectoryTypes.TERRAIN);
        invokeLater(UPDATE, DIR_SIZE - updates); // update progressBar
      }
      if (objects) {
        int updates = syncDirectory(TerraSyncDirectoryTypes.OBJECTS.dirname + path, false,
            TerraSyncDirectoryTypes.OBJECTS);
        invokeLater(UPDATE, DIR_SIZE - updates); // update progressBar
      }
      if (buildings) {
        int updates = syncDirectory(TerraSyncDirectoryTypes.BUILDINGS.dirname + path, false,
            TerraSyncDirectoryTypes.BUILDINGS);
        invokeLater(UPDATE, 2000 - updates); // update progressBar
      }
      return findAirports(new File(localBaseDir, TerraSyncDirectoryTypes.TERRAIN + File.separator + path));

    } catch (Exception e) {
      log.log(Level.SEVERE, "Can't sync tile " + path, e);
      JOptionPane.showMessageDialog(TerraMaster.frame,
          "Can't sync tile " + path + System.lineSeparator() + e.toString(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    return new HashSet<>();
  }

  /**
   * returns an array of unique 3-char prefixes
   * 
   * @param d
   * @return
   */
  private HashSet<String> findAirports(File d) {
    HashSet<String> set = new HashSet<String>();

    if (!d.exists())
      return set;
    for (File f : d.listFiles()) {
      String n = TileName.getAirportCode(f.getName());
      if (n != null) {
        set.add(n.substring(0, 3));
      }
    }
    return set;
  }

  /**
   * sync "Airports/W/A/T"
   * 
   * @param names
   */
  private void syncAirports(String[] names) {

    HashSet<String> nodes = new HashSet<>();
    for (String i : names) {
      String node = String.format("Airports/%c/%c/%c", i.charAt(0), i.charAt(1), i.charAt(2));
      nodes.add(node);
    }
    invokeLater(UPDATE, AIRPORT_MAX - nodes.size() * 100);
    for (String node : nodes) {
      int updates = syncDirectory(node, false, TerraSyncDirectoryTypes.AIRPORTS);
      invokeLater(UPDATE, 100 - updates);
    }
  }

  /**
   * Get a weighted random URL
   * 
   * @return
   */

  private WeightedUrl getBaseUrl() {
    if (urls.isEmpty()) {
      log.warning("No URLs to sync with");
    }

    // Compute the total weight of all items together
    double totalWeight = 0.0d;
    for (WeightedUrl i : urls) {
      totalWeight += i.getWeight();
    }
    // Now choose a random item
    int randomIndex = -1;
    double random = Math.random() * totalWeight;
    for (int i = 0; i < urls.size(); ++i) {
      random -= urls.get(i).getWeight();
      if (random <= 0.0d) {
        randomIndex = i;
        break;
      }
    }
    return urls.get(randomIndex);
  }

  /**
   * Downloads a File into a byte[]
   * 
   * @param url
   * @return
   * @throws IOException
   * @throws FileNotFoundException
   */

  private byte[] downloadFile(WeightedUrl baseUrl, String file) throws IOException {
    long start = System.currentTimeMillis();
    URL url = new URL(baseUrl.getUrl().toExternalForm() + file);

    log.finest(() -> "Downloading : " + url.toExternalForm());
    httpConn = (HttpURLConnection) url.openConnection();
    httpConn.setConnectTimeout(10000);
    httpConn.setReadTimeout(20000);
    int responseCode = (httpConn).getResponseCode();

    if (responseCode == HttpURLConnection.HTTP_OK) {
      final String fileName;
      String disposition = httpConn.getHeaderField("Content-Disposition");
      String contentType = httpConn.getContentType();
      int contentLength = httpConn.getContentLength();

      if (disposition != null) {
        // extracts file name from header field
        int index = disposition.indexOf("filename=");
        if (index > 0) {
          fileName = disposition.substring(index + 10, disposition.length() - 1);
        }
        else {
          fileName = "";
        }
      } else {
        fileName = url.getFile();
      }

      log.finest(()->"Content-Type = " + contentType);
      log.finest(()->"Content-Disposition = " + disposition);
      log.finest(()->"Content-Length = " + contentLength);
      log.finest(()->"fileName = " + fileName);

      // opens input stream from the HTTP connection
      InputStream inputStream = httpConn.getInputStream();

      // opens an output stream to save into file
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      int bytesRead = -1;
      byte[] buffer = new byte[1024];
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }

      outputStream.close();
      inputStream.close();

      log.fine("File downloaded");
      downloadStats.get(baseUrl).actualDownloads += 1;                
      downloadStats.get(baseUrl).numberBytes += outputStream.size();  
      downloadStats.get(baseUrl).time += System.currentTimeMillis()-start;
      return outputStream.toByteArray();
    } else {
      downloadStats.get(baseUrl).errors += 1;                
      log.warning( ()->
          "No file to download. Server replied HTTP code: " + responseCode + " for " + url.toExternalForm());
    }
    httpConn.disconnect();
    return "".getBytes();
  }

  private void syncModels() {
    if (localBaseDir == null) {
      JOptionPane.showMessageDialog(TerraMaster.frame, "TerraSync path not set");
    }

    try {
      syncDirectory("Models", false, TerraSyncDirectoryTypes.MODELS);
    } catch (Exception e) {
      log.log(Level.SEVERE, e.toString(), e);
    }
  }

  /**
   * Syncs the given directory.
   * 
   * @param path
   * @param force
   * @param models
   * @return
   */

  private int syncDirectory(String path, boolean force, TerraSyncDirectoryTypes models) {
    while (!urls.isEmpty()) {
      WeightedUrl baseUrl = getBaseUrl();
      try {

        int updates = 0;
        if (cancelFlag)
          return updates;
        String localDirIndex = readDirIndex(path);
        String[] localLines = localDirIndex.split("\r?\n");
        if (!force && ageCheck && getDirIndexAge(path) < maxAge)
          return localLines.length;
        String remoteDirIndex = new String(downloadFile(baseUrl, path.replace("\\", "/") + "/.dirindex"));
        String[] lines = remoteDirIndex.split("\r?\n");
        HashMap<String, String> lookup = new HashMap<>();
        for (int i = 0; i < localLines.length; i++) {
          String line = localLines[i];
          String[] splitLine = line.split(":");
          if (splitLine.length > 2)
            lookup.put(splitLine[1], splitLine[2]);
        }
        for (int i = 0; i < lines.length; i++) {
          if (cancelFlag)
            return updates;
          String line = lines[i];
          String[] splitLine = line.split(":");
          if (line.startsWith("d:")) {
            // We've got a directory if force ignore what we know
            // otherwise check the SHA against
            // the one from the server
            String dirname = path + "/" + splitLine[1];
            if (force || !(new File(dirname).exists()) || !splitLine[2].equals(lookup.get(splitLine[1])))
              updates += syncDirectory(dirname, force, models);
          } else if (line.startsWith("f:")) {
            // We've got a file
            File localFile = new File(localBaseDir, path + File.separator + splitLine[1]);
            log.finest(localFile.getAbsolutePath());
            boolean load = true;
            if (localFile.exists()) {
              log.finest("Localfile : " + localFile.getAbsolutePath());
              byte[] b = calcSHA1(localFile);
              String bytesToHex = bytesToHex(b);
              //Changed
              load = !splitLine[2].equals(bytesToHex);
            } else {
              //New
              if (!localFile.getParentFile().exists()) {
                localFile.getParentFile().mkdirs();
              }
            }
            WeightedUrl filebaseUrl = getBaseUrl();
            if (load) {
              try {
                downloadFile(localFile, filebaseUrl,  path.replace("\\", "/") + "/" +  splitLine[1]);
              } catch (javax.net.ssl.SSLHandshakeException e) {
                log.log(Level.WARNING, "Handshake Error " + e.toString() + " syncing " + path + " removing Base-URL", e);
                JOptionPane.showMessageDialog(TerraMaster.frame, "Sync can fail if Java older than 8u101 and 7u111 with https hosts.\r\n" + filebaseUrl.getUrl().toExternalForm(),
                    "SSL Error", JOptionPane.ERROR_MESSAGE);
                markBad(filebaseUrl, e);
              } catch (SocketException e) {
                log.log(Level.WARNING, "Connect Error " + e.toString() + " syncing with "
                    + baseUrl.getUrl().toExternalForm() + path.replace("\\", "/") + " removing Base-URL", e);
                markBad(filebaseUrl, e);
              }
            }
            else {
              downloadStats.get(filebaseUrl).equal += 1;              
            }
            invokeLater(UPDATE, 1);
            updates++;
          }
          log.finest(line);
        }
        if (models == TerraSyncDirectoryTypes.OBJECTS || models == TerraSyncDirectoryTypes.TERRAIN
            || models == TerraSyncDirectoryTypes.BUILDINGS)
          TerraMaster.addScnMapTile(TerraMaster.mapScenery, new File(localBaseDir, path), models);

        storeDirIndex(path, remoteDirIndex);
        return updates;
      } catch (javax.net.ssl.SSLHandshakeException e) {
        log.log(Level.WARNING, "Handshake Error " + e.toString() + " syncing " + path, e);
        JOptionPane.showMessageDialog(TerraMaster.frame, "Sync can fail if Java older than 8u101 and 7u111 with https hosts.\r\n" + baseUrl.getUrl().toExternalForm(),
            "SSL Error", JOptionPane.ERROR_MESSAGE);
        markBad(baseUrl,e);
      } catch (SocketException e) {
        log.log(Level.WARNING, "Connect Error " + e.toString() + " syncing with " + baseUrl.getUrl().toExternalForm()
            + path.replace("\\", "/") + " removing URL", e);
        markBad(baseUrl,e);
        return 0;
      } catch (UnknownHostException e) {
        log.log(Level.WARNING, "Unknown Host Error " + e.toString() + " syncing with " + baseUrl.getUrl().toExternalForm()
            + path.replace("\\", "/") + " removing URL. Connected?", e);
        markBad(baseUrl,e);
        return 0;
      } catch (Exception e) {
        log.log(Level.WARNING, "General Error " + e.toString() + " syncing with " + baseUrl.getUrl().toExternalForm()
            + path.replace("\\", "/"), e);
        return 0;
      }
    }
    return 0;
  }
  
  /**
   * 
   * @param filebaseUrl
   * @param e 
   * @return
   */

  public boolean markBad(WeightedUrl filebaseUrl, Exception e) {
    TileResult tileResult = downloadStats.get(filebaseUrl);
    tileResult.setException(e);
    badUrls.put(filebaseUrl, tileResult);
    return urls.remove(filebaseUrl);
  }

  /**
   * Downloads a file and stores it in the given local file
   * 
   * @param localFile
   * @param filebaseUrl 
   * @param url
   * @throws IOException
   */

  private int downloadFile(File localFile, WeightedUrl filebaseUrl, String url) throws IOException {
    byte[] fileContent = downloadFile(filebaseUrl, url);
    try (FileOutputStream fos = new FileOutputStream(localFile)) {
      fos.write(fileContent);
    }
    return fileContent.length;
  }

  private String readDirIndex(String path) throws IOException {
    File file = new File(new File(localBaseDir, path), DIRINDEX_FILENAME);
    return file.exists() ? new String(readFile(file)) : "";
  }

  private long getDirIndexAge(String path) throws IOException {
    File file = new File(new File(localBaseDir, path), DIRINDEX_FILENAME);
    return file.exists() ? (System.currentTimeMillis() - file.lastModified()) : (Long.MAX_VALUE);
  }

  private void storeDirIndex(String path, String remoteDirIndex) throws IOException {
    File file = new File(new File(localBaseDir, path), DIRINDEX_FILENAME);
    writeFile(file, remoteDirIndex);
  }

  private static final char[] HEXARRAY = "0123456789abcdef".toCharArray();

  private String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEXARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEXARRAY[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * Calculates the SHA1 Hash for the given File
   * 
   * @param file
   * @return
   * @throws NoSuchAlgorithmException
   * @throws IOException
   */

  private byte[] calcSHA1(File file) throws NoSuchAlgorithmException, IOException {
    MessageDigest digest = MessageDigest.getInstance("SHA-1");
    InputStream fis = new FileInputStream(file);
    int n = 0;
    byte[] buffer = new byte[8192];
    while (n != -1) {
      n = fis.read(buffer);
      if (n > 0) {
        digest.update(buffer, 0, n);
      }
    }
    return digest.digest();
  }

  /**
   * Reads the given File.
   * 
   * @param file
   * @return
   * @throws NoSuchAlgorithmException
   * @throws IOException
   */

  private byte[] readFile(File file) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (InputStream fis = new FileInputStream(file)) {
      int n = 0;
      byte[] buffer = new byte[8192];
      int off = 0;
      while (n != -1) {
        n = fis.read(buffer);
        if (n > 0) {
          bos.write(buffer, 0, n);
          off += n;
        }
      }
    }
    return bos.toByteArray();
  }

  private void writeFile(File file, String remoteDirIndex) throws IOException {
    file.getParentFile().mkdirs();
    FileOutputStream fos = new FileOutputStream(file);
    fos.write(remoteDirIndex.getBytes());
    fos.flush();
    fos.close();
  }

  /**
   * Does the Async notification of the GUI
   * 
   * @param action
   */

  private void invokeLater(final int action, final int num) {
    if (num < 0)
      log.warning("Update < 0 (" + action + ")");
    // invoke this on the Event Disp Thread
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        switch (action) {
        case RESET: // reset progressBar
          TerraMaster.frame.butStop.setEnabled(false);
          try {
            Thread.sleep(1200);
          } catch (InterruptedException e) {
          }
          TerraMaster.frame.progressBar.setMaximum(0);
          TerraMaster.frame.progressBar.setVisible(false);
          break;
        case UPDATE: // update progressBar
          TerraMaster.frame.progressUpdate(num);
          break;
        case EXTEND: // progressBar maximum++
          TerraMaster.frame.progressBar.setMaximum(TerraMaster.frame.progressBar.getMaximum() + num);
          break;
        }
      }
    });
  }

  @Override
  public void setTypes(boolean t, boolean o, boolean b) {
    terrain = t;
    objects = o;
    buildings = b;
  }

  @Override
  public void restoreSettings() {
    terrain = Boolean.parseBoolean(TerraMaster.props.getProperty(TerraSyncDirectoryTypes.TERRAIN.name(), "true"));
    objects = Boolean.parseBoolean(TerraMaster.props.getProperty(TerraSyncDirectoryTypes.OBJECTS.name(), "true"));
    buildings = Boolean.parseBoolean(TerraMaster.props.getProperty(TerraSyncDirectoryTypes.BUILDINGS.name(), "false"));
    maxAge = Long.parseLong(TerraMaster.props.getProperty(TerraMasterProperties.MAX_TILE_AGE, "0"));

  }

}
