package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.context.ContextService;
import ca.uhn.fhir.jpa.starter.context.models.Context;
import ca.uhn.fhir.jpa.starter.utils.IHttpClient;
import ca.uhn.fhir.jpa.starter.utils.ILRUCache;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * ContextInterceptor: Class to intercept the Context header and apply the correct permissions to the
 * request. GET requests are modified to scope resource according to user's data sharing policy (fetched
 * via setting API. POST/PUT/PATCH requests have context stamped on the resource.meta.tag
 */
@Interceptor
public class ContextInterceptor extends AuthorizationInterceptor {

  static final String CONTEXT = "Context";
  static final String CONTEXT_TAG_SYSTEM_CARE_CLOUD = "CodeSystem/carecloud-meta-tag-scope";
  static final String CONTEXT_TAG_CODE_SHARED = "shared";
  static final String TAG_PARAM_KEY = "_tag";
  private static final Logger ourLog = LoggerFactory.getLogger(LoggingInterceptor.class);

  private Logger myLogger;

  private ILRUCache<String, List<String>> cache;
  private IHttpClient httpClient;

  public ContextInterceptor(ILRUCache<String, List<String>> cache, IHttpClient httpClient) {
    super();
    this.cache = cache;
    this.httpClient = httpClient;
    myLogger = ourLog;
  }

  /**
   * POST/PUT/PATCH requires a context. Context is stamped on the meta.tag of any mutation.
   */
  @Override
  public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
    RequestTypeEnum requestType = theRequestDetails.getRequestType();
    if (requestType != RequestTypeEnum.GET) {
      // POST/PUT/PATCH
      String token = theRequestDetails.getHeader(CONTEXT);
      if (token == null) {
        throw new AuthenticationException("Missing Context header.");
      }
      try {
        ContextService contextService = new ContextService(cache, httpClient, token);
        contextService.verify();
        Context context = contextService.getContext();
        String organizationReference = context
          .getScope()
          .getOrganizationReference();
        for (IBaseCoding tag : theRequestDetails
          .getResource()
          .getMeta().getTag()
        ) {
          if (tag.getCode().equals(organizationReference) && tag.getSystem().equals(CONTEXT_TAG_SYSTEM_CARE_CLOUD)) {
            return new RuleBuilder().allow().write().allResources().withAnyId().build();
          }
        }
        theRequestDetails
          .getResource()
          .getMeta()
          .addTag()
          .setCode(organizationReference)
          .setSystem(CONTEXT_TAG_SYSTEM_CARE_CLOUD);
        return new RuleBuilder().allow().write().allResources().withAnyId().build();
      } catch (JWTVerificationException | UnsupportedEncodingException e) {
        myLogger.error(e.getMessage(), e);
        throw new AuthenticationException("Context header is invalid.");
      }
    }
    return new RuleBuilder()
      .allowAll()
      .build();
  }

  /**
   * GET request queries are filtered by context. (e.g. _tag=system|code)
   * All queries have access to shared resources. If a valid context is passed
   * then the data access policy for that context is fetched and included in the query.
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
  public void applyDataSharingPolicy(ServletRequestDetails theRequestDetails) throws AuthenticationException {
    if (theRequestDetails.getRequestType() == RequestTypeEnum.GET) {
      Map<String, String[]> parameters = new HashMap<>(theRequestDetails.getParameters());
      String[] tag = parameters.get(TAG_PARAM_KEY);
      if (tag == null) {
        tag = new String[0];
      }
      List<String> policyList = new ArrayList<>();
      policyList.add(CONTEXT_TAG_SYSTEM_CARE_CLOUD + "|" + CONTEXT_TAG_CODE_SHARED);
      try {
        String token = theRequestDetails.getHeader(CONTEXT);
        if (token != null) {
          ContextService contextService = new ContextService(cache, httpClient, token);
          contextService.verify();
          List<String> policy = contextService.fetchDataSharingPolicy();
          policyList.addAll(policy);
        }
      } catch (Exception e) {
        myLogger.error("Invalid Context on setting GET request access policy", e);
      } finally {
        int len = tag.length;
        String[] newTag = new String[len + 1];
        System.arraycopy(tag, 0, newTag, 0, tag.length);
        newTag[len] = String.join(",", policyList);
        parameters.put(TAG_PARAM_KEY, newTag);
        theRequestDetails.setParameters(parameters);
      }
    }
  }
}