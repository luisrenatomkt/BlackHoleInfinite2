package com.blackholeinfinite;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class ItemBlackHoleGenerator extends Item {
    public ItemBlackHoleGenerator() {
        setUnlocalizedName("black_hole_generator");
        setCreativeTab(CreativeTabs.tabMisc);
        setMaxStackSize(16);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) return stack; // criacao validada somente pelo servidor
        if (!BHConfig.generatorEnabled) {
            player.addChatMessage(new ChatComponentText("O Black Hole Generator esta desativado na configuracao."));
            return stack;
        }
        double x, y, z;
        MovingObjectPosition mop = player.rayTrace(64.0D, 1.0F);
        if (mop != null && mop.hitVec != null) {
            x = mop.hitVec.xCoord; y = mop.hitVec.yCoord; z = mop.hitVec.zCoord;
            if (mop.sideHit != null) {
                x += mop.sideHit.getFrontOffsetX() * 2.0; y += mop.sideHit.getFrontOffsetY() * 2.0 + 0.5; z += mop.sideHit.getFrontOffsetZ() * 2.0;
            }
        } else {
            Vec3 look = player.getLookVec();
            x = player.posX + look.xCoord * 8; y = player.posY + player.getEyeHeight() + look.yCoord * 8; z = player.posZ + look.zCoord * 8;
        }
        BlackHole h = BlackHoleManager.spawn(world, x, y, z);
        if (h == null) {
            player.addChatMessage(new ChatComponentText("Limite de buracos negros simultaneos atingido."));
            return stack;
        }
        if (!player.capabilities.isCreativeMode) stack.stackSize--;
        return stack;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean adv) {
        tooltip.add(StatCollector.translateToLocal("item.black_hole_generator.tooltip1"));
        tooltip.add(StatCollector.translateToLocal("item.black_hole_generator.tooltip2"));
    }
}
