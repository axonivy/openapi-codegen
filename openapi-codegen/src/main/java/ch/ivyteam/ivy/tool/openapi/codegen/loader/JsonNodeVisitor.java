package ch.ivyteam.ivy.tool.openapi.codegen.loader;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class JsonNodeVisitor {
  public void visitNode(JsonNode node) {
    if (node == null) {
      return;
    }
    if (node.isContainerNode()) {
      if (node.isArray()) {
        ArrayNode containerArray = (ArrayNode) node;
        visitArrayNode(containerArray);
      } else if (node.isObject()) {
        ObjectNode containerObject = (ObjectNode) node;
        visitObjectNode(containerObject);
      }
    } else {
      visitLeaf(node);
    }
  }

  protected void visitArrayNode(ArrayNode containerArray) {
    for (int i = 0; i < containerArray.size(); i++) {
      if (visitArrayValue(containerArray, i, containerArray.get(i))) {
        visitNode(containerArray.get(i));
      }
    }
  }

  protected void visitObjectNode(ObjectNode containerObject) {
    for (Iterator<String> it = containerObject.fieldNames(); it.hasNext();) {
      String fieldName = it.next();
      if (visitObjectField(containerObject, fieldName, containerObject.get(fieldName), it)) {
        visitNode(containerObject.get(fieldName));
      }
    }
  }

  @SuppressWarnings("unused")
  protected boolean visitArrayValue(ArrayNode parent, int index, JsonNode node) {
    return true;
  }

  @SuppressWarnings("unused")
  protected boolean visitObjectField(ObjectNode parent, String fieldName, JsonNode node,
      Iterator<String> fieldIterator) {
    return true;
  }

  @SuppressWarnings("unused")
  protected void visitLeaf(JsonNode node) {}
}
