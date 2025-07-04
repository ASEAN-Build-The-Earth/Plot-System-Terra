package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.alpslib.utils.AlpsUtils;
import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.core.config.ConfigUtil;
import com.alpsbte.plotsystemterra.core.plotsystem.CreatePlotMenu;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotCreator;
import com.alpsbte.plotsystemterra.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;

public class CMD_CreatePlot implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("plotsystem.createplot")) return true;

        try {
            // Tutorial Plot (DEV ONLY)
            if (ConfigUtil.getInstance().configs[0].getBoolean(ConfigPaths.DEV_MODE)
                    && args.length > 1
                    && args[0].equalsIgnoreCase("tutorial")
                    && AlpsUtils.tryParseInt(args[1]) != null) {
                PlotCreator.createTutorialPlot(player, Integer.parseInt(args[1]));
                return true;
            }

            // manual input
            if(args.length == 2) {
                String cityID = args[0];
                String difficultyID = args[1];

                if (cityID != null && difficultyID != null) {
                    PlotCreator.createPlotManually(((Player) sender).getPlayer(), cityID, difficultyID);
                    return true;
                }
            }

            // Creation Menu
            new CreatePlotMenu(player);
        } catch (Exception ex) {
            PlotSystemTerra.getPlugin().getComponentLogger()
                    .error(text("An error occurred while opening create plot menu!"), ex);
            player.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while opening create plot menu!")));
        }
        return true;
    }
}
