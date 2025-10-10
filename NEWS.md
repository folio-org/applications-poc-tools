## Version `v3.0.7` (14.10.2025)
* Tenant specific Kong route management support (MGRAPPS-70)

---

* ## Version `v3.0.6` (13.10.2025)
* Support "delete" operation in secure store (FSSP-1)
* SecretStore implementation for folio-secure-store-proxy (APPPOCTOOL-59)
* Implement mTLS Client-Side Authentication for FSSP client (APPPOCTOOL-62)
* Add support for custom Keycloak base URL for JWKS endpoint ((MODSIDECAR-148)[https://folio-org.atlassian.net/browse/MODSIDECAR-148])

---

## Version `v3.0.5` (25.09.2025)
* Add wildcard to route's tenant header regex (APPPOCTOOL-64)
* Use SECURE\_STORE\_ENV, not ENV, for secure store key; drop KeycloakSecretUtils (APPPOCTOOL-63)

---

## Version `v3.0.4` (21.08.2025)
* Upgrade to Keycloak 26.3.3 (KEYCLOAK-56)

---

## Version `v3.0.3` (21.08.2025)
* Support find topics by name in KafkaAdminService (MGRENTITLE-135)

---

## Version `v3.0.2` (11.07.2025)
* Update semver4j version to 5.8.0 and adjust satisfies method call

---

## Version `v3.0.1` (27.06.2025)
* Fix JWT token decoding
