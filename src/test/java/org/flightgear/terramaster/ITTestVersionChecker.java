package org.flightgear.terramaster;

import static org.junit.Assert.*;

import java.util.List;

import org.flightgear.terramaster.VersionChecker.Version;
import org.junit.Test;

public class ITTestVersionChecker {

  @Test
  public void test() {
    VersionChecker vc = new VersionChecker(null);
    List<Version> versionList = vc.getVersionList();
    assertNotNull(versionList);
  }

}
