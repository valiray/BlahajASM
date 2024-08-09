package mirror.normalasm;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeNormal {

    private static final Unsafe $ = UnsafeAccess.UNSAFE;

    public static void removeFMLSecurityManager() {
        NormalLogger.instance.warn("Detaching FMLSecurityManager.");
        Field out = NormalReflector.getField(System.class, "out");
        Field err = NormalReflector.getField(System.class, "err");
        long errOffset = $.staticFieldOffset(err);
        long offset = errOffset + (errOffset - $.staticFieldOffset(out));
        $.putObjectVolatile($.staticFieldBase(err), offset, null);
        if (System.getSecurityManager() != null) {
            NormalLogger.instance.warn("Failed to detach FMLSecurityManager.");
        }
    }

    private UnsafeNormal() { }

}
