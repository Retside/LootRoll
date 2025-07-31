package me.newtale.lootRoll.utils;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;


public class RollUtils {

    public static Vector createRandomDirection() {
        double x = (ThreadLocalRandom.current().nextDouble() - 0.3) * 0.6; // -0.3 до 0.3
        double y = ThreadLocalRandom.current().nextDouble() * 0.4 + 0.1; // 0.1 до 0.5
        double z = (ThreadLocalRandom.current().nextDouble() - 0.3) * 0.6; // -0.3 до 0.3
        return new Vector(x, y, z);
    }

    public static Item createDroppedItem(Location location, ItemStack itemStack) {
        Vector randomDirection = createRandomDirection();
        Item droppedItem = location.getWorld().dropItem(location, itemStack);
        droppedItem.setVelocity(randomDirection);
        return droppedItem;
    }

    public static int generateRoll() {
        return ThreadLocalRandom.current().nextInt(1, 101);
    }
}