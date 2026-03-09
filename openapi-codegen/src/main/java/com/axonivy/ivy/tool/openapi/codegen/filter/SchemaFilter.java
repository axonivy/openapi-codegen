package com.axonivy.ivy.tool.openapi.codegen.filter;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.axonivy.ivy.tool.openapi.codegen.filter.FilteringGenerator.Filter;


/**
 * remove non functional Schema descriptor annotations:
 * No need to pollute our pojos and classpath with this library yet.
 */
public class SchemaFilter implements Filter {
  private static final String SCHEMA_IMPORT = "import io.swagger.v3.oas.annotations.media.Schema;";
  private static final Pattern SCHEMA_USAGE = Pattern.compile("\\s*@Schema\\(.*");

  @Override
  public String apply(String filename, String content) {
    if (!filename.endsWith(".java")) {
      return content;
    }
    String pureJacksonPojo = Strings.CS.remove(content, SCHEMA_IMPORT);
    return SCHEMA_USAGE.matcher(pureJacksonPojo).replaceAll(StringUtils.EMPTY);
  }
}
