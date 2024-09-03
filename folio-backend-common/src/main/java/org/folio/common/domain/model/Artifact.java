package org.folio.common.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Artifact extends WithNameVersion {

  default String getId() {
    return getArtifactId();
  }

  /**
   * Creates artifact id from artifact name and version.
   *
   * @return created artifact id as {@link String} object
   */
  @JsonIgnore
  default String getArtifactId() {
    var name = getName();
    var version = getVersion();
    return name + "-" + version;
  }
}
