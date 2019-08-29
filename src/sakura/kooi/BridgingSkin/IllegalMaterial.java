package sakura.kooi.BridgingSkin;

import java.util.ArrayList;

import org.bukkit.Material;

public class IllegalMaterial {
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
		illegalMaterial.add(Material.ANVIL);
		illegalMaterial.add(Material.GRAVEL);
	}
	public static boolean isIllegal(final Material skin) {
		return illegalMaterial.contains(skin);
	}
}
