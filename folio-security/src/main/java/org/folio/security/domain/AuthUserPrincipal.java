package org.folio.security.domain;

import java.util.UUID;
import lombok.Data;
import org.springframework.security.core.AuthenticatedPrincipal;

@Data
public class AuthUserPrincipal implements AuthenticatedPrincipal {

  private UUID userId;
  private String authUserId;
  private String tenant;

  @Override
  public String getName() {
    return userId != null ? userId.toString() : null;
  }

  /**
   * Sets userId for {@link AuthUserPrincipal} and returns {@link AuthUserPrincipal}.
   *
   * @return this {@link AuthUserPrincipal} with new userId value
   */
  public AuthUserPrincipal userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Sets authUserId for {@link AuthUserPrincipal} and returns {@link AuthUserPrincipal}.
   *
   * @return this {@link AuthUserPrincipal} with new authUserId value
   */
  public AuthUserPrincipal authUserId(String authUserId) {
    this.authUserId = authUserId;
    return this;
  }

  /**
   * Sets tenant for {@link AuthUserPrincipal} and returns {@link AuthUserPrincipal}.
   *
   * @return this {@link AuthUserPrincipal} with new tenant value
   */
  public AuthUserPrincipal tenant(String tenant) {
    this.tenant = tenant;
    return this;
  }
}
