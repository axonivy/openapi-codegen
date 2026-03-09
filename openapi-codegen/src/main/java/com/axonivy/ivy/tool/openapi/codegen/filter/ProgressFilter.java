package com.axonivy.ivy.tool.openapi.codegen.filter;

import com.axonivy.ivy.tool.openapi.codegen.OpenApiSpec;
import com.axonivy.ivy.tool.openapi.codegen.filter.FilteringGenerator.Filter;

import io.swagger.v3.oas.models.OpenAPI;

public class ProgressFilter implements Filter {

  private final int total;
  private int done;

  public ProgressFilter(OpenAPI openApi) {
    this.total = new OpenApiSpec(openApi).getModelCount();
    this.done = 0;
  }

  @Override
  public String apply(String filename, String content) {
    done++;
    IO.println("Generate " + done + "/" + total + ": " + filename);
    return content;
  }
}
