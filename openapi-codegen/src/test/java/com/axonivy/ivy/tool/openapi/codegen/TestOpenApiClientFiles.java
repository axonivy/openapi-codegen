package com.axonivy.ivy.tool.openapi.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestOpenApiClientFiles {

  @Test
  void backup_yaml(@TempDir Path out) {
    new OpenApiClientFiles(out).backupSpec("""
      openapi: 3.0.2
        info:
          title: Swagger Petstore - OpenAPI 3.0
      """);
    assertThat(out.resolve("openapi.yaml"))
        .exists();
  }

  @Test
  void backup_json(@TempDir Path out) {
    var clientOut = out.resolve("yamlClient");
    new OpenApiClientFiles(clientOut).backupSpec("""
      {
        "openapi": "3.0.2",
        "info": {
            "title": "Swagger Petstore - OpenAPI 3.0"
        }
      }
      """);
    assertThat(clientOut.resolve("openapi.json"))
        .exists();
  }

}
