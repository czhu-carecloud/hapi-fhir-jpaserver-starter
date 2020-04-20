FROM hapiproject/hapi:base as build-hapi

ARG HAPI_FHIR_URL=https://github.com/jamesagnew/hapi-fhir/
ARG HAPI_FHIR_BRANCH=v4.2.0
ARG HAPI_FHIR_STARTER_URL=https://github.com/hapifhir/hapi-fhir-jpaserver-starter/
ARG HAPI_FHIR_STARTER_BRANCH=v4.2.0

RUN git clone --branch ${HAPI_FHIR_BRANCH} ${HAPI_FHIR_URL}
WORKDIR /tmp/hapi-fhir/
RUN /tmp/apache-maven-3.6.2/bin/mvn dependency:resolve
RUN /tmp/apache-maven-3.6.2/bin/mvn install -DskipTests

WORKDIR /tmp
COPY . ./hapi-fhir-jpaserver-starter

# If we need CareCloud dependencies, pass valid ssh token as arg
#ARG TOKEN
#RUN git config --global url."https://service-carecloud:${TOKEN}@github.com/".insteadOf "https://github.com/"

WORKDIR /tmp/hapi-fhir-jpaserver-starter
RUN /tmp/apache-maven-3.6.2/bin/mvn clean install -DskipTests

FROM tomcat:9-jre11

RUN mkdir -p /data/hapi/lucenefiles && chmod 775 /data/hapi/lucenefiles
COPY --from=build-hapi /tmp/hapi-fhir-jpaserver-starter/target/*.war /usr/local/tomcat/webapps/

# Use port 80 instead of default tomcat server port 8080
RUN sed -i 's/port="8080"/port="80"/' /usr/local/tomcat/conf/server.xml

EXPOSE 80

CMD ["catalina.sh", "run"]