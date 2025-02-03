package mirror.normalasm.core;

import mirror.normalasm.NormalLogger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SpriteMixinPlugin implements IMixinConfigPlugin {
    static boolean logged = false;
    @Override
    public void onLoad(String s) { }
    @Override
    public String getRefMapperConfig() {
        return "";
    }
    @Override
    public boolean shouldApplyMixin(String s, String s1) {
        if (!logged) {
            NormalLogger.instance.error("Optifine is installed. On demand sprites won't be activated as Optifine already has Smart Animations.");
            logged = true;
        }
        return !NormalTransformer.isOptifineInstalled;
    }
    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) { }
    @Override
    public List<String> getMixins() {
        return Collections.emptyList();
    }
    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) { }
    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) { }
}
