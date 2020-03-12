package ca.uhn.fhir.jpa.starter.kafka.models;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.instance.model.api.IBaseResource;
import ca.uhn.fhir.context.FhirContext;

import ca.uhn.fhir.jpa.starter.context.models.Context;

public class FHIRKafkaMessage {
  FhirContext ctx = FhirContext.forDstu3();
  IParser parser = ctx.newJsonParser();

  public FHIRKafkaMessage(String requestType, Context context, IBaseResource payload,
                          String recordId, Long timeStamp){
    this.type = requestType;
    this.context = context;
    this.payload = payload;
    this.timeStamp = timeStamp;
    if (recordId != null) {
      FHIRMeta meta = new FHIRMeta(recordId);
      this.meta = meta;
    }
  }

  private String type;

  private IBaseResource payload;

  private Long timeStamp;

  private FHIRMeta meta;

  private Context context;

  public void setType(String type){
    this.type = type;
  }
  public String getType(){
    return this.type;
  }
  public void setPayload(IBaseResource payload){
    this.payload = payload;
  }
  public IBaseResource getPayload(){
    return this.payload;
  }
  public void setTimeStamp(Long timeStamp){
    this.timeStamp = timeStamp;
  }
  public Long getTimeStamp(){
    return this.timeStamp;
  }
  public void setMeta(String recordId){
    FHIRMeta meta = new FHIRMeta(recordId);
    this.meta = meta;
  }
  public FHIRMeta getMeta(){
    return this.meta;
  }

  public void setContext(Context context){
    this.context = context;
  }

  public Context getContext(){
    return this.context;
  }

  public String toString() {
    final StringBuffer sb = new StringBuffer("{");
    sb.append("\"type\":\"").append(type).append("\"");
    sb.append(", \"payload\":").append(parser.encodeResourceToString(payload));
    sb.append(", \"context\":").append(context.toString());
    sb.append(", \"meta\":").append(meta.toString());
    sb.append(", \"timeStamp\": \"").append(timeStamp.toString()).append("\"");
    sb.append("}");
    return sb.toString();
  }
}
