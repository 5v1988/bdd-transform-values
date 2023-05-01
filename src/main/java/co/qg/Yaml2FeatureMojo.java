package co.qg;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

@Mojo(name = "substitute-data", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES)
public class Yaml2FeatureMojo extends AbstractMojo {

  public static String FEATURE_FILE_EXT = ".feature";

  @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
  private File target;

  @Parameter(property = "substitute-data.tokenFileName", defaultValue = "token.yaml")
  private String tokenFileName;

  @Parameter(property = "substitute-data.tokenFileFormat", defaultValue = "yaml")
  private String tokenFileFormat;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Begins substituting all tokenized values in feature files!");
    try {
      Stream<Path> tokenFiles = Files.walk(Paths.get(target.getAbsolutePath())).filter(tokenFile ->
          tokenFile.getFileName().toString().equalsIgnoreCase(tokenFileName));
      List<Path> fileList = tokenFiles.collect(Collectors.toList());
      if (fileList.size() != 1) {
        throw new AssertionError(
            String.format("Token file: [ %s ] is not in the project", tokenFileName));
      }
      String tokenFile = fileList.get(0).toFile().getAbsolutePath();
      JSONObject tokenObj = new JSONObject(getTokensAsObject(tokenFile, tokenFileFormat));

      getLog().info(String.format("Picking up token file: %s for the replacing values", tokenFile));
      Stream<Path> features = Files.walk(Paths.get(target.getAbsolutePath())).filter(feature ->
          feature.toFile().getAbsolutePath().trim().endsWith(FEATURE_FILE_EXT));

      features.forEach(feature -> {
        getLog().info(
            String.format("Processing the feature file: %s", feature.toFile().getAbsolutePath()));
        try {
          Charset charset = StandardCharsets.UTF_8;
          String content = null;
          String tokenToReplace, tokenJp;
          content = new String(Files.readAllBytes(feature), charset);
          Matcher m = Pattern.compile("(\\(.*\\))").matcher(content);
          while (m.find()) {
            tokenToReplace = m.group();
            tokenJp = m.group().replaceAll("\\(|\\)", "");
            String tokenReplaced = JsonPath.read(tokenObj.toString(), "$." + tokenJp);
            content = content.replace(tokenToReplace, tokenReplaced);
          }
          Files.write(feature, content.getBytes(charset));
          getLog().info(String.format("Completed processing the feature file: %s",
              feature.toFile().getAbsolutePath()));
        } catch (IOException io) {
          getLog().error(io.getMessage());
        }
      });
    } catch (IOException io) {
      getLog().error(io.getMessage());
    }

  }

  private static Map<String, Object> getTokensAsObject(String fileName, String fileFormat)
      throws IOException {
    Map<String, Object> data;
    try (InputStream inputStream = new FileInputStream(fileName)) {
      if (fileFormat.equalsIgnoreCase("json")) {
        data = new ObjectMapper().readValue(new File(fileName), HashMap.class);
      } else {
        Yaml yaml = new Yaml();
        data = yaml.load(inputStream);
      }
    } catch (IOException e) {
      throw new IOException(String.format(" Exception reading token object : %s", e.getMessage()));
    }
    return data;
  }

}
