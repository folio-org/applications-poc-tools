package org.folio.security.domain.model.descriptor;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ModuleDescriptor {

  private String id;
  private String name;
  private List<String> replaces;
  private List<String> tags;
  private List<InterfaceReference> requires;
  private List<InterfaceDescriptor> provides;
  private List<InterfaceReference> optional;
  private List<RoutingEntry> filters;
  private List<Permission> permissionSets;
  private List<Capability> capabilities;
  private List<EnvEntry> env;
  private Object metadata;
  private UiModuleDescriptor uiDescriptor;
  private LaunchDescriptor launchDescriptor;
  private UserDescriptor user;

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
  public ModuleDescriptor name(String name) {
    this.name = name;
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
   * Sets capabilities field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor capabilities(List<Capability> capabilities) {
    this.capabilities = capabilities;
    return this;
  }

  /**
   * Adds capabilities item to collection and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor addCapabilitiesItem(Capability capabilityItem) {
    if (this.capabilities == null) {
      this.capabilities = new ArrayList<>();
    }
    this.capabilities.add(capabilityItem);
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
   * Sets metadata field and returns {@link ModuleDescriptor}.
   *
   * @return modified {@link ModuleDescriptor} value
   */
  public ModuleDescriptor metadata(Object metadata) {
    this.metadata = metadata;
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
  public ModuleDescriptor user(UserDescriptor user) {
    this.user = user;
    return this;
  }
}
