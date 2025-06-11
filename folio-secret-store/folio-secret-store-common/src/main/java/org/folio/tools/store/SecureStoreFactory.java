package org.folio.tools.store;

import java.util.Objects;
import java.util.Properties;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.folio.tools.store.impl.AwsStore;
import org.folio.tools.store.impl.EphemeralStore;
import org.folio.tools.store.impl.FsspStore;
import org.folio.tools.store.impl.VaultStore;

@Log4j2
public final class SecureStoreFactory {

  private SecureStoreFactory() {
  }

  public static SecureStore getSecureStore(String type, Properties props) {
    SecureStore secureStore;

    type = Objects.isNull(type) ? Strings.EMPTY : type;

    secureStore = switch (type) {
      case VaultStore.TYPE -> new VaultStore(props);
      case AwsStore.TYPE -> AwsStore.create(props);
      case FsspStore.TYPE -> new FsspStore(props);
      default -> new EphemeralStore(props);
    };

    log.info("type: {}, class: {}", type, secureStore.getClass().getName());
    return secureStore;
  }
}
