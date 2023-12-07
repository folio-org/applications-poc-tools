package org.folio.common.utils;

import static org.folio.common.utils.KeycloakPermissionUtils.toPermissionName;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class PermissionUtilsTest {

  @Test
  void toPermissionName_positive() {
    var permission = toPermissionName(List.of("POST", "GET"), "test", "/foo/bar");
    assertEquals("[POST, GET] access for 'test' to '/foo/bar'", permission);
  }
}
