package io.quarkus.qute.deployment.templatelocator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.All;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Locate;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.TemplateLocator;
import io.quarkus.qute.Variant;
import io.quarkus.test.QuarkusUnitTest;

public class CustomTemplateLocatorTest {

    private static final String TEMPLATE_LOCATION_1 = "/path/to/my/custom/template/basic.html";
    private static final String TEMPLATE_LOCATION_2 = "/second/path/to/my/custom/template/regular.html";
    private static final String TEMPLATE_LOCATION_3 = "/third/path/to/my/custom/template/custom.html";
    private static final String TEMPLATE_LOCATION_4 = "/fourth/path/to/my/custom/template/custom.html";
    private static final String TEMPLATE_LOCATION_5 = "/fifth/path/to/my/custom/template/custom.html";
    private static final String CHECKED_TEMPLATE_LOCATION = "my_checked_template_base/myCheckedTemplate";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(CustomLocator1.class, CustomLocator2.class, CustomLocator3.class,
                            TemplateValueProvider.class));

    @Location(TEMPLATE_LOCATION_1)
    Template template1;

    @Location(TEMPLATE_LOCATION_2)
    Template template2;

    @Location(TEMPLATE_LOCATION_3)
    Template template3;

    @Location(TEMPLATE_LOCATION_4)
    Template template4;

    @Location(TEMPLATE_LOCATION_5)
    Template template5;

    @All
    @Inject
    List<TemplateLocator> locatorList;

    @Test
    public void testCustomLocatorRegistration1() {
        assertEquals("Basic Qute Template!", template1.data("name", "Qute Template").render());
    }

    @Test
    public void testCustomLocatorRegistration2() {
        assertEquals("Regular test template data!", template2.data("name", "test template data").render());
    }

    @Test
    public void testCustomLocatorRegistration3() {
        assertEquals("Custom name!", template3.data("name", "name").render());
    }

    @Test
    public void testCustomLocatorRegistration4() {
        assertEquals("Custom template 4 customName!", template4.data("name", "customName").render());
    }

    @Test
    public void testCustomLocatorRegistration5() {
        assertEquals("Custom template 5 customName!", template5.data("name", "customName").render());
    }

    @Test
    public void testCheckedTemplate() {
        assertEquals("My Checked Template Number: 3", Templates.myCheckedTemplate(3).render());
    }

    @Test
    public void testLocatorsAreRegisteredAsSingletons() {
        assertEquals(4, locatorList.size());
    }

    @Singleton
    public static class TemplateValueProvider {

        public String getTemplateValue() {
            return "Basic {name}!";
        }
    }

    @Locate(TEMPLATE_LOCATION_1)
    @Locate(TEMPLATE_LOCATION_4)
    @Locate(TEMPLATE_LOCATION_5)
    public static class CustomLocator1 implements TemplateLocator {

        @Inject
        TemplateValueProvider templateValueProvider;

        @Override
        public Optional<TemplateLocation> locate(String s) {

            if (s.equals(TEMPLATE_LOCATION_1) || s.equals(TEMPLATE_LOCATION_4) || s.equals(TEMPLATE_LOCATION_5)) {
                return Optional.of(new TemplateLocation() {

                    @Override
                    public Reader read() {
                        final String value;
                        switch (s) {
                            case TEMPLATE_LOCATION_1:
                                value = templateValueProvider.getTemplateValue();
                                break;
                            case TEMPLATE_LOCATION_4:
                                value = "Custom template 4 {name}!";
                                break;
                            case TEMPLATE_LOCATION_5:
                                value = "Custom template 5 {name}!";
                                break;
                            default:
                                value = "";
                                break;
                        }
                        return new StringReader(value);
                    }

                    @Override
                    public Optional<Variant> getVariant() {
                        return Optional.empty();
                    }
                });
            }
            return Optional.empty();
        }

    }

    @Locate(TEMPLATE_LOCATION_2)
    public static class CustomLocator2 implements TemplateLocator {

        @Override
        public Optional<TemplateLocation> locate(String s) {

            if (s.equals(TEMPLATE_LOCATION_2)) {
                return Optional.of(new TemplateLocation() {

                    @Override
                    public Reader read() {
                        return new StringReader("Regular {name}!");
                    }

                    @Override
                    public Optional<Variant> getVariant() {
                        return Optional.empty();
                    }
                });
            }
            return Optional.empty();
        }

    }

    @Singleton
    public static class TemplateValueProvider2 {

        public String getTemplateValue() {
            return "Custom {name}!";
        }
    }

    @Locate(TEMPLATE_LOCATION_3)
    public static class CustomLocator3 implements TemplateLocator {

        @Inject
        TemplateValueProvider2 templateValueProvider2;

        @Override
        public Optional<TemplateLocation> locate(String s) {

            if (s.equals(TEMPLATE_LOCATION_3)) {
                return Optional.of(new TemplateLocation() {

                    @Override
                    public Reader read() {
                        return new StringReader(templateValueProvider2.getTemplateValue());
                    }

                    @Override
                    public Optional<Variant> getVariant() {
                        return Optional.empty();
                    }
                });
            }
            return Optional.empty();
        }

    }

    @Locate(CHECKED_TEMPLATE_LOCATION)
    public static class CheckedTemplateLocator implements TemplateLocator {

        @Override
        public Optional<TemplateLocation> locate(String s) {

            if (s.equals(CHECKED_TEMPLATE_LOCATION)) {
                return Optional.of(new TemplateLocation() {

                    @Override
                    public Reader read() {
                        return new StringReader("My Checked Template Number: {templateNumber}");
                    }

                    @Override
                    public Optional<Variant> getVariant() {
                        return Optional.empty();
                    }
                });
            }
            return Optional.empty();
        }

    }

    @CheckedTemplate(basePath = "my_checked_template_base")
    static class Templates {

        static native TemplateInstance myCheckedTemplate(int templateNumber);

    }

}
