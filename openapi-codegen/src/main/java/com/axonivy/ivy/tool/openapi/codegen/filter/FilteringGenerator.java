package com.axonivy.ivy.tool.openapi.codegen.filter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.swagger.codegen.v3.DefaultGenerator;

public class FilteringGenerator extends DefaultGenerator {
  private final List<FilteringGenerator.Filter> filters = new ArrayList<>();

  public FilteringGenerator filter(FilteringGenerator.Filter filterImpl) {
    filters.add(filterImpl);
    return this;
  }

  @Override
  public File writeToFile(String filename, String contents) throws IOException {
    for (FilteringGenerator.Filter filter : filters) {
      contents = filter.apply(filename, contents);
    }
    return super.writeToFile(filename, contents);
  }

  public interface Filter {
    String apply(String filename, String content);
  }
}
