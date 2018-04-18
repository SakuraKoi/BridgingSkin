package ldcr.BridgingSkin;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import ldcr.BridgingSkin.data.SkinSet;

public class SkinSelectCommand implements CommandExecutor {
    private static final ArrayList<Material> illegalMaterial = new ArrayList<Material>();
    static {
	illegalMaterial.add(Material.REDSTONE_BLOCK);
	illegalMaterial.add(Material.PISTON_BASE);
	illegalMaterial.add(Material.PISTON_STICKY_BASE);
	illegalMaterial.add(Material.LEVER);
	illegalMaterial.add(Material.DISPENSER);
	illegalMaterial.add(Material.LAPIS_BLOCK);
	illegalMaterial.add(Material.EMERALD_BLOCK);
	illegalMaterial.add(Material.BEACON);
	illegalMaterial.add(Material.REDSTONE_COMPARATOR);
	illegalMaterial.add(Material.DIODE);
	illegalMaterial.add(Material.REDSTONE);
	illegalMaterial.add(Material.REDSTONE_TORCH_ON);
	illegalMaterial.add(Material.STONE_BUTTON);
	illegalMaterial.add(Material.WOOD_BUTTON);
	illegalMaterial.add(Material.HOPPER);
	illegalMaterial.add(Material.GOLD_PLATE);
	illegalMaterial.add(Material.IRON_PLATE);
	illegalMaterial.add(Material.STONE_PLATE);
	illegalMaterial.add(Material.WOOD_PLATE);
	illegalMaterial.add(Material.DAYLIGHT_DETECTOR);
	illegalMaterial.add(Material.DROPPER);
	illegalMaterial.add(Material.SLIME_BLOCK);
    }
    public static boolean isIllegal(final Material skin) {
	return illegalMaterial.contains(skin);
    }
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
	if (!(sender instanceof Player)) {
	    sender.sendMessage("§6§l[BridgingAnalyzer] §c仅玩家可以执行.");
	    return true;
	}
	final Player p =(Player) sender;
	final Inventory inv = Bukkit.createInventory(null, 54,"§6§l皮肤库存");
	final ArrayList<SkinSet> illegalSkins = new ArrayList<SkinSet>();
	for (final SkinSet skin : BridgingSkin.getSkin(p.getUniqueId().toString()).allSkin) {
	    final Material material = Material.getMaterial(skin.material);
	    if (material==null) {
		illegalSkins.add(skin);
		continue;
	    }
	    if (illegalMaterial.contains(material)) {
		illegalSkins.add(skin);
		continue;
	    }
	}
	if (!illegalSkins.isEmpty()) {
	    BridgingSkin.getSkin(p.getUniqueId().toString()).allSkin.removeAll(illegalSkins);
	    sender.sendMessage("§6§l[BridgingAnalyzer] §c在你的皮肤库存发现了一些无效物品, 已自动删除.");
	}
	for (final SkinSet skin : BridgingSkin.getSkin(p.getUniqueId().toString()).allSkin) {
	    Material material = Material.getMaterial(skin.material);
	    ItemStack stack;
	    if (material==null) {
		material = Material.BARRIER;
		stack = new ItemStack(material,64,(short)0,skin.data);
		final ItemMeta meta = stack.getItemMeta();
		meta.setDisplayName("§c无效皮肤 数据错误");
		stack.setItemMeta(meta);
	    } else {
		stack = new ItemStack(material,64,(short)0,skin.data);
	    }
	    inv.addItem(stack);
	}
	p.openInventory(inv);
	return true;
    }
}


