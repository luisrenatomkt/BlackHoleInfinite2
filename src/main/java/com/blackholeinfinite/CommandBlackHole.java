package com.blackholeinfinite;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class CommandBlackHole extends CommandBase {
    @Override public String getCommandName() { return "blackhole"; }
    @Override public String getCommandUsage(ICommandSender s) { return "/blackhole <spawn|remove|info|status>"; }
    @Override public int getRequiredPermissionLevel() { return 2; }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender s, String[] args, BlockPos pos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "spawn", "remove", "info", "status") : null;
    }

    private void say(ICommandSender s, String m) { s.addChatMessage(new ChatComponentText(m)); }

    @Override
    public void processCommand(ICommandSender s, String[] args) throws CommandException {
        if (args.length < 1) throw new WrongUsageException(getCommandUsage(s));
        String sub = args[0].toLowerCase();
        if (sub.equals("spawn")) {
            World w = s.getEntityWorld();
            Vec3 p = s.getPositionVector();
            BlackHole h = BlackHoleManager.spawn(w, p.xCoord, p.yCoord + 1.0, p.zCoord);
            if (h == null) say(s, "Limite de buracos negros simultaneos atingido (config: maxActiveHoles).");
            else say(s, "Buraco negro #" + h.id + " criado em " + (int) h.x + ", " + (int) h.y + ", " + (int) h.z);
        } else if (sub.equals("remove")) {
            say(s, BlackHoleManager.removeAll() + " buraco(s) negro(s) removido(s).");
        } else if (sub.equals("info")) {
            List<BlackHole> all = BlackHoleManager.all();
            if (all.isEmpty()) { say(s, "Nenhum buraco negro ativo."); return; }
            for (BlackHole h : all) {
                say(s, "== Buraco negro #" + h.id + " (dim " + h.dim + ") em " + (int) h.x + ", " + (int) h.y + ", " + (int) h.z);
                say(s, "Massa acumulada: " + BHMath.fmt(h.mass));
                say(s, "Blocos absorvidos: " + h.blocksAbsorbed.add(java.math.BigInteger.valueOf(h.pending)));
                say(s, "Raio visual: " + BHMath.fmt(h.visualRadius) + " | Alcance gravitacional: " + BHMath.fmt(h.gravRadius)
                        + " | Raio de destruicao: " + BHMath.fmt(h.destroyRadius));
                say(s, "Intensidade: " + BHMath.fmt(h.intensity) + " | Crescimento: " + BHMath.fmt(h.growthPerSec) + " blocos/s");
            }
        } else if (sub.equals("status")) {
            List<BlackHole> all = BlackHoleManager.all();
            if (all.isEmpty()) { say(s, "Nenhum buraco negro ativo."); return; }
            for (BlackHole h : all) {
                say(s, "#" + h.id + ": " + (h.growing ? "CRESCENDO" : "parado") + " - " + h.activity);
                say(s, "Varredura #" + h.sweeps + ": anel " + h.cr + "/" + h.lastMaxRing + " (idade " + (h.age / 20) + "s)");
            }
        } else throw new WrongUsageException(getCommandUsage(s));
    }
}
