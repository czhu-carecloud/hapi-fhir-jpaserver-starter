package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.dao.DaoRegistry;
import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.provider.dstu3.JpaResourceProviderDstu3;

import ca.uhn.fhir.jpa.starter.context.ContextService;
import ca.uhn.fhir.jpa.starter.context.models.Context;
import ca.uhn.fhir.jpa.starter.context.models.User;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;

import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import org.hl7.fhir.dstu3.model.*;

import org.hl7.fhir.dstu3.model.AuditEvent.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;


@Interceptor
public class AuditEventInterceptor {
  private static final Logger log = LoggerFactory.getLogger(AuditEventInterceptor.class.getName());

  private static final String CONTEXT = "Context";

  private final DaoRegistry daoRegistry;
  private final IFhirResourceDao theDao;
  private final JpaResourceProviderDstu3 jpaResourceProvider;
  private final FhirContext fhirContext = FhirContext.forDstu3();
  private final IParser jsonParser = fhirContext.newJsonParser();
  private final ObjectMapper mapper = new ObjectMapper();

  public AuditEventInterceptor(DaoRegistry daoRegistry) {
    this.daoRegistry = daoRegistry;
    this.theDao = daoRegistry.getResourceDao(AuditEvent.class);
    this.jpaResourceProvider = new JpaResourceProviderDstu3(theDao);
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
  public void createAuditEventResourceFromUpdate (
    IBaseResource theOldResource, IBaseResource theNewResource, RequestDetails theRequestDetails, ServletRequestDetails servletRequestDetails)
    throws Exception {

    String theOldResourceString = jsonParser.encodeResourceToString(theOldResource);
    String theNewResourceString = jsonParser.encodeResourceToString(theNewResource);
    JsonNode theOldResourceJsonNode = mapper.readTree(theOldResourceString);
    JsonNode theNewResourceJsonNode = mapper.readTree(theNewResourceString);
    
    JsonNode thePatch = JsonDiff.asJson(theOldResourceJsonNode, theNewResourceJsonNode);

    String token = theRequestDetails.getHeader(CONTEXT);
    ContextService contextService = new ContextService(null, null, token);
    Context theContext = contextService.getContext();

    AuditEvent theAuditEvent = buildAuditEventResourceFromRequest(theRequestDetails, theContext, theNewResource, theOldResource, thePatch);

    MethodOutcome createAuditEventOutcome = jpaResourceProvider.create(servletRequestDetails.getServletRequest(), theAuditEvent, null, theRequestDetails);

    log.info("{} Created for the update of {})", createAuditEventOutcome.getId().toString(), theNewResource.getIdElement().toString());

  }

  @Hook(Pointcut.STORAGE_PRECOMMIT_RESOURCE_CREATED)
  public void createAuditEventResourceFromCreate (
    IBaseResource theNewResource, RequestDetails theRequestDetails, ServletRequestDetails servletRequestDetails)
    throws IOException{

    if (theNewResource.getClass() != AuditEvent.class) {
      String token = theRequestDetails.getHeader(CONTEXT);
      ContextService contextService = new ContextService(null, null, token);
      Context theContext = contextService.getContext();

      AuditEvent theAuditEvent = buildAuditEventResourceFromRequest(theRequestDetails, theContext, theNewResource, null, null);

      MethodOutcome createAuditEventOutcome = jpaResourceProvider.create(servletRequestDetails.getServletRequest(), theAuditEvent, null, theRequestDetails);

      log.info("{} Created for the creation of {})", createAuditEventOutcome.getId().toString(), theNewResource.getIdElement().toString());

      return;
    }

  }

  private AuditEvent buildAuditEventResourceFromRequest(
    RequestDetails theRequestDetails, Context theContext, IBaseResource theNewResource, IBaseResource theOldResource, JsonNode thePatch)
  {

    AuditEvent theAuditEvent = new AuditEvent();
    String theRequestTypeString = theRequestDetails.getRequestType().toString();

    Coding theTypeCoding = buildTypeCoding(theRequestTypeString);
    AuditEventAction theAction = buildAuditEventAction(theRequestTypeString);
    AuditEventAgentComponent theAgent = buildAuditEventAgentComponent(theContext);
    AuditEventOutcome theOutcome = buildAuditEventOutcome();
    AuditEventSourceComponent theSource = buildAuditEventSourceComponent();
    AuditEventEntityComponent theEntity = buildAuditEventEntityComponent(theContext, theNewResource, theOldResource, theRequestTypeString, thePatch);

    theAuditEvent
      .setType(theTypeCoding)
      .setAction(theAction)
      .setRecorded(new Date())
      .setOutcome(theOutcome)
      .addAgent(theAgent)
      .setSource(theSource)
      .addEntity(theEntity)
      .addExtension();

    return theAuditEvent;
  }

  private Coding buildTypeCoding (String theRequestTypeString) {
    Coding theCoding = new Coding();

    theCoding
      .setSystem("http://hl7.org/fhir/audit-event-type")
      .setUserSelected(false);

    if (Objects.equals(theRequestTypeString, "SEARCH")) {
      theCoding.setCode("110112").setDisplay("Query");
    } else {
      theCoding.setCode("rest").setDisplay("RESTful Operation");
    }

    return theCoding;
  }

  private AuditEvent.AuditEventAction buildAuditEventAction(String theRequestTypeString){

    switch (theRequestTypeString) {
      case "POST":   return AuditEventAction.C;
      case "GET":    return AuditEventAction.R;
      case "PUT":
      case "PATCH":  return AuditEventAction.U;
      case "DELETE": return AuditEventAction.D;
      case "SEARCH": return AuditEventAction.E;
      default:       return null;

    }
  }
  
  private AuditEventAgentComponent buildAuditEventAgentComponent(Context theContext){
    AuditEventAgentComponent theAgentComponent = new AuditEventAgentComponent();
    boolean userIsInitiator;
    boolean hasUserId = false;
    boolean hasPractitionerId = false;

    if (theContext != null) {
      User theUser = theContext.getUser();
      String thePractitionerId = theContext.getScope().getPractitionerId();

      if (theUser != null){
        hasUserId = true;
        Identifier userIdentifier = new Identifier();

        userIdentifier.setValue(theUser.getId());
        theAgentComponent.setUserId(userIdentifier);
      }

      if (thePractitionerId != null){
        hasPractitionerId = true;
        Reference theAgentReference = new Reference();
        theAgentReference.setDisplay("Practitioner");
        theAgentReference.setReference("Practitioner/" + thePractitionerId);

        theAgentComponent.setReference(theAgentReference);
      }
    }

    userIsInitiator = hasUserId || hasPractitionerId;

    // At least 1 agent is required. Requestor is the only required property for agent
    theAgentComponent.setRequestor(userIsInitiator);

    return theAgentComponent;
  }

  private AuditEventOutcome buildAuditEventOutcome() {
    // TODO: Handling only successful outcomes, for now. Need to handle errors and unsuccessful actions.
    return AuditEventOutcome._4;
  }


  private AuditEventSourceComponent buildAuditEventSourceComponent(){
    AuditEventSourceComponent theSource = new AuditEventSourceComponent();
    Identifier theSourceIdentifier = new Identifier();
    Coding theSourceCoding = new Coding();

    theSourceCoding
      .setSystem("http://hl7.org/fhir/security-source-type")
      .setCode("4")
      .setDisplay("Application Server");

    theSource
      .setIdentifier(theSourceIdentifier)
      .addType(theSourceCoding);

    return theSource;
  }

  private AuditEventEntityComponent buildAuditEventEntityComponent(
    Context theContext, IBaseResource theResource, IBaseResource theOldResource, String theRequestTypeString, JsonNode thePatch
  ){
    ArrayList<String> snapshotMethods = new ArrayList<String>() {
      {
        add("POST");
        add("PATCH");
        add("PUT");
      }
    };
    AuditEventEntityComponent theEntity = new AuditEventEntityComponent();
    AuditEventEntityDetailComponent theSnapshotDetail =  new AuditEventEntityDetailComponent();
    AuditEventEntityDetailComponent thePatchDetail =  new AuditEventEntityDetailComponent();
    Identifier theEntityIdentifier = new Identifier();
    Period theEntityIdentifierPeriod = new Period();
    Reference theEntityIdentifierAssigner = new Reference();
    Coding theEntityTypeCoding = new Coding();
    Coding theEntityRoleCoding = new Coding();

    String theResourceType = theResource.getIdElement().getResourceType();
    String theEntityIdentifierSystem = theResourceType + theResource.getIdElement().getIdPart();

    theEntityIdentifierPeriod.setStart(new Date());
    theEntityIdentifierAssigner.setReference(theContext.getScope().getOrganizationId());

    theEntityIdentifier
      .setUse(Identifier.IdentifierUse.USUAL)
      .setSystem(theEntityIdentifierSystem)
      .setValue(theResource.getIdElement().getResourceType())
      .setPeriod(theEntityIdentifierPeriod)
      .setAssigner(theEntityIdentifierAssigner);

    theEntityTypeCoding
      .setSystem("http://hl7.org/fhir/audit-entity-type")
      .setVersion("3.0.1")
      .setCode(theResourceType)
      .setDisplay(theResourceType);

    theEntityRoleCoding
      .setSystem("http://hl7.org/fhir/search-entry-mode")
      .setVersion("3.2.0")
      .setCode("match")
      .setDisplay("Match");

    theEntity
      .setIdentifier(theEntityIdentifier)
      .setType(theEntityTypeCoding)
      .setRole(theEntityRoleCoding);

    if (snapshotMethods.contains(theRequestTypeString)) {
      theSnapshotDetail
        .setType("snapshot");

        if (theRequestTypeString == "POST") {
          theSnapshotDetail.setValue(theResource.toString().getBytes());
        } else {
          theSnapshotDetail.setValue(theOldResource.toString().getBytes());
        }

      theEntity.addDetail(theSnapshotDetail);
    }

    if (thePatch != null) {
      thePatchDetail
        .setType("patch")
        .setValue(thePatch.toString().getBytes());

      theEntity.addDetail(thePatchDetail);
    }

    return theEntity;
  }

}
