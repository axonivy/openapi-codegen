package com.axonivy.ivy.tool.openapi.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class OpenApiSpecBackup {

  public static void write(Path outputDir, String rawSpec) throws IOException {
    var copy = outputDir.resolve("openapi" + extension(rawSpec));
    Files.createDirectories(outputDir);
    Files.writeString(copy, rawSpec, StandardOpenOption.CREATE);
  }

  private static String extension(String rawSpec) {
    if (!rawSpec.trim().startsWith("{")) {
      return ".yaml";
    }
    return ".json";
  }
}
