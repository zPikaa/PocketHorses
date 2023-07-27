package it.pika.pockethorses.menu;

import com.google.common.collect.Lists;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import it.pika.libs.item.ItemBuilder;
import it.pika.pockethorses.Perms;
import it.pika.pockethorses.PocketHorses;
import it.pika.pockethorses.enums.Messages;
import it.pika.pockethorses.objects.horses.ConfigHorse;
import it.pika.pockethorses.objects.horses.Horse;
import it.pika.pockethorses.objects.horses.SpawnedHorse;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

import static it.pika.libs.chat.Chat.error;
import static it.pika.libs.chat.Chat.success;

public class MyHorsesMenu implements InventoryProvider {

    public SmartInventory get() {
        return SmartInventory.builder()
                .id("inv")
                .title(PocketHorses.parseColors(PocketHorses.getConfigFile().getString("Horses-GUI.Title")))
                .size(PocketHorses.getConfigFile().getInt("Horses-GUI.Size.Rows"), 9)
                .provider(this)
                .manager(PocketHorses.getInventoryManager())
                .build();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        for (Horse horse : PocketHorses.getHorsesOf(player)) {
            var configHorse = ConfigHorse.of(horse.getName());
            if (configHorse == null)
                continue;

            if (configHorse.isPermission() && !player.hasPermission(Perms.getHorse(horse.getName())))
                continue;

            contents.add(ClickableItem.of(new ItemBuilder()
                    .material(Material.valueOf(PocketHorses.getConfigFile().getString("Horses-GUI.Horse-Item.Material")))
                    .name(PocketHorses.parseMessage(PocketHorses.getConfigFile()
                            .getString("Horses-GUI.Horse-Item.Name"), horse, player))
                    .lore(PocketHorses.parseMessage(PocketHorses.getConfigFile().getStringList("Horses-GUI.Horse-Item.Lore"), horse, player))
                    .build(), e -> {
                player.closeInventory();

                if (e.isLeftClick()) {
                    if (alreadySpawned(player, horse)) {
                        error(player, Messages.ALREADY_SPAWNED.get());
                        return;
                    }

                    if (!PocketHorses.getConfigFile().getBoolean("Options.More-Horses-At-Time")
                            && PocketHorses.getSpawnedHorses().containsKey(player.getName())) {
                        error(player, Messages.CANNOT_SPAWN.get());
                        return;
                    }

                    var cooldown = PocketHorses.getCooldowns().getRemainingCooldown(player.getUniqueId());
                    if (!cooldown.isZero() && !cooldown.isNegative()) {
                        error(player, Messages.IN_COOLDOWN.get().formatted(cooldown.toSeconds()));
                        return;
                    }

                    horse.spawn(player);
                    success(player, Messages.HORSE_SPAWNED.get());
                    return;
                }

                if (e.isRightClick()) {
                    if (!configHorse.isRecyclable() || !PocketHorses.isShopEnabled())
                        return;

                    new ConfirmMenu(() -> {
                        player.closeInventory();

                        PocketHorses.getStorage().takeHorse(player, horse);
                        PocketHorses.getEconomy().depositPlayer(player, configHorse.getRecyclePrice());

                        success(player, Messages.HORSE_RECYCLED.get());
                    }, player::closeInventory).get().open(player);
                }
            }));
        }
    }

    @Override
    public void update(Player player, InventoryContents contents) {
    }

    private boolean alreadySpawned(Player player, Horse horse) {
        if (!PocketHorses.getSpawnedHorses().containsKey(player.getName()))
            return false;

        for (SpawnedHorse spawnedHorse : PocketHorses.getSpawnedHorses().get(player.getName())) {
            if (spawnedHorse.getUuid() != horse.getUuid())
                continue;

            return true;
        }

        return false;
    }

    private List<String> parse(List<String> list, Player player) {
        List<String> newList = Lists.newArrayList();

        for (String s : list)
            newList.add(s.replaceAll("%owner%", player.getName()));

        return newList;
    }

}
