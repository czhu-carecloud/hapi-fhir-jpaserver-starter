package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.utils.IHttpClient;
import ca.uhn.fhir.jpa.starter.utils.ILRUCache;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.IPreResourceAccessDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;


import ca.uhn.fhir.jpa.starter.kafka.models.FHIRKafkaMessage;
import ca.uhn.fhir.jpa.starter.kafka.FHIRKafkaProducer;
import ca.uhn.fhir.jpa.starter.context.ContextService;
import ca.uhn.fhir.jpa.starter.context.models.Context;

public class KafkaInterceptor {
  private static final Logger log = LoggerFactory.getLogger(KafkaInterceptor.class.getName());

  FHIRKafkaProducer fhirKafkaProducer = new FHIRKafkaProducer();

  private ILRUCache<String, List<String>> cache;
  private IHttpClient httpClient;


  @Hook(Pointcut.STORAGE_PREACCESS_RESOURCES)
  public void verify(RequestDetails theRequestDetails, IPreResourceAccessDetails preResourceAccessDetails) throws IOException {
    /*
     * TODO: extend functionality for Audit Events
     */
    final String CONTEXT = "Context";

    RequestTypeEnum requestType = theRequestDetails.getRequestType();
    String token = theRequestDetails.getHeader(CONTEXT);
    ContextService contextService = new ContextService(cache, httpClient, token);
    contextService.verify();
    Context context = contextService.getContext();

    if (requestType != RequestTypeEnum.GET) {

      for (int i = 0; i < preResourceAccessDetails.size(); i++) {
        FHIRKafkaMessage message = new FHIRKafkaMessage();
        IBaseResource theResource = preResourceAccessDetails.getResource(i);
        String resourceType = theResource.fhirType();
        Long timeStamp = new Date().getTime();

        String resourceId = theResource.getIdElement().toString().split("[/]")[1];

        message.setType(requestType.toString());
        message.setContext(context);
        message.setPayload(theResource);
        message.setMeta(resourceId);
        message.setTimeStamp(timeStamp);

        fhirKafkaProducer.publish(resourceType, resourceId, message);
      }

      return;

    }
  }
}
