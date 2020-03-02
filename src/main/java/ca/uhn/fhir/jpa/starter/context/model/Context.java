package ca.uhn.fhir.jpa.starter.context.model;

public class Context {
  private Scope scope;

  private User user;

  public void setScope(Scope scope) {
    this.scope = scope;
  }

  public Scope getScope() {
    return this.scope;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public User getUser() {
    return this.user;
  }
}
