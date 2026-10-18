package io.quarkus.redis.generator;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescription;

import io.smallrye.mutiny.Uni;

public class ClassGeneratorHelper {

    private ClassGeneratorHelper() {
        // Avoid direct instantiation
    }

    public static Type getUniOfVoid() {
        JavaParser parser = new JavaParser();
        return parser.parseClassOrInterfaceType("Uni<Void>").getResult().get();
    }

    public static Type getUniOfResponse() {
        JavaParser parser = new JavaParser();
        return parser.parseClassOrInterfaceType("Uni<Response>").getResult().get();
    }

    public static void copyClassJavadocAndAppendContent(ClassOrInterfaceDeclaration api,
            ClassOrInterfaceDeclaration blockingTxApi, String append) {
        JavadocComment copy = api.getJavadocComment().orElseThrow().clone();
        Javadoc javadoc = copy.parse();
        JavadocDescription description = javadoc.getDescription();
        if (append != null) {
            String newJavadocDescription = javadoc.getDescription().toText() + "\n" + append + "\n";
            description = JavadocDescription.parseText(newJavadocDescription);
        }
        Javadoc updatedJavadoc = new Javadoc(description);
        updatedJavadoc.getBlockTags().addAll(javadoc.getBlockTags());
        blockingTxApi.setJavadocComment(updatedJavadoc);
    }

    public static void copyImportsExceptUni(CompilationUnit unit, CompilationUnit cu) {
        for (ImportDeclaration is : unit.getImports()) {
            if (!is.toString().contains(Uni.class.getName())) {
                cu.addImport(is);
            }
        }
    }

    public static void copyImports(CompilationUnit unit, CompilationUnit cu) {
        cu.setImports(unit.getImports());
    }

    public static void setPackage(CompilationUnit unit, CompilationUnit cu) {
        cu.setPackageDeclaration(unit.getPackageDeclaration().orElseThrow());
    }

    public static Type getBlockingType(Type type) {
        ClassOrInterfaceType asClass = type.asClassOrInterfaceType();
        Type param = asClass.getTypeArguments().orElseThrow().get(0);
        if (param.asString().equals(Void.class.getSimpleName())) {
            return new VoidType();
        }
        if (param.isClassOrInterfaceType()) {
            if (param.asClassOrInterfaceType().isBoxedType()) {
                return param.asClassOrInterfaceType().toUnboxedType();
            }
        }
        return param;
    }

    public static boolean isReturningUniOfVoid(Type type) {
        if (!type.isClassOrInterfaceType()) {
            return false;
        }
        ClassOrInterfaceType r = type.asClassOrInterfaceType();
        if (!r.getName().asString().equals(Uni.class.getSimpleName())) {
            return false;
        }
        if (r.getTypeArguments().isEmpty()) {
            return false;
        }
        if (r.getTypeArguments().get().size() != 1) {
            return false;
        }
        Type param = r.getTypeArguments().get().get(0);
        return param.asClassOrInterfaceType().getName().asString().equals(Void.class.getSimpleName());
    }

}
