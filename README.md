# OpenAPI Codegen

[![snapshot-version][snap]][sonatype]

[snap]: https://img.shields.io/maven-metadata/v?versionPrefix=1&label=dev&logo=sonatype&metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fcom%2Faxonivy%2Fivy%2Ftool%2Frest%2Fopenapi-codegen%2Fmaven-metadata.xml
[sonatype]: https://central.sonatype.com/service/rest/repository/browse/maven-snapshots/com/axonivy/ivy/tool/rest/openapi-codegen/

This tool generates an Axon Ivy compatible REST Client from an OpenAPI specification.

## Usage

### CLI

The generator can be run using Maven's CLI:

```bash
mvn com.axonivy.ivy.tool.rest:openapi-codegen:generate-openapi-client\
 -Divy.generate.openapi.client.spec=https://petstore3.swagger.io/api/v3/openapi.json\
 -Divy.generate.openapi.client.output=src_generated/rest/petstore\
 -Divy.generate.openapi.client.namespace=com.swagger.petstore.client
```

### CI

The generator can run on every build cycle of your project.
With this you don't need to include the generated sources under version control.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.axonivy.ivy.tool.rest</groupId>
      <artifactId>openapi-codegen</artifactId>
      <executions>
        <execution>
          <id>petstore.codegen</id>
          <phase>generate-sources</phase>
          <goals>
            <goal>generate-openapi-client</goal>
          </goals>
          <configuration>
            <openApiSpec>https://petstore3.swagger.io/api/v3/openapi.json</openApiSpec>
            <outputDir>${basedir}/src_generated/rest/petstore</outputDir>
            <namespace>io.swagger.petstore.client</namespace>
          </configuration>
        </execution>
      </executions>
    </plugin>
    ...
```

Note that in this setup, the `config/rest-clients.yaml` needs to be kept sync:
- the name of the client must be reflect in the `<outputDir>` right after `${basedir}/src_generated/rest/`.
- the `OpenaAPI.Namespace` must have the same value as the configured `<namespace>`.

```yaml
RestClients:
  petstore:
    Url: https://petstore3.swagger.io/api/v3
    Features:
      - ch.ivyteam.ivy.rest.client.mapper.JsonFeature
    OpenAPI:
      SpecUrl: https://petstore3.swagger.io/api/v3/openapi.json
      Namespace: io.swagger.petstore.client
```

## FAQ

- A newer swagger codegen plugin is out, which promises to fix a generator issues I have in an OpenAPI spec. Can I use it?

  > Yes, just add your preferred version to the `<dependencies>` of the plugin.
  > As long as the APIs are compatible, it will run.

  ```xml
  <plugin>
    <groupId>com.axonivy.ivy.tool.rest</groupId>
    <artifactId>openapi-codegen</artifactId>
    ...
    <dependencies>
      <dependency>
        <groupId>io.swagger.codegen.v3</groupId>
        <artifactId>swagger-codegen</artifactId>
        <version>3.0.75</version>
      </dependency>
    </dependencies>
  </plugin>


