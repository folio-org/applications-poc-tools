package org.folio.common.configuration.properties;

import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static org.apache.commons.lang3.StringUtils.firstNonBlank;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class FolioEnvironment {

  @NotEmpty
  @Pattern(regexp = "[\\w\\-]+", message = "Value must follow the pattern: '[\\w\\-]+'")
  @Value("${application.environment}")
  private String environment;

  /**
   * Return folio env name from environment or system properties as {@link String} object.
   *
   * @return folio env name.
   */
  public static String getFolioEnvName() {
    return firstNonBlank(getenv("ENV"), getProperty("env"), "folio");
  }
}
