package org.folio.integration.kafka;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.common.configuration.properties.FolioEnvironment;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({KafkaTopicConfiguration.class, FolioEnvironment.class, KafkaAdminService.class})
@EnableConfigurationProperties(FolioKafkaProperties.class)
public @interface EnableKafka {}
