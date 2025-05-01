package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.alpslib.utils.AlpsUtils;
import com.alpsbte.plotsystemterra.core.plotsystem.CityProject;
import com.alpsbte.plotsystemterra.core.plotsystem.CreatePlotMenu;
import com.alpsbte.plotsystemterra.core.plotsystem.PlotCreator;
import com.alpsbte.plotsystemterra.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

import static net.kyori.adventure.text.Component.text;

public class CMD_CreatePlot implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if(sender instanceof Player) {
            if(Utils.hasPermission(sender, "createplot")) {
                try {
                    if (args.length > 1) {
                        if (args[0].equalsIgnoreCase("tutorial") && AlpsUtils.tryParseInt(args[1]) != null) {
                            PlotCreator.createTutorialPlot(((Player) sender).getPlayer(), Integer.parseInt(args[1]));
                            return true;
                        }
                        else { // manual input
                            Integer cityID = AlpsUtils.tryParseInt(args[0]);
                            Integer difficultyID = AlpsUtils.tryParseInt(args[1]);

                            if (cityID != null && difficultyID != null) {
                                if(difficultyID <= 0 || difficultyID > 3)
                                    sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Difficulty has to be 1 or 2 or 3")));
                                else PlotCreator.createPlot(((Player) sender).getPlayer(), new CityProject(cityID), difficultyID);
                                return true;
                            }
                        }
                    }
                    new CreatePlotMenu(((Player) sender).getPlayer());
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "An error occurred while opening create plot menu!", ex);
                    sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("An error occurred while opening create plot menu!")));
                }
            }
        }
        return true;
    }
}
