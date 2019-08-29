package sakura.kooi.BridgingSkin.data;

import com.google.gson.annotations.SerializedName;

public class SkinSet {
	@SerializedName("Material")
	public String material;
	@SerializedName("Data")
	public byte data;
	public SkinSet(final String material,final byte data) {
		this.material = material;
		this.data = data;
	}

	public SkinSet() {
		this("SANDSTONE",(byte) 2);
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof SkinSet)) return super.equals(obj);
		if (material.equals(((SkinSet)obj).material))
			if (data == ((SkinSet)obj).data) return true;
		return false;
	}
	@Override
	public int hashCode() {
		return (material + data).hashCode();
	}
}
