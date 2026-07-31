package com.phobia.levels.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.phobia.levels.LevelPlugin;
import com.phobia.levels.boosters.BoosterManager;
import com.phobia.levels.boosters.BoosterType;
import com.phobia.levels.data.PlayerData;

public class PlayerBoard {

    private final Player player;
    private Scoreboard scoreboard;
    private Objective objective;

    public PlayerBoard(Player player) {
        this.player = player;
        create();
    }

    public void create() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("levels", "dummy",
                ChatColor.YELLOW.toString() + ChatColor.BOLD + "Stats");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        setupTeams();

        update();
        player.setScoreboard(scoreboard);
    }

    private void setupTeams() {
        createLine("level", ChatColor.BLACK.toString(), 15);
        createLine("xp", ChatColor.DARK_BLUE.toString(), 14);
        createLine("kills", ChatColor.DARK_GREEN.toString(), 13);
        createLine("mobkills", ChatColor.AQUA.toString(), 12);
        createLine("deaths", ChatColor.DARK_AQUA.toString(), 11);
        createLine("kdr", ChatColor.DARK_RED.toString(), 10);
        createLine("pocket", ChatColor.DARK_PURPLE.toString(), 9);
        createLine("online", ChatColor.GOLD.toString(), 8);
        
        objective.getScore(ChatColor.RESET.toString()).setScore(7);

        createLine("pboost", ChatColor.GRAY.toString(), 6);
        createLine("sboost", ChatColor.BLUE.toString(), 5);
        createLine("timer", ChatColor.GREEN.toString(), 4);

        // ADDED: personal booster lines
        createLine("pboost2", ChatColor.LIGHT_PURPLE.toString(), 3);
        createLine("timer2", ChatColor.YELLOW.toString(), 2);

        objective.getScore(ChatColor.DARK_GRAY + "    mcguns.net  ").setScore(0);
    }

    private void createLine(String teamId, String entry, int score) {
        Team team = scoreboard.registerNewTeam(teamId);
        team.addEntry(entry);
        objective.getScore(entry).setScore(score);
    }

    public void update() {
        if (objective == null) return;

        PlayerData data = LevelPlugin.getInstance().getPlayerDataManager().getData(player);
        double playerMult = LevelPlugin.getInstance().getPlayerMultiplier(player);
        
        double globalXpMult = LevelPlugin.getInstance().getGlobalBooster();
        long xpBoosterTime = LevelPlugin.getInstance().getBoosterTimeRemaining();

        double globalTokenMult = LevelPlugin.getInstance().getTokenBooster();
        long tokenBoosterTime = LevelPlugin.getInstance().getTokenBoosterTimeRemaining();

        // --- NEW: Dynamic Prestige Icon Processing ---
        String prestigeIcon = "";
        int currentPrestige = data.getPrestige();
        if (currentPrestige > 0) {
            String rawIcon = LevelPlugin.getInstance().getConfig().getString("prestige-system.icons." + currentPrestige, "");
            prestigeIcon = ChatColor.translateAlternateColorCodes('&', rawIcon);
        }

        // Apply prestige icon right before the level label layout line
        updateTeamText("level", ChatColor.WHITE + "Level: " + prestigeIcon + ChatColor.GREEN + data.getLevel());
        updateTeamText("xp", ChatColor.WHITE + "XP: " + ChatColor.GREEN + data.getXp() + ChatColor.GRAY + "/" + ChatColor.GREEN + data.getRequiredXp());
        updateTeamText("kills", ChatColor.WHITE + "Kills: " + ChatColor.GREEN + data.getKills());
        updateTeamText("mobkills", ChatColor.WHITE + "Mob Kills: " + ChatColor.GREEN + data.getMobKills());
        updateTeamText("deaths", ChatColor.WHITE + "Deaths: " + ChatColor.RED + data.getDeaths());
        
        String kdrFormatted = String.format("%.2f", data.getKdr());
        updateTeamText("kdr", ChatColor.WHITE + "KDR: " + ChatColor.GOLD + kdrFormatted);
        
        updateTeamText("pocket", ChatColor.WHITE + "Pocket: " + ChatColor.YELLOW + data.getTokens());
        updateTeamText("online", ChatColor.WHITE + "Online: " + ChatColor.GREEN + Bukkit.getOnlinePlayers().size());
        
        updateTeamText("pboost", ChatColor.WHITE + "Your Boost: " + ChatColor.AQUA + "x" + playerMult);

        boolean hasXpBoost = globalXpMult > 1.0;
        boolean hasTokenBoost = globalTokenMult > 1.0;

        if (hasXpBoost && hasTokenBoost) {
            long minTime = Math.min(xpBoosterTime, tokenBoosterTime);
            long minutes = minTime / 60;
            long seconds = minTime % 60;

            updateTeamText("sboost", ChatColor.WHITE + "XP & Token Boost");
            updateTeamText("timer", ChatColor.LIGHT_PURPLE + "x" + globalXpMult + " XP " + ChatColor.YELLOW + "x" + globalTokenMult + " T (" + minutes + "m)");
        } else if (hasXpBoost) {
            long minutes = xpBoosterTime / 60;
            long seconds = xpBoosterTime % 60;

            updateTeamText("sboost", ChatColor.WHITE + "Server Boost: " + ChatColor.LIGHT_PURPLE + "x" + globalXpMult);
            updateTeamText("timer", ChatColor.GRAY + "Ends in: " + ChatColor.WHITE + minutes + "m " + seconds + "s");
        } else if (hasTokenBoost) {
            long minutes = tokenBoosterTime / 60;
            long seconds = tokenBoosterTime % 60;

            updateTeamText("sboost", ChatColor.WHITE + "Token Boost: " + ChatColor.YELLOW + "x" + globalTokenMult);
            updateTeamText("timer", ChatColor.GRAY + "Ends in: " + ChatColor.WHITE + minutes + "m " + seconds + "s");
        } else {
            updateTeamText("sboost", ChatColor.GRAY + "" + ChatColor.ITALIC + "*No active");
            updateTeamText("timer", ChatColor.GRAY + "" + ChatColor.ITALIC + " booster*");
        }

        // ADDED: personal booster display, mirrors the global block above
        BoosterManager boosterManager = LevelPlugin.getInstance().getBoosterManager();
        double personalXpMult = boosterManager.getPersonalMultiplier(player, BoosterType.XP);
        double personalTokenMult = boosterManager.getPersonalMultiplier(player, BoosterType.TOKENS);
        long personalXpTime = boosterManager.getPersonalRemainingSeconds(player, BoosterType.XP);
        long personalTokenTime = boosterManager.getPersonalRemainingSeconds(player, BoosterType.TOKENS);

        boolean hasPersonalXp = personalXpMult > 1.0;
        boolean hasPersonalToken = personalTokenMult > 1.0;

        if (hasPersonalXp && hasPersonalToken) {
            long minTime = Math.min(personalXpTime, personalTokenTime);
            long minutes = minTime / 60;

            updateTeamText("pboost2", ChatColor.WHITE + "Personal Boost:");
            updateTeamText("timer2", ChatColor.LIGHT_PURPLE + "x" + personalXpMult + " XP " + ChatColor.YELLOW + "x" + personalTokenMult + " T (" + minutes + "m)");
        } else if (hasPersonalXp) {
            long minutes = personalXpTime / 60;
            long seconds = personalXpTime % 60;

            updateTeamText("pboost2", ChatColor.WHITE + "Personal XP: " + ChatColor.LIGHT_PURPLE + "x" + personalXpMult);
            updateTeamText("timer2", ChatColor.GRAY + "Ends in: " + ChatColor.WHITE + minutes + "m " + seconds + "s");
        } else if (hasPersonalToken) {
            long minutes = personalTokenTime / 60;
            long seconds = personalTokenTime % 60;

            updateTeamText("pboost2", ChatColor.WHITE + "Personal Token: " + ChatColor.YELLOW + "x" + personalTokenMult);
            updateTeamText("timer2", ChatColor.GRAY + "Ends in: " + ChatColor.WHITE + minutes + "m " + seconds + "s");
        } else {
            updateTeamText("pboost2", "");
            updateTeamText("timer2", "");
        }
    }

    private void updateTeamText(String teamId, String text) {
        Team team = scoreboard.getTeam(teamId);
        if (team != null) {
            team.setPrefix(text);
        }
    }

    public void destroy() {
        try {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        } catch (Exception ignored) {}
        scoreboard = null;
        objective = null;
    }
}