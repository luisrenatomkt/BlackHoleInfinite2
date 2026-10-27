package com.blackholeinfinite;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSavedData;

import java.util.ArrayList;
import java.util.List;

public class BlackHoleSavedData extends WorldSavedData {
    public final List<BlackHole> holes = new ArrayList<BlackHole>();
    public int nextId = 1;

    public BlackHoleSavedData(String name) { super(name); }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        holes.clear();
        nextId = Math.max(1, nbt.getInteger("nextId"));
        NBTTagList l = nbt.getTagList("holes", 10);
        for (int i = 0; i < l.tagCount(); i++) holes.add(BlackHole.read(l.getCompoundTagAt(i)));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("nextId", nextId);
        NBTTagList l = new NBTTagList();
        for (BlackHole h : holes) { NBTTagCompound t = new NBTTagCompound(); h.write(t); l.appendTag(t); }
        nbt.setTag("holes", l);
    }
}
