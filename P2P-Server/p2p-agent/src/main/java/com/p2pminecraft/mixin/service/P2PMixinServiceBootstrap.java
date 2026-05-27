package com.p2pminecraft.mixin.service;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

/**
 * Bootstrap du service Mixin P2P — enregistré via ServiceLoader.
 */
public class P2PMixinServiceBootstrap implements IMixinServiceBootstrap {

    @Override
    public String getName() { return "P2PJavaAgent"; }

    @Override
    public String getServiceClassName() {
        return "com.p2pminecraft.mixin.service.P2PMixinService";
    }

    @Override
    public void bootstrap() {}
}
