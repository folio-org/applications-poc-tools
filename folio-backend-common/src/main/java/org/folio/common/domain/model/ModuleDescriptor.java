package org.folio.common.domain.model;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.folio.common.utils.SemverUtils;

@Data
public class ModuleDescriptor implements Artifact {

  private String id;
  @JsonProperty("name")
  private String description;
  private List<String> replaces = new ArrayList<>();
  private List<String> tags = new ArrayList<>();
  private List<InterfaceReference> requires = new ArrayList<>();
  private List<InterfaceDescriptor> provides = new ArrayList<>();
  private List<InterfaceReference> optional = new ArrayList<>();
  private List<RoutingEntry> filters = new ArrayList<>();
  private List<Permission> permissionSets = new ArrayList<>();
  private List<EnvEntry> env = new ArrayList<>();
  private UiModuleDescriptor uiDescriptor;
  private LaunchDescriptor launchDescriptor;

  @Deprecated
  private UserDescriptor user;

  private AnyDescriptor metadata;
  private AnyDescriptor extensions;

  /**
   * Sets id field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Sets name field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor description(String name) {
    this.description = name;
    return this;
  }

  /**
   * Sets replaces field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor replaces(List<String> replaces) {
    this.replaces = replaces;
    return this;
  }

  /**
   * Adds replaces item to collection and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor addReplacesItem(String replacesItem) {
    if (this.replaces == null) {
      this.replaces = new ArrayList<>();
    }
    this.replaces.add(replacesItem);
    return this;
  }

  /**
   * Sets tags field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor tags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  /**
   * Adds tags item to collection and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor addTagsItem(String tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Sets requires field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor requires(List<InterfaceReference> requires) {
    this.requires = requires;
    return this;
  }

  /**
   * Adds requires interface item to collection and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor addRequiresItem(InterfaceReference requiresItem) {
    if (this.requires == null) {
      this.requires = new ArrayList<>();
    }
    this.requires.add(requiresItem);
    return this;
  }

  /**
   * Sets provides field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor provides(List<InterfaceDescriptor> provides) {
    this.provides = provides;
    return this;
  }

  /**
   * Adds provides interfaces item to collection and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor addProvidesItem(InterfaceDescriptor providesItem) {
    if (this.provides == null) {
      this.provides = new ArrayList<>();
    }
    this.provides.add(providesItem);
    return this;
  }

  /**
   * Sets optional field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor optional(List<InterfaceReference> optional) {
    this.optional = optional;
    return this;
  }

  /**
   * Adds optional interface item to collection and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor addOptionalItem(InterfaceReference optionalItem) {
    if (this.optional == null) {
      this.optional = new ArrayList<>();
    }
    this.optional.add(optionalItem);
    return this;
  }

  /**
   * Sets filters field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor filters(List<RoutingEntry> filters) {
    this.filters = filters;
    return this;
  }

  /**
   * Adds filter item to collection and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor addFiltersItem(RoutingEntry filtersItem) {
    if (this.filters == null) {
      this.filters = new ArrayList<>();
    }
    this.filters.add(filtersItem);
    return this;
  }

  /**
   * Sets permissionSets field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor permissionSets(List<Permission> permissionSets) {
    this.permissionSets = permissionSets;
    return this;
  }

  /**
   * Adds permission set item to collection and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor addPermissionSetsItem(Permission permissionSetsItem) {
    if (this.permissionSets == null) {
      this.permissionSets = new ArrayList<>();
    }
    this.permissionSets.add(permissionSetsItem);
    return this;
  }

  /**
   * Sets env field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor env(List<EnvEntry> env) {
    this.env = env;
    return this;
  }

  /**
   * Adds env item to collection and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor addEnvItem(EnvEntry envItem) {
    if (this.env == null) {
      this.env = new ArrayList<>();
    }
    this.env.add(envItem);
    return this;
  }

  /**
   * Sets uiDescriptor field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor uiDescriptor(UiModuleDescriptor uiDescriptor) {
    this.uiDescriptor = uiDescriptor;
    return this;
  }

  /**
   * Sets launchDescriptor field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor launchDescriptor(LaunchDescriptor launchDescriptor) {
    this.launchDescriptor = launchDescriptor;
    return this;
  }

  /**
   * Sets user field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  @Deprecated
  public ModuleDescriptor user(UserDescriptor user) {
    this.user = user;
    return this;
  }

  /**
   * Sets metadata field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor metadata(AnyDescriptor metadata) {
    this.metadata = metadata;
    return this;
  }

  /**
   * Sets extensions for {@link ModuleDescriptor} and returns {@link ModuleDescriptor}.
   *
   * @return this {@link ModuleDescriptor} with new extensions value
   */
  public ModuleDescriptor extensions(AnyDescriptor extensions) {
    this.extensions = extensions;
    return this;
  }

  @Override
  @JsonIgnore
  public String getVersion() {
    return isNotBlank(id) ? SemverUtils.getVersion(id) : null;
  }

  @Override
  @JsonIgnore
  public String getName() {
    return isNotBlank(id) ? SemverUtils.getName(id) : null;
  }
}
