package org.folio.integration.kafka.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ResourceResultEvent implements TenantAwareEvent {

  @NotBlank
  private String id;
  @NotBlank
  private String tenant;

  @Nullable
  private String moduleId;
  private String resourceName;

  @NotNull
  private ResourceResultStatus status;
  @Nullable
  private String details;
}
