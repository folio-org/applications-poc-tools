package org.folio.test.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.test.FakeKafkaConsumer;
import org.folio.test.extensions.impl.KafkaContainerExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(KafkaContainerExtension.class)
@Import(FakeKafkaConsumer.class)
public @interface EnableKafka {}
