package ca.uhn.fhir.jpa.starter.kafka;

import com.carecloud.fhir.hapi.stu3.CareCloudHapiProperties;
import com.carecloud.fhir.hapi.stu3.producer.IProducer;
import org.apache.kafka.clients.producer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import ca.uhn.fhir.jpa.starter.kafka.models.FHIRKafkaMessage;

import java.util.Properties;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Component("FHIRKafkaProducer")
public class FHIRKafkaProducer implements DisposableBean, IProducer {
  private static final Logger log = LoggerFactory.getLogger(FHIRKafkaProducer.class.getName());

  private static final String BOOTSTRAP_SERVER_CONFIG = CareCloudHapiProperties.getKafkaHost();
  private static final String KAFKA_KEY = CareCloudHapiProperties.getKafkaKey();
  private static final String KAFKA_SECRET = CareCloudHapiProperties.getKafkaSecret();
  private static final String JASS_TEMPLATE = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";
  private static final String JASS_CONFIG = String.format(JASS_TEMPLATE, KAFKA_KEY, KAFKA_SECRET);

  private Properties producerProps;
  private Producer<String, String> producer;
  private MessagePublishedCallback callback;

  public FHIRKafkaProducer() {
    producerProps = new Properties();
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER_CONFIG);
    producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
    producerProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1");
    producerProps.put(ProducerConfig.RETRIES_CONFIG, String.valueOf(Integer.MAX_VALUE));
    producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
    producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG, "0");
    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    producerProps.put("security.protocol", "SASL_SSL");
    producerProps.put("sasl.mechanism", "PLAIN");
    producerProps.put("sasl.jaas.config", JASS_CONFIG);

    log.info("Kafka configuration for Kafka Producer");
    producerProps.list(System.out);

    producer = new KafkaProducer<>(producerProps);
    callback = new MessagePublishedCallback();
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

  private static class MessagePublishedCallback implements Callback {
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
      if (e != null) {
        log.error("Error while producing message {}{}", recordMetadata != null ? String.format(" for topic: %s ", recordMetadata.topic()) : "", e);
      } else {
        log.info("Sent message to topic: {} - partition: {} - offset: {}", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
      }
    }
  }
}
