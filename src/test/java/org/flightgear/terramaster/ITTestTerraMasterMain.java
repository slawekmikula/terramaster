package org.flightgear.terramaster;

import java.awt.Frame;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

public class ITTestTerraMasterMain {

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
      Robot r = new Robot();
      r.delay(3000);
      Frame[] frames = Frame.getFrames();
      for (Frame frame : frames) {
        if(frame.getClass().getName().contains("MapFrame"))
        {
          WindowListener l = new WindowListener() {
            
            @Override
            public void windowOpened(WindowEvent e) {
              // TODO Auto-generated method stub
              
            }
            
            @Override
            public void windowIconified(WindowEvent e) {
              // TODO Auto-generated method stub
              
            }
            
            @Override
            public void windowDeiconified(WindowEvent e) {
              // TODO Auto-generated method stub
              
            }
            
            @Override
            public void windowDeactivated(WindowEvent e) {
              // TODO Auto-generated method stub
              
            }
            
            @Override
            public void windowClosing(WindowEvent e) {
              // TODO Auto-generated method stub
              
            }
            
            @Override
            public void windowClosed(WindowEvent e) {
            }
            
            @Override
            public void windowActivated(WindowEvent e) {
              // TODO Auto-generated method stub
              
            }
          };
          frame.addWindowListener(l );
        }
        System.out.println(frame);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

}
