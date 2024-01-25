package org.folio.common.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.common.utils.InterfaceComparisonUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class InterfaceReference {

  private String id;
  private String version;

  public InterfaceReference id(String id) {
    this.id = id;
    return this;
  }

  public InterfaceReference version(String version) {
    this.version = version;
    return this;
  }

  /**
   * Check if this InterfaceDescriptor is compatible with the required one.
   *
   * @param required interface that is required
   */
  public boolean isCompatible(InterfaceReference required) {
    return InterfaceComparisonUtils.isCompatible(this.id, this.version, required.id, required.version);
  }

  /**
   * Check if this InterfaceDescriptor is compatible with the required one.
   *
   * @param required interface that is required
   */
  public boolean isCompatible(InterfaceDescriptor required) {
    return InterfaceComparisonUtils.isCompatible(this.id, this.version, required.getId(), required.getVersion());
  }
}
