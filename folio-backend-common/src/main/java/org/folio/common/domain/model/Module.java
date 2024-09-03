package org.folio.common.domain.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Module implements Artifact {

  private String id;
  private String name;
  private String version;
  private String url;

  public Module(String name, String version) {
    this.name = name;
    this.version = version;
  }

  public Module id(String id) {
    this.id = id;
    return this;
  }

  public Module name(String name) {
    this.name = name;
    return this;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public Module version(String version) {
    this.version = version;
    return this;
  }

  @NotNull
  @Pattern(regexp = "^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([\\dA-Za-z-]+(?:\\.[\\dA-Za-z-]+)*))?(?:\\+[\\dA-Za-z-]+)?$")
  public String getVersion() {
    return version;
  }

  public Module url(String url) {
    this.url = url;
    return this;
  }
}
