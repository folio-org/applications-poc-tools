package org.folio.common.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ApplicationDeployment {

  @Valid private List<Module> modules = new ArrayList<>();
  @Valid private List<Module> uiModules = new ArrayList<>();
  private Deployment deployment;

  public ApplicationDeployment modules(List<@Valid Module> modules) {
    this.modules = modules;
    return this;
  }

  public ApplicationDeployment addModulesItem(Module modulesItem) {
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

  public ApplicationDeployment uiModules(List<@Valid Module> uiModules) {
    this.uiModules = uiModules;
    return this;
  }

  public ApplicationDeployment addUiModulesItem(Module uiModulesItem) {
    if (this.uiModules == null) {
      this.uiModules = new ArrayList<>();
    }
    this.uiModules.add(uiModulesItem);
    return this;
  }

  @Valid
  @JsonProperty("ui-modules")
  public List<@Valid Module> getUiModules() {
    return uiModules;
  }

  public ApplicationDeployment deployment(Deployment deployment) {
    this.deployment = deployment;
    return this;
  }

  @Valid 
  public Deployment getDeployment() {
    return deployment;
  }
}
