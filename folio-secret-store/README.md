## Secure Stores

Three secure stores currently implemented for safe retrieval of encrypted credentials:

#### EphemeralStore ####

Only intended for _development purposes_. Credentials are defined in plain text in a specified properties
file.  `src/main/resources/ephemeral.properties`

#### AwsStore ####

Retrieves credentials from Amazon Web Services Systems Manager (AWS SSM), more specifically the Parameter Store, where
they're stored encrypted using a KMS key.  `src.main/resources/aws_ss.properties`

**Key:** `<salt>_<tenantId>_<username>`

e.g. Key=`ab73kbw90e_diku_diku`

#### VaultStore ####

Retrieves credentials from a Vault (https://vaultproject.io). This was added as a more generic alternative for those not
using AWS.  `src/main/resources/vault.properties`

**Key:** `<salt>/<tenantId>`
**Field:** `<username>`

e.g. Key=`ab73kbw90e/diku`, Field=`diku`

#### FsspStore ####

Retrieves credentials by key from a Folio Secure Store Proxy (FSSP). Configuration is typically provided in a properties
file or via environment variables.

**Key:** `<key>`

Example: Key=`service-key`

## Configuration

To use secrete stores, you must add dependencies to your pom.xml file:

```xml
    <dependency>
      <groupId>org.folio</groupId>
      <artifactId>folio-secret-store-common</artifactId>
      <version>1.0.0-SNAPSHOT</version>
    </dependency>
```

## Properties and  Variables

### EphemeralStore

| Property name | type   | Property value                                                                                     |
|---------------|--------|----------------------------------------------------------------------------------------------------|
| tenants       | string | Tenant's IDs divided by comma                                                                      |
| {tenantId}    | string | Tenant credentials as string with following format: tenantId:keycloakClientId:keycloakClientSecret |

### AwsStore

| Property name          | type   | Property value                                                                                               |
|------------------------|--------|--------------------------------------------------------------------------------------------------------------|
| region                 | string | The AWS region to pass to the AWS SSM Client Builder.                                                        |
| accessKey              | string | The AWS access key to pass to the AWS SSM Client.                                                            |
| secretKey              | string | The AWS secret key to pass to the AWS SSM Client.                                                            |
| useIam                 | string | If true, will rely on the current IAM role for authorization instead of explicitly providing AWS credentials |
| ecsCredentialsEndpoint | string | The HTTP endpoint to use for retrieving AWS credentials.                                                     |
| ecsCredentialsPath     | string | The path component of the credentials' endpoint URI.                                                         |

### VaultStore

| Property name           | type   | Property value                                                                       |
|-------------------------|--------|--------------------------------------------------------------------------------------|
| token                   | string | Token for accessing vault, may be a root token.                                      |
| address                 | string | The address of your vault.                                                           |
| enableSSL               | string | Whether to use SSL.                                                                  |
| ssl.pem.path            | string | The path to an X.509 certificate in unencrypted PEM format, using UTF-8 encoding.    |
| ssl.truststore.jks.path | string | The path to a JKS truststore file containing Vault server certs that can be trusted. |
| ssl.keystore.jks.path   | string | The path to a JKS keystore file containing a client cert and private key.            |
| ssl.keystore.password   | string | The password used to access the JKS keystore (optional).                             |

### FsspStore

| Property name           | type    | Property value                                                                                 |
|------------------------|---------|----------------------------------------------------------------------------------------------|
| address                | string  | The address of Folio Secure Store Proxy (FSSP).                                               |
| secretPath             | string  | The root path for secrets. Default: "secure-store/entries"                                   |
| enableSsl              | boolean | Whether to use SSL. Default: false                                                            |
| trustStorePath         | string  | The path to the trust store file.                                                             |
| trustStorePassword     | string  | The password for the trust store file.                                                        |
| trustStoreFileType     | string  | The type of the trust store file (e.g., "jks", "pem"). Default: "jks"                    |

## Autoconfiguration

To simplify integration with the secret stores, the `folio-secret-store-starter` module provides autoconfiguration.
The `SecretStoreAutoConfiguration` class will automatically configure the `SecretStore` bean based on the
`secret-store.type` property. The default value is `ephemeral`.
For using autoconfiguration, you must add dependencies to your pom.xml file:

```xml
    <dependency>
      <groupId>org.folio</groupId>
      <artifactId>folio-secret-store-starter</artifactId>
      <version>1.0.0-SNAPSHOT</version>
    </dependency>
```

### Example usage

```yaml
application:
  secret-store:
    type: aws-ssm
    ephemeral:
      content:
        key1: value1
        key2: value2
    aws-ssm:
      region: eu-west-1
      use-iam: true
      ecs-credentials-endpoint: http://localhost:8080
      ecs-credentials-path: /path/to/credentials
    vault:
      token: secrettoken
      address: http://localhost:8200
      enable-ssl: true
      pem-file-path: /src/main/resources/vault.pem
      keystore-password: optionalsecret
      keystore-file-path: /src/main/resources/keystore.jks
      truststore-file-path: /src/main/resources/truststore.jks
    fssp:
      address: http://localhost:8400
      secret-path: /secure-store/entries
      enable-ssl: true
      trust-store-path: /src/main/resources/truststore.jks
      trust-store-password: secretpassword
      trust-store-file-type: jks
```
