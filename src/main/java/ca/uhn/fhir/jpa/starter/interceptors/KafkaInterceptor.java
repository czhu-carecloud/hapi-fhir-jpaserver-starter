package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.producer.IProducer;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IPreResourceAccessDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.IOException;
import java.util.Date;

import ca.uhn.fhir.jpa.starter.kafka.models.FHIRKafkaMessage;
import ca.uhn.fhir.jpa.starter.kafka.FHIRKafkaProducer;
import ca.uhn.fhir.jpa.starter.context.ContextService;
import ca.uhn.fhir.jpa.starter.context.models.Context;

/**
 * KafkaInterceptor: Class which intercepts calls prior to Database transaction commits and
 * produces a Kafka message. Messages produced adhere to schema defined in FHIRKafkaMessage
 * model.
 */

@Interceptor
public class KafkaInterceptor {

  static final String CONTEXT = "Context";

  private IProducer producer;

  public KafkaInterceptor(FHIRKafkaProducer producer){
    this.producer = producer;
  }

  @Hook(Pointcut.STORAGE_PREACCESS_RESOURCES)
  public void publishKafkaMessagesFromPreResourceAccessDetails(
    RequestDetails theRequestDetails, IPreResourceAccessDetails preResourceAccessDetails)
    throws IOException {
    /*
     * TODO: extend functionality for Audit Events
     */
    RequestTypeEnum requestType = theRequestDetails.getRequestType();
    String token = theRequestDetails.getHeader(CONTEXT);
    ContextService contextService = new ContextService(null, null, token);
    Context context = contextService.getContext();

    if (requestType != RequestTypeEnum.GET) {
      for (int i = 0; i < preResourceAccessDetails.size(); i++) {
        IBaseResource theResource = preResourceAccessDetails.getResource(i);
        String resourceType = theResource.fhirType();
        Long timeStamp = new Date().getTime();
        String resourceId = theResource.getIdElement().toString().split("[/]")[1];

        FHIRKafkaMessage message = new FHIRKafkaMessage(requestType.toString(), context, theResource, resourceId, timeStamp);

        producer.publish(resourceType, resourceId, message);
      }
    }
  }
}
