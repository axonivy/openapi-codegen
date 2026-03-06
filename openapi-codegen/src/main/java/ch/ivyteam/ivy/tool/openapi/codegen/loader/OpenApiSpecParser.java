package ch.ivyteam.ivy.tool.openapi.codegen.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.util.DeserializationUtils;
import io.swagger.v3.parser.util.DeserializationUtils.Options;

public class OpenApiSpecParser {
  public static final String OPENAPI_SPEC_JSON = "openapi.json";
  public static final String OPENAPI_SPEC_YAML = "openapi.yaml";

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

  public OpenAPI parse(URI specUri) {
    for (SwaggerParserExtension extension : getParsers()) {
      var api = Optional.of(extension.readLocation(specUri.toString(),
          Collections.emptyList(), swaggerOpts)).map(SwaggerParseResult::getOpenAPI);
      if (api.isPresent()) {
        return api.get();
      }
    }
    return null;
  }

  public OpenAPI parse(InputStream specStream) throws IOException {
    String rawSpecJson = new String(specStream.readAllBytes(), StandardCharsets.UTF_8);
    return parse(rawSpecJson);
  }

  public OpenAPI parse(String rawSpecJson) {
    for (SwaggerParserExtension extension : getParsers()) {
      var api = Optional.of(extension.readContents(rawSpecJson,
          Collections.emptyList(), swaggerOpts)).map(SwaggerParseResult::getOpenAPI);
      if (api.isPresent()) {
        return api.get();
      }
    }
    return null;
  }

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
