package org.acme.qute

class Quark(val flavor: Flavor, val color: Color) {
    enum class Flavor {
        DOWN, UP, STRANGE, CHARM, BOTTOM, TOP
    }

    enum class Color(val hex: String) {
        RED("#ff6961"), GREEN("#77dd77"), BLUE("#aec6cf");
    }
}
