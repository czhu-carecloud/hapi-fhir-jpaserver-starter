package ca.uhn.fhir.jpa.starter.utils;

import com.github.dnault.xmlpatch.repackaged.org.apache.commons.io.Charsets;
import com.google.gson.Gson;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HttpClient implements IHttpClient {
  private CloseableHttpClient httpClient;
  private Gson _gson;

  public HttpClient() {
    httpClient = HttpClients.createDefault();
    _gson = new Gson();
  }

  public String postJson(String url, Object body) throws IOException {
    HttpEntity httpEntity = new StringEntity(_gson.toJson(body), ContentType.APPLICATION_JSON);
    HttpPost httpPost = new HttpPost(url);
    httpPost.setEntity(httpEntity);
    CloseableHttpResponse response = httpClient.execute(httpPost);
    HttpEntity entity = response.getEntity();
    Header encodingHeader = entity.getContentEncoding();

    Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 :
      Charsets.toCharset(encodingHeader.getValue());

    String json = EntityUtils.toString(entity, encoding);
    response.close();
    return json;
  }
}
