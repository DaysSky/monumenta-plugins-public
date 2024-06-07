package com.playmonumenta.plugins.seasonalevents;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public enum MonumentaContent {
	LABS("labs", ContentType.DUNGEON, 1),
	WHITE("white", ContentType.DUNGEON, 1),
	ORANGE("orange", ContentType.DUNGEON, 1),
	MAGENTA("magenta", ContentType.DUNGEON, 1),
	LIGHTBLUE("lightblue", ContentType.DUNGEON, 1),
	YELLOW("yellow", ContentType.DUNGEON, 1),
	WILLOWS("willows", ContentType.DUNGEON, 1),
	SANCTUM("sanctum", ContentType.STRIKE, 1),
	REVERIE("reverie", ContentType.DUNGEON, 1),
	CORRIDORS("corridors", ContentType.DUNGEON, 1),
	CORRIDORS_ROOM("corridorsroom", ContentType.OTHER, 0),
	VERDANT("verdant", ContentType.STRIKE, 1),
	KAUL("kaul", ContentType.BOSS, 1),
	AZACOR("azacor", ContentType.BOSS, 1),
	SNOW_SPIRIT("snowspirit", ContentType.BOSS, 1),
	ARENA("arena", ContentType.OTHER, 1),
	LIME("lime", ContentType.DUNGEON, 2),
	PINK("pink", ContentType.DUNGEON, 2),
	GRAY("gray", ContentType.DUNGEON, 2),
	LIGHT_GRAY("lightgray", ContentType.DUNGEON, 2),
	CYAN("cyan", ContentType.DUNGEON, 2),
	PURPLE("purple", ContentType.DUNGEON, 2),
	TEAL("teal", ContentType.DUNGEON, 2),
	FORUM("forum", ContentType.DUNGEON, 2),
	SHIFTING("shiftingcity", ContentType.DUNGEON, 2),
	DEPTHS("depths", ContentType.DUNGEON, 2), // Called from depths party in plugin
	MIST("mist", ContentType.STRIKE, 2),
	REMORSE("remorse", ContentType.STRIKE, 2),
	HORSEMAN("horseman", ContentType.BOSS, 2),
	ELDRASK("eldrask", ContentType.BOSS, 2),
	HEKAWT("hekawt", ContentType.BOSS, 2),
	RUSH("rush", ContentType.OTHER, 2),
	RUSH_WAVE("rushwave", ContentType.OTHER, 0),
	BLUE("blue", ContentType.DUNGEON, 3),
	BROWN("brown", ContentType.DUNGEON, 3),
	GREEN("green", ContentType.DUNGEON, 3),
	RED("red", ContentType.DUNGEON, 3),
	BLACK("black", ContentType.DUNGEON, 3),
	SKT("skt", ContentType.DUNGEON, 3),
	RUIN("ruin", ContentType.STRIKE, 3),
	PORTAL("portal", ContentType.STRIKE, 3),
	GALLERY("gallery", ContentType.OTHER, 3),
	ZENITH("zenith", ContentType.DUNGEON, 3), // Called from depths party in plugin
	SIRIUS("sirius", ContentType.BOSS, 3),
	GALLERY_ROUND("galleryround", ContentType.OTHER, 0),
	GODSPORE("godspore", ContentType.BOSS, 3),
	KINGS_BOUNTY("r1daily", ContentType.OTHER, 0),
	CELSIAN_BOUNTY("r2daily", ContentType.OTHER, 0),
	RING_BOUNTY("r3daily", ContentType.OTHER, 0),
	DELVE_BOUNTY("delvebounty", ContentType.OTHER, 0),
	STARPOINT_POI("starpoint", ContentType.POI, 0),
	KEEP_POI("keep", ContentType.POI, 0),
	WOLFSWOOD_POI("wolfswood", ContentType.POI, 0),
	FISHING_COMBAT("fishingcombat", ContentType.OTHER, 0);

	public static final Set<Integer> ALL_CONTENT_REGION_INDEXES
		= Arrays.stream(MonumentaContent.values())
		.map(content -> content.mRegion)
		.collect(Collectors.toSet());

	private final int mRegion;
	private final String mLabel;
	private final ContentType mContentType;

	MonumentaContent(String label, ContentType contentType, int region) {
		mRegion = region;
		mLabel = label;
		mContentType = contentType;
	}

	public int getRegion() {
		return mRegion;
	}

	public String getLabel() {
		return mLabel;
	}

	public ContentType getContentType() {
		return mContentType;
	}

	public static @Nullable MonumentaContent getContentSelection(String label) {
		if (label == null) {
			return null;
		}
		for (MonumentaContent selection : MonumentaContent.values()) {
			if (selection.getLabel().equals(label)) {
				return selection;
			}
		}
		return null;
	}
}

