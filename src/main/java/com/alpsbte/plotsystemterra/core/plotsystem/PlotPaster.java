package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.PlotSystemTerra;
import com.alpsbte.plotsystemterra.core.config.ConfigPaths;
import com.alpsbte.plotsystemterra.core.model.CityProject;
import com.alpsbte.plotsystemterra.core.model.Plot;
import com.alpsbte.plotsystemterra.utils.Utils;
import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;

public class PlotPaster extends Thread {
    private static String serverName = null;
    private final int pasteInterval;
    public final World world;
    private final boolean broadcastMessages;

    public PlotPaster() {
        FileConfiguration config = PlotSystemTerra.getPlugin().getConfig();

        serverName = config.getString(ConfigPaths.SERVER_NAME);
        this.world = Bukkit.getWorld(Objects.requireNonNull(config.getString(ConfigPaths.WORLD_NAME)));
        this.pasteInterval = config.getInt(ConfigPaths.PASTING_INTERVAL);
        this.broadcastMessages = config.getBoolean(ConfigPaths.BROADCAST_INFO);
    }

    @Override
    public void run() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(PlotSystemTerra.getPlugin(), () ->
            CompletableFuture.supplyAsync(() -> PlotSystemTerra.getDataProvider().getPlotDataProvider().getPlotsToPaste())
                .orTimeout((long) 60.0, TimeUnit.SECONDS)
                .whenComplete((plots, error) -> {

                if(error != null) {
                    PlotSystemTerra.getPlugin().getComponentLogger().error(text("Error occurred fetching plot to paste"), error);
                    return;
                }

                PlotSystemTerra.getPlugin().getComponentLogger().info(text("Fetched #" + plots.size() + " to paste"));

                int pastedPlots = 0;
                for (Plot plot : plots) {
                    CityProject cityProject = PlotSystemTerra.getDataProvider().getCityProjectDataProvider().getCityProject(plot.getCityProjectId());
                    // paste schematic
                    try {
                        if (pastePlotSchematic(plot, cityProject, world, plot.getCompletedSchematic(), plot.getPlotVersion())) {
                            pastedPlots++;
                        }
                    } catch (Exception e) {
                        PlotSystemTerra.getPlugin().getComponentLogger().error(text("An error occurred while pasting plot #" + plot.getId()), e);
                    }
                }

                if (broadcastMessages && pastedPlots != 0) {
                    Bukkit.broadcast(Utils.ChatUtils.getInfoFormat(text("Pasted ", GREEN)
                        .append(text(pastedPlots, GOLD)
                        .append(text(" plot" + (pastedPlots > 1 ? "s" : "") + "!", GREEN)))));
                }
            })
        , 0L, 20L * pasteInterval);
    }

    public static boolean pastePlotSchematic(Plot plot, CityProject city, World world, byte[] completedSchematic, double plotVersion) throws WorldEditException {
        // check server name
        if (serverName == null) {
            PlotSystemTerra.getPlugin().getComponentLogger().error(text("Server name is not configured properly! Unable to paste plots."));
            return false;
        }

        // if (serverName.equals(city.getServerName())) return false;

        // check mc version
        int[] serverVersion = getMajorMinorPatch(Bukkit.getServer().getMinecraftVersion());
        int[] plotMcVersion = getMajorMinorPatch(plot.getMcVersion());

        if (serverVersion == null) {
            PlotSystemTerra.getPlugin().getComponentLogger().error(text("Invalid server version! Aborting plot pasting."));
            return false;
        }
        if (plotMcVersion == null) {
            PlotSystemTerra.getPlugin().getComponentLogger().error(text("Invalid plot version for plot " + plot.getId() + "! Aborting plot pasting."));
            return false;
        }
        if (isMMPVersionNewer(plotMcVersion, serverVersion)) {
            PlotSystemTerra.getPlugin().getComponentLogger().error(
                    text("Plot " + plot.getId() + " was built on a newer minecraft version! Cannot paste plot! Please update to version " + plotMcVersion[0] + "." + plotMcVersion[1] + "." + plotMcVersion[2] + "!"));
            return false;
        }

        Bukkit.getScheduler().runTask(PlotSystemTerra.getPlugin(), () -> {
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(FaweAPI.getWorld(world.getName()))) {
                BlockVector3 toPaste;
                if (!(plotVersion >= 3)) {
                    PlotSystemTerra.getPlugin().getComponentLogger().error(text("Cannot paste plot! Plot version " + plotVersion + "is no longer supported! Must be at least 3!"));
                    return;
                }

                ByteArrayInputStream inputStream = new ByteArrayInputStream(completedSchematic);
                try (ClipboardReader reader = BuiltInClipboardFormat.FAST_V2.getReader(inputStream)) {
                    Clipboard completedClipboard = reader.read();
                    BlockVector3 plotOriginOutline = completedClipboard.getOrigin();
                    toPaste = BlockVector3.at(plotOriginOutline.x(), plotOriginOutline.y(), plotOriginOutline.z());
                    PlotSystemTerra.getPlugin().getComponentLogger().info(text("Pasting plot at " + toPaste.toParserString()));


                    Operation clipboardHolder = new ClipboardHolder(completedClipboard)
                            .createPaste(editSession)
                            .to(toPaste)
                            .ignoreAirBlocks(true)
                            .build();
                    Operations.complete(clipboardHolder);
                }

                PlotSystemTerra.getDataProvider().getPlotDataProvider().setPastedAsync(plot.getId())
                        .thenRun(() -> PlotSystemTerra.getPlugin().getComponentLogger().info(text("Plot #" + plot.getId() + " successfully marked as pasted!")));

            } catch (Exception e) {
                PlotSystemTerra.getPlugin().getComponentLogger().error(text("An error occurred while pasting plot #" + plot.getId()), e);
            }
        });
        return true;
    }

    private static int[] getMajorMinorPatch(String version) {
        int[] output = new int[3];
        String[] versionArr = version.split("\\.");

        // Invalid version!
        if (versionArr.length < 1 || versionArr.length > 3) return null;

        // Major
        output[0] = Integer.parseInt(versionArr[0]);

        // Minor
        output[1] = (versionArr.length > 1) ? Integer.parseInt(versionArr[1]) : 0;

        // Patch
        output[2] = (versionArr.length > 2) ? Integer.parseInt(versionArr[2]) : 0;

        return output;
    }

    private static boolean isMMPVersionNewer(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            if (a[i] > b[i]) return true; // A is newer than B
            if (a[i] < b[i]) return false; // A is older than B
        }

        return false; // A and B are the same
    }
}
