package io.quarkus.kogito.deployment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.kie.kogito.codegen.process.persistence.proto.Proto;
import org.kie.kogito.codegen.process.persistence.proto.ProtoGenerator;
import org.kie.kogito.codegen.process.persistence.proto.ProtoMessage;

public class JandexProtoGenerator implements ProtoGenerator<ClassInfo> {

    private final IndexView index;
    private final DotName generatedAnnotation;

    public JandexProtoGenerator(IndexView index, DotName generatedAnnotation) {
        this.index = index;
        this.generatedAnnotation = generatedAnnotation;
    }

    public Proto generate(String packageName, Collection<ClassInfo> dataModel, String... headers) {
        try {
            Proto proto = new Proto(packageName, headers);

            for (ClassInfo clazz : dataModel) {
                messageFromClass(proto, clazz, index, null, null, null);
            }
            return proto;
        } catch (Exception e) {
            throw new RuntimeException("Error while generating proto for data model", e);
        }
    }

    @Override
    public Proto generate(String messageComment, String fieldComment, String packageName, ClassInfo dataModel,
            String... headers) {
        try {
            Proto proto = new Proto(packageName, headers);

            messageFromClass(proto, dataModel, index, packageName, messageComment, fieldComment);
            return proto;
        } catch (Exception e) {
            throw new RuntimeException("Error while generating proto for data model", e);
        }
    }

    protected ProtoMessage messageFromClass(Proto proto, ClassInfo clazz, IndexView index, String packageName,
            String messageComment, String fieldComment)
            throws Exception {

        String name = clazz.simpleName();
        String altName = getReferenceOfModel(clazz, "name");
        if (altName != null) {

            name = altName;
        }
        ProtoMessage message = new ProtoMessage(name, packageName == null ? clazz.name().prefix().toString() : packageName);

        for (FieldInfo pd : clazz.fields()) {

            String fieldTypeString = pd.type().name().toString();

            DotName fieldType = pd.type().name();
            if (pd.type().kind() == Kind.PARAMETERIZED_TYPE) {
                fieldTypeString = "Collection";

                List<Type> typeParameters = pd.type().asParameterizedType().arguments();
                if (typeParameters.isEmpty()) {
                    throw new IllegalArgumentException("Field " + pd.name() + " of class " + clazz.name().toString()
                            + " uses collection without type information");
                }
                fieldType = typeParameters.get(0).name();
            }
            String protoType = protoType(fieldTypeString);

            if (protoType == null) {
                ProtoMessage another = messageFromClass(proto, index.getClassByName(fieldType), index, packageName,
                        messageComment, fieldComment);
                protoType = another.getName();
            }

            message.addField(applicabilityByType(fieldTypeString), protoType, pd.name()).setComment(fieldComment);
        }
        message.setComment(messageComment);
        proto.addMessage(message);
        return message;
    }

    @Override
    public Collection<ClassInfo> extractDataClasses(Collection<ClassInfo> input, String targetDirectory) {
        Set<ClassInfo> dataModelClasses = new HashSet<>();
        for (ClassInfo modelClazz : input) {
            try {
                for (FieldInfo pd : modelClazz.fields()) {

                    if (pd.type().name().toString().startsWith("java.lang")
                            || pd.type().name().toString().equals(Date.class.getCanonicalName())) {
                        continue;
                    }

                    dataModelClasses.add(index.getClassByName(pd.type().name()));
                }

                generateModelClassProto(modelClazz, targetDirectory);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return dataModelClasses;
    }

    protected void generateModelClassProto(ClassInfo modelClazz, String targetDirectory) throws Exception {

        String processId = getReferenceOfModel(modelClazz, "reference");
        String name = getReferenceOfModel(modelClazz, "name");

        if (processId != null) {

            Proto modelProto = generate("@Indexed",
                    "@Field(store = Store.YES, analyze = Analyze.YES)",
                    modelClazz.name().prefix().toString() + "." + processId, modelClazz,
                    "import \"kogito-index.proto\";",
                    "import \"kogito-types.proto\";",
                    "option kogito_model = \"" + name + "\";",
                    "option kogito_id = \"" + processId + "\";");

            ProtoMessage modelMessage = modelProto.getMessages().stream().filter(msg -> msg.getName().equals(name)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unable to find model message"));
            modelMessage.addField("repeated", "org.kie.kogito.index.model.ProcessInstanceMeta", "processInstances")
                    .setComment("@Field(store = Store.YES, analyze = Analyze.YES)");
            modelMessage.addField("repeated", "org.kie.kogito.index.model.UserTaskInstanceMeta", "userTasks")
                    .setComment("@Field(store = Store.YES, analyze = Analyze.YES)");

            Path protoFilePath = Paths.get(targetDirectory, "classes", "/persistence/" + processId + ".proto");

            Files.createDirectories(protoFilePath.getParent());
            Files.write(protoFilePath, modelProto.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    protected String getReferenceOfModel(ClassInfo modelClazz, String name) {
        AnnotationInstance generatedData = modelClazz.classAnnotation(generatedAnnotation);

        if (generatedData != null) {

            return generatedData.value(name).asString();
        }

        return null;
    }

}
