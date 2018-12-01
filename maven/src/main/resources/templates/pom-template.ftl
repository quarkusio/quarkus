<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>${mProjectGroupId}</groupId>
    <artifactId>${mProjectArtifactId}</artifactId>
    <version>${mProjectVersion}</version>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>

        <shamrock.version>${shamrockVersion}</shamrock.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.jboss.shamrock</groupId>
            <artifactId>shamrock-jaxrs-deployment</artifactId>
            <scope>provided</scope>
            <version>${r"${shamrock.version}"}</version>
        </dependency>
        <dependency>
            <groupId>org.jboss.shamrock</groupId>
            <artifactId>shamrock-arc-deployment</artifactId>
            <scope>provided</scope>
            <version>${r"${shamrock.version}"}</version>
        </dependency>
        <dependency>
            <groupId>org.jboss.shamrock</groupId>
            <artifactId>shamrock-logging-deployment</artifactId>
            <scope>provided</scope>
            <version>${r"${shamrock.version}"}</version>
        </dependency>

        <!-- Test dependencies -->
        <dependency>
            <groupId>org.jboss.shamrock</groupId>
            <artifactId>shamrock-junit</artifactId>
            <version>${r"${shamrock.version}"}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>3.2.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jboss.shamrock</groupId>
                <artifactId>shamrock-maven-plugin</artifactId>
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
                        <groupId>org.jboss.shamrock</groupId>
                        <artifactId>shamrock-maven-plugin</artifactId>
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
