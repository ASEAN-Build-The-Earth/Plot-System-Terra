/*
 *  The MIT License (MIT)
 *
 *  Copyright © 2021-2026, Alps BTE <bte.atchli@gmail.com>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.alpsbte.plotsystemterra.commands;

import com.alpsbte.alpslib.utils.AlpsUtils;
import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.data.DataException;
import com.alpsbte.plotsystemterra.core.model.Plot;
import com.alpsbte.plotsystemterra.utils.Utils;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

/**
 * Debugging command to check a plot's information by ID
 *
 * @author tin (ASEAN-BTE)
 */
public class CMD_CheckPlot implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, String @NotNull [] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("plotsystem.createplot")) return true;

        if (args.length < 1 || AlpsUtils.tryParseInt(args[0]) == null) {
            sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Incorrect Input! Try /check <ID>")));
            return true;
        }

        int plotID = Integer.parseInt(args[0]);
        String genericNotFoundMsg = "Plot with the ID " + plotID + " could not be found!";

        sender.sendMessage(Utils.ChatUtils.getInfoFormat(text("Fetching plot data...")));
        try {
            CompletableFuture.supplyAsync(() -> PlotSystemTerra.getDataProvider().getPlotDataProvider().getPlot(plotID))
                .thenAccept(plot -> onSuccess(sender, plot, plotID)).exceptionally(e -> {
                    sender.sendMessage(Utils.ChatUtils.getAlertFormat(text(genericNotFoundMsg)));
                    PlotSystemTerra.getPlugin().getComponentLogger().warn("/plotsystem-terra:check: {}", genericNotFoundMsg, e);
                    return null;
                });
        } catch (DataException e) {
            sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Plot with the ID " + plotID + " could not be found!" + " " + e.getMessage())));
            PlotSystemTerra.getPlugin().getComponentLogger().warn("/plotsystem-terra:check: {}", genericNotFoundMsg, e);
        }
        return true;
    }

    private void onSuccess(CommandSender sender, Plot plot, int plotId) {
        if (plot == null) {
            sender.sendMessage(Utils.ChatUtils.getAlertFormat(text("Plot with the ID " + plotId + " could not be found!")));
            PlotSystemTerra.getPlugin().getComponentLogger().warn("/plotsystem-terra:check: Plot ID returns Null");
            return;
        }

        String cityProjectID = plot.getCityProjectId();
        String status = plot.getStatus();
        byte[] size = plot.getCompletedSchematic();
        double version = plot.getPlotVersion();
        String mcVersion = plot.getMcVersion();

        sender.sendMessage(empty());
        sender.sendMessage(text("==============", GRAY)
                .decoration(TextDecoration.STRIKETHROUGH, true)
                .append(text(" Plot #" + plotId + ' ', YELLOW)
                        .decoration(TextDecoration.STRIKETHROUGH, false)
                        .decoration(TextDecoration.BOLD, true))
                .append(text("==============", GRAY)));
        sender.sendMessage(empty());

        sender.sendMessage(text("Status: ", YELLOW).append(text(status != null? status : "N/A", GRAY)));
        sender.sendMessage(text("City Project: ", YELLOW).append(text(cityProjectID != null? cityProjectID : "N/A", GRAY)));
        sender.sendMessage(text("Completed Schematic Size: ", YELLOW).append(text(size == null? "N/A" : size.length + " bytes", GRAY)));
        sender.sendMessage(text("Plot's Minecraft Version: ", YELLOW).append(text(mcVersion == null? "Unknown" : mcVersion , GRAY)));
        sender.sendMessage(text("Plot Version: ", YELLOW).append(text(version, GRAY)));

        String bottom = "==============================";
        sender.sendMessage(empty());
        sender.sendMessage(text(bottom, GRAY).decoration(TextDecoration.STRIKETHROUGH, true));
    }
}
