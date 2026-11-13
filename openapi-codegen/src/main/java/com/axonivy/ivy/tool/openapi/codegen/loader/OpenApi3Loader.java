package com.axonivy.ivy.tool.openapi.codegen.loader;

import java.util.Map;

import com.axonivy.ivy.tool.openapi.codegen.defaults.PropertyDefaultCleaner;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

class OpenApi3Loader extends OpenAPIV3Parser {
  private final TypeHintCleaner typeHintCleaner = new TypeHintCleaner();
  private final PropertyDefaultCleaner propertyDefaultCleaner = new PropertyDefaultCleaner();
  private boolean doClean = true;

  @Override
  public SwaggerParseResult parseJsonNode(String path, JsonNode node, ParseOptions options) {
    propertyDefaultCleaner.clean(node);
    if (doClean) {
      typeHintCleaner.visitNode(node);
    }
    return super.parseJsonNode(path, node, options);
  }

  Map<String, Map<String, JsonNode>> removedPropertyDefaults() {
    return propertyDefaultCleaner.removedDefaults();
  }

  void clearRemovedPropertyDefaults() {
    propertyDefaultCleaner.clear();
  }

  public void doTypeCleanup(boolean cleanup) {
    this.doClean = cleanup;
  }

}
