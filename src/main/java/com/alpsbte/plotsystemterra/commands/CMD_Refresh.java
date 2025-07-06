package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.model.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.CityProjectData;
import com.alpsbte.plotsystemterra.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class CMD_Refresh implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;

        // Player with create plot permission should be able to refresh its data
        if (!player.hasPermission("plotsystem.createplot")) return true;

        sender.sendMessage(text("Fetching city project data...", GOLD));

        CityProjectData.fetchDataAsync().whenComplete((data, error) -> {

            if(error != null) {
                sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Error occurred! ("  + error.getMessage() + ')')));
                PlotSystemTerra.getPlugin().getComponentLogger().error(text("An error occurred refreshing city project data"), error);
                return;
            }

            CityProjectData.Cache cache = PlotSystemTerra.getPlugin().getCityProjectData().refreshCache(data);

            for(CityProject city : cache.get()) {
                sender.sendMessage(text("Fetched city ID: " + city.getId(), GOLD));
            }

            sender.sendMessage(text("Fetched #" + cache.get().size() + " city projects to PlotSystem-Terra", GREEN));
        });


        return true;
    }
}
