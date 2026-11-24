package com.axonivy.ivy.tool.openapi.codegen.loader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class AllOfResolver extends JsonNodeVisitor {

  public static void resolve(JsonNode nodes) {
    var resolver = new AllOfResolver();
    resolver.visitNode(nodes);
    resolver.replacements.forEach(AllOfReplacement::apply);
  }

  private final List<AllOfReplacement> replacements = new ArrayList<>();

  @Override
  protected boolean visitObjectField(ObjectNode parent, String fieldName, JsonNode node,
      Iterator<String> fields) {
    if ("allOf".equals(fieldName) && node instanceof ArrayNode allOf) {
      if (allOf.size() == 1 && allOf.get(0) instanceof ObjectNode type) {
        if (type.properties().size() == 1 && type.get("$ref") instanceof TextNode ref) {
          replacements.add(new AllOfReplacement(parent, fieldName, ref));
        }
      }
    }
    return super.visitObjectField(parent, fieldName, node, fields);
  }

  private static record AllOfReplacement(ObjectNode parent, String fieldName, JsonNode ref) {
    void apply() {
      parent.set("$ref", ref);
      parent.remove("allOf");
    }
  }

}
