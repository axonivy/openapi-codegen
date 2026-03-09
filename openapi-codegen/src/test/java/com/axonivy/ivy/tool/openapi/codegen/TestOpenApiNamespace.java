package com.axonivy.ivy.tool.openapi.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.axonivy.ivy.tool.openapi.codegen.loader.OpenApiSpecParser;

import io.swagger.v3.oas.models.OpenAPI;

public class TestOpenApiNamespace {

  @Test
  void evalutateFromJson() throws IOException {
    URI specSrc = TstRes.petstoreUri();
    var apiSpec = new OpenApiSpecParser().parse(specSrc);
    assertThat(OpenApiNamespace.evaluate(apiSpec, specSrc))
        .isEqualTo("io.swagger.petstore.v2.client");
  }

  @Test
  void evalutateFromJson_variables() throws IOException {
    URI specSrc = URI.create("https://developer.ebay.com/api-docs/master/buy/browse/openapi/3/buy_browse_v1_beta_oas3.json");
    var ebayApi = load(() -> TstRes.load("ebay3-min.json"));
    assertThat(OpenApiNamespace.evaluate(ebayApi, specSrc))
        .isEqualTo("com.ebay.api.buy.browse.v1.client");
  }

  private static OpenAPI load(Supplier<InputStream> stream) throws IOException {
    try (var spec = stream.get()) {
      return new OpenApiSpecParser().parse(spec).get().api();
    }
  }

  @Test
  void evaluateInvalidUrls() {
    assertThat(OpenApiNamespace.namespaceFromUrl(null)).isEqualTo("");
    assertThat(OpenApiNamespace.namespaceFromUrl("")).isEqualTo("");
    assertThat(OpenApiNamespace.namespaceFromUrl("invalid:url")).isEqualTo("");
    assertThat(OpenApiNamespace.namespaceFromUrl("localhost")).isEqualTo("");
  }

  @Test
  void evaluateUrls() {
    assertThat(OpenApiNamespace.namespaceFromUrl("http://localhost")).isEqualTo("localhost.client");
    assertThat(OpenApiNamespace.namespaceFromUrl("http://axonivy.com")).isEqualTo("com.axonivy.client");
    assertThat(OpenApiNamespace.namespaceFromUrl("https://axonivy.com/test")).isEqualTo("com.axonivy.test.client");
    assertThat(OpenApiNamespace.namespaceFromUrl("https://platform.uipath.com/AXONPRESALES/AXONPRESALES"))
        .isEqualTo("com.uipath.platform.axonpresales.axonpresales.client");
    assertThat(OpenApiNamespace.namespaceFromUrl("https://PLATFORM.uipath.com/TEST"))
        .isEqualTo("com.uipath.platform.test.client");
  }

  @Test
  void evaluateSpecialCharUrls() {
    assertThat(OpenApiNamespace.namespaceFromUrl("https://graph.microsoft.com/v1.0"))
        .isEqualTo("com.microsoft.graph.v1_0.client");
    assertThat(OpenApiNamespace.namespaceFromUrl("https://graph.microsoft.com/v1.0/$äbla/_a!"))
        .isEqualTo("com.microsoft.graph.v1_0.__bla._a_.client");
  }

  @Test
  void evaluateBindingSign() {
    assertThat(OpenApiNamespace.namespaceFromUrl("https://microsoft-graph.com/api"))
        .isEqualTo("com.microsoft.graph.api.client");
  }

  @Test
  void evaluateLonesomeNumbers() {
    assertThat(OpenApiNamespace.namespaceFromUrl("http://runtime-v2-lex.us-east-1.amazonaws.com/api/2/goto"))
        .isEqualTo("com.amazonaws.us.east1.runtime.v2.lex.api2.goto.client");

    assertThat(OpenApiNamespace.namespaceFromUrl("http://runtime-v2-lex.us-east-1.amazonaws.com/api/2g/goto"))
        .isEqualTo("com.amazonaws.us.east1.runtime.v2.lex.api2g.goto.client");
  }

}
