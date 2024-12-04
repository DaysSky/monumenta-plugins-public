package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.particle.PartialParticle;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ParticlesList {
	private static final EnumSet<Particle> PARTICLES_WITH_PARAMETERS = EnumSet.of(Particle.REDSTONE, Particle.ITEM_CRACK, Particle.BLOCK_CRACK, Particle.BLOCK_CRACK, Particle.FALLING_DUST, Particle.DUST_COLOR_TRANSITION);

	public static class CParticle {
		public Particle mParticle;
		public int mCount;
		public double mDx;
		public double mDy;
		public double mDz;
		public double mVelocity;
		public @Nullable Object mExtra2; //used when we have a particle that is inside PARTICLE_MATERIALS or Particle.REDSTONE

		public CParticle(Particle particle) {
			this(particle, 1, 0, 0, 0);
		}

		public CParticle(Particle particle, int count) {
			this(particle, count, 0, 0, 0);
		}

		public CParticle(Particle particle, int count, double dx, double dy, double dz) {
			this(particle, count, dx, dy, dz, 0.0d);
		}

		public CParticle(Particle particle, int count, double dx, double dy, double dz, double extra1) {
			this(particle, count, dx, dy, dz, extra1, null);
		}

		public CParticle(Particle particle, int count, double dx, double dy, double dz, double extra1, @Nullable Object extra2) {
			mParticle = particle;
			mCount = count;
			mDx = dx;
			mDy = dy;
			mDz = dz;
			mVelocity = extra1;
			mExtra2 = extra2;
		}

		@Override
		public String toString() {
			if (mExtra2 != null && mExtra2 instanceof DustOptions) {
				String color = "#" + Integer.toHexString(((DustOptions) mExtra2).getColor().asRGB());
				String size = Float.toString(((DustOptions) mExtra2).getSize());
				return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mVelocity + "," + color + "," + size + ")";
			} else if (mExtra2 != null && mExtra2 instanceof BlockData) {
				return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mVelocity + "," + ((BlockData) mExtra2).getMaterial().name() + ")";
			} else if (mExtra2 != null && mExtra2 instanceof ItemStack) {
				return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mVelocity + "," + ((ItemStack) mExtra2).getType().name() + ")";
			} else if (mExtra2 != null) {
				String ret = "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mVelocity + "," + mExtra2 + ")";
				Plugin.getInstance().getLogger().warning("Got strange particle serialization to string of unknown type, likely plugin bug: " + ret);
				return ret;
			}
			return "(" + mParticle.name() + "," + mCount + "," + mDx + "," + mDy + "," + mDz + "," + mVelocity + ")";
		}

		public void spawn(LivingEntity boss, Location loc) {
			spawn(boss, loc, 0, 0, 0, 0.0d);
		}

		public void spawn(LivingEntity boss, Location loc, double dx, double dy, double dz) {
			spawn(boss, loc, dx, dy, dz, 0.0d);
		}

		public void spawn(LivingEntity boss, Location loc, double dx, double dy, double dz, double extra1) {
			double fdx = mDx != 0 ? mDx : dx;
			double fdy = mDy != 0 ? mDy : dy;
			double fdz = mDz != 0 ? mDz : dz;
			double fVelocity = mVelocity != 0.0d ? mVelocity : extra1;
			spawnNow(boss, loc, fdx, fdy, fdz, fVelocity, mExtra2);
		}

		public <T extends AbstractPartialParticle<T>> T toPartialParticle(T particle) {
			particle.count(mCount);
			if (mDx != 0) {
				particle.mDeltaX = mDx;
			}
			if (mDy != 0) {
				particle.mDeltaX = mDy;
			}
			if (mDz != 0) {
				particle.mDeltaX = mDz;
			}
			if (mVelocity != 0) {
				particle.extra(mVelocity);
			}
			particle.data(mExtra2);
			return particle;
		}

		private void spawnNow(LivingEntity boss, Location loc, double dx, double dy, double dz, double extra1, @Nullable Object extra2) {
			try {
				new PartialParticle(mParticle, loc, mCount, dx, dy, dz, extra1, extra2).spawnAsEntityActive(boss);
			} catch (Exception e) {
				Plugin.getInstance().getLogger().warning("Failed to spawn a particle at loc. Reason: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public static final ParticlesList EMPTY = fromString("[]");

	private final List<CParticle> mParticleList;

	public ParticlesList(List<CParticle> particles) {
		mParticleList = particles;
	}

	public boolean isEmpty() {
		return mParticleList.isEmpty();
	}

	public List<CParticle> getParticleList() {
		return mParticleList;
	}

	public void spawn(LivingEntity boss, Location loc) {
		spawn(boss, loc, 0, 0, 0);
	}

	public void spawn(LivingEntity boss, Location loc, double dx, double dy, double dz) {
		for (CParticle particle : mParticleList) {
			particle.spawn(boss, loc, dx, dy, dz);
		}
	}

	public void spawn(LivingEntity boss, Location loc, double dx, double dy, double dz, double extra1) {
		for (CParticle particle : mParticleList) {
			particle.spawn(boss, loc, dx, dy, dz, extra1);
		}
	}

	public <T extends AbstractPartialParticle<T>> void spawn(LivingEntity boss, Function<Particle, T> particleSupplier) {
		for (CParticle particle : mParticleList) {
			particle.toPartialParticle(particleSupplier.apply(particle.mParticle)).spawnAsEntityActive(boss);
		}
	}

	@Override
	public String toString() {
		String msg = "[";
		boolean first = true;
		for (CParticle cParticle : mParticleList) {
			msg = msg + (first ? "" : ",") + cParticle.toString();
			first = false;
		}
		return msg + "]";
	}

	public static ParticlesList fromString(String string) {
		ParseResult<ParticlesList> result = fromReader(new StringReader(string), "");
		if (result.getResult() == null) {
			Plugin.getInstance().getLogger().warning("Failed to parse '" + string + "' as ParticlesList");
			Thread.dumpStack();
			return new ParticlesList(new ArrayList<>(0));
		}

		return result.getResult();
	}

	/*
	 * Parses a ParticlesList at the next position in the StringReader.
	 * If this item parses successfully:
	 *   The returned ParseResult will contain a non-null getResult() and a null getTooltip()
	 *   The reader will be advanced to the next character past this ParticlesList value.
	 * Else:
	 *   The returned ParseResult will contain a null getResult() and a non-null getTooltip()
	 *   The reader will not be advanced
	 */
	public static ParseResult<ParticlesList> fromReader(StringReader reader, String hoverDescription) {
		if (!reader.advance("[")) {
			return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "[", hoverDescription)));
		}

		List<CParticle> particlesList = new ArrayList<>(2);

		boolean atLeastOneParticleIter = false;
		while (true) {
			// Start trying to parse the next individual particle entry in the list

			if (reader.advance("]")) {
				// Got closing bracket and parsed rest successfully - complete particle list, break this loop
				break;
			}

			if (atLeastOneParticleIter) {
				if (!reader.advance(",")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.ofString(reader.readSoFar() + ",", hoverDescription),
						Tooltip.ofString(reader.readSoFar() + "]", hoverDescription)
					));
				}
				if (!reader.advance("(")) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "(", hoverDescription)));
				}
			} else {
				if (!reader.advance("(")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.ofString(reader.readSoFar() + "(", hoverDescription),
						Tooltip.ofString(reader.readSoFar() + "]", hoverDescription)
					));
				}
			}

			atLeastOneParticleIter = true;
			Particle particle = reader.readParticle();
			if (particle == null) {
				// Entry not valid, offer all entries as completions
				List<Tooltip<String>> suggArgs = new ArrayList<>(Particle.values().length);
				String soFar = reader.readSoFar();
				for (Particle valid : Particle.values()) {
					suggArgs.add(Tooltip.ofString(soFar + valid.name(), hoverDescription));
				}
				return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
			}

			/* Valid CParticles:
			 * (REDSTONE,count=1,dx=0,dy=0,dz=0,velocity=1,#color=none,size=1)
			 * (BLOCK_CRACK|BLOCK_CRACK|FALLING_DUST,count=1,dx=0,dy=0,dz=0,velocity=1,Material=none)
			 * (ITEM_CRACK,count=1,dx=0,dy=0,dz=0,velocity=1,ItemStack=none)
			 * (<any other>,count=1,dx=0,dy=0,dz=0,velocity=1)
			 */
			if (!reader.advance(",")) {
				if (PARTICLES_WITH_PARAMETERS.contains(particle)) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", "Specify count, variance, velocity, and particle-specific parameters")));
				}
				if (!reader.advance(")")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.ofString(reader.readSoFar() + ",", "Specify count, variance, velocity, and particle-specific parameters"),
						Tooltip.ofString(reader.readSoFar() + ")", "Use default 1 particle, zero variance, 1 velocity")
					));
				}
				// End of this particle, loop to next
				particlesList.add(new CParticle(particle));
				continue;
			}

			Long count = reader.readLong();
			if (count == null || count < 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "1", "Particle count >= 0")));
			}

			if (!reader.advance(",")) {
				if (PARTICLES_WITH_PARAMETERS.contains(particle)) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", "Specify variance, velocity and particle-specific parameters")));
				}
				if (!reader.advance(")")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.ofString(reader.readSoFar() + ",", "Specify variance, velocity and particle-specific parameters"),
						Tooltip.ofString(reader.readSoFar() + ")", "Use default zero variance, 1 velocity")
					));
				}
				// End of this particle, loop to next
				particlesList.add(new CParticle(particle, count.intValue()));
				continue;
			}

			Double dx = reader.readDouble();
			if (dx == null || dx < 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "0.0", "X Variance >= 0")));
			}
			if (!reader.advance(",")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", hoverDescription)));
			}
			Double dy = reader.readDouble();
			if (dy == null || dy < 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "0.0", "Y Variance >= 0")));
			}
			if (!reader.advance(",")) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", hoverDescription)));
			}
			Double dz = reader.readDouble();
			if (dz == null || dz < 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "0.0", "Z Variance >= 0")));
			}

			if (!reader.advance(",")) {
				if (PARTICLES_WITH_PARAMETERS.contains(particle)) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", "Specify velocity and particle-specific parameters")));
				}
				if (!reader.advance(")")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.ofString(reader.readSoFar() + ",", "Specify velocity and particle-specific parameters"),
						Tooltip.ofString(reader.readSoFar() + ")", "Use default 1 velocity")
					));
				}
				// End of this particle, loop to next
				particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz));
				continue;
			}

			Double velocity = reader.readDouble();
			if (velocity == null || velocity < 0) {
				return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "1.0", "Velocity >= 0")));
			}

			if (!PARTICLES_WITH_PARAMETERS.contains(particle)) {
				if (!reader.advance(")")) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", hoverDescription)));
				}

				// End of this particle, loop to next
				particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz, velocity));
				continue;
			} else {
				if (!reader.advance(",")) {
					return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", "Specify particle-specific parameters")));
				}

				if (particle == Particle.REDSTONE || particle == Particle.DUST_COLOR_TRANSITION) {
					// Redstone takes a color, and an optional size
					Color color = reader.readColor();
					if (color == null) {
						// Color not valid - need to offer all colors as a completion option, plus #FFFFFF
						List<Tooltip<String>> suggArgs = new ArrayList<>(1 + StringReader.COLOR_MAP.size());
						String soFar = reader.readSoFar();
						for (String valid : StringReader.COLOR_MAP.keySet()) {
							suggArgs.add(Tooltip.ofString(soFar + valid, "Particle color"));
						}
						suggArgs.add(Tooltip.ofString("#FFFFFF", "Particle color"));
						return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
					}

					Color toColor = color;
					if (particle == Particle.DUST_COLOR_TRANSITION) {
						if (!reader.advance(",")) {
							return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ",", "add end color transition")));
						}
						toColor = reader.readColor();
						if (toColor == null) {
							// Color not valid - need to offer all colors as a completion option, plus #FFFFFF
							List<Tooltip<String>> suggArgs = new ArrayList<>(1 + StringReader.COLOR_MAP.size());
							String soFar = reader.readSoFar();
							for (String valid : StringReader.COLOR_MAP.keySet()) {
								suggArgs.add(Tooltip.ofString(soFar + valid, "Particle end color"));
							}
							suggArgs.add(Tooltip.ofString("#FFFFFF", "Particle end color"));
							return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
						}
					}

					float size = 1;
					if (reader.advance(",")) {
						Double parsedSize = reader.readDouble();
						if (parsedSize == null || parsedSize <= 0) {
							return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "1.0", "Size > 0")));
						}
						size = parsedSize.floatValue();
						if (!reader.advance(")")) {
							return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", hoverDescription)));
						}
					} else {
						if (!reader.advance(")")) {
							return ParseResult.of(Tooltip.arrayOf(
								Tooltip.ofString(reader.readSoFar() + ",", "Specify redstone particle size"),
								Tooltip.ofString(reader.readSoFar() + ")", "Use default 1 redstone size")
							));
						}
					}

					DustOptions data;
					if (particle == Particle.DUST_COLOR_TRANSITION) {
						data = new Particle.DustTransition(color, toColor, size);
					} else {
						data = new DustOptions(color, size);
					}

					// End of this particle, loop to next
					particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz, velocity, data));
					continue;
				} else {
					// All other supported parameter particles take a material
					Material mat = reader.readMaterial();
					if (mat == null) {
						// Entry not valid, offer all entries as completions
						List<Tooltip<String>> suggArgs = new ArrayList<>(Material.values().length);
						String soFar = reader.readSoFar();
						for (Material valid : Material.values()) {
							suggArgs.add(Tooltip.ofString(soFar + valid.name(), hoverDescription));
						}
						return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
					}

					if (!reader.advance(")")) {
						return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + ")", hoverDescription)));
					}

					// End of this particle, loop to next
					if (particle.equals(Particle.ITEM_CRACK)) {
						// ITEM_CRACK particle requires an itemstack, not the underlying material
						particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz, velocity, new ItemStack(mat)));
					} else {
						particlesList.add(new CParticle(particle, count.intValue(), dx, dy, dz, velocity, mat.createBlockData()));
					}
					continue;
				}
			}
		}

		return ParseResult.of(new ParticlesList(particlesList));
	}
}
