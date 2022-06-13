package io.quarkus.grpc.deployment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.utils.SourceRoot;

import io.quarkus.deployment.CodeGenContext;

public class GrpcPostProcessing {

    private static final Logger log = Logger.getLogger(GrpcPostProcessing.class);

    private static final String POST_PROCESS_QUARKUS_GENERATED_ANNOTATION = "quarkus.generate-code.grpc-post-processing.use-quarkus-generated-annotation";
    private static final String POST_PROCESS_NO_FINAL = "quarkus.generate-code.grpc-post-processing.no-final";
    public static final String JAVAX_GENERATED = "javax.annotation.Generated";
    public static final String QUARKUS_GENERATED = "io.quarkus.grpc.common.Generated";
    public static final String STUB = "Stub";
    public static final String BIND_METHOD = "bindService";

    private final CodeGenContext context;
    private final Path root;

    public GrpcPostProcessing(CodeGenContext context, Path root) {
        this.context = context;
        this.root = root;
    }

    private boolean isEnabled(String name, boolean def) {
        return Boolean.getBoolean(name) || context.config().getOptionalValue(name, Boolean.class).orElse(def);
    }

    public void postprocess() {
        SourceRoot sr = new SourceRoot(root);
        try {
            // Parse all files from root
            List<ParseResult<CompilationUnit>> results = sr.tryToParse();
            for (ParseResult<CompilationUnit> result : results) {
                if (result.isSuccessful()) {
                    CompilationUnit unit = result.getResult().orElseThrow(); // the parsing succeed, so we can retrieve the cu
                    CompilationUnit.Storage storage = unit.getStorage().orElseThrow(); // we read from the FS, so we have a storage
                    if (unit.getPrimaryType().isPresent()) {
                        TypeDeclaration<?> type = unit.getPrimaryType().get();
                        postprocess(unit, type, context.config());
                    }
                    Files.write(storage.getPath(), List.of(unit.toString()), StandardCharsets.UTF_8);
                } else {
                    // Compilation issue - report and skip
                    log.errorf(
                            "Unable to parse a class generated using protoc, skipping post-processing for this " +
                                    "file. Reported problems are %s",
                            result.toString());
                }

            }
        } catch (Exception e) {
            // read issue, report and exit
            log.error("Unable to parse the classes generated using protoc - skipping gRPC post processing", e);
        }
    }

    private void postprocess(CompilationUnit unit, TypeDeclaration<?> primary, Config config) {
        log.debugf("Post-processing %s", primary.getFullyQualifiedName().orElse(primary.getNameAsString()));

        unit.accept(new ModifierVisitor<Void>() {

            @Override
            public Visitable visit(NormalAnnotationExpr n, Void arg) {
                if (isEnabled(POST_PROCESS_QUARKUS_GENERATED_ANNOTATION, true)) {
                    if (n.getNameAsString().equals(JAVAX_GENERATED)) {
                        n.setName(QUARKUS_GENERATED);
                    }
                }
                return super.visit(n, arg);
            }

            @Override
            public Visitable visit(ClassOrInterfaceDeclaration n, Void arg) {
                if (isEnabled(POST_PROCESS_NO_FINAL, true)) {
                    if (n.hasModifier(Modifier.Keyword.FINAL) && n.getNameAsString().endsWith(STUB)) {
                        n.removeModifier(Modifier.Keyword.FINAL);
                    }
                }
                return super.visit(n, arg);
            }

            @Override
            public Visitable visit(MethodDeclaration n, Void arg) {
                if (isEnabled(POST_PROCESS_NO_FINAL, true)) {
                    if (n.hasModifier(Modifier.Keyword.FINAL)
                            && n.getNameAsString().equalsIgnoreCase(BIND_METHOD)) {
                        n.removeModifier(Modifier.Keyword.FINAL);
                    }
                }
                return super.visit(n, arg);
            }
        }, null);
    }
}
