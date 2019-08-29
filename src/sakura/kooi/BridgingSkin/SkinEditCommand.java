package sakura.kooi.BridgingSkin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import sakura.kooi.BridgingSkin.data.PlayerSkin;
import sakura.kooi.BridgingSkin.data.SkinSet;

public class SkinEditCommand implements CommandExecutor {

	@Override
	public boolean onCommand(final CommandSender sender, final Command arg, final String arg2, final String[] args) {
		if (sender instanceof Player) {
			if (sender.hasPermission("bridgingSkin.admin")) {
				if (args.length<2) {
					sender.sendMessage(new String[] {
							"§6§lBridgingSkin §7>> §e/bskin-edit edit <player>",
							"§6§lBridgingSkin §7>> §e/bskin-edit clear <material> [data]"
					});
					return true;
				}
				switch (args[0]) {
				case "edit": {
					final String player = args[1];
					final OfflinePlayer offp = Bukkit.getOfflinePlayer(player);
					if (offp==null) {
						sender.sendMessage("§6§lBridgingSkin §7>> §c玩家 "+player+" 不存在");
						return true;
					}
					final PlayerSkin skin = BridgingSkin.getSkin(offp.getName());
					if (skin==null) {
						sender.sendMessage("§6§lBridgingSkin §7>> §c玩家 "+offp.getName()+" 没有皮肤");
						return true;
					}
					openSkinInventory((Player) sender, skin);
					return true;
				}
				case "clear": {
					final Material material = Material.getMaterial(args[1].toUpperCase());
					if (material == null) {
						sender.sendMessage("§6§lBridgingSkin §7>> §cMaterial "+args[1].toUpperCase()+" 不存在");
						return true;
					}
					byte data = -1;
					if (args.length==3) {
						try {
							data = Byte.parseByte(args[2]);
						} catch (final Exception e) {
							sender.sendMessage("§6§lBridgingSkin §7>> §c数据值错误");
							return true;
						}
					}
					Bukkit.getScheduler().runTaskAsynchronously(BridgingSkin.getInstance(), new ClearThread(sender, material, data));
					return true;
				}
				}
			}
		}
		return true;
	}
	private void openSkinInventory(final Player player, final PlayerSkin skins) {
		final SkinEditHolder holder = new SkinEditHolder(skins);
		final Inventory inv = Bukkit.createInventory(holder, 54,"§6§l编辑皮肤库存: 玩家 "+skins.player);
		holder.setInv(inv);
		for (final SkinSet skin : skins.allSkin) {
			final Material material = Material.getMaterial(skin.material);
			ItemStack stack;
			if (material==null) {
				continue;
			}
			if (IllegalMaterial.isIllegal(material)) {
				continue;
			}
			stack = new ItemStack(material,1,(short)0,skin.data);
			inv.addItem(stack);
		}
		player.openInventory(inv);
	}
}
