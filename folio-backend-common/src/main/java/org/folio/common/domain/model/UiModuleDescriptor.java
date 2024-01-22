package org.folio.common.domain.model;

import lombok.Data;

@Data
public class UiModuleDescriptor {

  private String npm;
  private String url;
  private String local;
  private String args;
}
