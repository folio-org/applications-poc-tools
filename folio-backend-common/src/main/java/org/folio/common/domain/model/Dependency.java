package org.folio.common.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.common.domain.validation.constraints.SemVersionOrRange;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@JsonIgnoreProperties({"id"})
public class Dependency implements WithNameVersion {

  private String name;
  private String version;

  public Dependency name(String name) {
    this.name = name;
    return this;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public Dependency version(String version) {
    this.version = version;
    return this;
  }

  @NotNull
  @SemVersionOrRange
  public String getVersion() {
    return version;
  }
}
