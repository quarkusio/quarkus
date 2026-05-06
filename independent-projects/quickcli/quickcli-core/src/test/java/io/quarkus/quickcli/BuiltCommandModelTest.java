package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.quarkus.quickcli.model.BuiltCommandModel;
import io.quarkus.quickcli.model.CommandModelRegistry;

class BuiltCommandModelTest {

    static class ModelCmd implements Runnable {
        String name;
        CommandSpec spec;
        Object parent;
        List<String> unmatched;

        @Override
        public void run() {
        }
    }

    static class MixinClass {
        String value;
    }

    static class ArgGroupClass {
        boolean flag;
    }

    static class MixinCmd implements Runnable {
        MixinClass mixin;
        CommandSpec spec;

        @Override
        public void run() {
        }
    }

    static class ArgGroupCmd implements Runnable {
        ArgGroupClass group;

        @Override
        public void run() {
        }
    }

    static class ParentCmd implements Runnable {
        @Override
        public void run() {
        }
    }

    // --- createInstance ---

    @Test
    void createInstance() {
        BuiltCommandModel model = TestModelHelper.builder(ModelCmd.class, ModelCmd::new)
                .name("cmd").build();
        Object instance = model.createInstance();
        assertInstanceOf(ModelCmd.class, instance);
    }

    // --- commandClass ---

    @Test
    void commandClass() {
        BuiltCommandModel model = TestModelHelper.builder(ModelCmd.class, ModelCmd::new)
                .name("cmd").build();
        assertEquals(ModelCmd.class, model.commandClass());
    }

    // --- initMixins ---

    @Test
    void initMixins() throws Exception {
        BuiltCommandModel model = TestModelHelper.builder(MixinCmd.class, MixinCmd::new)
                .name("cmd")
                .addMixin(new BuiltCommandModel.MixinBinding(
                        "mixin", MixinClass.class,
                        (inst, val) -> ((MixinCmd) inst).mixin = (MixinClass) val))
                .build();

        MixinCmd instance = new MixinCmd();
        assertNull(instance.mixin);
        Factory factory = new Factory() {
            @Override
            public <T> T create(Class<T> cls) throws Exception {
                return cls.getDeclaredConstructor().newInstance();
            }
        };
        model.initMixins(instance, factory);
        assertNotNull(instance.mixin);
        assertInstanceOf(MixinClass.class, instance.mixin);
    }

    // --- initArgGroups ---

    @Test
    void initArgGroups() throws Exception {
        BuiltCommandModel model = TestModelHelper.builder(ArgGroupCmd.class, ArgGroupCmd::new)
                .name("cmd")
                .addArgGroup(new BuiltCommandModel.ArgGroupBinding(
                        "group", ArgGroupClass.class,
                        (inst, val) -> ((ArgGroupCmd) inst).group = (ArgGroupClass) val))
                .build();

        ArgGroupCmd instance = new ArgGroupCmd();
        assertNull(instance.group);
        Factory factory = new Factory() {
            @Override
            public <T> T create(Class<T> cls) throws Exception {
                return cls.getDeclaredConstructor().newInstance();
            }
        };
        model.initArgGroups(instance, factory);
        assertNotNull(instance.group);
    }

    // --- injectSpec ---

    @Test
    void injectSpec() {
        BuiltCommandModel model = TestModelHelper.builder(ModelCmd.class, ModelCmd::new)
                .name("cmd")
                .specAccessor((inst, val) -> ((ModelCmd) inst).spec = (CommandSpec) val)
                .build();

        ModelCmd instance = new ModelCmd();
        CommandSpec spec = model.buildSpec();
        model.injectSpec(instance, spec);
        assertSame(spec, instance.spec);
    }

    @Test
    void injectSpecNoAccessor() {
        BuiltCommandModel model = TestModelHelper.builder(ModelCmd.class, ModelCmd::new)
                .name("cmd").build();

        ModelCmd instance = new ModelCmd();
        // Should not throw even without a spec accessor
        model.injectSpec(instance, model.buildSpec());
        assertNull(instance.spec);
    }

    // --- injectSpec with mixee spec accessors ---

    @Test
    void injectSpecWithMixeeAccessors() {
        AtomicReference<CommandSpec> mixeeSpec = new AtomicReference<>();
        BuiltCommandModel model = TestModelHelper.builder(MixinCmd.class, MixinCmd::new)
                .name("cmd")
                .specAccessor((inst, val) -> ((MixinCmd) inst).spec = (CommandSpec) val)
                .addMixeeSpecAccessor((inst, val) -> mixeeSpec.set((CommandSpec) val))
                .build();

        MixinCmd instance = new MixinCmd();
        CommandSpec spec = model.buildSpec();
        model.injectSpec(instance, spec);
        assertSame(spec, instance.spec);
        assertSame(spec, mixeeSpec.get());
    }

    // --- setParentCommand ---

    @Test
    void setParentCommand() {
        BuiltCommandModel model = TestModelHelper.builder(ModelCmd.class, ModelCmd::new)
                .name("cmd")
                .parentCommandAccessor((inst, val) -> ((ModelCmd) inst).parent = val)
                .build();

        ModelCmd instance = new ModelCmd();
        ParentCmd parent = new ParentCmd();
        model.setParentCommand(instance, parent);
        assertSame(parent, instance.parent);
    }

    @Test
    void setParentCommandNoAccessor() {
        BuiltCommandModel model = TestModelHelper.builder(ModelCmd.class, ModelCmd::new)
                .name("cmd").build();

        ModelCmd instance = new ModelCmd();
        // Should not throw even without a parent accessor
        model.setParentCommand(instance, new ParentCmd());
        assertNull(instance.parent);
    }

    // --- setUnmatched ---

    @Test
    void setUnmatched() {
        BuiltCommandModel model = TestModelHelper.builder(ModelCmd.class, ModelCmd::new)
                .name("cmd")
                .unmatchedAccessor((inst, val) -> ((ModelCmd) inst).unmatched = castList(val))
                .build();

        ModelCmd instance = new ModelCmd();
        model.setUnmatched(instance, List.of("--extra", "arg"));
        assertEquals(List.of("--extra", "arg"), instance.unmatched);
    }

    @Test
    void setUnmatchedNoAccessor() {
        BuiltCommandModel model = TestModelHelper.builder(ModelCmd.class, ModelCmd::new)
                .name("cmd").build();

        ModelCmd instance = new ModelCmd();
        // Should not throw even without an unmatched accessor
        model.setUnmatched(instance, List.of("--extra"));
        assertNull(instance.unmatched);
    }

    // --- buildSpec includes all metadata ---

    @Test
    void buildSpecIncludesMetadata() {
        BuiltCommandModel model = TestModelHelper.builder(ModelCmd.class, ModelCmd::new)
                .name("myapp")
                .description("desc")
                .version("1.0")
                .mixinStandardHelpOptions(true)
                .header("Header")
                .footer("Footer")
                .aliases("app", "ma")
                .hasUnmatchedField(true)
                .hasSpecField(true)
                .build();

        CommandSpec spec = model.buildSpec();
        assertEquals("myapp", spec.name());
        assertArrayEquals(new String[] { "desc" }, spec.description());
        assertArrayEquals(new String[] { "1.0" }, spec.version());
        assertTrue(spec.mixinStandardHelpOptions());
        assertArrayEquals(new String[] { "Header" }, spec.header());
        assertArrayEquals(new String[] { "Footer" }, spec.footer());
        assertArrayEquals(new String[] { "app", "ma" }, spec.aliases());
        assertTrue(spec.hasUnmatchedField());
        assertTrue(spec.hasSpecField());
    }

    // --- buildSpec includes usage message config ---

    @Test
    void buildSpecUsageMessage() {
        BuiltCommandModel model = TestModelHelper.builder(ModelCmd.class, ModelCmd::new)
                .name("cmd")
                .commandListHeading("Cmds:%n")
                .synopsisHeading("Use: ")
                .optionListHeading("Opts:%n")
                .headerHeading("Head:%n")
                .parameterListHeading("Params:%n")
                .showDefaultValues(true)
                .build();

        CommandSpec spec = model.buildSpec();
        UsageMessageSpec usage = spec.usageMessage();
        assertEquals("Cmds:%n", usage.commandListHeading());
        assertEquals("Use: ", usage.synopsisHeading());
        assertEquals("Opts:%n", usage.optionListHeading());
        assertEquals("Head:%n", usage.headerHeading());
        assertEquals("Params:%n", usage.parameterListHeading());
        assertTrue(usage.showDefaultValues());
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(Object val) {
        return (List<T>) val;
    }
}
