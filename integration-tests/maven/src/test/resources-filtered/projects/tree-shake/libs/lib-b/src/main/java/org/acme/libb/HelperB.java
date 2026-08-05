package org.acme.libb;

import org.acme.liba.UsedByB;
import org.acme.liba.UsedByBoth;

public class HelperB {

    private final UsedByB usedByB = new UsedByB();
    private final UsedByBoth usedByBoth = new UsedByBoth();

    public String help() {
        return usedByB.getValue() + "+" + usedByBoth.getValue();
    }

    // String constant in method bytecode — tree-shaker scans LDC instructions
    // and matches against known class names
    public String getStringRef() {
        return "org.acme.stringconst.StringRefTarget";
    }
}
