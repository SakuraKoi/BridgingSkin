package sakura.kooi.BridgingSkin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import sakura.kooi.BridgingSkin.data.SkinSet;

public class SkinEditListener implements Listener {
	/*
	@EventHandler
	public void onInventoryClick (final InventoryClickEvent e) {
		if (e.isCancelled()) return;
		if ((e.getInventory().getTitle()!=null) && e.getInventory().getTitle().contains("§6§l编辑皮肤库存")) {
			e.setCancelled(true);
		}
		if (e.getClickedInventory()==null) return;
		if ((e.getClickedInventory().getTitle()!=null) && e.getClickedInventory().getTitle().contains("§6§l编辑皮肤库存")) {
			e.setCancelled(true);
			final ItemStack item = e.getCurrentItem();
			if ((item==null)|| (item.getType()==Material.AIR)) return;
			//new SkinSet(item.getType().name(),item.getData().getData());
		}
	}
	 */
	@EventHandler
	public void onInventoryClose(final InventoryCloseEvent e) {
		if (e.getInventory().getHolder() instanceof SkinEditHolder) {
			final SkinEditHolder holder = (SkinEditHolder) e.getInventory().getHolder();
			holder.getSkins().allSkin.clear();
			for (final ItemStack item : e.getInventory().getContents()) {
				if (item==null) {
					continue;
				}
				holder.getSkins().allSkin.add(new SkinSet(item.getType().name(),item.getData().getData()));
			}
			holder.getSkins().currentSkin = new SkinSet();
			BridgingSkin.saveSkin(holder.getSkins());
		}
	}
}
