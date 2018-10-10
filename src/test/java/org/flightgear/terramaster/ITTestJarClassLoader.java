package org.flightgear.terramaster;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import org.junit.Test;

public class ITTestJarClassLoader {

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
    ClassLoader cl = (ClassLoader) ct.newInstance(rootCl);
    // From inside inner JAR
    Class<?> parserClass = cl.loadClass("org.antlr.runtime.Lexer");
    assertNotNull(parserClass);

    Enumeration<URL> resources = cl.getResources("sqljet.build.properties");
    for (String string : jars) {
      System.out.println(string);
    }
  }

}
