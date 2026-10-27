package com.blackholeinfinite;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Simulacao de um buraco negro (lado servidor). Tudo e incremental e limitado por tick. */
public class BlackHoleEngine {
    private static final Map<Block, Boolean> eligibleCache = new HashMap<Block, Boolean>();

    public static void tick(WorldServer ws, BlackHole h) {
        try {
            h.age++;
            h.recompute();
            double gain = 0;
            boolean activity = false;

            if (BHConfig.entitiesEnabled) {
                double eg = processEntities(ws, h);
                if (eg > 0) { gain += eg; activity = true; }
            }

            int budget = (int) Math.min(BHConfig.maxAbsorbPerTick,
                    Math.max(1.0, BHConfig.absorbBase * (1.0 + Math.log10(1.0 + h.mass) * BHConfig.accelerationFactor)));
            int n = scanBlocks(ws, h, budget);
            if (n > 0) { h.addBlocks(n); gain += n * BHConfig.blockMass; activity = true; }

            if (gain > 0) h.addMass(gain * BHConfig.growthMultiplier);
            h.flush();
            h.growing = activity;
            h.activity = activity ? "absorvendo materia" : "varrendo regioes carregadas (sem materia elegivel)";

            if (h.age % 20 == 0) { h.growthPerSec = h.windowBlocks; h.windowBlocks = 0; }

            if (BHConfig.sounds && h.age % 50 == 0) {
                float vol = (float) Math.min(12.0, 1.0 + Math.log10(1.0 + h.mass));
                ws.playSoundEffect(h.x, h.y, h.z, "portal.portal", vol, 0.5F);
            }
        } catch (Throwable t) {
            h.activity = "erro: " + t;
            t.printStackTrace();
        }
    }

    // ------------------------------------------------------------ entidades
    private static boolean pullable(Entity e) {
        if (e.isDead || e.ridingEntity != null) return false;
        if (e instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) e;
            if (!BHConfig.playersPulled || p.capabilities.isCreativeMode || p.isSpectator()) return false;
            return true;
        }
        return e instanceof EntityItem || e instanceof EntityLivingBase || e instanceof EntityXPOrb
                || e instanceof EntityFallingBlock || e instanceof EntityTNTPrimed
                || e instanceof EntityBoat || e instanceof EntityMinecart;
    }

    private static double processEntities(WorldServer ws, BlackHole h) {
        List<Entity> list = ws.loadedEntityList;
        double R = h.gravRadius, R2 = R * R;
        double kill = Math.max(BHConfig.killZoneMin, h.visualRadius), kill2 = kill * kill;
        List<Entity> victims = null;
        double gain = 0;
        for (int i = 0; i < list.size(); i++) {
            Entity e = list.get(i);
            if (e == null || !pullable(e)) continue;
            double dx = h.x - e.posX, dy = h.y - (e.posY + e.height * 0.5), dz = h.z - e.posZ;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > R2) continue;
            if (d2 <= kill2) {
                if (e instanceof EntityPlayer) {
                    if (BHConfig.playersAbsorbed) e.attackEntityFrom(DamageSource.outOfWorld, 1.0E9F);
                } else {
                    if (victims == null) victims = new ArrayList<Entity>();
                    victims.add(e);
                    gain += BHConfig.entityMass * ((e instanceof EntityItem) ? Math.max(1, ((EntityItem) e).getEntityItem().stackSize) : 1);
                }
                continue;
            }
            double d = Math.sqrt(d2);
            double nd = Math.min(1.0, d / Math.max(R, 1e-6));
            double a = h.intensity * BHConfig.suctionSpeed * (0.2 + 1.8 * (1 - nd) * (1 - nd));
            double inv = 1.0 / Math.max(d, 1e-3);
            e.motionX += dx * inv * a; e.motionY += dy * inv * a; e.motionZ += dz * inv * a;
            double sp = Math.sqrt(e.motionX * e.motionX + e.motionY * e.motionY + e.motionZ * e.motionZ);
            if (sp > BHConfig.maxEntitySpeed) {
                double k = BHConfig.maxEntitySpeed / sp;
                e.motionX *= k; e.motionY *= k; e.motionZ *= k;
            }
            e.fallDistance = 0;
            e.velocityChanged = true;
        }
        if (victims != null) for (Entity v : victims) v.setDead();
        return gain;
    }

    // --------------------------------------------------------------- blocos
    private static boolean eligible(WorldServer ws, BlockPos pos, Block b) {
        if (BHConfig.protectUnbreakable && b.getBlockHardness(ws, pos) < 0) return false;
        if (!BHConfig.absorbLiquids && b.getMaterial().isLiquid()) return false;
        Boolean c = eligibleCache.get(b);
        if (c == null) {
            String name = String.valueOf(Block.blockRegistry.getNameForObject(b)).toLowerCase();
            boolean ok = !BHConfig.protectedBlocks.contains(name);
            if (ok && !BHConfig.absorbableBlocks.isEmpty()) ok = BHConfig.absorbableBlocks.contains(name);
            eligibleCache.put(b, ok);
            c = ok;
        }
        return c;
    }

    /**
     * Varredura em aneis de chunks ao redor do nucleo (do mais proximo ao mais distante).
     * Cursor (cr, ci, col) e persistido. Nunca varre o raio inteiro em um tick.
     */
    private static int scanBlocks(WorldServer ws, BlackHole h, int absorbBudget) {
        int absorbed = 0;
        int columnBudget = BHConfig.columnsPerTick;
        int safety = columnBudget * 4 + 512;
        int loads = 0;
        boolean restarted = false;

        double R = h.destroyRadius, R2 = R * R;
        int hx = MathHelper.floor_double(h.x), hy = MathHelper.floor_double(h.y), hz = MathHelper.floor_double(h.z);
        int hcx = hx >> 4, hcz = hz >> 4;
        double scanR = Math.min(R, (double) BHConfig.maxScanRadius);
        int maxRing = (int) Math.min(Math.ceil(scanR / 16.0) + 1, 1000000.0);
        h.lastMaxRing = maxRing;

        while (columnBudget > 0 && absorbed < absorbBudget && safety-- > 0) {
            if (h.cr > maxRing) {
                if (restarted) break;
                h.cr = 0; h.ci = 0; h.col = 0; h.sweeps++; restarted = true;
                continue;
            }
            int ringCount = h.cr == 0 ? 1 : 8 * h.cr;
            if (h.ci >= ringCount) { h.cr++; h.ci = 0; h.col = 0; continue; }

            int ox, oz;
            if (h.cr == 0) { ox = 0; oz = 0; }
            else {
                int side = h.ci / (2 * h.cr), off = h.ci % (2 * h.cr), r = h.cr;
                switch (side) {
                    case 0:  ox = -r + off; oz = -r; break;
                    case 1:  ox = r;        oz = -r + off; break;
                    case 2:  ox = r - off;  oz = r; break;
                    default: ox = -r;       oz = r - off; break;
                }
            }
            int cx = hcx + ox, cz = hcz + oz;

            double minX = cx * 16.0, minZ = cz * 16.0;
            double ddx = h.x < minX ? minX - h.x : (h.x > minX + 16 ? h.x - (minX + 16) : 0);
            double ddz = h.z < minZ ? minZ - h.z : (h.z > minZ + 16 ? h.z - (minZ + 16) : 0);
            if (ddx * ddx + ddz * ddz > R2) { h.ci++; h.col = 0; continue; }

            if (!ws.getChunkProvider().chunkExists(cx, cz)) {
                if (BHConfig.globalExpansion && loads < BHConfig.loadChunksPerTick) {
                    loads++;
                    ws.getChunkFromChunkCoords(cx, cz);
                } else { h.ci++; h.col = 0; continue; }
            }
            Chunk ch = ws.getChunkFromChunkCoords(cx, cz);

            while (h.col < 256 && columnBudget > 0 && absorbed < absorbBudget) {
                int lx = h.col & 15, lz = h.col >> 4;
                int wx = cx * 16 + lx, wz = cz * 16 + lz;
                double ex = wx + 0.5 - h.x, ez = wz + 0.5 - h.z;
                double h2 = R2 - ex * ex - ez * ez;
                if (h2 >= 0) {
                    columnBudget--;
                    int half = (int) Math.min(Math.sqrt(h2), 4096.0);
                    int lo = Math.max(0, hy - half);
                    int hi = Math.min(Math.min(255, hy + half), ch.getHeightValue(lx, lz) + 2);
                    if (lo <= hi) absorbed += absorbColumn(ws, h, wx, wz, lo, hi, hy, absorbBudget - absorbed);
                    if (absorbed >= absorbBudget) break; // recontinua na mesma coluna no proximo tick
                }
                h.col++;
            }
            if (h.col >= 256) { h.ci++; h.col = 0; }
        }
        return absorbed;
    }

    /** Absorve de dentro para fora (a partir da altura do nucleo). */
    private static int absorbColumn(WorldServer ws, BlackHole h, int wx, int wz, int lo, int hi, int hy, int limit) {
        int c = Math.max(lo, Math.min(hi, hy));
        int span = Math.max(c - lo, hi - c);
        int count = 0;
        for (int d = 0; d <= span && count < limit; d++) {
            for (int s = 0; s < 2 && count < limit; s++) {
                int y = s == 0 ? c + d : c - d;
                if (s == 1 && d == 0) continue;
                if (y < lo || y > hi) continue;
                BlockPos pos = new BlockPos(wx, y, wz);
                IBlockState st = ws.getBlockState(pos);
                Block b = st.getBlock();
                if (b == Blocks.air || !eligible(ws, pos, b)) continue;
                ws.setBlockState(pos, Blocks.air.getDefaultState(), 2);
                count++;
                if (h.visualQueue.size() < BHConfig.maxVisualBlocksPerSync)
                    h.visualQueue.add(new int[]{wx, y, wz, Block.getStateId(st)});
            }
        }
        return count;
    }
}
