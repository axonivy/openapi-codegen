package com.axonivy.ivy.tool.openapi.codegen.loader;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class StringDefaultCleaner extends JsonNodeVisitor {
  private static final String COMPONENTS = "components";
  private static final String DEFAULT = "default";
  private static final String OPENAPI_31_VERSION_PREFIX = "3.1.";
  private static final String SCHEMAS = "schemas";

  void clean(JsonNode openApi) {
    if (!openApi.path("openapi").asText().startsWith(OPENAPI_31_VERSION_PREFIX)) {
      return;
    }
    visitNode(openApi.path(COMPONENTS).path(SCHEMAS));
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