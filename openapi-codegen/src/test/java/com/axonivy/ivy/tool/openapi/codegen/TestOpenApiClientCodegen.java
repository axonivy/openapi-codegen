package com.axonivy.ivy.tool.openapi.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.LoaderOptions;

import com.axonivy.ivy.tool.openapi.codegen.loader.OpenApiSpecParser;

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
      var parser = new OpenApiSpecParser();
      var parsed = parser.parse(in).orElseThrow();
      assertThat(parsed.rawSpec())
        .contains("\"default\":\"TestWorkflow\"");
      var openApi = parsed.api();
      assertThat(openApi.getOpenapi())
        .isEqualTo("3.0.3");
      var generated = new OpenApiCodegen(openApi).generateSources(client.toFile());

      var processingCompleted = generated.stream()
          .filter(file -> file.getName().endsWith("ProcessingCompleted.java"))
          .findFirst().orElseThrow();
      assertThat(Files.readString(processingCompleted.toPath()))
          .contains("workflowName = \"TestWorkflow\";")
          .doesNotContain("workflowName = null;");

      var result = compile(generated, client);
      assertThat(result.diagnostics())
        .as("no compiler issues with OpenAPI 3.1 natif-31.json")
        .isEmpty();
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
                "identifierNull": {"type": "string", "format": "uuid", "default": "null"},
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
    var generated = new OpenApiCodegen(parsed.api()).generateSources(client.toFile());
    var source = Files.readString(generated.stream()
        .filter(file -> "DefaultValues.java".equals(file.getName()))
        .findFirst().orElseThrow().toPath());
    assertThat(source)
        .contains("label = \"sample\";")
        .contains("count = 3;")
        .contains("ratio = new BigDecimal(0.5);")
        .contains("enabled = false;")
        .contains("state = StateEnum.READY;")
        .contains("created = LocalDate.parse(\"2024-01-02\");")
        .contains("occurredAt = OffsetDateTime.parse(\"2024-01-02T03:04:05Z\");")
        .contains("identifier = UUID.fromString(\"f81d4fae-7dec-11d0-a765-00a0c91e6bf6\");")
        .contains("identifierNull = null;")
        .contains("labels = null;")
        .contains("metadata = null;")
        .contains("optional = null;");
    assertThat(compile(generated, client).success()).isTrue();
  }

  @Test
  void openApi31_stringPropertiesNormalizeForCodegen(@TempDir Path client) throws IOException {
    var spec = """
        {
          "openapi": "3.1.0",
          "info": {"title": "String types", "version": "1"},
          "paths": {},
          "components": {
            "schemas": {
              "StringTypes": {
                "type": "object",
                "properties": {
                  "plainText": {"type": "string"},
                  "uuidText": {"type": "string", "format": "uuid"},
                  "nullableText": {"type": ["string", "null"]}
                }
              }
            }
          }
        }
        """;
    var parsed = new OpenApiSpecParser().parse(spec).orElseThrow();
        var schemas = parsed.api().getComponents().getSchemas();
        assertThat(schemas).containsKey("StringTypes");
        var properties = schemas.get("StringTypes").getProperties();
    var plainText = (Schema<?>) properties.get("plainText");
    var uuidText = (Schema<?>) properties.get("uuidText");
    var nullableText = (Schema<?>) properties.get("nullableText");

    assertThat(parsed.api().getOpenapi()).isEqualTo("3.0.3");
    assertThat(plainText.getTypes()).contains("string");
    assertThat(plainText.getType()).isEqualTo("string");
    assertThat(uuidText.getType()).isEqualTo("string");
    assertThat(uuidText.getFormat()).isEqualTo("uuid");
    assertThat(nullableText.getType()).isEqualTo("string");
    assertThat(nullableText.getNullable()).isTrue();

    var codegen31 = new io.swagger.codegen.v3.generators.java.JavaClientCodegen();
    var model31 = codegen31.fromModel("StringTypes", parsed.api().getComponents().getSchemas().get("StringTypes"));
    assertThat(model31.vars.stream().filter(property -> "plainText".equals(property.name))
      .findFirst().orElseThrow().datatype).isEqualTo("String");
    assertThat(model31.vars.stream().filter(property -> "uuidText".equals(property.name))
      .findFirst().orElseThrow().datatype).isEqualTo("UUID");

    var openApi30Spec = spec.replace("[\"string\", \"null\"]", "\"string\", \"nullable\": true")
      .replace("3.1.0", "3.0.3");
    var openApi30 = new OpenApiSpecParser().parse(openApi30Spec).orElseThrow();
    var model30 = new io.swagger.codegen.v3.generators.java.JavaClientCodegen().fromModel(
      "StringTypes", openApi30.api().getComponents().getSchemas().get("StringTypes"));
    assertThat(model30.vars.stream().filter(property -> "plainText".equals(property.name))
      .findFirst().orElseThrow().datatype).isEqualTo("String");
    assertThat(model30.vars.stream().filter(property -> "uuidText".equals(property.name))
      .findFirst().orElseThrow().datatype).isEqualTo("UUID");
    var generated30 = new OpenApiCodegen(openApi30.api()).generateSources(client.resolve("oas30").toFile());
    var source30 = Files.readString(generated30.stream()
        .filter(file -> "StringTypes.java".equals(file.getName()))
        .findFirst().orElseThrow().toPath());
    assertThat(source30).contains("private String plainText = null;", "private UUID uuidText = null;");

    var generated31 = new OpenApiCodegen(parsed.api()).generateSources(client.resolve("oas31").toFile());
    var source31 = Files.readString(generated31.stream()
      .filter(file -> "StringTypes.java".equals(file.getName()))
      .findFirst().orElseThrow().toPath());
    assertThat(source31).contains("private String plainText = null;", "private UUID uuidText = null;");
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

    var allOfParent = parsed.api().getComponents().getSchemas().get("AllOfParent");
    var firstAllOfSchema = (Schema<?>) allOfParent.getAllOf().get(0);
    var allOfBaseName = (Schema<?>) firstAllOfSchema.getProperties().get("baseName");
    assertThat(allOfBaseName.getTypes()).contains("string");
    assertThat(allOfBaseName.getType()).isEqualTo("string");

    var generated = new OpenApiCodegen(parsed.api()).generateSources(client.toFile());
    assertThat(generated.stream().map(file -> java.util.Objects.requireNonNull(file).getName()).toList())
        .contains("ParentChild.java", "AllOfParent.java", "AnyOfParent.java");
    var childModel = generated.stream().filter(file -> "ParentChild.java".equals(file.getName())).findFirst().orElseThrow();
    var childSource = Files.readString(childModel.toPath());
    assertThat(childSource).contains("nestedName = \"nested\";");
    assertThat(compile(generated, client).success()).isTrue();
  }

  @Test
  void openApi31_singleAllOfRef_toDirectTypeRef(@TempDir Path client) throws IOException {
    var spec = """
      {
        "openapi": "3.1.0",
        "info": {"title": "natiF language", "version": "1"},
        "paths": {},
        "components": {
          "schemas": {
            "DeEnLanguage": {
              "type": "string",
              "enum": ["de", "en"],
              "title": "DeEnLanguage"
            },
            "TYOProcessingParameters": {
              "type": "object",
              "properties": {
                "language": {
                  "allOf": [{"$ref": "#/components/schemas/DeEnLanguage"}],
                  "description": "Language for processing"
                }
              }
            }
          }
        }
      }
      """;
    var parsed = new OpenApiSpecParser().parse(spec).orElseThrow();

    var language = (Schema<?>) parsed.api().getComponents().getSchemas()
      .get("TYOProcessingParameters").getProperties()
      .get("language");
    assertThat(language.get$ref())
      .as("The 'language' property is transformed to a direct $ref")
      .isEqualTo("#/components/schemas/DeEnLanguage");

    var generated = new OpenApiCodegen(parsed.api()).generateSources(client.toFile());
    var parameters = generated.stream()
        .filter(file -> "TYOProcessingParameters.java".equals(file.getName()))
        .findFirst().orElseThrow();
    assertThat(Files.readString(parameters.toPath()))
        .contains("private DeEnLanguage language = null;");
    assertThat(compile(generated, client).success()).isTrue();
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
    var generated = new OpenApiCodegen(parsed.api()).generateSources(client.toFile());
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
    assertThat(compile(generated, client).success()).isTrue();
  }

  private static CompileResult compile(List<File> sources, @TempDir Path client) throws IOException {
    var compiler = ToolProvider.getSystemJavaCompiler();
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    var classes = client.resolve("classes");
    Files.createDirectories(classes);
    try (var fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
      var units = fileManager.getJavaFileObjectsFromFiles(sources);
      CompilationTask task = compiler.getTask(null, fileManager, diagnostics,
          List.of("-classpath", System.getProperty("java.class.path"), "-proc:none", "-d", classes.toString()),
          null, units);
      var compiled = task.call();
      return new CompileResult(compiled, diagnostics.getDiagnostics());
    }
  }

  private record CompileResult(Boolean success, List<Diagnostic<? extends JavaFileObject>> diagnostics){
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
