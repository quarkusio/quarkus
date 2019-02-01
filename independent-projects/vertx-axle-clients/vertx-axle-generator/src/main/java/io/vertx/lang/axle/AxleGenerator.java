package io.vertx.lang.axle;

import io.vertx.codegen.*;
import io.vertx.codegen.type.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class AxleGenerator extends AbstractAxleGenerator {
  AxleGenerator() {
    super("axle");
    this.kinds = Collections.singleton("class");
    this.name = "Axle";
  }

  @Override
  protected void genRxImports(ClassModel model, PrintWriter writer) {
    writer.println("import org.reactivestreams.Publisher;");
    writer.println("import io.reactivex.Flowable;");
    writer.println("import java.util.function.Consumer;");
    writer.println("import java.util.concurrent.CompletionStage;");
    super.genRxImports(model, writer);
  }

  @Override
  protected void genToObservable(ApiTypeInfo type, PrintWriter writer) {
    TypeInfo streamType = type.getReadStreamArg();

    writer.print("  private org.reactivestreams.Publisher<");
    writer.print(genTypeName(streamType));
    writer.println("> publisher;");

    writer.println();

    genToXXXAble(streamType, "Publisher", "publisher", writer);
  }

  private void genToXXXAble(TypeInfo streamType, String rxType, String rxName, PrintWriter writer) {
    writer.print("  public synchronized org.reactivestreams.");
    writer.print(rxType);
    writer.print("<");
    writer.print(genTypeName(streamType));
    writer.println("> toPublisher() {");

    writer.print("    ");
    writer.print("if (");
    writer.print(rxName);
    writer.println(" == null) {");

    if (streamType.getKind() == ClassKind.API) {
      writer.print("      java.util.function.Function<");
      writer.print(streamType.getName());
      writer.print(", ");
      writer.print(genTypeName(streamType));
      writer.print("> conv = ");
      writer.print(genTypeName(streamType.getRaw()));
      writer.println("::newInstance;");

      writer.print("      ");
      writer.print(rxName);
      writer.print(" = io.vertx.axle.");
      writer.print(rxType);
      writer.print("Helper.to");
      writer.print(rxType);
      writer.println("(delegate, conv);");
    } else if (streamType.isVariable()) {
      String typeVar = streamType.getSimpleName();
      writer.print("      java.util.function.Function<");
      writer.print(typeVar);
      writer.print(", ");
      writer.print(typeVar);
      writer.print("> conv = (java.util.function.Function<");
      writer.print(typeVar);
      writer.print(", ");
      writer.print(typeVar);
      writer.println(">) __typeArg_0.wrap;");

      writer.print("      ");
      writer.print(rxName);
      writer.print(" = io.vertx.axle.");
      writer.print(rxType);
      writer.print("Helper.to");
      writer.print(rxType);
      writer.println("(delegate, conv);");
    } else {
      writer.print("      ");
      writer.print(rxName);
      writer.print(" = io.vertx.axle.");
      writer.print(rxType);
      writer.print("Helper.to");
      writer.print(rxType);
      writer.println("(this.getDelegate());");
    }

    writer.println("    }");
    writer.print("    return ");
    writer.print(rxName);
    writer.println(";");
    writer.println("  }");
    writer.println();
  }

  private void genToXXXEr(TypeInfo streamType, String rxType, String rxName, PrintWriter writer) {
    writer.format("  public synchronized io.vertx.reactivex.WriteStream%s<%s> to%s() {%n", rxType, genTypeName(streamType), rxType);

    writer.format("    if (%s == null) {%n", rxName);

    if (streamType.getKind() == ClassKind.API) {
      writer.format("      java.util.function.Function<%s, %s> conv = %s::getDelegate;%n", genTypeName(streamType.getRaw()), streamType.getName(), genTypeName(streamType));

      writer.format("      %s = io.vertx.axle.RxHelper.to%s(getDelegate(), conv);%n", rxName, rxType);
    } else if (streamType.isVariable()) {
      String typeVar = streamType.getSimpleName();
      writer.format("      java.util.function.Function<%s, %s> conv = (java.util.function.Function<%s, %s>) __typeArg_0.unwrap;%n", typeVar, typeVar, typeVar, typeVar);

      writer.format("      %s = io.vertx.axle.RxHelper.to%s(getDelegate(), conv);%n", rxName, rxType);
    } else {
      writer.format("      %s = io.vertx.axle.RxHelper.to%s(getDelegate());%n", rxName, rxType);
    }

    writer.println("    }");
    writer.format("    return %s;%n", rxName);
    writer.println("  }");
    writer.println();
  }

//  private String genFutureMethodName(MethodInfo method) {
//    return "rx" + Character.toUpperCase(method.getName().charAt(0)) + method.getName().substring(1);
//  }

  @Override
  protected void genMethods(ClassModel model, MethodInfo method, List<String> cacheDecls, PrintWriter writer) {
    genMethod(model, method, cacheDecls, writer);
    MethodInfo publisherOverload = genOverloadedMethod(method, org.reactivestreams.Publisher.class);
    if (publisherOverload != null) {
      genMethod(model, publisherOverload, cacheDecls, writer);
    }
  }

  @Override
  protected void genConsumerMethod(ClassModel model, MethodInfo method, PrintWriter writer) {
    MethodInfo futMethod = genConsumerMethod(method);
    startMethodTemplate(false, futMethod.getName(), futMethod, "", writer);
    writer.println(" {");
    writer.print("    ");
    if (!method.getReturnType().isVoid()) {
      writer.print("return ");
    }
    writer.print("__" + method.getName() + "(");
    List<ParamInfo> params = futMethod.getParams();
    for (int i = 0;i < params.size();i++) {
      if (i > 0) {
        writer.print(", ");
      }
      ParamInfo param = params.get(i);
      if (i < params.size() - 1) {
        writer.print(param.getName());
      } else {
        writer.print(param.getName() + " != null ? " + param.getName() + "::accept : null");
      }
    }
    writer.println(");");
    writer.println("  }");
    writer.println();
  }

  @Override
  protected void genCSMethod(ClassModel model, MethodInfo method, PrintWriter writer) {
    MethodInfo futMethod = genCSMethod(method);
    ClassTypeInfo raw = futMethod.getReturnType().getRaw();
    String methodSimpleName = raw.getSimpleName();
    String adapterType = "io.vertx.axle.AsyncResult" + methodSimpleName + ".to" + methodSimpleName;
    startMethodTemplate(false, futMethod.getName(), futMethod, "", writer);
    writer.println(" { ");
    writer.print("    return ");
    writer.print(adapterType);
    writer.println("(handler -> {");
    writer.print("      __");
    writer.print(method.getName());
    writer.print("(");
    List<ParamInfo> params = futMethod.getParams();
    writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
    if (params.size() > 0) {
      writer.print(", ");
    }
    writer.println("handler);");
    writer.println("    });");
    writer.println("  }");
    writer.println();
  }

  protected void genReadStream(List<? extends TypeParamInfo> typeParams, PrintWriter writer){
    writer.print("  org.reactivestreams.Publisher<");
    writer.print(typeParams.get(0).getName());
    writer.println("> toPublisher();");
    writer.println();
  }

  public MethodInfo genConsumerMethod(MethodInfo method) {
    List<ParamInfo> futParams = new ArrayList<>();
    int count = 0;
    int size = method.getParams().size() - 1;
    while (count < size) {
      ParamInfo param = method.getParam(count);
      /* Transform ReadStream -> Flowable */
      futParams.add(param);
      count = count + 1;
    }
    ParamInfo futParam = method.getParam(size);
    TypeInfo consumerType = ((ParameterizedTypeInfo) futParam.getType()).getArg(0);
    TypeInfo consumerUnresolvedType = ((ParameterizedTypeInfo) futParam.getUnresolvedType()).getArg(0);
    TypeInfo consumerReturnType = new io.vertx.codegen.type.ParameterizedTypeInfo(io.vertx.codegen.type.TypeReflectionFactory.create(Consumer.class).getRaw(), consumerUnresolvedType.isNullable(), Collections.singletonList(consumerType));
    futParams.add(new ParamInfo(futParams.size(), futParam.getName(), futParam.getDescription(), consumerReturnType));
    return method.copy().setParams(futParams);
  }

  private MethodInfo genCSMethod(MethodInfo method) {
    List<ParamInfo> futParams = new ArrayList<>();
    int count = 0;
    int size = method.getParams().size() - 1;
    while (count < size) {
      ParamInfo param = method.getParam(count);
      /* Transform ReadStream -> Flowable */
      futParams.add(param);
      count = count + 1;
    }
    ParamInfo futParam = method.getParam(size);
    TypeInfo futType = ((ParameterizedTypeInfo) ((ParameterizedTypeInfo) futParam.getType()).getArg(0)).getArg(0);
    TypeInfo futUnresolvedType = ((ParameterizedTypeInfo) ((ParameterizedTypeInfo) futParam.getUnresolvedType()).getArg(0)).getArg(0);
    TypeInfo futReturnType = new io.vertx.codegen.type.ParameterizedTypeInfo(io.vertx.codegen.type.TypeReflectionFactory.create(CompletionStage.class).getRaw(), futUnresolvedType.isNullable(), Collections.singletonList(futType));
    return method.copy().setReturnType(futReturnType).setParams(futParams);
  }

  private MethodInfo genOverloadedMethod(MethodInfo method, Class streamType) {
    List<ParamInfo> params = null;
    int count = 0;
    for (ParamInfo param : method.getParams()) {
      if (param.getType().isParameterized() && param.getType().getRaw().getName().equals("io.vertx.core.streams.ReadStream")) {
        if (params == null) {
          params = new ArrayList<>(method.getParams());
        }
        ParameterizedTypeInfo paramType = new io.vertx.codegen.type.ParameterizedTypeInfo(
          io.vertx.codegen.type.TypeReflectionFactory.create(streamType).getRaw(),
          false,
          java.util.Collections.singletonList(((ParameterizedTypeInfo) param.getType()).getArg(0))
        );
        params.set(count, new io.vertx.codegen.ParamInfo(
          param.getIndex(),
          param.getName(),
          param.getDescription(),
          paramType
        ));
      }
      count = count + 1;
    }
    if (params != null) {
      return method.copy().setParams(params);
    }
    return null;
  }
}
