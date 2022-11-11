package io.quarkus.hibernate.validator.runtime.graal;

import java.util.function.BooleanSupplier;

import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForReadableInstant;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForReadablePartial;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForReadableInstant;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForReadablePartial;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForReadableInstant;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForReadablePartial;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForReadableInstant;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForReadablePartial;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(value = PastValidatorForReadablePartial.class, onlyWith = JodaTimeUnavailable.class)
@Delete
final class DeletePastValidatorForReadablePartial {

}

@TargetClass(value = PastValidatorForReadableInstant.class, onlyWith = JodaTimeUnavailable.class)
@Delete
final class DeletePastValidatorForReadableInstant {

}

@TargetClass(value = FutureValidatorForReadablePartial.class, onlyWith = JodaTimeUnavailable.class)
@Delete
final class DeleteFutureValidatorForReadablePartial {

}

@TargetClass(value = FutureValidatorForReadableInstant.class, onlyWith = JodaTimeUnavailable.class)
@Delete
final class DeleteFutureValidatorForReadableInstant {

}

@TargetClass(value = PastOrPresentValidatorForReadablePartial.class, onlyWith = JodaTimeUnavailable.class)
@Delete
final class DeletePastOrPresentValidatorForReadablePartial {

}

@TargetClass(value = PastOrPresentValidatorForReadableInstant.class, onlyWith = JodaTimeUnavailable.class)
@Delete
final class DeletePastOrPresentValidatorForReadableInstant {

}

@TargetClass(value = FutureOrPresentValidatorForReadablePartial.class, onlyWith = JodaTimeUnavailable.class)
@Delete
final class DeleteFutureOrPresentValidatorForReadablePartial {

}

@TargetClass(value = FutureOrPresentValidatorForReadableInstant.class, onlyWith = JodaTimeUnavailable.class)
@Delete
final class DeleteFutureOrPresentValidatorForReadableInstant {

}

final class JodaTimeUnavailable implements BooleanSupplier {
    private boolean jodaTimeUnavailable;

    public JodaTimeUnavailable() {
        try {
            Class.forName("org.joda.time.Instant");
            jodaTimeUnavailable = false;
        } catch (ClassNotFoundException e) {
            jodaTimeUnavailable = true;
        }
    }

    @Override
    public boolean getAsBoolean() {
        return jodaTimeUnavailable;
    }
}

@SuppressWarnings("unused")
class HibernateValidatorSubstitutions {
}
