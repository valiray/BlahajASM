package mirror.normalasm.core;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import mirror.normalasm.config.NormalConfig;
import mirror.normalasm.core.classfactories.BakedQuadRedirectorFactory;

import java.util.List;
import java.util.Set;

public class NormalMixinPlugin implements IMixinConfigPlugin {

    static {
        if (NormalLoadingPlugin.isClient && NormalTransformer.squashBakedQuads) {
            BakedQuadRedirectorFactory.generateRedirectorClass();
        }
    }

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        switch (mixinClassName) {
            case "mirror.normalasm.common.forgefixes.mixins.ChunkMixin":
                return NormalConfig.instance.fixTileEntityOnLoadCME;
            case "mirror.normalasm.common.forgefixes.mixins.EntityEntryMixin":
                return NormalConfig.instance.fasterEntitySpawnPreparation;
            case "mirror.normalasm.common.forgefixes.mixins.DimensionTypeMixin":
                return NormalConfig.instance.fixDimensionTypesInliningCrash;
            case "mirror.normalasm.client.screenshot.mixins.MinecraftMixin":
                return NormalConfig.instance.copyScreenshotToClipboard;
            case "mirror.normalasm.client.screenshot.mixins.ScreenShotHelperMixin":
                return NormalConfig.instance.releaseScreenshotCache;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
