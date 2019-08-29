package sakura.kooi.BridgingSkin;

import lombok.AllArgsConstructor;
import sakura.kooi.BridgingSkin.data.PlayerSkin;
import sakura.kooi.BridgingSkin.data.SkinSet;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;

@AllArgsConstructor
public class ClearThread implements Runnable {
	private final CommandSender sender;
	private final Material material;
	private final byte data;

	@Override
	public void run() {
		int total = 0;
		int cleared = 0;
		for (final File file : BridgingSkin.rootDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(final File file) {
				if (file.getName().contains("-")) return false;
				return file.getName().endsWith(".json");
			}
		})) {
			boolean edited = false;
			try {
				final PlayerSkin skin = BridgingSkin.loadSkin(file);
				if (skin.currentSkin.material.equals(material.name())) {
					if (data == -1) {
						skin.currentSkin = new SkinSet();
						edited = true;
					} else if (skin.currentSkin.data == data) {
						skin.currentSkin = new SkinSet();
						edited = true;
					}
				}
				final ArrayList<SkinSet> remove = new ArrayList<>();
				for (final SkinSet entry : skin.allSkin){
					if (entry.material.equals(material.name())) {
						if (data == -1) {
							remove.add(entry);
							edited = true;
						} else if (entry.data == data) {
							remove.add(entry);
							edited = true;
						}
					}
				}
				skin.allSkin.removeAll(remove);
				total++;
				if (edited) {
					BridgingSkin.saveSkin(skin);
					BridgingSkin.skins.remove(skin.player);
					cleared++;
					sender.sendMessage("§6§lBridgingSkin §7>> §e成功清理玩家 "+skin.player+" 的皮肤库存");
				}/* else {
					sender.sendMessage("§6§lBridgingSkin §7>> §7扫描玩家 "+skin.player+" 的皮肤库存完毕");
				}*/
			} catch (final Exception e) {
				e.printStackTrace();
				sender.sendMessage("§6§lBridgingSkin §7>> §c处理文件 "+file.getName()+" 时出错");
			}
		}
		sender.sendMessage("§6§lBridgingSkin §7>> §b皮肤清理完毕 共扫描 "+total+" 玩家 / 清理 "+cleared+" 玩家");
	}

}
