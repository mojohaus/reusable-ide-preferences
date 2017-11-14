# MojoHaus Reusable IDE preferences plugin

This plugin allows simple management of IDE preferences by deploying them as Maven artifacts
and allowing them to be applied using Maven goals.

## Packaging IDE preferences

The first step is packaging the IDE preferences. Currently the only supported type
is `eclipse-formatter`. The artifact must have a packaging type of `eclipse-formatter` and 
configure the `eclipse-preferences-maven-plugin` with extensions set to true. The sample project
_sample-eclipse-formatter_ exemplifies how this should work.

## Applying IDE preferences

Once the IDE preferences are packaged and deployed to a Maven repository, in any Maven project
you can apply the following configuration:

    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>eclipse-preferences-maven-plugin</artifactId>
        <version>0.0.3-SNAPSHOT</version>
        <extensions>true</extensions>
        <executions>
            <execution>
                <phase>generate-resources</phase>
                <goals>
                    <goal>configure</goal>
                </goals>
            </execution>
        </executions>
    </plugin>


Additionally, include a dependency on your formatter.

    <dependency>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>demo</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <type>eclipse-formatter</type>
    </dependency>

When the project is built using the Maven CLI or when the project is imported in
Eclipse, the preferences will be applied. For performance reasons, refreshing the
dependency does not automatically trigger an update in Eclipse, you will need to
manually update the Maven project using the _Maven → Update Project..._ command.

## Additional configuration

The plug-in also allows easy configuration of other IDE settings. The example below
configures Eclipse Mylyn to automatically set the commit message and associate the
project to a task repository. 

    <configuration>
        <repository>
            <kind>jira</kind>
            <url>https://issues.apache.org/jira</url>
        </repository>
        <commitTemplate>\n\nBug: ${task.key} ( ${task.description} )</commitTemplate>
    </configuration>
    
## Building

To build the Maven plug-in, use `mvn install`. To also build the samples, make sure that the
plug-in is installed or available in a remote Maven repository and run `mvn install -Psamples -Denforcer.skip`.
The reason for this more complicated invocation is that the samples reference a -SNAPSHOT
version of the plugin, which does not work for build extensions and is also blocked
by the maven-enforcer-plugin configuration. This limitation will be removed after a first
release.