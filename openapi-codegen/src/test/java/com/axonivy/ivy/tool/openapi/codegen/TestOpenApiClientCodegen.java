package com.axonivy.ivy.tool.openapi.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.LoaderOptions;

import com.axonivy.ivy.tool.openapi.codegen.defaults.PropertyDefaultAssigner;
import com.axonivy.ivy.tool.openapi.codegen.loader.OpenApiSpecParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.util.DeserializationUtils;

class TestOpenApiClientCodegen {

  @Test
  void simpleOpenApiDemo(@TempDir Path client) throws IOException {
    try (var in = TstRes.petstore()) {
      OpenAPI openAPISpec = new OpenApiSpecParser().parse(in).get().api();
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
    return generated.replaceFirst("\\@jakarta\\.annotation\\.Generated.*", "@jakarta.annotation.Generated");
  }

  @Test
  void genWithoutPackageHint(@TempDir Path client) throws IOException {
    try (var in = TstRes.petstore()) {
      OpenAPI openAPISpec = new OpenApiSpecParser().parse(in).get().api();
      OpenApiCodegen generator = new OpenApiCodegen(openAPISpec);
      List<File> generated = generator.generateSources(client.toFile());
      Path modelPath = generated.get(0).toPath();
      Path swaggerDefaultPath = client.resolve("com").resolve("axonivy")
          .resolve("client").resolve("Category.java");
      assertThat(modelPath).as("package hint is optional")
          .isEqualTo(swaggerDefaultPath);
    }
  }

  @Test
  void getWithoutSwaggerSchemaDeps(@TempDir Path client) throws IOException {
    try (var in = TstRes.petstore()) {
      var openAPISpec = new OpenApiSpecParser().parse(in).get().api();
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
      var openAPISpec = new OpenApiSpecParser().parse(in).get().api();
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
      OpenAPI openAPISpec = new OpenApiSpecParser().parse(in).get().api();
      Schema<?> wrapUpMap = openAPISpec.getComponents().getSchemas().get("WrapUpCodeMapping");
      @SuppressWarnings("rawtypes")
      Map<String, Schema> fields = wrapUpMap.getProperties();
      Schema<?> defaultSetEnum = fields.get("defaultSet");
      Schema<?> mappingMapEnum = fields.get("mapping");
      assertThat(defaultSetEnum)
          .as("Array<String> and no hint on the enum in OpenAPI model > but in GeneratorModel")
          .isInstanceOf(ArraySchema.class);
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

  /**
   * ISSUE XIVY-18770 Open API 3.1 defaults
   */
  @Test
  void openApi31_defaultInitializers(@TempDir Path client) throws IOException {
    try (var in = TstRes.load("natif-31.json")) {
      var parsed = new OpenApiSpecParser().parse(in).orElseThrow();
      assertThat(parsed.rawSpec()).contains("\"default\":\"TestWorkflow\"");
      var openApi = parsed.api();
      assertThat(openApi.getOpenapi()).isEqualTo("3.1.0");
      var generated = new OpenApiCodegen(openApi, parsed.propertyDefaults()).generateSources(client.toFile());

      var processingCompleted = generated.stream()
          .filter(file -> file.getName().endsWith("ProcessingCompleted.java"))
          .findFirst().orElseThrow();
      assertThat(Files.readString(processingCompleted.toPath()))
          .contains("workflowName = \"TestWorkflow\";")
          .doesNotContain("workflowName = null;");
    }
  }

  @Test
  void openApi31_defaults_allTypes(@TempDir Path client) throws IOException {
    var spec = """
      {
        "openapi": "3.1.0",
        "info": {"title": "Defaults", "version": "1"},
        "paths": {},
        "components": {
          "schemas": {
            "DefaultValues": {
              "type": "object",
              "properties": {
                "label": {"type": "string", "default": "sample"},
                "count": {"type": "integer", "default": 3},
                "ratio": {"type": "number", "default": 0.5},
                "enabled": {"type": "boolean", "default": false},
                "state": {"type": "string", "enum": ["ready", "done"], "default": "ready"},
                "created": {"type": "string", "format": "date", "default": "2024-01-02"},
                "occurredAt": {"type": "string", "format": "date-time", "default": "2024-01-02T03:04:05Z"},
                "identifier": {"type": "string", "format": "uuid", "default": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6"},
                "labels": {
                  "type": "array",
                  "items": {"type": "string"},
                  "default": ["one", "two"]
                },
                "metadata": {
                  "type": "object",
                  "default": {"source": "sample", "count": 2, "optional": null}
                },
                "optional": {"type": ["string", "null"], "default": null}
              }
            }
          }
        }
      }
      """;
    var parsed = new OpenApiSpecParser().parse(spec).orElseThrow();
    assertThat(parsed.propertyDefaults().get("DefaultValues"))
        .containsKeys("label", "count", "ratio", "enabled", "labels", "metadata", "optional");

    var generated = new OpenApiCodegen(parsed.api(), parsed.propertyDefaults()).generateSources(client.toFile());
    var source = Files.readString(generated.stream()
        .filter(file -> "DefaultValues.java".equals(file.getName()))
        .findFirst().orElseThrow().toPath());
    assertThat(source)
        .contains("label = \"sample\";")
        .contains("count = Integer.valueOf(\"3\");")
        .contains("ratio = Double.valueOf(\"0.5\");")
        .contains("enabled = false;")
        .contains("state = \"ready\";")
        .contains("created = \"2024-01-02\";")
        .contains("occurredAt = \"2024-01-02T03:04:05Z\";")
        .contains("identifier = \"f81d4fae-7dec-11d0-a765-00a0c91e6bf6\";")
        .contains("labels = java.util.Arrays.asList(\"one\", \"two\");")
        .contains("metadata = new java.util.LinkedHashMap<String, Object>() {{")
        .contains("put(\"count\", Integer.valueOf(\"2\"));")
        .contains("put(\"optional\", null);")
        .contains("optional = null;");
    compile(generated, client);
  }

  @Test
  void openApi31_defaults_inNestedAndComposedSchemas(@TempDir Path client) throws IOException {
    var spec = """
      {
        "openapi": "3.1.0",
        "info": {"title": "Nested defaults", "version": "1"},
        "paths": {},
        "components": {
          "schemas": {
            "Parent": {
              "type": "object",
              "properties": {
                "child": {
                  "type": "object",
                  "properties": {
                    "nestedName": {"type": "string", "default": "nested"}
                  }
                }
              }
            },
            "AllOfParent": {
              "allOf": [
                {"type": "object", "properties": {"baseName": {"type": "string"}}},
                {"type": "object", "properties": {
                  "allOfName": {"type": "string", "default": "allOf"}
                }}
              ]
            },
            "AnyOfParent": {
              "anyOf": [
                {"type": "object", "properties": {
                  "anyOfName": {"type": "string", "default": "anyOf"}
                }},
                {"type": "object", "properties": {"otherName": {"type": "string"}}}
              ]
            }
          }
        }
      }
      """;
    var parsed = new OpenApiSpecParser().parse(spec).orElseThrow();

    assertThat(parsed.propertyDefaults().values().stream()
        .flatMap(properties -> properties.values().stream())
        .map(value -> java.util.Objects.requireNonNull(value).asText())
        .toList()).contains("nested", "allOf", "anyOf");
    assertThat(parsed.propertyDefaults().get("Parent_child"))
        .containsEntry("nestedName", TextNode.valueOf("nested"));
    assertThat(parsed.propertyDefaults().get("AllOfParent"))
        .containsEntry("allOfName", TextNode.valueOf("allOf"));
    assertThat(parsed.propertyDefaults().get("AnyOfParent"))
        .containsEntry("anyOfName", TextNode.valueOf("anyOf"));

    var generated = new OpenApiCodegen(parsed.api(), parsed.propertyDefaults()).generateSources(client.toFile());
    assertThat(generated.stream().map(file -> java.util.Objects.requireNonNull(file).getName()).toList())
        .contains("ParentChild.java", "AllOfParent.java", "AnyOfParent.java");
    var childModel = generated.stream().filter(file -> "ParentChild.java".equals(file.getName())).findFirst().orElseThrow();
    var childSource = Files.readString(childModel.toPath());
    assertThat(childSource).contains("nestedName = \"nested\";");
    compile(generated, client);
  }

  @Test
  void textualDefaults_restoreEnumsAndFormattedTypes() {
    var generatedModel = """
      public class TypedDefaults {
        public enum StatusEnum {
          READY("ready");
          private final String value;
          StatusEnum(String value) { this.value = value; }
          public static StatusEnum fromValue(String input) {
            return READY;
          }
        }
        private StatusEnum state = null;
        private java.time.LocalDate created = null;
        private java.time.OffsetDateTime occurredAt = null;
        private java.util.UUID identifier = null;
        private java.io.File path = null;
        private java.util.List<java.io.File> paths = null;
        private java.util.Map<String, java.io.File> metadata = null;
      }
      """;
    Map<String, Map<String, JsonNode>> defaults = Map.of("TypedDefaults", Map.of(
        "state", TextNode.valueOf("ready"),
        "created", TextNode.valueOf("2024-01-02"),
        "occurredAt", TextNode.valueOf("2024-01-02T03:04:05Z"),
        "identifier", TextNode.valueOf("f81d4fae-7dec-11d0-a765-00a0c91e6bf6"),
        "path", TextNode.valueOf("/tmp/default"),
        "paths", JsonNodeFactory.instance.arrayNode().add("/tmp/default"),
        "metadata", JsonNodeFactory.instance.objectNode().put("path", "/tmp/default")));

    var assigner = new PropertyDefaultAssigner(defaults, new io.swagger.codegen.v3.generators.java.JavaClientCodegen());
    var restored = assigner.apply("TypedDefaults.java", generatedModel);

    assertThat(restored)
        .contains("state = StatusEnum.fromValue(\"ready\");")
        .contains("created = java.time.LocalDate.parse(\"2024-01-02\");")
        .contains("occurredAt = java.time.OffsetDateTime.parse(\"2024-01-02T03:04:05Z\");")
        .contains("identifier = java.util.UUID.fromString(\"f81d4fae-7dec-11d0-a765-00a0c91e6bf6\");")
        .contains("path = null;")
        .contains("paths = null;")
        .contains("metadata = null;");
  }

  @Test
  void openApi31_defaults_inInlineRequestAndResponse(@TempDir Path client) throws IOException {
    var spec = """
      {
        "openapi": "3.1.0",
        "info": {"title": "Inline defaults", "version": "1"},
        "paths": {
          "/widgets": {
            "post": {
              "operationId": "createWidget",
              "requestBody": {
                "content": {
                  "application/json": {
                    "schema": {
                      "type": "object",
                      "properties": {
                        "label": {"type": "string", "default": "inline"}
                      }
                    }
                  }
                }
              },
              "responses": {
                "200": {
                  "description": "Created",
                  "content": {
                    "application/json": {
                      "schema": {
                        "type": "object",
                        "properties": {
                          "status": {"type": "string", "default": "ready"}
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      """;
    var parsed = new OpenApiSpecParser().parse(spec).orElseThrow();
    assertThat(parsed.propertyDefaults().values().stream()
        .map(properties -> properties.get("label"))
        .filter(java.util.Objects::nonNull)
        .map(JsonNode::asText)
        .toList()).containsExactly("inline");
    assertThat(parsed.propertyDefaults().values().stream()
        .map(properties -> properties.get("status"))
        .filter(java.util.Objects::nonNull)
        .map(JsonNode::asText)
        .toList()).containsExactly("ready");
    var generated = new OpenApiCodegen(parsed.api(), parsed.propertyDefaults()).generateSources(client.toFile());
    var modelSources = generated.stream()
        .filter(file -> {
          try {
            String source = Files.readString(file.toPath());
            return source.contains("label = \"inline\";") || source.contains("status = \"ready\";");
          } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
          }
        })
        .map(file -> {
          try {
            return Files.readString(file.toPath());
          } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
          }
        })
        .toList();
    assertThat(modelSources)
        .anySatisfy(source -> assertThat(source).contains("label = \"inline\";"))
        .anySatisfy(source -> assertThat(source).contains("status = \"ready\";"));
    compile(generated, client);
  }

  private static void compile(List<File> sources, @TempDir Path client) throws IOException {
    var compiler = ToolProvider.getSystemJavaCompiler();
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    var classes = client.resolve("classes");
    Files.createDirectories(classes);
    try (var fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
      var units = fileManager.getJavaFileObjectsFromFiles(sources);
      var compiled = compiler.getTask(null, fileManager, diagnostics,
          List.of("-classpath", System.getProperty("java.class.path"), "-proc:none", "-d", classes.toString()),
          null, units).call();
      assertThat(compiled)
          .withFailMessage("Generated support spec models failed to compile: %s", diagnostics.getDiagnostics())
          .isTrue();
    }
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
