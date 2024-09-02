package org.folio.common.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.common.utils.InterfaceComparisonUtils;

@Data
@NoArgsConstructor
public class InterfaceDescriptor implements WithNameVersion {

  public static final String SYSTEM_INTERFACE_TYPE = "system";
  public static final String TIMER_INTERFACE = "_timer";

  private String id;
  private String version;
  private String interfaceType;
  private List<RoutingEntry> handlers = new ArrayList<>();
  private List<Permission> permissionSets = new ArrayList<>();
  private List<String> scope;

  /**
   * Creates interface descriptor with ID and version.
   *
   * @param id interface ID
   * @param version interface version
   */
  public InterfaceDescriptor(String id, String version) {
    this.id = id;
    if (validateVersion(version)) {
      this.version = version;
    } else {
      throw new IllegalArgumentException("Bad version number '" + version + "'");
    }
  }

  /**
   * Sets id field and returns {@link InterfaceDescriptor}.
   *
   * @return modified {@link InterfaceDescriptor} value
   */
  public InterfaceDescriptor id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Sets version field and returns {@link InterfaceDescriptor}.
   *
   * @return modified {@link InterfaceDescriptor} value
   */
  public InterfaceDescriptor version(String version) {
    this.version = version;
    return this;
  }

  /**
   * Sets interfaceType field and returns {@link InterfaceDescriptor}.
   *
   * @return modified {@link InterfaceDescriptor} value
   */
  public InterfaceDescriptor interfaceType(String interfaceType) {
    this.interfaceType = interfaceType;
    return this;
  }

  /**
   * Sets handlers field and returns {@link InterfaceDescriptor}.
   *
   * @return modified {@link InterfaceDescriptor} value
   */
  public InterfaceDescriptor handlers(List<RoutingEntry> handlers) {
    this.handlers = handlers;
    return this;
  }

  /**
   * Adds handler item to collection and returns {@link InterfaceDescriptor}.
   *
   * @return modified {@link InterfaceDescriptor} value
   */
  public InterfaceDescriptor addHandlersItem(RoutingEntry handlersItem) {
    if (this.handlers == null) {
      this.handlers = new ArrayList<>();
    }
    this.handlers.add(handlersItem);
    return this;
  }

  /**
   * Sets permissionSets field and returns {@link InterfaceDescriptor}.
   *
   * @return modified {@link InterfaceDescriptor} value
   */
  public InterfaceDescriptor permissionSets(List<Permission> permissionSets) {
    this.permissionSets = permissionSets;
    return this;
  }

  /**
   * Adds permision set item to collection and returns {@link InterfaceDescriptor}.
   *
   * @return modified {@link InterfaceDescriptor} value
   */
  public InterfaceDescriptor addPermissionSetsItem(Permission permissionSetsItem) {
    if (this.permissionSets == null) {
      this.permissionSets = new ArrayList<>();
    }
    this.permissionSets.add(permissionSetsItem);
    return this;
  }

  /**
   * Sets scope field and returns {@link InterfaceDescriptor}.
   *
   * @return modified {@link InterfaceDescriptor} value
   */
  public InterfaceDescriptor scope(List<String> scope) {
    this.scope = scope;
    return this;
  }

  /**
   * Adds scope item to collection and returns {@link InterfaceDescriptor}.
   *
   * @return modified {@link InterfaceDescriptor} value
   */
  public InterfaceDescriptor addScopeItem(String scopeItem) {
    if (this.scope == null) {
      this.scope = new ArrayList<>();
    }
    this.scope.add(scopeItem);
    return this;
  }

  /**
   * Check if this InterfaceDescriptor is compatible with the required one.
   *
   * @param required interface that is required
   */
  public boolean isCompatible(InterfaceDescriptor required) {
    return InterfaceComparisonUtils.isCompatible(this.id, this.version, required.id, required.version);
  }

  /**
   * Check if this InterfaceDescriptor is compatible with the required one.
   *
   * @param required interface that is required
   */
  public boolean isCompatible(InterfaceReference required) {
    return InterfaceComparisonUtils.isCompatible(this.id, this.version, required.getId(), required.getVersion());
  }

  /**
   * Checks if interface descriptor has type system.
   *
   * @return true if interface type is system, false - otherwise.
   */
  @JsonIgnore
  public boolean isSystem() {
    return SYSTEM_INTERFACE_TYPE.equals(this.interfaceType);
  }

  /**
   * Checks if interface is a time interface.
   *
   * @return true if interface represents timer, false - otherwise.
   */
  @JsonIgnore
  public boolean isTimer() {
    return TIMER_INTERFACE.equals(this.id);
  }

  /**
   * Validate that the version conforms to the format XX.YY.ZZ or XX.YY
   *
   * @return true if a good version number
   */
  public static boolean validateVersion(String version) {
    var p = versionParts(version, 0);
    return p.length > 0;
  }

  /**
   * Compares two interfaces.
   *
   * <p>Returns:</p>
   * <ul>
   *   <li>{@code 0} - if interfaces are equal</li>
   *   <li>{@code 2 or -2} - if interfaces have difference in the minor version part</li>
   *   <li>{@code 1 or -1} - if interfaces have difference in the patch version part</li>
   *   <li>{@link Integer#MAX_VALUE} otherwise</li>
   * </ul>
   *
   * @param required required interface with possibly multiple versions
   * @return integer value as a comparison result
   */
  public int compare(InterfaceDescriptor required) {
    return InterfaceComparisonUtils.compare(this.id, this.version, required.id, required.version);
  }

  @Override
  @JsonIgnore
  public String getName() {
    return id;
  }

  /**
   * Return the version parts.
   *
   * @param version full interface version
   * @return an array of 3 elements, XX, YY, ZZ, with -1 for missing parts
   */
  private static int[] versionParts(String version, int idx) {
    return InterfaceComparisonUtils.versionParts(version, idx);
  }
}
