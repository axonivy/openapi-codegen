package ch.ivyteam.ivy.tool.openapi.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.LoaderOptions;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.util.DeserializationUtils;

import ch.ivyteam.ivy.tool.openapi.codegen.loader.OpenApiSpecParser;

class TestOpenApiClientCodegen {

  @Test
  void simpleOpenApiDemo(@TempDir Path client) throws IOException {
    try (var in = TstRes.petstore()) {
      OpenAPI openAPISpec = new OpenApiSpecParser().parse(in);
      OpenApiCodegen generator = new OpenApiCodegen(openAPISpec);
      generator.setPackage("io.swagger.petstore");
      List<File> generated = generator.generateSources(client.toFile());
      assertPetStoreFiles(generated);
    }
  }

  private static void assertPetStoreFiles(List<File> generated) throws IOException {
    assertThat(generated).isNotEmpty();
    assertThat(generated.stream().map(File::getName))
        .as("java source files are generated from OpenAPI spec.")
        .contains("Pet.java", "Order.java");
    assertThat(generated.stream().filter(f -> !f.getName().endsWith(".java")))
        .as("supporting files (e.g. gradle builders) are not generated")
        .isEmpty();

    var sourceFile = generated.get(0);
    assertThat(Files.readString(sourceFile.toPath()))
        .as("generated to pre-set package")
        .contains("package io.swagger.petstore;");

    var petJava = generated.stream()
        .filter(f -> "Pet.java".equals(f.getName()))
        .findFirst().get();
    var pet = Files.readString(petJava.toPath(), StandardCharsets.UTF_8);
    try (var in = TstRes.load("Pet.java")) {
      var reference = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      assertThat(cleanTime(pet))
          .as("generator output and imports remain stable")
          .isEqualTo(cleanTime(reference));
    }
  }

  private static String cleanTime(String generated) {
    return generated.replaceFirst("\\@javax\\.annotation\\.Generated.*", "@javax.annotation.Generated");
  }

  @Test
  void genWithoutPackageHint(@TempDir Path client) throws IOException {
    try (var in = TstRes.petstore()) {
      OpenAPI openAPISpec = new OpenApiSpecParser().parse(in);
      OpenApiCodegen generator = new OpenApiCodegen(openAPISpec);
      List<File> generated = generator.generateSources(client.toFile());
      Path modelPath = generated.get(0).toPath();
      Path srcGenerated = client.resolve("src_generated");
      Path swaggerDefaultPath = srcGenerated.resolve("com").resolve("axonivy")
          .resolve("client").resolve("Category.java");
      assertThat(modelPath).as("package hint is optional")
          .isEqualTo(swaggerDefaultPath);
    }
  }

  @Test
  void getWithoutSwaggerSchemaDeps(@TempDir Path client) throws IOException {
    try (var in = TstRes.petstore()) {
      var openAPISpec = new OpenApiSpecParser().parse(in);
      var generator = new OpenApiCodegen(openAPISpec);
      var generated = generator.generateSources(client.toFile());
      assertThat(Files.readString(generated.get(0).toPath()))
          .doesNotContain("import io.swagger.v3.oas.annotations.media.Schema;")
          .doesNotContain("@Schema(description = ");
    }
  }

  /**
   * ISSUE XIVY-12125 OpenAPI codegen fails to create date-time instances
   */
  @Test
  void genWithoutJodaTime(@TempDir Path client) throws IOException {
    try (var in = TstRes.load("sbb-min.json")) {
      var openAPISpec = new OpenApiSpecParser().parse(in);
      var generator = new OpenApiCodegen(openAPISpec);
      var generated = generator.generateSources(client.toFile());
      assertThat(generated)
          .as("Generating a DateTime is ignored ()")
          .isEmpty();
    }
  }

  /**
   * ISSUE XIVY-5142 Can not generate OpenAPI client for 'Genesis' swagger-20 definition
   */
  @Test
  void genesisEnum(@TempDir Path client) throws IOException {
    try (var in = TstRes.load("genesis-wrapUp-api_swagger2.json")) {
      OpenAPI openAPISpec = new OpenApiSpecParser().parse(in);
      Schema<?> wrapUpMap = openAPISpec.getComponents().getSchemas().get("WrapUpCodeMapping");
      @SuppressWarnings("rawtypes")
      Map<String, Schema> fields = wrapUpMap.getProperties();
      Schema<?> defaultSetEnum = fields.get("defaultSet");
      Schema<?> mappingMapEnum = fields.get("mapping");
      assertThat(defaultSetEnum).isInstanceOf(ArraySchema.class)
          .as("Array<String> and no hint on the enum in OpenAPI model > but in GeneratorModel");
      assertThat(mappingMapEnum.getAdditionalProperties())
          .as("Array<String> and no hint on the enum in OpenAPI model > but in GeneratorModel")
          .isInstanceOf(ArraySchema.class);

      OpenApiCodegen generator = new OpenApiCodegen(openAPISpec);
      List<File> generated = generator.generateSources(client.toFile());
      assertThat(generated).as("plain source generation works").isNotEmpty();
    }
  }

  /**
   * ISSUE XIVY-11174 Can't generate OpenAPI client from huge YAML file
   */
  @Test
  void swaggerParser_yamlCodepointsAreConfigurable() throws Exception {
    OpenApiSpecParser.yamlOptions().setMaxYamlCodePoints(23);
    LoaderOptions swaggerYamlOpts = DeserializationUtils.buildLoaderOptions();
    assertThat(swaggerYamlOpts.getCodePointLimit()).isEqualTo(23);
  }

  // @Test
  // void reducedIvyLogOutput(@TempDir Path client) throws IOException {
  // loadIvyLogConfig();
  // var logger = LogManager.getLogger("io.swagger.codegen.v3.generators.java.AbstractJavaCodegen");
  // var appender = new LoggerAccess.LogAppender();
  // appender.start();
  // appender.addTo(logger);
  // try (var in = TstRes.petstore()) {
  // OpenAPI openAPISpec = new OpenApiSpecParser().parse(in);
  // OpenApiCodegen generator = new OpenApiCodegen(openAPISpec);
  // generator.setPackage("io.swagger.petstore");
  // generator.generateSources(client.toFile(), null);

  // assertThat(appender.events())
  // .as("frequent false-positive swagger warnings should not distract the user")
  // .isEmpty();
  // } finally {
  // appender.removeFrom(logger);
  // appender.stop();
  // }
  // }

  // @SuppressWarnings("restriction")
  // private void loadIvyLogConfig() {
  // ch.ivyteam.ivy.logging.LoggingSetup.loadLoggingConfig(null);
  // }

}
