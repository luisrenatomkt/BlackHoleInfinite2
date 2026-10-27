package com.blackholeinfinite;

import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;

public class BlackHoleTicker {
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != Side.SERVER) return;
        if (!(e.world instanceof WorldServer)) return;
        WorldServer ws = (WorldServer) e.world;
        BlackHoleSavedData d = BlackHoleManager.get(ws, false);
        if (d == null || d.holes.isEmpty()) return;
        for (BlackHole h : new ArrayList<BlackHole>(d.holes)) BlackHoleEngine.tick(ws, h);
        d.markDirty();
        if (ws.getTotalWorldTime() % BHConfig.syncInterval == 0) BlackHoleManager.sync(ws, d);
    }

    @SubscribeEvent
    public void onUnload(WorldEvent.Unload e) {
        BlackHoleManager.clearCache();
    }
}
