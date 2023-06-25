package co.qg;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

/**
 * <h1>It transforms tokens from yaml file</h1>
 * @author Veera
 * @version 1.0.0
 *
 */

@Mojo(name = "transform-values", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES)
public class TransformationValuesMojo extends AbstractMojo {

  public static final String TOKEN_PATTERN = "\\[.*path:\\s*([^,\\s]+).*\\]";
  public static final String DATE_PATTERN = "\\[\\s*type:\\s*date\\s*,\\s*format:\\s*(.+)\\s*,\\s*delta:\\s*(-?\\d+)\\s*\\]";
  public static final String FEATURE_FILE_EXT = ".feature";

  @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
  private File target;

  @Parameter(property = "transform-values.tokenFileName", defaultValue = "token.yaml")
  private String tokenFileName;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Begins transforming all tokenized values in feature files!");
    try {
      Stream<Path> tokenFiles = Files.walk(Paths.get(target.getAbsolutePath())).filter(tokenFile ->
          tokenFile.getFileName().toString().equalsIgnoreCase(tokenFileName));
      List<Path> fileList = tokenFiles.collect(Collectors.toList());
      if (fileList.size() != 1) {
        throw new AssertionError(
            String.format("Token file: [ %s ] is not in the project", tokenFileName));
      }
      String ymlName = fileList.get(0).toFile().getAbsolutePath();
      JSONObject yamlObj = new JSONObject(getYamlObject(ymlName));

      getLog().info(String.format("Picking up token file: %s for the replacing values", ymlName));
      Stream<Path> features = Files.walk(Paths.get(target.getAbsolutePath())).filter(feature ->
          feature.toFile().getAbsolutePath().trim().endsWith(FEATURE_FILE_EXT));

      features.forEach(feature -> {
        getLog().info(
            String.format("Processing the feature file: %s", feature.toFile().getAbsolutePath()));
        try {
          Charset charset = StandardCharsets.UTF_8;
          String content = null;
          String tokenToReplace;
          content = new String(Files.readAllBytes(feature), charset);
          Matcher dateTokens = Pattern.compile(DATE_PATTERN).matcher(content);
          // Begins transforming all date values
          while (dateTokens.find()) {
            tokenToReplace = dateTokens.group();
            String dateFormat = dateTokens.group(1);
            String deltaString = dateTokens.group(2);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
            int delta = Integer.parseInt(deltaString);
            LocalDateTime finalDate = LocalDateTime.now().plusDays(delta);
            String tokenReplaced = formatter.format(finalDate);
            content = content.replace(tokenToReplace, tokenReplaced);
          }
          //Begins transforming tokens from yaml file
          Matcher yamlTokens = Pattern.compile(TOKEN_PATTERN).matcher(content);
          while (yamlTokens.find()) {
            tokenToReplace = yamlTokens.group();
            String path = yamlTokens.group(1);
            String tokenReplaced = JsonPath.read(yamlObj.toString(), "$." + path);
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

  /**
   *
   * @param yamlName — File name containing all values to be substituted
   * @return data — Parse and return yaml in json object
   * @throws IOException
   */

  private static Map<String, Object> getYamlObject(String yamlName) throws IOException {
    Map<String, Object> data;
    try (InputStream inputStream = new FileInputStream(yamlName)) {
      Yaml yaml = new Yaml();
      data = yaml.load(inputStream);
    } catch (IOException e) {
      throw new IOException(String.format(" Exception reading token object : %s", e.getMessage()));
    }
    return data;
  }

}
