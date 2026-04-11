package org.folio.integration.kafka.producer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.common.configuration.properties.FolioEnvironment;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Meta-annotation that activates Kafka producer infrastructure for a Spring application.
 *
 * <p>Placing this annotation on any {@code @Configuration} class will:
 * <ul>
 *   <li>import {@link KafkaTopicConfiguration} to create declared topics on startup;</li>
 *   <li>import {@link FolioEnvironment} to make the FOLIO environment name available;</li>
 *   <li>import {@link KafkaAdminService} to enable programmatic topic management;</li>
 *   <li>bind {@link KafkaProducerProperties} from
 *       {@code application.kafka.producer.*} configuration properties.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({KafkaTopicConfiguration.class, FolioEnvironment.class, KafkaAdminService.class})
@EnableConfigurationProperties(KafkaProducerProperties.class)
public @interface EnableKafkaProducer {}
