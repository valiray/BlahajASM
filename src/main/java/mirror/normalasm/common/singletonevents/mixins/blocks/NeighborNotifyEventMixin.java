package mirror.normalasm.common.singletonevents.mixins.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.spongepowered.asm.mixin.*;
import mirror.normalasm.common.singletonevents.IRefreshEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.EnumSet;

@Mixin(NeighborNotifyEvent.class)
public class NeighborNotifyEventMixin extends BlockEvent implements IRefreshEvent {

    @Shadow @Final @Mutable private EnumSet<EnumFacing> notifiedSides;
    @Shadow @Final @Mutable private boolean forceRedstoneUpdate;

    @Unique private EventPriority normalPriority;
    @Unique private WeakReference<World> normalWorldRef;
    @Unique private BlockPos normalPos;
    @Unique private IBlockState normalState;

    NeighborNotifyEventMixin(World world, BlockPos pos, IBlockState state) {
        super(world, pos, state);
        throw new AssertionError();
    }

    @Override
    public World getWorld() {
        return this.normalWorldRef.get();
    }

    @Override
    public BlockPos getPos() {
        return normalPos;
    }

    @Override
    public IBlockState getState() {
        return normalState;
    }

    @Override
    public void beforeNeighborNotify(World world, BlockPos pos, IBlockState state, EnumSet<EnumFacing> notifiedSides, boolean forceRedstoneUpdate) {
        this.normalWorldRef = new WeakReference<>(world);
        this.normalPos = pos;
        this.normalState = state;
        this.notifiedSides = notifiedSides;
        this.forceRedstoneUpdate = forceRedstoneUpdate;
    }

    @Nullable
    @Override
    public EventPriority getPhase() {
        return normalPriority;
    }

    @Override
    public void setPhase(@Nonnull EventPriority next) {
        this.normalPriority = next;
    }

}
