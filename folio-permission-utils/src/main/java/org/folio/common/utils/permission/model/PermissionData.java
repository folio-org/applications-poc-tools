package org.folio.common.utils.permission.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionData {

  private String resource;
  private PermissionType type;
  private PermissionAction action;
  private String permissionName;
}
