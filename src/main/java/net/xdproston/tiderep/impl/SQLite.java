package net.xdproston.tiderep.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.xdproston.tiderep.Files;
import net.xdproston.tiderep.Main;
import net.xdproston.tiderep.interfaces.Database;
import net.xdproston.tiderep.logger.Logger;
import net.xdproston.tiderep.logger.LoggerType;
import org.bukkit.entity.Player;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class SQLite implements Database
{
    private static final String DB_PATH_STR = "jdbc:sqlite:" + Main.getInstance().getDataFolder().getAbsolutePath() + "/users.db";

    protected HikariDataSource hds;
    protected Connection connect;
    protected Statement stmt;

    @Override
    public void init() {
        try {
            HikariConfig hc = new HikariConfig();
            hc.setPoolName("SQLite");
            hc.setDriverClassName("org.sqlite.JDBC");
            hc.setJdbcUrl(DB_PATH_STR);

            hds = new HikariDataSource(hc);
            connect = hds.getConnection();
            stmt = connect.createStatement();
        } catch (Exception e) {
            Logger.send(LoggerType.SEVERE, "An error occurred during the connection of the sqlite database:" + String.format("%s - %s", e.getClass().getName(), e.getMessage()));
        }

        execute(stmt, "CREATE TABLE IF NOT EXISTS users(\"name\" TEXT UNIQUE NOT NULL, \"reputation\" INT NOT NULL, \"sends\" TEXT DEFAULT 'NONE');");
    }

    @Override
    public boolean hasPlayerInDatabase(Player target) {
        try (ResultSet rs = executeQuery(stmt, String.format("SELECT EXISTS(SELECT name FROM users WHERE name = '%s');", target.getName()))) {
            return rs != null && rs.getString(1).equalsIgnoreCase("1");
        }
        catch (Exception e) {
            Logger.send(LoggerType.SEVERE, "An error occurred during the execution of the sql script:" + String.format("%s - %s", e.getClass().getName(), e.getMessage()));
        }
        return false;
    }

    @Override
    public void addPlayerToDatabase(Player target) {
        execute(stmt, String.format("INSERT INTO users(name, reputation) VALUES('%s', %d);", target.getName(), Files.Config.STARTUP_REPUTATION));
    }

    @Override
    public int getPlayerReputation(Player target) {
        try (ResultSet rs = executeQuery(stmt, String.format("SELECT reputation FROM users WHERE name = '%s';", target.getName()))) {
            return rs != null ? rs.getInt(1) : 0;
        }
        catch (Exception e) {
            Logger.send(LoggerType.SEVERE, "An error occurred during the execution of the sql script:" + String.format("%s - %s", e.getClass().getName(), e.getMessage()));
        }
        return -927817563;
    }

    @Override
    public void addPlayerToSends(Player target, Player player) {
        try (ResultSet rs = executeQuery(stmt, String.format("SELECT sends FROM users WHERE name = '%s';", target.getName()))) {
            String sends = rs != null ? rs.getString(1) : null;

            if (sends != null && sends.equalsIgnoreCase("NONE")) {
                execute(stmt, String.format("UPDATE users SET sends = '%s' WHERE name = '%s';", player.getName(), target.getName()));
            } else {
                execute(stmt, String.format("UPDATE users SET sends = '%s, %s' WHERE name = '%s';", sends, player.getName(), target.getName()));
            }
        }
        catch (Exception e) {
            Logger.send(LoggerType.SEVERE, "An error occurred during the execution of the sql script:" + String.format("%s - %s", e.getClass().getName(), e.getMessage()));
        }
    }

    @Override
    public boolean containsPlayerInSends(Player target, Player player) {
        try (ResultSet rs = executeQuery(stmt, String.format("SELECT sends FROM users WHERE name = '%s';", target.getName()))) {
            String sends = rs != null ? rs.getString(1) : null;

            String[] listOfSends = sends != null ? sends.split(", ") : new String[0];
            for (String name : listOfSends) {
                if (player.getName().equalsIgnoreCase(name)) return true;
            }
        }
        catch (Exception e) {
            Logger.send(LoggerType.SEVERE, "An error occurred during the execution of the sql script:" + String.format("%s - %s", e.getClass().getName(), e.getMessage()));
        }
        return false;
    }

    @Override
    public ArrayList<String> getPlayerNamesInDatabase() {
        try (ResultSet rs = executeQuery(stmt, "SELECT name FROM users;")) {
            ArrayList<String> list = new ArrayList<>();
            do {list.add(rs != null ? rs.getString(1) : null);} while (rs != null && rs.next());

            return list;
        }
        catch (Exception e) {
            Logger.send(LoggerType.SEVERE, "An error occurred during the execution of the sql script:" + String.format("%s - %s", e.getClass().getName(), e.getMessage()));
        }
        return null;
    }

    @Override
    public void setPlayerReputation(Player target, int reputation) {
        execute(stmt, String.format("UPDATE users SET reputation = %d WHERE name = '%s';", reputation, target.getName()));
    }

    @Override
    public void close() {
        try {
            if (stmt != null) stmt.close();
            if (connect != null) connect.close();
        } catch (Exception e) {
            Logger.send(LoggerType.SEVERE, "An error occurred while closing the database:" + String.format("%s - %s", e.getClass().getName(), e.getMessage()));
        }
    }
}