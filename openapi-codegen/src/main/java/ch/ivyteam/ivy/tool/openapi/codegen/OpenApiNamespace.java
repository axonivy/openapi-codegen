package ch.ivyteam.ivy.tool.openapi.codegen;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.swagger.v3.oas.models.OpenAPI;

public class OpenApiNamespace {

  public static String evaluate(OpenAPI openApi, URI schemaUri) {
    if (openApi != null) {
      String baseUri = new OpenApiSpec(openApi).baseUri(schemaUri);
      if (baseUri != null) {
        return namespaceFromUrl(baseUri);
      }
    }
    return "";
  }

  public static String namespaceFromUrl(String serverUrl) {
    try {
      var url = URI.create(StringUtils.lowerCase(serverUrl)).toURL();
      List<String> hostParts = new ArrayList<>(Arrays.asList(url.getHost().split("\\.")));
      Collections.reverse(hostParts);
      hostParts.addAll(Arrays.asList(url.getPath().split("/")));
      hostParts.add("client");

      StringBuilder ns = new StringBuilder();
      hostParts.stream()
          .flatMap(part -> Arrays.asList(part.split("\\-")).stream())
          .map(part -> part.replaceAll("\\W", "_"))
          .filter(part -> !part.isBlank())
          .forEachOrdered(part -> {
            if (!Pattern.matches("^[0-9]+.*", part) && !ns.toString().isEmpty()) {
              ns.append(".");
            }
            ns.append(part);
          });
      return ns.toString();
    } catch (Exception e) {
      return "";
    }
  }
}
