package org.folio.common.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Data
@ToString(onlyExplicitlyIncluded = true)
public class RoutingEntry {

  @ToString.Include
  private List<String> methods;

  @ToString.Include
  private String pathPattern;

  @ToString.Include
  private String path;

  private String rewritePath;
  private String phase;
  private String level;
  private String type;
  private String redirectPath;
  private String unit;
  private String delay;

  private RoutingEntrySchedule schedule;
  private List<String> permissionsRequired;
  private List<String> permissionsDesired;
  private List<String> modulePermissions;
  private List<String> permissionsRequiredTenant;

  @JsonProperty("delegateCORS")
  private Boolean delegateCors;

  /**
   * Provides static path for request routing.
   *
   * @return static path as {@link String}
   */
  @JsonIgnore
  public String getStaticPath() {
    return StringUtils.isNotEmpty(this.path) ? this.path : this.pathPattern;
  }

  /**
   * Sets methods field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry methods(List<String> methods) {
    this.methods = methods;
    return this;
  }

  /**
   * Sets method item to collection and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry addMethodsItem(String methodsItem) {
    if (this.methods == null) {
      this.methods = new ArrayList<>();
    }
    this.methods.add(methodsItem);
    return this;
  }

  /**
   * Sets pathPattern field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry pathPattern(String pathPattern) {
    this.pathPattern = pathPattern;
    return this;
  }

  /**
   * Sets path field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry path(String path) {
    this.path = path;
    return this;
  }

  /**
   * Sets rewritePath field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry rewritePath(String rewritePath) {
    this.rewritePath = rewritePath;
    return this;
  }

  /**
   * Sets phase field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry phase(String phase) {
    this.phase = phase;
    return this;
  }

  /**
   * Sets level field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry level(String level) {
    this.level = level;
    return this;
  }

  /**
   * Sets type field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Sets redirectPath field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry redirectPath(String redirectPath) {
    this.redirectPath = redirectPath;
    return this;
  }

  /**
   * Sets unit field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry unit(String unit) {
    this.unit = unit;
    return this;
  }

  /**
   * Sets delay field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry delay(String delay) {
    this.delay = delay;
    return this;
  }

  /**
   * Sets schedule field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry schedule(RoutingEntrySchedule schedule) {
    this.schedule = schedule;
    return this;
  }

  /**
   * Sets permissionsRequired field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry permissionsRequired(List<String> permissionsRequired) {
    this.permissionsRequired = permissionsRequired;
    return this;
  }

  /**
   * Adds permissionsRequired item to collection field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry addPermissionsRequiredItem(String permissionsRequiredItem) {
    if (this.permissionsRequired == null) {
      this.permissionsRequired = new ArrayList<>();
    }
    this.permissionsRequired.add(permissionsRequiredItem);
    return this;
  }

  /**
   * Sets permissionsDesired field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry permissionsDesired(List<String> permissionsDesired) {
    this.permissionsDesired = permissionsDesired;
    return this;
  }

  /**
   * Adds permissionsDesired item to collection field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry addPermissionsDesiredItem(String permissionsDesiredItem) {
    if (this.permissionsDesired == null) {
      this.permissionsDesired = new ArrayList<>();
    }
    this.permissionsDesired.add(permissionsDesiredItem);
    return this;
  }

  /**
   * Sets modulePermissions field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry modulePermissions(List<String> modulePermissions) {
    this.modulePermissions = modulePermissions;
    return this;
  }

  /**
   * Adds modulePermissions item to collection field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry addModulePermissionsItem(String modulePermissionsItem) {
    if (this.modulePermissions == null) {
      this.modulePermissions = new ArrayList<>();
    }
    this.modulePermissions.add(modulePermissionsItem);
    return this;
  }

  /**
   * Sets permissionsRequiredTenant field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry permissionsRequiredTenant(List<String> permissionsRequiredTenant) {
    this.permissionsRequiredTenant = permissionsRequiredTenant;
    return this;
  }

  /**
   * Adds permissionsRequiredTenant item to collection field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry addPermissionsRequiredTenantItem(String permissionsRequiredTenantItem) {
    if (this.permissionsRequiredTenant == null) {
      this.permissionsRequiredTenant = new ArrayList<>();
    }
    this.permissionsRequiredTenant.add(permissionsRequiredTenantItem);
    return this;
  }

  /**
   * Sets delegateCors field and returns {@link RoutingEntry}.
   *
   * @return modified {@link RoutingEntry} value
   */
  public RoutingEntry delegateCors(Boolean delegateCors) {
    this.delegateCors = delegateCors;
    return this;
  }
}
