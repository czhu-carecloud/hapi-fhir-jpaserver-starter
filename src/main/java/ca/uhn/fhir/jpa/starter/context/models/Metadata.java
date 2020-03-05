package ca.uhn.fhir.jpa.starter.context.models;

public class Metadata {
  private String entity_id;
  private String entity_type;
  private String application;


  // Getter Methods

  public String getEntity_id() {
    return entity_id;
  }

  public String getEntity_type() {
    return entity_type;
  }

  public String getApplication() {
    return application;
  }

  // Setter Methods

  public void setEntity_id(String entity_id) {
    this.entity_id = entity_id;
  }

  public void setEntity_type(String entity_type) {
    this.entity_type = entity_type;
  }

  public void setApplication(String application) {
    this.application = application;
  }
}
