package org.folio.tools.store.utils;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

@Log4j2
@UtilityClass
public class ResourceUtils {

  private static final String CLASSPATH_URL_PREFIX = "classpath:";
  private static final String URL_PROTOCOL_FILE = "file";

  public static File getFile(String resourceLocation) throws FileNotFoundException {
    requireNonNull(resourceLocation, "Resource location is not defined");

    if (resourceLocation.startsWith(CLASSPATH_URL_PREFIX)) {
      String path = resourceLocation.substring(CLASSPATH_URL_PREFIX.length());
      String description = "Class path resource [" + path + "]";
      ClassLoader cl = TlsUtils.class.getClassLoader();

      URL url = (cl != null ? cl.getResource(path) : ClassLoader.getSystemResource(path));
      if (url == null) {
        throw new FileNotFoundException(description
          + " cannot be resolved to absolute file path because it does not exist");
      }

      return getFile(url, description);
    }

    try {
      return getFile(toURL(resourceLocation));
    } catch (MalformedURLException ex) {
      return new File(resourceLocation);
    }
  }

  private static File getFile(URL resourceUrl) throws FileNotFoundException {
    return getFile(resourceUrl, "URL");
  }

  private static File getFile(URL resourceUrl, String description) throws FileNotFoundException {
    requireNonNull(resourceUrl, "Resource URL must not be null");

    if (!URL_PROTOCOL_FILE.equals(resourceUrl.getProtocol())) {
      throw new FileNotFoundException(
        description + " cannot be resolved to absolute file path " +
          "because it does not reside in the file system: " + resourceUrl);
    }
    try {
      return new File(toURI(resourceUrl).getSchemeSpecificPart());
    } catch (URISyntaxException ex) {
      return new File(resourceUrl.getFile());
    }
  }

  private static URL toURL(String location) throws MalformedURLException {
    try {
      return toURI(location).toURL();
    } catch (URISyntaxException | IllegalArgumentException ex) {
      return new URL(location);
    }
  }

  private static URI toURI(URL url) throws URISyntaxException {
    return toURI(url.toString());
  }

  private static URI toURI(String location) throws URISyntaxException {
    return new URI(StringUtils.replace(location, " ", "%20"));
  }
}
