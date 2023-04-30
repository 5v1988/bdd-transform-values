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
  public static String YAML_FILE_EXT = ".yaml";
  public static String YML_FILE_EXT = ".yml";

  @Parameter(defaultValue = "${project.build.directory}", required = true)
  private File target;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("Begins substituting all tokenized values in feature files!");

    try {
      Stream<Path> allYaml = Files.walk(Paths.get(target.getAbsolutePath())).filter(yaml ->
          yaml.toFile().getAbsolutePath().trim().endsWith(YAML_FILE_EXT) |
              yaml.toFile().getAbsolutePath().trim().endsWith(YML_FILE_EXT));
      String ymlName = allYaml.collect(Collectors.toList()).get(0).toFile().getAbsolutePath();
      JSONObject yamlObj = new JSONObject(getYamlObject(ymlName));
      getLog().info(String.format("Picking up Yaml file: %s for the replacing values", ymlName));

      Stream<Path> features = Files.walk(Paths.get(target.getAbsolutePath())).filter(feature ->
          feature.toFile().getAbsolutePath().trim().endsWith(FEATURE_FILE_EXT));

      features.forEach(feature -> {
        getLog().info(String.format("Processing the feature file: %s", feature.toFile().getAbsolutePath()));
        try {
          Charset charset = StandardCharsets.UTF_8;
          String content = null;
          String tokenToReplace, tokenJp;
          content = new String(Files.readAllBytes(feature), charset);
          Matcher m = Pattern.compile("(\\(.*\\))").matcher(content);
          while (m.find()) {
            tokenToReplace = m.group();
            tokenJp = m.group().replace("(", "").replace(")", "");
            String tokenReplaced = JsonPath.read(yamlObj.toString(), "$." + tokenJp);
            content = content.replace(tokenToReplace, tokenReplaced);
            getLog().info(String.format("Replaced Token: %s", tokenToReplace));
            getLog().info(String.format("Replacing Token: %s", tokenReplaced));
          }
          Files.write(feature, content.getBytes(charset));
        } catch (IOException io) {
          getLog().error(io.getMessage());
        }
      });
    } catch (IOException io) {
      getLog().error(io.getMessage());
    }

  }

  private static Map<String, Object> getYamlObject(String yamlName) throws IOException {
    Map<String, Object> data;
    try (InputStream inputStream = new FileInputStream(yamlName)) {
      Yaml yaml = new Yaml();
      data = yaml.load(inputStream);
    } catch (IOException e) {
      throw new IOException(" Exception reading Yaml object!");
    }
    return data;
  }

}
