package io.quarkus.core.actionmaker;

import static io.quarkus.core.actionmaker.Util.*;
import static io.smallrye.jdeparser.Expr.$v;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.smallrye.jdeparser.Expr;
import io.smallrye.jdeparser.JDeparser;
import io.smallrye.jdeparser.SourceVersion;
import io.smallrye.jdeparser.Sources;
import io.smallrye.jdeparser.Type;
import io.smallrye.jdeparser.Var;
import io.smallrye.jdeparser.creator.ClassCreator;
import io.smallrye.jdeparser.creator.InterfaceCreator;
import io.smallrye.jdeparser.creator.SourceFileCreator;
import io.smallrye.jdeparser.format.Filer;
import io.smallrye.jdeparser.format.FormatPreferences;

/**
 * Code generator that produces the action and service builder interfaces
 * for the Quarkus service mechanism.
 * <p>
 * Generates:
 * <ul>
 * <li>{@code Action0} through {@code Action10} - functional interfaces for typed service actions</li>
 * <li>{@code VoidAction0} through {@code VoidAction10} - functional interfaces for void service actions</li>
 * <li>{@code ServiceBuilder0} through {@code ServiceBuilder9} - type-safe builder chain interfaces</li>
 * <li>{@code VoidServiceBuilder0} through {@code VoidServiceBuilder9} - builder chain interfaces for void services</li>
 * </ul>
 */
public final class MakeServiceTypes {

    /**
     * Maximum number of action parameters (StartContext not counted).
     */
    private static final int MAX_PCNT = 10;

    /**
     * Maximum number of dependency parameters for service builders.
     * This is one less than {@code MAX_PCNT} because the start context
     * always occupies the first action parameter slot.
     */
    private static final int MAX_DEPS = MAX_PCNT - 1;

    private static final String PKG = "io.quarkus.core.deployment.action";
    private static final String IMPL_PKG = "io.quarkus.core.deployment.action.impl";
    private static final String START_CONTEXT = "io.quarkus.core.StartContext";
    private static final String ASYNC_START_CONTEXT = "io.quarkus.core.AsyncStartContext";
    private static final String ASYNC_VOID_START_CONTEXT = "io.quarkus.core.AsyncVoidStartContext";

    MakeServiceTypes() {
    }

    /**
     * Entry point for the code generator.
     *
     * @param argsArray command line arguments; the first argument is the output directory
     * @throws IOException if writing the generated sources fails
     */
    public static void main(String[] argsArray) throws IOException {
        List<String> args = List.of(argsArray);
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Must provide destination directory as an argument");
        }
        Path outputDir = Path.of(args.get(0));
        Sources sources = JDeparser.createSources(Filer.newInstance(outputDir), FormatPreferences.defaults(),
                SourceVersion.JAVA_17);
        generateActionInterfaces(sources);
        generateVoidActionInterfaces(sources);
        generateServiceBuilderInterfaces(sources);
        generateServiceBuilderImpls(sources);
        generateVoidServiceBuilderInterfaces(sources);
        generateVoidServiceBuilderImpls(sources);
        System.out.println("Writing sources to " + outputDir);
        sources.writeSources();
    }

    // ── Action interfaces ──

    /**
     * Generate {@code Action0} through {@code Action10} functional interfaces.
     * Each extends {@link Serializable} to support lambda transliteration.
     *
     * @param sources the JDeparser sources to generate into
     */
    private static void generateActionInterfaces(Sources sources) {
        for (int i = 0; i <= MAX_PCNT; i++) {
            String className = "Action" + i;
            final int cnt = i;
            sources.createSourceFile(PKG, className, sf -> {
                sf.import_(Serializable.class);
                sf.interface_(className, cc -> {
                    cc.public_();
                    cc.docComment(dcc -> dcc.text("An action which takes " + cnt + " " + plural("argument", cnt) + "."));
                    cc.extends_(Type.of(Serializable.class));
                    Type tee = cc.typeParam("T", tpc -> {
                        tpc.docComment(dcc -> dcc.text("the return type of the action"));
                    });
                    for (int j = 0; j < cnt; j++) {
                        final int paramIdx = j + 1;
                        cc.typeParam("P" + paramIdx, tpc -> {
                            tpc.docComment(dcc -> dcc.text("the type of the " + ord(paramIdx) + " parameter"));
                        });
                    }
                    cc.method("run", mc -> {
                        mc.docComment(dcc -> {
                            dcc.text("Perform the action. If the action returns ");
                            dcc.code("null");
                            dcc.text(" then start will fail if there are any non-");
                            dcc.code("Optional");
                            dcc.text(" consumers.");
                            dcc.return_(rc -> {
                                rc.text("the result of the action, or ");
                                rc.code("null");
                                rc.text(" if there is no result");
                            });
                        });
                        mc.returning(tee);
                        for (int j = 0; j < cnt; j++) {
                            final int paramIdx = j + 1;
                            mc.param("p" + paramIdx, Type.named("P" + paramIdx), pc -> {
                                pc.docComment(dcc -> {
                                    dcc.text("the argument for the " + ord(paramIdx) + " parameter (not ");
                                    dcc.code("null");
                                    dcc.text(")");
                                });
                            });
                        }
                        mc.throws_(Type.of(Exception.class));
                    });
                });
            });
        }
    }

    /**
     * Generate {@code VoidAction0} through {@code VoidAction10} functional interfaces.
     * These are like the {@code Action} interfaces but with a {@code void} return type
     * and no {@code T} type parameter.
     *
     * @param sources the JDeparser sources to generate into
     */
    private static void generateVoidActionInterfaces(Sources sources) {
        for (int i = 0; i <= MAX_PCNT; i++) {
            String className = "VoidAction" + i;
            final int cnt = i;
            sources.createSourceFile(PKG, className, sf -> {
                sf.import_(Serializable.class);
                sf.interface_(className, cc -> {
                    cc.public_();
                    cc.docComment(dcc -> dcc
                            .text("A void action which takes " + cnt + " " + plural("argument", cnt) + "."));
                    cc.extends_(Type.of(Serializable.class));
                    for (int j = 0; j < cnt; j++) {
                        final int paramIdx = j + 1;
                        cc.typeParam("P" + paramIdx, tpc -> {
                            tpc.docComment(dcc -> dcc.text("the type of the " + ord(paramIdx) + " parameter"));
                        });
                    }
                    cc.method("run", mc -> {
                        mc.docComment(dcc -> dcc.text("Perform the action."));
                        mc.returning(Type.VOID);
                        for (int j = 0; j < cnt; j++) {
                            final int paramIdx = j + 1;
                            mc.param("p" + paramIdx, Type.named("P" + paramIdx), pc -> {
                                pc.docComment(dcc -> {
                                    dcc.text("the argument for the " + ord(paramIdx) + " parameter (not ");
                                    dcc.code("null");
                                    dcc.text(")");
                                });
                            });
                        }
                        mc.throws_(Type.of(Exception.class));
                    });
                });
            });
        }
    }

    // ── Typed service builder interfaces ──

    /**
     * Generate {@code ServiceBuilder0} through {@code ServiceBuilderN} type-safe builder chain interfaces.
     * <p>
     * Each {@code ServiceBuilderN} has:
     * <ul>
     * <li>{@code require(Class<?>)} and {@code require(Class<?>, String)} methods that return {@code ServiceBuilder(N+1)}
     * (except for {@code ServiceBuilder9} which has reached the maximum arity)</li>
     * <li>{@code after()} methods for ordering-only dependencies (5 overloads)</li>
     * <li>{@code action(Action(N+1))} - synchronous terminal method; the action's first parameter is always
     * a {@code StartContext}</li>
     * <li>{@code actionAsync(Action(N+1))} - asynchronous terminal method; the action's first parameter is always
     * an {@code AsyncStartContext<T>}</li>
     * </ul>
     *
     * @param sources the JDeparser sources to generate into
     */
    private static void generateServiceBuilderInterfaces(Sources sources) {
        generateServiceBuilderInterfacesParameterized(
                sources, "ServiceBuilder", START_CONTEXT);
    }

    /**
     * Parameterized generation of typed service builder interfaces.
     *
     * @param sources the JDeparser sources to generate into
     * @param classPrefix the class name prefix
     * @param contextFqn the fully-qualified context type name
     */
    private static void generateServiceBuilderInterfacesParameterized(
            Sources sources,
            String classPrefix,
            String contextFqn) {
        for (int i = 0; i <= MAX_DEPS; i++) {
            String className = classPrefix + i;
            final int depCount = i;
            // action arity is depCount + 1 (context occupies the first slot)
            final int actionArity = depCount + 1;
            sources.createSourceFile(PKG, className, sf -> {
                sf.import_(List.class);
                sf.import_(Type.named(contextFqn));
                sf.import_(Type.named(ASYNC_START_CONTEXT));
                sf.interface_(className, cc -> {
                    cc.public_();
                    cc.docComment(dcc -> {
                        dcc.text("A service builder with " + depCount + " " + plural("dependency", depCount)
                                + " declared.");
                        dcc.text(" Use {@code require} to add dependencies, {@code after} to add"
                                + " ordering-only dependencies, or {@code action}/"
                                + "{@code actionAsync} to terminate the builder chain.");
                    });
                    // T = service type
                    Type svcTypeParam = cc.typeParam("T", tpc -> {
                        tpc.docComment(dcc -> dcc.text("the service type"));
                    });
                    // P1..PN = dependency types
                    for (int j = 0; j < depCount; j++) {
                        final int paramIdx = j + 1;
                        cc.typeParam("P" + paramIdx, tpc -> {
                            tpc.docComment(dcc -> dcc.text("the type of the " + ord(paramIdx) + " dependency"));
                        });
                    }

                    // self type for after() return
                    Type[] selfTypeArgs = new Type[depCount + 1];
                    selfTypeArgs[0] = svcTypeParam;
                    for (int j = 0; j < depCount; j++) {
                        selfTypeArgs[j + 1] = Type.named("P" + (j + 1));
                    }
                    Type selfType = Type.named(className).typeArg(selfTypeArgs);

                    // require() methods - only if we haven't hit max deps
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount = depCount + 1;
                        String nextBuilder = classPrefix + nextDepCount;
                        // require(Class<PN1> type)
                        cc.method("require", mc -> {
                            mc.docComment(dcc -> {
                                dcc.text("Add a dependency on a service of the given type.");
                                dcc.return_(rc -> rc.text("a builder with the added dependency"));
                            });
                            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
                                tpc.docComment(dcc -> dcc.text("the type of the dependency to add"));
                            });
                            // return type: Builder(N+1)<T, P1, ..., PN, PN1>
                            Type[] nextTypeArgs = new Type[nextDepCount + 1];
                            nextTypeArgs[0] = svcTypeParam;
                            for (int j = 0; j < depCount; j++) {
                                nextTypeArgs[j + 1] = Type.named("P" + (j + 1));
                            }
                            nextTypeArgs[nextDepCount] = pn1;
                            mc.returning(Type.named(nextBuilder).typeArg(nextTypeArgs));
                            mc.param("type", Type.of(Class.class).typeArg(pn1), pc -> {
                                pc.docComment(dcc -> dcc.text("the dependency type (must not be {@code null})"));
                            });
                        });
                        // require(Class<PN1> type, String name)
                        cc.method("require", mc -> {
                            mc.docComment(dcc -> {
                                dcc.text("Add a dependency on a named service of the given type.");
                                dcc.return_(rc -> rc.text("a builder with the added dependency"));
                            });
                            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
                                tpc.docComment(dcc -> dcc.text("the type of the dependency to add"));
                            });
                            Type[] nextTypeArgs = new Type[nextDepCount + 1];
                            nextTypeArgs[0] = svcTypeParam;
                            for (int j = 0; j < depCount; j++) {
                                nextTypeArgs[j + 1] = Type.named("P" + (j + 1));
                            }
                            nextTypeArgs[nextDepCount] = pn1;
                            mc.returning(Type.named(nextBuilder).typeArg(nextTypeArgs));
                            mc.param("type", Type.of(Class.class).typeArg(pn1), pc -> {
                                pc.docComment(dcc -> dcc.text("the dependency type (must not be {@code null})"));
                            });
                            mc.param("name", Type.of(String.class), pc -> {
                                pc.docComment(dcc -> dcc.text("the dependency name (must not be {@code null})"));
                            });
                        });
                    }

                    // request() methods - optional dependencies (only if we haven't hit max deps)
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount = depCount + 1;
                        String nextBuilder = classPrefix + nextDepCount;
                        sf.import_(Optional.class);
                        generateRequestInterfaceMethods(cc, svcTypeParam, depCount, nextDepCount, nextBuilder, true);
                    }

                    // consumeAll() methods - multi-service dependencies (only if we haven't hit max deps)
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount = depCount + 1;
                        String nextBuilder = classPrefix + nextDepCount;
                        sf.import_(Map.class);
                        generateConsumeAllInterfaceMethod(cc, svcTypeParam, depCount, nextDepCount, nextBuilder, true);
                    }

                    // after() methods - ordering-only dependencies
                    generateAfterInterfaceMethods(cc, selfType);

                    // before() methods - reverse ordering dependencies
                    generateBeforeInterfaceMethods(cc, selfType);

                    // afterBuildItem() method
                    generateAfterBuildItemInterfaceMethod(cc, selfType);

                    // withPhase() - runtime phase assignment (only on Builder0)
                    if (depCount == 0) {
                        generateAtPhaseInterfaceMethod(cc, sf, selfType);
                    }

                    // action() - sync terminal
                    cc.method("action", mc -> {
                        mc.docComment(dcc -> {
                            dcc.text("Define the synchronous action for this service. ");
                            dcc.text("The action receives a {@code " + contextFqn.substring(contextFqn.lastIndexOf('.') + 1)
                                    + "} followed by all declared dependencies.");
                        });
                        mc.returning(Type.VOID);
                        Type[] actionTypeArgs = new Type[actionArity + 1];
                        actionTypeArgs[0] = svcTypeParam;
                        actionTypeArgs[1] = Type.named(contextFqn);
                        for (int j = 0; j < depCount; j++) {
                            actionTypeArgs[j + 2] = Type.named("P" + (j + 1));
                        }
                        mc.param("action", Type.named("Action" + actionArity).typeArg(actionTypeArgs), pc -> {
                            pc.docComment(dcc -> dcc.text("the service action (must not be {@code null})"));
                        });
                    });

                    // actionAsync() - async terminal (only for runtime builders)
                    cc.method("actionAsync", mc -> {
                        mc.docComment(dcc -> {
                            dcc.text("Define the asynchronous action for this service. ");
                            dcc.text(
                                    "The action receives an {@code AsyncStartContext} followed by all declared dependencies.");
                        });
                        mc.returning(Type.VOID);
                        Type[] actionTypeArgs = new Type[actionArity + 1];
                        actionTypeArgs[0] = Type.of(Void.class);
                        actionTypeArgs[1] = Type.named(ASYNC_START_CONTEXT).typeArg(svcTypeParam);
                        for (int j = 0; j < depCount; j++) {
                            actionTypeArgs[j + 2] = Type.named("P" + (j + 1));
                        }
                        mc.param("action", Type.named("Action" + actionArity).typeArg(actionTypeArgs), pc -> {
                            pc.docComment(dcc -> dcc.text("the service action (must not be {@code null})"));
                        });
                    });
                });
            });
        }
    }

    // ── Void service builder interfaces ──

    /**
     * Generate {@code VoidServiceBuilder0} through {@code VoidServiceBuilder9} builder chain interfaces
     * for void runtime services.
     *
     * @param sources the JDeparser sources to generate into
     */
    private static void generateVoidServiceBuilderInterfaces(Sources sources) {
        generateVoidServiceBuilderInterfacesParameterized(
                sources, "VoidServiceBuilder", START_CONTEXT, ASYNC_VOID_START_CONTEXT);
    }

    /**
     * Parameterized generation of void service builder interfaces.
     *
     * @param sources the JDeparser sources to generate into
     * @param classPrefix the class name prefix
     * @param contextFqn the fully-qualified sync context type name
     * @param asyncContextFqn the fully-qualified async context type name
     */
    private static void generateVoidServiceBuilderInterfacesParameterized(
            Sources sources,
            String classPrefix,
            String contextFqn,
            String asyncContextFqn) {
        for (int i = 0; i <= MAX_DEPS; i++) {
            String className = classPrefix + i;
            final int depCount = i;
            final int actionArity = depCount + 1;
            sources.createSourceFile(PKG, className, sf -> {
                sf.import_(List.class);
                sf.import_(Type.named(contextFqn));
                sf.import_(Type.named(asyncContextFqn));
                sf.interface_(className, cc -> {
                    cc.public_();
                    cc.docComment(dcc -> {
                        dcc.text("A void service builder with " + depCount + " " + plural("dependency", depCount)
                                + " declared.");
                        dcc.text(" Use {@code require} to add dependencies, {@code after} to add"
                                + " ordering-only dependencies, or {@code action}/"
                                + "{@code actionAsync} to terminate the builder chain.");
                    });
                    // P1..PN = dependency types (no T type param for void builders)
                    for (int j = 0; j < depCount; j++) {
                        final int paramIdx = j + 1;
                        cc.typeParam("P" + paramIdx, tpc -> {
                            tpc.docComment(dcc -> dcc.text("the type of the " + ord(paramIdx) + " dependency"));
                        });
                    }

                    // self type for after() return
                    Type selfType;
                    if (depCount == 0) {
                        selfType = Type.named(className);
                    } else {
                        Type[] selfTypeArgs = new Type[depCount];
                        for (int j = 0; j < depCount; j++) {
                            selfTypeArgs[j] = Type.named("P" + (j + 1));
                        }
                        selfType = Type.named(className).typeArg(selfTypeArgs);
                    }

                    // require() methods - only if we haven't hit max deps
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount = depCount + 1;
                        String nextBuilder = classPrefix + nextDepCount;
                        // require(Class<PN1> type)
                        cc.method("require", mc -> {
                            mc.docComment(dcc -> {
                                dcc.text("Add a dependency on a service of the given type.");
                                dcc.return_(rc -> rc.text("a builder with the added dependency"));
                            });
                            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
                                tpc.docComment(dcc -> dcc.text("the type of the dependency to add"));
                            });
                            Type[] nextTypeArgs = new Type[nextDepCount];
                            for (int j = 0; j < depCount; j++) {
                                nextTypeArgs[j] = Type.named("P" + (j + 1));
                            }
                            nextTypeArgs[depCount] = pn1;
                            mc.returning(Type.named(nextBuilder).typeArg(nextTypeArgs));
                            mc.param("type", Type.of(Class.class).typeArg(pn1), pc -> {
                                pc.docComment(dcc -> dcc.text("the dependency type (must not be {@code null})"));
                            });
                        });
                        // require(Class<PN1> type, String name)
                        cc.method("require", mc -> {
                            mc.docComment(dcc -> {
                                dcc.text("Add a dependency on a named service of the given type.");
                                dcc.return_(rc -> rc.text("a builder with the added dependency"));
                            });
                            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
                                tpc.docComment(dcc -> dcc.text("the type of the dependency to add"));
                            });
                            Type[] nextTypeArgs = new Type[nextDepCount];
                            for (int j = 0; j < depCount; j++) {
                                nextTypeArgs[j] = Type.named("P" + (j + 1));
                            }
                            nextTypeArgs[depCount] = pn1;
                            mc.returning(Type.named(nextBuilder).typeArg(nextTypeArgs));
                            mc.param("type", Type.of(Class.class).typeArg(pn1), pc -> {
                                pc.docComment(dcc -> dcc.text("the dependency type (must not be {@code null})"));
                            });
                            mc.param("name", Type.of(String.class), pc -> {
                                pc.docComment(dcc -> dcc.text("the dependency name (must not be {@code null})"));
                            });
                        });
                    }

                    // request() methods - optional dependencies (only if we haven't hit max deps)
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount2 = depCount + 1;
                        String nextBuilder2 = classPrefix + nextDepCount2;
                        sf.import_(Optional.class);
                        generateRequestInterfaceMethods(cc, null, depCount, nextDepCount2, nextBuilder2, false);
                    }

                    // consumeAll() methods - multi-service dependencies (only if we haven't hit max deps)
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount2 = depCount + 1;
                        String nextBuilder2 = classPrefix + nextDepCount2;
                        sf.import_(Map.class);
                        generateConsumeAllInterfaceMethod(cc, null, depCount, nextDepCount2, nextBuilder2, false);
                    }

                    // after() methods - ordering-only dependencies
                    generateAfterInterfaceMethods(cc, selfType);

                    // before() methods - reverse ordering dependencies
                    generateBeforeInterfaceMethods(cc, selfType);

                    // afterBuildItem() method
                    generateAfterBuildItemInterfaceMethod(cc, selfType);

                    // withPhase() - runtime phase assignment (only on Builder0)
                    if (depCount == 0) {
                        generateAtPhaseInterfaceMethod(cc, sf, selfType);
                    }

                    // action() - sync terminal using VoidAction
                    cc.method("action", mc -> {
                        mc.docComment(dcc -> {
                            dcc.text("Define the synchronous action for this void service. ");
                            dcc.text("The action receives a {@code " + contextFqn.substring(contextFqn.lastIndexOf('.') + 1)
                                    + "} followed by all declared dependencies.");
                        });
                        mc.returning(Type.VOID);
                        Type[] actionTypeArgs = new Type[actionArity];
                        actionTypeArgs[0] = Type.named(contextFqn);
                        for (int j = 0; j < depCount; j++) {
                            actionTypeArgs[j + 1] = Type.named("P" + (j + 1));
                        }
                        mc.param("action", Type.named("VoidAction" + actionArity).typeArg(actionTypeArgs), pc -> {
                            pc.docComment(dcc -> dcc.text("the service action (must not be {@code null})"));
                        });
                    });

                    // actionAsync() - async terminal
                    cc.method("actionAsync", mc -> {
                        mc.docComment(dcc -> {
                            dcc.text("Define the asynchronous action for this void service. ");
                            dcc.text("The action receives an {@code AsyncVoidStartContext}"
                                    + " followed by all declared dependencies.");
                        });
                        mc.returning(Type.VOID);
                        Type[] actionTypeArgs = new Type[actionArity];
                        actionTypeArgs[0] = Type.named(asyncContextFqn);
                        for (int j = 0; j < depCount; j++) {
                            actionTypeArgs[j + 1] = Type.named("P" + (j + 1));
                        }
                        mc.param("action", Type.named("VoidAction" + actionArity).typeArg(actionTypeArgs), pc -> {
                            pc.docComment(dcc -> dcc.text("the service action (must not be {@code null})"));
                        });
                    });
                });
            });
        }
    }

    /**
     * Generate the five {@code after()} method declarations on a builder interface.
     * These are ordering-only dependency methods that return the same builder type.
     *
     * @param cc the class/interface context to add methods to
     * @param selfType the self-referencing return type of the builder
     */
    private static void generateAfterInterfaceMethods(
            InterfaceCreator cc,
            Type selfType) {
        // after(String name)
        cc.method("after", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Add an ordering-only dependency on a void service with the given name. ");
                dcc.text("The service must complete before this service starts, but its value is not injected.");
                dcc.return_(rc -> rc.text("a builder with the added dependency"));
            });
            mc.returning(selfType);
            mc.param("name", Type.of(String.class), pc -> {
                pc.docComment(dcc -> dcc.text("the void service name (must not be {@code null})"));
            });
        });
        // after(List<String> nameParts)
        cc.method("after", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Add an ordering-only dependency on a void service with the given multi-part name.");
                dcc.return_(rc -> rc.text("a builder with the added dependency"));
            });
            mc.returning(selfType);
            mc.param("nameParts", Type.of(List.class).typeArg(Type.of(String.class)), pc -> {
                pc.docComment(dcc -> dcc.text("the void service name parts (must not be {@code null})"));
            });
        });
        // after(Class<?> type)
        cc.method("after", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Add an ordering-only dependency on an unnamed typed service.");
                dcc.return_(rc -> rc.text("a builder with the added dependency"));
            });
            mc.returning(selfType);
            mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD), pc -> {
                pc.docComment(dcc -> dcc.text("the service type (must not be {@code null})"));
            });
        });
        // after(Class<?> type, String name)
        cc.method("after", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Add an ordering-only dependency on a named typed service.");
                dcc.return_(rc -> rc.text("a builder with the added dependency"));
            });
            mc.returning(selfType);
            mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD), pc -> {
                pc.docComment(dcc -> dcc.text("the service type (must not be {@code null})"));
            });
            mc.param("name", Type.of(String.class), pc -> {
                pc.docComment(dcc -> dcc.text("the service name (must not be {@code null})"));
            });
        });
        // after(Class<?> type, List<String> nameParts)
        cc.method("after", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Add an ordering-only dependency on a typed service with the given multi-part name.");
                dcc.return_(rc -> rc.text("a builder with the added dependency"));
            });
            mc.returning(selfType);
            mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD), pc -> {
                pc.docComment(dcc -> dcc.text("the service type (must not be {@code null})"));
            });
            mc.param("nameParts", Type.of(List.class).typeArg(Type.of(String.class)), pc -> {
                pc.docComment(dcc -> dcc.text("the service name parts (must not be {@code null})"));
            });
        });
    }

    /**
     * Generate the four {@code before()} method declarations on a builder interface.
     * These are reverse ordering methods: declaring {@code before(X)} means X depends
     * on this service — this service starts before X and stops after X.
     *
     * @param cc the class/interface context to add methods to
     * @param selfType the self-referencing return type of the builder
     */
    private static void generateBeforeInterfaceMethods(
            InterfaceCreator cc,
            Type selfType) {
        // before(String name)
        cc.method("before", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Declare that the given void service should start after this service and stop before it. ");
                dcc.text("If the target does not exist, the declaration is silently ignored.");
                dcc.return_(rc -> rc.text("this builder"));
            });
            mc.returning(selfType);
            mc.param("name", Type.of(String.class), pc -> {
                pc.docComment(dcc -> dcc.text("the void service name (must not be {@code null})"));
            });
        });
        // before(Class<?> type)
        cc.method("before", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Declare that the given unnamed typed service should start after this service and stop before it. ");
                dcc.text("If the target does not exist, the declaration is silently ignored.");
                dcc.return_(rc -> rc.text("this builder"));
            });
            mc.returning(selfType);
            mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD), pc -> {
                pc.docComment(dcc -> dcc.text("the service type (must not be {@code null})"));
            });
        });
        // before(Class<?> type, String name)
        cc.method("before", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Declare that the given named typed service should start after this service and stop before it. ");
                dcc.text("If the target does not exist, the declaration is silently ignored.");
                dcc.return_(rc -> rc.text("this builder"));
            });
            mc.returning(selfType);
            mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD), pc -> {
                pc.docComment(dcc -> dcc.text("the service type (must not be {@code null})"));
            });
            mc.param("name", Type.of(String.class), pc -> {
                pc.docComment(dcc -> dcc.text("the service name (must not be {@code null})"));
            });
        });
        // before(Class<?> type, List<String> nameParts)
        cc.method("before", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Declare that the given typed service should start after this service and stop before it. ");
                dcc.text("If the target does not exist, the declaration is silently ignored.");
                dcc.return_(rc -> rc.text("this builder"));
            });
            mc.returning(selfType);
            mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD), pc -> {
                pc.docComment(dcc -> dcc.text("the service type (must not be {@code null})"));
            });
            mc.param("nameParts", Type.of(List.class).typeArg(Type.of(String.class)), pc -> {
                pc.docComment(dcc -> dcc.text("the service name parts (must not be {@code null})"));
            });
        });
    }

    /**
     * Generate the {@code afterBuildItem(Class)} method declaration on a builder interface.
     *
     * @param cc the interface context
     * @param selfType the self-referencing return type
     */
    private static void generateAfterBuildItemInterfaceMethod(
            InterfaceCreator cc,
            Type selfType) {
        cc.method("afterBuildItem", mc -> {
            mc.annotate(Type.of(Deprecated.class));
            mc.docComment(dcc -> {
                dcc.text("Declare that this service must start after the build step that produces ");
                dcc.text("the given build item has completed. ");
                dcc.text("This is a transitional API for services that coexist with legacy ");
                dcc.text("bytecode recorders. Once the producing recorder is converted to a ");
                dcc.text("service, replace with a direct service dependency via ");
                dcc.code("require()");
                dcc.text(", ");
                dcc.code("after()");
                dcc.text(", or ");
                dcc.code("before()");
                dcc.text(". ");
                dcc.text("If no step produces the item, the dependency is silently ignored.");
                dcc.return_(rc -> rc.text("this builder"));
                dcc.deprecated("Transitional API for recorder coexistence.");
            });
            mc.returning(selfType);
            mc.param("buildItemClass", Type.of(Class.class).typeArg(
                    Type.named("io.quarkus.builder.item.BuildItem").wildcardExtends()), pc -> {
                        pc.docComment(dcc -> dcc.text("the build item class (must not be {@code null})"));
                    });
        });
    }

    /**
     * Generate the {@code afterBuildItem(Class)} method implementation on a builder impl class.
     *
     * @param cc the class context
     * @param selfType the self-referencing return type
     */
    private static void generateAfterBuildItemImplMethod(
            ClassCreator cc,
            Type selfType) {
        cc.method("afterBuildItem", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            mc.returning(selfType);
            Var buildItemClass = mc.param("buildItemClass", Type.of(Class.class).typeArg(
                    Type.named("io.quarkus.builder.item.BuildItem").wildcardExtends()));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("afterBuildItem", buildItemClass));
                b0.return_(Expr.THIS);
            });
        });
    }

    /**
     * Generate the {@code atPhase(Phase)} method declaration on a builder interface.
     * This method assigns the service to a runtime phase and returns the same builder type.
     *
     * @param cc the interface context
     * @param sf the source file context (for imports)
     * @param selfType the self-referencing return type
     */
    private static void generateAtPhaseInterfaceMethod(
            InterfaceCreator cc,
            SourceFileCreator sf,
            Type selfType) {
        Type phaseType = Type.named("io.quarkus.deployment.Phase");
        sf.import_(phaseType);
        cc.method("atPhase", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Assign this service to a runtime phase. ");
                dcc.text("The default phase is {@code Phase.APPLICATION}.");
                dcc.return_(rc -> rc.text("this builder"));
            });
            mc.returning(selfType);
            mc.param("phase", phaseType, pc -> {
                pc.docComment(dcc -> dcc.text("the runtime phase (must not be {@code null})"));
            });
        });
    }

    /**
     * Generate the {@code atPhase(Phase)} method implementation on a builder impl class.
     * Delegates to {@code sbi.atPhase()} and returns {@code this}.
     *
     * @param cc the class context
     * @param sf the source file context (for imports)
     * @param selfType the self-referencing return type
     */
    private static void generateAtPhaseImplMethod(
            ClassCreator cc,
            SourceFileCreator sf,
            Type selfType) {
        Type phaseType = Type.named("io.quarkus.deployment.Phase");
        sf.import_(phaseType);
        cc.method("atPhase", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            mc.returning(selfType);
            Var phase = mc.param("phase", phaseType);
            mc.body(b0 -> {
                b0.emit($v("sbi").call("atPhase", phase));
                b0.return_(Expr.THIS);
            });
        });
    }

    // Turn off the formatter because it doesn't understand how HTML relates to @code
    //@formatter:off
    /**
     * Generate the two {@code request()} method declarations on a builder interface.
     * These are optional dependency methods that step to the next arity with {@code Optional<P>}
     * as the type parameter.
     *
     * @param cc the interface context
     * @param svcTypeParam the service type parameter ({@code T}), or {@code null} for void builders
     * @param depCount the current dependency count
     * @param nextDepCount the next dependency count (depCount + 1)
     * @param nextBuilder the next builder class name
     * @param hasServiceTypeParam whether the builder has a {@code T} type parameter
     */
    //@formatter:on
    private static void generateRequestInterfaceMethods(
            InterfaceCreator cc,
            Type svcTypeParam,
            int depCount,
            int nextDepCount,
            String nextBuilder,
            boolean hasServiceTypeParam) {
        // request(Class<PN1> type)
        cc.method("request", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Add an optional dependency on a service of the given type. ");
                dcc.text("The dependency is injected as {@code Optional<T>}; if absent, an empty optional is provided.");
                dcc.return_(rc -> rc.text("a builder with the added dependency"));
            });
            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
                tpc.docComment(dcc -> dcc.text("the type of the dependency to add"));
            });
            Type optionalPn1 = Type.of(Optional.class).typeArg(pn1);
            Type[] nextTypeArgs;
            if (hasServiceTypeParam) {
                nextTypeArgs = new Type[nextDepCount + 1];
                nextTypeArgs[0] = svcTypeParam;
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j + 1] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[nextDepCount] = optionalPn1;
            } else {
                nextTypeArgs = new Type[nextDepCount];
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[depCount] = optionalPn1;
            }
            mc.returning(Type.named(nextBuilder).typeArg(nextTypeArgs));
            mc.param("type", Type.of(Class.class).typeArg(pn1), pc -> {
                pc.docComment(dcc -> dcc.text("the dependency type (must not be {@code null})"));
            });
        });
        // request(Class<PN1> type, String name)
        cc.method("request", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Add an optional dependency on a named service of the given type. ");
                dcc.text("The dependency is injected as {@code Optional<T>}; if absent, an empty optional is provided.");
                dcc.return_(rc -> rc.text("a builder with the added dependency"));
            });
            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
                tpc.docComment(dcc -> dcc.text("the type of the dependency to add"));
            });
            Type optionalPn1 = Type.of(Optional.class).typeArg(pn1);
            Type[] nextTypeArgs;
            if (hasServiceTypeParam) {
                nextTypeArgs = new Type[nextDepCount + 1];
                nextTypeArgs[0] = svcTypeParam;
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j + 1] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[nextDepCount] = optionalPn1;
            } else {
                nextTypeArgs = new Type[nextDepCount];
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[depCount] = optionalPn1;
            }
            mc.returning(Type.named(nextBuilder).typeArg(nextTypeArgs));
            mc.param("type", Type.of(Class.class).typeArg(pn1), pc -> {
                pc.docComment(dcc -> dcc.text("the dependency type (must not be {@code null})"));
            });
            mc.param("name", Type.of(String.class), pc -> {
                pc.docComment(dcc -> dcc.text("the dependency name (must not be {@code null})"));
            });
        });
    }

    // ── Typed service builder impls ──

    /**
     * Generate runtime {@code ServiceBuilderImpl0} through {@code ServiceBuilderImpl9} wrapper classes.
     *
     * @param sources the JDeparser sources to generate into
     */
    private static void generateServiceBuilderImpls(Sources sources) {
        generateServiceBuilderImplsParameterized(
                sources, "ServiceBuilderImpl", "ServiceBuilder", START_CONTEXT);
    }

    /**
     * Parameterized generation of typed service builder implementation classes.
     *
     * @param sources the JDeparser sources to generate into
     * @param implPrefix the implementation class name prefix
     * @param interfacePrefix the interface class name prefix
     * @param contextFqn the fully-qualified context type name
     */
    private static void generateServiceBuilderImplsParameterized(
            Sources sources,
            String implPrefix,
            String interfacePrefix,
            String contextFqn) {
        for (int i = 0; i <= MAX_DEPS; i++) {
            String className = implPrefix + i;
            String interfaceFqn = PKG + "." + interfacePrefix + i;
            final int depCount = i;
            // action arity is depCount + 1 (context occupies the first slot)
            final int actionArity = depCount + 1;
            sources.createSourceFile(IMPL_PKG, className, sf -> {
                sf.class_(className, cc -> {
                    cc.public_();
                    cc.final_();
                    Type sbiType = Type.named("io.quarkus.core.deployment.action.impl.ServiceBuilderImpl");
                    sf.import_(sbiType);
                    cc.field("sbi", fc -> {
                        fc.type(sbiType);
                        fc.private_();
                        fc.final_();
                    });
                    List<Type> interfaceTypeArgs = new ArrayList<>(depCount + 1);
                    // T = service type
                    Type svcTypeParam = cc.typeParam("T", tpc -> {
                    });
                    interfaceTypeArgs.add(Type.named("T"));
                    // P1..PN = dependency types
                    for (int j = 0; j < depCount; j++) {
                        String tpName = "P" + (j + 1);
                        cc.typeParam(tpName, tpc -> {
                        });
                        interfaceTypeArgs.add(Type.named(tpName));
                    }
                    Type interfaceRawType = Type.named(interfaceFqn);
                    cc.implements_(interfaceRawType.typeArg(interfaceTypeArgs));
                    sf.import_(interfaceRawType);

                    // self type for after() return
                    Type[] selfTypeArgs = new Type[depCount + 1];
                    selfTypeArgs[0] = svcTypeParam;
                    for (int j = 0; j < depCount; j++) {
                        selfTypeArgs[j + 1] = Type.named("P" + (j + 1));
                    }
                    Type selfType = Type.named(className).typeArg(selfTypeArgs);

                    // constructor
                    cc.constructor(mc -> {
                        mc.public_();
                        Var sbi = mc.param("sbi", sbiType);
                        mc.body(b0 -> b0.emit(Expr.THIS.field("sbi").assign(sbi)));
                    });

                    // require() method impls - only if we haven't hit max deps
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount = depCount + 1;
                        String nextBuilder = implPrefix + nextDepCount;
                        // require(Class<PN1> type)
                        cc.method("require", mc -> {
                            mc.public_();
                            mc.annotate(Type.of(Override.class));
                            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
                            });
                            // return type: Impl(N+1)<T, P1, ..., PN, PN1>
                            Type[] nextTypeArgs = new Type[nextDepCount + 1];
                            nextTypeArgs[0] = svcTypeParam;
                            for (int j = 0; j < depCount; j++) {
                                nextTypeArgs[j + 1] = Type.named("P" + (j + 1));
                            }
                            nextTypeArgs[nextDepCount] = pn1;
                            Type nextType = Type.named(nextBuilder).typeArg(nextTypeArgs);
                            mc.returning(nextType);
                            Var type = mc.param("type", Type.of(Class.class).typeArg(pn1));
                            mc.body(b0 -> {
                                b0.emit($v("sbi").call("require", type));
                                b0.return_(nextType.new_($v("sbi")));
                            });
                        });
                        // require(Class<PN1> type, String name)
                        cc.method("require", mc -> {
                            mc.public_();
                            mc.annotate(Type.of(Override.class));
                            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
                            });
                            Type[] nextTypeArgs = new Type[nextDepCount + 1];
                            // return type: Impl(N+1)<T, P1, ..., PN, PN1>
                            nextTypeArgs[0] = svcTypeParam;
                            for (int j = 0; j < depCount; j++) {
                                nextTypeArgs[j + 1] = Type.named("P" + (j + 1));
                            }
                            nextTypeArgs[nextDepCount] = pn1;
                            Type nextType = Type.named(nextBuilder).typeArg(nextTypeArgs);
                            mc.returning(nextType);
                            Var type = mc.param("type", Type.of(Class.class).typeArg(pn1));
                            Var name = mc.param("name", Type.of(String.class));
                            mc.body(b0 -> {
                                b0.emit($v("sbi").call("require", type, name));
                                b0.return_(nextType.new_($v("sbi")));
                            });
                        });
                    }

                    // request() method impls - optional dependencies (only if we haven't hit max deps)
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount = depCount + 1;
                        String nextBuilder = implPrefix + nextDepCount;
                        generateRequestImplMethods(cc, sf, svcTypeParam, depCount, nextDepCount, nextBuilder, true);
                    }

                    // consumeAll() method impls (only if we haven't hit max deps)
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount = depCount + 1;
                        String nextBuilder = implPrefix + nextDepCount;
                        generateConsumeAllImplMethod(cc, sf, svcTypeParam, depCount, nextDepCount, nextBuilder, true);
                    }

                    // after() method impls
                    generateAfterImplMethods(cc, sf, selfType);

                    // before() method impls
                    generateBeforeImplMethods(cc, sf, selfType);

                    // afterBuildItem() method impl
                    generateAfterBuildItemImplMethod(cc, selfType);

                    // withPhase() method impl (only on Impl0)
                    if (depCount == 0) {
                        generateAtPhaseImplMethod(cc, sf, selfType);
                    }

                    // action() - sync terminal
                    cc.method("action", mc -> {
                        mc.public_();
                        mc.annotate(Type.of(Override.class));
                        mc.returning(Type.VOID);
                        Type[] actionTypeArgs = new Type[actionArity + 1];
                        actionTypeArgs[0] = svcTypeParam;
                        actionTypeArgs[1] = Type.named(contextFqn);
                        sf.import_(actionTypeArgs[1]);
                        for (int j = 0; j < depCount; j++) {
                            actionTypeArgs[j + 2] = Type.named("P" + (j + 1));
                        }
                        Type actionType = Type.named(PKG + ".Action" + actionArity);
                        sf.import_(actionType);
                        Var action = mc.param("action", actionType.typeArg(actionTypeArgs));
                        mc.body(b0 -> b0.emit($v("sbi").call("doAction", action, Expr.FALSE)));
                    });

                    // actionAsync() - async terminal
                    cc.method("actionAsync", mc -> {
                        mc.public_();
                        mc.annotate(Type.of(Override.class));
                        mc.returning(Type.VOID);
                        Type[] actionTypeArgs = new Type[actionArity + 1];
                        actionTypeArgs[0] = Type.of(Void.class);
                        actionTypeArgs[1] = Type.named(ASYNC_START_CONTEXT).typeArg(svcTypeParam);
                        sf.import_(actionTypeArgs[1].erasure());
                        for (int j = 0; j < depCount; j++) {
                            actionTypeArgs[j + 2] = Type.named("P" + (j + 1));
                        }
                        Type actionType = Type.named(PKG + ".Action" + actionArity);
                        sf.import_(actionType);
                        Var action = mc.param("action", actionType.typeArg(actionTypeArgs));
                        mc.body(b0 -> b0.emit($v("sbi").call("doAction", action, Expr.TRUE)));
                    });
                });
            });
        }
    }

    // ── Void service builder impls ──

    /**
     * Generate runtime {@code VoidServiceBuilderImpl0} through {@code VoidServiceBuilderImpl9} wrapper classes.
     *
     * @param sources the JDeparser sources to generate into
     */
    private static void generateVoidServiceBuilderImpls(Sources sources) {
        generateVoidServiceBuilderImplsParameterized(
                sources, "VoidServiceBuilderImpl", "VoidServiceBuilder", START_CONTEXT);
    }

    /**
     * Parameterized generation of void service builder implementation classes.
     *
     * @param sources the JDeparser sources to generate into
     * @param implPrefix the implementation class name prefix
     * @param interfacePrefix the interface class name prefix
     * @param contextFqn the fully-qualified context type name
     */
    private static void generateVoidServiceBuilderImplsParameterized(
            Sources sources,
            String implPrefix,
            String interfacePrefix,
            String contextFqn) {
        for (int i = 0; i <= MAX_DEPS; i++) {
            String className = implPrefix + i;
            String interfaceFqn = PKG + "." + interfacePrefix + i;
            final int depCount = i;
            final int actionArity = depCount + 1;
            sources.createSourceFile(IMPL_PKG, className, sf -> {
                sf.class_(className, cc -> {
                    cc.public_();
                    cc.final_();
                    Type sbiType = Type.named("io.quarkus.core.deployment.action.impl.ServiceBuilderImpl");
                    sf.import_(sbiType);
                    cc.field("sbi", fc -> {
                        fc.type(sbiType);
                        fc.private_();
                        fc.final_();
                    });
                    List<Type> interfaceTypeArgs = new ArrayList<>(depCount);
                    // P1..PN = dependency types (no T for void)
                    for (int j = 0; j < depCount; j++) {
                        String tpName = "P" + (j + 1);
                        cc.typeParam(tpName, tpc -> {
                        });
                        interfaceTypeArgs.add(Type.named(tpName));
                    }
                    Type interfaceRawType = Type.named(interfaceFqn);
                    if (depCount > 0) {
                        cc.implements_(interfaceRawType.typeArg(interfaceTypeArgs));
                    } else {
                        cc.implements_(interfaceRawType);
                    }
                    sf.import_(interfaceRawType);

                    // self type for after() return
                    Type selfType;
                    if (depCount == 0) {
                        selfType = Type.named(className);
                    } else {
                        Type[] selfTypeArgs = new Type[depCount];
                        for (int j = 0; j < depCount; j++) {
                            selfTypeArgs[j] = Type.named("P" + (j + 1));
                        }
                        selfType = Type.named(className).typeArg(selfTypeArgs);
                    }

                    // constructor
                    cc.constructor(mc -> {
                        mc.public_();
                        Var sbi = mc.param("sbi", sbiType);
                        mc.body(b0 -> b0.emit(Expr.THIS.field("sbi").assign(sbi)));
                    });

                    // require() method impls - only if we haven't hit max deps
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount = depCount + 1;
                        String nextBuilder = implPrefix + nextDepCount;
                        // require(Class<PN1> type)
                        cc.method("require", mc -> {
                            mc.public_();
                            mc.annotate(Type.of(Override.class));
                            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
                            });
                            Type[] nextTypeArgs = new Type[nextDepCount];
                            for (int j = 0; j < depCount; j++) {
                                nextTypeArgs[j] = Type.named("P" + (j + 1));
                            }
                            nextTypeArgs[depCount] = pn1;
                            Type nextType = Type.named(nextBuilder).typeArg(nextTypeArgs);
                            mc.returning(nextType);
                            Var type = mc.param("type", Type.of(Class.class).typeArg(pn1));
                            mc.body(b0 -> {
                                b0.emit($v("sbi").call("require", type));
                                b0.return_(nextType.new_($v("sbi")));
                            });
                        });
                        // require(Class<PN1> type, String name)
                        cc.method("require", mc -> {
                            mc.public_();
                            mc.annotate(Type.of(Override.class));
                            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
                            });
                            Type[] nextTypeArgs = new Type[nextDepCount];
                            for (int j = 0; j < depCount; j++) {
                                nextTypeArgs[j] = Type.named("P" + (j + 1));
                            }
                            nextTypeArgs[depCount] = pn1;
                            Type nextType = Type.named(nextBuilder).typeArg(nextTypeArgs);
                            mc.returning(nextType);
                            Var type = mc.param("type", Type.of(Class.class).typeArg(pn1));
                            Var name = mc.param("name", Type.of(String.class));
                            mc.body(b0 -> {
                                b0.emit($v("sbi").call("require", type, name));
                                b0.return_(nextType.new_($v("sbi")));
                            });
                        });
                    }

                    // request() method impls - optional dependencies (only if we haven't hit max deps)
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount2 = depCount + 1;
                        String nextBuilder2 = implPrefix + nextDepCount2;
                        generateRequestImplMethods(cc, sf, null, depCount, nextDepCount2, nextBuilder2, false);
                    }

                    // consumeAll() method impls (only if we haven't hit max deps)
                    if (depCount < MAX_DEPS) {
                        final int nextDepCount2 = depCount + 1;
                        String nextBuilder2 = implPrefix + nextDepCount2;
                        generateConsumeAllImplMethod(cc, sf, null, depCount, nextDepCount2, nextBuilder2, false);
                    }

                    // after() method impls
                    generateAfterImplMethods(cc, sf, selfType);

                    // before() method impls
                    generateBeforeImplMethods(cc, sf, selfType);

                    // afterBuildItem() method impl
                    generateAfterBuildItemImplMethod(cc, selfType);

                    // withPhase() method impl (only on Impl0)
                    if (depCount == 0) {
                        generateAtPhaseImplMethod(cc, sf, selfType);
                    }

                    // action() - sync terminal using VoidAction
                    cc.method("action", mc -> {
                        mc.public_();
                        mc.annotate(Type.of(Override.class));
                        mc.returning(Type.VOID);
                        Type[] actionTypeArgs = new Type[actionArity];
                        actionTypeArgs[0] = Type.named(contextFqn);
                        sf.import_(actionTypeArgs[0]);
                        for (int j = 0; j < depCount; j++) {
                            actionTypeArgs[j + 1] = Type.named("P" + (j + 1));
                        }
                        Type actionType = Type.named(PKG + ".VoidAction" + actionArity);
                        sf.import_(actionType);
                        Var action = mc.param("action", actionType.typeArg(actionTypeArgs));
                        mc.body(b0 -> b0.emit($v("sbi").call("doAction", action, Expr.FALSE)));
                    });

                    // actionAsync() - async terminal
                    cc.method("actionAsync", mc -> {
                        mc.public_();
                        mc.annotate(Type.of(Override.class));
                        mc.returning(Type.VOID);
                        Type[] actionTypeArgs = new Type[actionArity];
                        actionTypeArgs[0] = Type.named(ASYNC_VOID_START_CONTEXT);
                        sf.import_(actionTypeArgs[0]);
                        for (int j = 0; j < depCount; j++) {
                            actionTypeArgs[j + 1] = Type.named("P" + (j + 1));
                        }
                        Type actionType = Type.named(PKG + ".VoidAction" + actionArity);
                        sf.import_(actionType);
                        Var action = mc.param("action", actionType.typeArg(actionTypeArgs));
                        mc.body(b0 -> b0.emit($v("sbi").call("doAction", action, Expr.TRUE)));
                    });
                });
            });
        }
    }

    /**
     * Generate the five {@code after()} method implementations on a builder impl class.
     * Each calls the corresponding method on the wrapped {@code ServiceBuilderImpl} and returns {@code this}.
     *
     * @param cc the class context to add methods to
     * @param sf the source file context (for imports)
     * @param selfType the self-referencing return type
     */
    private static void generateAfterImplMethods(
            ClassCreator cc,
            SourceFileCreator sf,
            Type selfType) {
        // after(String name)
        cc.method("after", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            mc.returning(selfType);
            Var name = mc.param("name", Type.of(String.class));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("after", name));
                b0.return_(Expr.THIS);
            });
        });
        // after(List<String> nameParts)
        cc.method("after", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            mc.returning(selfType);
            sf.import_(List.class);
            Var nameParts = mc.param("nameParts", Type.of(List.class).typeArg(Type.of(String.class)));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("after", nameParts));
                b0.return_(Expr.THIS);
            });
        });
        // after(Class<?> type)
        cc.method("after", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            mc.returning(selfType);
            Var type = mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("after", type));
                b0.return_(Expr.THIS);
            });
        });
        // after(Class<?> type, String name)
        cc.method("after", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            mc.returning(selfType);
            Var type = mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD));
            Var name = mc.param("name", Type.of(String.class));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("after", type, name));
                b0.return_(Expr.THIS);
            });
        });
        // after(Class<?> type, List<String> nameParts)
        cc.method("after", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            mc.returning(selfType);
            sf.import_(List.class);
            Var type = mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD));
            Var nameParts = mc.param("nameParts", Type.of(List.class).typeArg(Type.of(String.class)));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("after", type, nameParts));
                b0.return_(Expr.THIS);
            });
        });
    }

    /**
     * Generate the four {@code before()} method implementations on a builder impl class.
     * Each calls the corresponding method on the wrapped {@code ServiceBuilderImpl} and returns {@code this}.
     *
     * @param cc the class context to add methods to
     * @param sf the source file context (for imports)
     * @param selfType the self-referencing return type
     */
    private static void generateBeforeImplMethods(
            ClassCreator cc,
            SourceFileCreator sf,
            Type selfType) {
        // before(String name)
        cc.method("before", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            mc.returning(selfType);
            Var name = mc.param("name", Type.of(String.class));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("before", name));
                b0.return_(Expr.THIS);
            });
        });
        // before(Class<?> type)
        cc.method("before", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            mc.returning(selfType);
            Var type = mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("before", type));
                b0.return_(Expr.THIS);
            });
        });
        // before(Class<?> type, String name)
        cc.method("before", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            mc.returning(selfType);
            Var type = mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD));
            Var name = mc.param("name", Type.of(String.class));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("before", type, name));
                b0.return_(Expr.THIS);
            });
        });
        // before(Class<?> type, List<String> nameParts)
        cc.method("before", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            mc.returning(selfType);
            sf.import_(List.class);
            Var type = mc.param("type", Type.of(Class.class).typeArg(Type.WILDCARD));
            Var nameParts = mc.param("nameParts", Type.of(List.class).typeArg(Type.of(String.class)));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("before", type, nameParts));
                b0.return_(Expr.THIS);
            });
        });
    }

    // Turn off the formatter because it doesn't understand how HTML relates to @code
    //@formatter:off
    /**
     * Generate the two {@code request()} method implementations on a builder impl class.
     * Each calls {@code sbi.request()} and returns a new next-arity wrapper with
     * {@code Optional<P>} as the type parameter.
     *
     * @param cc the class context
     * @param sf the source file context (for imports)
     * @param svcTypeParam the service type parameter ({@code T}), or {@code null} for void builders
     * @param depCount the current dependency count
     * @param nextDepCount the next dependency count (depCount + 1)
     * @param nextBuilder the next builder impl class name
     * @param hasServiceTypeParam whether the builder has a {@code T} type parameter
     */
    //@formatter:on
    private static void generateRequestImplMethods(
            ClassCreator cc,
            SourceFileCreator sf,
            Type svcTypeParam,
            int depCount,
            int nextDepCount,
            String nextBuilder,
            boolean hasServiceTypeParam) {
        sf.import_(Optional.class);
        // request(Class<PN1> type)
        cc.method("request", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
            });
            Type optionalPn1 = Type.of(Optional.class).typeArg(pn1);
            Type[] nextTypeArgs;
            if (hasServiceTypeParam) {
                nextTypeArgs = new Type[nextDepCount + 1];
                nextTypeArgs[0] = svcTypeParam;
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j + 1] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[nextDepCount] = optionalPn1;
            } else {
                nextTypeArgs = new Type[nextDepCount];
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[depCount] = optionalPn1;
            }
            Type nextType = Type.named(nextBuilder).typeArg(nextTypeArgs);
            mc.returning(nextType);
            Var type = mc.param("type", Type.of(Class.class).typeArg(pn1));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("request", type));
                b0.return_(nextType.new_($v("sbi")));
            });
        });
        // request(Class<PN1> type, String name)
        cc.method("request", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
            });
            Type optionalPn1 = Type.of(Optional.class).typeArg(pn1);
            Type[] nextTypeArgs;
            if (hasServiceTypeParam) {
                nextTypeArgs = new Type[nextDepCount + 1];
                nextTypeArgs[0] = svcTypeParam;
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j + 1] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[nextDepCount] = optionalPn1;
            } else {
                nextTypeArgs = new Type[nextDepCount];
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[depCount] = optionalPn1;
            }
            Type nextType = Type.named(nextBuilder).typeArg(nextTypeArgs);
            mc.returning(nextType);
            Var type = mc.param("type", Type.of(Class.class).typeArg(pn1));
            Var name = mc.param("name", Type.of(String.class));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("request", type, name));
                b0.return_(nextType.new_($v("sbi")));
            });
        });
    }

    /**
     * Generate the {@code consumeAll()} method declaration on a builder interface.
     * Consumes all services of the given type as a {@code Map<String, P>}.
     * Steps arity like {@code require()}.
     *
     * @param cc the interface context
     * @param svcTypeParam the service type parameter ({@code T}), or {@code null} for void builders
     * @param depCount the current dependency count
     * @param nextDepCount the next dependency count (depCount + 1)
     * @param nextBuilder the next builder class name
     * @param hasServiceTypeParam whether the builder has a {@code T} type parameter
     */
    private static void generateConsumeAllInterfaceMethod(
            InterfaceCreator cc,
            Type svcTypeParam,
            int depCount,
            int nextDepCount,
            String nextBuilder,
            boolean hasServiceTypeParam) {
        cc.method("consumeAll", mc -> {
            mc.docComment(dcc -> {
                dcc.text("Consume all services of the given type. ");
                dcc.text("The services are injected as a {@code Map<String, T>} where keys are service names. ");
                dcc.text("Zero matches produces an empty map.");
                dcc.return_(rc -> rc.text("a builder with the added dependency"));
            });
            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
                tpc.docComment(dcc -> dcc.text("the type of the services to consume"));
            });
            Type mapPn1 = Type.of(Map.class).typeArg(Type.of(String.class), pn1);
            Type[] nextTypeArgs;
            if (hasServiceTypeParam) {
                nextTypeArgs = new Type[nextDepCount + 1];
                nextTypeArgs[0] = svcTypeParam;
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j + 1] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[nextDepCount] = mapPn1;
            } else {
                nextTypeArgs = new Type[nextDepCount];
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[depCount] = mapPn1;
            }
            mc.returning(Type.named(nextBuilder).typeArg(nextTypeArgs));
            mc.param("type", Type.of(Class.class).typeArg(pn1), pc -> {
                pc.docComment(dcc -> dcc.text("the service type to consume (must not be {@code null})"));
            });
        });
    }

    /**
     * Generate the {@code consumeAll()} method implementation on a builder impl class.
     * Calls {@code sbi.consumeAll()} and returns a new next-arity wrapper with
     * {@code Map<String, P>} as the type parameter.
     *
     * @param cc the class context
     * @param sf the source file context (for imports)
     * @param svcTypeParam the service type parameter ({@code T}), or {@code null} for void builders
     * @param depCount the current dependency count
     * @param nextDepCount the next dependency count (depCount + 1)
     * @param nextBuilder the next builder impl class name
     * @param hasServiceTypeParam whether the builder has a {@code T} type parameter
     */
    private static void generateConsumeAllImplMethod(
            ClassCreator cc,
            SourceFileCreator sf,
            Type svcTypeParam,
            int depCount,
            int nextDepCount,
            String nextBuilder,
            boolean hasServiceTypeParam) {
        sf.import_(Map.class);
        cc.method("consumeAll", mc -> {
            mc.public_();
            mc.annotate(Type.of(Override.class));
            Type pn1 = mc.typeParam("P" + nextDepCount, tpc -> {
            });
            Type mapPn1 = Type.of(Map.class).typeArg(Type.of(String.class), pn1);
            Type[] nextTypeArgs;
            if (hasServiceTypeParam) {
                nextTypeArgs = new Type[nextDepCount + 1];
                nextTypeArgs[0] = svcTypeParam;
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j + 1] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[nextDepCount] = mapPn1;
            } else {
                nextTypeArgs = new Type[nextDepCount];
                for (int j = 0; j < depCount; j++) {
                    nextTypeArgs[j] = Type.named("P" + (j + 1));
                }
                nextTypeArgs[depCount] = mapPn1;
            }
            Type nextType = Type.named(nextBuilder).typeArg(nextTypeArgs);
            mc.returning(nextType);
            Var type = mc.param("type", Type.of(Class.class).typeArg(pn1));
            mc.body(b0 -> {
                b0.emit($v("sbi").call("consumeAll", type));
                b0.return_(nextType.new_($v("sbi")));
            });
        });
    }
}
