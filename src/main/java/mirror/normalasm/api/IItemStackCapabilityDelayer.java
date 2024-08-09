package mirror.normalasm.api;

import mirror.normalasm.config.NormalConfig;

/**
 * {@link net.minecraft.item.ItemStack} implements this at runtime.
 *
 * This interface aids the delaying of capabilities initialization if {@link NormalConfig#delayItemStackCapabilityInit} == true
 */
public interface IItemStackCapabilityDelayer {

    boolean hasInitializedCapabilities();

    /**
     * Can only run when {@link IItemStackCapabilityDelayer#hasInitializedCapabilities()} == true
     */
    void initializeCapabilities();

}
