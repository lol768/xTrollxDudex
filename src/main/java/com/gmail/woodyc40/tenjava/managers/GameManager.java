/**
 * Name: GameManager.java
 * Created: 7 December 2013
 *
 * @version 1.0
 * @author AgentTroll
 */
package com.gmail.woodyc40.tenjava.managers;

import com.gmail.woodyc40.tenjava.Game;
import com.gmail.woodyc40.tenjava.Players;
import com.gmail.woodyc40.tenjava.TenJava;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.SpawnEgg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameManager {

    Map<String, Location> locs = new HashMap<>();
    Map<String, PlayerInventory> pi = new HashMap<>();

    private int arenas;
    private List<Players> playerData = new ArrayList<>();
    private static List<Game> games = new ArrayList<>();
    private static Map<String, Game> creators = new HashMap<>();
    private static final GameManager gm = new GameManager();


    protected GameManager() {}

    public static GameManager getInstance() {
        return gm;
    }

    public Game createArena(Player creator) {
        arenas++;

        Game g = new Game(arenas);
        g.setWorld(creator.getWorld());
        g.p = creator.getName();
        games.add(g);

        creators.put(creator.getName(), g);

        TenJava.getInstance().getConfig().set("Arenas." + arenas + ".world", creator.getWorld().getName());
        TenJava.getInstance().saveConfig();

        creator.getInventory().addItem(new ItemStack(Material.BLAZE_ROD));
        creator.sendMessage(MessageManager.getInstance().getPrefix() + "Click the spawn");

        return g;
    }

    public void loadGames() {
        arenas = 0;
        for(String key : TenJava.getInstance().getConfig().getConfigurationSection("Arenas").getKeys(false)) {
            Game g = new Game(Integer.parseInt(key));
            g.setWorld(Bukkit.getServer().getWorld(TenJava.getInstance().getConfig().getString("Arenas." + g.getId() + ".world")));
            g.setSpawn(deserializeLoc(TenJava.getInstance().getConfig().getString("Arenas." + g.getId() + ".spawn")));
            games.add(g);
        }
    }

    public Game getArena(Player p) {
        for(Game g : games) {
            if(g.getPlayer() == p) {
                return g;
            }
        }
        return null;
    }

    public Game getArena(World w) {
        for(Game g : games) {
            if(g.getWorld().getName().equals(w.getName())) {
                return g;
            }
        }
        return null;
    }

    public Game getArena(int i) {
        for(Game g : games) {
            if(g.getId() == i) {
                return g;
            }
        }
        return null;
    }

    public boolean exists(World w) {
        if(games.isEmpty()) {
            return false;
        }
        for(Game g : games) {
            if(g.getWorld().getName().equals(w.getName())) {
                return true;
            }
        }
        return false;
    }

    public void addPlayer(Player ply) {
        for(Players p : playerData) {
            if(p.name.equals(ply.getName())) {
                return;
            }
        }
        if(ply.hasPlayedBefore()) {
            return;
        }
        playerData.add(new Players(ply, 2, 0));
    }

    public void addPlayer(Player p, int id) {
        Game g = getArena(id);
        if(g == null) {
            p.sendMessage(MessageManager.getInstance().getError() + "This arena does not exist.");
            return;
        }
        if(g.isInGame()) {
            p.sendMessage(MessageManager.getInstance().getError() + "This arena is in game");
            return;
        }
        PlayerInventory pi = p.getInventory();
        this.pi.put(p.getName(), pi);
        pi.clear();

        pi.addItem(new SpawnEgg(EntityType.CREEPER).toItemStack());
        pi.addItem(new SpawnEgg(EntityType.ZOMBIE).toItemStack());
        pi.addItem(new SpawnEgg(EntityType.SKELETON).toItemStack());
        pi.addItem(new SpawnEgg(EntityType.SPIDER).toItemStack());
        pi.addItem(new ItemStack(Material.IRON_BLOCK));
        pi.addItem(new ItemStack(Material.BLAZE_POWDER));
        pi.addItem(new ItemStack(Material.GOLD_INGOT));
        pi.addItem(new ItemStack(Material.WOOD_DOOR){
            {
                ItemMeta meta = getItemMeta();
                meta.setDisplayName("Leave the arena");

                setItemMeta(meta);
            }
        });
        pi.addItem(new ItemStack(Material.STONE_BUTTON)
        {
            {
                ItemMeta meta = getItemMeta();
                meta.setDisplayName("Get Statistics");

                setItemMeta(meta);
            }
        });

        p.teleport(g.getSpawn());
        g.p = p.getName();

        locs.put(p.getName(), p.getLocation());
        p.setAllowFlight(true);
        p.setFlying(false);
        p.sendMessage(MessageManager.getInstance().getPrefix() + "Fly, hurry!");
        g.getCom().startup();
    }

    public void removePlayer(Player p) {
        for(Game g : games) {
            if(g.p == p.getName()) {
                g.p = null;
                g.kills = 0;
                for(Entity e : g.getWorld().getEntities()) {
                    e.remove();
                }
            }
        }
        p.teleport(locs.get(p.getName()));
        locs.remove(p.getName());

        p.getInventory().clear();
        for(ItemStack i : pi.get(p.getName())) {
            p.getInventory().addItem(i);
        }
        pi.remove(p.getName());

        p.setFlying(false);
        p.setAllowFlight(false);

    }

    public boolean isInGame(Player p) {
        for(Game g : games) {
            if(g.p.equals(p.getName())) {
                return true;
            }
        }
        return false;
    }

    public Players getPlayerData(Player pl) {
        for(Players p : playerData) {
            if(p.name.equals(pl.getName())) {
                return p;
            }
        }
        return null;
    }

    public boolean isCreating(Player p) {
        return creators.containsKey(p.getName());
    }

    public void notCreating(Player p) {
        creators.remove(p.getName());
        getArena(p).p = "";
    }

    public String serializeLoc(Location l){
        return l.getWorld().getName()+","+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ();
    }
    public Location deserializeLoc(String s){
        String[] st = s.split(",");
        return new Location(Bukkit.getServer().getWorld(st[0]), Integer.parseInt(st[1]), Integer.parseInt(st[2]), Integer.parseInt(st[3]));
    }

}
