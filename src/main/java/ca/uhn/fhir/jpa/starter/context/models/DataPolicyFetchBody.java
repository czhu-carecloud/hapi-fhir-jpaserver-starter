package ca.uhn.fhir.jpa.starter.context.models;

public class DataPolicyFetchBody {

  static final String POLICY_SLUG = "data-sharing";
  static final String ORGANIZATION_ENTITY_TYPE = "organization";

  Metadata metadata;
  private boolean disableInheritance;

  public DataPolicyFetchBody(String organizationID) {
    Metadata metadata = new Metadata();
    metadata.setApplication(POLICY_SLUG);
    metadata.setEntity_type(ORGANIZATION_ENTITY_TYPE);
    metadata.setEntity_id(organizationID);
    setDisableInheritance(true);
    setMetadata(metadata);
  }

  // Getter Methods

  public Metadata getMetadata() {
    return metadata;
  }

  public boolean getDisableInheritance() {
    return disableInheritance;
  }

  // Setter Methods

  public void setMetadata(Metadata metadataObject) {
    metadata = metadataObject;
  }

  public void setDisableInheritance(boolean disableInheritance) {
    this.disableInheritance = disableInheritance;
  }
}

