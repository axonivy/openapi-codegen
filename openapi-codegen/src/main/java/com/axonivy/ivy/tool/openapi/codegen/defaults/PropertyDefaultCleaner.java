package com.axonivy.ivy.tool.openapi.codegen.defaults;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Removes component property defaults before Swagger parses OpenAPI 3.1 schemas.
 */
public class PropertyDefaultCleaner {
  private static final String COMPONENTS = "components";
  private static final String DEFAULT = "default";
  private static final String OPENAPI_31_VERSION_PREFIX = "3.1.";
  private static final String SCHEMAS = "schemas";
  private final Map<String, Map<String, JsonNode>> removedDefaults = new LinkedHashMap<>();

  public void clean(JsonNode openApi) {
    removedDefaults.clear();
    if (!openApi.path("openapi").asText().startsWith(OPENAPI_31_VERSION_PREFIX)) {
      return;
    }
    JsonNode schemas = openApi.path(COMPONENTS).path(SCHEMAS);
    if (!(schemas instanceof ObjectNode schemaObject)) {
      return;
    }
    schemaObject.propertyStream().forEach(schema -> {
      JsonNode properties = schema.getValue().path("properties");
      if (!(properties instanceof ObjectNode propertyObject)) {
        return;
      }
      propertyObject.propertyStream().forEach(property -> {
        if (!(property.getValue() instanceof ObjectNode propertySchema)) {
          return;
        }
        JsonNode defaultValue = propertySchema.get(DEFAULT);
        if (defaultValue != null) {
          removedDefaults.computeIfAbsent(schema.getKey(), key -> new LinkedHashMap<>())
              .put(property.getKey(), defaultValue.deepCopy());
          propertySchema.remove(DEFAULT);
        }
      });
    });
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