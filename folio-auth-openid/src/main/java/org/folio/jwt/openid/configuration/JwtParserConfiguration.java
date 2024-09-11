package org.folio.jwt.openid.configuration;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtParserConfiguration {

  /**
   * Defines if token issuer URI must match with issuer root URI.
   */
  private final boolean validateUri;

  /**
   * OpenID root issuer URI.
   */
  private final String issuerRootUri;
}
