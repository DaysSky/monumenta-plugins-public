package com.playmonumenta.plugins.particle;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.jetbrains.annotations.Nullable;


public class PartialParticle extends AbstractPartialParticle<PartialParticle> {

	/*
	 * Minimal constructor, useful for builder pattern
	 */
	public PartialParticle(Particle particle, Location location) {
		super(particle, location);
		mMinimumCount = 0;
	}

	public PartialParticle(Particle particle, Location location, int count) {
		super(particle, location);
		mCount = count;
		mMinimumCount = 0;
	}

	/*
	 * Share the same delta for X, Y and Z.
	 * Use default data.
	 * Use default directional/variance settings.
	 * Use default multiplier mode.
	 */
	public PartialParticle(Particle particle, Location location, int count, double delta, double extra) {
		this(particle, location, count, delta, delta, delta, extra);
	}

	public PartialParticle(Particle particle, Location location, int count, @Nullable Object data) {
		this(particle, location, count, 0, 0, 0, 0, data);
	}

	/*
	 * Use default data.
	 * Use default directional/variance settings.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ) {
		this(particle, location, count, deltaX, deltaY, deltaZ, 0);
	}

	/*
	 * Use default data.
	 * Use default directional/variance settings.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ, double extra) {
		this(particle, location, count, deltaX, deltaY, deltaZ, extra, null);
	}

	/*
	 * Use default directional/variance settings.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ, @Nullable Object data) {
		this(particle, location, count, deltaX, deltaY, deltaZ, 0, data, false, 0);
	}

	/*
	 * Use default directional/variance settings.
	 */
	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data) {
		this(particle, location, count, deltaX, deltaY, deltaZ, extra, data, false, 0);
	}

	public PartialParticle(Particle particle, Location location, int count, double deltaX, double deltaY, double deltaZ, double extra, @Nullable Object data, boolean directionalMode) {
		this(particle, location, count, deltaX, deltaY, deltaZ, extra, data, directionalMode, 0);
	}

	public PartialParticle(
		Particle particle,
		Location location,
		int count,
		double deltaX,
		double deltaY,
		double deltaZ,
		double extra,
		@Nullable Object data,
		boolean directionalMode,
		double extraVariance
	) {
		super(particle, location);
		mCount = count;
		mDeltaX = deltaX;
		mDeltaY = deltaY;
		mDeltaZ = deltaZ;
		mExtra = extra;
		mData = data;
		mDirectionalMode = directionalMode;
		mExtraVariance = extraVariance;
		mMinimumCount = 0;
	}

	@Override
	public PartialParticle copy() {
		return copy(new PartialParticle(mParticle, mLocation, mCount, mDeltaX, mDeltaY, mDeltaZ, mExtra, mData, mDirectionalMode, mExtraVariance));
	}

}
