package ca.uhn.fhir.jpa.starter.context.model;

public class User {
  private String id;

  private String roleId;

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return this.id;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getRoleId() {
    return this.roleId;
  }
}
