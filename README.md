# BDD-Transform-Values

## Introduction
This library when run as a maven plugin will transform values on feature files in Cucumber-JVM based
automation, and in some cases, this helps to substitute values, mostly for the commonly used data from yaml
files.

## Requirement

[Maven](https://maven.apache.org/)

[Cucumber-JVM](https://cucumber.io/docs/installation/java/)

[Jayway's JsonPath()](https://github.com/json-path/JsonPath)

## Installation

Add this block to pom.xml

    <plugins>
      ...
      <plugin>
        <groupId>io.github.5v1988</groupId>
        <artifactId>bdd-transform-values-maven-plugin</artifactId>
        <version>1.0.2</version>
        <executions>
          <execution>
            <goals>
              <goal>transform-values</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      ...
    </plugins>

By default, this plugin will look for substituting values from the file: `token.yaml`.
Optionally, the file with a different name can also be passed as follows:

    <plugin>
        ...
        <configuration>
            <tokenFileName>another-file.yaml</tokenFileName>
        </configuration>
        ...
    </plugin>

After adding this on pom.xml, along surefire-plugin, `mvn test` either substitutes values from 
yaml file by matching the path or auto-generate like in case of date values. Please also note that
it doesn't modify the original feature files, but the ones in build directory: `target`

## Examples:

```yaml
test-config:
  url: https://test.com
account:
  email: test@email.com
  password: qa-test123!
  name: Tommy
```

```gherkin
Feature: Login feature

  @example1
  Scenario: To verify that the user logins successfully [without plugin]
    Given User opens the app url: "https://test.com"
    When User enters account details: "test@email.com" and "qa-test123!"
    Then User verifies the name as : "Tommy" and date as "06/26/2023"

  @example2
  Scenario Outline: To verify that the user logins successfully [with plugin]
    Given User opens the app url: "[ type: token, path: test-config.url ]"
    When User enters account details: "<UserName>" and "<Password>"
    Then User verifies the name as : "<Name>" and date as "[ type: date, format: MM/dd/yyyy, delta: 0 ]"

    Examples:
      | UserName                             | Password                                | Name                                |
      | [ type: token, path: account.email ] | [ type: token, path: account.password ] | [ type: token, path: account.name ] |
```

##Explanation:

`[ type: token, path: account.email ]` => In this template, Jsonpath `account.name` gets 
value `Tommy` from the yaml file

`[ type: date, format: MM/dd/yyyy, delta: -1 ]` => Yesterday's date in format: MM/dd/yyyy

`[ type: date, format: dd-MM-yyyy, delta: 1 ]` => Tomorrow's date in format: dd-MM-yyyy

##In Progress:

`[ type: random, format: first-name ]` => This template returns randomly generated mobile
number. other formats could be last-name, full-name, mobile-number, address-line-1 etc. 





