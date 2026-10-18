package org.acme.libd;

import org.acme.liba.UsedByD;
import org.acme.liba.UsedByBoth;
import org.acme.handles.HandleTarget;

public class HelperD {

    private final UsedByD usedByD = new UsedByD();
    private final UsedByBoth usedByBoth = new UsedByBoth();

    public String help() {
        // Direct reference to HandleTarget so it becomes reachable
        HandleTarget.doWork();
        return usedByD.getValue() + "+" + usedByBoth.getValue();
    }

    // Comma-delimited class name list in method bytecode — tree-shaker
    // splits on comma/colon and matches segments against known class names
    public String getDelimitedClasses() {
        return "org.acme.delimited.ListTargetA,org.acme.delimited.ListTargetB";
    }
}
