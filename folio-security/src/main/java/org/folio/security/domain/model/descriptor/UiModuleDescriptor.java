package org.folio.security.domain.model.descriptor;

import lombok.Data;

@Data
public class UiModuleDescriptor {

  private String npm;
  private String url;
  private String local;
  private String args;
}
