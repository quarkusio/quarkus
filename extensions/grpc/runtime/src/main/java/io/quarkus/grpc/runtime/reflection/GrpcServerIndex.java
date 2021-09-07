package io.quarkus.grpc.runtime.reflection;

import static com.google.protobuf.Descriptors.FileDescriptor;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import com.google.protobuf.Descriptors;

import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.protobuf.ProtoFileDescriptorSupplier;

public class GrpcServerIndex {

    private final Set<String> names;
    private final Set<FileDescriptor> services;
    private final Map<String, FileDescriptor> descriptorsByName;
    private final Map<String, FileDescriptor> descriptorsBySymbol;
    private final Map<String, Map<Integer, FileDescriptor>> descriptorsByExtensionAndNumber;

    public GrpcServerIndex(
            List<ServerServiceDefinition> definitions) {
        Queue<FileDescriptor> fileDescriptorsToProcess = new ArrayDeque<>();
        Set<String> files = new HashSet<>();
        Set<String> names = new HashSet<>();
        Set<FileDescriptor> services = new HashSet<>();
        Map<String, FileDescriptor> descriptorsByName = new HashMap<>();
        Map<String, FileDescriptor> descriptorsBySymbol = new HashMap<>();
        Map<String, Map<Integer, FileDescriptor>> descriptorsByExtensionAndNumber = new HashMap<>();

        // Collect the services
        for (ServerServiceDefinition definition : definitions) {
            ServiceDescriptor serviceDescriptor = definition.getServiceDescriptor();
            if (serviceDescriptor.getSchemaDescriptor() instanceof ProtoFileDescriptorSupplier) {
                ProtoFileDescriptorSupplier supplier = (ProtoFileDescriptorSupplier) serviceDescriptor
                        .getSchemaDescriptor();
                FileDescriptor fd = supplier.getFileDescriptor();
                String serviceName = serviceDescriptor.getName();
                if (names.contains(serviceName)) {
                    throw new IllegalStateException("Duplicated gRPC service: " + serviceName);
                }
                services.add(fd);
                names.add(serviceName);

                if (!files.contains(fd.getName())) {
                    files.add(fd.getName());
                    fileDescriptorsToProcess.add(fd);
                }
            }
        }

        // Traverse the set of service and add dependencies
        while (!fileDescriptorsToProcess.isEmpty()) {
            FileDescriptor fd = fileDescriptorsToProcess.remove();
            processFileDescriptor(fd, descriptorsByName, descriptorsBySymbol, descriptorsByExtensionAndNumber);
            for (FileDescriptor dep : fd.getDependencies()) {
                if (!files.contains(dep.getName())) {
                    files.add(dep.getName());
                    fileDescriptorsToProcess.add(dep);
                }
            }
        }

        this.services = Collections.unmodifiableSet(services);
        this.descriptorsByName = Collections.unmodifiableMap(descriptorsByName);
        this.descriptorsByExtensionAndNumber = Collections.unmodifiableMap(descriptorsByExtensionAndNumber);
        this.descriptorsBySymbol = Collections.unmodifiableMap(descriptorsBySymbol);
        this.names = Collections.unmodifiableSet(names);
    }

    public Set<String> getServiceNames() {
        return names;
    }

    public FileDescriptor getFileDescriptorByName(String name) {
        return descriptorsByName.get(name);
    }

    public FileDescriptor getFileDescriptorBySymbol(String symbol) {
        return descriptorsBySymbol.get(symbol);
    }

    public FileDescriptor getFileDescriptorByExtensionAndNumber(String type, int number) {
        Map<Integer, FileDescriptor> map = descriptorsByExtensionAndNumber
                .getOrDefault(type, Collections.emptyMap());
        return map.get(number);
    }

    public Set<Integer> getExtensionNumbersOfType(String type) {
        return descriptorsByExtensionAndNumber.getOrDefault(type, Collections.emptyMap()).keySet();
    }

    private void processFileDescriptor(FileDescriptor fd,
            Map<String, FileDescriptor> descriptorsByName,
            Map<String, FileDescriptor> descriptorsBySymbol,
            Map<String, Map<Integer, FileDescriptor>> descriptorsByExtensionAndNumber) {
        String name = fd.getName();
        if (descriptorsByName.containsKey(name)) {
            throw new IllegalStateException("File name already used: " + name);
        }
        descriptorsByName.put(name, fd);
        for (Descriptors.ServiceDescriptor service : fd.getServices()) {
            processService(service, fd, descriptorsBySymbol);
        }
        for (Descriptors.Descriptor type : fd.getMessageTypes()) {
            processType(type, fd, descriptorsBySymbol, descriptorsByExtensionAndNumber);
        }
        for (Descriptors.FieldDescriptor extension : fd.getExtensions()) {
            processExtension(extension, fd, descriptorsByExtensionAndNumber);
        }
    }

    private void processService(Descriptors.ServiceDescriptor service, FileDescriptor fd,
            Map<String, FileDescriptor> descriptorsBySymbol) {
        String fullyQualifiedServiceName = service.getFullName();
        if (descriptorsBySymbol.containsKey(fullyQualifiedServiceName)) {
            throw new IllegalStateException("Service already defined: " + fullyQualifiedServiceName);
        }
        descriptorsBySymbol.put(fullyQualifiedServiceName, fd);
        for (Descriptors.MethodDescriptor method : service.getMethods()) {
            String fullyQualifiedMethodName = method.getFullName();
            if (descriptorsBySymbol.containsKey(fullyQualifiedMethodName)) {
                throw new IllegalStateException(
                        "Method already defined: " + fullyQualifiedMethodName + " in " + fullyQualifiedServiceName);
            }
            descriptorsBySymbol.put(fullyQualifiedMethodName, fd);
        }
    }

    private void processType(Descriptors.Descriptor type, FileDescriptor fd,
            Map<String, FileDescriptor> descriptorsBySymbol,
            Map<String, Map<Integer, FileDescriptor>> descriptorsByExtensionAndNumber) {
        String fullyQualifiedTypeName = type.getFullName();
        if (descriptorsBySymbol.containsKey(fullyQualifiedTypeName)) {
            throw new IllegalStateException("Type already defined: " + fullyQualifiedTypeName);
        }
        descriptorsBySymbol.put(fullyQualifiedTypeName, fd);
        for (Descriptors.FieldDescriptor extension : type.getExtensions()) {
            processExtension(extension, fd, descriptorsByExtensionAndNumber);
        }
        for (Descriptors.Descriptor nestedType : type.getNestedTypes()) {
            processType(nestedType, fd, descriptorsBySymbol, descriptorsByExtensionAndNumber);
        }
    }

    private void processExtension(Descriptors.FieldDescriptor extension, FileDescriptor fd,
            Map<String, Map<Integer, FileDescriptor>> descriptorsByExtensionAndNumber) {
        String extensionName = extension.getContainingType().getFullName();
        int extensionNumber = extension.getNumber();

        descriptorsByExtensionAndNumber.computeIfAbsent(extensionName,
                new Function<String, Map<Integer, FileDescriptor>>() {
                    @Override
                    public Map<Integer, FileDescriptor> apply(String s) {
                        return new HashMap<>();
                    }
                });

        if (descriptorsByExtensionAndNumber.get(extensionName).containsKey(extensionNumber)) {
            throw new IllegalStateException(
                    "Extension name " + extensionName + " and number " + extensionNumber + " are already defined");
        }
        descriptorsByExtensionAndNumber.get(extensionName).put(extensionNumber, fd);
    }

}
