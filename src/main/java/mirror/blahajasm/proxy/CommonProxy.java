package mirror.blahajasm.proxy;

import betterwithmods.module.gameplay.Gameplay;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import mirror.blahajasm.api.BlahajStringPool;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.*;
import mirror.blahajasm.BlahajASM;
import mirror.blahajasm.BlahajLogger;
import mirror.blahajasm.BlahajReflector;
import mirror.blahajasm.api.datastructures.DummyMap;
import mirror.blahajasm.api.datastructures.ResourceCache;
import mirror.blahajasm.api.mixins.RegistrySimpleExtender;
import mirror.blahajasm.client.BlahajIncompatibilityHandler;
import mirror.blahajasm.common.java.JavaFixes;
import mirror.blahajasm.common.modfixes.betterwithmods.BWMBlastingOilOptimization;
import mirror.blahajasm.common.modfixes.ebwizardry.ArcaneLocks;
import mirror.blahajasm.config.BlahajConfig;

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
            messages.add("BlahajASM has replaced and improved upon functionalities from the following mods.");
            messages.add("Therefore, these mods are now incompatible with BlahajASM:");
            messages.add("");
            if (texFix) {
                messages.add(TextFormatting.BOLD + "TexFix");
            }
            if (vanillaFix) {
                messages.add(TextFormatting.BOLD + "VanillaFix");
            }
            BlahajIncompatibilityHandler.normalHaetPizza(messages);
        }
    }

    public void construct(FMLConstructionEvent event) {
        if (BlahajConfig.instance.cleanupLaunchClassLoaderEarly) {
            cleanupLaunchClassLoader();
        }
        if (BlahajConfig.instance.threadPriorityFix)
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);
    }

    public void preInit(FMLPreInitializationEvent event) { }

    public void init(FMLInitializationEvent event) { }

    public void postInit(FMLPostInitializationEvent event) {
        if (BlahajConfig.instance.skipCraftTweakerRecalculatingSearchTrees) {
            BlahajReflector.getClass("crafttweaker.mc1120.CraftTweaker").ifPresent(c -> {
                try {
                    Field alreadyChangedThePlayer = c.getDeclaredField("alreadyChangedThePlayer");
                    alreadyChangedThePlayer.setAccessible(true);
                    alreadyChangedThePlayer.setBoolean(null, true);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            });
        }
        if (Loader.isModLoaded("betterwithmods") && BlahajConfig.instance.bwmBlastingOilOptimization) {
            if (!Gameplay.disableBlastingOilEvents) {
                MinecraftForge.EVENT_BUS.register(BWMBlastingOilOptimization.class);
            }
        }
        if (Loader.isModLoaded("ebwizardry") && BlahajConfig.instance.optimizeArcaneLockRendering) {
            BlahajASM.customTileDataConsumer = ArcaneLocks.TRACK_ARCANE_TILES;
        }
    }

    public void loadComplete(FMLLoadCompleteEvent event) {
        BlahajLogger.instance.info("Trimming simple registries");
        HttpUtil.DOWNLOADER_EXECUTOR.execute(() -> {
            BlahajASM.simpleRegistryInstances.forEach(RegistrySimpleExtender::trim);
            BlahajASM.simpleRegistryInstances = null;
        });
        if (BlahajConfig.instance.cleanupLaunchClassLoaderEarly || BlahajConfig.instance.cleanCachesOnGameLoad) {
            invalidateLaunchClassLoaderCaches();
        } else if (BlahajConfig.instance.cleanupLaunchClassLoaderLate) {
            cleanupLaunchClassLoader();
        }
        if (BlahajStringPool.getSize() > 0) {
            MinecraftForge.EVENT_BUS.register(BlahajStringPool.class);
            BlahajLogger.instance.info("{} total strings processed. {} unique strings in BlahajStringPool, {} strings deduplicated altogether during game load.", BlahajStringPool.getDeduplicatedCount(), BlahajStringPool.getSize(), BlahajStringPool.getDeduplicatedCount() - BlahajStringPool.getSize());
        }
        if (BlahajConfig.instance.filePermissionsCacheCanonicalization) {
            MinecraftForge.EVENT_BUS.register(JavaFixes.INSTANCE);
        }
    }

    private void invalidateLaunchClassLoaderCaches() {
        try {
            BlahajLogger.instance.info("Invalidating and Cleaning LaunchClassLoader caches");
            if (!BlahajConfig.instance.noClassCache) {
                ((Map<String, Class<?>>) BlahajReflector.resolveFieldGetter(LaunchClassLoader.class, "cachedClasses").invoke(Launch.classLoader)).clear();
            }
            if (!BlahajConfig.instance.noResourceCache) {
                ((Map<String, byte[]>) BlahajReflector.resolveFieldGetter(LaunchClassLoader.class, "resourceCache").invoke(Launch.classLoader)).clear();
                ((Set<String>) BlahajReflector.resolveFieldGetter(LaunchClassLoader.class, "negativeResourceCache").invoke(Launch.classLoader)).clear();
            }
            ((Set<String>) BlahajReflector.resolveFieldGetter(LaunchClassLoader.class, "invalidClasses").invoke(Launch.classLoader)).clear();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static void cleanupLaunchClassLoader() {
        try {
            BlahajLogger.instance.info("Cleaning up LaunchClassLoader");
            if (BlahajConfig.instance.noClassCache) {
                BlahajReflector.resolveFieldSetter(LaunchClassLoader.class, "cachedClasses").invoke(Launch.classLoader, DummyMap.of());
            } else if (BlahajConfig.instance.weakClassCache) {
                Map<String, Class<?>> oldClassCache = (Map<String, Class<?>>) BlahajReflector.resolveFieldGetter(LaunchClassLoader.class, "cachedClasses").invoke(Launch.classLoader);
                Cache<String, Class<?>> newClassCache = CacheBuilder.newBuilder().concurrencyLevel(2).weakValues().build();
                newClassCache.putAll(oldClassCache);
                BlahajReflector.resolveFieldSetter(LaunchClassLoader.class, "cachedClasses").invoke(Launch.classLoader, newClassCache.asMap());
            }
            if (BlahajConfig.instance.noResourceCache) {
                BlahajReflector.resolveFieldSetter(LaunchClassLoader.class, "resourceCache").invoke(Launch.classLoader, new ResourceCache());
                BlahajReflector.resolveFieldSetter(LaunchClassLoader.class, "negativeResourceCache").invokeExact(Launch.classLoader, DummyMap.asSet());
            } else if (BlahajConfig.instance.weakResourceCache) {
                Map<String, byte[]> oldResourceCache = (Map<String, byte[]>) BlahajReflector.resolveFieldGetter(LaunchClassLoader.class, "resourceCache").invoke(Launch.classLoader);
                Cache<String, byte[]> newResourceCache = CacheBuilder.newBuilder().concurrencyLevel(2).weakValues().build();
                newResourceCache.putAll(oldResourceCache);
                BlahajReflector.resolveFieldSetter(LaunchClassLoader.class, "resourceCache").invoke(Launch.classLoader, newResourceCache.asMap());
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
