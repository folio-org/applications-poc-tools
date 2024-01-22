package org.folio.tools.kong.exception;

import java.io.Serial;
import java.util.List;
import lombok.Getter;
import org.folio.common.domain.model.error.Parameter;

@Getter
public class KongIntegrationException extends RuntimeException {

  @Serial private static final long serialVersionUID = -2287895699076196457L;
  private final transient List<Parameter> errors;

  /**
   * Creates a new {@link KongIntegrationException} with corresponding error message.
   *
   * @param message - error message as {@link String} object
   * @param errors - {@link List} with error {@link Parameter} objects
   */
  public KongIntegrationException(String message, List<Parameter> errors) {
    super(message);
    this.errors = errors;
  }
}
