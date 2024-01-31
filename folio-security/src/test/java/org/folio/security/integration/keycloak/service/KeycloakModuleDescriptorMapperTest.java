package org.folio.security.integration.keycloak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.test.TestUtils.parse;
import static org.folio.test.TestUtils.readString;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;

@UnitTest
class KeycloakModuleDescriptorMapperTest {

  private static final ModuleDescriptor MD = parse(readString("json/mappingDescriptor.json"), ModuleDescriptor.class);
  private static final ModuleDescriptor EMPTY_MD =
    parse(readString("json/emptyDescriptor.json"), ModuleDescriptor.class);

  private static final String FOO_RESOURCE = "/foo";
  private static final List<String> FOO_SCOPES = List.of("POST");
  private static final List<String> FOO_PERMS = List.of("POST#foo.item.post");

  private static final String FOO_BY_ID = "/foo/{id}";
  private static final List<String> FOO_BY_ID_SCOPES = List.of("GET", "PUT", "DELETE");
  private static final List<String> FOO_BY_ID_PERMS =
    List.of("GET#foo.item.get", "PUT#foo.item.put", "DELETE#foo.item.delete");

  private static final String TIMER_RESOURCE = "/foo/timer";
  private static final List<String> TIMER_PERMISSIONS = Collections.emptyList();
  private static final List<String> TIMER_SCOPES = List.of("POST");

  private final KeycloakModuleDescriptorMapper mapper = new KeycloakModuleDescriptorMapper();

  @Test
  void map_positive() {
    var fooResource = resource(FOO_RESOURCE, FOO_PERMS, FOO_SCOPES);
    var fooByIdResource = resource(FOO_BY_ID, FOO_BY_ID_PERMS, FOO_BY_ID_SCOPES);
    var timerResource = resource(TIMER_RESOURCE, TIMER_PERMISSIONS, TIMER_SCOPES);

    var actual = mapper.map(MD, false);

    var resources = actual.getResourceServer().getResources();

    assertThat(resources).usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
      .containsExactlyInAnyOrder(fooResource, fooByIdResource, timerResource);
  }

  @Test
  void map_positive_excludingSystemInterfaces() {
    var fooResource = resource(FOO_RESOURCE, FOO_PERMS, FOO_SCOPES);
    var fooByIdResource = resource(FOO_BY_ID, FOO_BY_ID_PERMS, FOO_BY_ID_SCOPES);

    var actual = mapper.map(MD, true);

    var resources = actual.getResourceServer().getResources();

    assertThat(resources).usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
      .containsExactlyInAnyOrder(fooResource, fooByIdResource);
  }

  @Test
  void map_positive_emptyModuleDescriptor() {
    var actual = mapper.map(EMPTY_MD, false);

    var resources = actual.getResourceServer().getResources();
    assertThat(resources).isEmpty();
  }

  private static ResourceRepresentation resource(String name, List<String> folioPermissions, List<String> scopes) {
    var resource = new ResourceRepresentation();
    scopes.stream().map(ScopeRepresentation::new).forEach(resource::addScope);
    resource.setName(name);
    resource.setAttributes(Map.of("folio_permissions", folioPermissions));
    return resource;
  }
}
