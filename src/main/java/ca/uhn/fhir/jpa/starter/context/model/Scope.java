package ca.uhn.fhir.jpa.starter.context.model;

public class Scope {
  private String organizationDisplay;

  private String organizationId;

  private String practitionerDisplay;

  private String practitionerId;

  private String user;

  public void setOrganizationDisplay(String organizationDisplay) {
    this.organizationDisplay = organizationDisplay;
  }

  public String getOrganizationDisplay() {
    return this.organizationDisplay;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public String getOrganizationId() {
    return this.organizationId;
  }

  public void setPractitionerDisplay(String practitionerDisplay) {
    this.practitionerDisplay = practitionerDisplay;
  }

  public String getPractitionerDisplay() {
    return this.practitionerDisplay;
  }

  public void setPractitionerId(String practitionerId) {
    this.practitionerId = practitionerId;
  }

  public String getPractitionerId() {
    return this.practitionerId;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getUser() {
    return this.user;
  }
}

