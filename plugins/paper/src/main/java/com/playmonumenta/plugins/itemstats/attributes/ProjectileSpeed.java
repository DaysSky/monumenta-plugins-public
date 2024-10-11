package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class ProjectileSpeed implements Attribute {

	private static final String SPEED_METAKEY = "AttributeArrowSpeedMetakey";

	@Override
	public String getName() {
		return "Projectile Speed";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.PROJECTILE_SPEED;
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile proj) {
		if (value != 1) {
			proj.setMetadata(SPEED_METAKEY, new FixedMetadataValue(plugin, value));
			proj.setVelocity(proj.getVelocity().multiply(value));
		}
	}

	public static double getProjectileSpeedModifier(Projectile proj) {
		if (proj.hasMetadata(SPEED_METAKEY)) {
			return proj.getMetadata(SPEED_METAKEY).get(0).asDouble();
		}
		return 1;
	}
}
