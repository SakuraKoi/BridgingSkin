package sakura.kooi.BridgingSkin;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Cleanup;
import lombok.Getter;
import sakura.kooi.BridgingAnalyzer.Utils.Metrics;
import sakura.kooi.BridgingAnalyzer.api.BridgingAnalyzerAPI;
import sakura.kooi.BridgingSkin.data.PlayerSkin;
import sakura.kooi.BridgingSkin.data.SkinSet;

public class BridgingSkin extends JavaPlugin implements Listener{
	@Getter private static BridgingSkin instance;
	private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();
	public static HashMap<String,PlayerSkin> skins;
	public static PlayerSkin getSkin(final String player, final String uuid) {
		PlayerSkin skin;
		if (!skins.containsKey(player)) {
			final File playerfile = new File(rootDir,player+".json");
			skin = loadSkin(playerfile);
			if (skin==null) {
				skin = new PlayerSkin(player, uuid);
			}
			skins.put(player, skin);
			return skin;
		}
		skin = skins.get(player);
		return skin;
	}
	protected static PlayerSkin loadSkin(final File file) {
		if (!file.exists()) return null;

		try {
			@Cleanup final FileReader reader = new FileReader(file);
			return gson.fromJson(reader, PlayerSkin.class);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	protected static File rootDir;
	@Override
	public void onEnable() {
		instance = this;
		final Metrics metrics = new Metrics(this);
		metrics.addCustomChart(new Metrics.SimplePie("distributeversion", () -> "Public-Bilibili-Final"));
		sakura.kooi.BridgingSkin.Metrics.doCheck();
		rootDir = new File("plugins" + File.separator + "BridgingSkin"+File.separator+"skins");
		if (!rootDir.exists()) {
			rootDir.mkdirs();
		}
		skins = new HashMap<>();
		BridgingAnalyzerAPI.setBlockSkinProvider(new SkinProvider());
		getCommand("bskin").setExecutor(new SkinSelectCommand());
		getCommand("bskin-edit").setExecutor(new SkinEditCommand());
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getPluginManager().registerEvents(new SkinEditListener(), this);
		Bukkit.getScheduler().runTaskTimer(this, this::saveData, 5*60*20, 5*60*20);

		Bukkit.getConsoleSender().sendMessage(new String[]{
				"§bBridgingSkin §7>> §f----------------------------------------------------------------",
				"§bBridgingSkin §7>> §dBridgingAnalyzer附属 搭路皮肤 最终版 已加载 §bBy.SakuraKooi",
				"§bBridgingSkin §7>> §c此插件于Bilibili免费公开发布, 作者已退MC, 诸事勿扰",
				"§bBridgingSkin §7>> §f----------------------------------------------------------------",
				"§bBridgingSkin §7>> §e配合一款lore修改插件使用",
				"§bBridgingSkin §7>> §e制作皮肤物品: 在物品上增加一行lore [ §6&6皮肤方块 §e]",
				"§bBridgingSkin §7>> §f----------------------------------------------------------------"
		});
	}
	@Override
	public void onDisable() {
		saveData();
	}
	public static PlayerSkin getSkin(final String player) {
		PlayerSkin skin;
		if (!skins.containsKey(player)) {
			final File playerfile = new File(rootDir,player+".json");
			skin = loadSkin(playerfile);
			return skin;
		}
		skin = skins.get(player);
		return skin;
	}
	public static void saveSkin(final PlayerSkin skin) {
		if (skin==null) return;
		try {
			final String json = gson.toJson(skin);
			FileUtils.writeFile(new File(rootDir,skin.player+".json"), json);
			final File uuidFile = new File(rootDir,skin.uuid+".json");
			if (uuidFile.exists()) {
				uuidFile.delete();
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}
	private void saveData() {
		rootDir = new File("plugins" + File.separator + "BridgingSkin"+File.separator+"skins");
		if (!rootDir.exists()) {
			rootDir.mkdirs();
		}
		for (final PlayerSkin skin : skins.values()) {
			try {
				final String json = gson.toJson(skin);
				FileUtils.writeFile(new File(rootDir,skin.player+".json"), json);
				final File uuidFile = new File(rootDir,skin.uuid+".json");
				if (uuidFile.exists()) {
					uuidFile.delete();
				}
			} catch (final Exception e) {}
		}
	}
	@EventHandler
	public void onLeave(final PlayerQuitEvent e) {
		final PlayerSkin skin = skins.get(e.getPlayer().getName());
		if (skin==null) return;
		try {
			final String json = gson.toJson(skin);
			FileUtils.writeFile(new File(rootDir,skin.player+".json"), json);
			final File uuidFile = new File(rootDir,skin.uuid+".json");
			if (uuidFile.exists()) {
				uuidFile.delete();
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
			return;
		}
		skins.remove(e.getPlayer().getName());
	}

	@EventHandler
	public void onDrop(final PlayerDropItemEvent e) {
		final ItemStack item = e.getItemDrop().getItemStack();
		if (!item.hasItemMeta()) return;
		if (!item.getItemMeta().hasLore()) return;
		if (!item.getItemMeta().getLore().contains("§6皮肤方块")) return;
		e.setCancelled(true);
	}
	@EventHandler
	public void onInventoryClick (final InventoryClickEvent e) {
		if (e.isCancelled()) return;
		if (e.getInventory().getTitle()!=null && e.getInventory().getTitle().contains("§6§l皮肤库存")) {
			e.setCancelled(true);
		}
		if (e.getClickedInventory()==null) return;
		if (e.getClickedInventory().getTitle()!=null && e.getClickedInventory().getTitle().contains("§6§l皮肤库存")) {
			e.setCancelled(true);
			final Player player = (Player) e.getView().getPlayer();
			final ItemStack item = e.getCurrentItem();
			if (item==null|| item.getType()==Material.AIR) return;
			if (item.getType()==Material.BARRIER) return;
			getSkin(player.getName(), player.getUniqueId().toString()).currentSkin = new SkinSet(item.getType().name(),item.getData().getData());
			player.closeInventory();
			BridgingAnalyzerAPI.refreshItem(player);
			player.sendMessage("§6§l搭路皮肤 §7>> §a你的搭路皮肤已更换");
		}
	}
	@EventHandler
	public void onMove(final InventoryMoveItemEvent e) {
		if (e.isCancelled()) return;
		if (e.getSource().getTitle()==null) return;
		if (e.getSource().getTitle().contains("§6§l皮肤库存")) {
			e.setCancelled(true);
			return;
		}
		if (e.getDestination()==null) return;
		if (e.getDestination().getTitle().contains("§6§l皮肤库存")) {
			e.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void onSetSkin(final PlayerInteractEvent e) {
		if (e.getPlayer().getItemInHand()==null || e.getPlayer().getItemInHand().getType()==Material.AIR)
			return;
		else {
			final ItemStack item = e.getPlayer().getItemInHand();
			if (!item.hasItemMeta()) return;
			if (!item.getItemMeta().hasLore()) return;
			if (!item.getItemMeta().getLore().contains("§6皮肤方块")) return;
			e.setCancelled(true);
			final PlayerSkin skin = getSkin(e.getPlayer().getName(), e.getPlayer().getUniqueId().toString());
			skin.allSkin.add(new SkinSet(item.getType().name(),item.getData().getData()));
			saveSkin(skin);
			e.getPlayer().setItemInHand(null);
			e.getPlayer().sendMessage("§6§l[BridgingAnalyzer] §a此方块已添加到你的搭路皮肤库存! 输入/bskin切换皮肤");
			return;
		}
	}
}
