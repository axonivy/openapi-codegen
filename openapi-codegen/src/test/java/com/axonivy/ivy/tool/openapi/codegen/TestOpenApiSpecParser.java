package com.axonivy.ivy.tool.openapi.codegen;

import java.io.IOException;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.axonivy.ivy.tool.openapi.codegen.loader.OpenApiSpecParser;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

class TestOpenApiSpecParser {

  @Test
  void resolveDefault() throws IOException {
    var openApi = new OpenApiSpecParser().parse(TstRes.petstoreUri());
    Assertions.assertThat(modelsOf(openApi))
        .containsOnly(
            "ApiResponse",
            "Category",
            "Pet",
            "Tag",
            "Order",
            "User",
            "petId_uploadImage_body",
            "pet_petId_body");
  }

  @Test
  void resolveFully() throws IOException {
    var parser = new OpenApiSpecParser();
    parser.swaggerOpts.setResolveFully(true);
    var openApi = parser.parse(TstRes.petstoreUri());
    Assertions.assertThat(modelsOf(openApi))
        .containsOnly(
            "ApiResponse",
            "Category",
            "Pet",
            "Tag",
            "Order",
            "User",
            "petId_uploadImage_body",
            "inline_response_200",
            "pet_category",
            "pet_tags",
            "pet_body",
            "pet_body_1",
            "pet_body_2",
            "pet_body_3",
            "inline_response_200_1",
            "pet_petId_body",
            "store_order_body",
            "user_createWithArray_body",
            "user_username_body",
            "user_body");
  }

  @Test
  void openApi30StringDefaultsRemainUntouched() {
    var spec = """
        openapi: 3.0.3
        info:
          title: Default preservation
          version: 1.0.0
        paths: {}
        components:
          schemas:
            Example:
              type: object
              properties:
                name:
                  type: string
                  default: Standard
        """;
    var openApi = new OpenApiSpecParser().parse(spec).orElseThrow().api();
    var name = (Schema<?>) openApi.getComponents().getSchemas().get("Example").getProperties().get("name");

    Assertions.assertThat(name.getDefault()).isEqualTo("Standard");
  }

  private Set<String> modelsOf(OpenAPI openApi) {
    return openApi.getComponents().getSchemas().keySet();
  }

}
