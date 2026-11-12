package com.axonivy.ivy.tool.openapi.codegen.loader;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

class OpenApi3Loader extends OpenAPIV3Parser {
  private final TypeHintCleaner typeHintCleaner = new TypeHintCleaner();
  private final StringDefaultCleaner stringDefaultCleaner = new StringDefaultCleaner();
  private boolean doClean = true;

  @Override
  public SwaggerParseResult parseJsonNode(String path, JsonNode node, ParseOptions options) {
    stringDefaultCleaner.clean(node);
    if (doClean) {
      typeHintCleaner.visitNode(node);
    }
    return super.parseJsonNode(path, node, options);
  }

  public void doTypeCleanup(boolean cleanup) {
    this.doClean = cleanup;
  }

}
