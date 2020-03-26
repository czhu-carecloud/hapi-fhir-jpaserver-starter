package com.carecloud.fhir.hapi.stu3;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.starter.HapiProperties;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Objects;

public class CareCloudHapiProperties extends HapiProperties {

  private static String CACHE_MAX_CAPACITY = "CACHE_MAX_CAPACITY";
  private static String ADDRESS_SETTINGS_API = "ADDRESS_SETTINGS_API";

  private static String KAFKA_HOST = "KAFKA_HOST";
  private static String KAFKA_KEY = "KAFKA_KEY";
  private static String KAFKA_SECRET = "KAFKA_SECRET";
  private static String KAFKA_GROUP = "KAFKA_GROUP";
  private static String JWT_SECRET = "JWT_SECRET";
  private static String DATASOURCE_PASSWORD = "DATASOURCE_PASSWORD";
  private static String DATASOURCE_URL = "DATASOURCE_URL";
  private static String DATASOURCE_USERNAME = "DATASOURCE_USERNAME";
  private static String SERVER_ADDRESS = "SERVER_ADDRESS";
  private static String FHIR_VERSION = "FHIR_VERSION";

  private static Dotenv dotenv = Dotenv
    .configure()
    .ignoreIfMalformed()
    .ignoreIfMissing()
    .load();

  public static String getAddressSettingsApi() {
    return dotenv.get(ADDRESS_SETTINGS_API);
  }

  public static Integer getCacheMaxCapacity() {
    return Integer.parseInt(Objects.requireNonNull(dotenv.get(CACHE_MAX_CAPACITY)));
  }

  // environment variable getters

  public static String getKafkaHost() {
    return dotenv.get(KAFKA_HOST);
  }

  public static String getKafkaKey() {
    return dotenv.get(KAFKA_KEY);
  }

  public static String getKafkaSecret() {
    return dotenv.get(KAFKA_SECRET);
  }

  public static String getKafkaGroup() {
    return dotenv.get(KAFKA_GROUP);
  }

  public static String getJwtSecret() {
    return dotenv.get(JWT_SECRET);
  }

  public static String getDataSourceUrl() {
    return dotenv.get(DATASOURCE_URL);
  }

  public static String getDataSourceUsername() {
    return dotenv.get(DATASOURCE_USERNAME);
  }

  public static String getDataSourcePassword() {
    return dotenv.get(DATASOURCE_PASSWORD);
  }

  public static String getServerAddress() {
    return dotenv.get(SERVER_ADDRESS);
  }

  public static FhirVersionEnum getFhirVersion() {
    String fhirVersionString = dotenv.get(FHIR_VERSION);

    if (fhirVersionString != null && fhirVersionString.length() > 0) {
      return FhirVersionEnum.valueOf(fhirVersionString);
    }

    return FhirVersionEnum.DSTU3;
  }
}
