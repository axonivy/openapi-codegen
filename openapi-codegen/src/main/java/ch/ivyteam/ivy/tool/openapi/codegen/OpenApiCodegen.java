package ch.ivyteam.ivy.tool.openapi.codegen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.ClientOpts;
import io.swagger.codegen.v3.CodegenArgument;
import io.swagger.codegen.v3.CodegenConfig;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.v3.oas.models.OpenAPI;

import ch.ivyteam.ivy.tool.openapi.codegen.filter.FilteringGenerator;
import ch.ivyteam.ivy.tool.openapi.codegen.filter.ProgressFilter;
import ch.ivyteam.ivy.tool.openapi.codegen.filter.SchemaFilter;

/**
 * @author rew
 * @since 9.2.0
 */
public class OpenApiCodegen {
  private final OpenAPI openApi;
  private String modelPkg;

  public OpenApiCodegen(OpenAPI openApi) {
    this.openApi = openApi;
    this.modelPkg = "com.axonivy.client";
  }

  public void setPackage(String modelPkg) {
    if (StringUtils.isNotBlank(modelPkg)) {
      this.modelPkg = modelPkg;
    }
  }

  public String getModelPkg() {
    return modelPkg;
  }

  public List<File> generateSources(File outDir) {
    final ClientOptInput input = newJerseyClientOpt().openAPI(openApi);
    input.getConfig().additionalProperties().put(CodegenConstants.MODEL_PACKAGE, modelPkg);
    input.getConfig().setOutputDir(outDir.getAbsolutePath());
    return modelsOnly(() -> new FilteringGenerator()
        .filter(new SchemaFilter())
        .filter(new ProgressFilter(openApi))
        .opts(input).generate());
  }

  private static ClientOptInput newJerseyClientOpt() {
    CodegenConfig jerseyCodegen = newJerseyCodegen();
    jerseyCodegen.setLanguageArguments(new ArrayList<>(DEFAULT.GENERATOR_OPTS));
    jerseyCodegen.additionalProperties().putAll(DEFAULT.CODEGEN_CONF);
    return new ClientOptInput().config(jerseyCodegen).opts(new ClientOpts());
  }

  private static CodegenConfig newJerseyCodegen() {
    CodegenConfig codegen = new io.swagger.codegen.v3.generators.java.JavaClientCodegen();
    codegen.setLibrary(DEFAULT.REST_LIB);
    return codegen;
  }

  private static List<File> modelsOnly(Supplier<List<File>> run) {
    try {
      System.setProperty(CodegenConstants.GENERATE_MODELS, "true");
      System.setProperty(CodegenConstants.SUPPORTING_FILES, "false");

      return run.get();
    } finally {
      System.clearProperty(CodegenConstants.SUPPORTING_FILES);
      System.clearProperty(CodegenConstants.GENERATE_MODELS);
    }
  }

  private static CodegenArgument boolArg(String option, Boolean value) {
    return new CodegenArgument().option(option).type("boolean").value(value.toString());
  }

  public interface DEFAULT {

    String DATE_LIBRARY = "java8";
    String REST_LIB = "jersey2";

    List<CodegenArgument> GENERATOR_OPTS = List.of(
        boolArg(CodegenConstants.MODEL_TESTS_OPTION, Boolean.FALSE),
        boolArg(CodegenConstants.MODEL_DOCS_OPTION, Boolean.FALSE),
        boolArg(CodegenConstants.API_TESTS_OPTION, Boolean.FALSE),
        boolArg(CodegenConstants.API_DOCS_OPTION, Boolean.FALSE));

    Map<String, String> CODEGEN_CONF = Map.of(
        "sourceFolder", "src_generated",
        "dateLibrary", DATE_LIBRARY);
  }

}
