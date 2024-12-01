package mirror.blahajasm.api;

import mirror.blahajasm.config.BlahajConfig;

/**
 * {@link net.minecraft.item.ItemStack} implements this at runtime.
 *
 * This interface aids the delaying of capabilities initialization if {@link BlahajConfig#delayItemStackCapabilityInit} == true
 */
public interface IItemStackCapabilityDelayer {

    boolean hasInitializedCapabilities();

    /**
     * Can only run when {@link IItemStackCapabilityDelayer#hasInitializedCapabilities()} == true
     */
    void initializeCapabilities();

}
