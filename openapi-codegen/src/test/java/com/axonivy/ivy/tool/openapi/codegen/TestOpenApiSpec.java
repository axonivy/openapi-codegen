package com.axonivy.ivy.tool.openapi.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.ivy.tool.openapi.codegen.loader.OpenApiSpecParser;

import io.swagger.v3.oas.models.OpenAPI;


public class TestOpenApiSpec {
  private OpenApiSpec petStore;
  private OpenApiSpec petStore3;
  private OpenApiSpec person;
  private OpenApiSpec ebay;

  @BeforeEach
  void load() throws IOException {
    petStore = new OpenApiSpec(load(TstRes::petstore));
    petStore3 = new OpenApiSpec(load(() -> TstRes.load("petstore3-min.json")));
    person = new OpenApiSpec(load(() -> TstRes.load("converted-persons.json")));
    ebay = new OpenApiSpec(load(() -> TstRes.load("ebay3-min.json")));
  }

  @Test
  void modelCount() {
    assertThat(petStore.getModelCount()).isEqualTo(8);
    assertThat(person.getModelCount()).isEqualTo(0);
  }

  @Test
  void specVersion() {
    assertThat(petStore.specVersion()).isEqualTo("2.0");
    assertThat(person.specVersion()).isEqualTo("3.0.0");
  }

  @Test
  void baseUri() {
    assertThat(petStore.baseUri(URI.create("https://petstore.swagger.io/v2/swagger.json")))
        .isEqualTo("https://petstore.swagger.io/v2");

    assertThat(ebay.baseUri(URI.create("https://developer.ebay.com/api-docs/master/buy/browse/openapi/3/buy_browse_v1_beta_oas3.json")))
        .isEqualTo("https://api.ebay.com/buy/browse/v1");

    assertThat(petStore3.baseUri(URI.create("https://petstore3.swagger.io/api/v3/openapi.json")))
        .as("yes, server URIs can be relative according to the spec: https://swagger.io/specification/#server-object")
        .isEqualTo("https://petstore3.swagger.io/api/v3");
  }

  @Test
  void title() {
    assertThat(petStore3.title().get()).isEqualTo("Swagger Petstore - OpenAPI 3.0");
    assertThat(person.title()).isEqualTo(Optional.empty());
  }

  @Test
  void description() {
    assertThat(petStore3.description().get())
        .startsWith("This is a sample Pet Store Server based on the OpenAPI 3.0 specification.");
    assertThat(person.description()).isEqualTo(Optional.empty());
  }

  private static OpenAPI load(Supplier<InputStream> stream) throws IOException {
    try (var spec = stream.get()) {
      return new OpenApiSpecParser().parse(spec);
    }
  }
}
