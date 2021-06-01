package io.quarkus.grpc.protoc.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.html.HtmlEscapers;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;
import com.salesforce.jprotoc.ProtocPlugin;

/**
 * @author Paulo Lopes
 */
public class MutinyGrpcGenerator extends Generator {

    private static final Logger log = Logger.getLogger(MutinyGrpcGenerator.class.getName());

    private static final int SERVICE_NUMBER_OF_PATHS = 2;
    private static final int METHOD_NUMBER_OF_PATHS = 4;
    public static final String CLASS_PREFIX = "Mutiny";

    private String getServiceJavaDocPrefix() {
        return "    ";
    }

    private String getMethodJavaDocPrefix() {
        return "        ";
    }

    @Override
    public List<PluginProtos.CodeGeneratorResponse.File> generateFiles(PluginProtos.CodeGeneratorRequest request)
            throws GeneratorException {
        ProtoTypeMap typeMap = ProtoTypeMap.of(request.getProtoFileList());

        List<DescriptorProtos.FileDescriptorProto> protosToGenerate = request.getProtoFileList().stream()
                .filter(protoFile -> request.getFileToGenerateList().contains(protoFile.getName()))
                .collect(Collectors.toList());

        List<ServiceContext> services = findServices(protosToGenerate, typeMap);
        validateServices(services);
        return generateFiles(services);
    }

    private void validateServices(List<ServiceContext> services) {
        boolean failed = false;
        for (ServiceContext service : services) {
            if (service.packageName == null || service.packageName.isBlank()) {
                log.log(Level.SEVERE, "Using the default java package is not supported for "
                        + "Quarkus gRPC code generation. Please specify `option java_package = \"your.package\"` in "
                        + service.protoName);
                failed = true;
            }
        }
        if (failed) {
            throw new IllegalArgumentException("Code generation failed. Please check the log above for details.");
        }
    }

    private List<ServiceContext> findServices(List<DescriptorProtos.FileDescriptorProto> protos, ProtoTypeMap typeMap) {
        List<ServiceContext> contexts = new ArrayList<>();

        protos.forEach(fileProto -> {
            for (int serviceNumber = 0; serviceNumber < fileProto.getServiceCount(); serviceNumber++) {
                ServiceContext serviceContext = buildServiceContext(
                        fileProto.getService(serviceNumber),
                        typeMap,
                        fileProto.getSourceCodeInfo().getLocationList(),
                        serviceNumber);
                serviceContext.protoName = fileProto.getName();
                serviceContext.packageName = extractPackageName(fileProto);
                contexts.add(serviceContext);
            }
        });

        return contexts;
    }

    private String extractPackageName(DescriptorProtos.FileDescriptorProto proto) {
        DescriptorProtos.FileOptions options = proto.getOptions();
        if (options != null) {
            String javaPackage = options.getJavaPackage();
            if (!Strings.isNullOrEmpty(javaPackage)) {
                return javaPackage;
            }
        }

        return Strings.nullToEmpty(proto.getPackage());
    }

    private ServiceContext buildServiceContext(DescriptorProtos.ServiceDescriptorProto serviceProto, ProtoTypeMap typeMap,
            List<DescriptorProtos.SourceCodeInfo.Location> locations, int serviceNumber) {
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.classPrefix = CLASS_PREFIX;
        serviceContext.fileName = CLASS_PREFIX + serviceProto.getName() + "Grpc.java";
        serviceContext.className = CLASS_PREFIX + serviceProto.getName() + "Grpc";
        serviceContext.serviceName = serviceProto.getName();
        serviceContext.deprecated = serviceProto.getOptions() != null && serviceProto.getOptions().getDeprecated();

        List<DescriptorProtos.SourceCodeInfo.Location> allLocationsForService = locations.stream()
                .filter(location -> location.getPathCount() >= 2 &&
                        location.getPath(0) == DescriptorProtos.FileDescriptorProto.SERVICE_FIELD_NUMBER &&
                        location.getPath(1) == serviceNumber)
                .collect(Collectors.toList());

        DescriptorProtos.SourceCodeInfo.Location serviceLocation = allLocationsForService.stream()
                .filter(location -> location.getPathCount() == SERVICE_NUMBER_OF_PATHS)
                .findFirst()
                .orElseGet(DescriptorProtos.SourceCodeInfo.Location::getDefaultInstance);
        serviceContext.javaDoc = getJavaDoc(getComments(serviceLocation), getServiceJavaDocPrefix());

        for (int methodNumber = 0; methodNumber < serviceProto.getMethodCount(); methodNumber++) {
            MethodContext methodContext = buildMethodContext(
                    serviceProto.getMethod(methodNumber),
                    typeMap,
                    locations,
                    methodNumber);

            serviceContext.methods.add(methodContext);
        }
        return serviceContext;
    }

    private MethodContext buildMethodContext(DescriptorProtos.MethodDescriptorProto methodProto, ProtoTypeMap typeMap,
            List<DescriptorProtos.SourceCodeInfo.Location> locations, int methodNumber) {
        MethodContext methodContext = new MethodContext();
        methodContext.methodName = lowerCaseFirst(methodProto.getName());
        methodContext.inputType = typeMap.toJavaTypeName(methodProto.getInputType());
        methodContext.outputType = typeMap.toJavaTypeName(methodProto.getOutputType());
        methodContext.deprecated = methodProto.getOptions() != null && methodProto.getOptions().getDeprecated();
        methodContext.isManyInput = methodProto.getClientStreaming();
        methodContext.isManyOutput = methodProto.getServerStreaming();
        methodContext.methodNumber = methodNumber;

        DescriptorProtos.SourceCodeInfo.Location methodLocation = locations.stream()
                .filter(location -> location.getPathCount() == METHOD_NUMBER_OF_PATHS &&
                        location.getPath(METHOD_NUMBER_OF_PATHS - 1) == methodNumber)
                .findFirst()
                .orElseGet(DescriptorProtos.SourceCodeInfo.Location::getDefaultInstance);
        methodContext.javaDoc = getJavaDoc(getComments(methodLocation), getMethodJavaDocPrefix());

        if (!methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
            methodContext.mutinyCallsMethodName = "oneToOne";
            methodContext.grpcCallsMethodName = "asyncUnaryCall";
        }
        if (!methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
            methodContext.mutinyCallsMethodName = "oneToMany";
            methodContext.grpcCallsMethodName = "asyncServerStreamingCall";
        }
        if (methodProto.getClientStreaming() && !methodProto.getServerStreaming()) {
            methodContext.mutinyCallsMethodName = "manyToOne";
            methodContext.grpcCallsMethodName = "asyncClientStreamingCall";
        }
        if (methodProto.getClientStreaming() && methodProto.getServerStreaming()) {
            methodContext.mutinyCallsMethodName = "manyToMany";
            methodContext.grpcCallsMethodName = "asyncBidiStreamingCall";
        }
        return methodContext;
    }

    private String lowerCaseFirst(String s) {
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private List<PluginProtos.CodeGeneratorResponse.File> generateFiles(List<ServiceContext> services) {
        List<PluginProtos.CodeGeneratorResponse.File> files = new ArrayList<>();
        for (ServiceContext service : services) {
            files.add(buildFile(service, "MutinyStub.mustache", absoluteFileName(service, null)));
            files.add(buildFile(service, "MutinyInterface.mustache",
                    absoluteFileName(service, service.serviceName + ".java")));
            files.add(buildFile(service, "MutinyBean.mustache",
                    absoluteFileName(service, service.serviceName + "Bean.java")));
            files.add(buildFile(service, "MutinyClient.mustache",
                    absoluteFileName(service, service.serviceName + "Client.java")));
        }
        return files;
    }

    private PluginProtos.CodeGeneratorResponse.File buildFile(ServiceContext context, String templateName, String fileName) {
        String content = applyTemplate(templateName, context);
        return PluginProtos.CodeGeneratorResponse.File
                .newBuilder()
                .setName(fileName)
                .setContent(content)
                .build();
    }

    private String absoluteFileName(ServiceContext ctx, String fileName) {
        if (fileName == null) {
            fileName = ctx.fileName;
        }
        String dir = ctx.packageName.replace('.', '/');
        if (Strings.isNullOrEmpty(dir)) {
            return fileName;
        } else {
            return dir + "/" + fileName;
        }
    }

    private String getComments(DescriptorProtos.SourceCodeInfo.Location location) {
        return location.getLeadingComments().isEmpty() ? location.getTrailingComments() : location.getLeadingComments();
    }

    private String getJavaDoc(String comments, String prefix) {
        if (!comments.isEmpty()) {
            StringBuilder builder = new StringBuilder("/**\n")
                    .append(prefix).append(" * <pre>\n");
            Arrays.stream(HtmlEscapers.htmlEscaper().escape(comments).split("\n"))
                    .map(line -> line.replace("*/", "&#42;&#47;").replace("*", "&#42;"))
                    .forEach(line -> builder.append(prefix).append(" * ").append(line).append("\n"));
            builder
                    .append(prefix).append(" * </pre>\n")
                    .append(prefix).append(" */");
            return builder.toString();
        }
        return null;
    }

    /**
     * Template class for proto Service objects.
     */
    private static class ServiceContext {
        // CHECKSTYLE DISABLE VisibilityModifier FOR 8 LINES
        public String fileName;
        public String protoName;
        public String packageName;
        public String className;
        public String classPrefix;
        public String serviceName;
        public boolean deprecated;
        public String javaDoc;
        public List<MethodContext> methods = new ArrayList<>();

        public List<MethodContext> unaryUnaryMethods() {
            return methods.stream().filter(m -> !m.isManyInput && !m.isManyOutput).collect(Collectors.toList());
        }

        public List<MethodContext> unaryManyMethods() {
            return methods.stream().filter(m -> !m.isManyInput && m.isManyOutput).collect(Collectors.toList());
        }

        public List<MethodContext> manyUnaryMethods() {
            return methods.stream().filter(m -> m.isManyInput && !m.isManyOutput).collect(Collectors.toList());
        }

        public List<MethodContext> manyManyMethods() {
            return methods.stream().filter(m -> m.isManyInput && m.isManyOutput).collect(Collectors.toList());
        }
    }

    /**
     * Template class for proto RPC objects.
     */
    private static class MethodContext {
        // CHECKSTYLE DISABLE VisibilityModifier FOR 10 LINES
        public String methodName;
        public String inputType;
        public String outputType;
        public boolean deprecated;
        public boolean isManyInput;
        public boolean isManyOutput;
        public String mutinyCallsMethodName;
        public String grpcCallsMethodName;
        public int methodNumber;
        public String javaDoc;

        // This method mimics the upper-casing method ogf gRPC to ensure compatibility
        // See https://github.com/grpc/grpc-java/blob/v1.8.0/compiler/src/java_plugin/cpp/java_generator.cpp#L58
        public String methodNameUpperUnderscore() {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < methodName.length(); i++) {
                char c = methodName.charAt(i);
                s.append(Character.toUpperCase(c));
                if ((i < methodName.length() - 1) && Character.isLowerCase(c)
                        && Character.isUpperCase(methodName.charAt(i + 1))) {
                    s.append('_');
                }
            }
            return s.toString();
        }

        public String methodNamePascalCase() {
            String mn = methodName.replace("_", "");
            return String.valueOf(Character.toUpperCase(mn.charAt(0))) + mn.substring(1);
        }

        public String methodNameCamelCase() {
            String mn = methodName.replace("_", "");
            return String.valueOf(Character.toLowerCase(mn.charAt(0))) + mn.substring(1);
        }

        public String methodHeader() {
            String mh = "";
            if (!Strings.isNullOrEmpty(javaDoc)) {
                mh = javaDoc;
            }

            if (deprecated) {
                mh += "\n        @Deprecated";
            }

            return mh;
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            ProtocPlugin.generate(new MutinyGrpcGenerator());
        } else {
            ProtocPlugin.debug(new MutinyGrpcGenerator(), args[0]);
        }
    }
}
