package com.blackholeinfinite;

import net.minecraft.nbt.NBTTagCompound;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class BlackHole {
    public int id, dim;
    public double x, y, z;
    public double mass;
    public BigInteger blocksAbsorbed = BigInteger.ZERO;
    public long pending;
    public double gravRadius, destroyRadius, visualRadius, intensity;

    // progresso da varredura (persistido)
    public int cr, ci, col, lastMaxRing;
    public long sweeps;
    public long age;

    // estado transitorio
    public boolean growing;
    public double growthPerSec;
    public String activity = "idle";
    public long windowBlocks;
    public final List<int[]> visualQueue = new ArrayList<int[]>();

    public BlackHole() {}

    public BlackHole(int id, int dim, double x, double y, double z) {
        this.id = id; this.dim = dim; this.x = x; this.y = y; this.z = z;
        this.mass = BHMath.clampMass(BHConfig.startMass);
        recompute();
    }

    public void recompute() {
        mass = BHMath.clampMass(mass);
        gravRadius = BHMath.gravRadius(mass);
        destroyRadius = BHMath.destroyRadius(mass);
        visualRadius = BHMath.visualRadius(mass);
        intensity = BHMath.intensity(mass);
    }

    public void addMass(double g) { mass = BHMath.clampMass(mass + Math.max(0, BHMath.finite(g, 0))); }

    public void addBlocks(long n) { pending += n; windowBlocks += n; }

    public void flush() {
        if (pending > 0) { blocksAbsorbed = blocksAbsorbed.add(BigInteger.valueOf(pending)); pending = 0; }
    }

    public void write(NBTTagCompound t) {
        t.setInteger("id", id); t.setInteger("dim", dim);
        t.setDouble("x", x); t.setDouble("y", y); t.setDouble("z", z);
        t.setDouble("mass", mass);
        t.setString("blocks", blocksAbsorbed.add(BigInteger.valueOf(pending)).toString());
        t.setDouble("grav", gravRadius); t.setDouble("destroy", destroyRadius);
        t.setDouble("visual", visualRadius); t.setDouble("intensity", intensity);
        t.setInteger("cr", cr); t.setInteger("ci", ci); t.setInteger("col", col);
        t.setLong("sweeps", sweeps); t.setLong("age", age);
        t.setBoolean("growing", growing);
    }

    public static BlackHole read(NBTTagCompound t) {
        BlackHole h = new BlackHole();
        h.id = t.getInteger("id"); h.dim = t.getInteger("dim");
        h.x = t.getDouble("x"); h.y = t.getDouble("y"); h.z = t.getDouble("z");
        h.mass = BHMath.clampMass(t.hasKey("mass") ? t.getDouble("mass") : BHConfig.startMass);
        try { h.blocksAbsorbed = new BigInteger(t.getString("blocks")); } catch (Exception ex) { h.blocksAbsorbed = BigInteger.ZERO; }
        h.cr = t.getInteger("cr"); h.ci = t.getInteger("ci"); h.col = t.getInteger("col");
        h.sweeps = t.getLong("sweeps"); h.age = t.getLong("age");
        h.growing = t.getBoolean("growing");
        h.recompute();
        return h;
    }
}
