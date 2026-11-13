package com.axonivy.ivy.tool.openapi.codegen.defaults;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.axonivy.ivy.tool.openapi.codegen.filter.FilteringGenerator.Filter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.codegen.v3.generators.java.JavaClientCodegen;

/**
 * Restores model property defaults as Java field initializers.
 */
public class PropertyDefaultAssigner implements Filter {
  private final Map<String, Map<String, JsonNode>> defaultsByModel;
  private final JavaClientCodegen codegen;

  public PropertyDefaultAssigner(Map<String, Map<String, JsonNode>> propertyDefaults, JavaClientCodegen codegen) {
    Map<String, Map<String, JsonNode>> defaults = new LinkedHashMap<>();
    propertyDefaults.forEach((schema, properties) ->
        defaults.put(codegen.toModelName(schema) + ".java", properties));
    this.defaultsByModel = Map.copyOf(defaults);
    this.codegen = codegen;
  }

  @Override
  public String apply(String filename, String content) {
    Map<String, JsonNode> defaults = defaultsByModel.get(new File(filename).getName());
    if (defaults == null) {
      return content;
    }
    for (var property : defaults.entrySet()) {
      String fieldName = codegen.toVarName(property.getKey());
      Pattern initializer = Pattern.compile("(\\b(?:private|protected|public)\\s+([\\w.$<>?,\\[\\] ]+?)\\s+"
          + Pattern.quote(fieldName) + "\\s*=\\s*)null;");
      Matcher matcher = initializer.matcher(content);
      if (!matcher.find()) {
        continue;
      }
      String javaValue = javaLiteral(property.getValue(), matcher.group(2).trim(), content);
      if (javaValue == null) {
        continue;
      }
      String replacement = matcher.group(1) + javaValue + ";";
      content = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
    }
    return content;
  }

  private static String javaLiteral(JsonNode value, String fieldType, String generatedSource) {
    String rawType = rawType(fieldType);
    if (value.isNull()) {
      return isPrimitive(rawType) ? null : "null";
    }
    if (value.isTextual()) {
      String stringValue = javaStringLiteral(value.asText());
      if (isType(rawType, "String", "Object")) {
        return stringValue;
      }
      if (hasEnumFromValue(rawType, generatedSource)) {
        return rawType + ".fromValue(" + stringValue + ")";
      }
      return switch (simpleType(rawType)) {
        case "LocalDate", "LocalDateTime", "OffsetDateTime", "ZonedDateTime", "LocalTime", "OffsetTime",
            "Instant", "Duration", "Year", "YearMonth" -> rawType + ".parse(" + stringValue + ")";
        case "UUID" -> rawType + ".fromString(" + stringValue + ")";
        case "URI" -> rawType + ".create(" + stringValue + ")";
        default -> null;
      };
    }
    if (value.isBoolean()) {
      return isType(rawType, "boolean", "Boolean", "Object") ? value.asText() : null;
    }
    if (value.isNumber()) {
      return javaNumber(value, rawType);
    }
    if (value.isArray()) {
      return javaArray(value, fieldType, rawType, generatedSource);
    }
    if (value.isObject()) {
      return javaObject(value, fieldType, rawType, generatedSource);
    }
    return null;
  }

  private static boolean hasEnumFromValue(String rawType, String generatedSource) {
    String enumName = simpleType(rawType);
    String declaration = "\\benum\\s+" + Pattern.quote(enumName) + "\\s*\\{";
    String converter = "\\bstatic\\s+" + Pattern.quote(enumName) + "\\s+fromValue\\s*\\(\\s*String\\b";
    return Pattern.compile(declaration).matcher(generatedSource).find()
        && Pattern.compile(converter).matcher(generatedSource).find();
  }

  private static String javaNumber(JsonNode value, String rawType) {
    String number = value.asText();
    String type = simpleType(rawType);
    return switch (type) {
      case "Object", "Number" -> boxedNumber(value);
      case "Byte" -> "Byte.valueOf(" + javaStringLiteral(number) + ")";
      case "byte" -> "Byte.parseByte(" + javaStringLiteral(number) + ")";
      case "Short" -> "Short.valueOf(" + javaStringLiteral(number) + ")";
      case "short" -> "Short.parseShort(" + javaStringLiteral(number) + ")";
      case "Integer" -> "Integer.valueOf(" + javaStringLiteral(number) + ")";
      case "int" -> "Integer.parseInt(" + javaStringLiteral(number) + ")";
      case "Long" -> "Long.valueOf(" + javaStringLiteral(number) + ")";
      case "long" -> "Long.parseLong(" + javaStringLiteral(number) + ")";
      case "Float" -> "Float.valueOf(" + javaStringLiteral(number) + ")";
      case "float" -> "Float.parseFloat(" + javaStringLiteral(number) + ")";
      case "Double" -> "Double.valueOf(" + javaStringLiteral(number) + ")";
      case "double" -> "Double.parseDouble(" + javaStringLiteral(number) + ")";
      case "BigInteger" -> "new java.math.BigInteger(" + javaStringLiteral(number) + ")";
      case "BigDecimal" -> "new java.math.BigDecimal(" + javaStringLiteral(number) + ")";
      default -> null;
    };
  }

  private static String boxedNumber(JsonNode value) {
    String number = javaStringLiteral(value.asText());
    if (value.isIntegralNumber()) {
      BigInteger integer = value.bigIntegerValue();
      if (integer.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0
          && integer.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0) {
        return "Integer.valueOf(" + number + ")";
      }
      if (integer.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0
          && integer.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0) {
        return "Long.valueOf(" + number + ")";
      }
      return "new java.math.BigInteger(" + number + ")";
    }
    if (value.isBigDecimal()) {
      return "new java.math.BigDecimal(" + number + ")";
    }
    return "Double.valueOf(" + number + ")";
  }

  private static String javaArray(JsonNode value, String fieldType, String rawType, String generatedSource) {
    if (rawType.endsWith("[]")) {
      String elementType = rawType.substring(0, rawType.length() - 2);
      String values = joinValues(value, elementType, generatedSource);
      return values == null ? null : "new " + elementType + "[] {" + values + "}";
    }
    if (!isType(rawType, "Object", "List", "Collection", "Iterable", "Set")) {
      return null;
    }
    String elementType = typeArguments(fieldType).stream().findFirst().orElse("Object");
    String values = joinValues(value, elementType, generatedSource);
    if (values == null) {
      return null;
    }
    if (value.isEmpty()) {
      return isType(rawType, "Set") ? "java.util.Collections.emptySet()" : "java.util.Collections.emptyList()";
    }
    if (isType(rawType, "Set")) {
      return "new java.util.LinkedHashSet<>(java.util.Arrays.asList(" + values + "))";
    }
    return "java.util.Arrays.asList(" + values + ")";
  }

  private static String joinValues(JsonNode array, String elementType, String generatedSource) {
    List<String> values = new ArrayList<>();
    for (JsonNode item : array) {
      String literal = javaLiteral(item, elementType, generatedSource);
      if (literal == null) {
        return null;
      }
      values.add(literal);
    }
    return values.stream().collect(Collectors.joining(", "));
  }

  private static String javaObject(JsonNode value, String fieldType, String rawType, String generatedSource) {
    if (!isType(rawType, "Object", "Map", "HashMap", "LinkedHashMap")) {
      return null;
    }
    List<String> arguments = typeArguments(fieldType);
    String typeArguments = arguments.isEmpty() ? "<String, Object>" : "<" + String.join(", ", arguments) + ">";
    String valueType = arguments.size() > 1 ? arguments.get(1) : "Object";
    StringBuilder initializer = new StringBuilder("new java.util.LinkedHashMap")
        .append(typeArguments).append("() {{ ");
    var entries = ((ObjectNode) value).propertyStream().iterator();
    while (entries.hasNext()) {
      var entry = entries.next();
      String literal = javaLiteral(entry.getValue(), valueType, generatedSource);
      if (literal == null) {
        return null;
      }
      initializer.append("put(")
          .append(javaStringLiteral(entry.getKey()))
          .append(", ").append(literal).append("); ");
    }
    if (value.isEmpty()) {
      return new StringBuilder("new java.util.LinkedHashMap").append(typeArguments).append("()").toString();
    }
    return initializer.append("}}").toString();
  }

  private static List<String> typeArguments(String type) {
    int start = type.indexOf('<');
    int end = type.lastIndexOf('>');
    if (start < 0 || end <= start) {
      return List.of();
    }
    List<String> arguments = new ArrayList<>();
    int depth = 0;
    int argumentStart = start + 1;
    for (int i = start + 1; i < end; i++) {
      char character = type.charAt(i);
      if (character == '<') {
        depth++;
      } else if (character == '>') {
        depth--;
      } else if (character == ',' && depth == 0) {
        arguments.add(type.substring(argumentStart, i).trim());
        argumentStart = i + 1;
      }
    }
    arguments.add(type.substring(argumentStart, end).trim());
    return arguments;
  }

  private static String rawType(String type) {
    int genericStart = type.indexOf('<');
    return (genericStart < 0 ? type : type.substring(0, genericStart)).trim();
  }

  private static String simpleType(String type) {
    int packageSeparator = type.lastIndexOf('.');
    return packageSeparator < 0 ? type : type.substring(packageSeparator + 1);
  }

  private static boolean isType(String type, String... names) {
    String simple = simpleType(type);
    for (String name : names) {
      if (name.equals(simple)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isPrimitive(String type) {
    return isType(type, "boolean", "byte", "short", "int", "long", "float", "double", "char");
  }

  private static String javaStringLiteral(String value) {
    StringBuilder literal = new StringBuilder("\"");
    for (char character : value.toCharArray()) {
      switch (character) {
        case '\\' -> literal.append("\\\\");
        case '"' -> literal.append("\\\"");
        case '\b' -> literal.append("\\b");
        case '\t' -> literal.append("\\t");
        case '\n' -> literal.append("\\n");
        case '\f' -> literal.append("\\f");
        case '\r' -> literal.append("\\r");
        default -> {
          if (character < 0x20 || character == 0x7f) {
            literal.append('\\')
                .append((char) ('0' + ((character >> 6) & 7)))
                .append((char) ('0' + ((character >> 3) & 7)))
                .append((char) ('0' + (character & 7)));
          } else {
            literal.append(character);
          }
        }
      }
    }
    return literal.append('"').toString();
  }
}