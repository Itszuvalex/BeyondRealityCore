package com.mcbeyondreality.beyondrealitycore.event;

import com.mcbeyondreality.beyondrealitycore.data.BannedBlocksForDimension;
import com.mcbeyondreality.beyondrealitycore.gui.GuiColor;
import com.mcbeyondreality.beyondrealitycore.handlers.ConfigHandler;
import com.mcbeyondreality.beyondrealitycore.notification.Notification;
import com.mcbeyondreality.beyondrealitycore.notification.NotificationTickHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class RightClickEvent {
    private int[] slots = {1, 2, 3, 4, 5, 6, 7, 8, 0, -1};
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void playerRightClick(PlayerInteractEvent event)
    {
        if (event.isCanceled() || event.world.isRemote ||
                event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK || !ConfigHandler.rightClick) return;
        ItemStack heldItem = event.entityPlayer.inventory.getCurrentItem();

        if (heldItem == null || !(heldItem.getItem() instanceof ItemTool)) return;
        for(String name : ConfigHandler.rightClickWhiteList)
        {
            if(heldItem.getUnlocalizedName().equals(name)) {
                int oldSlot = event.entityPlayer.inventory.currentItem;
                if (oldSlot < 0 || oldSlot > 8) return;

                int newSlot = slots[oldSlot];
                if (newSlot < 0 || newSlot > 8) return;
                ItemStack slotStack = event.entityPlayer.inventory.getStackInSlot(newSlot);

                if (slotStack == null || slotStack.getItem() instanceof ItemTool) return;

                event.entityPlayer.inventory.currentItem = newSlot;
                if(!((EntityPlayerMP) event.entityPlayer).theItemInWorldManager.activateBlockOrUseItem(event.entityPlayer, event.world, slotStack, event.x, event.y, event.z, event.face, 0.5f, 0.5f, 0.5f))
                    return;

                if (slotStack.stackSize <= 0) slotStack = null;
                event.entityPlayer.inventory.currentItem = oldSlot;
                event.entityPlayer.inventory.setInventorySlotContents(newSlot, slotStack);
                ((EntityPlayerMP) event.entityPlayer).playerNetServerHandler.sendPacket(new S2FPacketSetSlot(0, newSlot + 36, slotStack));
                event.setCanceled(true);
                return;
            }

        }


    }

    @SubscribeEvent
    public void onBlockPlace(PlayerInteractEvent event)


    {
        if(event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
        {
            if(event.entityPlayer.getCurrentEquippedItem() != null) {
                GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(event.entityPlayer.getCurrentEquippedItem().getItem());
                if(id != null) {
                    String idName = id.modId + ":" + id.name + ":" + event.entityPlayer.getCurrentEquippedItem().getItemDamage();

                    if (BannedBlocksForDimension.isBlockBanned(event.entity.dimension, idName)) {
                        if(event.entityPlayer.worldObj.isRemote)
                            NotificationTickHandler.guiNotification.queueNotification(new Notification(event.entityPlayer.getCurrentEquippedItem(), EnumChatFormatting.RED + "Object Banned", EnumChatFormatting.YELLOW + "You can't use that here"));
                        event.setCanceled(true);
                    }
                }
            }
        }
    }



    @SubscribeEvent
    public void onToolTip(ItemTooltipEvent event)
    {
        if (event.entity == null) return;

        if (BannedBlocksForDimension.isBlockBanned(event.entity.dimension, event.itemStack.getItem().getUnlocalizedName())) {
            event.toolTip.add(GuiColor.RED + "Banned in this Dimension");
        }
    }
}
