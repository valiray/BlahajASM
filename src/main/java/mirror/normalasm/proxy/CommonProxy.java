package mirror.normalasm.proxy;

import betterwithmods.module.gameplay.Gameplay;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import mirror.normalasm.api.NormalStringPool;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.*;
import mirror.normalasm.NormalASM;
import mirror.normalasm.NormalLogger;
import mirror.normalasm.NormalReflector;
import mirror.normalasm.api.datastructures.DummyMap;
import mirror.normalasm.api.datastructures.ResourceCache;
import mirror.normalasm.api.mixins.RegistrySimpleExtender;
import mirror.normalasm.client.NormalIncompatibilityHandler;
import mirror.normalasm.common.java.JavaFixes;
import mirror.normalasm.common.modfixes.betterwithmods.BWMBlastingOilOptimization;
import mirror.normalasm.common.modfixes.ebwizardry.ArcaneLocks;
import mirror.normalasm.config.NormalConfig;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommonProxy {

    /**
     * This has to be called after FMLPreInitializationEvent as FMLConstructionEvent is wrapped in a different try - catch that behaves differently...
     */
    public void throwIncompatibility() {
        boolean texFix = Loader.isModLoaded("texfix");
        boolean vanillaFix = Loader.isModLoaded("vanillafix");
        if (texFix || vanillaFix) {
            List<String> messages = new ArrayList<>();
            messages.add("NormalASM has replaced and improved upon functionalities from the following mods.");
            messages.add("Therefore, these mods are now incompatible with NormalASM:");
            messages.add("");
            if (texFix) {
                messages.add(TextFormatting.BOLD + "TexFix");
            }
            if (vanillaFix) {
                messages.add(TextFormatting.BOLD + "VanillaFix");
            }
            NormalIncompatibilityHandler.normalHaetPizza(messages);
        }
    }

    public void construct(FMLConstructionEvent event) {
        if (NormalConfig.instance.cleanupLaunchClassLoaderEarly) {
            cleanupLaunchClassLoader();
        }
        if (NormalConfig.instance.threadPriorityFix)
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);
    }

    public void preInit(FMLPreInitializationEvent event) { }

    public void init(FMLInitializationEvent event) { }

    public void postInit(FMLPostInitializationEvent event) {
        if (NormalConfig.instance.skipCraftTweakerRecalculatingSearchTrees) {
            NormalReflector.getClass("crafttweaker.mc1120.CraftTweaker").ifPresent(c -> {
                try {
                    Field alreadyChangedThePlayer = c.getDeclaredField("alreadyChangedThePlayer");
                    alreadyChangedThePlayer.setAccessible(true);
                    alreadyChangedThePlayer.setBoolean(null, true);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            });
        }
        if (Loader.isModLoaded("betterwithmods") && NormalConfig.instance.bwmBlastingOilOptimization) {
            if (!Gameplay.disableBlastingOilEvents) {
                MinecraftForge.EVENT_BUS.register(BWMBlastingOilOptimization.class);
            }
        }
        if (Loader.isModLoaded("ebwizardry") && NormalConfig.instance.optimizeArcaneLockRendering) {
            NormalASM.customTileDataConsumer = ArcaneLocks.TRACK_ARCANE_TILES;
        }
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        NormalLogger.instance.info("Trimming simple registries");
        HttpUtil.DOWNLOADER_EXECUTOR.execute(() -> {
            NormalASM.simpleRegistryInstances.forEach(RegistrySimpleExtender::trim);
            NormalASM.simpleRegistryInstances = null;
        });
        if (NormalConfig.instance.cleanupLaunchClassLoaderEarly || NormalConfig.instance.cleanCachesOnGameLoad) {
            invalidateLaunchClassLoaderCaches();
        } else if (NormalConfig.instance.cleanupLaunchClassLoaderLate) {
            cleanupLaunchClassLoader();
        }
        if (NormalStringPool.getSize() > 0) {
            MinecraftForge.EVENT_BUS.register(NormalStringPool.class);
            NormalLogger.instance.info("{} total strings processed. {} unique strings in NormalStringPool, {} strings deduplicated altogether during game load.", NormalStringPool.getDeduplicatedCount(), NormalStringPool.getSize(), NormalStringPool.getDeduplicatedCount() - NormalStringPool.getSize());
        }
        if (NormalConfig.instance.filePermissionsCacheCanonicalization) {
            MinecraftForge.EVENT_BUS.register(JavaFixes.INSTANCE);
        }
    }

    private void invalidateLaunchClassLoaderCaches() {
        try {
            NormalLogger.instance.info("Invalidating and Cleaning LaunchClassLoader caches");
            if (!NormalConfig.instance.noClassCache) {
                ((Map<String, Class<?>>) NormalReflector.resolveFieldGetter(LaunchClassLoader.class, "cachedClasses").invoke(Launch.classLoader)).clear();
            }
            if (!NormalConfig.instance.noResourceCache) {
                ((Map<String, byte[]>) NormalReflector.resolveFieldGetter(LaunchClassLoader.class, "resourceCache").invoke(Launch.classLoader)).clear();
                ((Set<String>) NormalReflector.resolveFieldGetter(LaunchClassLoader.class, "negativeResourceCache").invoke(Launch.classLoader)).clear();
            }
            ((Set<String>) NormalReflector.resolveFieldGetter(LaunchClassLoader.class, "invalidClasses").invoke(Launch.classLoader)).clear();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static void cleanupLaunchClassLoader() {
        try {
            NormalLogger.instance.info("Cleaning up LaunchClassLoader");
            if (NormalConfig.instance.noClassCache) {
                NormalReflector.resolveFieldSetter(LaunchClassLoader.class, "cachedClasses").invoke(Launch.classLoader, DummyMap.of());
            } else if (NormalConfig.instance.weakClassCache) {
                Map<String, Class<?>> oldClassCache = (Map<String, Class<?>>) NormalReflector.resolveFieldGetter(LaunchClassLoader.class, "cachedClasses").invoke(Launch.classLoader);
                Cache<String, Class<?>> newClassCache = CacheBuilder.newBuilder().concurrencyLevel(2).weakValues().build();
                newClassCache.putAll(oldClassCache);
                NormalReflector.resolveFieldSetter(LaunchClassLoader.class, "cachedClasses").invoke(Launch.classLoader, newClassCache.asMap());
            }
            if (NormalConfig.instance.noResourceCache) {
                NormalReflector.resolveFieldSetter(LaunchClassLoader.class, "resourceCache").invoke(Launch.classLoader, new ResourceCache());
                NormalReflector.resolveFieldSetter(LaunchClassLoader.class, "negativeResourceCache").invokeExact(Launch.classLoader, DummyMap.asSet());
            } else if (NormalConfig.instance.weakResourceCache) {
                Map<String, byte[]> oldResourceCache = (Map<String, byte[]>) NormalReflector.resolveFieldGetter(LaunchClassLoader.class, "resourceCache").invoke(Launch.classLoader);
                Cache<String, byte[]> newResourceCache = CacheBuilder.newBuilder().concurrencyLevel(2).weakValues().build();
                newResourceCache.putAll(oldResourceCache);
                NormalReflector.resolveFieldSetter(LaunchClassLoader.class, "resourceCache").invoke(Launch.classLoader, newResourceCache.asMap());
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
