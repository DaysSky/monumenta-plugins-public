package com.playmonumenta.plugins.portals;

import com.playmonumenta.plugins.utils.NmsUtils;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Portal {
	private static final double VERTICAL_BOOST = 0.18;

	public int mPortalNum;
	public UUID mUuid1;
	public UUID mUuid2;

	public Location mLocation1;
	public Location mLocation2;

	public Location mBlock1;
	public Location mBlock2;

	public BlockFace mFacing;
	public Vector mPortalTopDirection;
	public Vector mPortalOutDirection;

	//The portal it links to
	public @Nullable Portal mPair;
	//The owner of the portal
	public @Nullable Player mOwner;

	public Portal(int portalNum, UUID uuid1, UUID uuid2, Location loc1, Location loc2, BlockFace face, Location b1, Location b2) {
		mPortalNum = portalNum;
		mUuid1 = uuid1;
		mUuid2 = uuid2;
		mLocation1 = loc1;
		mLocation2 = loc2;
		mFacing = face;
		mBlock1 = b1;
		mBlock2 = b2;
		mPortalTopDirection = b2.toVector().subtract(b1.toVector());
		mPortalOutDirection = face.getDirection();
	}

	public @Nullable Vector getShift() {
		if (mFacing == BlockFace.UP) {
			return new Vector(0, 1, 0);
		} else if (mFacing == BlockFace.DOWN) {
			return new Vector(0, -1, 0);
		} else if (mFacing == BlockFace.WEST) {
			return new Vector(-1, 0, 0);
		} else if (mFacing == BlockFace.EAST) {
			return new Vector(1, 0, 0);
		} else if (mFacing == BlockFace.NORTH) {
			return new Vector(0, 0, -1);
		} else if (mFacing == BlockFace.SOUTH) {
			return new Vector(0, 0, 1);
		}
		return null;
	}

	public Location getYaw(Location loc, Entity p) {
		Location l = loc.clone();
		if (mFacing == BlockFace.SOUTH) {
			l.setYaw(0);
		} else if (mFacing == BlockFace.NORTH) {
			l.setYaw(180);
		} else if (mFacing == BlockFace.WEST) {
			l.setYaw(90);
		} else if (mFacing == BlockFace.EAST) {
			l.setYaw(-90);
		} else {
			l.setYaw(p.getLocation().getYaw());

		}
		l.setPitch(p.getLocation().getPitch());
		return l;
	}

	public World getWorld() {
		return mLocation1.getWorld();
	}

	public BoundingBox getBoundingBox() {
		return BoundingBox.of(mLocation1, mLocation2).expand(0, 0, 0, 1, 1, 1);
	}

	private Vector portalLeftDirection() {
		return mPortalTopDirection.getCrossProduct(mPortalOutDirection);
	}

	private Location centerLocation() {
		return mLocation1.clone().toCenterLocation().add(mPortalTopDirection.clone().multiply(0.5));
	}

	private static boolean willBeInBlock(Entity entity, Location location) {
		return NmsUtils.getVersionAdapter().hasCollisionWithBlocks(location.getWorld(), entity.getBoundingBox().shift(entity.getLocation().multiply(-1)).shift(location), false);
	}

	private void fixInsideWall(Entity entity, Location location) {
		double entityHalfWidth = entity.getWidth() / 2.0;
		switch (mFacing) {
		case UP:
			// y+
			location.setY(Math.max(location.getY(), mLocation1.getY()));
			break;
		case DOWN:
			// y-
			location.setY(Math.min(location.getY(), mLocation1.getY() + 1.0 - entity.getHeight()));
			break;
		case SOUTH:
			// z+
			location.setZ(Math.max(location.getZ(), mLocation1.getZ() + entityHalfWidth));
			break;
		case NORTH:
			// z-
			location.setZ(Math.min(location.getZ(), mLocation1.getZ() + 1.0 - entityHalfWidth));
			break;
		case EAST:
			// x+
			location.setX(Math.max(location.getX(), mLocation1.getX() + entityHalfWidth));
			break;
		case WEST:
		default:
			// x-
			location.setX(Math.min(location.getX(), mLocation1.getX() + 1.0 - entityHalfWidth));
			break;
		}
	}

	private Location defaultTeleportLocation(Entity entity) {
		Location centerLoc = centerLocation();
		double entityHalfWidth = entity.getWidth() / 2.0;
		Location location;
		switch (mFacing) {
		case UP:
			// y+
			location = centerLoc.clone();
			location.setY(mLocation1.getY());
			break;
		case DOWN:
			// y-
			location = centerLoc.clone();
			location.setY(mLocation1.getY() + 1.0 - entity.getHeight());
			break;
		case SOUTH:
			// z+
			location = mLocation1.clone();
			location.setX(location.getX() + 0.5);
			location.setZ(location.getZ() + entityHalfWidth);
			break;
		case NORTH:
			// z-
			location = mLocation1.clone();
			location.setX(location.getX() + 0.5);
			location.setZ(location.getZ() + 1.0 - entityHalfWidth);
			break;
		case EAST:
			// x+
			location = mLocation1.clone();
			location.setZ(location.getZ() + 0.5);
			location.setX(location.getX() + entityHalfWidth);
			break;
		case WEST:
		default:
			// x-
			location = mLocation1.clone();
			location.setZ(location.getZ() + 0.5);
			location.setX(location.getX() + 1.0 - entityHalfWidth);
			break;
		}
		return location;
	}

	private static double getVectorComponent(Vector input, Vector direction) {
		return input.getX() * direction.getX() + input.getY() * direction.getY() + input.getZ() * direction.getZ();
	}

	private Vector fromInterPortalComponents(Vector input) {
		Vector result = new Vector();
		result.add(portalLeftDirection().clone().multiply(input.getX()));
		result.add(mPortalTopDirection.clone().multiply(input.getY()));
		result.add(mPortalOutDirection.clone().multiply(input.getZ()));
		return result;
	}

	// Does not handle look; that is better handled separately
	private Location toInterPortalCoords(Location locIn) {
		Vector locCentered = locIn.clone().subtract(centerLocation()).toVector();
		double relativeLeft = getVectorComponent(locCentered, portalLeftDirection());
		double relativeUp = getVectorComponent(locCentered, mPortalTopDirection);
		double relativeForward = getVectorComponent(locCentered, mPortalOutDirection);
		return new Location(locIn.getWorld(), -relativeLeft, relativeUp, -relativeForward);
	}

	private Vector toInterPortalDirection(Vector directionIn) {
		double relativeLeft = getVectorComponent(directionIn, portalLeftDirection());
		double relativeUp = getVectorComponent(directionIn, mPortalTopDirection);
		double relativeForward = getVectorComponent(directionIn, mPortalOutDirection);
		return new Vector(-relativeLeft, relativeUp, -relativeForward);
	}

	// Does not handle look; that is better handled separately
	private Location fromInterPortalCoords(Location locIn) {
		return centerLocation().add(fromInterPortalComponents(locIn.toVector()));
	}

	private Vector fromInterPortalDirection(Vector directionIn) {
		return fromInterPortalComponents(directionIn);
	}

	public List<Location> occupiedLocations() {
		return List.of(mLocation1.toBlockLocation(), mLocation2.toBlockLocation(), mBlock1.toBlockLocation(), mBlock2.toBlockLocation());
	}

	// Travel from this portal to the other portal
	public void travel(Entity entity) {
		travel(entity, entity.getVelocity());
	}

	public void travel(Entity entity, Vector velocity) {
		if (mPair == null) {
			return;
		}

		if (mLocation1.getWorld() != mLocation2.getWorld()) {
			return;
		}

		double halfHeight = entity.getHeight() / 2;
		Location location = entity.getLocation().clone();
		Vector direction = location.getDirection();
		if (mFacing == BlockFace.DOWN) {
			direction.setY(Math.abs(direction.getY()));
		}
		Vector fireballDirection = direction; // Dummy value to satisfy the compiler; not actually used.
		if (entity instanceof Fireball fireball) {
			fireballDirection = fireball.getDirection();
		}

		location = mPair.fromInterPortalCoords(toInterPortalCoords(location.add(0, halfHeight, 0))).subtract(0, halfHeight, 0);
		mPair.fixInsideWall(entity, location);
		if (willBeInBlock(entity, location)) {
			location = mPair.defaultTeleportLocation(entity);
		}
		direction = mPair.fromInterPortalDirection(toInterPortalDirection(direction));
		velocity = mPair.fromInterPortalDirection(toInterPortalDirection(velocity));
		velocity.add(mPair.mFacing.getDirection().multiply(VERTICAL_BOOST));
		if (entity instanceof Fireball) {
			fireballDirection = mPair.fromInterPortalDirection(toInterPortalDirection(fireballDirection));
		}

		location.setDirection(direction);
		entity.teleport(location);
		entity.setVelocity(velocity);
		if (entity instanceof Fireball fireball) {
			fireball.setDirection(fireballDirection);
		}
	}
}
