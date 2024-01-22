package org.folio.common.domain.model;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class Capability {

  private String resource;
  private String action;
  private String type;
  private String applicationId;
  private List<String> permissions;
  private Map<String, List<String>> capabilities;
}
