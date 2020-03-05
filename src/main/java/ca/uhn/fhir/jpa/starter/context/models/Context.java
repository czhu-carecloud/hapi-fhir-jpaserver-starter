package ca.uhn.fhir.jpa.starter.context.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;

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

  public static Context getDecoded(String encodedToken) throws UnsupportedEncodingException {
    String[] pieces = encodedToken.split("\\.");
    String b64payload = pieces[1];
    String jsonString = new String(Base64.decodeBase64(b64payload), "UTF-8");

    return new Gson().fromJson(jsonString, Context.class);
  }

  public String toString() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this);
  }
}
