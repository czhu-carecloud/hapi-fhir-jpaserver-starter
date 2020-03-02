package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.context.ContextService;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwt.exceptions.JWTVerificationException;

@Interceptor
public class ContextInterceptor {

  static final String CONTEXT = "Context";

  @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
  public void verify(RequestDetails theRequestDetails) throws AuthenticationException {
    RequestTypeEnum requestType = theRequestDetails.getRequestType();
    if (requestType != RequestTypeEnum.GET) {
      String token = theRequestDetails.getHeader(CONTEXT);
      if (token == null) {
        throw new AuthenticationException("Not Authorized.");
      }
      try {
        new ContextService(token).verify();
      } catch (JWTVerificationException e) {
        throw new AuthenticationException("Invalid Authentication Token.");
      }
    }
  }
}
