package org.folio.security.integration.keycloak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.test.TestUtils.parse;
import static org.folio.test.TestUtils.readString;

import org.assertj.core.api.ThrowingConsumer;
import org.folio.security.domain.model.descriptor.ModuleDescriptor;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

@UnitTest
class KeycloakModuleDescriptorMapperTest {

  private static final ModuleDescriptor MD = parse(readString("json/mappingDescriptor.json"), ModuleDescriptor.class);
  private static final String RESOURCE_FOO = "/foo";
  private static final String RESOURCE_FOO_PERMS = "POST#foo.item.post";
  private static final String RESOURCE_FOO_ID = "/foo/{id}";
  private static final String RESOURCE_FOO_ID_PERMS = "GET#foo.item.get,PUT#foo.item.put,DELETE#foo.item.delete";

  private final KeycloakModuleDescriptorMapper mapper = new KeycloakModuleDescriptorMapper();

  @Test
  void map_positive_resourceTypeWithPermissions() {
    var actual = mapper.map(MD);

    var resources = actual.getResourceServer().getResources();
    assertThat(resources).anySatisfy(resourceWithTypeAndName(RESOURCE_FOO_ID, RESOURCE_FOO_ID_PERMS));
    assertThat(resources).anySatisfy(resourceWithTypeAndName(RESOURCE_FOO, RESOURCE_FOO_PERMS));
  }

  private static ThrowingConsumer<ResourceRepresentation> resourceWithTypeAndName(String name, String type) {
    return res -> {
      assertThat(res.getName()).isEqualTo(name);
      assertThat(res.getType()).isEqualTo(type);
    };
  }
}
