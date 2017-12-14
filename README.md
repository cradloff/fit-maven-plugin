Fit Maven Plugin
================

Use this plugin to execute fit tests from maven.

pom.xml:

     <plugin>
        <groupId>fit.plugin</groupId>
        <artifactId>fit-maven-plugin</artifactId>
        <version>3.0-SNAPSHOT</version>
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

files

stylesheet
