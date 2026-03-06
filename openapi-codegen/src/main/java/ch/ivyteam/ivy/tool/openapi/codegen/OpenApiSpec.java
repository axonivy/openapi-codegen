package ch.ivyteam.ivy.tool.openapi.codegen;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;

/**
 * Stable and tested interface for swaggers OpenAPI object model.
 * @since 9.2
 */
public class OpenApiSpec {
  private final OpenAPI openApi;

  public OpenApiSpec(OpenAPI openApi) {
    this.openApi = openApi;
  }

  public int getModelCount() {
    Map<String, ?> schemas = openApi.getComponents().getSchemas();
    if (schemas == null) {
      return 0;
    }
    return schemas.size();
  }

  public int getPathCount() {
    Paths paths = openApi.getPaths();
    if (paths == null) {
      return 0;
    }
    return paths.size();
  }

  public String specVersion() {
    Optional<String> swaggerVersion = findExtension("x-original-swagger-version");
    return swaggerVersion.orElse(openApi.getOpenapi());
  }

  private Optional<String> findExtension(String key) {
    Map<String, Object> extensions = openApi.getExtensions();
    if (extensions != null) {
      Object value = extensions.get(key);
      if (value instanceof String) {
        return Optional.of((String) value);
      }
    }
    return Optional.empty();
  }

  public Optional<String> title() {
    return info().map(Info::getTitle).filter(StringUtils::isNotBlank);
  }

  public Optional<String> description() {
    return info().map(Info::getDescription).filter(StringUtils::isNotBlank);
  }

  private Optional<Info> info() {
    return Optional.ofNullable(openApi.getInfo());
  }

  public String baseUri(URI schemaUri) {
    List<Server> servers = openApi.getServers();
    if (servers.isEmpty()) {
      return null;
    }
    Server first = servers.iterator().next();
    String url = interpolate(first.getUrl(), first.getVariables());
    return makeAbsolute(schemaUri, url);
  }

  private static String interpolate(String url, ServerVariables vars) {
    if (vars != null) {
      for (java.util.Map.Entry<String, ServerVariable> entry : vars.entrySet()) {
        url = Strings.CS.replace(url, "{" + entry.getKey() + "}", entry.getValue().getDefault());
      }
    }
    return url;
  }

  @SuppressWarnings("deprecation")
  private static String makeAbsolute(URI schemaUri, String url) {
    boolean isRelative = url.startsWith("/");
    if (isRelative && schemaUri != null) {
      try {
        return new URL(schemaUri.toURL(), url).toString();
      } catch (MalformedURLException ex) {
        return url;
      }
    }
    return url;
  }
}
