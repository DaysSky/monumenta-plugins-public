package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.abilities.scout.Agility;
import com.playmonumenta.plugins.abilities.scout.EagleEye;
import com.playmonumenta.plugins.abilities.scout.HuntingCompanion;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.SwiftCuts;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.abilities.scout.Versatile;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.WindBomb;
import com.playmonumenta.plugins.abilities.scout.hunter.PinningShot;
import com.playmonumenta.plugins.abilities.scout.hunter.PredatorStrike;
import com.playmonumenta.plugins.abilities.scout.hunter.SplitArrow;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.scout.ranger.WhirlingBlade;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;


public class Scout extends PlayerClass {

	public static final int CLASS_ID = 6;
	public static final int RANGER_SPEC_ID = 11;
	public static final int HUNTER_SPEC_ID = 12;

	public Scout() {
		mAbilities.add(Agility.INFO);
		mAbilities.add(HuntingCompanion.INFO);
		mAbilities.add(EagleEye.INFO);
		mAbilities.add(WindBomb.INFO);
		mAbilities.add(Sharpshooter.INFO);
		mAbilities.add(SwiftCuts.INFO);
		mAbilities.add(Swiftness.INFO);
		mAbilities.add(Volley.INFO);
		mClass = CLASS_ID;
		mClassName = "Scout";
		mClassColor = TextColor.fromHexString("#59B4EB");
		mClassGlassFiller = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
		mDisplayItem = Material.BOW;
		mClassDescription = "Scouts are agile masters of archery and exploration.";
		mClassPassiveDescription = String.format("You gain %d%% of your Projectile Damage %% as Attack Damage and you gain %d%% of your Attack Damage %% as Projectile Damage.",
			(int) (Versatile.DAMAGE_MULTIPLY_MELEE * 100), (int) (Versatile.DAMAGE_MULTIPLY_PROJ * 100));
		mClassPassiveName = "Versatile";

		mSpecOne.mAbilities.add(Quickdraw.INFO);
		mSpecOne.mAbilities.add(WhirlingBlade.INFO);
		mSpecOne.mAbilities.add(TacticalManeuver.INFO);
		mSpecOne.mSpecQuestScoreboard = "Quest103e";
		mSpecOne.mSpecialization = RANGER_SPEC_ID;
		mSpecOne.mSpecName = "Ranger";
		mSpecOne.mDisplayItem = Material.WHEAT;
		mSpecOne.mDescription = "Rangers are agile experts of exploration that have unparalleled mastery of movement.";

		mSpecTwo.mAbilities.add(PinningShot.INFO);
		mSpecTwo.mAbilities.add(SplitArrow.INFO);
		mSpecTwo.mAbilities.add(PredatorStrike.INFO);
		mSpecTwo.mSpecQuestScoreboard = "Quest103l";
		mSpecTwo.mSpecialization = HUNTER_SPEC_ID;
		mSpecTwo.mSpecName = "Hunter";
		mSpecTwo.mDisplayItem = Material.LEATHER;
		mSpecTwo.mDescription = "Hunters are precise masters of archery that have dedicated themselves to projectile weaponry.";

		mTriggerOrder = ImmutableList.of(
			EagleEye.INFO,
			Swiftness.INFO,
			WindBomb.INFO,
			HuntingCompanion.INFO, // after wind bomb

			PredatorStrike.INFO,

			Quickdraw.INFO, // after eagle eye
			TacticalManeuver.INFO,
			WhirlingBlade.INFO // after wind bomb
		);
	}
}
