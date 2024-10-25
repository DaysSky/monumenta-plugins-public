package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class PPLightning extends AbstractPartialParticle<PPLightning> {
	/*
	 * Height above mLocation to start the lightning bolt.
	 */
	protected double mHeight;

	/*
	 * Width within which all particles must start in spite of hop variance,
	 * prior to applying deltas (which may then put them outside).
	 */
	protected double mMaxWidth;

	/*
	 * Random variance applied after each downward hop of the lightning's points
	 * (initially evenly spread along the Y axis), drawing jagged lines.
	 */
	protected double mHopXZ;
	protected double mHopY;
	protected int mDuration = Constants.TICKS_PER_SECOND;

	protected @Nullable BukkitRunnable mRunnable;

	protected double mHopsPerBlock = 2;

	// To prevent each line from looking too sparse (ugly),
	// if player's particle multiplier setting is not completely off,
	// use at least this many particles.
	private static final int HOP_MINIMUM_PARTICLES = 4;

	private final ArrayList<Location> mGeneratedHops = new ArrayList<>();
	private final HashMap<Integer, List<Location>> mParticleLocations = new HashMap<>();

	/*-------------------------------------------------------------------------------
	 * Constructors
	 */

	public PPLightning(Particle particle, Location strikeLocation) {
		super(particle, strikeLocation);
	}

	@Override
	public PPLightning copy() {
		return copy(new PPLightning(mParticle, mLocation.clone()));
	}

	@Override
	public PPLightning copy(PPLightning copy) {
		super.copy(copy);
		copy.mHeight = mHeight;
		copy.mMaxWidth = mMaxWidth;
		copy.mHopXZ = mHopXZ;
		copy.mHopY = mHopY;
		copy.mDuration = mDuration;
		copy.mHopsPerBlock = mHopsPerBlock;
		copy.mRunnable = mRunnable;
		copy.mGeneratedHops.addAll(mGeneratedHops);
		copy.mParticleLocations.putAll(mParticleLocations);
		return copy;
	}

	/*-------------------------------------------------------------------------------
	 * Required init methods
	 * One of these must be called prior to spawning this particle
	 */

	/*
	 * Share the same hop length for XZ and Y.
	 */
	public PPLightning init(double height, double maxWidth, double hopLength) {
		return init(height, maxWidth, hopLength, hopLength);
	}

	/*
	 * Define attributes specific to this subclass of PartialParticle.
	 */
	public PPLightning init(double height, double maxWidth, double hopXZ, double hopY) {
		mHeight = height;
		mMaxWidth = maxWidth;
		mHopXZ = hopXZ;
		mHopY = hopY;

		return this;
	}

	/*-------------------------------------------------------------------------------
	 * Parameter getters and setters
	 */

	public PPLightning height(double height) {
		mHeight = height;
		return this;
	}

	public double height() {
		return mHeight;
	}

	public PPLightning maxWidth(double maxWidth) {
		mMaxWidth = maxWidth;
		return this;
	}

	public double maxWidth() {
		return mMaxWidth;
	}

	public PPLightning hopXZ(double hopXZ) {
		mHopXZ = hopXZ;
		return this;
	}

	public double hopXZ() {
		return mHopXZ;
	}

	public PPLightning hopY(double hopY) {
		mHopY = hopY;
		return this;
	}

	public double hopY() {
		return mHopY;
	}

	public PPLightning duration(int duration) {
		mDuration = duration;
		return this;
	}

	public PPLightning hopsPerBlock(double hops) {
		mHopsPerBlock = hops;
		return this;
	}

	public double hopsPerBlock() {
		return mHopsPerBlock;
	}

	public @Nullable BukkitRunnable runnable() {
		return mRunnable;
	}

	/*-------------------------------------------------------------------------------
	 * Methods
	 */

	@Override
	protected void doSpawn(PartialParticleBuilder packagedValues) {
		generateHopsOnce(packagedValues.location());

		int hopParticleCount = Math.max(HOP_MINIMUM_PARTICLES, packagedValues.count());
		List<Location> hopParticleLocations = generateParticleLocationsOnce(hopParticleCount);
		double particlesPerTick = hopParticleLocations.size() / (double) mDuration;

		packagedValues.count(1);

		mRunnable = new BukkitRunnable() {
			int mAnimationProgress = 0;
			int mIndexPointer = 0;

			@Override
			public void run() {
				// Frame of animation this run, starting at 1
				mAnimationProgress++;
				int targetIndex = (int) Math.round(mAnimationProgress * particlesPerTick) - 1;

				for (int index = mIndexPointer; index <= targetIndex; index++) {
					packagedValues.location(
						hopParticleLocations.get(index)
					);
					spawnUsingSettings(packagedValues);
				}

				// If this was the last frame of animation
				if (mAnimationProgress >= mDuration) {
					cancel();
					mRunnable = null;
				} else {
					mIndexPointer = targetIndex;
				}
			}
		};
		mRunnable.runTaskTimerAsynchronously(Plugin.getInstance(), 0, 1);
	}

	// Each lightning bolt will have the same hops for all players,
	// and so look "the same" in the way the same bolt jags from any screen,
	// just drawing differently based on their particle multiplier settings
	private void generateHopsOnce(Location strikeLocation) {
		if (!mGeneratedHops.isEmpty()) {
			return;
		}

		int hopCount = (int) Math.ceil(mHeight * mHopsPerBlock);
		double endHeight = strikeLocation.getY();
		double startHeight = endHeight + mHeight;

		double strikeX = strikeLocation.getX();
		double strikeZ = strikeLocation.getZ();
		double halfMaxWidth = mMaxWidth / 2;
		double capMinX = strikeX - halfMaxWidth;
		double capMaxX = strikeX + halfMaxWidth;
		double capMinZ = strikeZ - halfMaxWidth;
		double capMaxZ = strikeZ + halfMaxWidth;
		// endHeight is always capMinY, startHeight is always capMaxY

		for (int hopIndex = 0; hopIndex < hopCount; hopIndex++) {
			double currentHeight = startHeight - (mHeight / hopCount * hopIndex);
			Location currentHopLocation = strikeLocation.clone();
			currentHopLocation.setY(currentHeight);

			// First and last points don't get varied
			if (
				hopIndex != 0
					&& hopIndex != hopCount - 1
			) {
				double currentX = currentHopLocation.getX();
				double currentZ = currentHopLocation.getZ();
				// Hops should randomise between the closest they can get to the
				// nearest edge, and the furthest they can get away from current XZ,
				// without exceeding the mMaxWidth about XZ center.
				// Eg if we're closer to capMinX, we randomise between capMinX,
				// and currentX + mHopXZ,
				// rather than currentX - mHopXZ for the former,
				// which may randomise a value outside of mMaxWidth
				double hopMinX = Math.max(capMinX, currentX - mHopXZ);
				double hopMaxX = Math.min(capMaxX, currentX + mHopXZ);
				double hopMinZ = Math.max(capMinZ, currentZ - mHopXZ);
				double hopMaxZ = Math.min(capMaxZ, currentZ + mHopXZ);

				double hopMinY = Math.max(endHeight, currentHeight - mHopY);
				double hopMaxY = Math.min(startHeight, currentHeight + mHopY);

				currentHopLocation.set(
					FastUtils.randomDoubleInRange(hopMinX, hopMaxX),
					FastUtils.randomDoubleInRange(hopMinY, hopMaxY),
					FastUtils.randomDoubleInRange(hopMinZ, hopMaxZ)
				);
			}
			mGeneratedHops.add(currentHopLocation);
		}
	}

	// Store locations for the same hopParticleCount for performance,
	// generating the same results only once,
	// then referring to them again as the timer loops
	private List<Location> generateParticleLocationsOnce(int hopParticleCount) {
		@Nullable List<Location> hopParticleLocations = mParticleLocations.get(hopParticleCount);
		if (hopParticleLocations != null) {
			return hopParticleLocations;
		} else {
			hopParticleLocations = new ArrayList<>();

			for (int hopIndex = 0; hopIndex < mGeneratedHops.size(); hopIndex++) {
				Location currentHop = mGeneratedHops.get(hopIndex);
				if (hopIndex == 0) {
					hopParticleLocations.add(currentHop);

					continue;
				}

				Location previousHop = mGeneratedHops.get(hopIndex - 1);

				// Move from previous to current hop
				Location movingParticleLocation = previousHop.clone();
				Location hopInterval = currentHop.clone().subtract(previousHop).multiply(1d / hopParticleCount);
				for (int i = 0; i < hopParticleCount; i++) {
					movingParticleLocation.add(hopInterval);
					hopParticleLocations.add(movingParticleLocation.clone());
				}
			}
			mParticleLocations.put(hopParticleCount, hopParticleLocations);

			return hopParticleLocations;
		}
	}
}
