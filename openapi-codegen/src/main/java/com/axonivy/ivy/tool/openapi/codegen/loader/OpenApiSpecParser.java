package com.axonivy.ivy.tool.openapi.codegen.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.util.DeserializationUtils;
import io.swagger.v3.parser.util.DeserializationUtils.Options;

public class OpenApiSpecParser {
  public final OpenApi3Loader v3Parser = new OpenApi3Loader();
  public final ParseOptions swaggerOpts = defaultOpts();

  public OpenApiSpecParser() {
    yamlOptions().setMaxYamlCodePoints(Integer.MAX_VALUE); // allow huge YAML files
  }

  public static Options yamlOptions() {
    return DeserializationUtils.getOptions();
  }

  public void doTypeCleanup(boolean cleanup) {
    this.v3Parser.doTypeCleanup(cleanup);
  }

  public void doDefaultProcessing(boolean processing) {
    this.v3Parser.doDefaultProcessing(processing);
  }

  public OpenAPI parse(URI specUri) {
    for (SwaggerParserExtension extension : getParsers()) {
      var result = extension.readLocation(specUri.toString(), Collections.emptyList(), swaggerOpts);
      OpenAPI api = result.getOpenAPI();
      if (api != null) {
        SchemaTypeNormalizer.normalize(api);
        return api;
      }
    }
    return null;
  }

  public Optional<ParseResult> parse(InputStream specStream) throws IOException {
    String rawSpecJson = new String(specStream.readAllBytes(), StandardCharsets.UTF_8);
    return parse(rawSpecJson);
  }

  public Optional<ParseResult> parse(String rawSpecJson) {
    v3Parser.clearRemovedPropertyDefaults();
    for (SwaggerParserExtension extension : getParsers()) {
      var result = extension.readContents(rawSpecJson, Collections.emptyList(), swaggerOpts);
      OpenAPI api = result.getOpenAPI();
      if (api != null) {
        SchemaTypeNormalizer.normalize(api);
        return Optional.of(new ParseResult(api, rawSpecJson, v3Parser.removedPropertyDefaults()));
      }
    }
    return Optional.empty();
  }

  public record ParseResult(OpenAPI api, String rawSpec, Map<String, Map<String, JsonNode>> propertyDefaults) {}

  private static ParseOptions defaultOpts() {
    ParseOptions options = new ParseOptions();
    options.setResolve(true);
    options.setResolveFully(false);
    options.setFlatten(true);
    options.setFlattenComposedSchemas(false);
    options.setResolveRequestBody(true);
    return options;
  }

  private List<SwaggerParserExtension> getParsers() {
    List<SwaggerParserExtension> parsers = OpenAPIV3Parser.getExtensions();
    parsers.set(0, v3Parser);
    return parsers;
  }

}
