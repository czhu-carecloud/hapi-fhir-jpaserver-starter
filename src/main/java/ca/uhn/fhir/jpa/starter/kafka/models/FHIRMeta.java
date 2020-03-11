package ca.uhn.fhir.jpa.starter.kafka.models;

import com.google.gson.Gson;

public class FHIRMeta
{
  private String recordId;

  public void setRecordId(String recordId){
    this.recordId = recordId;
  }
  public String getRecordId(){
    return this.recordId;
  }

  @Override
  public String toString() {
    Gson gson = new Gson();
    return gson.toJson(this);
  }
}
