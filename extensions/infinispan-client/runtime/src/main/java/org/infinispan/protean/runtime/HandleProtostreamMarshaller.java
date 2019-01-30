package org.infinispan.protean.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.impl.MarshallerRegistration;

/**
 * This class handles loading protostream marshaller. No other current class should reference classes from the
 * protostream or infinispan-remote-query-client modules besides this one.
 * @author William Burns
 */
class HandleProtostreamMarshaller {

   static void handleQueryRequirements(Properties properties) {
      properties.put(InfinispanClientProducer.PROTOBUF_FILE_PREFIX + MarshallerRegistration.QUERY_PROTO_RES,
            getContents(MarshallerRegistration.QUERY_PROTO_RES));
      properties.put(InfinispanClientProducer.PROTOBUF_FILE_PREFIX + MarshallerRegistration.MESSAGE_PROTO_RES,
            getContents(MarshallerRegistration.MESSAGE_PROTO_RES));
   }

   private static String getContents(String fileName) {
      InputStream stream = HandleProtostreamMarshaller.class.getResourceAsStream(fileName);
      return new Scanner(stream).useDelimiter("\\A").next();
   }

   static void handlePossibleMarshaller(Object marshallerInstance, Properties properties, BeanManager beanManager) {
      ProtoStreamMarshaller marshaller = (ProtoStreamMarshaller) marshallerInstance;
      SerializationContext serializationContext = marshaller.getSerializationContext();

      FileDescriptorSource fileDescriptorSource = null;
      for (Map.Entry<Object, Object> property : properties.entrySet()) {
         Object key = property.getKey();
         if (key instanceof String) {
            String keyString = (String) key;
            if (keyString.startsWith(InfinispanClientProducer.PROTOBUF_FILE_PREFIX)) {
               String fileName = keyString.substring(InfinispanClientProducer.PROTOBUF_FILE_PREFIX.length());
               String fileContents = (String) property.getValue();
               if (fileDescriptorSource == null) {
                  fileDescriptorSource = new FileDescriptorSource();
               }
               fileDescriptorSource.addProtoFile(fileName, fileContents);
            }
         }
      }

      try {
         if (fileDescriptorSource != null) {
            serializationContext.registerProtoFiles(fileDescriptorSource);
         }

         Set<Bean<FileDescriptorSource>> protoFileBeans = (Set) beanManager.getBeans(FileDescriptorSource.class);
         for (Bean<FileDescriptorSource> bean : protoFileBeans) {
            CreationalContext<FileDescriptorSource> ctx = beanManager.createCreationalContext(bean);
            FileDescriptorSource fds = (FileDescriptorSource) beanManager.getReference(bean, FileDescriptorSource.class, ctx);
            serializationContext.registerProtoFiles(fds);
         }
      } catch (IOException e) {
         // TODO: pick a better exception
         throw new RuntimeException(e);
      }

      Set<Bean<MessageMarshaller>> beans = (Set) beanManager.getBeans(MessageMarshaller.class);
      for (Bean<MessageMarshaller> bean : beans) {
         CreationalContext<MessageMarshaller> ctx = beanManager.createCreationalContext(bean);
         MessageMarshaller messageMarshaller = (MessageMarshaller) beanManager.getReference(bean, MessageMarshaller.class, ctx);
         serializationContext.registerMarshaller(messageMarshaller);
      }
   }
}
