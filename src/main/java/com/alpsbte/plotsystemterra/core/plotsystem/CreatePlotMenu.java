package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.alpslib.utils.item.ItemBuilder;
import com.alpsbte.alpslib.utils.item.LoreBuilder;
import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.model.CityProject;
import com.alpsbte.plotsystemterra.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.mask.BinaryMask;
import org.ipvp.canvas.mask.Mask;
import org.ipvp.canvas.type.ChestMenu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class CreatePlotMenu {
    private final Menu createPlotMenu = ChestMenu.builder(6).title(text("Create Plot")).redraw(true).build();
    private final Menu difficultyMenu = ChestMenu.builder(3).title(text("Select Plot Difficulty")).redraw(true).build();

    private List<CityProject> cityProjects;
    private int selectedCityID = -1;

    private final Player player;

    public CreatePlotMenu(Player player) {
        this.player = player;
        player.sendMessage(Utils.ChatUtils.getInfoFormat(text("Fetching city project data...")));

        Collection<CityProject> cache = PlotSystemTerra.getPlugin().getCityProjectData().getCache();

        if(!cache.isEmpty()) {
            this.cityProjects = new ArrayList<>(cache);
            return;
        }

        // If cache doesn't exist for some reason, fetch for it again.
        CompletableFuture.supplyAsync(() -> PlotSystemTerra.getDataProvider().getCityProjectDataProvider().getCityProjects())
                .exceptionally(e -> {
                    player.sendMessage(Utils.ChatUtils.getAlertFormat(text("Could not fetch city project data!")));
                    PlotSystemTerra.getPlugin().getComponentLogger().error(text("An error occurred fetching city project data"), e);
                    return List.of();
                })
                .thenAccept(cityProjects -> {
                    this.cityProjects = cityProjects;
                    if (cityProjects.isEmpty()) {
                        player.sendMessage(Utils.ChatUtils.getAlertFormat(text("No city projects were found for your build team!")));
                        return;
                    }
                    Bukkit.getScheduler().runTask(PlotSystemTerra.getPlugin(), this::openCityProjectUI);
                });
    }

    public void openCityProjectUI() {
        Mask mask = BinaryMask.builder(createPlotMenu)
                .item(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE, 1).setName(empty()).build())
                .pattern("111101111") // First row
                .pattern("000000000") // Second row
                .pattern("000000000") // Third row
                .pattern("000000000") // Fourth row
                .pattern("000000000") // Fifth row
                .pattern("111010111") // Sixth row
                .build();
        mask.apply(createPlotMenu);

        createPlotMenu.getSlot(4).setItem(getStats(player.getLocation()));

        // Set City Project items
        for (int i = 0; i < cityProjects.size(); i++) {
            int cityID = i;

            ItemStack cityProjectItem;
            try {
                cityProjectItem = cityProjects.get(i).getItem();
            } catch (DataException e) {
                cityProjectItem = new ItemBuilder(Material.BARRIER)
                        .setName(text("Error", RED, BOLD))
                        .setLore(new LoreBuilder()
                                .addLine("Could not load city project.")
                                .build())
                        .build();
            }
            createPlotMenu.getSlot(9 + i).setItem(cityProjectItem);
            createPlotMenu.getSlot(9 + i).setClickHandler((clickPlayer, clickInformation) -> {
                if (selectedCityID != cityID) {
                    selectedCityID = cityID;
                    createPlotMenu.getSlot(4).setItem(getStats(player.getLocation()));

                    clickPlayer.playSound(clickPlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                }
            });
        }

        createPlotMenu.getSlot(48).setItem(
                new ItemBuilder(Material.GREEN_WOOL, 1)
                        .setName(text("Continue", GREEN, BOLD))
                        .build());
        createPlotMenu.getSlot(48).setClickHandler((clickPlayer, clickInformation) -> {
            clickPlayer.closeInventory();
            getDifficultyMenu().open(clickPlayer);
        });

        createPlotMenu.getSlot(50).setItem(
                new ItemBuilder(Material.RED_WOOL, 1)
                        .setName(text("Cancel", GREEN, BOLD))
                        .build());
        createPlotMenu.getSlot(50).setClickHandler((clickPlayer, clickInformation)
                -> clickPlayer.closeInventory());

        createPlotMenu.open(player);
    }

    public Menu getDifficultyMenu() {
        CityProject cityProject = cityProjects.get(selectedCityID);

        // Set glass border
        Mask mask = BinaryMask.builder(createPlotMenu)
                .item(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE, 1).setName(empty()).build())
                .pattern("111111111") // First row
                .pattern("000000000") // Second row
                .pattern("111111111") // Third row
                .build();
        mask.apply(difficultyMenu);

        difficultyMenu.getSlot(10).setItem(
                new ItemBuilder(Material.LIME_WOOL, 1)
                        .setName(text("Easy", GREEN, BOLD))
                        .build()
        );

        difficultyMenu.getSlot(10).setClickHandler((clickPlayer, clickInformation) -> {
            clickPlayer.closeInventory();
            PlotCreator.createPlot(clickPlayer, cityProject, "easy");
        });

        difficultyMenu.getSlot(13).setItem(
                new ItemBuilder(Material.ORANGE_WOOL, 1)
                        .setName(text("Medium", GOLD, BOLD))
                        .build()
        );

        difficultyMenu.getSlot(13).setClickHandler((clickPlayer, clickInformation) -> {
            clickPlayer.closeInventory();
            PlotCreator.createPlot(clickPlayer, cityProject, "medium");
        });

        difficultyMenu.getSlot(16).setItem(
                new ItemBuilder(Material.RED_WOOL, 1)
                        .setName(text("Hard", RED, BOLD))
                        .build()
        );

        difficultyMenu.getSlot(16).setClickHandler((clickPlayer, clickInformation) -> {
            clickPlayer.closeInventory();
            PlotCreator.createPlot(clickPlayer, cityProject, "hard");
        });

        return difficultyMenu;
    }

    private ItemStack getStats(Location coords) {
        return new ItemBuilder((selectedCityID == -1) ? new ItemStack(Material.NAME_TAG) : cityProjects.get(selectedCityID).getItem())
                .setName(text("STATS", GOLD, BOLD))
                .setLore(new LoreBuilder()
                        .addLines(text("X: ", AQUA).append(text(coords.getX(), GRAY)),
                                text("Y: ", AQUA).append(text(coords.getY(), GRAY)),
                                text("Z: ", AQUA).append(text(coords.getZ(), GRAY)),
                                text("City: ", AQUA).append(text(((selectedCityID != -1) ? cityProjects.get(selectedCityID).getId() : "none"), GRAY)))
                        .build())
                .build();
    }
}
