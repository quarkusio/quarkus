package io.quarkus.runtime.logging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.c.CContext;

import com.oracle.svm.core.util.VMError;

@Platforms(Platform.WINDOWS.class)
public class WindowsConsoleDirectives implements CContext.Directives {

    private static final String[] windowsLibs = new String[] { "<windows.h>" };

    @Override
    public boolean isInConfiguration() {
        return Platform.includedIn(Platform.WINDOWS.class);
    }

    @Override
    public List<String> getHeaderFiles() {
        if (Platform.includedIn(Platform.WINDOWS.class)) {
            List<String> result = new ArrayList<>(Arrays.asList(windowsLibs));
            return result;
        } else {
            throw VMError.shouldNotReachHere("Unsupported OS");
        }
    }

    @Override
    public List<String> getMacroDefinitions() {
        return Arrays.asList("_WIN64");
    }

}