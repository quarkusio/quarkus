package org.acme.qute;

public class Quark {
    public enum Flavor { DOWN, UP, STRANGE, CHARM, BOTTOM, TOP }

    public enum Color {
        RED("#ff6961"),
        GREEN("#77dd77"),
        BLUE("#aec6cf");

        public final String hex;

        Color(String hex) {
            this.hex = hex;
        }
    }

    public final Flavor flavor;
    public final Color color;

    public Quark(Flavor flavor, Color color) {
        this.flavor = flavor;
        this.color = color;
    }
}