package org.folio.integration.kafka.consumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerFilteringConfiguration;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerProperties;
import org.folio.integration.kafka.consumer.filter.mmd.configuration.ModuleMetadataConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({KafkaConsumerFilteringConfiguration.class, ModuleMetadataConfiguration.class})
@EnableConfigurationProperties(KafkaConsumerProperties.class)
public @interface EnableKafkaConsumer {}
