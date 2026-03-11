package com.axonivy.ivy.tool.openapi.codegen.filter;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axonivy.ivy.tool.openapi.codegen.filter.FilteringGenerator.Filter;

import io.swagger.v3.oas.models.OpenAPI;

public class ProgressFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProgressFilter.class);

  private final int total;
  private int done;

  public ProgressFilter(OpenAPI openApi) {
    this.total = Optional.ofNullable(openApi.getComponents().getSchemas()).map(Map::size).orElse(0);
    this.done = 0;
  }

  public static void muteDefaultWriteLog() {
    System.setProperty("org.slf4j.simpleLogger.log.io.swagger.codegen.v3.AbstractGenerator", "warn");
  }

  @Override
  public String apply(String filename, String content) {
    done++;
    LOGGER.info("Generate " + done + "/" + total + ": " + filename);
    return content;
  }
}
