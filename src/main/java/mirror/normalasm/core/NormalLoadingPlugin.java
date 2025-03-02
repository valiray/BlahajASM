package mirror.normalasm.core;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import mirror.normalasm.UnsafeNormal;
import mirror.normalasm.config.NormalConfig;
import mirror.normalasm.NormalLogger;
import mirror.normalasm.api.DeobfuscatingRewritePolicy;
import mirror.normalasm.api.StacktraceDeobfuscator;
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

@IFMLLoadingPlugin.Name("NormalASM")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
public class NormalLoadingPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    public static final String VERSION = "5.25";

    public static final boolean isDeobf = FMLLaunchHandler.isDeobfuscatedEnvironment();


    // public static final boolean isModDirectorInstalled = NormalReflector.doesTweakExist("net.jan.moddirector.launchwrapper.ModDirectorTweaker");
    public static final boolean isVMOpenJ9 = SystemUtils.JAVA_VM_NAME.toLowerCase(Locale.ROOT).contains("openj9");
    public static final boolean isClient = FMLLaunchHandler.side() == Side.CLIENT;

    public NormalLoadingPlugin() {
        NormalLogger.instance.info("BlahajASM is on the {}-side.", isClient ? "client" : "server");
        NormalLogger.instance.info("BlahajASM is preparing and loading in mixins since Rongmario's too lazy to write pure ASM at times despite the mod being called 'BlahajASM'");
        if (NormalConfig.instance.outdatedCaCertsFix) {
            try (InputStream is = this.getClass().getResource("/cacerts").openStream()) {
                File cacertsCopy = File.createTempFile("cacerts", "");
                cacertsCopy.deleteOnExit();
                FileUtils.copyInputStreamToFile(is, cacertsCopy);
                System.setProperty("javax.net.ssl.trustStore", cacertsCopy.getAbsolutePath());
                NormalLogger.instance.warn("Replacing CA Certs with an updated one...");
            } catch (Exception e) {
                NormalLogger.instance.warn("Unable to replace CA Certs.", e);
            }
        }
        if (NormalConfig.instance.removeForgeSecurityManager) {
            UnsafeNormal.removeFMLSecurityManager();
        }
        if (NormalConfig.instance.crashReportImprovements || NormalConfig.instance.rewriteLoggingWithDeobfuscatedNames) {
            File modDir = new File(Launch.minecraftHome, "config/normalasm");
            modDir.mkdirs();
            // Initialize StacktraceDeobfuscator
            NormalLogger.instance.info("Initializing StacktraceDeobfuscator...");
            try {
                File mappings = new File(modDir, "methods-stable_39.csv");
                if (mappings.exists()) {
                    NormalLogger.instance.info("Found MCP stable-39 method mappings: {}", mappings.getName());
                } else {
                    NormalLogger.instance.info("Downloading MCP stable-39 method mappings to: {}", mappings.getName());
                }
                StacktraceDeobfuscator.init(mappings);
            } catch (Exception e) {
                NormalLogger.instance.error("Failed to get MCP stable-39 data!", e);
            }
            NormalLogger.instance.info("Initialized StacktraceDeobfuscator.");
            if (NormalConfig.instance.rewriteLoggingWithDeobfuscatedNames) {
                NormalLogger.instance.info("Installing DeobfuscatingRewritePolicy...");
                DeobfuscatingRewritePolicy.install();
                NormalLogger.instance.info("Installed DeobfuscatingRewritePolicy.");
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
                    NormalLogger.instance.fatal("BlahajASM notices that you're using Eclipse OpenJ9 {}!", SystemUtils.JAVA_VERSION);
                    NormalLogger.instance.fatal("This OpenJ9 version is outdated and contains a critical bug: https://github.com/eclipse-openj9/openj9/issues/8353");
                    NormalLogger.instance.fatal("Either use '-Xjit:disableGuardedStaticFinalFieldFolding' as part of your java arguments, or update OpenJ9!");
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
        return "mirror.normalasm.core.NormalFMLCallHook";
    }

    @Override
    public void injectData(Map<String, Object> data) { }

    @Override
    public String getAccessTransformerClass() {
        return "mirror.normalasm.core.NormalTransformer";
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
                    return NormalConfig.instance.reuseBucketQuads;
                case "mixins.rendering.json":
                    return NormalConfig.instance.optimizeSomeRendering;
                case "mixins.datastructures_modelmanager.json":
                    return NormalConfig.instance.moreModelManagerCleanup;
                case "mixins.screenshot.json":
                    return NormalConfig.instance.releaseScreenshotCache || NormalConfig.instance.asyncScreenshot;
                case "mixins.resolve_mc2071.json":
                    return NormalConfig.instance.resolveMC2071;
                case "mixins.fix_mc_skindownloading.json":
                    return NormalConfig.instance.limitSkinDownloadingThreads;
            }
        }
        switch (mixinConfig) {
            case "mixins.registries.json":
                return NormalConfig.instance.optimizeRegistries;
            case "mixins.stripitemstack.json":
                return NormalConfig.instance.stripNearUselessItemStackFields;
            case "mixins.lockcode.json":
                return NormalConfig.instance.lockCodeCanonicalization;
            case "mixins.recipes.json":
                return NormalConfig.instance.optimizeFurnaceRecipeStore;
            case "mixins.misc_fluidregistry.json":
                return NormalConfig.instance.quickerEnableUniversalBucketCheck;
            case "mixins.forgefixes.json":
                return NormalConfig.instance.fixFillBucketEventNullPointerException || NormalConfig.instance.fixTileEntityOnLoadCME;
            case "mixins.capability.json":
                return NormalConfig.instance.delayItemStackCapabilityInit;
            case "mixins.singletonevents.json":
                return NormalConfig.instance.makeEventsSingletons;
            case "mixins.efficienthashing.json":
                return NormalConfig.instance.efficientHashing;
            case "mixins.crashes.json":
                return NormalConfig.instance.crashReportImprovements;
            case "mixins.fix_mc129057.json":
                return NormalConfig.instance.fixMC129057;
            case "mixins.priorities.json":
                return NormalConfig.instance.threadPriorityFix;
        }
        return true;
    }

}