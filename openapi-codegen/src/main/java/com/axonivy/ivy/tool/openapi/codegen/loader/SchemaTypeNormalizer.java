package com.axonivy.ivy.tool.openapi.codegen.loader;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

final class SchemaTypeNormalizer {
  private final Set<Schema<?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());

  static void normalize(OpenAPI openApi) {
    if (openApi.getOpenapi() == null || !openApi.getOpenapi().startsWith("3.1.")) {
      return;
    }
    if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
      return;
    }
    new SchemaTypeNormalizer().normalizeSchemas(openApi.getComponents().getSchemas().values());
  }

  private void normalizeSchemas(Iterable<?> schemas) {
    if (schemas == null) {
      return;
    }
    for (Object schema : schemas) {
      if (schema instanceof Schema<?> schemaObject) {
        normalizeSchema(schemaObject);
      }
    }
  }

  private void normalizeSchema(Schema<?> schema) {
    if (schema == null || !visited.add(schema)) {
      return;
    }

    Set<String> types = schema.getTypes();
    if (schema.getType() == null && types != null) {
      String normalizedType = null;
      for (String type : types) {
        if ("null".equals(type)) {
          continue;
        }
        if (normalizedType != null) {
          normalizedType = null;
          break;
        }
        normalizedType = type;
      }
      if ("string".equals(normalizedType)) {
        schema.setType(normalizedType);
        if (types.contains("null")) {
          schema.setNullable(true);
        }
      }
    }

    normalizeSchemaMap(schema.getProperties());
    normalizeSchema(schema.getItems());
    if (schema.getAdditionalProperties() instanceof Schema<?> additionalProperties) {
      normalizeSchema(additionalProperties);
    }
    normalizeSchemas(schema.getAllOf());
    normalizeSchemas(schema.getAnyOf());
    normalizeSchemas(schema.getOneOf());
    normalizeSchema(schema.getNot());
    normalizeSchema(schema.getIf());
    normalizeSchema(schema.getThen());
    normalizeSchema(schema.getElse());
    normalizeSchemas(schema.getPrefixItems());
    normalizeSchemaMap(schema.getPatternProperties());
    normalizeSchema(schema.getContains());
    normalizeSchema(schema.getContentSchema());
    normalizeSchema(schema.getPropertyNames());
    normalizeSchema(schema.getUnevaluatedProperties());
    normalizeSchema(schema.getAdditionalItems());
    normalizeSchema(schema.getUnevaluatedItems());
    normalizeSchemaMap(schema.getDependentSchemas());
  }

  private void normalizeSchemaMap(Map<?, ?> schemas) {
    if (schemas != null) {
      normalizeSchemas(schemas.values());
    }
  }
}