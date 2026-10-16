package org.folio.integration.kafka.consumer.it;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.awaitility.Awaitility.await;
import static org.folio.integration.kafka.consumer.KafkaTenantHeaders.FOLIO_TENANT_ID;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.folio.common.utils.OkapiHeaders;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerPropertiesConfiguration;
import org.folio.integration.kafka.consumer.filter.MarkerTableBasedTenantAwareMessageFilter;
import org.folio.integration.kafka.consumer.filter.TenantModuleVersionRepository;
import org.folio.integration.kafka.consumer.filter.TenantModuleVersionService;
import org.folio.test.extensions.EnableKafka;
import org.folio.test.extensions.EnablePostgres;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@EnableKafka
@EnablePostgres
@SpringBootTest(classes = {
  KafkaConsumerPropertiesConfiguration.class,
  MarkerTableBasedKafkaConsumerFilteringIT.TestConfig.class
},
  webEnvironment = WebEnvironment.NONE)
@TestPropertySource(properties = {
  "spring.kafka.consumer.auto-offset-reset=earliest"
})
class MarkerTableBasedKafkaConsumerFilteringIT {

  private static final String TOPIC = "test-marker-table-filter-events";
  private static final String MATCHING_VERSION_TENANT = "matching-version-tenant";
  private static final String DIFFERENT_VERSION_TENANT = "different-version-tenant";
  private static final String PRE_MARKER_TENANT = "pre-marker-tenant";
  private static final String UNINITIALIZED_TENANT = "uninitialized-tenant";
  private static final String EMPTY_MARKER_TENANT = "empty-marker-tenant";
  private static final String FOLIO_HEADER_TENANT = "folio-header-tenant";
  private static final String CACHE_INVALIDATION_TENANT = "cache-invalidation-tenant";
  private static final String DIFFERENT_VERSION_CONTROL_TENANT = "different-version-control-tenant";
  private static final String PRE_MARKER_CONTROL_TENANT = "pre-marker-control-tenant";
  private static final String CACHE_INVALIDATION_CONTROL_TENANT = "cache-invalidation-control-tenant";
  private static final String LIFECYCLE_TENANT = "lifecycle-tenant";
  private static final String CURRENT_MODULE_ID = "mod-test-2.0.0";
  private static final String DIFFERENT_MODULE_ID = "mod-test-1.0.0";

  @Autowired private KafkaTemplate<String, String> kafkaTemplate;
  @Autowired private JdbcOperations jdbcOperations;
  @Autowired private TenantModuleVersionRepository tenantModuleVersionRepository;
  @Autowired private TenantModuleVersionService tenantModuleVersionService;
  @Autowired private AtomicReference<String> currentTenant;
  @Autowired private TestEventCollector eventCollector;

  @BeforeEach
  void setUp() {
    eventCollector.clear();
  }

  @Test
  void filter_positive_matchingModuleVersion_messageAccepted() throws Exception {
    recreateInitializedTenant(MATCHING_VERSION_TENANT, true);
    tenantModuleVersionRepository.saveModuleVersion(schema(MATCHING_VERSION_TENANT), CURRENT_MODULE_ID);

    sendEvent(OkapiHeaders.TENANT, MATCHING_VERSION_TENANT, "matching-version-event");

    awaitEvents("matching-version-event");
  }

  @Test
  void filter_positive_differentModuleVersion_messageFiltered() throws Exception {
    recreateInitializedTenant(DIFFERENT_VERSION_TENANT, true);
    tenantModuleVersionRepository.saveModuleVersion(schema(DIFFERENT_VERSION_TENANT), DIFFERENT_MODULE_ID);
    prepareControlTenant(DIFFERENT_VERSION_CONTROL_TENANT);

    sendEvent(OkapiHeaders.TENANT, DIFFERENT_VERSION_TENANT, "different-version-event");
    sendEvent(OkapiHeaders.TENANT, DIFFERENT_VERSION_CONTROL_TENANT, "control-event");

    awaitEvents("control-event");
  }

  @Test
  void filter_positive_schemaPredatesMarker_messageFiltered() throws Exception {
    recreateInitializedTenant(PRE_MARKER_TENANT, false);
    prepareControlTenant(PRE_MARKER_CONTROL_TENANT);

    sendEvent(OkapiHeaders.TENANT, PRE_MARKER_TENANT, "pre-marker-event");
    sendEvent(OkapiHeaders.TENANT, PRE_MARKER_CONTROL_TENANT, "control-event");

    awaitEvents("control-event");
  }

  @Test
  void filter_positive_uninitializedTenant_messageAccepted() throws Exception {
    dropTenantSchema(UNINITIALIZED_TENANT);

    sendEvent(OkapiHeaders.TENANT, UNINITIALIZED_TENANT, "uninitialized-event");

    awaitEvents("uninitialized-event");
  }

  @Test
  void filter_positive_emptyMarkerTable_messageAccepted() throws Exception {
    recreateInitializedTenant(EMPTY_MARKER_TENANT, true);

    sendEvent(OkapiHeaders.TENANT, EMPTY_MARKER_TENANT, "empty-marker-event");

    awaitEvents("empty-marker-event");
  }

  @Test
  void filter_positive_missingTenantHeader_messageAccepted() throws Exception {
    kafkaTemplate.send(TOPIC, "missing-header-event").get();

    awaitEvents("missing-header-event");
  }

  @Test
  void filter_positive_folioTenantHeader_messageAccepted() throws Exception {
    recreateInitializedTenant(FOLIO_HEADER_TENANT, true);
    tenantModuleVersionRepository.saveModuleVersion(schema(FOLIO_HEADER_TENANT), CURRENT_MODULE_ID);

    sendEvent(FOLIO_TENANT_ID, FOLIO_HEADER_TENANT, "folio-header-event");

    awaitEvents("folio-header-event");
  }

  @Test
  void filter_positive_markerTableDroppedAfterCaching_messageFiltered() throws Exception {
    recreateInitializedTenant(CACHE_INVALIDATION_TENANT, true);
    tenantModuleVersionRepository.saveModuleVersion(schema(CACHE_INVALIDATION_TENANT), CURRENT_MODULE_ID);
    sendEvent(OkapiHeaders.TENANT, CACHE_INVALIDATION_TENANT, "cache-warmup-event");
    awaitEvents("cache-warmup-event");

    eventCollector.clear();
    jdbcOperations.execute(
      "DROP TABLE \"%s\".\"tenant_module_version\"".formatted(schema(CACHE_INVALIDATION_TENANT)));
    prepareControlTenant(CACHE_INVALIDATION_CONTROL_TENANT);

    sendEvent(OkapiHeaders.TENANT, CACHE_INVALIDATION_TENANT, "missing-table-event");
    sendEvent(OkapiHeaders.TENANT, CACHE_INVALIDATION_CONTROL_TENANT, "control-event");

    awaitEvents("control-event");
  }

  @Test
  void markerLifecycle_positive_upsertsAndDeletesModuleVersion() {
    recreateInitializedTenant(LIFECYCLE_TENANT, true);
    currentTenant.set(LIFECYCLE_TENANT);
    var previousVersionService = new TenantModuleVersionService(
      tenantModuleVersionRepository,
      name -> tenantModuleVersionRepository.tableExists(name, "test_entity"),
      currentTenant::get,
      MarkerTableBasedKafkaConsumerFilteringIT::schema,
      DIFFERENT_MODULE_ID);

    previousVersionService.markCurrentVersion();
    assertThat(tenantModuleVersionRepository.findModuleVersion(schema(LIFECYCLE_TENANT)))
      .contains(DIFFERENT_MODULE_ID);

    tenantModuleVersionService.markCurrentVersion();
    assertThat(tenantModuleVersionRepository.findModuleVersion(schema(LIFECYCLE_TENANT)))
      .contains(CURRENT_MODULE_ID);

    tenantModuleVersionService.clear();
    assertThat(tenantModuleVersionRepository.findModuleVersion(schema(LIFECYCLE_TENANT))).isEmpty();

    recreateInitializedTenant(LIFECYCLE_TENANT, false);
    assertThatNoException().isThrownBy(tenantModuleVersionService::clear);
  }

  private void awaitEvents(String... expectedEvents) {
    await().atMost(10, SECONDS).untilAsserted(() ->
      assertThat(eventCollector.getEvents())
        .extracting(ConsumerRecord::value)
        .containsExactly(expectedEvents));
  }

  private void sendEvent(String tenantHeader, String tenant, String event) throws Exception {
    var producerRecord = new ProducerRecord<String, String>(TOPIC, event);
    producerRecord.headers().add(tenantHeader, tenant.getBytes(UTF_8));
    kafkaTemplate.send(producerRecord).get();
  }

  private void prepareControlTenant(String tenant) {
    recreateInitializedTenant(tenant, true);
    tenantModuleVersionRepository.saveModuleVersion(schema(tenant), CURRENT_MODULE_ID);
  }

  private void recreateInitializedTenant(String tenant, boolean createMarkerTable) {
    var schema = schema(tenant);
    dropTenantSchema(tenant);
    jdbcOperations.execute("CREATE SCHEMA \"%s\"".formatted(schema));
    jdbcOperations.execute("CREATE TABLE \"%s\".\"test_entity\" (id INTEGER PRIMARY KEY)".formatted(schema));
    if (createMarkerTable) {
      jdbcOperations.execute("""
        CREATE TABLE "%s"."tenant_module_version" (
          id INTEGER PRIMARY KEY,
          module_id TEXT NOT NULL,
          updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """.formatted(schema));
    }
  }

  private void dropTenantSchema(String tenant) {
    jdbcOperations.execute("DROP SCHEMA IF EXISTS \"%s\" CASCADE".formatted(schema(tenant)));
  }

  private static String schema(String tenant) {
    return tenant.replace('-', '_');
  }

  @org.springframework.kafka.annotation.EnableKafka
  @TestConfiguration
  @EnableConfigurationProperties(KafkaProperties.class)
  static class TestConfig {

    @Bean
    KafkaAdmin kafkaAdmin(KafkaProperties kafkaProperties) {
      return new KafkaAdmin(kafkaProperties.buildAdminProperties());
    }

    @Bean
    NewTopic testTopic() {
      return new NewTopic(TOPIC, 1, (short) 1);
    }

    @Bean
    DataSource dataSource() {
      return new DriverManagerDataSource(
        System.getProperty("spring.datasource.url"), "folio_admin", "qwerty123");
    }

    @Bean
    JdbcOperations jdbcOperations(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }

    @Bean
    TenantModuleVersionRepository tenantModuleVersionRepository(JdbcOperations jdbcOperations) {
      return new TenantModuleVersionRepository(jdbcOperations);
    }

    @Bean
    AtomicReference<String> currentTenant() {
      return new AtomicReference<>();
    }

    @Bean
    TenantModuleVersionService tenantModuleVersionService(TenantModuleVersionRepository repository,
      AtomicReference<String> currentTenant) {
      return new TenantModuleVersionService(repository, schema -> repository.tableExists(schema, "test_entity"),
        currentTenant::get, MarkerTableBasedKafkaConsumerFilteringIT::schema, CURRENT_MODULE_ID);
    }

    @Bean("markerTableBasedTenantAwareMessageFilter")
    RecordFilterStrategy<String, String> markerTableBasedTenantAwareMessageFilter(
      TenantModuleVersionService tenantModuleVersionService) {
      return new MarkerTableBasedTenantAwareMessageFilter<>(tenantModuleVersionService, true);
    }

    @Bean
    TestEventCollector testEventCollector() {
      return new TestEventCollector();
    }

    @Bean
    DefaultKafkaProducerFactory<String, String> producerFactory(KafkaProperties kafkaProperties) {
      return new DefaultKafkaProducerFactory<>(
        kafkaProperties.buildProducerProperties(), new StringSerializer(), new StringSerializer());
    }

    @Bean
    KafkaTemplate<String, String> kafkaTemplate(
      DefaultKafkaProducerFactory<String, String> producerFactory) {
      return new KafkaTemplate<>(producerFactory);
    }

    @Bean("markerTableFilterContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> markerTableFilterContainerFactory(
      KafkaProperties kafkaProperties) {
      var consumerFactory = new DefaultKafkaConsumerFactory<>(
        kafkaProperties.buildConsumerProperties(), new StringDeserializer(), new StringDeserializer());
      var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
      factory.setConsumerFactory(consumerFactory);
      return factory;
    }
  }

  static class TestEventCollector {

    private final List<ConsumerRecord<String, String>> events = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = TOPIC, containerFactory = "markerTableFilterContainerFactory",
      filter = "markerTableBasedTenantAwareMessageFilter", groupId = "it-marker-table-filter-listener")
    void consume(ConsumerRecord<String, String> event) {
      events.add(event);
    }

    List<ConsumerRecord<String, String>> getEvents() {
      return List.copyOf(events);
    }

    void clear() {
      events.clear();
    }
  }
}
