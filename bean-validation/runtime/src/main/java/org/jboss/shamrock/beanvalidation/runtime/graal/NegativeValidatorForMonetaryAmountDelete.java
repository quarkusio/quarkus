package org.jboss.shamrock.beanvalidation.runtime.graal;

import org.hibernate.validator.internal.constraintvalidators.bv.money.NegativeValidatorForMonetaryAmount;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@Delete()
@TargetClass(NegativeValidatorForMonetaryAmount.class)
final class NegativeValidatorForMonetaryAmountDelete {
}
