package ca.uhn.fhir.jpa.starter.kafka;

import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.jpa.starter.kafka.models.FHIRKafkaMessage;

import java.util.Properties;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Component("FHIRKafkaProducer")
public class FHIRKafkaProducer implements DisposableBean, IKafkaProducer {
  private static final Logger log = LoggerFactory.getLogger(FHIRKafkaProducer.class.getName());

  private Properties producerProps;
  private Producer<String, String> producer;
  private TestCallback callback;

  public FHIRKafkaProducer() {
    java.util.Map<String, String> env = System.getenv();

    String bootstrapServerConfig = HapiProperties.getKafkaHost();
    String KAFKA_KEY = HapiProperties.getKafkaKey();
    String KAFKA_SECRET = HapiProperties.getKafkaSecret();

    String jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";
    String jaasCfg = String.format(jaasTemplate, KAFKA_KEY, KAFKA_SECRET);

    producerProps = new Properties();
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerConfig);
    producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
    producerProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1");
    producerProps.put(ProducerConfig.RETRIES_CONFIG, String.valueOf(Integer.MAX_VALUE));
    producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
    producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG, "0");
    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    producerProps.put("security.protocol", "SASL_SSL");
    producerProps.put("sasl.mechanism", "PLAIN");
    producerProps.put("sasl.jaas.config", jaasCfg);

    log.info("Kafka configuration for Kafka Producer");
    producerProps.list(System.out);

    producer = new KafkaProducer<>(producerProps);
    callback = new TestCallback();
  }

  @Override
  public void destroy() {
    log.info("Shutting down Kafka Producer");
    producer.flush();
    producer.close(5000, MILLISECONDS);
  }

  @Override
  public void publish(String topic, String key, FHIRKafkaMessage message) {
    String serializedMessage = message.toString();
    ProducerRecord<String, String> data = new ProducerRecord<>(topic, key, serializedMessage);
    producer.send(data, callback);
  }

  private static class TestCallback implements Callback {
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
      if (e != null) {
        log.error("Error while producing message {}{}", recordMetadata != null ? String.format(" for topic: %s ", recordMetadata.topic()) : "", e);
      } else {
        log.info("Sent message to topic: {} - partition: {} - offset: {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
      }
    }
  }

}
