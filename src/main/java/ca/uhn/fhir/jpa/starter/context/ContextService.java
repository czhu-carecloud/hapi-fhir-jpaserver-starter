package ca.uhn.fhir.jpa.starter.context;

import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.jpa.starter.context.models.Context;
import ca.uhn.fhir.jpa.starter.context.models.DataPolicyFetchBody;
import ca.uhn.fhir.jpa.starter.context.models.DataSharingPolicy;
import ca.uhn.fhir.jpa.starter.utils.IHttpClient;
import ca.uhn.fhir.jpa.starter.utils.ILRUCache;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import java.io.UnsupportedEncodingException;


public class ContextService {


  private String _settingsApi;
  private String _token;
  private Context _context;
  private Gson _gson;
  private JWTVerifier verifier;
  private IHttpClient _httpClient;
  private Boolean _contextSet;
  private ILRUCache<String, List<String>> cache;

  public ContextService(ILRUCache<String, List<String>> cache, IHttpClient httpClient, String token) {
    this.cache = cache;
    _httpClient = httpClient;
    _token = token;
    String _secret = HapiProperties.getJwtSecret();
    _settingsApi = HapiProperties.getAddressSettingsApi();
    _gson = new Gson();
    Algorithm _algorithm = Algorithm.HMAC256(_secret);
    verifier = JWT.require(_algorithm).build();
    _contextSet = false;
  }

  public void verify() throws JWTVerificationException {
    verifier.verify(_token);
  }

  private void setContext() throws UnsupportedEncodingException {
    _context = Context.getDecoded(_token);
    _contextSet = true;
  }

  public Context getContext() throws UnsupportedEncodingException {
    if (!_contextSet) {
      setContext();
    }
    return _context;
  }

  public List<String> fetchDataSharingPolicy() throws IOException {
    String roleID = getContext().getUser().getRoleId();
    // check cache for policy
    List<String> policy = cache.get(roleID);
    // if not found, fetch from settings
    if (policy == null) {
      policy = new ArrayList<>();
      String organizationID = getContext().getScope().getOrganizationId();
      DataPolicyFetchBody body = new DataPolicyFetchBody(organizationID);
      String json = _httpClient.postJson(_settingsApi + "/settings/fetch", body);
      DataSharingPolicy[] policies = _gson.fromJson(json, DataSharingPolicy[].class);
      for (DataSharingPolicy dataSharingPolicy : policies
      ) {
        policy.addAll(dataSharingPolicy.getEntityReferences());
      }
      cache.put(roleID, policy);
    }
    return policy;
  }

}
