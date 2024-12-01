package mirror.blahajasm.common.java;

import com.google.common.base.Stopwatch;
import mirror.blahajasm.api.BlahajStringPool;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import mirror.blahajasm.BlahajLogger;
import mirror.blahajasm.BlahajReflector;

import java.lang.invoke.MethodHandle;
import java.security.*;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;

@SuppressWarnings("all")
public class JavaFixes {

    private static final MethodHandle SECURECLASSLOADER$PDCACHE$GETTER;
    private static final MethodHandle PERMISSION$NAME$SETTER;

    static {
        MethodHandle secureClassLoader$pdcache$getter = null;
        MethodHandle permission$name$setter = null;
        try {
            secureClassLoader$pdcache$getter = BlahajReflector.resolveFieldGetter(SecureClassLoader.class, "pdcache");
            permission$name$setter = BlahajReflector.resolveFieldSetter(Permission.class, "name");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SECURECLASSLOADER$PDCACHE$GETTER = secureClassLoader$pdcache$getter;
        PERMISSION$NAME$SETTER = permission$name$setter;
    }

    public static final JavaFixes INSTANCE = new JavaFixes();

    private JavaFixes() {
        run();
    }


    private void run() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            BlahajStringPool.establishPool(BlahajStringPool.FILE_PERMISSIONS_ID, 512);
            HashMap<CodeSource, ProtectionDomain> pdcache = (HashMap<CodeSource, ProtectionDomain>) SECURECLASSLOADER$PDCACHE$GETTER.invoke(Launch.classLoader);
            for (ProtectionDomain pd : pdcache.values()) {
                PermissionCollection pc = pd.getPermissions();
                if (pc != null) {
                    Enumeration<Permission> perms = pc.elements();
                    while (perms.hasMoreElements()) {
                        Permission perm = perms.nextElement();
                        PERMISSION$NAME$SETTER.invokeExact(perm, BlahajStringPool.canonicalize(perm.getName()));
                    }
                }
            }
            BlahajStringPool.purgePool(BlahajStringPool.FILE_PERMISSIONS_ID);
        }
        catch (ConcurrentModificationException ignored) { } // Swallow it, we don't care enough about the CME here
        catch (Throwable t) {
            t.printStackTrace();
        }
        BlahajLogger.instance.info("Took {} to canonicalize Java's FilePermission caches.", stopwatch.stop());
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        run();
    }

}
