package com.axonivy.ivy.tool.openapi.codegen.mojo;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.axonivy.ivy.tool.openapi.codegen.OpenApiClientFiles;
import com.axonivy.ivy.tool.openapi.codegen.OpenApiCodegen;
import com.axonivy.ivy.tool.openapi.codegen.filter.ProgressFilter;
import com.axonivy.ivy.tool.openapi.codegen.loader.OpenApiSpecParser;
import com.axonivy.ivy.tool.openapi.codegen.loader.OpenApiSpecParser.ParseResult;

/**
 * <p>
 * Generates an OpenAPI client
 * </p>
 * <p>
 * Command line invocation is supported.
 * </p>
 * <code>
 * mvn com.axonivy.ivy.tool.rest:openapi-codegen:generate-openapi-client
 * -Divy.generate.openapi.client.spec=https://petstore3.swagger.io/api/v3/openapi.json
 * -Divy.generate.openapi.client.output=src_generated/rest/petstore
 * -Divy.generate.openapi.client.package=com.swagger.petstore.client
 * </code>
 *
 * @since 1.0.0
 */
@Mojo(name = OpenApiClientGeneratorMojo.GOAL, requiresProject = false)
public class OpenApiClientGeneratorMojo extends AbstractMojo {
  public static final String GOAL = "generate-openapi-client";

  @Parameter(property = "ivy.generate.openapi.client.skip", defaultValue = "false")
  boolean skipGenerate;

  @Parameter(property = "ivy.generate.openapi.client.spec", required = true)
  URL openApiSpec;

  @Parameter(property = "ivy.generate.openapi.client.output", required = true)
  Path outputDir;

  @Parameter(property = "ivy.generate.openapi.client.namespace")
  String namespace;

  /**
   * Generate types for generic 'allOf', 'anyOf' references.
   * This can help to build a valid client, if generated sources can't be compiled using the default options
   */
  @Parameter(property = "ivy.generate.openapi.client.resolveFully")
  Boolean resolveFully;

  @Override
  public void execute() throws MojoExecutionException {
    if (skipGenerate) {
      return;
    }
    getLog().info("Generating OpenAPI client sources...");
    var parsed = parseOpenAPI();

    var client = new OpenApiClientFiles(outputDir);
    client.cleanup(getLog()::info);
    client.backupSpec(parsed.rawSpec());

    var generator = new OpenApiCodegen(parsed.api());
    if (namespace != null) {
      generator.setPackage(namespace);
    }

    ProgressFilter.muteDefaultWriteLog();
    var sources = generator.generateSources(outputDir.toFile());
    getLog().info("Wrote " + sources.size() + " sources");
  }

  private ParseResult parseOpenAPI() throws MojoExecutionException {
    try (var in = openApiSpec.openStream()) {
      var parser = new OpenApiSpecParser();
      if (resolveFully != null) {
        parser.swaggerOpts.setResolveFully(resolveFully);
      }
      return parser.parse(in)
          .orElseThrow(() -> new MojoExecutionException("Failed to parse OpenAPI from " + openApiSpec));
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to read OpenAPI spec from " + openApiSpec, ex);
    }
  }

}
