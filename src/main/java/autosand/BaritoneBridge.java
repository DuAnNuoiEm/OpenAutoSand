package autosand;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.pathing.goals.GoalGetToBlock;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

final class BaritoneBridge {
   private static boolean owns = false;
   private static Boolean oldBreak;
   private static Boolean oldPlace;

   private BaritoneBridge() {
   }

   static boolean installed() {
      return FabricLoader.getInstance().isModLoaded("baritone");
   }

   private static IBaritone get() {
      return BaritoneAPI.getProvider().getPrimaryBaritone();
   }

   static boolean goTo(BlockPos pos) {
      if (!installed()) {
         return false;
      } else {
         try {
            IBaritone b = get();
            if (b.getPathingBehavior().isPathing() && !owns) {
               return false;
            } else {
               if (oldBreak == null) {
                  oldBreak = (Boolean)BaritoneAPI.getSettings().allowBreak.value;
                  oldPlace = (Boolean)BaritoneAPI.getSettings().allowPlace.value;
               }

               BaritoneAPI.getSettings().allowBreak.value = false;
               BaritoneAPI.getSettings().allowPlace.value = false;
               b.getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(pos));
               owns = true;
               return true;
            }
         } catch (Throwable var2) {
            restore();
            return false;
         }
      }
   }

   static boolean isPathing() {
      try {
         return installed() && get().getPathingBehavior().isPathing();
      } catch (Throwable var1) {
         return false;
      }
   }

   static boolean isBusy() {
      try {
         if (!installed()) {
            return false;
         } else {
            IPathingBehavior pb = get().getPathingBehavior();
            return pb.isPathing() || pb.getInProgress().isPresent();
         }
      } catch (Throwable var1) {
         return false;
      }
   }

   static void cancel() {
      try {
         if (owns && installed()) {
            get().getPathingBehavior().cancelEverything();
         }
      } catch (Throwable var1) {
      }

      owns = false;
      restore();
   }

   static void restore() {
      try {
         if (oldBreak != null) {
            BaritoneAPI.getSettings().allowBreak.value = oldBreak;
         }

         if (oldPlace != null) {
            BaritoneAPI.getSettings().allowPlace.value = oldPlace;
         }
      } catch (Throwable var1) {
      }

      oldPlace = null;
      oldBreak = null;
   }
}
