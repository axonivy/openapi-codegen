package com.axonivy.ivy.tool.openapi.codegen.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class OpenApi31CompatibilityNormalizer extends JsonNodeVisitor {
  private static final String OPENAPI_31_VERSION_PREFIX = "3.1.";
  private final List<TypeChange> typeChanges = new ArrayList<>();

  static String normalize(JsonNode document) {
    if (!(document instanceof ObjectNode root)) {
      return null;
    }
    String originalVersion = root.path("openapi").asText();
    if (!originalVersion.startsWith(OPENAPI_31_VERSION_PREFIX) || !supportsTypeArrays(root)) {
      return null;
    }
    var normalizer = new OpenApi31CompatibilityNormalizer();
    normalizer.visitNode(root);
    for (TypeChange typeChange : normalizer.typeChanges) {
      typeChange.apply();
    }
    root.put("openapi", "3.0.3");
    return originalVersion;
  }

  private static boolean supportsTypeArrays(JsonNode node) {
    if (node.isArray()) {
      for (JsonNode child : node) {
        if (!supportsTypeArrays(child)) {
          return false;
        }
      }
    } else if (node.isObject()) {
      JsonNode type = node.get("type");
      if (type != null && type.isArray()) {
        Set<String> nonNullTypes = new LinkedHashSet<>();
        for (JsonNode typeNode : type) {
          if (!typeNode.isTextual()) {
            return false;
          }
          if (!"null".equals(typeNode.asText())) {
            nonNullTypes.add(typeNode.asText());
          }
        }
        if (nonNullTypes.size() != 1) {
          return false;
        }
      }
      for (var field : node.properties()) {
        if (!supportsTypeArrays(field.getValue())) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  protected boolean visitObjectField(ObjectNode parent, String fieldName, JsonNode node,
      java.util.Iterator<String> fieldIterator) {
    if ("type".equals(fieldName) && node.isArray()) {
      String type = null;
      boolean nullable = false;
      for (JsonNode typeNode : node) {
        if ("null".equals(typeNode.asText())) {
          nullable = true;
        } else {
          type = typeNode.asText();
        }
      }
      typeChanges.add(new TypeChange(parent, type, nullable));
      return false;
    }
    return super.visitObjectField(parent, fieldName, node, fieldIterator);
  }

  private record TypeChange(ObjectNode schema, String type, boolean nullable) {
    private void apply() {
      schema.put("type", type);
      if (nullable) {
        schema.put("nullable", true);
      }
    }
  }
}