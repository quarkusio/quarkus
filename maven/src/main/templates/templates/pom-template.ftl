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

        <shamrock.version>${shamrock_version}</shamrock.version>
        <restassured.version>${rest_assured_version}</restassured.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>${plugin_groupId}</groupId>
                <artifactId>${bom_artifactId}</artifactId>
                <version>${r"${shamrock.version}"}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.jboss.shamrock</groupId>
            <artifactId>shamrock-jaxrs-deployment</artifactId>
        </dependency>
        <dependency>
            <groupId>org.jboss.shamrock</groupId>
            <artifactId>shamrock-arc-deployment</artifactId>
        </dependency>

        <!-- Test extensions -->
        <dependency>
            <groupId>org.jboss.shamrock</groupId>
            <artifactId>shamrock-junit</artifactId>
        </dependency>
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>${r"${restassured.version}"}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>${plugin_groupId}</groupId>
                <artifactId>${plugin_artifactId}</artifactId>
                <version>${r"${shamrock.version}"}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>build</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <profile>
            <id>native</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>${plugin_groupId}</groupId>
                        <artifactId>${plugin_artifactId}</artifactId>
                        <version>${r"${shamrock.version}"}</version>
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
                </plugins>
            </build>
        </profile>
    </profiles>

</project>
