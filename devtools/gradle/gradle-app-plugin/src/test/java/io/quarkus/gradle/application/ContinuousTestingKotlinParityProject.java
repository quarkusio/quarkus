package io.quarkus.gradle.application;

import java.io.IOException;
import java.nio.file.Path;

final class ContinuousTestingKotlinParityProject extends ContinuousTestingParityProject {

    ContinuousTestingKotlinParityProject(Path projectDirectory) {
        super(projectDirectory);
    }

    void writeApplication() throws IOException {
        String kotlinVersion = System.getProperty("kotlin_version", "2.4.0");
        String kspVersion = System.getProperty("ksp_version", "2.3.9");
        writeFile("settings.gradle", """
                pluginManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                    plugins {
                        id 'org.jetbrains.kotlin.jvm' version '%1$s'
                        id 'org.jetbrains.kotlin.kapt' version '%1$s'
                        id 'com.google.devtools.ksp' version '%2$s'
                    }
                }

                rootProject.name = 'continuous-testing-kotlin-parity'
                include 'processors'
                """.formatted(kotlinVersion, kspVersion));
        writeFile("gradle.properties", "version = 999-SNAPSHOT\n");
        writeFile("build.gradle", """
                buildscript {
                    dependencies {
                        classpath files(%1$s)
                    }
                }

                plugins {
                    id 'org.jetbrains.kotlin.jvm'
                    id 'org.jetbrains.kotlin.kapt'
                    id 'com.google.devtools.ksp'
                }

                apply plugin: 'io.quarkus.application'

                repositories {
                    mavenLocal()
                    mavenCentral()
                }

                dependencies {
                    implementation enforcedPlatform("io.quarkus:quarkus-bom:${project.property('version')}")
                    implementation "io.quarkus:quarkus-arc"
                    implementation "io.quarkus:quarkus-kotlin"
                    implementation "org.jetbrains.kotlin:kotlin-stdlib:%2$s"
                    testImplementation "io.quarkus:quarkus-junit:${project.property('version')}"
                    testImplementation project(':processors')
                    kaptTest project(':processors')
                    kspTest project(':processors')
                }
                """.formatted(ContinuousBuildTestSupport.pluginClasspathFiles(), kotlinVersion));
        writeFile("src/main/kotlin/org/acme/ParityApplication.kt", """
                package org.acme

                class ParityApplication
                """);
        writeProcessorProject(kspVersion);
    }

    void writeTest(String kotlinValue, String kaptValue, String kspValue) throws IOException {
        writeFile("src/test/kotlin/org/acme/GeneratedParityTest.kt", """
                package org.acme

                import java.nio.file.Files
                import java.nio.file.Path

                import org.acme.generated.KaptGenerated
                import org.acme.generated.KspGenerated
                import org.acme.processor.KaptValue
                import org.acme.processor.KspValue
                import org.junit.jupiter.api.Test

                @KaptValue("%1$s")
                class KaptInput

                @KspValue("%2$s")
                class KspInput

                class GeneratedParityTest {
                    @Test
                    fun recordsGeneratedValues() {
                        writeMarker("%3$s", "%4$s")
                        writeMarker("%5$s", KaptGenerated.value())
                        writeMarker("%6$s", KspGenerated.value())
                    }

                    private fun writeMarker(marker: String, value: String) {
                        val path = Path.of(marker)
                        Files.createDirectories(path.parent)
                        Files.writeString(path, value)
                    }
                }
                """.formatted(kaptValue, kspValue,
                propertyPath("build/parity/kotlin.txt"), kotlinValue,
                propertyPath("build/parity/kapt.txt"),
                propertyPath("build/parity/ksp.txt")));
    }

    private void writeProcessorProject(String kspVersion) throws IOException {
        writeFile("processors/build.gradle", """
                plugins {
                    id 'org.jetbrains.kotlin.jvm'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    compileOnly 'com.google.devtools.ksp:symbol-processing-api:%s'
                }
                """.formatted(kspVersion));
        writeFile("processors/src/main/java/org/acme/processor/KaptValue.java", """
                package org.acme.processor;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Retention(RetentionPolicy.SOURCE)
                @Target(ElementType.TYPE)
                public @interface KaptValue {
                    String value();
                }
                """);
        writeFile("processors/src/main/java/org/acme/processor/KspValue.java", """
                package org.acme.processor;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Retention(RetentionPolicy.SOURCE)
                @Target(ElementType.TYPE)
                public @interface KspValue {
                    String value();
                }
                """);
        writeKaptProcessor();
        writeKspProcessor();
        writeFile("processors/src/main/resources/META-INF/services/javax.annotation.processing.Processor",
                "org.acme.processor.KaptValueProcessor\n");
        writeFile("processors/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider",
                "org.acme.processor.KspValueProcessorProvider\n");
    }

    private void writeKaptProcessor() throws IOException {
        writeFile("processors/src/main/java/org/acme/processor/KaptValueProcessor.java", """
                package org.acme.processor;

                import java.io.IOException;
                import java.io.Writer;
                import java.util.Set;

                import javax.annotation.processing.AbstractProcessor;
                import javax.annotation.processing.RoundEnvironment;
                import javax.annotation.processing.SupportedAnnotationTypes;
                import javax.lang.model.SourceVersion;
                import javax.lang.model.element.Element;
                import javax.lang.model.element.TypeElement;

                @SupportedAnnotationTypes("org.acme.processor.KaptValue")
                public class KaptValueProcessor extends AbstractProcessor {
                    @Override
                    public SourceVersion getSupportedSourceVersion() {
                        return SourceVersion.latestSupported();
                    }

                    @Override
                    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
                        for (Element element : roundEnvironment.getElementsAnnotatedWith(KaptValue.class)) {
                            String value = element.getAnnotation(KaptValue.class).value();
                            try (Writer writer = processingEnv.getFiler()
                                    .createSourceFile("org.acme.generated.KaptGenerated", element)
                                    .openWriter()) {
                                writer.write("package org.acme.generated;\\n"
                                        + "public final class KaptGenerated {\\n"
                                        + "    private KaptGenerated() {}\\n"
                                        + "    public static String value() { return \\"" + value + "\\"; }\\n"
                                        + "}\\n");
                            } catch (IOException e) {
                                throw new IllegalStateException("Failed to generate KAPT parity source", e);
                            }
                        }
                        return true;
                    }
                }
                """);
    }

    private void writeKspProcessor() throws IOException {
        writeFile("processors/src/main/kotlin/org/acme/processor/KspValueProcessor.kt", """
                package org.acme.processor

                import com.google.devtools.ksp.processing.Dependencies
                import com.google.devtools.ksp.processing.Resolver
                import com.google.devtools.ksp.processing.SymbolProcessor
                import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
                import com.google.devtools.ksp.processing.SymbolProcessorProvider
                import com.google.devtools.ksp.symbol.KSClassDeclaration

                class KspValueProcessorProvider : SymbolProcessorProvider {
                    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
                        KspValueProcessor(environment)
                }

                private class KspValueProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
                    private var generated = false

                    override fun process(resolver: Resolver) = emptyList<com.google.devtools.ksp.symbol.KSAnnotated>().also {
                        if (generated) {
                            return@also
                        }
                        val symbol = resolver.getSymbolsWithAnnotation(KspValue::class.qualifiedName!!)
                            .filterIsInstance<KSClassDeclaration>()
                            .firstOrNull() ?: return@also
                        val annotation = symbol.annotations.first { it.shortName.asString() == "KspValue" }
                        val value = annotation.arguments.first { it.name?.asString() == "value" }.value as String
                        environment.codeGenerator.createNewFile(
                            Dependencies(false, symbol.containingFile!!),
                            "org.acme.generated",
                            "KspGenerated",
                            "kt"
                        ).writer().use { writer ->
                            writer.write("package org.acme.generated\\n\\n"
                                + "object KspGenerated {\\n"
                                + "    @JvmStatic\\n"
                                + "    fun value(): String = \\"" + value + "\\"\\n"
                                + "}\\n")
                        }
                        generated = true
                    }
                }
                """);
    }
}
