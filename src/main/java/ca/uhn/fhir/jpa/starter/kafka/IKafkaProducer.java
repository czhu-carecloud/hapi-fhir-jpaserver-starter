package ca.uhn.fhir.jpa.starter.kafka;

import ca.uhn.fhir.jpa.starter.kafka.models.FHIRKafkaMessage;

public interface IKafkaProducer {
  void publish(String topic, String key, FHIRKafkaMessage message);
}
