package org.folio.common.gateway;

/**
 * Settings for module self-registration in an API Gateway.
 *
 * @param moduleSelfUrl - module URL for self-registration
 * @param retries - the number of retries to execute upon failure to proxy
 * @param connectTimeout - connect timeout in milliseconds from the gateway to the upstream service
 * @param readTimeout - read timeout in milliseconds from the gateway to the upstream service
 * @param writeTimeout - write timeout in milliseconds from the gateway to the upstream service
 */
public record ModuleRegistrationSettings(String moduleSelfUrl, Integer retries, Integer connectTimeout,
  Integer readTimeout, Integer writeTimeout) {}
