package com.axonivy.ivy.tool.openapi.codegen;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;

public class OpenApiClientFiles {

  private final Path outputDir;

  public OpenApiClientFiles(Path outputDir) {
    this.outputDir = outputDir;
  }

  public void backupSpec(String rawSpec) {
    var copy = outputDir.resolve("openapi" + extension(rawSpec));
    try {
      Files.createDirectories(outputDir);
      Files.writeString(copy, rawSpec, StandardOpenOption.CREATE);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to backup OpenAPI spec to " + copy, ex);
    }
  }

  private static String extension(String rawSpec) {
    if (!rawSpec.trim().startsWith("{")) {
      return ".yaml";
    }
    return ".json";
  }

  public void cleanup(Consumer<String> log) {
    if (outputDir == null) {
      return;
    }
    if (!Files.exists(outputDir)) {
      return;
    }

    log.accept("Cleaning existing files in " + outputDir);
    try (var stream = Files.walk(outputDir)) {
      stream.sorted(Comparator.reverseOrder())
          .filter(p -> !Objects.equals(p, outputDir))
          .map(Path::toFile)
          .forEach(File::delete);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
