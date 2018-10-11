package org.flightgear.terramaster;

import static org.awaitility.Awaitility.await;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.awt.Frame;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ITTestTerraMasterMain {

  private Robot r;

  @Test
  public void test() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
      NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, IOException {
    File targetDir = new File("..");
    String[] jars = targetDir.list(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {
        System.out.println(dir.getAbsolutePath());
        return name.endsWith("jar");
      }
    });
    String pathToJar = new File(targetDir, jars[0]).getAbsolutePath();
    URL urlToJar = new File(targetDir, jars[0]).toURI().toURL();
    URLClassLoader rootCl = new URLClassLoader(new URL[] { urlToJar }, null);

    Class<?> clClass = rootCl.loadClass("org.flightgear.terramaster.JarClassLoader");
    Constructor<?> ct = clClass.getConstructor(ClassLoader.class);
    ClassLoader jcl = (ClassLoader) ct.newInstance(rootCl);
    try {
      Method m = jcl.getClass().getDeclaredMethod("invokeMain", String.class, String[].class);
      m.invoke(jcl, "org.flightgear.terramaster.TerraMaster", new String[] {});
      r = new Robot();
      r.setAutoDelay(100);

      await().atMost(20, TimeUnit.SECONDS).until(() -> Frame.getFrames().length > 0);

      WindowListener l = mock(WindowListener.class);
      Frame[] frames = Frame.getFrames();

      Frame mapFrame = null;
      for (Frame frame : frames) {
        if (frame.getClass().getName().contains("MapFrame")) {
          frame.addWindowListener(l);
          mapFrame = frame;
        }
      }
      final Frame mapFrame2 = mapFrame;

      await().atMost(20, TimeUnit.SECONDS).until(() -> mapFrame2.getHeight() > 0);
      mapFrame.setBounds(0, 0, mapFrame.getWidth(), mapFrame.getHeight());
      mouseMove(mapFrame.getWidth()-10, 5);
      r.mousePress(InputEvent.BUTTON1_MASK);
      r.mouseRelease(InputEvent.BUTTON1_MASK);
      verify(l, timeout(5000).times(99)).windowClosed(any());
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void mouseMove(int x, int y) {
    for (int count = 0; (MouseInfo.getPointerInfo().getLocation().getX() != x
        || MouseInfo.getPointerInfo().getLocation().getY() != y) && count < 100; count++) {
      r.mouseMove(x, y);
    }
  }

}
