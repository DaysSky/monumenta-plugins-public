package com.playmonumenta.plugins.depths.abilities.aspects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class ScytheAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Scythe";
	public static final double DAMAGE_MODIFIER = 0.95;

	public static final DepthsAbilityInfo<ScytheAspect> INFO =
		new DepthsAbilityInfo<>(ScytheAspect.class, ABILITY_NAME, ScytheAspect::new, null, DepthsTrigger.WEAPON_ASPECT)
			.displayItem(Material.IRON_HOE)
			.description("While holding a scythe, you gain an independent level of life drain and 5% damage reduction.");

	public ScytheAspect(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!event.getType().equals(DamageEvent.DamageType.MELEE)) {
			return false;
		}
		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		if (ItemUtils.isHoe(mainhand)) {
			if (PlayerUtils.isFallingAttack(mPlayer)) {
				PlayerUtils.healPlayer(mPlugin, mPlayer, 1.0);
			} else {
				double attackSpeed = 4;
				double multiplier = 1;
				if (mainhand.hasItemMeta()) {
					ItemMeta meta = mainhand.getItemMeta();
					if (meta.hasAttributeModifiers()) {
						Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.GENERIC_ATTACK_SPEED);
						if (modifiers != null) {
							for (AttributeModifier modifier : modifiers) {
								if (modifier.getSlot() == EquipmentSlot.HAND) {
									if (modifier.getOperation() == Operation.ADD_NUMBER) {
										attackSpeed += modifier.getAmount();
									} else if (modifier.getOperation() == Operation.ADD_SCALAR) {
										multiplier += modifier.getAmount();
									}
								}
							}
						}
					}
				}
				PlayerUtils.healPlayer(mPlugin, mPlayer, 1 / Math.sqrt(attackSpeed * multiplier) * mPlayer.getCooledAttackStrength(0));
			}
		}
		return false;
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			event.setFlatDamage(event.getDamage() * DAMAGE_MODIFIER);
		}
	}

}

