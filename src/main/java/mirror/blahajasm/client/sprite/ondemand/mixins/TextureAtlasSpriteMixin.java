package mirror.blahajasm.client.sprite.ondemand.mixins;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import mirror.blahajasm.client.sprite.ondemand.IAnimatedSpriteActivator;

@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin implements IAnimatedSpriteActivator {

    @Unique private boolean normal$active;

    @Override
    public boolean isActive() {
        return normal$active;
    }

    @Override
    public void setActive(boolean active) {
        this.normal$active = active;
    }

}
