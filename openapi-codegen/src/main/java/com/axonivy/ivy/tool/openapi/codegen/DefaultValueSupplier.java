package com.axonivy.ivy.tool.openapi.codegen;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import org.joda.time.LocalDate;

import io.swagger.v3.oas.models.media.Schema;

class DefaultValueSupplier {
  public static String toDefaultValue(Schema<?> schema) {
    if ("string".equals(schema.getType())) {
      if ("uuid".equals(schema.getFormat()) && schema.getDefault() instanceof UUID) {
        return "UUID.fromString(\"" + schema.getDefault() + "\")";
      }
      if ("date".equals(schema.getFormat())) {
        if (schema.getDefault() instanceof Date date) {
          return "LocalDate.parse(\"" + LocalDate.fromDateFields(date).toString() + "\")";
        }
      }
      if ("date-time".equals(schema.getFormat())) {
        if (schema.getDefault() instanceof OffsetDateTime offset) {
          return "OffsetDateTime.parse(\"" + offset.toString() + "\")";
        }
      }
      return null;
    }
    if ("object".equals(schema.getType()) && schema.getDefault() != null) {
      return "null";
    }
    return null;
  }
}