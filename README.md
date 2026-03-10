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
 -Divy.generate.openapi.client.package=com.swagger.petstore.client
```

