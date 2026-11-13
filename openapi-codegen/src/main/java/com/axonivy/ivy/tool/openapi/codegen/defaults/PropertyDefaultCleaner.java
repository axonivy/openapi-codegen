package com.axonivy.ivy.tool.openapi.codegen.defaults;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Removes model property defaults before Swagger parses OpenAPI 3.1 schemas.
 */
public class PropertyDefaultCleaner {
  private static final String COMPONENTS = "components";
  private static final String DEFAULT = "default";
  private static final String OPENAPI_31_VERSION_PREFIX = "3.1.";
  private static final String SCHEMAS = "schemas";
  private static final Set<String> COMPOSITION_KEYWORDS = Set.of("allOf", "anyOf", "oneOf");
  private static final Set<String> HTTP_METHODS = Set.of(
      "get", "put", "post", "delete", "options", "head", "patch", "trace");
  private final Map<String, Map<String, JsonNode>> removedDefaults = new LinkedHashMap<>();

  public void clean(JsonNode openApi) {
    removedDefaults.clear();
    if (!openApi.path("openapi").asText().startsWith(OPENAPI_31_VERSION_PREFIX)) {
      return;
    }
    JsonNode schemas = openApi.path(COMPONENTS).path(SCHEMAS);
    if (schemas instanceof ObjectNode schemaObject) {
      schemaObject.propertyStream().forEach(schema -> cleanModelSchema(schema.getValue(), schema.getKey()));
    }
    cleanPathItems(openApi.path("paths"));
    cleanPathItems(openApi.path("webhooks"));
  }

  private void cleanPathItems(JsonNode pathItems) {
    if (!(pathItems instanceof ObjectNode pathItemObject)) {
      return;
    }
    pathItemObject.propertyStream().forEach(pathItem -> {
      if (!(pathItem.getValue() instanceof ObjectNode operations)) {
        return;
      }
      String modelPrefix = pathToModelPrefix(pathItem.getKey());
      operations.propertyStream()
          .filter(operation -> HTTP_METHODS.contains(operation.getKey()))
          .forEach(operation -> cleanOperation(operation.getValue(), modelPrefix));
    });
  }

  private void cleanOperation(JsonNode operation, String modelPrefix) {
    cleanContentSchema(operation.path("requestBody").path("content"), modelPrefix + "_body");

    JsonNode responses = operation.path("responses");
    if (responses instanceof ObjectNode responseObject) {
      responseObject.propertyStream().forEach(response -> cleanContentSchema(response.getValue().path("content"),
          "inline_response_" + response.getKey()));
    }
  }

  private static String pathToModelPrefix(String path) {
    return path.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
  }

  private void cleanContentSchema(JsonNode content, String modelName) {
    if (!(content instanceof ObjectNode contentObject)) {
      return;
    }
    contentObject.propertyStream().forEach(mediaType -> cleanModelSchema(mediaType.getValue().path("schema"), modelName));
  }

  private void cleanModelSchema(JsonNode schema, String modelName) {
    if (!(schema instanceof ObjectNode)) {
      return;
    }
    JsonNode properties = schema.path("properties");
    if (properties instanceof ObjectNode propertyObject) {
      propertyObject.propertyStream().forEach(property -> {
        if (!(property.getValue() instanceof ObjectNode propertySchema)) {
          return;
        }
        JsonNode defaultValue = propertySchema.get(DEFAULT);
        if (defaultValue != null) {
          removedDefaults.computeIfAbsent(modelName, key -> new LinkedHashMap<>())
              .put(property.getKey(), defaultValue.deepCopy());
          propertySchema.remove(DEFAULT);
        }
        cleanModelSchema(propertySchema, modelName + "_" + property.getKey());
      });
    }

    for (String keyword : COMPOSITION_KEYWORDS) {
      for (JsonNode branch : schema.path(keyword)) {
        cleanModelSchema(branch, modelName);
      }
    }
    cleanModelSchema(schema.path("items"), modelName);
    cleanModelSchema(schema.path("additionalProperties"), modelName);
    cleanModelSchema(schema.path("not"), modelName);
    cleanModelSchema(schema.path("if"), modelName);
    cleanModelSchema(schema.path("then"), modelName);
    cleanModelSchema(schema.path("else"), modelName);
  }

  public void clear() {
    removedDefaults.clear();
  }

  public Map<String, Map<String, JsonNode>> removedDefaults() {
    Map<String, Map<String, JsonNode>> copy = new LinkedHashMap<>();
    removedDefaults.forEach((schema, properties) -> {
      Map<String, JsonNode> propertyCopy = new LinkedHashMap<>();
      properties.forEach((property, value) -> propertyCopy.put(property, value.deepCopy()));
      copy.put(schema, Map.copyOf(propertyCopy));
    });
    return Map.copyOf(copy);
  }
}