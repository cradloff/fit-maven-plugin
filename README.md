# Fit Maven Plugin

Use this plugin to execute [Fit tests](http://fit.c2.com/) from Maven. Fit is a framework for tests
which are specified in a Html page. It is a great complement for JUnit.

## Versioning
The versioning for this plugin starts with 3.0. This is because there is already a plugin with the same
name (but another group-id), that started with version 2.0. That project never left beta status and had
some severe bugs, that prevented me from using it, so I started my own plugin. To prevent confusion
which project is more up to date, I decided to start with version 3.0.

## Usage
Add the plugin to your `pom.xml`:

    <plugin>
      <groupId>com.github.cradloff</groupId>
      <artifactId>fit-maven-plugin</artifactId>
      <version>3.1</version>
      <executions>
        <execution>
          <id>fixture</id>
          <phase>integration-test</phase>
          <goals>
            <goal>run</goal>
          </goals>
        </execution>
      </executions>
    </plugin>

To execute the Fit tests along with your JUnit tests, change `phase` to `test`.

Add Fit library to your dependencies:

    <dependency>
      <groupId>com.c2.fit</groupId>
      <artifactId>fit</artifactId>
      <version>1.1</version>
    </dependency>

Put your tests under `src/test/fit`, your Fixtures under `src/test/java`. To format your tests,
add a CSS stylesheet and save it to `src/test/fit/fixture.css`. The stylesheet will be copied
to the target under `target/fit`.

Build your project and view the result at `target/fit/summary.html`. The summary will list all
fit tests along with the number of right, wrong and exceptions. Additionally the time to execute the
test and the title are displayed.

## Configuration
The following parameters can be specified in the `configuration` section in `execution`:

<dl>
<dt>sourceDirectory</dt>
<dd>Location of source directory. Default: src/test/fit</dd>

<dt>outputDirectory</dt>
<dd>Location of output directory. Default: ${project.build.directory}/fit</dd>

<dt>caseSensitive</dt>
<dd>specify whether sourceIncludes is case sensitive.Default: true</dd>

<dt>sourceIncludes</dt>
<dd>Pattern for Fit Tests as comma separated values (e.g. "*.html,*.xhtml,*.fit"). Default: *.html</dd>

<dt>ignoreFailures</dt>
<dd>ignore failures? Default: false</dd>

<dt>sourceEncoding</dt>
<dd>encoding of input files Default: ${project.build.sourceEncoding}</dd>

<dt>outputEncoding</dt>
<dd>encoding of output files Default: ${project.reporting.outputEncoding}</dd>
</dl>

The plugin definition with all parameters (with default values on unix) looks like this:

    <plugin>
      <groupId>com.github.cradloff</groupId>
      <artifactId>fit-maven-plugin</artifactId>
      <version>3.1</version>
      <executions>
        <execution>
          <configuration>
            <sourceDirectory>src/test/fit</sourceDirectory>
            <outputDirectory>target/fit</outputDirectory>
            <caseSensitive>true</caseSensitive>
            <sourceIncludes>*.html</sourceIncludes>
            <ignoreFailures>false</ignoreFailures>
            <sourceEncoding>UTF-8</sourceEncoding>
            <outputEncoding>UTF-8</outputEncoding>
          </configuration>
          <id>fixture</id>
          <phase>integration-test</phase>
          <goals>
            <goal>run</goal>
          </goals>
        </execution>
      </executions>
    </plugin>

## Example

`src/test/fit/Calculator.java`:

    package fit;
    
    public class Calculator extends ColumnFixture {
        public int value1;
        public int value2;
        public String operator;
    
        public int result() {
            int result;
            switch (operator) {
            case "+":
                result = value1 + value2;
                break;
            case "-":
                result = value1 - value2;
                break;
            case "*":
                result = value1 * value2;
                break;
            case "/":
                result = value1 / value2;
                break;
            default:
                throw new IllegalArgumentException("unknown operator " + operator);
            }
    
            return result;
        }
    }

`src/test/fit/fixture.css`:

    table {
        border-collapse: collapse;
        margin-top: 0.5em;
    }
    
    td, th {
        border: 1px solid black;
    }
    
    .head {
        background-color: #EEEEEE;
    }
    
    .highlight {
        background-color: #F8F8F8;
    }
    
    .number {
        text-align: right;
    }

`src/test/fit/calculator.html`:

    <html>
    <head>
      <meta charset="UTF-8">
      <!-- title gets added to summary.html -->
      <title>Calculator</title>
      <!-- include css stylesheet -->
      <link rel="stylesheet" href="fixture.css" />
    </head>
    <body>
    <h1>Calculator Test</h1>
    <p>This test runs a simple calculator.</p>
    
    <!-- test calculator -->
    <table>
      <caption>Calculator</caption>
      <tr class="head">
        <td colspan="4">fit.Calculator</td>
      </tr>
      <tr class="head">
        <td>value1</td>
        <td>value2</td>
        <td>operator</td>
        <td>result()</td>
      </tr>
      <tr><td>1</td><td>1</td><td>+</td><td>2</td></tr>
      <tr><td>1</td><td>1</td><td>-</td><td>0</td></tr>
      <tr><td>1</td><td>1</td><td>*</td><td>1</td></tr>
      <tr><td>1</td><td>1</td><td>/</td><td>1</td></tr>
      <tr><td>2</td><td>2</td><td>+</td><td>4</td></tr>
      <tr><td>2</td><td>2</td><td>-</td><td>0</td></tr>
      <tr><td>2</td><td>2</td><td>*</td><td>4</td></tr>
      <tr><td>2</td><td>2</td><td>/</td><td>1</td></tr>
      <tr><td>3</td><td>3</td><td>+</td><td>6</td></tr>
      <tr><td>3</td><td>3</td><td>-</td><td>0</td></tr>
      <tr><td>3</td><td>3</td><td>*</td><td>9</td></tr>
      <tr><td>3</td><td>3</td><td>/</td><td>1</td></tr>
    </table>
    
    <!-- display a summary -->
    <table>
      <caption>Summary</caption>
      <tr class="head">
        <td colspan="2">fit.Summary</td>
      </tr>
    </table>
    
    </body>
    </html>

Run your project:

    mvn install
    
    [INFO] --- fit-maven-plugin:3.1:run (fixture) @ myproject ---
    [INFO] Executing fit tests (*.html) in /home/user/workspace/myproject/src/test/fit
    [INFO] Writing results to /home/user/workspace/myproject/target/fit
    [INFO] Current working directory: /home/user/workspace/myproject
    [INFO] Result: 12 right, 0 wrong, 0 ignored, 0 exceptions

View the results in `target/fit/calculator.html` and the summary in `target/fit/summary.html`.
