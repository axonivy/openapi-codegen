package ch.ivyteam.ivy.tool.openapi.codegen;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class TstRes {
  public static InputStream load(String res) {
    return TstRes.class.getResourceAsStream("res/" + res);
  }

  public static InputStream petstore() {
    return load("petstore.json");
  }

  public static URI petstoreUri() throws IOException {
    return toLocal(petstore(), "petstore.json");
  }

  public static URI resolve(String res) throws IOException {
    return toLocal(load(res), res);
  }

  private static URI toLocal(InputStream is, String name) throws IOException {
    Path tstRes = Files.createTempDirectory("openApiTstRes");
    Path spec = tstRes.resolve(name);
    try (is) {
      Files.copy(is, spec, StandardCopyOption.REPLACE_EXISTING);
    }
    return spec.toUri();
  }

}
