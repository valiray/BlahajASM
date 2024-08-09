package mirror.normalasm.client.sprite.ondemand;

public interface IBufferPrimerConfigurator {

    void setPrimer(IAnimatedSpritePrimer primer);

    void hookTexture(float u, float v);

}
