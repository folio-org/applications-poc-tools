package org.folio.common.utils;

import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InterfaceComparisonUtils {

  /**
   * Check if this InterfaceDescriptor is compatible with the required one.
   */
  public static boolean isCompatible(String id1, String version1, String id2, String version2) {
    var d = compare(id1, version1, id2, version2);
    return d >= 0 && d <= 2;
  }

  /**
   * Compares two interfaces by name and version.
   *
   * <p>Returns:</p>
   * <ul>
   *   <li>{@code 0} - if interfaces are equal</li>
   *   <li>{@code 2 or -2} - if interfaces have difference in the minor version part</li>
   *   <li>{@code 1 or -1} - if interfaces have difference in the patch version part</li>
   *   <li>{@link Integer#MAX_VALUE} otherwise</li>
   * </ul>
   *
   * @param id1 - first interface identifier
   * @param version1 - first interface version
   * @param id2 - second interface identifier
   * @param version2 - second interface version
   * @return integer value as a comparison result
   */
  public static int compare(String id1, String version1, String id2, String version2) {
    if (!Objects.equals(id1, id2)) {
      return Integer.MAX_VALUE;
    }

    var t = versionParts(version1, 0);
    if (t.length == 0) {
      return Integer.MAX_VALUE;
    }

    for (var idx = 0; ; idx++) {
      var r = versionParts(version2, idx);
      if (r.length == 0) {
        break;
      }

      if (t[0] == r[0]) {
        var diff = t[1] - r[1];
        if (diff > 0) {
          return 2;
        } else if (diff < 0) {
          return -2;
        }

        diff = t[2] - r[2];
        if (diff > 0) {
          return 1;
        } else if (diff < 0) {
          return -1;
        }
        return 0;
      }
    }

    return Integer.MAX_VALUE;
  }

  /**
   * Return the version parts.
   *
   * @param version full interface version
   * @return an array of 3 elements, XX, YY, ZZ, with -1 for missing parts
   */
  public static int[] versionParts(String version, int idx) {
    var verComp = version.split(" ");
    if (verComp.length <= idx) {
      return new int[0];
    }

    var parts = verComp[idx].split("\\.");
    if (parts.length < 2 || parts.length > 3) {
      return new int[0];
    }

    var versionParts = new int[3];
    for (var i = 0; i < 3; i++) {
      if (i < parts.length) {
        try {
          versionParts[i] = Integer.parseInt(parts[i]);
        } catch (NumberFormatException ex) {
          return new int[0];
        }
      } else {
        versionParts[i] = -1;
      }
    }
    return versionParts;
  }
}
