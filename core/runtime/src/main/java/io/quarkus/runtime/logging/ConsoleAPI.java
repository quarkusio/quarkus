package io.quarkus.runtime.logging;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.word.Pointer;

@CContext(WindowsConsoleDirectives.class)
@Platforms(Platform.WINDOWS.class)
public class ConsoleAPI {

    static final int STD_INPUT_HANDLE = -10;
    static final int STD_OUTPUT_HANDLE = -11;
    static final int STD_ERROR_HANDLE = -12;

    static final int ENABLE_PROCESSED_INPUT = 0x0001;
    static final int ENABLE_LINE_INPUT = 0x0002;
    static final int ENABLE_ECHO_INPUT = 0x0004;
    static final int ENABLE_WINDOW_INPUT = 0x0008;
    static final int ENABLE_MOUSE_INPUT = 0x0010;
    static final int ENABLE_INSERT_MODE = 0x0020;
    static final int ENABLE_QUICK_EDIT_MODE = 0x0040;
    static final int ENABLE_EXTENDED_FLAGS = 0x0080;

    // HANDLE WINAPI GetStdHandle(
    // __in DWORD nStdHandle
    // );
    @CFunction
    public static native Pointer GetStdHandle(int nStdHandle);

    // BOOL WINAPI SetConsoleMode(
    //   _In_  HANDLE hConsoleHandle,
    //   _In_  DWORD dwMode);
    @CFunction
    public static native int SetConsoleMode(Pointer hConsoleHandle, int dwMode);

    // BOOL WINAPI GetConsoleMode(
    //   _In_   HANDLE hConsoleHandle,
    //   _Out_  LPDWORD lpMode);
    @CFunction
    public static native void GetConsoleMode(Pointer hConsoleHandle, CIntPointer dwMode);

    /**
     * kernel32 = ctypes.windll.kernel32
     * kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
     **/
}