package org.folio.security.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Data;

@Data
public class OkapiAccessToken {

  private String sub;

  @JsonProperty("user_id")
  private UUID userId;

  private String tenant;
}
