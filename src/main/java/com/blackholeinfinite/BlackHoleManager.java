package com.blackholeinfinite;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.*;

public class BlackHoleManager {
    private static final String NAME = "blackholeinfinite_dim";
    private static final Map<Integer, BlackHoleSavedData> cache = new HashMap<Integer, BlackHoleSavedData>();
    private static final Set<Integer> missing = new HashSet<Integer>();

    public static void clearCache() { cache.clear(); missing.clear(); }

    public static BlackHoleSavedData get(World w, boolean create) {
        int dim = w.provider.getDimensionId();
        BlackHoleSavedData d = cache.get(dim);
        if (d != null) return d;
        if (!create && missing.contains(dim)) return null;
        String name = NAME + dim;
        d = (BlackHoleSavedData) w.getMapStorage().loadData(BlackHoleSavedData.class, name);
        if (d == null) {
            if (!create) { missing.add(dim); return null; }
            d = new BlackHoleSavedData(name);
            w.getMapStorage().setData(name, d);
        }
        cache.put(dim, d);
        missing.remove(dim);
        return d;
    }

    public static List<BlackHole> all() {
        List<BlackHole> out = new ArrayList<BlackHole>();
        MinecraftServer s = MinecraftServer.getServer();
        if (s == null) return out;
        for (WorldServer ws : s.worldServers) {
            if (ws == null) continue;
            BlackHoleSavedData d = get(ws, false);
            if (d != null) out.addAll(d.holes);
        }
        return out;
    }

    public static BlackHole spawn(World w, double x, double y, double z) {
        if (BHConfig.maxActiveHoles > 0 && all().size() >= BHConfig.maxActiveHoles) return null;
        BlackHoleSavedData d = get(w, true);
        BlackHole h = new BlackHole(d.nextId++, w.provider.getDimensionId(), x, y, z);
        d.holes.add(h);
        d.markDirty();
        sync(w, d);
        return h;
    }

    public static int removeAll() {
        int n = 0;
        MinecraftServer s = MinecraftServer.getServer();
        if (s == null) return 0;
        for (WorldServer ws : s.worldServers) {
            if (ws == null) continue;
            BlackHoleSavedData d = get(ws, false);
            if (d != null && !d.holes.isEmpty()) {
                n += d.holes.size();
                d.holes.clear();
                d.markDirty();
                sync(ws, d);
            }
        }
        return n;
    }

    public static void sync(World w, BlackHoleSavedData d) {
        SyncMessage m = new SyncMessage();
        m.dim = w.provider.getDimensionId();
        for (BlackHole h : d.holes) {
            SyncMessage.Entry e = new SyncMessage.Entry();
            e.id = h.id; e.x = h.x; e.y = h.y; e.z = h.z;
            e.visual = h.visualRadius; e.grav = h.gravRadius; e.destroy = h.destroyRadius; e.mass = h.mass;
            int[] flat = new int[h.visualQueue.size() * 4];
            for (int i = 0; i < h.visualQueue.size(); i++) {
                int[] p = h.visualQueue.get(i);
                flat[i * 4] = p[0]; flat[i * 4 + 1] = p[1]; flat[i * 4 + 2] = p[2]; flat[i * 4 + 3] = p[3];
            }
            h.visualQueue.clear();
            e.absorbed = flat;
            m.entries.add(e);
        }
        BlackHoleMod.network.sendToDimension(m, m.dim);
    }
}
