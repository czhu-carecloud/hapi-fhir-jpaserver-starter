package ca.uhn.fhir.jpa.starter.producer;

import ca.uhn.fhir.jpa.starter.kafka.models.FHIRKafkaMessage;

public interface IProducer {
  void publish(String topic, String key, FHIRKafkaMessage message);
}
