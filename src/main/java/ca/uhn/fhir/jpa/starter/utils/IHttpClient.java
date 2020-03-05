package ca.uhn.fhir.jpa.starter.utils;

import java.io.IOException;

public interface IHttpClient {
  public String postJson(String url, Object body) throws IOException;
}
