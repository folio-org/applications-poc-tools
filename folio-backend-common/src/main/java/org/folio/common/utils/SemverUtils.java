package org.folio.common.utils;

import static org.apache.commons.lang3.RegExUtils.removeAll;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.folio.common.utils.CollectionUtils.toStream;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.semver4j.Semver;

@UtilityClass
public class SemverUtils {

  private static final Pattern VERSION_PATTERN = Pattern.compile(
    "-(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
      + "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)" //NOSONAR
      + "(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?" //NOSONAR
      + "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");
  private static final String VERSION_DELIMITER = "-";

  /**
   * Returns application/module version from application/module id.
   *
   * @param sourceId - application or module id
   * @return application/module's version
   */
  public static String getVersion(String sourceId) {
    var name = getName(sourceId);
    return removeStart(sourceId, name + VERSION_DELIMITER);
  }

  /**
   * Returns application/module name from application/module id.
   *
   * @param sourceId - application/module id
   * @return application/module's name
   */
  public static String getName(String sourceId) {
    if (isEmpty(sourceId)) {
      throw new IllegalArgumentException("Source cannot be blank");
    }

    var name = removeAll(sourceId, VERSION_PATTERN);
    var version = removeStart(sourceId, name + VERSION_DELIMITER);
    if (!Semver.isValid(version)) {
      throw new IllegalArgumentException("Invalid semantic version: source = " + sourceId);
    }

    return name;
  }

  /**
   * Returns application/module names from application/module ids.
   *
   * @param ids - application/module ids list
   * @return application/module's name
   */
  public static List<String> getNames(Collection<String> ids) {
    return toStream(ids)
      .map(SemverUtils::getName)
      .distinct()
      .toList();
  }

  /**
   * Check if the version satisfies a range or version.
   *
   * @param version version
   * @param rangeOrVersion range or version
   * @return {@code true} if the version satisfies the range or version, {@code false} otherwise
   */
  public static boolean satisfies(String version, String rangeOrVersion) {
    var semver = Semver.parse(version);

    if (semver == null) {
      throw new IllegalArgumentException("Invalid semantic version: " + version);
    }

    return semver.satisfies(rangeOrVersion, true);
  }

  /**
   * Check if the version of application/module satisfies a range or version.
   *
   * @param sourceId application/module id which includes version under the test
   * @param rangeOrVersion range or version
   * @return {@code true} if the version satisfies the range or version, {@code false} otherwise
   */
  public static boolean applicationSatisfies(String sourceId, String rangeOrVersion) {
    return satisfies(getVersion(sourceId), rangeOrVersion);
  }
}
