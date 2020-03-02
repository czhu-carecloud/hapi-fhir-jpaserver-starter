package ca.uhn.fhir.jpa.starter.context;

import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.jpa.starter.context.model.Context;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;

public class ContextService {

  private String _token;
  private String _secret;
  private Context _context;
  private Gson _gson;
  private DecodedJWT _jwt;
  private Algorithm _algorithm;
  private JWTVerifier verifier;
  private Boolean _verified;
  private Boolean _contextSet = false;

  public ContextService(String token) {
    _token = token;
    _secret = HapiProperties.getJwtSecret();
    _gson = new Gson();
    _algorithm = Algorithm.HMAC256(_secret);
    verifier = JWT.require(_algorithm).build();
    _verified = false;
    _contextSet = false;
  }

  public void verify() throws JWTVerificationException {
    DecodedJWT jwt = verifier.verify(_token);
    _jwt = jwt;
    _verified = true;
  }

  private void setContext() {
    if (!_verified) {
      verify();
    }
    String serialized = _gson.toJson(_jwt.getClaims());
    _context = _gson.fromJson(serialized, Context.class);
    _contextSet = true;
  }

  public Context getContext() {
    if (!_verified) {
      verify();
    }
    if (!_contextSet) {
      setContext();
    }
    return _context;
  }

  public DecodedJWT getDecodedJWT() {
    if (!_verified) {
      verify();
    }
    return _jwt;
  }

}
