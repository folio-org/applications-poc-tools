package org.folio.common.domain.model;

import static java.lang.String.join;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface WithNameVersion {

  String getName();

  String getVersion();

  @JsonIgnore
  default String nameVersion() {
    return join(" ", this.getName(), this.getVersion());
  }
}
