<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>${project_groupId}</groupId>
    <artifactId>${project_artifactId}</artifactId>
    <version>${project_version}</version>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>

        <quarkus.version>${quarkus_version}</quarkus.version>
        <surefire-plugin.version>${surefire_plugin_version}</surefire-plugin.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>${plugin_groupId}</groupId>
                <artifactId>${bom_artifactId}</artifactId>
                <version>${quarkus.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-resteasy</artifactId>
        </dependency>

        <!-- Test dependencies -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-junit5</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Quarkus build plugin - also provides the quarkus:dev live-reload feature -->
            <plugin>
                <groupId>${plugin_groupId}</groupId>
                <artifactId>${plugin_artifactId}</artifactId>
                <version>${quarkus.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>build</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <!-- Run the tests in JVM mode -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>${surefire-plugin.version}</version>
                <configuration>
                    <systemProperties>
                        <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                    </systemProperties>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <!-- Use this profile to build a native executable using GraalVM -->
        <profile>
            <id>native</id>
            <activation>
                <property>
                    <name>native</name>
                </property>
            </activation>
            <build>
                <plugins>
                    <plugin>
                        <groupId>${plugin_groupId}</groupId>
                        <artifactId>${plugin_artifactId}</artifactId>
                        <version>${quarkus.version}</version>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>native-image</goal>
                                </goals>
                                <configuration>
                                    <enableHttpUrlHandler>true</enableHttpUrlHandler>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                    <!-- Run the tests with the native executable -->
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-failsafe-plugin</artifactId>
                        <version>${surefire-plugin.version}</version>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>integration-test</goal>
                                    <goal>verify</goal>
                                </goals>
                                <configuration>
                                    <systemProperties>
                                        <native.image.path>${project.build.directory}/${project.build.finalName}-runner</native.image.path>
                                    </systemProperties>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>

</project>
