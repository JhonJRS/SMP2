package net.sirplop.aetherworks.blockentity;

public interface IForgePart {
    void onForgeTick(IForge forge);
    boolean isTopPart();
    boolean isInvalid();
}
