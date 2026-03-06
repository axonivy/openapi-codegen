package ch.ivyteam.ivy.tool.openapi.codegen.loader;

import java.util.Iterator;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.v3.parser.util.OpenAPIDeserializer;

class TypeHintCleaner extends JsonNodeVisitor {
  private static final String PROPS = "additionalProperties";

  private static final String OBJECT_VALUE = "object";
  private static final String TYPE_FIELD = "type";

  /**
   * removes AdditionalProperties that enforce objects to be of 'Map' nature rather than 'Object'.
   * @see OpenAPIDeserializer#getSchema
   */
  @Override
  protected boolean visitObjectField(ObjectNode parent, String fieldName, JsonNode node, Iterator<String> fieldIterator) {
    if (PROPS.equals(fieldName)) {
      if (hasObjectTypeHint(node) && hasObjectTypeHint(parent)) {
        fieldIterator.remove();
        return false;
      }
    }
    return super.visitObjectField(parent, fieldName, node, fieldIterator);
  }

  private static boolean hasObjectTypeHint(JsonNode node) {
    JsonNode typeField = node.get(TYPE_FIELD);
    if (typeField == null) {
      return false;
    }
    return Objects.equals(typeField.asText(), OBJECT_VALUE);
  }
}
