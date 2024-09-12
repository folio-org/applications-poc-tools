package org.folio.jwt.openid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.test.TestUtils.OBJECT_MAPPER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.folio.jwt.openid.configuration.JwtParserConfiguration;
import org.folio.jwt.openid.utils.TestJwtGenerator;
import org.folio.test.types.UnitTest;
import org.jose4j.lang.UnresolvableKeyException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class JsonWebTokenParserTest {

  private static final String TENANT_NAME = "testtenant";
  private static final String KEYCLOAK_URL = "https://keycloak.sample.org";
  private static final String ISSUER_URL = KEYCLOAK_URL + "/realms/" + TENANT_NAME;
  private static final UUID USER_ID = UUID.randomUUID();

  @InjectMocks private JsonWebTokenParser jsonWebTokenParser;
  @Mock private OpenidJwtParserProvider openidJwtParserProvider;
  @Mock private JwtParserConfiguration jwtParserConfiguration;
  @Mock private JWTParser jwtParser;
  @Mock private JsonWebToken jsonWebToken;
  @Spy private ObjectMapper objectMapper = OBJECT_MAPPER;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(openidJwtParserProvider, jwtParserConfiguration);
  }

  @Test
  void parse_positive() throws Exception {
    var jwt = TestJwtGenerator.generateJwtString(KEYCLOAK_URL, TENANT_NAME, KEYCLOAK_URL, USER_ID);

    when(openidJwtParserProvider.getParser(ISSUER_URL)).thenReturn(jwtParser);
    when(jwtParserConfiguration.getIssuerRootUri()).thenReturn(KEYCLOAK_URL);
    when(jwtParserConfiguration.isValidateUri()).thenReturn(true);
    when(jwtParser.parse(jwt)).thenReturn(jsonWebToken);

    var result = jsonWebTokenParser.parse(jwt);

    assertThat(result).isEqualTo(jsonWebToken);
    verify(objectMapper).readTree(anyString());
  }

  @Test
  void parse_negative_invalidAmountOfSegmentsDummyToken() {
    var jwt = "DummyToken";
    assertThatThrownBy(() -> jsonWebTokenParser.parse(jwt))
      .isInstanceOf(ParseException.class)
      .hasMessage("Invalid amount of segments in JsonWebToken.");
  }

  @Test
  void parse_negative_tooManySegments() {
    var jwt = "seg1.seg2.seg3.seg4";
    assertThatThrownBy(() -> jsonWebTokenParser.parse(jwt))
      .isInstanceOf(ParseException.class)
      .hasMessage("Invalid amount of segments in JsonWebToken.");
  }

  @Test
  void parse_negative_unknownIssuerUrl() {
    var jwt = TestJwtGenerator.generateJwtString("https://keycloak:8080", TENANT_NAME, KEYCLOAK_URL, USER_ID);
    when(jwtParserConfiguration.isValidateUri()).thenReturn(true);
    when(jwtParserConfiguration.getIssuerRootUri()).thenReturn(KEYCLOAK_URL);

    assertThatThrownBy(() -> jsonWebTokenParser.parse(jwt))
      .isInstanceOf(ParseException.class)
      .hasMessage("Invalid JsonWebToken issuer");
  }

  @Test
  void parse_negative_disabledUriValidation() throws ParseException {
    var keycloakUrl = "https://keycloak:8080";
    var jwt = TestJwtGenerator.generateJwtString(keycloakUrl, TENANT_NAME, KEYCLOAK_URL, USER_ID);
    when(openidJwtParserProvider.getParser(keycloakUrl + "/realms/" + TENANT_NAME)).thenReturn(jwtParser);
    when(jwtParserConfiguration.isValidateUri()).thenReturn(false);
    when(jwtParser.parse(jwt)).thenReturn(jsonWebToken);

    var result = jsonWebTokenParser.parse(jwt);

    assertThat(result).isEqualTo(jsonWebToken);
  }

  @Test
  void parse_negative_jwtParserNotFound() {
    var jwt = TestJwtGenerator.generateJwtString(KEYCLOAK_URL, TENANT_NAME, KEYCLOAK_URL, USER_ID);
    when(jwtParserConfiguration.getIssuerRootUri()).thenReturn(KEYCLOAK_URL);
    when(jwtParserConfiguration.isValidateUri()).thenReturn(true);
    when(openidJwtParserProvider.getParser(ISSUER_URL)).thenReturn(null);

    assertThatThrownBy(() -> jsonWebTokenParser.parse(jwt))
      .isInstanceOf(ParseException.class)
      .hasMessage("Invalid JsonWebToken issuer");
  }

  @Test
  void parse_negative_parseExceptionWithoutCause() throws ParseException {
    var jwt = TestJwtGenerator.generateJwtString(KEYCLOAK_URL, TENANT_NAME, KEYCLOAK_URL, USER_ID);
    when(jwtParserConfiguration.getIssuerRootUri()).thenReturn(KEYCLOAK_URL);
    when(jwtParserConfiguration.isValidateUri()).thenReturn(true);
    when(openidJwtParserProvider.getParser(ISSUER_URL)).thenReturn(jwtParser);
    when(jwtParser.parse(jwt)).thenThrow(new ParseException("Token is expired"));

    assertThatThrownBy(() -> jsonWebTokenParser.parse(jwt))
      .isInstanceOf(ParseException.class)
      .hasMessage("Token is expired");
  }

  @Test
  void parse_negative_parseExceptionWithUnresolvableKeyException() throws ParseException {
    var jwt = TestJwtGenerator.generateJwtString(KEYCLOAK_URL, TENANT_NAME, KEYCLOAK_URL, USER_ID);
    when(jwtParserConfiguration.getIssuerRootUri()).thenReturn(KEYCLOAK_URL);
    when(jwtParserConfiguration.isValidateUri()).thenReturn(true);
    when(openidJwtParserProvider.getParser(ISSUER_URL)).thenReturn(jwtParser);
    when(jwtParser.parse(jwt)).thenThrow(new ParseException("Invalid token", new UnresolvableKeyException("error")));

    assertThatThrownBy(() -> jsonWebTokenParser.parse(jwt))
      .isInstanceOf(ParseException.class)
      .hasMessage("Invalid token");

    verify(openidJwtParserProvider).invalidateCache(ISSUER_URL);
  }
}
