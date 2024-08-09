package mirror.normalasm.common.internal.mixins;

import com.google.common.base.Strings;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.*;
import org.apache.logging.log4j.ThreadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import mirror.normalasm.config.NormalConfig;
import mirror.normalasm.spark.NormalSparker;

import javax.annotation.Nullable;

@Mixin(value = LoadController.class, priority = -1000, remap = false)
public abstract class LoadControllerMixin {

    @Shadow(remap = false) private ModContainer activeContainer;

    @Unique private static Boolean hasSpark;
    @Unique private static boolean gameHasLoaded = false;

    @Shadow(remap = false) @Nullable protected abstract ModContainer findActiveContainerFromStack();

    /**
     * @author Rongmario
     * @reason Allow a faster lookup through ThreadContext first as some contexts submit modIds through this way
     */
    @Nullable
    @Overwrite
    public ModContainer activeContainer() {
        if (activeContainer == null) {
            String modId = ThreadContext.get("mod");
            if (Strings.isNullOrEmpty(modId)) {
                return findActiveContainerFromStack();
            }
            ModContainer container = Loader.instance().getIndexedModList().get(modId);
            return container == null ? findActiveContainerFromStack() : container;
        }
        return activeContainer;
    }

    /**
     * MixinBooter injects into this exact same method
     */
    @Inject(method = "propogateStateMessage", at = @At("HEAD"))
    private void injectBeforeDistributingState(FMLEvent stateEvent, CallbackInfo ci) {
        if (hasSpark == null) {
            hasSpark = Loader.isModLoaded("spark");
        }
        if (hasSpark) {
            if (stateEvent instanceof FMLStateEvent) {
                if (stateEvent instanceof FMLConstructionEvent) {
                    if (NormalConfig.instance.sparkProfileCoreModLoading) {
                        NormalSparker.stop("coremod");
                    }
                    if (NormalConfig.instance.sparkProfileConstructionStage) {
                        NormalSparker.start(LoaderState.CONSTRUCTING.toString());
                    }
                } else if (stateEvent instanceof FMLPreInitializationEvent) {
                    if (NormalConfig.instance.sparkProfileConstructionStage) {
                        NormalSparker.stop(LoaderState.CONSTRUCTING.toString());
                    }
                    if (NormalConfig.instance.sparkProfilePreInitializationStage) {
                        NormalSparker.start(LoaderState.PREINITIALIZATION.toString());
                    }
                } else if (stateEvent instanceof FMLInitializationEvent) {
                    if (NormalConfig.instance.sparkProfilePreInitializationStage) {
                        NormalSparker.stop(LoaderState.PREINITIALIZATION.toString());
                    }
                    if (NormalConfig.instance.sparkProfileInitializationStage) {
                        NormalSparker.start(LoaderState.INITIALIZATION.toString());
                    }
                } else if (stateEvent instanceof FMLPostInitializationEvent) {
                    if (NormalConfig.instance.sparkProfileInitializationStage) {
                        NormalSparker.stop(LoaderState.INITIALIZATION.toString());
                    }
                    if (NormalConfig.instance.sparkProfilePostInitializationStage) {
                        NormalSparker.start(LoaderState.POSTINITIALIZATION.toString());
                    }
                } else if (stateEvent instanceof FMLLoadCompleteEvent) {
                    if (NormalConfig.instance.sparkProfilePostInitializationStage) {
                        NormalSparker.stop(LoaderState.POSTINITIALIZATION.toString());
                    }
                    if (NormalConfig.instance.sparkProfileLoadCompleteStage) {
                        NormalSparker.start(LoaderState.AVAILABLE.toString());
                    }
                } else if (stateEvent instanceof FMLServerAboutToStartEvent) {
                    if (NormalConfig.instance.sparkProfileWorldAboutToStartStage) {
                        NormalSparker.start(LoaderState.SERVER_ABOUT_TO_START.toString());
                    }
                    if (NormalConfig.instance.sparkProfileEntireWorldLoad) {
                        NormalSparker.start("world");
                    }
                } else if (stateEvent instanceof FMLServerStartingEvent) {
                    if (NormalConfig.instance.sparkProfileWorldAboutToStartStage) {
                        NormalSparker.stop(LoaderState.SERVER_ABOUT_TO_START.toString());
                    }
                    if (NormalConfig.instance.sparkProfileWorldStartingStage) {
                        NormalSparker.start(LoaderState.SERVER_STARTING.toString());
                    }
                } else if (stateEvent instanceof FMLServerStartedEvent) {
                    if (NormalConfig.instance.sparkProfileWorldStartingStage) {
                        NormalSparker.stop(LoaderState.SERVER_STARTING.toString());
                    }
                    if (NormalConfig.instance.sparkProfileWorldStartedStage) {
                        NormalSparker.start(LoaderState.SERVER_STARTED.toString());
                    }
                }
            } else if (stateEvent instanceof FMLModIdMappingEvent && !gameHasLoaded && ((FMLModIdMappingEvent) stateEvent).isFrozen) {
                if (NormalConfig.instance.sparkProfileFinalizingStage) {
                    NormalSparker.start("finalizing");
                }
            }
        }
    }

    @Inject(method = "propogateStateMessage", at = @At("RETURN"))
    private void injectAfterDistributingState(FMLEvent stateEvent, CallbackInfo ci) {
        if (hasSpark) {
            if (stateEvent instanceof FMLStateEvent) {
                if (stateEvent instanceof FMLLoadCompleteEvent) {
                    if (NormalConfig.instance.sparkProfileLoadCompleteStage) {
                        NormalSparker.stop(LoaderState.AVAILABLE.toString());
                    }
                } else if (stateEvent instanceof FMLServerStartedEvent) {
                    if (NormalConfig.instance.sparkProfileWorldStartedStage) {
                        NormalSparker.stop(LoaderState.SERVER_STARTED.toString());
                    }
                    if (NormalConfig.instance.sparkProfileEntireWorldLoad) {
                        NormalSparker.stop("world");
                    }
                    if (NormalConfig.instance.sparkSummarizeHeapSpaceAfterWorldLoads) {
                        NormalSparker.checkHeap(true, true);
                    }
                }
            } else if (stateEvent instanceof FMLModIdMappingEvent && !gameHasLoaded && ((FMLModIdMappingEvent) stateEvent).isFrozen) {
                if (NormalConfig.instance.sparkProfileFinalizingStage) {
                    NormalSparker.stop("finalizing");
                    gameHasLoaded = true; // Don't profile when this fires on serverStopped etc
                }
                if (NormalConfig.instance.sparkProfileEntireGameLoad) {
                    NormalSparker.stop("game");
                }
                if (NormalConfig.instance.sparkSummarizeHeapSpaceAfterGameLoads) {
                    NormalSparker.checkHeap(true, true);
                }
            }
        }
    }

}
