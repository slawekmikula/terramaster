package org.flightgear.terramaster;

import java.io.ByteArrayInputStream;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.flightgear.terramaster.dns.FlightgearNAPTRQuery;
import org.flightgear.terramaster.dns.WeightedUrl;

import de.keithpaterson.tar_n_feathers.TarFile;
import de.keithpaterson.tar_n_feathers.TarFileHeader;

/**
 * Implementation of the new TerraSync Version
 * 
 * @author keith.paterson
 */

public class HTTPTerraSync extends Thread implements TileService {
  private static final String DIRINDEX_FILENAME = ".dirindex";

  private static final int DIR_SIZE = 600;

  private static final int AIRPORT_MAX = 30000;

  private Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);

  private static final int RESET = 1;
  private static final int UPDATE = 2;
  private static final int EXTEND = 3;
  private static final int START = 4;
  private LinkedList<Syncable> syncList = new LinkedList<>();
  private boolean cancelFlag = false;

  private List<WeightedUrl> urls = new ArrayList<>();
  SecureRandom rand = new SecureRandom();
  private File localBaseDir;

  private HttpURLConnection httpConn;

  private boolean ageCheck;

  private long maxAge;

  private Object mutex = new Object();

  private HashMap<WeightedUrl, TileResult> downloadStats = new HashMap<>();
  private HashMap<WeightedUrl, TileResult> badUrls = new HashMap<>();
  private HashMap<String, String[]> dirIndexCache = new HashMap<>();

  private TerraMaster terraMaster;

  private boolean quitFlag;

  FlightgearNAPTRQuery flightgearNAPTRQuery = null; 

  public HTTPTerraSync(TerraMaster terraMaster) {
    super("HTTPTerraSync");
    this.terraMaster = terraMaster;
    flightgearNAPTRQuery = new FlightgearNAPTRQuery(terraMaster);
  }

  @Override
  public void setScnPath(File file) {
    localBaseDir = file;
  }

  @Override
  public void sync(Collection<Syncable> set, boolean ageCheck) {

    this.ageCheck = ageCheck;
    for (Syncable tileName : set) {
      if (tileName == null)
        continue;
      synchronized (syncList) {
        syncList.add(tileName);
        cancelFlag = false;
        syncList.sort((Syncable o1, Syncable o2) -> o1.getName().compareTo(o2.getName()));
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
  public Collection<Syncable> getSyncList() {
    return syncList;
  }

  @Override
  public void quit() {
    quitFlag = true;
    synchronized (syncList) {
      syncList.clear();
    }
    synchronized (mutex) {
      mutex.notifyAll();
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
      TileData d = terraMaster.getMapScenery().remove(n);
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
      while (!quitFlag) {
        synchronized (mutex) {
          if (syncList.isEmpty())
            mutex.wait(2000);
          if (syncList.isEmpty())
            continue;
        }
        // Woke up
        sync();
      }
      log.fine("HTTP TerraSync ended gracefully");
    } catch (Exception e) {
      log.log(Level.SEVERE, "HTTP Crashed ", e);
    }
  }

  private void sync() {
    int tilesize = 10000;
    // update progressbar
    invokeLater(START, 0); // update
    invokeLater(EXTEND, syncList.size() * tilesize + AIRPORT_MAX); // update
    while (!syncList.isEmpty()) {
      urls = flightgearNAPTRQuery
          .queryDNSServer(terraMaster.getProps().getProperty(TerraMasterProperties.SCENERY_VERSION, "ws20"));
      downloadStats.clear();
      badUrls.clear();
      urls.forEach(element -> downloadStats.put(element, new TileResult(element)));
      final Syncable n;
      synchronized (syncList) {
        if (syncList.isEmpty())
          continue;
        n = syncList.getFirst();
      }

      TerraSyncDirectoryTypes[] types = n.getTypes();
      for (TerraSyncDirectoryTypes terraSyncDirectoryType : types) {
        int updates = syncDirectory(terraSyncDirectoryType.dirname + n.buildPath(), false, terraSyncDirectoryType);
        invokeLater(UPDATE, DIR_SIZE - updates); // update progressBar
      }

      synchronized (syncList) {
        syncList.remove(n);
      }
    }
    HashMap<WeightedUrl, TileResult> completeStats = new HashMap<>();
    completeStats.putAll(downloadStats);
    completeStats.putAll(badUrls);

    terraMaster.showDnsStats(flightgearNAPTRQuery);
    terraMaster.showStats(completeStats);
    // syncList is now empty
    invokeLater(RESET, 0); // reset progressBar
  }

  /**
   * returns an array of unique 3-char prefixes
   * 
   * @param d
   * @return
   */
  private HashSet<String> findAirports(File d) {
    HashSet<String> set = new HashSet<>();

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

    double random = rand.nextDouble() * totalWeight;
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
        } else {
          fileName = "";
        }
      } else {
        fileName = url.getFile();
      }

      log.finest(() -> "Content-Type = " + contentType);
      log.finest(() -> "Content-Disposition = " + disposition);
      log.finest(() -> "Content-Length = " + contentLength);
      log.finest(() -> "fileName = " + fileName);

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
      downloadStats.get(baseUrl).time += System.currentTimeMillis() - start;
      return outputStream.toByteArray();
    } else {
      downloadStats.get(baseUrl).errors += 1;
      log.warning(
          () -> "No file to download. Server replied HTTP code: " + responseCode + " for " + url.toExternalForm());
    }
    httpConn.disconnect();
    return "".getBytes();
  }

  /**
   * Syncs the given directory.
   * 
   * @param path
   * @param force
   * @param type
   * @return
   */

  private int syncDirectory(String path, boolean force, TerraSyncDirectoryTypes type) {
    while (!urls.isEmpty()) {
      WeightedUrl baseUrl = getBaseUrl();
      try {
        int updates = 0;
        if (cancelFlag)
          return updates;
        HashMap<String, String> parentTypeLookup = buildTypeLookup(getRemoteDirIndex(baseUrl, getParent(path)));
        String[] parts = path.replace("\\", "/").split("/");
        String string = parts[parts.length - 1];
        String pathType = parentTypeLookup.get(string);

        if ("t".equals(pathType)) {
          updates += processTar(path, force, type);
        } else if ("d".equals(pathType)) {
          updates += processDir(path, force, type);
        } else {
          log.log(Level.WARNING, () -> "Couldn't process " + path + " with type " + pathType );
        }

        if (type.isTile())
          addScnMapTile(terraMaster.getMapScenery(), new File(localBaseDir, path), type);
        if (type == TerraSyncDirectoryTypes.TERRAIN) {
          HashSet<String> airports = findAirports(new File(localBaseDir, path));
          ArrayList<Syncable> sync = new ArrayList<>();
          airports.forEach(icaoPart -> sync.add(new AirportsSync(icaoPart)));
          sync(sync, false);
        }

        return updates;
      } catch (javax.net.ssl.SSLHandshakeException e) {
        log.log(Level.WARNING, "Handshake Error " + e.toString() + " syncing " + path, e);
        JOptionPane.showMessageDialog(terraMaster.frame,
            "Sync can fail if Java older than 8u101 and 7u111 with https hosts.\r\n"
                + baseUrl.getUrl().toExternalForm(),
            "SSL Error", JOptionPane.ERROR_MESSAGE);
        markBad(baseUrl, e);
      } catch (SocketException e) {
        log.log(Level.WARNING, "Connect Error " + e.toString() + " syncing with " + baseUrl.getUrl().toExternalForm()
            + path.replace("\\", "/") + " removing URL", e);
        markBad(baseUrl, e);
        return 0;
      } catch (UnknownHostException e) {
        log.log(Level.WARNING, "Unknown Host Error " + e.toString() + " syncing with "
            + baseUrl.getUrl().toExternalForm() + path.replace("\\", "/") + " removing URL. Connected?", e);
        markBad(baseUrl, e);
        return 0;
      } catch (Exception e) {
        log.log(Level.WARNING, "General Error " + e.toString() + " syncing with " + baseUrl.getUrl().toExternalForm()
            + path.replace("\\", "/"), e);
        return 0;
      }
    }
    return 0;
  }

  private int processTar(String path, boolean force, TerraSyncDirectoryTypes type) throws IOException {
    byte[] bs = downloadFile(getBaseUrl(), path + ".tgz");
    Files.createDirectory(Paths.get(localBaseDir.getAbsolutePath(), path));
    try (TarFile tf = new TarFile(new GZIPInputStream(new ByteArrayInputStream(bs)))) {
      TarFileHeader h = null;
      while ((h = tf.readHeader()) != null) {
        tf.writeFileContentToDir(new File(localBaseDir, path));
      }
    }
    return bs.length;
  }

  private int processDir(String path, boolean force, TerraSyncDirectoryTypes type)
      throws IOException, NoSuchAlgorithmException {
    int updates = 0;
    String localDirIndex = readLocalDirIndex(path);
    String[] localLines = localDirIndex.split("\r?\n");
    if (!force && ageCheck && getDirIndexAge(path) < maxAge)
      return localLines.length;
    String[] lines = getRemoteDirIndex(getBaseUrl(), path);
    HashMap<String, String> lookup = buildLookup(localLines);
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
          updates += syncDirectory(dirname, force, type);
      } else if (line.startsWith("f:")) {
        // We've got a file
        File localFile = new File(localBaseDir, path + File.separator + splitLine[1]);
        log.finest(localFile.getAbsolutePath());
        boolean load = true;
        if (localFile.exists()) {
          log.finest("Localfile : " + localFile.getAbsolutePath());
          byte[] b = calcSHA1(localFile);
          String bytesToHex = bytesToHex(b);
          // Changed
          load = !splitLine[2].equals(bytesToHex);
        } else {
          // New
          if (!localFile.getParentFile().exists()) {
            localFile.getParentFile().mkdirs();
          }
        }
        WeightedUrl filebaseUrl = getBaseUrl();
        if (load) {
          downloadFile(path, getBaseUrl(), splitLine, localFile, filebaseUrl);
        } else {
          downloadStats.get(filebaseUrl).equal += 1;
        }
        invokeLater(UPDATE, 1);
        updates++;
      } else if (line.startsWith("t:")) {
        updates += processTar(path + splitLine[1], force, type);
      }
      log.finest(line);
    }
    return updates;
  }

  private String getParent(String path) {
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1);
    if (path.contains("/"))
      return path.substring(0, path.lastIndexOf('/'));
    return "";
  }

  private HashMap<String, String> buildLookup(String[] localLines) {
    HashMap<String, String> lookup = new HashMap<>();
    for (int i = 0; i < localLines.length; i++) {
      String line = localLines[i];
      String[] splitLine = line.split(":");
      if (splitLine.length > 2)
        lookup.put(splitLine[1], splitLine[2]);
    }
    return lookup;
  }

  private HashMap<String, String> buildTypeLookup(String[] localLines) {
    HashMap<String, String> lookup = new HashMap<>();
    for (int i = 0; i < localLines.length; i++) {
      String line = localLines[i];
      String[] splitLine = line.split(":");
      if (splitLine.length > 2)
        lookup.put(splitLine[1], splitLine[0]);
    }
    return lookup;
  }

  private String[] getRemoteDirIndex(WeightedUrl baseUrl, String path) throws IOException {
    if (dirIndexCache.containsKey(path))
      return dirIndexCache.get(path);
    String remoteDirIndex = new String(downloadFile(baseUrl, path.replace("\\", "/") + "/.dirindex"));
    if (!remoteDirIndex.isEmpty()) {
      storeDirIndex(path, remoteDirIndex);
      dirIndexCache.put(path, remoteDirIndex.split("\r?\n"));
    }
    return remoteDirIndex.split("\r?\n");
  }

  private void downloadFile(String path, WeightedUrl baseUrl, String[] splitLine, File localFile,
      WeightedUrl filebaseUrl) throws IOException {
    try {
      downloadFile(localFile, filebaseUrl, path.replace("\\", "/") + "/" + splitLine[1]);
    } catch (javax.net.ssl.SSLHandshakeException e) {
      log.log(Level.WARNING, "Handshake Error " + e.toString() + " syncing " + path + " removing Base-URL", e);
      JOptionPane.showMessageDialog(terraMaster.frame,
          "Sync can fail if Java older than 8u101 and 7u111 with https hosts.\r\n"
              + filebaseUrl.getUrl().toExternalForm(),
          "SSL Error", JOptionPane.ERROR_MESSAGE);
      markBad(filebaseUrl, e);
    } catch (SocketException e) {
      log.log(Level.WARNING, "Connect Error " + e.toString() + " syncing with " + baseUrl.getUrl().toExternalForm()
          + path.replace("\\", "/") + " removing Base-URL", e);
      markBad(filebaseUrl, e);
    }
  }

  /**
   * 
   * @param filebaseUrl
   * @param e
   * @return
   */

  private boolean markBad(WeightedUrl filebaseUrl, Exception e) {
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

  private String readLocalDirIndex(String path) throws IOException {
    File file = new File(new File(localBaseDir, path), DIRINDEX_FILENAME);
    return file.exists() ? new String(readFile(file)) : "";
  }

  private long getDirIndexAge(String path) {
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
    try (InputStream fis = new FileInputStream(file)) {
      int n = 0;
      byte[] buffer = new byte[8192];
      while (n != -1) {
        n = fis.read(buffer);
        if (n > 0) {
          digest.update(buffer, 0, n);
        }
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
      while (n != -1) {
        n = fis.read(buffer);
        if (n > 0) {
          bos.write(buffer, 0, n);
        }
      }
    }
    return bos.toByteArray();
  }

  private void writeFile(File file, String remoteDirIndex) throws IOException {
    file.getParentFile().mkdirs();
    try (FileOutputStream fos = new FileOutputStream(file)) {
      fos.write(remoteDirIndex.getBytes());
    }
    log.finest(()-> "Written "+ file.getAbsolutePath());
  }

  /**
   * Does the Async notification of the GUI
   * 
   * @param action
   */

  private void invokeLater(final int action, final int num) {
    if (num < 0)
      log.warning(()->"Update < 0 (" + action + ")");
    // invoke this on the Event Disp Thread
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        switch (action) {

        case RESET: // reset progressBar
          terraMaster.frame.butStop.setEnabled(false);
          try {
            Thread.sleep(1200);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          terraMaster.frame.progressBar.setMaximum(0);
          terraMaster.frame.progressBar.setVisible(false);
          break;
        case UPDATE: // update progressBar
          terraMaster.frame.progressUpdate(num);
          break;
        case EXTEND: // progressBar maximum++
          terraMaster.frame.progressBar.setMaximum(terraMaster.frame.progressBar.getMaximum() + num);
          break;
        case START:
          terraMaster.frame.progressBar.setMaximum(terraMaster.frame.progressBar.getMaximum() + syncList.size() * 2);
          terraMaster.frame.progressBar.setVisible(true);
          terraMaster.frame.butStop.setEnabled(true);
          break;
        default:
          break;
        }
      }
    });
  }

  @Override
  public void restoreSettings() {
    maxAge = Long.parseLong(terraMaster.getProps().getProperty(TerraMasterProperties.MAX_TILE_AGE, "0"));

  }

  public void addScnMapTile(Map<TileName, TileData> map, File i, TerraSyncDirectoryTypes type) {
    TileName n = TileName.getTile(i.getName());
    TileData t = map.get(n);
    if (t == null) {
      // make a new TileData
      t = new TileData();
    }
    switch (type) {
    case TERRAIN:
      t.setDirTerrain(i);
      break;
    case OBJECTS:
      t.setDirObjects(i);
      break;
    case BUILDINGS:
      t.setDirBuildings(i);
      break;
    case PYLONS:
      t.setDirPylons(i);
      break;
    case ROADS:
      t.setDirRoads(i);
      break;
    case MODELS:
    case AIRPORTS:
      throw new IllegalArgumentException("Models not supported");
    }
    map.put(n, t);
  }

  // given a 10x10 dir, add the 1x1 tiles within to the HashMap
   void buildScnMap(File dir, Map<TileName, TileData> map, TerraSyncDirectoryTypes type) {
    File tiles[] = dir.listFiles();
    Pattern p = Pattern.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");

    for (File f : tiles) {
      Matcher m = p.matcher(f.getName());
      if (m.matches())
        addScnMapTile(map, f, type);
    }
  }

  /**
   *  builds a HashMap of /Terrain and /Objects
   */
  public Map<TileName, TileData> newScnMap(String path) {
    TerraSyncDirectoryTypes[] types = { TerraSyncDirectoryTypes.TERRAIN, TerraSyncDirectoryTypes.OBJECTS,
        TerraSyncDirectoryTypes.BUILDINGS };
    Pattern patt = Pattern.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");
    Map<TileName, TileData> map = new HashMap<>(180 * 90);

    for (TerraSyncDirectoryTypes terraSyncDirectoryType : types) {
      File d = new File(path + File.separator + terraSyncDirectoryType.dirname);
      File[] list = d.listFiles();
      if (list != null) {
        // list of 10x10 dirs
        for (File f : list) {
          Matcher m = patt.matcher(f.getName());
          if (m.matches()) {
            // now look inside this dir
            buildScnMap(f, map, terraSyncDirectoryType);
          }
        }
      }
    }
    return map;
  }

}
