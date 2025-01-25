package mirror.blahajasm.core;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import mirror.blahajasm.UnsafeBlahaj;
import mirror.blahajasm.config.BlahajConfig;
import mirror.blahajasm.BlahajLogger;
import mirror.blahajasm.api.DeobfuscatingRewritePolicy;
import mirror.blahajasm.api.StacktraceDeobfuscator;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@IFMLLoadingPlugin.Name("BlahajASM")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
public class BlahajLoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    public static final String VERSION = "5.22";

    public static final boolean isDeobf = FMLLaunchHandler.isDeobfuscatedEnvironment();


    // public static final boolean isModDirectorInstalled = BlahajReflector.doesTweakExist("net.jan.moddirector.launchwrapper.ModDirectorTweaker");
    public static final boolean isVMOpenJ9 = SystemUtils.JAVA_VM_NAME.toLowerCase(Locale.ROOT).contains("openj9");
    public static final boolean isClient = FMLLaunchHandler.side() == Side.CLIENT;

    public BlahajLoadingPlugin() {
        BlahajLogger.instance.info("BlahajASM is on the {}-side.", isClient ? "client" : "server");
        BlahajLogger.instance.info("BlahajASM is preparing and loading in mixins since Rongmario's too lazy to write pure ASM at times despite the mod being called 'BlahajASM'");
        if (BlahajConfig.instance.outdatedCaCertsFix) {
            try (InputStream is = this.getClass().getResource("/cacerts").openStream()) {
                File cacertsCopy = File.createTempFile("cacerts", "");
                cacertsCopy.deleteOnExit();
                FileUtils.copyInputStreamToFile(is, cacertsCopy);
                System.setProperty("javax.net.ssl.trustStore", cacertsCopy.getAbsolutePath());
                BlahajLogger.instance.warn("Replacing CA Certs with an updated one...");
            } catch (Exception e) {
                BlahajLogger.instance.warn("Unable to replace CA Certs.", e);
            }
        }
        if (BlahajConfig.instance.removeForgeSecurityManager) {
            UnsafeBlahaj.removeFMLSecurityManager();
        }
        if (BlahajConfig.instance.crashReportImprovements || BlahajConfig.instance.rewriteLoggingWithDeobfuscatedNames) {
            File modDir = new File(Launch.minecraftHome, "config/blahajasm");
            modDir.mkdirs();
            // Initialize StacktraceDeobfuscator
            BlahajLogger.instance.info("Initializing StacktraceDeobfuscator...");
            try {
                File mappings = new File(modDir, "methods-stable_39.csv");
                if (mappings.exists()) {
                    BlahajLogger.instance.info("Found MCP stable-39 method mappings: {}", mappings.getName());
                } else {
                    BlahajLogger.instance.info("Downloading MCP stable-39 method mappings to: {}", mappings.getName());
                }
                StacktraceDeobfuscator.init(mappings);
            } catch (Exception e) {
                BlahajLogger.instance.error("Failed to get MCP stable-39 data!", e);
            }
            BlahajLogger.instance.info("Initialized StacktraceDeobfuscator.");
            if (BlahajConfig.instance.rewriteLoggingWithDeobfuscatedNames) {
                BlahajLogger.instance.info("Installing DeobfuscatingRewritePolicy...");
                DeobfuscatingRewritePolicy.install();
                BlahajLogger.instance.info("Installed DeobfuscatingRewritePolicy.");
            }
        }
        boolean needToDGSFFFF = isVMOpenJ9 && SystemUtils.IS_JAVA_1_8;
        int buildAppendIndex = SystemUtils.JAVA_VERSION.indexOf("_");
        if (needToDGSFFFF && buildAppendIndex != -1) {
            if (Integer.parseInt(SystemUtils.JAVA_VERSION.substring(buildAppendIndex + 1)) < 265) {
                for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                    if (arg.equals("-Xjit:disableGuardedStaticFinalFieldFolding")) {
                        needToDGSFFFF = false;
                        break;
                    }
                }
                if (needToDGSFFFF) {
                    BlahajLogger.instance.fatal("BlahajASM notices that you're using Eclipse OpenJ9 {}!", SystemUtils.JAVA_VERSION);
                    BlahajLogger.instance.fatal("This OpenJ9 version is outdated and contains a critical bug: https://github.com/eclipse-openj9/openj9/issues/8353");
                    BlahajLogger.instance.fatal("Either use '-Xjit:disableGuardedStaticFinalFieldFolding' as part of your java arguments, or update OpenJ9!");
                }
            }
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return "mirror.blahajasm.core.BlahajFMLCallHook";
    }

    @Override
    public void injectData(Map<String, Object> data) { }

    @Override
    public String getAccessTransformerClass() {
        return "mirror.blahajasm.core.BlahajTransformer";
    }

    @Override
    public List<String> getMixinConfigs() {
        return isClient ? Arrays.asList(
                "mixins.devenv.json",
                "mixins.internal.json",
                "mixins.vanities.json",
                "mixins.registries.json",
                "mixins.stripitemstack.json",
                "mixins.lockcode.json",
                "mixins.recipes.json",
                "mixins.misc_fluidregistry.json",
                "mixins.forgefixes.json",
                "mixins.capability.json",
                "mixins.singletonevents.json",
                "mixins.efficienthashing.json",
                "mixins.crashes.json",
                "mixins.fix_mc129057.json",
                "mixins.bucket.json",
                "mixins.priorities.json",
                "mixins.rendering.json",
                "mixins.datastructures_modelmanager.json",
                "mixins.screenshot.json",
                "mixins.ondemand_sprites.json",
                "mixins.searchtree_vanilla.json",
                "mixins.resolve_mc2071.json",
                "mixins.fix_mc_skindownloading.json",
                "mixins.fix_mc186052.json") :
                Arrays.asList(
                        "mixins.devenv.json",
                        "mixins.vfix_bugfixes.json",
                        "mixins.internal.json",
                        "mixins.vanities.json",
                        "mixins.registries.json",
                        "mixins.stripitemstack.json",
                        "mixins.lockcode.json",
                        "mixins.recipes.json",
                        "mixins.misc_fluidregistry.json",
                        "mixins.forgefixes.json",
                        "mixins.capability.json",
                        "mixins.singletonevents.json",
                        "mixins.efficienthashing.json",
                        "mixins.priorities.json",
                        "mixins.crashes.json",
                        "mixins.fix_mc129057.json");
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        if (FMLLaunchHandler.isDeobfuscatedEnvironment() && "mixins.devenv.json".equals(mixinConfig)) {
            return true;
        }
        if (isClient) {
            switch (mixinConfig) {
                case "mixins.bucket.json":
                    return BlahajConfig.instance.reuseBucketQuads;
                case "mixins.rendering.json":
                    return BlahajConfig.instance.optimizeSomeRendering;
                case "mixins.datastructures_modelmanager.json":
                    return BlahajConfig.instance.moreModelManagerCleanup;
                case "mixins.screenshot.json":
                    return BlahajConfig.instance.releaseScreenshotCache || BlahajConfig.instance.asyncScreenshot;
                case "mixins.ondemand_sprites.json":
                    return BlahajConfig.instance.onDemandAnimatedTextures;
                case "mixins.resolve_mc2071.json":
                    return BlahajConfig.instance.resolveMC2071;
                case "mixins.fix_mc_skindownloading.json":
                    return BlahajConfig.instance.limitSkinDownloadingThreads;
            }
        }
        switch (mixinConfig) {
            case "mixins.registries.json":
                return BlahajConfig.instance.optimizeRegistries;
            case "mixins.stripitemstack.json":
                return BlahajConfig.instance.stripNearUselessItemStackFields;
            case "mixins.lockcode.json":
                return BlahajConfig.instance.lockCodeCanonicalization;
            case "mixins.recipes.json":
                return BlahajConfig.instance.optimizeFurnaceRecipeStore;
            case "mixins.misc_fluidregistry.json":
                return BlahajConfig.instance.quickerEnableUniversalBucketCheck;
            case "mixins.forgefixes.json":
                return BlahajConfig.instance.fixFillBucketEventNullPointerException || BlahajConfig.instance.fixTileEntityOnLoadCME;
            case "mixins.capability.json":
                return BlahajConfig.instance.delayItemStackCapabilityInit;
            case "mixins.singletonevents.json":
                return BlahajConfig.instance.makeEventsSingletons;
            case "mixins.efficienthashing.json":
                return BlahajConfig.instance.efficientHashing;
            case "mixins.crashes.json":
                return BlahajConfig.instance.crashReportImprovements;
            case "mixins.fix_mc129057.json":
                return BlahajConfig.instance.fixMC129057;
            case "mixins.priorities.json":
                return BlahajConfig.instance.threadPriorityFix;
        }
        return true;
    }

}