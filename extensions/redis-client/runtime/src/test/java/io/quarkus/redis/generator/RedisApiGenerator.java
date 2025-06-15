package io.quarkus.redis.generator;

import static io.quarkus.redis.generator.ClassGeneratorHelper.copyClassJavadocAndAppendContent;
import static io.quarkus.redis.generator.ClassGeneratorHelper.copyImports;
import static io.quarkus.redis.generator.ClassGeneratorHelper.copyImportsExceptUni;
import static io.quarkus.redis.generator.ClassGeneratorHelper.getBlockingType;
import static io.quarkus.redis.generator.ClassGeneratorHelper.getUniOfResponse;
import static io.quarkus.redis.generator.ClassGeneratorHelper.isReturningUniOfVoid;
import static io.quarkus.redis.generator.ClassGeneratorHelper.setPackage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.ReactiveTransactionalRedisCommands;
import io.quarkus.redis.datasource.RedisCommands;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.TransactionalRedisCommands;
import io.quarkus.redis.datasource.stream.ReactiveStreamCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;
import io.quarkus.redis.runtime.datasource.AbstractRedisCommandGroup;
import io.quarkus.redis.runtime.datasource.AbstractRedisCommands;
import io.quarkus.redis.runtime.datasource.AbstractTransactionalCommands;
import io.quarkus.redis.runtime.datasource.AbstractTransactionalRedisCommandGroup;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.RedisCommandExecutor;
import io.quarkus.redis.runtime.datasource.TransactionHolder;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

/**
 * A tools generating the various interface and implementation from the existing reactive API.
 */

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class RedisApiGenerator {

    private static final Logger LOGGER = Logger.getLogger("redis-api-generator");

    public static void main(String[] args) throws FileNotFoundException {
        // PARAMETERS
        String reactiveApi = ReactiveStreamCommands.class.getName();
        String prefix = "x";
        // ---------

        File out = new File("extensions/redis-client/runtime/target/generation");
        out.mkdirs();

        // Read existing API
        CompilationUnit cu = StaticJavaParser.parse(toFile(reactiveApi));
        ClassOrInterfaceDeclaration reactiveAPI = (ClassOrInterfaceDeclaration) cu.getPrimaryType().orElseThrow();

        // Generate APIs
        ClassOrInterfaceDeclaration blockingAPI = generateBlockingInterface(cu, reactiveAPI, out);
        ClassOrInterfaceDeclaration txBlockingAPI = generateBlockingTransactionalInterface(cu, reactiveAPI, out);
        ClassOrInterfaceDeclaration txReactiveAPI = generateBlockingReactiveTransactionalInterface(cu, reactiveAPI,
                out);

        // Generate Implementations
        generateBlockingImplementation(cu, reactiveAPI, blockingAPI, out);
        generateBlockingTransactionalImplementation(cu, txReactiveAPI, txBlockingAPI, out);

        // Generate skeletons
        ClassOrInterfaceDeclaration abstractImpl = generateAbstractSkeleton(cu, reactiveAPI, out, prefix);
        generateReactiveImplSkeleton(cu, reactiveAPI, abstractImpl, out);
        generateReactiveTransactionalImplSkeleton(cu, reactiveAPI, txReactiveAPI, out);
    }

    private static void generateReactiveImplSkeleton(CompilationUnit unit, ClassOrInterfaceDeclaration reactiveAPI,
            ClassOrInterfaceDeclaration abstractImpl, File dir) {
        String classname = reactiveAPI.getNameAsString() + "Impl";
        CompilationUnit cu = new CompilationUnit();
        cu.setPackageDeclaration("io.quarkus.redis.runtime.datasource");
        copyImports(unit, cu);
        cu.addImport(
                unit.getPackageDeclaration().orElseThrow().getNameAsString() + "." + reactiveAPI.getNameAsString());
        cu.addImport(Response.class);
        cu.addImport(unit.getPackageDeclaration().orElseThrow().getNameAsString(), false, true);

        String itf = reactiveAPI.getNameAsString();
        String ac = abstractImpl.getNameAsString();
        if (!reactiveAPI.getTypeParameters().isEmpty()) {
            itf += "<";
            itf += reactiveAPI.getTypeParameters().stream().map(NodeWithSimpleName::getNameAsString)
                    .collect(Collectors.joining(","));
            itf += ">";
            ac += "<";
            ac += reactiveAPI.getTypeParameters().stream().map(NodeWithSimpleName::getNameAsString)
                    .collect(Collectors.joining(","));
            ac += ">";
        }

        ClassOrInterfaceDeclaration clazz = cu.addClass(classname).addExtendedType(ac).addImplementedType(itf)
                .addImplementedType(ReactiveRedisCommands.class).setTypeParameters(reactiveAPI.getTypeParameters());

        clazz.addField(ReactiveRedisDataSource.class, "reactive").setPrivate(true).setFinal(true);
        ConstructorDeclaration cst = clazz.addConstructor(Modifier.Keyword.PUBLIC)
                .addParameter(ReactiveRedisDataSourceImpl.class, "redis");
        for (TypeParameter tp : reactiveAPI.getTypeParameters()) {
            cst.addParameter("Class<" + tp.getNameAsString() + ">", tp.getNameAsString().toLowerCase());
        }
        String cstLine1 = "super(redis, " + reactiveAPI.getTypeParameters().stream()
                .map(tp -> tp.getNameAsString().toLowerCase()).collect(Collectors.joining(",")) + ");";
        String cstLine2 = "this.reactive = redis;";
        cst.getBody().addStatement(cstLine1).addStatement(cstLine2);

        clazz.addMethod("getDataSource").addMarkerAnnotation(Override.class).setType(ReactiveRedisDataSource.class)
                .setPublic(true).getBody().get().addStatement("return reactive;");

        for (MethodDeclaration method : reactiveAPI.getMethods()) {
            MethodDeclaration declaration = clazz.addMethod(method.getNameAsString())
                    .setModifiers(method.getModifiers()).setParameters(method.getParameters()).setPublic(true)
                    .setType(method.getType()).addMarkerAnnotation(Override.class);

            String block = "{" + "// TODO IMPLEMENT ME\n" + "// return super._" + method.getNameAsString() + "("
                    + method.getParameters().stream().map(NodeWithSimpleName::getNameAsString).collect(
                            Collectors.joining(","))
                    + ")\n" + "// .map(r -> decode(r));\n" + "return null;" + "\n}";
            declaration.setBody(new JavaParser().parseBlock(block).getResult().get());
        }

        try {
            File file = new File(dir, classname + ".java");
            LOGGER.infof("Generating reactive implementation for %s into %s", reactiveAPI.getName(),
                    file.getAbsolutePath());
            Files.writeString(file.toPath(), cu.toString());
        } catch (IOException e) {
            LOGGER.errorf("Unable to write reactive implementation %s", classname, e);
        }

    }

    private static void generateReactiveTransactionalImplSkeleton(CompilationUnit unit,
            ClassOrInterfaceDeclaration reactiveAPI, ClassOrInterfaceDeclaration reactiveTxAPI, File dir) {
        String classname = reactiveTxAPI.getNameAsString() + "Impl";
        String impl = reactiveAPI.getNameAsString() + "Impl";
        CompilationUnit cu = new CompilationUnit();
        cu.setPackageDeclaration("io.quarkus.redis.runtime.datasource");
        copyImports(unit, cu);
        cu.addImport(
                unit.getPackageDeclaration().orElseThrow().getNameAsString() + "." + reactiveTxAPI.getNameAsString());
        cu.addImport(Response.class);
        cu.addImport(unit.getPackageDeclaration().orElseThrow().getNameAsString(), false, true);

        String rx = reactiveTxAPI.getNameAsString();
        if (!reactiveTxAPI.getTypeParameters().isEmpty()) {
            rx = rx + "<" + reactiveTxAPI.getTypeParameters().stream().map(NodeWithSimpleName::getNameAsString)
                    .collect(Collectors.joining(",")) + ">";
            impl = impl + "<" + reactiveTxAPI.getTypeParameters().stream().map(NodeWithSimpleName::getNameAsString)
                    .collect(Collectors.joining(",")) + ">";
        }

        ClassOrInterfaceDeclaration clazz = cu.addClass(classname).addExtendedType(AbstractTransactionalCommands.class)
                .addImplementedType(rx).setTypeParameters(reactiveTxAPI.getTypeParameters());

        clazz.addField(impl, "reactive").setPrivate(true).setFinal(true);
        ConstructorDeclaration cst = clazz.addConstructor(Modifier.Keyword.PUBLIC)
                .addParameter(ReactiveTransactionalRedisDataSource.class, "ds").addParameter(impl, "reactive")
                .addParameter(TransactionHolder.class, "tx");
        String cstLine1 = "super(ds, tx);";
        String cstLine2 = "this.reactive = reactive;";
        cst.getBody().addStatement(cstLine1).addStatement(cstLine2);

        for (MethodDeclaration method : reactiveTxAPI.getMethods()) {
            MethodDeclaration rxDecl = findMethod(reactiveAPI, method);
            MethodDeclaration declaration = clazz.addMethod(method.getNameAsString())
                    .setModifiers(method.getModifiers()).setPublic(true).setParameters(method.getParameters())
                    .setType(method.getType()).addMarkerAnnotation(Override.class);

            String expectedType = "";
            if (rxDecl != null) {
                expectedType = rxDecl.getType().asClassOrInterfaceType().asString();
            }
            String block = "{" + "// TODO IMPLEMENT ME\n" + "// this.tx.enqueue(decoding); // " + expectedType + "\n"
                    + "return this.reactive._" + method.getNameAsString() + "("
                    + method.getParameters().stream().map(NodeWithSimpleName::getNameAsString)
                            .collect(Collectors.joining(","))
                    + ")\n" + "    .invoke(this::queuedOrDiscard).replaceWithVoid();\n" + "\n}";
            declaration.setBody(new JavaParser().parseBlock(block).getResult().get());
        }

        try {
            File file = new File(dir, classname + ".java");
            LOGGER.infof("Generating transactional reactive implementation for %s into %s", reactiveTxAPI.getName(),
                    file.getAbsolutePath());
            Files.writeString(file.toPath(), cu.toString());
        } catch (IOException e) {
            LOGGER.errorf("Unable to write transactional reactive implementation %s", classname, e);
        }

    }

    private static MethodDeclaration findMethod(ClassOrInterfaceDeclaration reactiveAPI, MethodDeclaration method) {
        for (MethodDeclaration m : reactiveAPI.getMethods()) {
            if (m.getNameAsString().equals(method.getNameAsString())
                    && m.getParameters().equals(method.getParameters())) {
                return m;
            }
        }
        return null;
    }

    private static ClassOrInterfaceDeclaration generateAbstractSkeleton(CompilationUnit unit,
            ClassOrInterfaceDeclaration reactiveAPI, File dir, String prefix) {
        String classname = "Abstract" + reactiveAPI.getNameAsString().replace("Reactive", "");
        CompilationUnit cu = new CompilationUnit();
        cu.setPackageDeclaration("io.quarkus.redis.runtime.datasource");
        copyImports(unit, cu);
        cu.addImport(
                unit.getPackageDeclaration().orElseThrow().getNameAsString() + "." + reactiveAPI.getNameAsString());
        cu.addImport(Response.class);
        cu.addImport(Command.class);
        cu.addImport(unit.getPackageDeclaration().orElseThrow().getNameAsString(), false, true);
        cu.addImport("io.smallrye.mutiny.helpers.ParameterValidation.nonNull", true, false);

        ClassOrInterfaceDeclaration clazz = cu.addClass(classname).addExtendedType(AbstractRedisCommands.class)
                .setTypeParameters(reactiveAPI.getTypeParameters());

        // Constructor
        ConstructorDeclaration cst = clazz.addConstructor().addParameter(RedisCommandExecutor.class, "redis");
        for (TypeParameter tp : reactiveAPI.getTypeParameters()) {
            cst.addParameter("Class<" + tp.getNameAsString() + ">", tp.getNameAsString().toLowerCase());
        }
        String statement = "super(redis, new Marshaller(" + reactiveAPI.getTypeParameters().stream()
                .map(tp -> tp.getNameAsString().toLowerCase()).collect(Collectors.joining(",")) + "));";
        cst.getBody().addStatement(statement);

        for (MethodDeclaration method : reactiveAPI.getMethods()) {
            MethodDeclaration declaration = clazz.addMethod("_" + method.getNameAsString())
                    .setParameters(method.getParameters()).setType(getUniOfResponse());

            StringBuilder block = new StringBuilder("{");
            block.append("// Validation\n");
            boolean keyAsFirstParam = !method.getParameters().isEmpty()
                    && method.getParameter(0).getNameAsString().equalsIgnoreCase("key");
            if (keyAsFirstParam) {
                block.append("nonNull(key, \"key\");\n");
            }
            block.append("// Create command\n");
            String cmd = method.getNameAsString().toUpperCase();
            if (cmd.startsWith(prefix.toUpperCase())) {
                cmd = prefix.toUpperCase() + "_" + cmd.substring(prefix.length());
            }
            block.append("RedisCommand cmd = RedisCommand.of(Command.").append(cmd).append(");\n");
            if (keyAsFirstParam) {
                block.append("//        .put(marshaller.encode(key));\n");
            }
            block.append("return execute(cmd);\n");
            block.append("\n}");

            ParseResult<BlockStmt> result = new JavaParser().parseBlock(block.toString());
            declaration.setBody(result.getResult().get());
        }

        try {
            File file = new File(dir, classname + ".java");
            LOGGER.infof("Generating abstract implementation for %s into %s", reactiveAPI.getName(),
                    file.getAbsolutePath());
            Files.writeString(file.toPath(), cu.toString());
        } catch (IOException e) {
            LOGGER.errorf("Unable to write abstract implementation %s", classname, e);
        }

        return clazz;

    }

    private static void generateBlockingImplementation(CompilationUnit unit, ClassOrInterfaceDeclaration reactiveAPI,
            ClassOrInterfaceDeclaration blockingApi, File dir) {
        String classname = "Blocking" + blockingApi.getNameAsString() + "Impl";
        CompilationUnit cu = new CompilationUnit();
        cu.setPackageDeclaration("io.quarkus.redis.runtime.datasource");
        copyImportsExceptUni(unit, cu);
        cu.addImport(unit.getPackageDeclaration().orElseThrow().getNameAsString(), false, true);

        String itf = blockingApi.getNameAsString();
        String rx = reactiveAPI.getNameAsString();
        if (!blockingApi.getTypeParameters().isEmpty()) {
            itf = itf + "<" + blockingApi.getTypeParameters().stream().map(NodeWithSimpleName::getNameAsString)
                    .collect(Collectors.joining(",")) + ">";
            rx = rx + "<" + blockingApi.getTypeParameters().stream().map(NodeWithSimpleName::getNameAsString)
                    .collect(Collectors.joining(",")) + ">";
        }

        ClassOrInterfaceDeclaration impl = cu.addClass(classname).addExtendedType(AbstractRedisCommandGroup.class)
                .addImplementedType(itf).setTypeParameters(reactiveAPI.getTypeParameters()).setPublic(true);

        // reactive field
        impl.addField(rx, "reactive", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

        // constructor
        ConstructorDeclaration cst = impl.addConstructor(Modifier.Keyword.PUBLIC)
                .addParameter(RedisDataSource.class, "ds").addParameter(rx, "reactive")
                .addParameter(Duration.class, "timeout");
        cst.getBody().addStatement("super(ds, timeout);").addStatement("this.reactive = reactive;");

        // For each method, implement
        for (MethodDeclaration method : blockingApi.getMethods()) {
            MethodDeclaration declaration = impl.addMethod(method.getNameAsString());
            declaration.setModifiers(method.getModifiers());
            declaration.setPublic(true);
            declaration.setParameters(method.getParameters());
            declaration.setDefault(method.isDefault());
            declaration.addMarkerAnnotation(Override.class);
            declaration.setType(method.getType());
            BlockStmt body = getBlockingImplBody(method);
            declaration.setBody(body);
        }

        try {
            File file = new File(dir, classname + ".java");
            LOGGER.infof("Generating blocking implementation for %s into %s", reactiveAPI.getName(),
                    file.getAbsolutePath());
            Files.writeString(file.toPath(), cu.toString());
        } catch (IOException e) {
            LOGGER.errorf("Unable to write blocking implementation %s", classname, e);
        }

    }

    private static void generateBlockingTransactionalImplementation(CompilationUnit unit,
            ClassOrInterfaceDeclaration reactiveTxAPI, ClassOrInterfaceDeclaration txblockingApi, File dir) {
        String classname = "Blocking" + txblockingApi.getNameAsString() + "Impl";
        CompilationUnit cu = new CompilationUnit();
        cu.setPackageDeclaration("io.quarkus.redis.runtime.datasource");
        copyImportsExceptUni(unit, cu);
        cu.addImport(unit.getPackageDeclaration().orElseThrow().getNameAsString(), false, true);

        String itf = txblockingApi.getNameAsString();
        String rx = reactiveTxAPI.getNameAsString();
        if (!txblockingApi.getTypeParameters().isEmpty()) {
            itf += "<";
            itf += txblockingApi.getTypeParameters().stream().map(NodeWithSimpleName::getNameAsString)
                    .collect(Collectors.joining(","));
            itf += ">";
            rx = rx + "<" + txblockingApi.getTypeParameters().stream().map(NodeWithSimpleName::getNameAsString)
                    .collect(Collectors.joining(",")) + ">";
        }

        ClassOrInterfaceDeclaration impl = cu.addClass(classname)
                .addExtendedType(AbstractTransactionalRedisCommandGroup.class).addImplementedType(itf)
                .setTypeParameters(reactiveTxAPI.getTypeParameters()).setPublic(true);

        // reactive field
        impl.addField(rx, "reactive", Modifier.Keyword.PRIVATE, Modifier.Keyword.FINAL);

        // constructor
        ConstructorDeclaration cst = impl.addConstructor(Modifier.Keyword.PUBLIC)
                .addParameter(TransactionalRedisDataSource.class, "ds").addParameter(rx, "reactive")
                .addParameter(Duration.class, "timeout");
        cst.getBody().addStatement("super(ds, timeout);").addStatement("this.reactive = reactive;");

        // For each method, implement
        for (MethodDeclaration method : txblockingApi.getMethods()) {
            MethodDeclaration declaration = impl.addMethod(method.getNameAsString());
            declaration.setModifiers(method.getModifiers());
            declaration.setPublic(true);
            declaration.setParameters(method.getParameters());
            declaration.setDefault(method.isDefault());
            declaration.addMarkerAnnotation(Override.class);
            declaration.setType(new VoidType());
            BlockStmt body = getBlockingImplBody(method);
            declaration.setBody(body);
        }

        try {
            File file = new File(dir, classname + ".java");
            LOGGER.infof("Generating transactional blocking implementation for %s into %s", reactiveTxAPI.getName(),
                    file.getAbsolutePath());
            Files.writeString(file.toPath(), cu.toString());
        } catch (IOException e) {
            LOGGER.errorf("Unable to write blocking implementation %s", classname, e);
        }
    }

    private static BlockStmt getBlockingImplBody(MethodDeclaration method) {
        JavaParser parser = new JavaParser();
        String parameters = method.getParameters().stream().map(NodeWithSimpleName::getNameAsString)
                .collect(Collectors.joining(", "));
        if (method.getType().isVoidType()) {
            String statement = "{ reactive." + method.getNameAsString() + "(" + parameters
                    + ")\n        .await().atMost(timeout); }";
            ParseResult<BlockStmt> block = parser.parseBlock(statement);
            return block.getResult().get();
        } else {
            String statement = "{ return reactive." + method.getNameAsString() + "(" + parameters
                    + ")\n        .await().atMost(timeout); }";
            return parser.parseBlock(statement).getResult().get();
        }
    }

    private static ClassOrInterfaceDeclaration generateBlockingTransactionalInterface(CompilationUnit unit,
            ClassOrInterfaceDeclaration api, File dir) {
        String blockingTxApiName = api.getNameAsString().replace("Reactive", "Transactional");
        CompilationUnit cu = new CompilationUnit();
        setPackage(unit, cu);
        copyImportsExceptUni(unit, cu);

        ClassOrInterfaceDeclaration blockingTxApi = cu.addInterface(blockingTxApiName)
                .addExtendedType(TransactionalRedisCommands.class).setTypeParameters(api.getTypeParameters())
                .setPublic(true);
        copyClassJavadocAndAppendContent(api, blockingTxApi,
                "This API is intended to be used in a Redis transaction ({@code MULTI}), thus, all command methods return {@code void}.");

        // For each method, produce the "transactional blocking" variant - always return void
        for (MethodDeclaration method : api.getMethods()) {
            MethodDeclaration declaration = blockingTxApi.addMethod(method.getNameAsString());
            declaration.setModifiers(method.getModifiers());
            declaration.setParameters(method.getParameters());
            declaration.setDefault(method.isDefault());
            declaration.removeBody();
            declaration.setType(new VoidType());

            // Edit javadoc to remove the @return tag is any.
            JavadocComment comment = method.getJavadocComment().orElseThrow();
            Javadoc javadoc = comment.parse();
            Javadoc copy = new Javadoc(javadoc.getDescription());
            for (JavadocBlockTag tag : javadoc.getBlockTags()) {
                if (tag.getType() != JavadocBlockTag.Type.RETURN) {
                    copy.addBlockTag(tag);
                }
            }
            declaration.setJavadocComment(copy);
        }

        try {
            File file = new File(dir, blockingTxApiName + ".java");
            LOGGER.infof("Generating transactional blocking API for %s into %s", api.getName(), file.getAbsolutePath());
            Files.writeString(file.toPath(), cu.toString());
        } catch (IOException e) {
            LOGGER.errorf("Unable to write transactional blocking API %s", blockingTxApiName, e);
        }

        return blockingTxApi;
    }

    private static ClassOrInterfaceDeclaration generateBlockingReactiveTransactionalInterface(CompilationUnit unit,
            ClassOrInterfaceDeclaration api, File dir) {
        String name = api.getNameAsString().replace("Reactive", "ReactiveTransactional");
        CompilationUnit cu = new CompilationUnit();
        setPackage(unit, cu);
        copyImports(unit, cu);

        ClassOrInterfaceDeclaration gen = cu.addInterface(name).setTypeParameters(api.getTypeParameters())
                .addExtendedType(ReactiveTransactionalRedisCommands.class).setPublic(true);
        copyClassJavadocAndAppendContent(api, gen,
                "This API is intended to be used in a Redis transaction ({@code MULTI}), thus, all command methods return {@code Uni<Void>}.");

        // For each method, produce the "transactional blocking" variant - always return Uni<Void>
        for (MethodDeclaration method : api.getMethods()) {
            MethodDeclaration declaration = gen.addMethod(method.getNameAsString());
            declaration.setModifiers(method.getModifiers());
            declaration.setParameters(method.getParameters());
            declaration.setDefault(method.isDefault());
            declaration.removeBody();
            declaration.setType(ClassGeneratorHelper.getUniOfVoid());

            // Edit javadoc to remove the @return tag is any.
            JavadocComment comment = method.getJavadocComment().orElseThrow();
            Javadoc javadoc = comment.parse();
            Javadoc copy = new Javadoc(javadoc.getDescription());
            for (JavadocBlockTag tag : javadoc.getBlockTags()) {
                if (tag.getType() != JavadocBlockTag.Type.RETURN) {
                    copy.addBlockTag(tag);
                } else {
                    copy.addBlockTag(new JavadocBlockTag(JavadocBlockTag.Type.RETURN,
                            " A {@code Uni} emitting {@code null} when the command has been enqueued"
                                    + " successfully in the transaction, a failure otherwise. In the case of failure, the transaction is discarded."));
                }
            }
            declaration.setJavadocComment(copy);
        }

        try {
            File file = new File(dir, name + ".java");
            LOGGER.infof("Generating reactive transactional API for %s into %s", api.getName(), file.getAbsolutePath());
            Files.writeString(file.toPath(), cu.toString());
        } catch (IOException e) {
            LOGGER.errorf("Unable to write reactive transactional API %s", name, e);
        }

        return gen;
    }

    private static ClassOrInterfaceDeclaration generateBlockingInterface(CompilationUnit unit,
            ClassOrInterfaceDeclaration api, File dir) {
        String blockingApiName = api.getNameAsString().replace("Reactive", "");
        CompilationUnit cu = new CompilationUnit();
        setPackage(unit, cu);
        copyImportsExceptUni(unit, cu);
        ClassOrInterfaceDeclaration blockingApi = cu.addInterface(blockingApiName).addExtendedType(RedisCommands.class)
                .setJavadocComment(api.getJavadocComment().orElseThrow()).setTypeParameters(api.getTypeParameters())
                .setPublic(true);

        // For each method, produce the "blocking" variant
        for (MethodDeclaration method : api.getMethods()) {
            MethodDeclaration declaration = blockingApi.addMethod(method.getNameAsString());
            declaration.setModifiers(method.getModifiers());
            declaration.setParameters(method.getParameters());
            declaration.setDefault(method.isDefault());
            declaration.removeBody();

            boolean isVoid = isReturningUniOfVoid(method.getType());
            declaration.setType(getBlockingType(method.getType()));

            // Copy javadoc and update the return tag
            JavadocComment comment = method.getJavadocComment().orElseThrow();
            Javadoc javadoc = comment.parse();
            Javadoc copy = new Javadoc(javadoc.getDescription());
            for (JavadocBlockTag tag : javadoc.getBlockTags()) {
                if (tag.getType() == JavadocBlockTag.Type.RETURN) {
                    if (!isVoid) {
                        String text = tag.getContent().toText();
                        JavadocBlockTag newReturn = new JavadocBlockTag(JavadocBlockTag.Type.RETURN,
                                text.replace("a uni producing ", " ").trim());
                        copy.addBlockTag(newReturn);
                    }
                } else {
                    copy.addBlockTag(tag);
                }
            }
            declaration.setJavadocComment(copy);
        }

        try {
            File file = new File(dir, blockingApiName + ".java");
            LOGGER.infof("Generating blocking API for %s into %s", api.getName(), file.getAbsolutePath());
            Files.writeString(file.toPath(), cu.toString());
        } catch (IOException e) {
            LOGGER.errorf("Unable to write blocking API %s", blockingApiName, e);
        }

        return blockingApi;

    }

    public static File toFile(String classname) {
        File file = new File("extensions/redis-client/runtime/src/main/java/" + classname.replace(".", "/") + ".java");
        RedisApiGenerator.LOGGER.infof("Reading %s", file.getAbsolutePath());
        return file;
    }

}
