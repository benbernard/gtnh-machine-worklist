package com.benbernard.machineworklist.bootstrap;

import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbPlugin;

/** Loaded before LWJGL, including classes excluded from Forge transformation. */
public final class WheelPlugin implements RfbPlugin {

    @Override
    public RfbClassTransformer[] makeTransformers() {
        return new RfbClassTransformer[] { new WheelTransformer() };
    }
}
