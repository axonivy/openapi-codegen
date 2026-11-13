package com.axonivy.ivy.tool.openapi.codegen.defaults;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.axonivy.ivy.tool.openapi.codegen.filter.FilteringGenerator.Filter;

import io.swagger.codegen.v3.generators.java.JavaClientCodegen;

/**
 * Interpolates string default values into generated Java model classes.
 */
public class StringDefaultAssigner implements Filter {
  private final Map<String, Map<String, String>> defaultsByModel;
  private final JavaClientCodegen codegen;

  public StringDefaultAssigner(Map<String, Map<String, String>> stringDefaults, JavaClientCodegen codegen) {
    Map<String, Map<String, String>> defaults = new LinkedHashMap<>();
    stringDefaults.forEach((schema, properties) ->
        defaults.put(codegen.toModelName(schema) + ".java", properties));
    this.defaultsByModel = Map.copyOf(defaults);
    this.codegen = codegen;
  }

  @Override
  public String apply(String filename, String content) {
    Map<String, String> defaults = defaultsByModel.get(new File(filename).getName());
    if (defaults == null) {
      return content;
    }
    for (var property : defaults.entrySet()) {
      String fieldName = codegen.toVarName(property.getKey());
      Pattern initializer = Pattern.compile("(\\b(?:String|Object)\\s+" + Pattern.quote(fieldName) + "\\s*=\\s*)null;");
      Matcher matcher = initializer.matcher(content);
      if (matcher.find()) {
        String replacement = matcher.group(1) + javaStringLiteral(property.getValue()) + ";";
        content = matcher.replaceFirst(Matcher.quoteReplacement(replacement));
      }
    }
    return content;
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