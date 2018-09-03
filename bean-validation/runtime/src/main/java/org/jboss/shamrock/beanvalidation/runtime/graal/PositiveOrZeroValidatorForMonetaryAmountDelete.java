package org.jboss.shamrock.beanvalidation.runtime.graal;

import org.hibernate.validator.internal.constraintvalidators.bv.money.PositiveOrZeroValidatorForMonetaryAmount;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@Delete()
@TargetClass(PositiveOrZeroValidatorForMonetaryAmount.class)
final class PositiveOrZeroValidatorForMonetaryAmountDelete {
}
