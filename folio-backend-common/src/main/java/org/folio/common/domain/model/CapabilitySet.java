package org.folio.common.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CapabilitySet {

  private String id;
  private String name;
  private String description;
  private String resource;
  private String action;
  private String type;
  @JsonAlias({"permissions", "permissionName"})
  private String permission;
  private List<String> capabilities = new ArrayList<>();
  private List<String> replaces = new ArrayList<>();
  private Boolean visible;
  private String applicationId;
  private String moduleId;

  public CapabilitySet id(String id) {
    this.id = id;
    return this;
  }

  public CapabilitySet name(String name) {
    this.name = name;
    return this;
  }

  public CapabilitySet description(String description) {
    this.description = description;
    return this;
  }

  public CapabilitySet resource(String resource) {
    this.resource = resource;
    return this;
  }

  public CapabilitySet action(String action) {
    this.action = action;
    return this;
  }

  public CapabilitySet type(String type) {
    this.type = type;
    return this;
  }

  public CapabilitySet permission(String permission) {
    this.permission = permission;
    return this;
  }

  public CapabilitySet permissions(String permissions) {
    this.permission = permissions;
    return this;
  }

  public CapabilitySet permissionName(String permissionName) {
    this.permission = permissionName;
    return this;
  }

  @JsonIgnore
  public String getPermissions() {
    return this.permission;
  }

  @JsonIgnore
  public String getPermissionName() {
    return this.permission;
  }

  public CapabilitySet capabilities(List<String> capabilities) {
    this.capabilities = capabilities;
    return this;
  }

  public CapabilitySet addCapabilitiesItem(String capabilitiesItem) {
    if (this.capabilities == null) {
      this.capabilities = new ArrayList<>();
    }
    this.capabilities.add(capabilitiesItem);
    return this;
  }

  public CapabilitySet replaces(List<String> replaces) {
    this.replaces = replaces;
    return this;
  }

  public CapabilitySet addReplacesItem(String replacesItem) {
    if (this.replaces == null) {
      this.replaces = new ArrayList<>();
    }
    this.replaces.add(replacesItem);
    return this;
  }

  public CapabilitySet visible(Boolean visible) {
    this.visible = visible;
    return this;
  }

  public CapabilitySet applicationId(String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  public CapabilitySet moduleId(String moduleId) {
    this.moduleId = moduleId;
    return this;
  }
}
