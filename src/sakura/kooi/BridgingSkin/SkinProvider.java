package sakura.kooi.BridgingSkin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import sakura.kooi.BridgingAnalyzer.api.BlockSkinProvider;
import sakura.kooi.BridgingSkin.data.SkinSet;

public class SkinProvider implements BlockSkinProvider {
	@SuppressWarnings("deprecation")
	@Override
	public ItemStack provide(final Player player) {
		final SkinSet skin = BridgingSkin.getSkin(player.getName(), player.getUniqueId().toString()).currentSkin;
		Material material = Material.getMaterial(skin.material);
		if ((material==null) || IllegalMaterial.isIllegal(material)) {
			material = Material.SANDSTONE;
		}
		if (material==Material.SANDSTONE) { // convert normal sandstone to smooth
			skin.data = 2;
		}
		return new ItemStack(material,64,(short)0,skin.data);
	}
}
