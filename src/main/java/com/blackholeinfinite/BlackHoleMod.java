package com.blackholeinfinite;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = BlackHoleMod.MODID, name = "Black Hole Infinite", version = "1.0.0", acceptedMinecraftVersions = "[1.8.9]")
public class BlackHoleMod {
    public static final String MODID = "blackholeinfinite";

    @Mod.Instance(MODID)
    public static BlackHoleMod instance;

    @SidedProxy(clientSide = "com.blackholeinfinite.ClientProxy", serverSide = "com.blackholeinfinite.CommonProxy")
    public static CommonProxy proxy;

    public static SimpleNetworkWrapper network;
    public static Item generator;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        BHConfig.load(e.getSuggestedConfigurationFile());
        generator = new ItemBlackHoleGenerator();
        GameRegistry.registerItem(generator, "black_hole_generator");
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        network.registerMessage(SyncMessage.Handler.class, SyncMessage.class, 0, Side.CLIENT);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        GameRegistry.addRecipe(new ItemStack(generator), "OOO", "ONO", "OOO",
                'O', Blocks.obsidian, 'N', Items.nether_star);
        BlackHoleTicker ticker = new BlackHoleTicker();
        FMLCommonHandler.instance().bus().register(ticker);
        MinecraftForge.EVENT_BUS.register(ticker);
        proxy.init();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        BlackHoleManager.clearCache();
        e.registerServerCommand(new CommandBlackHole());
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent e) {
        BlackHoleManager.clearCache();
    }
}
