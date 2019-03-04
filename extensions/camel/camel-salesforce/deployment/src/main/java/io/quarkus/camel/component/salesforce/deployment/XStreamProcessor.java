package io.quarkus.camel.component.salesforce.deployment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.mapper.AnnotationConfiguration;
import com.thoughtworks.xstream.mapper.CGLIBMapper;
import com.thoughtworks.xstream.mapper.Mapper;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;

public class XStreamProcessor {

    private static final String[] INTERFACES_TO_REGISTER = {
            Converter.class.getName(),
            Mapper.class.getName()
    };

    private static final Set<String> EXCLUDED_CLASSES = new HashSet<>(Arrays.asList(CGLIBMapper.class.getName()));

    @BuildStep(applicationArchiveMarkers = "com/thoughtworks/xstream/XStream.class")
    void process(CombinedIndexBuildItem indexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer) {
        for (String className : INTERFACES_TO_REGISTER) {
            for (ClassInfo i : indexBuildItem.getIndex().getAllKnownImplementors(DotName.createSimple(className))) {
                String name = i.name().toString();
                if (!EXCLUDED_CLASSES.contains(name)) {
                    reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(false, false, name));
                }
            }
        }
        reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(false, false,
                Mapper.class,
                Mapper.Null.class,
                ConverterRegistry.class,
                ConverterLookup.class,
                ClassLoaderReference.class,
                ReflectionProvider.class,
                AnnotationConfiguration.class));

        reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(false, false,
                Boolean.class,
                Byte.class,
                Character.class,
                Short.class,
                Integer.class,
                Long.class,
                Float.class,
                Double.class,
                java.lang.Void.class,
                java.lang.Object.class));

        reflectiveClassBuildItemBuildProducer
                .produce(new ReflectiveClassBuildItem(false, false, false, Class.class, ClassLoader.class));

        reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(false, false,
                "com.thoughtworks.xstream.core.JVM$Test",
                "com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider",
                "com.thoughtworks.xstream.core.JVM",
                "com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider",
                "com.thoughtworks.xstream.converters.reflection.FieldUtil15",
                "java.lang.Void",
                "java.lang.Object",
                "com.thoughtworks.xstream.core.util.CustomObjectOutputStream",
                "java.awt.Color",
                "javax.swing.LookAndFeel",
                "java.sql.Date",
                "com.thoughtworks.xstream.core.util.Base64JavaUtilCodec",
                "com.thoughtworks.xstream.security.AnyTypePermission",
                "java.lang.Number",
                "java.math.BigInteger",
                "java.math.BigDecimal",
                "java.lang.StringBuffer",
                "java.lang.String",
                "java.lang.reflect.Method",
                "java.lang.reflect.Constructor",
                "java.lang.reflect.Field",
                "java.util.Date",
                "java.net.URI",
                "java.net.URL",
                "java.util.BitSet",
                "java.util.Map",
                "java.util.Map$Entry",
                "java.util.Properties",
                "java.util.List",
                "java.util.Set",
                "java.util.SortedSet",
                "java.util.LinkedList",
                "java.util.Vector",
                "java.util.TreeMap",
                "java.util.TreeSet",
                "java.util.Hashtable",
                "java.awt.Font",
                "java.awt.font.TextAttribute",
                "javax.activation.ActivationDataFlavor",
                "java.sql.Timestamp",
                "java.sql.Time",
                "java.io.File",
                "java.util.Locale",
                "java.util.Calendar",
                "javax.security.auth.Subject",
                "java.util.LinkedHashMap",
                "java.util.LinkedHashSet",
                "java.lang.StackTraceElement",
                "java.util.Currency",
                "java.nio.charset.Charset",
                "javax.xml.datatype.Duration",
                "java.util.concurrent.ConcurrentHashMap",
                "java.util.EnumSet",
                "java.util.EnumMap",
                "java.lang.StringBuilder",
                "java.util.UUID",
                "java.nio.file.Path",
                "java.time.Clock$FixedClock",
                "java.time.Clock$OffsetClock",
                "java.time.Clock$SystemClock",
                "java.time.Clock$TickClock",
                "java.time.DayOfWeek",
                "java.time.Duration",
                "java.time.Instant",
                "java.time.LocalDate",
                "java.time.LocalDateTime",
                "java.time.LocalTime",
                "java.time.Month",
                "java.time.MonthDay",
                "java.time.OffsetDateTime",
                "java.time.OffsetTime",
                "java.time.Period",
                "java.time.Year",
                "java.time.YearMonth",
                "java.time.ZonedDateTime",
                "java.time.ZoneId",
                "java.time.chrono.Chronology",
                "java.time.chrono.HijrahDate",
                "java.time.chrono.HijrahEra",
                "java.time.chrono.JapaneseDate",
                "java.time.chrono.JapaneseEra",
                "java.time.chrono.MinguoDate",
                "java.time.chrono.MinguoEra",
                "java.time.chrono.ThaiBuddhistDate",
                "java.time.chrono.ThaiBuddhistEra",
                "java.time.temporal.ChronoField",
                "java.time.temporal.ChronoUnit",
                "java.time.temporal.IsoFields$Field",
                "java.time.temporal.IsoFields$Unit",
                "java.time.temporal.JulianFields$Field",
                "java.time.temporal.ValueRange",
                "java.time.temporal.WeekFields",
                "java.lang.invoke.SerializedLambda",
                "java.util.HashMap",
                "java.util.ArrayList",
                "java.util.HashSet",
                "java.util.GregorianCalendar",
                "java.util.Comparator",
                "java.util.SortedMap",
                "java.util.Collection",
                "java.lang.reflect.Proxy",
                "java.lang.reflect.InvocationHandler",
                "java.text.AttributedCharacterIterator$Attribute",
                "com.thoughtworks.xstream.converters.extended.StackTraceElementConverter",
                "com.thoughtworks.xstream.converters.extended.StackTraceElementFactory15",
                "com.thoughtworks.xstream.converters.extended.CurrencyConverter",
                "com.thoughtworks.xstream.converters.extended.CharsetConverter",
                "com.thoughtworks.xstream.converters.extended.DurationConverter",
                "com.thoughtworks.xstream.converters.basic.StringBuilderConverter",
                "com.thoughtworks.xstream.converters.basic.UUIDConverter",
                "com.thoughtworks.xstream.converters.extended.PathConverter",
                "com.thoughtworks.xstream.converters.time.ChronologyConverter",
                "com.thoughtworks.xstream.converters.time.DurationConverter",
                "com.thoughtworks.xstream.converters.time.HijrahDateConverter",
                "com.thoughtworks.xstream.converters.time.JapaneseDateConverter",
                "com.thoughtworks.xstream.converters.time.JapaneseEraConverter",
                "com.thoughtworks.xstream.converters.time.InstantConverter",
                "com.thoughtworks.xstream.converters.time.LocalDateConverter",
                "com.thoughtworks.xstream.converters.time.LocalDateTimeConverter",
                "com.thoughtworks.xstream.converters.time.LocalTimeConverter",
                "com.thoughtworks.xstream.converters.time.MinguoDateConverter",
                "com.thoughtworks.xstream.converters.time.MonthDayConverter",
                "com.thoughtworks.xstream.converters.time.OffsetDateTimeConverter",
                "com.thoughtworks.xstream.converters.time.OffsetTimeConverter",
                "com.thoughtworks.xstream.converters.time.PeriodConverter",
                "com.thoughtworks.xstream.converters.time.ThaiBuddhistDateConverter",
                "com.thoughtworks.xstream.converters.time.YearConverter",
                "com.thoughtworks.xstream.converters.time.YearMonthConverter",
                "com.thoughtworks.xstream.converters.time.ZonedDateTimeConverter",
                "com.thoughtworks.xstream.converters.time.ZoneIdConverter",
                "java.nio.file.Paths",
                "[Ljava.lang.String;",
                "java.time.ZoneOffset",
                "java.time.ZoneRegion",
                "java.time.chrono.HijrahChronology",
                "java.time.chrono.IsoChronology",
                "java.time.chrono.JapaneseChronology",
                "java.time.chrono.MinguoChronology",
                "java.time.chrono.ThaiBuddhistChronology"));

    }
}
