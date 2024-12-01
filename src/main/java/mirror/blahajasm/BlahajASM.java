package mirror.blahajasm;

import codechicken.asm.ClassHierarchyManager;
import mirror.blahajasm.core.BlahajLoadingPlugin;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import mirror.blahajasm.api.mixins.RegistrySimpleExtender;
import mirror.blahajasm.config.BlahajConfig;
import mirror.blahajasm.proxy.CommonProxy;

import java.util.*;
import java.util.function.BiConsumer;

@Mod(modid = "blahajasm", name = "BlahajASM", version = BlahajLoadingPlugin.VERSION, dependencies = "required-after:mixinbooter@[4.2,);after:jei")
public class BlahajASM {

    @SidedProxy(modId = "blahajasm", clientSide = "mirror.blahajasm.proxy.ClientProxy", serverSide = "mirror.blahajasm.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static List<RegistrySimpleExtender> simpleRegistryInstances = new ArrayList<>();

    public static BiConsumer<TileEntity, NBTTagCompound> customTileDataConsumer;

    static {
        if (BlahajConfig.instance.cleanupChickenASMClassHierarchyManager && BlahajReflector.doesClassExist("codechicken.asm.ClassHierarchyManager")) {
            // EXPERIMENTAL: As far as I know, this functionality of ChickenASM isn't actually used by any coremods that depends on ChickenASM
            BlahajLogger.instance.info("Replacing ClassHierarchyManager::superclasses with a dummy map.");
            ClassHierarchyManager.superclasses = new HashMap() {
                @Override
                public Object put(Object key, Object value) {
                    return value;
                }
            };
        }
    }

    @Mod.EventHandler
    public void construct(FMLConstructionEvent event) {
        proxy.construct(event);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.throwIncompatibility();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.loadComplete(event);
    }

}
