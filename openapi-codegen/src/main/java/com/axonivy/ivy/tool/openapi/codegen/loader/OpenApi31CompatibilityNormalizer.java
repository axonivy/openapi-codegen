package com.axonivy.ivy.tool.openapi.codegen.loader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Downgrade OpenAPI 3.1 into a OpenAPI 3 compliant version:
 * - remove type array: with 'null' items; present as 'nullable=true'
 * - set version to 3.0.x; let swagger codegen work in 3.0 mode (avoid swagger2 fallback)
 */
final class OpenApi31CompatibilityNormalizer extends JsonNodeVisitor {

  private static final String OPENAPI_31_VERSION_PREFIX = "3.1.";

  private static final String TYPE = "type";
  private static final String NULL = "null";

  private final List<TypeChange> typeChanges = new ArrayList<>();

  static String normalizeTo30(JsonNode document) {
    if (!(document instanceof ObjectNode root)) {
      return null;
    }
    String originalVersion = root.path("openapi").asText();
    if (!originalVersion.startsWith(OPENAPI_31_VERSION_PREFIX)) {
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

  @Override
  protected boolean visitObjectField(ObjectNode parent, String fieldName, JsonNode node, Iterator<String> fields) {
    if (TYPE.equals(fieldName) && node.isArray()) {
      String type = null;
      boolean nullable = false;
      for (JsonNode typeNode : node) {
        if (NULL.equals(typeNode.asText())) {
          nullable = true;
        } else {
          type = typeNode.asText();
        }
      }
      typeChanges.add(new TypeChange(parent, type, nullable));
      return false;
    }
    return super.visitObjectField(parent, fieldName, node, fields);
  }

  private record TypeChange(ObjectNode schema, String type, boolean nullable) {
    private void apply() {
      schema.put(TYPE, type);
      if (nullable) {
        schema.put("nullable", true);
      }
    }
  }
}
