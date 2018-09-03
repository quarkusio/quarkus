package org.jboss.shamrock.beanvalidation.runtime.graal;

import org.hibernate.validator.internal.constraintvalidators.bv.money.MinValidatorForMonetaryAmount;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@Delete()
@TargetClass(MinValidatorForMonetaryAmount.class)
final class MinValidatorForMonetaryAmountDelete {
}
