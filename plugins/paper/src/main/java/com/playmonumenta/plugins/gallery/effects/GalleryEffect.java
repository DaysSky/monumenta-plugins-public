package com.playmonumenta.plugins.gallery.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.effects.DisplayableEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryGame;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class GalleryEffect implements DisplayableEffect {

	protected final @NotNull GalleryEffectType mType;

	public GalleryEffect(@NotNull GalleryEffectType type) {
		mType = type;
	}

	//event called after the player obtain this effect but before this object is insert inside the list of player effects
	//can be used to store info on the player (Scoreboard Tags etc..) or clean up others old effects
	public void playerGainEffect(GalleryPlayer player) {
		player.sendMessage(Component.text("You have obtained ").append(Component.text(mType.getRealName(), NamedTextColor.GOLD)));
		GalleryEffect effect = player.getEffectOfType(mType);
		if (effect != null) {
			player.removeEffect(effect);
		}

	}

	//event called after the player lose this effect but before this object is removed from the list of player effects
	public void playerLoseEffect(GalleryPlayer player) {

	}

	public void onPlayerDamage(GalleryPlayer player, DamageEvent event, LivingEntity entity) {

	}

	public void onPlayerHurt(GalleryPlayer player, DamageEvent event, @Nullable LivingEntity enemy) {

	}

	public void onPlayerFatalHurt(GalleryPlayer player, DamageEvent event, @Nullable LivingEntity enemy) {

	}

	public void onPlayerDeathEvent(GalleryPlayer player, EntityDeathEvent event, int ticks) {

	}

	public void onOtherPlayerDeathEvent(GalleryPlayer player, EntityDeathEvent event, LivingEntity otherPlayer, int ticks) {

	}

	public void onRoundStart(GalleryPlayer player, GalleryGame game) {

	}

	public void tick(GalleryPlayer player, boolean oneSecond, boolean twoHertz, int ticks) {

	}

	public void refresh(GalleryPlayer player) {

	}

	public void clear(GalleryPlayer player) {
		player.removeEffect(this);
	}

	public GalleryEffectType getType() {
		return mType;
	}

	public JsonObject toJson() {
		JsonObject object = new JsonObject();
		object.addProperty("EffectTypeName", mType.name());
		return object;
	}

	public <A extends GalleryEffect> GalleryEffect fromJson(JsonObject object) {
		GalleryEffectType type = GalleryEffectType.valueOf(object.get("EffectTypeName").getAsString());
		return type.newEffect();
	}

	// These effects should always be first, and be in a consistent order based on the order in GalleryEffectType
	@Override
	@SuppressWarnings("EnumOrdinal")
	public int getDisplayPriority() {
		return 1000000000 + mType.ordinal();
	}

	public abstract boolean canBuy(GalleryPlayer player);

	public static @Nullable GalleryEffect fromJsonObject(JsonObject object) {
		GalleryEffectType type = GalleryEffectType.fromName(object.get("EffectTypeName").getAsString());
		if (type != null) {
			GalleryEffect effect = type.newEffect();
			if (effect != null) {
				return effect.fromJson(object);
			}
		}
		return null;
	}

}
