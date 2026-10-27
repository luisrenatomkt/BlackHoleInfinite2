package com.blackholeinfinite;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class SyncMessage implements IMessage {
    public static class Entry {
        public int id;
        public double x, y, z, visual, grav, destroy, mass;
        public int[] absorbed = new int[0];
    }

    public int dim;
    public List<Entry> entries = new ArrayList<Entry>();

    @Override
    public void toBytes(ByteBuf b) {
        b.writeInt(dim);
        b.writeInt(entries.size());
        for (Entry e : entries) {
            b.writeInt(e.id);
            b.writeDouble(e.x); b.writeDouble(e.y); b.writeDouble(e.z);
            b.writeDouble(e.visual); b.writeDouble(e.grav); b.writeDouble(e.destroy); b.writeDouble(e.mass);
            b.writeInt(e.absorbed.length / 4);
            for (int i = 0; i < e.absorbed.length; i++) b.writeInt(e.absorbed[i]);
        }
    }

    @Override
    public void fromBytes(ByteBuf b) {
        dim = b.readInt();
        int n = b.readInt();
        for (int i = 0; i < n; i++) {
            Entry e = new Entry();
            e.id = b.readInt();
            e.x = b.readDouble(); e.y = b.readDouble(); e.z = b.readDouble();
            e.visual = b.readDouble(); e.grav = b.readDouble(); e.destroy = b.readDouble(); e.mass = b.readDouble();
            int c = b.readInt();
            e.absorbed = new int[c * 4];
            for (int k = 0; k < e.absorbed.length; k++) e.absorbed[k] = b.readInt();
            entries.add(e);
        }
    }

    public static class Handler implements IMessageHandler<SyncMessage, IMessage> {
        @Override
        public IMessage onMessage(SyncMessage m, MessageContext ctx) {
            BlackHoleMod.proxy.handleSync(m);
            return null;
        }
    }
}
