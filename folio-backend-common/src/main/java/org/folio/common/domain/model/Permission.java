package org.folio.common.domain.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Permission {

  private String permissionName;
  private List<String> replaces;
  private String displayName;
  private String description;
  private List<String> subPermissions;
  private Boolean visible;

  public Permission permissionName(String permissionName) {
    this.permissionName = permissionName;
    return this;
  }

  public Permission replaces(List<String> replaces) {
    this.replaces = replaces;
    return this;
  }

  public Permission addReplacesItem(String replacesItem) {
    if (this.replaces == null) {
      this.replaces = new ArrayList<>();
    }
    this.replaces.add(replacesItem);
    return this;
  }

  public Permission displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  public Permission description(String description) {
    this.description = description;
    return this;
  }

  public Permission subPermissions(List<String> subPermissions) {
    this.subPermissions = subPermissions;
    return this;
  }

  public Permission addSubPermissionsItem(String subPermissionsItem) {
    if (this.subPermissions == null) {
      this.subPermissions = new ArrayList<>();
    }
    this.subPermissions.add(subPermissionsItem);
    return this;
  }

  public Permission visible(Boolean visible) {
    this.visible = visible;
    return this;
  }
}
