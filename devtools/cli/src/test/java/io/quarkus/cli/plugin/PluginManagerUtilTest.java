package io.quarkus.cli.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PluginManagerUtilTest {

    @Test
    public void shouldGetTypeFromLocation() {
        assertEquals(PluginType.jar, PluginUtil.getType("http://shomehost/some/path/my.jar"));
        assertEquals(PluginType.maven, PluginUtil.getType("my.group:my-artifact:my.version"));
        assertEquals(PluginType.jbang, PluginUtil.getType("something_not_in_path"));
    }

    @Test
    public void shouldGetNameFromLocation() {
        PluginManagerUtil util = PluginManagerUtil.getUtil();
        assertEquals("my", util.getName("http://shomehost/some/path/my.jar"));
        assertEquals("command", util.getName("http://shomehost/some/path/quarkus-command.jar"));
        assertEquals("command", util.getName("http://shomehost/some/path/quarkus-command-cli.jar"));

        assertEquals("my-artifact", util.getName("my.group:my-artifact:my.version"));
        assertEquals("my-artifact", util.getName("my.group:my-artifact-cli:my.version"));

        assertEquals("something_not_in_path", util.getName("something_not_in_path"));
        assertEquals("something", util.getName("something@quarkusio"));
        assertEquals("something", util.getName("something-cli@quarkusio"));
        //No replacement here
        assertEquals("something-cli2", util.getName("something-cli2@quarkusio"));

        PluginManagerSettings customSetttings = PluginManagerSettings.defaultSettings()
                .withPluignPrefix("awesomeoss")
                .withCatalogs("awesomeossio");
        util = PluginManagerUtil.getUtil(customSetttings);

        assertEquals("my", util.getName("http://shomehost/some/path/my.jar"));

        assertEquals("quarkus-command", util.getName("http://shomehost/some/path/quarkus-command.jar"));
        assertEquals("quarkus-command", util.getName("http://shomehost/some/path/quarkus-command-cli.jar"));

        assertEquals("command", util.getName("http://shomehost/some/path/awesomeoss-command.jar"));
        assertEquals("command", util.getName("http://shomehost/some/path/awesomeoss-command-cli.jar"));

        assertEquals("something-cli2", util.getName("something-cli2@awesomeossio"));
    }

    @Test
    public void shouldGetPluginFromLocation() {
        PluginManagerUtil util = PluginManagerUtil.getUtil();

        Plugin p = util.fromLocation("http://shomehost/some/path/my.jar");
        assertEquals("my", p.getName());
        assertEquals(PluginType.jar, p.getType());

        p = util.fromLocation("/some/path/my.jar");
        assertEquals("my", p.getName());
        assertEquals(PluginType.jar, p.getType());

        p = util.fromLocation("my.group:my-artifact-cli:my.version");
        assertEquals("my-artifact", p.getName());
        assertEquals(PluginType.maven, p.getType());

        p = util.fromLocation("quarkus-alias");
        assertEquals("alias", p.getName());
        assertEquals(PluginType.jbang, p.getType());
    }

    @Test
    public void shouldGetPluginFromAliasedLocation() {
        PluginManagerUtil util = PluginManagerUtil.getUtil();

        Plugin p = util.fromAlias("my-alias: http://shomehost/some/path/my.jar");
        assertEquals("my-alias", p.getName());
        assertEquals(PluginType.jar, p.getType());

        p = util.fromAlias("my-alias: /some/path/my.jar");
        assertEquals("my-alias", p.getName());
        assertEquals(PluginType.jar, p.getType());

        p = util.fromAlias("my-alias: my.group:my-artifact-cli:my.version");
        assertEquals("my-alias", p.getName());
        assertEquals(PluginType.maven, p.getType());

        p = util.fromAlias("my-alias: quarkus-alias");
        assertEquals("my-alias", p.getName());
        assertEquals(PluginType.jbang, p.getType());
    }
}
