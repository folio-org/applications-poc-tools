package org.folio.common.utils.tls;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

final class NoopHostnameVerifier implements HostnameVerifier {

  static final NoopHostnameVerifier INSTANCE = new NoopHostnameVerifier();

  @Override
  public boolean verify(String s, SSLSession sslSession) {
    return true;
  }

  @Override
  public String toString() {
    return "NO_OP";
  }
}
