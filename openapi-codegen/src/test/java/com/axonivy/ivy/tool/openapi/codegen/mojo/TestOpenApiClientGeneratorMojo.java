package com.axonivy.ivy.tool.openapi.codegen.mojo;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.axonivy.ivy.tool.openapi.codegen.TstRes;

@MojoTest
class TestOpenApiClientGeneratorMojo {

  private OpenApiClientGeneratorMojo mojo;

  @BeforeEach
  @InjectMojo(goal = OpenApiClientGeneratorMojo.GOAL)
  void setUp(OpenApiClientGeneratorMojo openApi) {
    this.mojo = openApi;
  }

  @Test
  void generate(@TempDir Path out) throws Exception {
    mojo.openApiSpec = TstRes.petstoreUri().toURL();
    mojo.clientPackage = "com.swagger.petstore";
    mojo.outputDir = out;
    mojo.execute();

    var petstore = out.resolve("com/swagger/petstore");
    try (var sources = Files.list(petstore)) {
      assertThat(sources)
          .extracting(p -> p.getFileName().toString())
          .contains("Pet.java");
    }

    assertThat(out.resolve("openapi.json"))
        .as("Used OpenAPI spec is preserved as file; for OpenAPI inscription features")
        .exists();
  }

}
