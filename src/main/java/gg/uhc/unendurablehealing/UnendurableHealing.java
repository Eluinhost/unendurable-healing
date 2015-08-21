package gg.uhc.unendurablehealing;

import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;

import java.util.List;
import java.util.Random;

public class UnendurableHealing implements Listener {

    protected static final ItemStack AIR = new ItemStack(Material.AIR);
    protected static final Random RANDOM = new Random();

    protected double multiplier = 0D;

    /**
     * Sets up with the default multiplier of 0 (no durability loss)
     */
    public UnendurableHealing() {}

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        // skip other sources (mainly to allow regen from plugins not to break armour)
        switch (event.getRegainReason()) {
            case REGEN:
            case MAGIC:
            case MAGIC_REGEN:
            case SATIATED:
            case EATING:
                break;
            default:
                return;
        }

        damageAllItems((Player) event.getEntity(), event.getAmount());
    }

    public void damageAllItems(Player player, double damage) {
        damageInventoryItems(player, damage);
        damageArmourSlots(player, damage);
        damageOnCursor(player, damage);
        damageOpenCrafting(player, damage);
    }

    protected void damageOpenCrafting(Player player, double damage) {
        // process in crafting squares
        InventoryView openInventory = player.getOpenInventory();

        // only run when a crafting invetory is open
        if(openInventory.getType() == InventoryType.CRAFTING || openInventory.getType() == InventoryType.WORKBENCH) {
            CraftingInventory top = (CraftingInventory) openInventory.getTopInventory();

            ItemStack[] matrix = top.getMatrix();
            for (Integer i : processItems(matrix, damage)) {
                matrix[i] = AIR;
                playBreakSound(player);
            }

            // replace original matrix
            top.setMatrix(matrix);
        }
    }

    protected void damageOnCursor(Player player, double damage) {
        // cursor has a little bit of an update problem client side, but fixes itself
        if (processItems(new ItemStack[]{player.getItemOnCursor()}, damage).size() > 0) {
            player.setItemOnCursor(AIR);
            playBreakSound(player);
        }
    }

    protected void damageArmourSlots(Player player, double damage) {
        PlayerInventory inventory = player.getInventory();

        ItemStack[] armour = inventory.getArmorContents();
        for (Integer i : processItems(armour, damage)) {
            armour[i] = AIR;
            playBreakSound(player);
        }

        // replace original (getArmorContents copies original)
        inventory.setArmorContents(armour);
    }

    protected void damageInventoryItems(Player player, double damage) {
        Inventory inventory = player.getInventory();

        for (Integer i :  processItems(player.getInventory().getContents(), damage)) {
            inventory.setItem(i, AIR);
            playBreakSound(player);
        }
    }

    protected static void playBreakSound(Player player) {
        // add a little variation to the pitch of the break
        player.playSound(player.getLocation(), Sound.ITEM_BREAK, RANDOM.nextFloat() + .5F, 0);
    }

    protected List<Integer> processItems(ItemStack[] contents, double damage) {
        List<Integer> brokenIndexes = Lists.newArrayList();

        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];

            // skip if empty
            if (null == stack || stack.getType() == Material.AIR) continue;

            // get base item's max durability
            short max = stack.getType().getMaxDurability();

            // skip if item cannot be damaged
            if (max == 0) continue;

            // calculate how much to do based on max + amount healed
            double damageToAdd = multiplier * max * damage;

            // ceil the returned value into a short + cap at max
            short newDurability = (short) Math.ceil(Math.min(stack.getDurability() + damageToAdd, max));

            // set the new value
            stack.setDurability(newDurability);

            // break the item if it's now broken
            if (stack.getDurability() == max) {
                brokenIndexes.add(i);
            }
        }

        return brokenIndexes;
    }
}
