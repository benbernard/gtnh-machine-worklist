package com.benbernard.machineworklist;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(
    modid = "machineworklist",
    name = "GTNH Machine Worklist",
    version = Tags.VERSION,
    dependencies = "required-after:NotEnoughItems@[2.8.44-GTNH]",
    acceptableRemoteVersions = "*")
public class MachineWorklist {

    @SidedProxy(
        clientSide = "com.benbernard.machineworklist.ClientProxy",
        serverSide = "com.benbernard.machineworklist.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }
}
