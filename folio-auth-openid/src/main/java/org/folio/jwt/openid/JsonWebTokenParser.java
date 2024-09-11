package org.folio.jwt.openid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.jwt.auth.principal.ParseException;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.folio.jwt.openid.configuration.JwtParserConfiguration;

@RequiredArgsConstructor
public class JsonWebTokenParser {

  public static final String INVALID_SEGMENTS_JWT_ERROR_MSG = "Invalid amount of segments in JsonWebToken.";
  private static final String TOKEN_SEPARATOR = "\\.";
  private static final String ISSUER_CLAIM = "iss";

  private final ObjectMapper objectMapper;
  private final JwtParserConfiguration properties;
  private final OpenidJwtParserProvider openidJwtParserProvider;

  /**
   * Parses json web token string from request to {@link JsonWebToken} object.
   *
   * @param accessToken - json web token {@link String} value
   * @return parsed {@link JsonWebToken} object
   * @throws ParseException - if json web token cannot be parsed
   */
  public JsonWebToken parse(String accessToken) throws ParseException {
    var accessTokenIssuer = getTokenIssuer(accessToken);

    var jwtParser = openidJwtParserProvider.getParser(accessTokenIssuer);
    if (jwtParser == null) {
      throw new ParseException("Invalid JsonWebToken issuer");
    }

    return jwtParser.parse(accessToken);
  }

  @SuppressWarnings("squid:S2129")
  private String getTokenIssuer(String authToken) throws ParseException {
    var split = authToken.split(TOKEN_SEPARATOR);
    if (split.length < 2 || split.length > 3) {
      throw new ParseException(INVALID_SEGMENTS_JWT_ERROR_MSG);
    }

    JsonNode jsonWebTokenTree;
    try {
      var decodedToken = new String(Base64.getDecoder().decode(split[1]));
      jsonWebTokenTree = objectMapper.readTree(decodedToken);
    } catch (Exception exception) {
      throw new ParseException("Failed to decode json web token", exception);
    }

    var issuer = jsonWebTokenTree.path(ISSUER_CLAIM).textValue();
    if (issuer == null) {
      throw new ParseException("Issuer not found in the json web token");
    }

    if (properties.isValidateUri()) {
      validateTokenIssuerUri(issuer);
    }

    return issuer;
  }

  private void validateTokenIssuerUri(String issuer) throws ParseException {
    if (!issuer.startsWith(properties.getIssuerRootUri())) {
      throw new ParseException("Invalid JsonWebToken issuer");
    }
  }
}
