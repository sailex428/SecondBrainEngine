package me.sailex.altoclef.util.helpers;

import me.sailex.altoclef.Debug;
import me.sailex.altoclef.util.baritone.CachedProjectile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;

public class ProjectileHelper {
   public static final double ARROW_GRAVITY_ACCEL = 0.05F;
   public static final double THROWN_ENTITY_GRAVITY_ACCEL = 0.03;

   public static boolean hasGravity(ProjectileEntity entity) {
      return entity instanceof ExplosiveProjectileEntity ? false : !entity.hasNoGravity();
   }

   private static Vec3d getClosestPointOnFlatLine(double shootX, double shootZ, double velX, double velZ, double playerX, double playerZ) {
      double deltaX = playerX - shootX;
      double deltaZ = playerZ - shootZ;
      double t = (velX * deltaX + velZ * deltaZ) / (velX * velX + velZ * velZ);
      double hitX = shootX + velX * t;
      double hitZ = shootZ + velZ * t;
      return new Vec3d(hitX, 0.0, hitZ);
   }

   public static double getFlatDistanceSqr(double shootX, double shootZ, double velX, double velZ, double playerX, double playerZ) {
      return getClosestPointOnFlatLine(shootX, shootZ, velX, velZ, playerX, playerZ).squaredDistanceTo(playerX, 0.0, playerZ);
   }

   private static double getArrowHitHeight(double gravity, double horizontalVel, double verticalVel, double initialHeight, double distanceTraveled) {
      double time = distanceTraveled / horizontalVel;
      return initialHeight - verticalVel * time - 0.5 * gravity * time * time;
   }

   public static Vec3d calculateArrowClosestApproach(Vec3d shootOrigin, Vec3d shootVelocity, double yGravity, Vec3d playerOrigin) {
      Vec3d flatEncounter = getClosestPointOnFlatLine(shootOrigin.x, shootOrigin.z, shootVelocity.x, shootVelocity.z, playerOrigin.x, playerOrigin.z);
      double encounterDistanceTraveled = flatEncounter.subtract(shootOrigin.x, flatEncounter.y, shootOrigin.z).length();
      double horizontalVel = Math.sqrt(shootVelocity.x * shootVelocity.x + shootVelocity.z * shootVelocity.z);
      double verticalVel = shootVelocity.y;
      double initialHeight = shootOrigin.y;
      double hitHeight = getArrowHitHeight(yGravity, horizontalVel, verticalVel, initialHeight, encounterDistanceTraveled);
      return new Vec3d(flatEncounter.x, hitHeight, flatEncounter.z);
   }

   public static Vec3d calculateArrowClosestApproach(CachedProjectile projectile, Vec3d pos) {
      return calculateArrowClosestApproach(projectile.position, projectile.velocity, projectile.gravity, pos);
   }

   public static double[] calculateAnglesForSimpleProjectileMotion(double launchHeight, double launchTargetDistance, double launchVelocity, double gravity) {
      double y = -1.0 * launchHeight;
      double root = launchVelocity * launchVelocity * launchVelocity * launchVelocity
         - gravity * (gravity * launchTargetDistance * launchTargetDistance + 2.0 * y * launchVelocity * launchVelocity);
      if (root < 0.0) {
         Debug.logMessage("Not enough velocity, returning 45 degrees.");
         return new double[]{45.0, 45.0};
      } else {
         double tanTheta0 = (launchVelocity * launchVelocity + Math.sqrt(root)) / gravity * launchTargetDistance;
         double tanTheta1 = (launchVelocity * launchVelocity - Math.sqrt(root)) / gravity * launchTargetDistance;
         double[] angles = new double[]{Math.toDegrees(Math.atan(tanTheta0)), Math.toDegrees(Math.atan(tanTheta1))};
         return new double[]{Math.min(angles[0], angles[1]), Math.max(angles[0], angles[1])};
      }
   }

   public static Vec3d getThrowOrigin(Entity entity) {
      return entity.getPos().subtract(0.0, 0.1, 0.0);
   }

   @Deprecated
   private static double getNearestTimeOfShotProjectile(Vec3d shootOrigin, Vec3d shootVelocity, double yGravity, Vec3d playerOrigin) {
      Vec3d D = playerOrigin.subtract(shootOrigin);
      double a = yGravity * yGravity / 2.0;
      double b = -(3.0 * yGravity * shootVelocity.y) / 2.0;
      double c = shootVelocity.lengthSquared() + yGravity * shootVelocity.y;
      double d = -1.0 * shootVelocity.dotProduct(D);
      double p = -b / 3.0 * a;
      double q = p * p * p + (b * c - 3.0 * a * d) / 6.0 * a * a;
      double r = c / 3.0 * a;
      double rootInner = q * q + Math.pow(r - p * p, 3.0);
      if (rootInner < 0.0) {
         return -1.0;
      } else {
         rootInner = Math.sqrt(rootInner);
         double outerPreCubeLeft = q + rootInner;
         double outerPreCubeRight = q - rootInner;
         return Math.pow(outerPreCubeLeft, 0.3333333333333333) + Math.pow(outerPreCubeRight, 0.3333333333333333) + p;
      }
   }
}
