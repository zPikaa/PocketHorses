package it.pika.pockethorses.menu;

import com.google.common.collect.Lists;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import it.pika.libs.item.ItemBuilder;
import it.pika.pockethorses.PocketHorses;
import it.pika.pockethorses.enums.Messages;
import it.pika.pockethorses.objects.ConfigHorse;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

import static it.pika.libs.chat.Chat.error;
import static it.pika.libs.chat.Chat.success;

public class ShopMenu implements InventoryProvider {

    public SmartInventory get() {
        return SmartInventory.builder()
                .id("inv")
                .title(PocketHorses.getConfigFile().getString("Shop-GUI.Title"))
                .size(PocketHorses.getConfigFile().getInt("Shop-GUI.Size.Rows"), 9)
                .provider(this)
                .manager(PocketHorses.getInventoryManager())
                .build();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        for (ConfigHorse horse : PocketHorses.getLoadedHorses()) {
            if (!horse.isBuyable())
                continue;

            contents.add(ClickableItem.of(new ItemBuilder()
                    .material(Material.valueOf(PocketHorses.getConfigFile().getString("Shop-GUI.Horse-Item.Material")))
                    .name(format(PocketHorses.getConfigFile().getString("Shop-GUI.Horse-Item.Name"), horse))
                    .lore(format(PocketHorses.getConfigFile().getStringList("Shop-GUI.Horse-Item.Lore"), horse))
                    .build(), e -> {
                player.closeInventory();

                if (PocketHorses.has(player, horse.getId()) &&
                        !PocketHorses.getConfigFile().getBoolean("Options.More-Than-Once-Same-Horse")) {
                    error(player, Messages.ALREADY_OWNED.get());
                    return;
                }

                if (!PocketHorses.getEconomy().has(player, horse.getPrice())) {
                    error(player, Messages.NOT_ENOUGH_MONEY.get());
                    return;
                }

                PocketHorses.getEconomy().withdrawPlayer(player, horse.getPrice());
                PocketHorses.getStorage().giveHorse(player, horse);

                success(player, Messages.PURCHASE_COMPLETED.get());
                if (PocketHorses.getConfigFile().getBoolean("Options.Play-Sound-When-Buy"))
                    player.playSound(player.getLocation(), Sound.valueOf(PocketHorses.
                            getConfigFile().getString("Shop-GUI.Buy-Sound")), 1F, 1F);
            }));
        }
    }

    @Override
    public void update(Player player, InventoryContents contents) {
    }

    private String format(String s, ConfigHorse horse) {
        return PocketHorses.parseColors(s)
                .replaceAll("%displayName%", horse.getDisplayName())
                .replaceAll("%speed%", String.valueOf(horse.getSpeed()))
                .replaceAll("%price%", String.valueOf(horse.getPrice()))
                .replaceAll("%jumpStrength%", String.valueOf(horse.getJumpStrength()));
    }

    private List<String> format(List<String> list, ConfigHorse horse) {
        List<String> newList = Lists.newArrayList();

        for (String s : list)
            newList.add(format(s, horse));

        return newList;
    }

}
