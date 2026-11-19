package com.axonivy.ivy.tool.openapi.codegen.loader;

import java.util.Map;

import com.axonivy.ivy.tool.openapi.codegen.defaults.PropertyDefaultCleaner;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

class OpenApi3Loader extends OpenAPIV3Parser {
  private final TypeHintCleaner typeHintCleaner = new TypeHintCleaner();
  private final PropertyDefaultCleaner propertyDefaultCleaner = new PropertyDefaultCleaner();
  private boolean doClean = true;
  private boolean doDefaultProcessing = true;
  private String originalOpenApiVersion;

  @Override
  public SwaggerParseResult parseJsonNode(String path, JsonNode node, ParseOptions options) {
    if (doDefaultProcessing) {
      propertyDefaultCleaner.clean(node);
    }
    if (doClean) {
      typeHintCleaner.visitNode(node);
    }
    String originalVersion = OpenApi31CompatibilityNormalizer.normalize(node);
    if (originalOpenApiVersion == null && originalVersion != null) {
      originalOpenApiVersion = originalVersion;
    }
    SwaggerParseResult result = super.parseJsonNode(path, node, options);
    return result;
  }

  void clearOriginalOpenApiVersion() {
    originalOpenApiVersion = null;
  }

  void restoreOriginalOpenApiVersion(OpenAPI openApi) {
    if (originalOpenApiVersion != null) {
      openApi.setOpenapi(originalOpenApiVersion);
      originalOpenApiVersion = null;
    }
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

  public void doDefaultProcessing(boolean processing) {
    this.doDefaultProcessing = processing;
  }

}
