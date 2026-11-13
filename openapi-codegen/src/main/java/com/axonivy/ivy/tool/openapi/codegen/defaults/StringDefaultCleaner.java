package com.axonivy.ivy.tool.openapi.codegen.defaults;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.axonivy.ivy.tool.openapi.codegen.loader.JsonNodeVisitor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Removes string default values assignments from OpenAPI 3.1 schemas.
 * This will later be enacted by the StringDefaultAssigner.
 */
public class StringDefaultCleaner extends JsonNodeVisitor {
  private static final String COMPONENTS = "components";
  private static final String DEFAULT = "default";
  private static final String OPENAPI_31_VERSION_PREFIX = "3.1.";
  private static final String SCHEMAS = "schemas";
  private final Map<String, Map<String, String>> removedDefaults = new LinkedHashMap<>();

  public void clean(JsonNode openApi) {
    removedDefaults.clear();
    if (!openApi.path("openapi").asText().startsWith(OPENAPI_31_VERSION_PREFIX)) {
      return;
    }
    JsonNode schemas = openApi.path(COMPONENTS).path(SCHEMAS);
    captureSchemaDefaults(schemas);
    visitNode(schemas);
  }

  public void clear() {
    removedDefaults.clear();
  }

  public Map<String, Map<String, String>> removedDefaults() {
    Map<String, Map<String, String>> copy = new LinkedHashMap<>();
    removedDefaults.forEach((schema, properties) -> copy.put(schema, Map.copyOf(properties)));
    return Map.copyOf(copy);
  }

  private void captureSchemaDefaults(JsonNode schemas) {
    if (!(schemas instanceof ObjectNode schemaObj)) {
      return;
    }
    schemaObj.propertyStream().forEach(schema -> {
      var properties = schema.getValue().path("properties");
      if (!(properties instanceof ObjectNode propertiesObj)) {
        return;
      }
      propertiesObj.propertyStream().forEach(property -> {
        JsonNode defaultValue = property.getValue().path(DEFAULT);
        if (defaultValue != null &&defaultValue.isTextual()) {
          removedDefaults.computeIfAbsent(schema.getKey(), key -> new LinkedHashMap<>())
              .put(property.getKey(), defaultValue.asText());
          ((ObjectNode) property.getValue()).remove(DEFAULT);
        }
      });
    });
  }

  @Override
  protected boolean visitObjectField(ObjectNode parent, String fieldName, JsonNode node, Iterator<String> fieldIterator) {
    if (DEFAULT.equals(fieldName) && node.isTextual()) {
      fieldIterator.remove();
      return false;
    }
    return super.visitObjectField(parent, fieldName, node, fieldIterator);
  }
}