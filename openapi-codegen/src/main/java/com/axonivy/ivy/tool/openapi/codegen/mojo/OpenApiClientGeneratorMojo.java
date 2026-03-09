package com.axonivy.ivy.tool.openapi.codegen.mojo;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.axonivy.ivy.tool.openapi.codegen.OpenApiCodegen;
import com.axonivy.ivy.tool.openapi.codegen.filter.ProgressFilter;
import com.axonivy.ivy.tool.openapi.codegen.loader.OpenApiSpecParser;

import io.swagger.v3.oas.models.OpenAPI;

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
 * -Divy.generate.openapi.client.output=src_generated/petstore
 * -Divy.generate.openapi.client.package=com.swagger.petstore.client
 * </code>
 *
 * @since 1.0.0
 */
@Mojo(name = OpenApiClientGeneratorMojo.GOAL)
public class OpenApiClientGeneratorMojo extends AbstractMojo {
  public static final String GOAL = "generate-openapi-client";

  @Parameter(property = "ivy.generate.openapi.client.skip", defaultValue = "false")
  boolean skipGenerate;

  @Parameter(property = "ivy.generate.openapi.client.spec", required = true)
  URL openApiSpec;

  @Parameter(property = "ivy.generate.openapi.client.package")
  String clientPackage;

  @Parameter(property = "ivy.generate.openapi.client.output", required = true)
  Path outputDir;

  @Override
  public void execute() throws MojoExecutionException {
    if (skipGenerate) {
      return;
    }
    getLog().info("Generating OpenAPI client sources...");

    var openAPI = loadOpenApi();
    var generator = new OpenApiCodegen(openAPI);

    if (clientPackage != null) {
      generator.setPackage(clientPackage);
    }

    ProgressFilter.muteDefaultWriteLog();
    var sources = generator.generateSources(outputDir.toFile());
    getLog().info("Wrote " + sources.size() + " sources");
  }

  private OpenAPI loadOpenApi() throws MojoExecutionException {
    try (var in = openApiSpec.openStream()) {
      return new OpenApiSpecParser().parse(in);
    } catch (IOException ex) {
      throw new MojoExecutionException("Failed to read OpenAPI spec from " + openApiSpec, ex);
    }
  }
}
