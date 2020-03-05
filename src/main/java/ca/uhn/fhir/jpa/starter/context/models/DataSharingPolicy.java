package ca.uhn.fhir.jpa.starter.context.models;

import java.util.ArrayList;
import java.util.List;

public class DataSharingPolicy {
  private Payload payload;

  public void setPayload(Payload payload) {
    this.payload = payload;
  }

  public Payload getPayload() {
    return this.payload;
  }

  private class Payload {
    private List<Resources> resources;

    public void setResources(List<Resources> resources) {
      this.resources = resources;
    }

    public List<Resources> getResources() {
      return this.resources;
    }
  }

  private class Resources {
    private String resourceType;

    private List<Entities> entities;

    public void setResourceType(String resourceType) {
      this.resourceType = resourceType;
    }

    public String getResourceType() {
      return this.resourceType;
    }

    public void setEntities(List<Entities> entities) {
      this.entities = entities;
    }

    public List<Entities> getEntities() {
      return this.entities;
    }
  }

  private class Entities {
    private String entityId;

    private String entityType;

    public void setEntityId(String entityId) {
      this.entityId = entityId;
    }

    public String getEntityId() {
      return this.entityId;
    }

    public void setEntityType(String entityType) {
      this.entityType = entityType;
    }

    public String getEntityType() {
      return this.entityType;
    }
  }

  public List<String> getEntityReferences() {
    List<String> references = new ArrayList<>();
    for (Resources resource : getPayload().getResources()
    ) {
      for (Entities entity : resource.getEntities()
      ) {
        references.add(entity.getEntityType() + "/" + entity.getEntityId());
      }
      return references;
    }
    return references;
  }
}

