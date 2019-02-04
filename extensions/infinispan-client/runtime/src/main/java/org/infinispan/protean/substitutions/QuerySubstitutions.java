package org.infinispan.protean.substitutions;

import java.io.IOException;
import java.util.function.BooleanSupplier;

import org.infinispan.protean.runtime.InfinispanClientProducer;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.impl.MarshallerRegistration;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Class that has all the query substitutions necessary to remove code that is loaded when proto marshaller is in use
 * @author William Burns
 */
final class QuerySubstitutions { }

@TargetClass(value = InfinispanClientProducer.class, onlyWith = Selector.class)
final class SubstituteInfinispanClientProducer {
   @Substitute
   private void injectProtoMarshallers(Object marshallerInstance) {
      // Don't even attempt proto stream updates
   }
}

final class Selector implements BooleanSupplier {
   @Override
   public boolean getAsBoolean() {
      try {
         Class.forName(InfinispanClientProducer.PROTOBUF_MARSHALLER_CLASS_NAME);
         return false;
      } catch (ClassNotFoundException | NoClassDefFoundError e) {
         // If the classes aren't found we have to remove the places that reference it
         return true;
      }
   }
}

@TargetClass(value = MarshallerRegistration.class)
final class SubstituteMarshallerRegistration {
   @Substitute
   public static void init(SerializationContext ctx) throws IOException {
      // Skip loading the proto definition files as this was already done at compile time with
      // HandleProtostreamMarshaller#handleQueryRequirements
      MarshallerRegistration.registerMarshallers(ctx);
   }
}