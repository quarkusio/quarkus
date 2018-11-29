/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.beanvalidation.runtime.graal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Email;
import javax.validation.constraints.Future;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Negative;
import javax.validation.constraints.NegativeOrZero;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Past;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.CodePointLength;
import org.hibernate.validator.constraints.EAN;
import org.hibernate.validator.constraints.ISBN;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.LuhnCheck;
import org.hibernate.validator.constraints.Mod10Check;
import org.hibernate.validator.constraints.Mod11Check;
import org.hibernate.validator.constraints.ModCheck;
import org.hibernate.validator.constraints.ParameterScriptAssert;
import org.hibernate.validator.constraints.SafeHtml;
import org.hibernate.validator.constraints.ScriptAssert;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.UniqueElements;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;
import org.hibernate.validator.constraints.pl.NIP;
import org.hibernate.validator.constraints.pl.PESEL;
import org.hibernate.validator.constraints.pl.REGON;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.hibernate.validator.internal.constraintvalidators.bv.AssertFalseValidator;
import org.hibernate.validator.internal.constraintvalidators.bv.AssertTrueValidator;
import org.hibernate.validator.internal.constraintvalidators.bv.DecimalMaxValidatorForCharSequence;
import org.hibernate.validator.internal.constraintvalidators.bv.DecimalMinValidatorForCharSequence;
import org.hibernate.validator.internal.constraintvalidators.bv.DigitsValidatorForCharSequence;
import org.hibernate.validator.internal.constraintvalidators.bv.DigitsValidatorForNumber;
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;
import org.hibernate.validator.internal.constraintvalidators.bv.MaxValidatorForCharSequence;
import org.hibernate.validator.internal.constraintvalidators.bv.MinValidatorForCharSequence;
import org.hibernate.validator.internal.constraintvalidators.bv.NotBlankValidator;
import org.hibernate.validator.internal.constraintvalidators.bv.NotNullValidator;
import org.hibernate.validator.internal.constraintvalidators.bv.NullValidator;
import org.hibernate.validator.internal.constraintvalidators.bv.PatternValidator;
import org.hibernate.validator.internal.constraintvalidators.bv.money.DecimalMaxValidatorForMonetaryAmount;
import org.hibernate.validator.internal.constraintvalidators.bv.money.DecimalMinValidatorForMonetaryAmount;
import org.hibernate.validator.internal.constraintvalidators.bv.money.MaxValidatorForMonetaryAmount;
import org.hibernate.validator.internal.constraintvalidators.bv.money.MinValidatorForMonetaryAmount;
import org.hibernate.validator.internal.constraintvalidators.bv.money.NegativeOrZeroValidatorForMonetaryAmount;
import org.hibernate.validator.internal.constraintvalidators.bv.money.NegativeValidatorForMonetaryAmount;
import org.hibernate.validator.internal.constraintvalidators.bv.money.PositiveOrZeroValidatorForMonetaryAmount;
import org.hibernate.validator.internal.constraintvalidators.bv.money.PositiveValidatorForMonetaryAmount;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForArray;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForArraysOfBoolean;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForArraysOfByte;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForArraysOfChar;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForArraysOfDouble;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForArraysOfFloat;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForArraysOfInt;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForArraysOfLong;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForArraysOfShort;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForCharSequence;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForCollection;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForMap;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MaxValidatorForBigDecimal;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MaxValidatorForBigInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MaxValidatorForDouble;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MaxValidatorForFloat;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MaxValidatorForLong;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MaxValidatorForNumber;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MinValidatorForBigDecimal;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MinValidatorForBigInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MinValidatorForDouble;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MinValidatorForFloat;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MinValidatorForLong;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.MinValidatorForNumber;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMaxValidatorForBigDecimal;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMaxValidatorForBigInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMaxValidatorForDouble;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMaxValidatorForFloat;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMaxValidatorForLong;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMaxValidatorForNumber;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMinValidatorForBigDecimal;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMinValidatorForBigInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMinValidatorForDouble;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMinValidatorForFloat;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMinValidatorForLong;
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.DecimalMinValidatorForNumber;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeOrZeroValidatorForBigDecimal;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeOrZeroValidatorForBigInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeOrZeroValidatorForByte;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeOrZeroValidatorForDouble;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeOrZeroValidatorForFloat;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeOrZeroValidatorForInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeOrZeroValidatorForLong;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeOrZeroValidatorForNumber;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeOrZeroValidatorForShort;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeValidatorForBigDecimal;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeValidatorForBigInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeValidatorForByte;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeValidatorForDouble;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeValidatorForFloat;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeValidatorForInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeValidatorForLong;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeValidatorForNumber;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.NegativeValidatorForShort;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveOrZeroValidatorForBigDecimal;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveOrZeroValidatorForBigInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveOrZeroValidatorForByte;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveOrZeroValidatorForDouble;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveOrZeroValidatorForFloat;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveOrZeroValidatorForInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveOrZeroValidatorForLong;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveOrZeroValidatorForNumber;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveOrZeroValidatorForShort;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveValidatorForBigDecimal;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveValidatorForBigInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveValidatorForByte;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveValidatorForDouble;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveValidatorForFloat;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveValidatorForInteger;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveValidatorForLong;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveValidatorForNumber;
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.PositiveValidatorForShort;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForArray;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForArraysOfBoolean;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForArraysOfByte;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForArraysOfChar;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForArraysOfDouble;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForArraysOfFloat;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForArraysOfInt;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForArraysOfLong;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForArraysOfShort;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForCharSequence;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForCollection;
import org.hibernate.validator.internal.constraintvalidators.bv.size.SizeValidatorForMap;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForCalendar;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForHijrahDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForInstant;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForJapaneseDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForLocalDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForLocalDateTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForLocalTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForMinguoDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForMonthDay;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForOffsetDateTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForOffsetTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForThaiBuddhistDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForYear;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForYearMonth;
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.FutureValidatorForZonedDateTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForCalendar;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForHijrahDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForInstant;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForJapaneseDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForLocalDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForLocalDateTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForLocalTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForMinguoDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForMonthDay;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForOffsetDateTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForOffsetTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForThaiBuddhistDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForYear;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForYearMonth;
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.FutureOrPresentValidatorForZonedDateTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForCalendar;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForHijrahDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForInstant;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForJapaneseDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForLocalDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForLocalDateTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForLocalTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForMinguoDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForMonthDay;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForOffsetDateTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForOffsetTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForThaiBuddhistDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForYear;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForYearMonth;
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.PastValidatorForZonedDateTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForCalendar;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForHijrahDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForInstant;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForJapaneseDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForLocalDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForLocalDateTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForLocalTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForMinguoDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForMonthDay;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForOffsetDateTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForOffsetTime;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForThaiBuddhistDate;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForYear;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForYearMonth;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForZonedDateTime;
import org.hibernate.validator.internal.constraintvalidators.hv.CodePointLengthValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.EANValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.ISBNValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.LengthValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.LuhnCheckValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.Mod10CheckValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.Mod11CheckValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.ModCheckValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.ParameterScriptAssertValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.SafeHtmlValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.ScriptAssertValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.URLValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.UniqueElementsValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.br.CNPJValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.br.CPFValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.pl.NIPValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.pl.PESELValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.pl.REGONValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.time.DurationMaxValidator;
import org.hibernate.validator.internal.constraintvalidators.hv.time.DurationMinValidator;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorDescriptor;
import org.hibernate.validator.internal.metadata.core.ConstraintHelper;
import org.hibernate.validator.internal.util.CollectionHelper;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(ConstraintHelper.class)
public final class ConstraintHelperSubstitution {

    @Alias
    public final Map<Class<? extends Annotation>, List<? extends ConstraintValidatorDescriptor<?>>> builtinConstraints;

    @Substitute
    public ConstraintHelperSubstitution () {
        Map<Class<? extends Annotation>, List<ConstraintValidatorDescriptor<?>>> tmpConstraints = new HashMap<>();

        // Bean Validation constraints

        putConstraint( tmpConstraints, AssertFalse.class, AssertFalseValidator.class );
        putConstraint( tmpConstraints, AssertTrue.class, AssertTrueValidator.class );


        putConstraints( tmpConstraints, DecimalMax.class,  Arrays.asList(
                DecimalMaxValidatorForBigDecimal.class,
                DecimalMaxValidatorForBigInteger.class,
                DecimalMaxValidatorForDouble.class,
                DecimalMaxValidatorForFloat.class,
                DecimalMaxValidatorForLong.class,
                DecimalMaxValidatorForNumber.class,
                DecimalMaxValidatorForCharSequence.class,
                DecimalMaxValidatorForMonetaryAmount.class
        ) );
        putConstraints( tmpConstraints, DecimalMin.class, Arrays.asList(
                DecimalMinValidatorForBigDecimal.class,
                DecimalMinValidatorForBigInteger.class,
                DecimalMinValidatorForDouble.class,
                DecimalMinValidatorForFloat.class,
                DecimalMinValidatorForLong.class,
                DecimalMinValidatorForNumber.class,
                DecimalMinValidatorForCharSequence.class,
                DecimalMinValidatorForMonetaryAmount.class
        ));

        putConstraints( tmpConstraints, Digits.class, DigitsValidatorForCharSequence.class, DigitsValidatorForNumber.class );
        putConstraint( tmpConstraints, Email.class, EmailValidator.class );

        List<Class<? extends ConstraintValidator<Future, ?>>> futureValidators = new ArrayList<>( 18 );
        futureValidators.add( FutureValidatorForCalendar.class );
        futureValidators.add( FutureValidatorForDate.class );
        // Java 8 date/time API validators
        futureValidators.add( FutureValidatorForHijrahDate.class );
        futureValidators.add( FutureValidatorForInstant.class );
        futureValidators.add( FutureValidatorForJapaneseDate.class );
        futureValidators.add( FutureValidatorForLocalDate.class );
        futureValidators.add( FutureValidatorForLocalDateTime.class );
        futureValidators.add( FutureValidatorForLocalTime.class );
        futureValidators.add( FutureValidatorForMinguoDate.class );
        futureValidators.add( FutureValidatorForMonthDay.class );
        futureValidators.add( FutureValidatorForOffsetDateTime.class );
        futureValidators.add( FutureValidatorForOffsetTime.class );
        futureValidators.add( FutureValidatorForThaiBuddhistDate.class );
        futureValidators.add( FutureValidatorForYear.class );
        futureValidators.add( FutureValidatorForYearMonth.class );
        futureValidators.add( FutureValidatorForZonedDateTime.class );

        putConstraints( tmpConstraints, Future.class, futureValidators );

        List<Class<? extends ConstraintValidator<FutureOrPresent, ?>>> futureOrPresentValidators = new ArrayList<>( 18 );
        futureOrPresentValidators.add( FutureOrPresentValidatorForCalendar.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForDate.class );
        // Java 8 date/time API validators
        futureOrPresentValidators.add( FutureOrPresentValidatorForHijrahDate.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForInstant.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForJapaneseDate.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForLocalDate.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForLocalDateTime.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForLocalTime.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForMinguoDate.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForMonthDay.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForOffsetDateTime.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForOffsetTime.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForThaiBuddhistDate.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForYear.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForYearMonth.class );
        futureOrPresentValidators.add( FutureOrPresentValidatorForZonedDateTime.class );

        putConstraints( tmpConstraints, FutureOrPresent.class, futureOrPresentValidators );

        putConstraint( tmpConstraints, ISBN.class, ISBNValidator.class );

        putConstraints( tmpConstraints, Max.class, Arrays.asList(
                MaxValidatorForBigDecimal.class,
                MaxValidatorForBigInteger.class,
                MaxValidatorForDouble.class,
                MaxValidatorForFloat.class,
                MaxValidatorForLong.class,
                MaxValidatorForNumber.class,
                MaxValidatorForCharSequence.class,
                MaxValidatorForMonetaryAmount.class
        ) );
        putConstraints( tmpConstraints, Min.class, Arrays.asList(
                MinValidatorForBigDecimal.class,
                MinValidatorForBigInteger.class,
                MinValidatorForDouble.class,
                MinValidatorForFloat.class,
                MinValidatorForLong.class,
                MinValidatorForNumber.class,
                MinValidatorForCharSequence.class,
                MinValidatorForMonetaryAmount.class
        ) );

        putConstraints( tmpConstraints, Negative.class, Arrays.asList(
                NegativeValidatorForBigDecimal.class,
                NegativeValidatorForBigInteger.class,
                NegativeValidatorForDouble.class,
                NegativeValidatorForFloat.class,
                NegativeValidatorForLong.class,
                NegativeValidatorForInteger.class,
                NegativeValidatorForShort.class,
                NegativeValidatorForByte.class,
                NegativeValidatorForNumber.class,
                NegativeValidatorForMonetaryAmount.class ) );

        putConstraints( tmpConstraints, NegativeOrZero.class, Arrays.asList(
                NegativeOrZeroValidatorForBigDecimal.class,
                NegativeOrZeroValidatorForBigInteger.class,
                NegativeOrZeroValidatorForDouble.class,
                NegativeOrZeroValidatorForFloat.class,
                NegativeOrZeroValidatorForLong.class,
                NegativeOrZeroValidatorForInteger.class,
                NegativeOrZeroValidatorForShort.class,
                NegativeOrZeroValidatorForByte.class,
                NegativeOrZeroValidatorForNumber.class,
                NegativeOrZeroValidatorForMonetaryAmount.class ) );

        putConstraint( tmpConstraints, NotBlank.class, NotBlankValidator.class );

        List<Class<? extends ConstraintValidator<NotEmpty, ?>>> notEmptyValidators = new ArrayList<>( 11 );
        notEmptyValidators.add( NotEmptyValidatorForCharSequence.class );
        notEmptyValidators.add( NotEmptyValidatorForCollection.class );
        notEmptyValidators.add( NotEmptyValidatorForArray.class );
        notEmptyValidators.add( NotEmptyValidatorForMap.class );
        notEmptyValidators.add( NotEmptyValidatorForArraysOfBoolean.class );
        notEmptyValidators.add( NotEmptyValidatorForArraysOfByte.class );
        notEmptyValidators.add( NotEmptyValidatorForArraysOfChar.class );
        notEmptyValidators.add( NotEmptyValidatorForArraysOfDouble.class );
        notEmptyValidators.add( NotEmptyValidatorForArraysOfFloat.class );
        notEmptyValidators.add( NotEmptyValidatorForArraysOfInt.class );
        notEmptyValidators.add( NotEmptyValidatorForArraysOfLong.class );
        notEmptyValidators.add( NotEmptyValidatorForArraysOfShort.class );
        putConstraints( tmpConstraints, NotEmpty.class, notEmptyValidators );

        putConstraint( tmpConstraints, NotNull.class, NotNullValidator.class );
        putConstraint( tmpConstraints, Null.class, NullValidator.class );

        List<Class<? extends ConstraintValidator<Past, ?>>> pastValidators = new ArrayList<>( 18 );
        pastValidators.add( PastValidatorForCalendar.class );
        pastValidators.add( PastValidatorForDate.class );
        // Java 8 date/time API validators
        pastValidators.add( PastValidatorForHijrahDate.class );
        pastValidators.add( PastValidatorForInstant.class );
        pastValidators.add( PastValidatorForJapaneseDate.class );
        pastValidators.add( PastValidatorForLocalDate.class );
        pastValidators.add( PastValidatorForLocalDateTime.class );
        pastValidators.add( PastValidatorForLocalTime.class );
        pastValidators.add( PastValidatorForMinguoDate.class );
        pastValidators.add( PastValidatorForMonthDay.class );
        pastValidators.add( PastValidatorForOffsetDateTime.class );
        pastValidators.add( PastValidatorForOffsetTime.class );
        pastValidators.add( PastValidatorForThaiBuddhistDate.class );
        pastValidators.add( PastValidatorForYear.class );
        pastValidators.add( PastValidatorForYearMonth.class );
        pastValidators.add( PastValidatorForZonedDateTime.class );

        putConstraints( tmpConstraints, Past.class, pastValidators );

        List<Class<? extends ConstraintValidator<PastOrPresent, ?>>> pastOrPresentValidators = new ArrayList<>( 18 );
        pastOrPresentValidators.add( PastOrPresentValidatorForCalendar.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForDate.class );
        // Java 8 date/time API validators
        pastOrPresentValidators.add( PastOrPresentValidatorForHijrahDate.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForInstant.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForJapaneseDate.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForLocalDate.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForLocalDateTime.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForLocalTime.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForMinguoDate.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForMonthDay.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForOffsetDateTime.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForOffsetTime.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForThaiBuddhistDate.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForYear.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForYearMonth.class );
        pastOrPresentValidators.add( PastOrPresentValidatorForZonedDateTime.class );

        putConstraints( tmpConstraints, PastOrPresent.class, pastOrPresentValidators );

        putConstraint( tmpConstraints, Pattern.class, PatternValidator.class );

        putConstraints( tmpConstraints, Positive.class, Arrays.asList(
                PositiveValidatorForBigDecimal.class,
                PositiveValidatorForBigInteger.class,
                PositiveValidatorForDouble.class,
                PositiveValidatorForFloat.class,
                PositiveValidatorForLong.class,
                PositiveValidatorForInteger.class,
                PositiveValidatorForShort.class,
                PositiveValidatorForByte.class,
                PositiveValidatorForNumber.class,
                PositiveValidatorForMonetaryAmount.class ) );

        putConstraints( tmpConstraints, PositiveOrZero.class, Arrays.asList(
                PositiveOrZeroValidatorForBigDecimal.class,
                PositiveOrZeroValidatorForBigInteger.class,
                PositiveOrZeroValidatorForDouble.class,
                PositiveOrZeroValidatorForFloat.class,
                PositiveOrZeroValidatorForLong.class,
                PositiveOrZeroValidatorForInteger.class,
                PositiveOrZeroValidatorForShort.class,
                PositiveOrZeroValidatorForByte.class,
                PositiveOrZeroValidatorForNumber.class,
                PositiveOrZeroValidatorForMonetaryAmount.class ) );

        List<Class<? extends ConstraintValidator<Size, ?>>> sizeValidators = new ArrayList<>( 11 );
        sizeValidators.add( SizeValidatorForCharSequence.class );
        sizeValidators.add( SizeValidatorForCollection.class );
        sizeValidators.add( SizeValidatorForArray.class );
        sizeValidators.add( SizeValidatorForMap.class );
        sizeValidators.add( SizeValidatorForArraysOfBoolean.class );
        sizeValidators.add( SizeValidatorForArraysOfByte.class );
        sizeValidators.add( SizeValidatorForArraysOfChar.class );
        sizeValidators.add( SizeValidatorForArraysOfDouble.class );
        sizeValidators.add( SizeValidatorForArraysOfFloat.class );
        sizeValidators.add( SizeValidatorForArraysOfInt.class );
        sizeValidators.add( SizeValidatorForArraysOfLong.class );
        sizeValidators.add( SizeValidatorForArraysOfShort.class );
        putConstraints( tmpConstraints, Size.class, sizeValidators );

        // Hibernate Validator specific constraints

        putConstraint( tmpConstraints, CNPJ.class, CNPJValidator.class );
        putConstraint( tmpConstraints, CPF.class, CPFValidator.class );
        putConstraint( tmpConstraints, DurationMax.class, DurationMaxValidator.class );
        putConstraint( tmpConstraints, DurationMin.class, DurationMinValidator.class );
        putConstraint( tmpConstraints, EAN.class, EANValidator.class );
        putConstraint( tmpConstraints, org.hibernate.validator.constraints.Email.class, org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator.class );
        putConstraint( tmpConstraints, Length.class, LengthValidator.class );
        putConstraint( tmpConstraints, CodePointLength.class, CodePointLengthValidator.class );
        putConstraint( tmpConstraints, ModCheck.class, ModCheckValidator.class );
        putConstraint( tmpConstraints, LuhnCheck.class, LuhnCheckValidator.class );
        putConstraint( tmpConstraints, Mod10Check.class, Mod10CheckValidator.class );
        putConstraint( tmpConstraints, Mod11Check.class, Mod11CheckValidator.class );
        putConstraint( tmpConstraints, REGON.class, REGONValidator.class );
        putConstraint( tmpConstraints, NIP.class, NIPValidator.class );
        putConstraint( tmpConstraints, PESEL.class, PESELValidator.class );
        putConstraint( tmpConstraints, org.hibernate.validator.constraints.NotBlank.class, org.hibernate.validator.internal.constraintvalidators.hv.NotBlankValidator.class );
        putConstraint( tmpConstraints, ParameterScriptAssert.class, ParameterScriptAssertValidator.class );
        putConstraint( tmpConstraints, SafeHtml.class, SafeHtmlValidator.class );
        putConstraint( tmpConstraints, ScriptAssert.class, ScriptAssertValidator.class );
        putConstraint( tmpConstraints, UniqueElements.class, UniqueElementsValidator.class );
        putConstraint( tmpConstraints, URL.class, URLValidator.class );

        this.builtinConstraints = Collections.unmodifiableMap( tmpConstraints );
    }

    @Substitute
    private void assertValidationAppliesToParameterSetUpCorrectly(Class<? extends Annotation> annotationType) {


    }

    @Substitute
    private void assertPayloadParameterExists(Class<? extends Annotation> annotationType) {
    }


    @Substitute
    private void assertNoParameterStartsWithValid(Class<? extends Annotation> annotationType) {
    }


    @Substitute
    private void assertGroupsParameterExists(Class<? extends Annotation> annotationType) {
    }

    @Substitute
    private void assertMessageParameterExists(Class<? extends Annotation> annotationType) {
    }

    @Alias
    private static <A extends Annotation> void putConstraint(Map<Class<? extends Annotation>, List<ConstraintValidatorDescriptor<?>>> validators,
            Class<A> constraintType, Class<? extends ConstraintValidator<A, ?>> validatorType) {
        validators.put( constraintType, Collections.singletonList( ConstraintValidatorDescriptor.forClass( validatorType, constraintType ) ) );
    }

    @Alias
    private static <A extends Annotation> void putConstraints(Map<Class<? extends Annotation>, List<ConstraintValidatorDescriptor<?>>> validators,
            Class<A> constraintType, Class<? extends ConstraintValidator<A, ?>> validatorType1, Class<? extends ConstraintValidator<A, ?>> validatorType2) {
        List<ConstraintValidatorDescriptor<?>> descriptors = new ArrayList<>( 2 );

        descriptors.add( ConstraintValidatorDescriptor.forClass( validatorType1, constraintType ) );
        descriptors.add( ConstraintValidatorDescriptor.forClass( validatorType2, constraintType ) );

        validators.put( constraintType, CollectionHelper.toImmutableList( descriptors ) );
    }

    @Alias
    private static <A extends Annotation> void putConstraints(Map<Class<? extends Annotation>, List<ConstraintValidatorDescriptor<?>>> validators,
            Class<A> constraintType, List<Class<? extends ConstraintValidator<A, ?>>> validatorTypes) {
        List<ConstraintValidatorDescriptor<?>> descriptors = new ArrayList<>( validatorTypes.size() );

        for ( Class<? extends ConstraintValidator<A, ?>> validatorType : validatorTypes ) {
            descriptors.add( ConstraintValidatorDescriptor.forClass( validatorType, constraintType ) );
        }

        validators.put( constraintType, CollectionHelper.toImmutableList( descriptors ) );
    }
}