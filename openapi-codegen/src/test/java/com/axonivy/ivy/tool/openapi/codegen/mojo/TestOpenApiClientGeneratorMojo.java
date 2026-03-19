package com.axonivy.ivy.tool.openapi.codegen.mojo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
    mojo.openApiSpec = TstRes.petstoreUri().toString();
    mojo.namespace = "com.swagger.petstore";
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

  @Test
  void regenerate_cleanup(@TempDir Path out) throws Exception {
    mojo.openApiSpec = TstRes.petstoreUri().toString();
    mojo.namespace = "com.swagger.petstore";
    mojo.outputDir = out;

    var legacy = out.resolve("legacy").resolve("MyClient.java");
    Files.createDirectories(legacy.getParent());
    Files.writeString(legacy, "package legacy;", StandardOpenOption.CREATE_NEW);

    mojo.execute();

    assertThat(legacy)
        .as("existing client is removed before re-generation")
        .doesNotExist();
  }

  @Test
  void openApiSpec_asUri() throws Exception {
    URI uri = TstRes.petstoreUri();
    assertThat(uri.toString()).startsWith("file:");

    assertThat(OpenApiClientGeneratorMojo.specResource(uri.toString()).toURI())
        .isEqualTo(uri);
  }

  @Test
  void openApiSpec_asPath() throws Exception {
    URI uri = TstRes.petstoreUri();
    String path = Path.of(uri).toString();
    assertThat(path)
        .as("plain filesystem paths are supported")
        .doesNotStartWith("file:");

    assertThat(OpenApiClientGeneratorMojo.specResource(path).toURI())
        .isEqualTo(uri);
  }

}
