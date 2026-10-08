package org.folio.common.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class Capability {

  private String id;
  private String name;
  private String description;
  private String resource;
  private String action;
  private String type;
  @JsonAlias("permission")
  private String permissionName;
  private List<String> replaces = new ArrayList<>();
  private Boolean visible;
  private String applicationId;
  private List<String> permissions;
  private Map<String, List<String>> capabilities;

  public Capability id(String id) {
    this.id = id;
    return this;
  }

  public Capability name(String name) {
    this.name = name;
    return this;
  }

  public Capability description(String description) {
    this.description = description;
    return this;
  }

  public Capability resource(String resource) {
    this.resource = resource;
    return this;
  }

  public Capability action(String action) {
    this.action = action;
    return this;
  }

  public Capability type(String type) {
    this.type = type;
    return this;
  }

  public Capability permissionName(String permissionName) {
    this.permissionName = permissionName;
    return this;
  }

  public Capability permission(String permission) {
    this.permissionName = permission;
    return this;
  }

  @JsonIgnore
  public String getPermission() {
    return this.permissionName;
  }

  public Capability replaces(List<String> replaces) {
    this.replaces = replaces;
    return this;
  }

  public Capability addReplacesItem(String replacesItem) {
    if (this.replaces == null) {
      this.replaces = new ArrayList<>();
    }
    this.replaces.add(replacesItem);
    return this;
  }

  public Capability visible(Boolean visible) {
    this.visible = visible;
    return this;
  }

  public Capability applicationId(String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  public Capability permissions(List<String> permissions) {
    this.permissions = permissions;
    return this;
  }

  public Capability addPermissionsItem(String permissionsItem) {
    if (this.permissions == null) {
      this.permissions = new ArrayList<>();
    }
    this.permissions.add(permissionsItem);
    return this;
  }

  public Capability capabilities(Map<String, List<String>> capabilities) {
    this.capabilities = capabilities;
    return this;
  }
}
