package org.folio.common.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApplicationDescriptor implements Artifact {

  private String id;
  private String name;
  private String version;
  private String description;
  @Valid private List<Module> modules = new ArrayList<>();
  @Valid private List<Module> uiModules = new ArrayList<>();
  @Valid private List<ModuleDescriptor> moduleDescriptors = new ArrayList<>();
  @Valid private List<ModuleDescriptor> uiModuleDescriptors = new ArrayList<>();
  private String platform;
  @Valid private List<Dependency> dependencies = new ArrayList<>();
  private ApplicationDeployment deployment;
  private AnyDescriptor metadata;

  public ApplicationDescriptor(String name, String version) {
    this.name = name;
    this.version = version;
  }

  public ApplicationDescriptor id(String id) {
    this.id = id;
    return this;
  }

  public ApplicationDescriptor name(String name) {
    this.name = name;
    return this;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public ApplicationDescriptor version(String version) {
    this.version = version;
    return this;
  }

  @NotNull
  @Pattern(regexp = "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([\\dA-Za-z-]+(?:\\.[\\dA-Za-z-]+)*))?(?:\\+[\\dA-Za-z-]+)?$")
  public String getVersion() {
    return version;
  }

  public ApplicationDescriptor description(String description) {
    this.description = description;
    return this;
  }

  public ApplicationDescriptor metadata(AnyDescriptor metadata) {
    this.metadata = metadata;
    return this;
  }

  public ApplicationDescriptor modules(List<@Valid Module> modules) {
    this.modules = modules;
    return this;
  }

  public ApplicationDescriptor addModulesItem(Module modulesItem) {
    if (this.modules == null) {
      this.modules = new ArrayList<>();
    }
    this.modules.add(modulesItem);
    return this;
  }

  @Valid
  public List<@Valid Module> getModules() {
    return modules;
  }

  public ApplicationDescriptor uiModules(List<@Valid Module> uiModules) {
    this.uiModules = uiModules;
    return this;
  }

  public ApplicationDescriptor addUiModulesItem(Module uiModulesItem) {
    if (this.uiModules == null) {
      this.uiModules = new ArrayList<>();
    }
    this.uiModules.add(uiModulesItem);
    return this;
  }

  @Valid
  public List<@Valid Module> getUiModules() {
    return uiModules;
  }

  public ApplicationDescriptor moduleDescriptors(List<@Valid ModuleDescriptor> moduleDescriptors) {
    this.moduleDescriptors = moduleDescriptors;
    return this;
  }

  public ApplicationDescriptor addModuleDescriptorsItem(ModuleDescriptor moduleDescriptorsItem) {
    if (this.moduleDescriptors == null) {
      this.moduleDescriptors = new ArrayList<>();
    }
    this.moduleDescriptors.add(moduleDescriptorsItem);
    return this;
  }

  @Valid
  public List<@Valid ModuleDescriptor> getModuleDescriptors() {
    return moduleDescriptors;
  }

  public ApplicationDescriptor uiModuleDescriptors(List<@Valid ModuleDescriptor> uiModuleDescriptors) {
    this.uiModuleDescriptors = uiModuleDescriptors;
    return this;
  }

  public ApplicationDescriptor addUiModuleDescriptorsItem(ModuleDescriptor uiModuleDescriptorsItem) {
    if (this.uiModuleDescriptors == null) {
      this.uiModuleDescriptors = new ArrayList<>();
    }
    this.uiModuleDescriptors.add(uiModuleDescriptorsItem);
    return this;
  }

  @Valid
  public List<@Valid ModuleDescriptor> getUiModuleDescriptors() {
    return uiModuleDescriptors;
  }

  public ApplicationDescriptor platform(String platform) {
    this.platform = platform;
    return this;
  }

  public ApplicationDescriptor dependencies(List<@Valid Dependency> dependencies) {
    this.dependencies = dependencies;
    return this;
  }

  public ApplicationDescriptor addDependenciesItem(Dependency dependenciesItem) {
    if (this.dependencies == null) {
      this.dependencies = new ArrayList<>();
    }
    this.dependencies.add(dependenciesItem);
    return this;
  }

  @Valid
  public List<@Valid Dependency> getDependencies() {
    return dependencies;
  }

  public ApplicationDescriptor deployment(ApplicationDeployment deployment) {
    this.deployment = deployment;
    return this;
  }

  @Valid
  public ApplicationDeployment getDeployment() {
    return deployment;
  }
}
