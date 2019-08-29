package sakura.kooi.BridgingSkin;

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

import sakura.kooi.BridgingSkin.data.SkinSet;

public class SkinSelectCommand implements CommandExecutor {
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("§6§l搭路皮肤 §7>> §c仅玩家可以执行.");
			return true;
		}
		final Player p =(Player) sender;
		final Inventory inv = Bukkit.createInventory(null, 54,"§6§l皮肤库存");
		final ArrayList<SkinSet> illegalSkins = new ArrayList<>();
		for (final SkinSet skin : BridgingSkin.getSkin(p.getName(), p.getUniqueId().toString()).allSkin) {
			final Material material = Material.getMaterial(skin.material);
			if (material==null) {
				illegalSkins.add(skin);
				continue;
			}
			if (IllegalMaterial.isIllegal(material)) {
				illegalSkins.add(skin);
				continue;
			}
		}
		if (!illegalSkins.isEmpty()) {
			BridgingSkin.getSkin(p.getName(), p.getUniqueId().toString()).allSkin.removeAll(illegalSkins);
			sender.sendMessage("§6§l搭路皮肤 §7>> §c在你的皮肤库存发现了一些无效物品, 已自动删除.");
		}
		for (final SkinSet skin : BridgingSkin.getSkin(p.getName(), p.getUniqueId().toString()).allSkin) {
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


