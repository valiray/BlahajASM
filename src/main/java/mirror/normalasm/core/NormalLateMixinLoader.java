package mirror.normalasm.core;

import com.google.common.collect.Lists;
import net.minecraftforge.fml.common.Loader;
import mirror.normalasm.config.NormalConfig;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.List;

public class NormalLateMixinLoader implements ILateMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Lists.newArrayList(
                "mixins.bakedquadsquasher.json",
                "mixins.modfixes_immersiveengineering.json",
                "mixins.modfixes_astralsorcery.json",
                "mixins.capability_astralsorcery.json",
                "mixins.modfixes_evilcraftcompat.json",
                "mixins.modfixes_ebwizardry.json",
                "mixins.modfixes_xu2.json",
                "mixins.modfixes_b3m.json",
                "mixins.searchtree_mod.json");
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        switch (mixinConfig) {
            case "mixins.bakedquadsquasher.json":
                return NormalTransformer.squashBakedQuads;
            case "mixins.modfixes_immersiveengineering.json":
                return NormalConfig.instance.fixBlockIEBaseArrayIndexOutOfBoundsException && Loader.isModLoaded("immersiveengineering");
            case "mixins.modfixes_evilcraftcompat.json":
                return NormalConfig.instance.repairEvilCraftEIOCompat && Loader.isModLoaded("evilcraftcompat") && Loader.isModLoaded("enderio") &&
                        Loader.instance().getIndexedModList().get("enderio").getVersion().equals("5.3.70"); // Only apply on newer EIO versions where compat was broken
            case "mixins.modfixes_ebwizardry.json":
                return NormalConfig.instance.optimizeArcaneLockRendering && Loader.isModLoaded("ebwizardry");
            case "mixins.modfixes_xu2.json":
                return (NormalConfig.instance.fixXU2CrafterCrash || NormalConfig.instance.disableXU2CrafterRendering) && Loader.isModLoaded("extrautils2");
            case "mixins.searchtree_mod.json":
                return NormalConfig.instance.replaceSearchTreeWithJEISearching && Loader.isModLoaded("jei");
            case "mixins.modfixes_astralsorcery.json":
                return NormalConfig.instance.optimizeAmuletRelatedFunctions && Loader.isModLoaded("astralsorcery");
            case "mixins.capability_astralsorcery.json":
                return NormalConfig.instance.fixAmuletHolderCapability && Loader.isModLoaded("astralsorcery");
            case "mixins.modfixes_b3m.json":
                return NormalConfig.instance.resourceLocationCanonicalization && Loader.isModLoaded("B3M"); // Stupid
        }
        return false;
    }

}
