package com.afofa555.eyeofpower;

import com.afofa555.eyeofpower.network.ModNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(EyeOfPowerMod.MODID)
public class EyeOfPowerMod {
    public static final String MODID = "eyeofpower";

    public EyeOfPowerMod(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);
    }
}
