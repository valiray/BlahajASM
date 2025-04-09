package mirror.normalasm.common.crashes;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.FMLContainer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.MinecraftDummyContainer;
import net.minecraftforge.fml.common.ModContainer;
import mirror.normalasm.NormalLogger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.util.*;

public class ModIdentifier {

    private static final boolean IS_JAVA_9_OR_NEWER = determineIsJava9OrNewer();

    private static boolean determineIsJava9OrNewer() {
        try {
            String version = System.getProperty("java.version");
            return !version.startsWith("1.");
        } catch (Exception e) {
            NormalLogger.instance.error("Failed to determine Java version, assuming Java 8 compatibility.", e);
            return false;
        }
    }

    public static Set<ModContainer> identifyFromStacktrace(Throwable e) {
        Map<File, Set<ModContainer>> modMap = makeModMap();
        if (modMap == null || modMap.isEmpty()) {
            NormalLogger.instance.warn("Mod map is empty or null, cannot identify mods from stacktrace.");
            return Collections.emptySet();
        }
        HashSet<String> classes = new LinkedHashSet<>();
        Throwable currentEx = e;
        int depth = 0;
        while (currentEx != null && depth < 20) {
            for (StackTraceElement element : currentEx.getStackTrace()) {
                classes.add(element.getClassName());
            }
            currentEx = currentEx.getCause();
            depth++;
        }
        if (depth >= 20) {
            NormalLogger.instance.warn("Reached maximum depth limit while traversing exception causes.");
        }

        Set<ModContainer> mods = new LinkedHashSet<>();
        for (String className : classes) {
            try {
                Set<ModContainer> classMods = identifyFromClass(className, modMap);
                if (classMods != null && !classMods.isEmpty()) {
                    mods.addAll(classMods);
                }
            } catch (Exception identificationEx) {
                NormalLogger.instance.error("Error identifying mod for class: " + className, identificationEx);
            }
        }
        return mods;
    }

    public static Set<ModContainer> identifyFromClass(String className) {
        try {
            Map<File, Set<ModContainer>> modMap = makeModMap();
            if (modMap == null) return Collections.emptySet();
            return identifyFromClass(className, modMap);
        } catch (Exception e) {
            NormalLogger.instance.error("Error during single class identification setup for: " + className, e);
            return Collections.emptySet();
        }
    }

    private static Set<ModContainer> identifyFromClass(String className, Map<File, Set<ModContainer>> modMap) {
        if (className.startsWith("org.spongepowered.asm.mixin.") ||
                className.startsWith("sun.") ||
                className.startsWith("java.") ||
                className.startsWith("jdk.") ||
                className.startsWith("net.minecraftforge.") ||
                className.startsWith("net.minecraft.")
        ) {
            return Collections.emptySet();
        }

        String untrasformedName = null;
        try {
            untrasformedName = untransformName(Launch.classLoader, className);
        } catch (Exception e) {
            NormalLogger.instance.warn("Failed to untransform class name: " + className + ". Skipping identification.", e);
            return Collections.emptySet();
        }

        String resourcePath = untrasformedName.replace('.', '/') + ".class";
        URL url = null;
        try {
            url = Launch.classLoader.getResource(resourcePath);
        } catch (Exception e) {
            NormalLogger.instance.error("Error calling ClassLoader.getResource for: " + resourcePath, e);
            return Collections.emptySet();
        }

        if (url == null) {
            return Collections.emptySet();
        }

        String protocol = null;
        try {
            protocol = url.getProtocol();
        } catch (Exception e) {
            NormalLogger.instance.error("Error getting protocol from URL: " + url + " for class " + className, e);
            return Collections.emptySet();
        }

        if (protocol == null) {
            NormalLogger.instance.warn("URL protocol is null for URL: " + url + " for class " + className);
            return Collections.emptySet();
        }

        if (IS_JAVA_9_OR_NEWER && "jrt".equals(protocol)) {
            return Collections.emptySet();
        }

        try {
            File canonicalSourceFile = null;

            if ("jar".equals(protocol)) {
                String filePath = url.getFile();
                int separatorIndex = filePath.indexOf('!');
                if (separatorIndex != -1) {
                    String jarPath = filePath.substring(0, separatorIndex);
                    URL jarUrl = new URL(jarPath);
                    try {
                        URI jarUri = jarUrl.toURI();
                        if ("file".equals(jarUri.getScheme())) {
                            File jarFile = new File(jarUri);
                            canonicalSourceFile = jarFile.getCanonicalFile();
                        } else {
                            NormalLogger.instance.warn("JAR URL scheme is not 'file', cannot process: {}", jarUri);
                            return Collections.emptySet();
                        }
                    } catch (URISyntaxException | IllegalArgumentException | IOException | SecurityException e) {
                        NormalLogger.instance.error("Error converting JAR URL to canonical file for class " + className + " (JAR URL: " + jarUrl + ")", e);
                        throw new RuntimeException("Failed processing JAR source for: " + className, e);
                    }

                } else {
                    NormalLogger.instance.warn("Unexpected JAR URL format (missing '!') for class {}: {}", className, url);
                    return Collections.emptySet();
                }
            } else if ("file".equals(protocol)) {
                File classFile = null;
                try {
                    URI classUri = url.toURI();
                    if ("file".equals(classUri.getScheme())) {
                        classFile = new File(classUri);
                    } else {
                        NormalLogger.instance.warn("Class URL scheme is 'file' but URI scheme is not? URI: {}", classUri);
                        return Collections.emptySet();
                    }
                } catch (URISyntaxException | IllegalArgumentException | SecurityException e) {
                    NormalLogger.instance.error("Error converting class file URL to File object for " + className + " (URL: " + url + ")", e);
                    throw new RuntimeException("Failed processing FILE source URI for: " + className, e);
                }

                if (classFile == null || !classFile.exists()) {
                    NormalLogger.instance.warn("Could not create valid File object or file does not exist for class {}: {}", className, url);
                    return Collections.emptySet();
                }

                String classCanonicalPath = null;
                try {
                    classCanonicalPath = classFile.getCanonicalPath();
                } catch (IOException | SecurityException e) {
                    NormalLogger.instance.error("Failed to get canonical path for class file " + className + " (File: " + classFile.getPath() + ")", e);
                    throw new RuntimeException("Failed getting canonical path for class file: " + className, e);
                }

                File matchingSourceDir = null;
                String longestMatchPath = "";

                for (File modSourceDir : modMap.keySet()) {
                    String modSourceCanonicalPath = null;
                    try {
                        modSourceCanonicalPath = modSourceDir.getPath();

                        if (classCanonicalPath.startsWith(modSourceCanonicalPath + File.separator)) {
                            if (matchingSourceDir == null || modSourceCanonicalPath.length() > longestMatchPath.length()) {
                                longestMatchPath = modSourceCanonicalPath;
                                matchingSourceDir = modSourceDir;
                            }
                        }
                    } catch (Exception e) {
                        NormalLogger.instance.error("Error accessing path from modMap key: " + modSourceDir, e);
                    }
                }

                if (matchingSourceDir != null) {
                    canonicalSourceFile = matchingSourceDir;
                } else {
                    return Collections.emptySet();
                }

            } else {
                return Collections.emptySet();
            }

            if (canonicalSourceFile != null) {
                Set<ModContainer> result = modMap.getOrDefault(canonicalSourceFile, Collections.emptySet());
                return result;
            } else {
                NormalLogger.instance.warn("Canonical source file was null before map lookup for class {}, URL {}", className, url);
                return Collections.emptySet();
            }

        } catch (RuntimeException e) {
            NormalLogger.instance.error("RuntimeException during identification for " + className, e);
            throw e;
        } catch (Exception e) {
            NormalLogger.instance.error("!!! UNEXPECTED CRITICAL FAILURE during identification for class " + className + " (URL: " + url + ") !!!", e);
            throw new RuntimeException("Unexpected failure identifying source for class: " + className, e);
        }
    }

    private static Map<File, Set<ModContainer>> makeModMap() {
        Map<File, Set<ModContainer>> modMap = new Object2ObjectOpenHashMap<>();
        List<ModContainer> modList = Loader.instance().getModList();

        for (ModContainer mod : modList) {
            if (mod == null) {
                NormalLogger.instance.warn("Encountered a null ModContainer in mod list.");
                continue;
            }
            if (mod instanceof MinecraftDummyContainer || mod instanceof FMLContainer) {
                continue;
            }

            File source = mod.getSource();
            if (source == null) {
                NormalLogger.instance.warn("Mod {} ({}) has a null source file, cannot map it.", mod.getModId(), mod.getName());
                continue;
            }

            try {
                File canonicalSource = source.getCanonicalFile();
                modMap.computeIfAbsent(canonicalSource, k -> new ObjectArraySet<>()).add(mod);
            } catch (IOException | SecurityException e) {
                NormalLogger.instance.error("Failed to get canonical path for mod source: " + source.getPath() + " for mod " + mod.getModId() + ". This mod might not be identified correctly in crashes.", e);
            } catch (Exception e) {
                NormalLogger.instance.error("Unexpected error processing source for mod " + mod.getModId(), e);
            }
        }
        return modMap;
    }

    private static String untransformName(LaunchClassLoader launchClassLoader, String className) {
        try {
            Method untransformNameMethod = LaunchClassLoader.class.getDeclaredMethod("untransformName", String.class);
            untransformNameMethod.setAccessible(true);
            String untransformed = (String) untransformNameMethod.invoke(launchClassLoader, className);
            return untransformed;
        } catch (Exception e) {
            NormalLogger.instance.error("Failed to reflectively call untransformName for class: " + className, e);
            throw new RuntimeException("Failed to untransform class name via reflection: " + className, e);
        }
    }
}