package com.blackholeinfinite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.*;

public class ClientProxy extends CommonProxy {
    public static class ClientHole {
        public int id, dim;
        public double x, y, z, visual, grav, destroy, mass;
        public long lastUpdate;
    }

    /** Bloco absorvido sendo puxado ate o nucleo (somente visual, no cliente). */
    public static class FlyingBlock {
        public int dim, stateId, age, maxAge;
        public double sx, sy, sz, cx, cy, cz;
    }

    public static final Map<Long, ClientHole> holes = new HashMap<Long, ClientHole>();
    public static final List<FlyingBlock> flying = new ArrayList<FlyingBlock>();

    @Override
    public void init() {
        Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(BlackHoleMod.generator, 0,
                new ModelResourceLocation(BlackHoleMod.MODID + ":black_hole_generator", "inventory"));
        MinecraftForge.EVENT_BUS.register(new HoleRenderer());
        FMLCommonHandler.instance().bus().register(this);
    }

    @Override
    public void handleSync(final SyncMessage m) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            public void run() { apply(m); }
        });
    }

    private static long key(int dim, int id) { return ((long) dim << 32) ^ (id & 0xffffffffL); }

    private void apply(SyncMessage m) {
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.currentTimeMillis();
        Set<Long> present = new HashSet<Long>();
        for (SyncMessage.Entry e : m.entries) {
            long k = key(m.dim, e.id);
            ClientHole h = holes.get(k);
            if (h == null) { h = new ClientHole(); holes.put(k, h); }
            h.id = e.id; h.dim = m.dim; h.x = e.x; h.y = e.y; h.z = e.z;
            h.visual = e.visual; h.grav = e.grav; h.destroy = e.destroy; h.mass = e.mass;
            h.lastUpdate = now;
            present.add(k);
            if (!BHConfig.visualEffects || mc.thePlayer == null) continue;
            for (int i = 0; i + 3 < e.absorbed.length && flying.size() < 900; i += 4) {
                double bx = e.absorbed[i] + 0.5, by = e.absorbed[i + 1] + 0.5, bz = e.absorbed[i + 2] + 0.5;
                double px = bx - mc.thePlayer.posX, py = by - mc.thePlayer.posY, pz = bz - mc.thePlayer.posZ;
                if (px * px + py * py + pz * pz > 160 * 160) continue;
                double dx = bx - e.x, dy = by - e.y, dz = bz - e.z;
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                FlyingBlock f = new FlyingBlock();
                f.dim = m.dim; f.stateId = e.absorbed[i + 3];
                f.sx = bx; f.sy = by; f.sz = bz; f.cx = e.x; f.cy = e.y; f.cz = e.z;
                f.maxAge = 12 + (int) Math.min(28, d * 0.5);
                flying.add(f);
            }
        }
        Iterator<Map.Entry<Long, ClientHole>> it = holes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, ClientHole> en = it.next();
            if (en.getValue().dim == m.dim && !present.contains(en.getKey())) it.remove();
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) { holes.clear(); flying.clear(); }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) { holes.clear(); flying.clear(); return; }

        Iterator<FlyingBlock> fi = flying.iterator();
        while (fi.hasNext()) { FlyingBlock f = fi.next(); if (++f.age >= f.maxAge) fi.remove(); }

        if (holes.isEmpty()) return;
        long now = System.currentTimeMillis();
        int dim = mc.theWorld.provider.getDimensionId();
        Iterator<ClientHole> it = holes.values().iterator();
        while (it.hasNext()) {
            ClientHole h = it.next();
            if (now - h.lastUpdate > 10000) { it.remove(); continue; }
            if (h.dim != dim || !BHConfig.visualEffects) continue;
            effects(mc, h);
        }
    }

    private void effects(Minecraft mc, ClientHole h) {
        EntityPlayerSP p = mc.thePlayer;
        World w = mc.theWorld;
        Random r = w.rand;
        double dx = h.x - p.posX, dy = h.y - p.posY, dz = h.z - p.posZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double rr = h.visual;
        int n = (int) Math.min(40, 6 + Math.log10(1 + h.mass) * 6);

        // campo de succao ao redor do jogador (aponta para o nucleo, com giro tangencial)
        if (dist < h.grav) {
            for (int i = 0; i < n; i++) {
                double px = p.posX + (r.nextDouble() - 0.5) * 28, py = p.posY + (r.nextDouble() - 0.5) * 14, pz = p.posZ + (r.nextDouble() - 0.5) * 28;
                double vx = h.x - px, vy = h.y - py, vz = h.z - pz;
                double d = Math.sqrt(vx * vx + vy * vy + vz * vz);
                if (d < 1 || d > h.grav) continue;
                double speed = 0.12 + 0.5 * (1 - d / h.grav);
                double nx = vx / d, ny = vy / d, nz = vz / d;
                w.spawnParticle(EnumParticleTypes.SPELL_WITCH, px, py, pz,
                        nx * speed - nz * 0.12, ny * speed, nz * speed + nx * 0.12);
            }
        }
        // redemoinho roxo ao redor do nucleo (so aparece perto do jogador)
        for (int i = 0; i < 60; i++) {
            double ang = r.nextDouble() * Math.PI * 2, rad = rr * (1.05 + r.nextDouble() * 1.6);
            double px = h.x + Math.cos(ang) * rad, pz = h.z + Math.sin(ang) * rad, py = h.y + (r.nextDouble() - 0.5) * rr * 0.1;
            double ex = px - p.posX, ey = py - p.posY, ez = pz - p.posZ;
            if (ex * ex + ey * ey + ez * ez > 200) continue;
            double sp = 0.25;
            w.spawnParticle(EnumParticleTypes.SPELL_WITCH, px, py, pz,
                    -Math.sin(ang) * sp - Math.cos(ang) * 0.06, 0, Math.cos(ang) * sp - Math.sin(ang) * 0.06);
        }
    }
}
