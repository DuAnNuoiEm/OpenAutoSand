package autosand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

final class AutoMiner {
   static String homeCmd = "home cat";
   static boolean deathRecover = true;
   static boolean autoRespawn = true;
   static boolean digThrough = true;
   static int scanRadius = 16;
   static final int[] SCAN_LEVELS = new int[]{8, 12, 16, 24};
   static boolean tunnel = false;
   static int tunnelLayers = 3;
   static int tunnelWidth = 3;
   static int tunnelBelow = 0;
   static final int[] TUNNEL_LAYER_LEVELS = new int[]{2, 3, 4, 5, 6, 8};
   static final int[] TUNNEL_WIDTH_LEVELS = new int[]{1, 3, 5};
   static final int[] TUNNEL_BELOW_LEVELS = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
   static boolean tunnelInit = false;
   static boolean tunnelMove = false;
   static Direction tunnelDir = Direction.NORTH;
   static int tunnelAx = 0;
   static int tunnelAz = 0;
   static int tunnelBase = 0;
   static int tunnelFails = 0;
   static int tunnelTurns = 0;
   static int tunnelPause = 0;
   static final double REACH = 4.3;
   static final double EYE_H = 1.62;
   static final int MAX_NODES = 2500;
   static final int MAX_RANGE = 48;
   static final int FALL_MAX = 3;
   static final double DIG_COST = 5.0;
   static final int MINE_TIMEOUT = 160;
   static final int TRAVEL_TIMEOUT = 500;
   static final int MAX_DEPTH = 6;
   static final int COLLECT_R = 12;
   static final int RECOVER_TIMEOUT = 9000;
   static final int RECOVER_NODES = 6000;
   static final int HOME_STILL = 200;
   static final double PEARL_MIN_DIST = 60.0;
   static final double PEARL_STOP_DIST = 12.0;
   static final double PEARL_RANGE = 45.0;
   static final float PEARL_MIN_HP = 12.0F;
   static final int PEARL_MAX_THROWS = 4;
   static final Set<Block> DIGGABLE = Set.of(Blocks.SAND, Blocks.RED_SAND);
   static final int P_IDLE = 0;
   static final int P_MOVE = 1;
   static final int P_MINE = 2;
   static final int P_ITEM = 3;
   static final int P_EXPLORE = 4;
   static final int RUN = 0;
   static final int DONE = 1;
   static final int FAIL = 2;
   static boolean active = false;
   static int ph = 0;
   static int phTicks = 0;
   static int stuck = 0;
   static int scanCool = 0;
   static int noTarget = 0;
   static int exploreCount = 0;
   static int ignoreAge = 0;
   static int baseY = -1000000;
   static BlockPos target = null;
   static boolean descendPhase = true;
   static boolean descendTarget = false;
   static int descendWaitTicks = 0;
   static int descendDepth = 2;
   static boolean nukerHold = true;
   static int descendStartY = Integer.MIN_VALUE;
   static final int NUKER_HOLD_R = 4;
   static final int NUKER_HOLD_MAX = 60;
   static int nukerHoldTicks = 0;
   static int nukerHoldLast = -1;
   static int nukerHoldCool = 0;
   static boolean nukerHoldHad = false;
   static long nukerHoldAt = -1000L;
   static ItemEntity itemTarget = null;
   static List<BlockPos> path = null;
   static int pi = 0;
   static Vec3 lastPos = null;
   static boolean baritoneOwnsPath = false;
   static double exploreYaw = 0.0;
   static long failKey = Long.MIN_VALUE;
   static int failCount = 0;
   static final Set<Long> ignoreBlocks = new HashSet<>();
   static final Set<Integer> ignoreItems = new HashSet<>();
   static final int R_NONE = 0;
   static final int R_WAIT = 1;
   static final int R_HOME = 2;
   static final int R_WALK = 3;
   static final int R_COLLECT = 4;
   static final int R_ORG = 5;
   static final int R_PEARL = 6;
   static final int R_CROSS = 7;
   static int rec = 0;
   static int recTimer = 0;
   static int recTicks = 0;
   static int recStep = 0;
   static int homeTries = 0;
   static int walkNoProg = 0;
   static int walkFails = 0;
   static int blindTicks = 0;
   static int collectIdle = 0;
   static int collectTicks = 0;
   static int itemWait = 0;
   static double walkBest = Double.MAX_VALUE;
   static boolean deathRecorded = false;
   static boolean shovelWarned = false;
   static BlockPos deathPos = null;
   static Object deathDim = null;
   static int deadTicks = 0;
   static Vec3 homeFrom = null;
   static Vec3 itemPlanPos = null;
   static String lastDeathText = null;
   static float pearlYaw = 0.0F;
   static float pearlPitch = 0.0F;
   static int kitStep = 2;
   static int kitDropped = 0;
   static float kitXRot = Float.NaN;
   static boolean deathMarker = false;
   static boolean gotShovel = false;
   static boolean teleported = false;
   static boolean canDig = false;
   static boolean pearlDone = false;
   static int pearlBuyFails = 0;
   static int pearlThrows = 0;
   static int pearlStep = 0;
   static int pearlTimer = 0;
   static int pearlHpWait = 0;
   static int arrowCnt = 0;
   static int hpEatCool = 0;
   static int hpEatTries = 0;
   static final int CROSS_MAX_THROWS = 4;
   static final double CROSS_MAX_RANGE = 30.0;
   static int crossThrows = 0;
   static int crossStep = 0;
   static int crossCool = 0;
   static int crossWait = 0;
   static Vec3 crossFrom = null;
   static final Item[] snap = new Item[9];
   static final boolean[] snapShovel = new boolean[9];
   static int snapTick = 0;
   static boolean shaft = false;
   static final int SHAFT_R = 1;
   static final int SHAFT_MAX_DEPTH = 40;
   static boolean shaftInit = false;
   static int shaftX;
   static int shaftZ;
   static int shaftTop;
   static boolean shaftBlocked = false;
   static final int NEAR_RADIUS = 5;
   static int retreatTicks = 0;
   static int retreatCool = 0;
   static int digAcc = 0;
   static List<BlockPos> pgPath = null;
   static Vec3 pgPos = Vec3.ZERO;
   static int pgTicks = 0;
   static int pgFails = 0;
   static int pgHit = 0;
   static boolean orgDone = false;
   static String orgWhy = null;
   static int fixCool = 0;
   static final String[] ARROWS = new String[]{"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
   static final double PEARL_SPEED = 1.5;
   static final double PEARL_G = 0.03;
   static final double PEARL_DRAG = 0.99;

   private AutoMiner() {
   }

   static boolean holdingForNuker(ClientLevel lv) {
      return lv.getGameTime() - nukerHoldAt <= 3L;
   }

   static void start(LocalPlayer p) {
      active = true;
      int y = p.blockPosition().getY();
      if (y > baseY) {
         baseY = y;
      }

      if (rec == 0) {
         path = null;
         target = null;
         itemTarget = null;
         ph = 0;
         phTicks = 0;
         stuck = 0;
      }
   }

   static void stop(Minecraft mc) {
      active = false;
      cancelBaritoneNavigation();
      path = null;
      target = null;
      itemTarget = null;
      ph = 0;
      phTicks = 0;
      release(mc);
   }

   static void reset(Minecraft mc) {
      orgDone = false;
      tunnelInit = false;
      tunnelMove = false;
      tunnelFails = 0;
      tunnelTurns = 0;
      tunnelPause = 0;
      stop(mc);
      rec = 0;
      deathRecorded = false;
      baseY = -1000000;
      descendPhase = true;
      descendTarget = false;
      descendWaitTicks = 0;
      descendStartY = Integer.MIN_VALUE;
      nukerHoldTicks = 0;
      nukerHoldLast = -1;
      nukerHoldCool = 0;
      nukerHoldHad = false;
      noTarget = 0;
      exploreCount = 0;
      ignoreBlocks.clear();
      ignoreItems.clear();
   }

   static boolean recovering() {
      return rec != 0;
   }

   static void release(Minecraft mc) {
      mc.options.keyUp.setDown(false);
      mc.options.keyJump.setDown(false);
      mc.options.keySprint.setDown(false);
      mc.options.keyAttack.setDown(false);
   }

   static void onDead(Minecraft mc, LocalPlayer p) {
      release(mc);
      if (!deathRecorded || gotShovel) {
         deathRecorded = true;
         gotShovel = false;
         deadTicks = 0;
         deathPos = p.blockPosition();
         deathDim = mc.level != null ? mc.level.dimension() : null;
         lastDeathText = deathPos.getX() + " " + deathPos.getY() + " " + deathPos.getZ();
         deathMarker = true;
         AutoSandOrders.statDeaths++;
         AutoSandOrders.save();
         AutoSandOrders.msg(p, "Đã chết tại " + lastDeathText + " (đã ghi nhớ toạ độ).");
      }

      if (autoRespawn && ++deadTicks % 20 == 10) {
         p.respawn();
      }
   }

   static void freshStart() {
      cancelBaritoneNavigation();
      ignoreBlocks.clear();
      ignoreItems.clear();
      ignoreAge = 0;
      tunnelInit = false;
      tunnelMove = false;
      tunnelFails = 0;
      tunnelTurns = 0;
      tunnelPause = 0;
      shaftInit = false;
      shaftBlocked = false;
      scanCool = 0;
      noTarget = 0;
      exploreCount = 0;
      path = null;
      target = null;
      itemTarget = null;
      ph = 0;
      phTicks = 0;
      stuck = 0;
   }

   static void onRespawn(Minecraft mc, LocalPlayer p) {
      freshStart();
      path = null;
      target = null;
      itemTarget = null;
      ph = 0;
      release(mc);
      baseY = -1000000;
      tunnelInit = false;
      tunnelMove = false;
      if (deathRecorded && deathPos != null) {
         rec = 1;
         recTimer = 40;
         recTicks = 0;
         recStep = 0;
         homeTries = 0;
         walkNoProg = 0;
         walkFails = 0;
         blindTicks = 0;
         walkBest = Double.MAX_VALUE;
         collectIdle = 0;
         shovelWarned = false;
         pearlDone = false;
         pearlBuyFails = 0;
         crossThrows = 0;
         crossStep = 0;
         crossCool = 0;
         teleported = false;
         kitStep = 0;
         kitDropped = 0;
         orgDone = false;
         ignoreItems.clear();
         AutoSandOrders.msg(p, "Đã hồi sinh. Chuẩn bị /" + homeCmd + ", đứng yên 10 giây rồi mới đi tiếp.");
      } else {
         deathRecorded = false;
      }
   }

   static String deathText() {
      return lastDeathText == null ? "Chưa chết lần nào" : "Chết ở " + lastDeathText;
   }

   static String recoverText() {
      return switch (rec) {
         case 1 -> "Vừa hồi sinh, chuẩn bị về home";
         case 2 -> "Chờ /" + homeCmd + " dịch chuyển";
         case 3 -> "Đang đi về chỗ chết";
         case 4 -> "Đang nhặt đồ rơi lúc chết";
         case 5 -> "Đang xếp xẻng / đồ lên thanh công cụ";
         case 6 -> "Đang ném ngọc Ender tới chỗ chết";
         case 7 -> "Đang ném ngọc Ender qua nước";
         default -> "Đang đào cát";
      };
   }

   static void tick(Minecraft mc, LocalPlayer p) {
      ClientLevel lv = mc.level;
      if (lv != null) {
         canDig = digThrough && hotbarShovel(p) >= 0;
         if (rec != 0) {
            if (AutoSandOrders.useNuker) {
               AutoSandOrders.setNuker(p, false);
            }

            tickRecovery(mc, p, lv);
         } else if (active) {
            if (tunnelPause > 0) {
               tunnelPause--;
            }

            fixShovel(mc, p);
            if (!waterRetreat(mc, p, lv)) {
               phTicks++;
               if (scanCool > 0) {
                  scanCool--;
               }

               switch (ph) {
                  case 0:
                     chooseAction(mc, p, lv);
                     break;
                  case 1:
                  case 3:
                  case 4:
                     tickTravel(mc, p, lv);
                     break;
                  case 2:
                     tickMine(mc, p, lv);
                     break;
                  default:
                     ph = 0;
               }
            }
         }
      }
   }

   static void snapshot(LocalPlayer p) {
      if (!deathRecorded && rec == 0) {
         if (++snapTick >= 40) {
            snapTick = 0;
            boolean has = false;

            for (int i = 0; i < 9; i++) {
               if (p.getInventory().getItem(i).is(ItemTags.SHOVELS)) {
                  has = true;
               }
            }

            if (has) {
               for (int ix = 0; ix < 9; ix++) {
                  ItemStack st = p.getInventory().getItem(ix);
                  snap[ix] = st.isEmpty() ? null : st.getItem();
                  snapShovel[ix] = st.is(ItemTags.SHOVELS);
               }
            }
         }
      }
   }

   static void chooseAction(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      release(mc);
      if (scanCool <= 0) {
         if (ignoreBlocks.size() > 200 || ++ignoreAge > 1200) {
            ignoreBlocks.clear();
            ignoreItems.clear();
            ignoreAge = 0;
         }

         if (descendPhase && planDescendSand(p, lv)) {
            noTarget = 0;
            exploreCount = 0;
         } else {
            if (AutoSandOrders.pickupRadius > 0 && !AutoSandOrders.isFull(p)) {
               ItemEntity it = findDrop(lv, p, p.position(), AutoSandOrders.pickupRadius, true);
               if (it != null) {
                  Vec3 ip = it.position();
                  List<BlockPos> r = findPath(
                     lv,
                     p.blockPosition(),
                     ip,
                     q -> Math.hypot(q.getX() + 0.5 - ip.x, q.getZ() + 0.5 - ip.z) < 0.9 && Math.abs(q.getY() - ip.y) < 2.0,
                     1200,
                     false
                  );
                  if (r != null && !r.isEmpty()) {
                     itemTarget = it;
                     path = r;
                     pi = 0;
                     stuck = 0;
                     ph = 3;
                     phTicks = 0;
                     return;
                  }

                  ignoreItems.add(it.getId());
               }
            }

            if (nukerHold(mc, p, lv)) {
               noTarget = 0;
               exploreCount = 0;
            } else if (planToSand(p, lv)) {
               noTarget = 0;
               exploreCount = 0;
            } else {
               baseY = p.blockPosition().getY();
               if (++exploreCount > 40 || !planExplore(lv, p)) {
                  scanCool = 40;
                  if (++noTarget >= 10 || exploreCount > 40) {
                     AutoSandOrders.msg(p, "Không tìm thấy cát quanh đây và không thăm dò thêm được. Tắt AutoSand.");
                     AutoSandOrders.setEnabled(mc, false);
                  }
               }
            }
         }
      }
   }

   static BlockPos tunnelCell(int s, int w, int y) {
      int fx = tunnelDir.getStepX();
      int fz = tunnelDir.getStepZ();
      return new BlockPos(tunnelAx + fx * s - fz * w, y, tunnelAz + fz * s + fx * w);
   }

   static int tunnelProgress(BlockPos b) {
      return (b.getX() - tunnelAx) * tunnelDir.getStepX() + (b.getZ() - tunnelAz) * tunnelDir.getStepZ();
   }

   static int tunnelLateral(BlockPos b) {
      return -(b.getX() - tunnelAx) * tunnelDir.getStepZ() + (b.getZ() - tunnelAz) * tunnelDir.getStepX();
   }

   static void tunnelStart(LocalPlayer p, Direction d, boolean keepBase) {
      BlockPos b = p.blockPosition();
      tunnelDir = d;
      tunnelAx = b.getX();
      tunnelAz = b.getZ();
      if (!keepBase) {
         tunnelBase = b.getY();
      }

      tunnelInit = true;
      tunnelFails = 0;
   }

   static boolean tunnelTurn(LocalPlayer p, String why) {
      tunnelTurns++;
      if (tunnelTurns > 2) {
         tunnelTurns = 0;
         tunnelPause = 1200;
         tunnelInit = false;
         AutoSandOrders.msg(p, "Đào hầm dừng: " + why + ". Chuyển sang đào thường 1 phút.");
         return false;
      } else {
         tunnelStart(p, tunnelTurns == 1 ? tunnelDir.getClockWise() : tunnelDir.getOpposite(), true);
         AutoSandOrders.msg(p, "Đào hầm: " + why + " -> rẽ sang hướng khác.");
         return true;
      }
   }

   static boolean planTunnel(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      BlockPos me = p.blockPosition();
      if (tunnelInit && (Math.abs(tunnelLateral(me)) > 3 || Math.abs(me.getY() - tunnelBase) > 6 + tunnelBelow)) {
         tunnelInit = false;
      }

      if (!tunnelInit) {
         tunnelStart(p, p.getDirection(), false);
      }

      if (tunnelFails >= 3) {
         tunnelFails = 0;
         return tunnelTurn(p, "không đi tiếp được");
      } else {
         int half = tunnelWidth / 2;
         int yMin = tunnelBase - tunnelBelow;
         int yMax = yMin + tunnelLayers - 1;
         int s0 = tunnelProgress(me);
         Vec3 eye = p.getEyePosition();
         List<AutoMiner.Cand> cells = new ArrayList<>();
         boolean sandAhead = false;

         for (int s = s0; s <= s0 + 8; s++) {
            for (int w = -half; w <= half; w++) {
               for (int y = yMax; y >= yMin; y--) {
                  BlockPos c = tunnelCell(s, w, y);
                  if (lv.getBlockState(c).is(Blocks.SAND)) {
                     sandAhead = true;
                     if (s <= s0 + 2 && !ignoreBlocks.contains(c.asLong()) && !underOrOver(p, c)) {
                        cells.add(new AutoMiner.Cand(c, (s - s0) * 100.0 + (yMax - y) * 10.0 + Math.abs(w)));
                     }
                  }
               }
            }
         }

         cells.sort(Comparator.comparingDouble(c -> c.score));

         for (AutoMiner.Cand c : cells) {
            if (waterWithin(lv, c.pos, 2)) {
               ignoreBlocks.add(c.pos.asLong());
            } else if (canMineFromEye(lv, p, eye, c.pos)) {
               target = c.pos;
               path = null;
               ph = 2;
               phTicks = 0;
               tunnelFails = 0;
               tunnelTurns = 0;
               tunnelMove = false;
               return true;
            }
         }

         if (!sandAhead) {
            return tunnelTurn(p, "hết cát phía trước");
         } else {
            BlockPos f1 = tunnelCell(s0 + 1, 0, me.getY());

            for (BlockPos q : new BlockPos[]{f1, f1.above()}) {
               BlockState st = lv.getBlockState(q);
               if (!st.getFluidState().isEmpty() || hazardState(st)) {
                  return tunnelTurn(p, "có nước / vật nguy hiểm phía trước");
               }

               if (!st.getCollisionShape(lv, q).isEmpty() && !st.is(Blocks.SAND)) {
                  return tunnelTurn(p, "bị chặn bởi khối không phải cát");
               }
            }

            int gx = f1.getX();
            int gz = f1.getZ();
            List<BlockPos> r = findPath(
               lv, me, Vec3.atCenterOf(f1), qx -> qx.getX() == gx && qx.getZ() == gz && Math.abs(qx.getY() - me.getY()) <= 3, 800, false
            );
            if (r != null && !r.isEmpty()) {
               path = r;
               pi = 0;
               stuck = 0;
               tunnelMove = true;
               ph = 4;
               phTicks = 0;
               return true;
            } else {
               tunnelFails++;
               scanCool = 5;
               return true;
            }
         }
      }
   }

   static boolean planShaft(LocalPlayer p, ClientLevel lv) {
      shaftBlocked = false;
      BlockPos me = p.blockPosition();
      if (shaftInit && (Math.abs(p.getX() - (shaftX + 0.5)) > 1.2 || Math.abs(p.getZ() - (shaftZ + 0.5)) > 1.2)) {
         shaftInit = false;
      }

      if (!shaftInit) {
         if (!p.onGround()) {
            return true;
         }

         shaftX = me.getX();
         shaftZ = me.getZ();
         shaftTop = me.getY();
         shaftInit = true;
      }

      if (!p.onGround()) {
         return true;
      } else if (shaftTop - me.getY() >= 40) {
         shaftBlocked = true;
         return false;
      } else {
         BlockPos below = new BlockPos(shaftX, me.getY() - 1, shaftZ);
         if (lv.getBlockState(below).is(Blocks.SAND) && !ignoreBlocks.contains(below.asLong()) && !waterWithin(lv, below, 2)) {
            boolean landing = false;

            for (int k = 1; k <= 3 && !landing; k++) {
               if (!lv.getBlockState(below.below(k)).isAir()) {
                  landing = true;
               }
            }

            if (landing) {
               target = below;
               path = null;
               ph = 2;
               phTicks = 0;
               return true;
            }
         }

         Vec3 eye = p.getEyePosition();

         for (int y = me.getY() + 1; y >= me.getY(); y--) {
            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  BlockPos c = new BlockPos(shaftX + dx, y, shaftZ + dz);
                  if (lv.getBlockState(c).is(Blocks.SAND) && !ignoreBlocks.contains(c.asLong())) {
                     if (waterWithin(lv, c, 2)) {
                        ignoreBlocks.add(c.asLong());
                     } else if (canMineFromEye(lv, p, eye, c)) {
                        target = c;
                        path = null;
                        ph = 2;
                        phTicks = 0;
                        return true;
                     }
                  }
               }
            }
         }

         shaftBlocked = true;
         return false;
      }
   }

   static int sandInReach(ClientLevel lv, LocalPlayer p, int r) {
      BlockPos o = BlockPos.containing(p.getX(), p.getEyeY(), p.getZ());
      MutableBlockPos m = new MutableBlockPos();
      int n = 0;

      for (int dx = -r; dx <= r; dx++) {
         for (int dy = -r; dy <= r; dy++) {
            for (int dz = -r; dz <= r; dz++) {
               if (dx * dx + dy * dy + dz * dz <= r * r) {
                  m.set(o.getX() + dx, o.getY() + dy, o.getZ() + dz);
                  if (lv.getBlockState(m).is(Blocks.SAND)) {
                     n++;
                  }
               }
            }
         }
      }

      return n;
   }

   static boolean nukerHold(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      if (!nukerHold || !AutoSandOrders.useNuker || AutoSandOrders.nukerWater || !Boolean.TRUE.equals(AutoSandOrders.nukerState)) {
         nukerHoldTicks = 0;
         nukerHoldLast = -1;
         return false;
      } else if (nukerHoldCool > 0) {
         nukerHoldCool--;
         return false;
      } else if (p.onGround() && !p.isInWater() && !p.isInLava()) {
         int n = sandInReach(lv, p, 4);
         if (n == 0) {
            if (nukerHoldHad) {
               nukerHoldHad = false;
               nukerHoldTicks = 0;
               nukerHoldLast = -1;
               if (descendDepth > 0) {
                  descendPhase = true;
                  descendStartY = Integer.MIN_VALUE;
                  descendWaitTicks = 0;
                  return true;
               }
            }

            return false;
         } else {
            nukerHoldHad = true;
            if (nukerHoldLast >= 0 && n >= nukerHoldLast) {
               if (++nukerHoldTicks > 60) {
                  nukerHoldTicks = 0;
                  nukerHoldLast = -1;
                  nukerHoldCool = 200;
                  nukerHoldHad = false;
                  return false;
               }
            } else {
               nukerHoldLast = n;
               nukerHoldTicks = 0;
            }

            nukerHoldAt = lv.getGameTime();
            release(mc);
            return true;
         }
      } else {
         return false;
      }
   }

   static boolean planDescendSand(LocalPlayer p, ClientLevel lv) {
      BlockPos feet = p.blockPosition();
      BlockPos below = feet.below();
      if (!p.isInWater() && !p.isInLava() && !AutoSandOrders.liquidAt(lv, below) && !AutoSandOrders.lavaAt(lv, below)) {
         if (descendStartY == Integer.MIN_VALUE) {
            descendStartY = feet.getY();
         }

         if (descendDepth > 0 && p.onGround() && descendStartY - feet.getY() >= descendDepth) {
            descendPhase = false;
            descendTarget = false;
            descendWaitTicks = 0;
            return false;
         } else if (lv.getBlockState(below).is(Blocks.SAND)
            && !ignoreBlocks.contains(below.asLong())
            && !waterWithin(lv, below, 1)
            && canMineFromEye(lv, p, p.getEyePosition(), below)) {
            descendWaitTicks = 0;
            target = below;
            descendTarget = true;
            path = null;
            ph = 2;
            phTicks = 0;
            return true;
         } else if (AutoSandOrders.passable(lv, below) && descendWaitTicks < 40) {
            descendWaitTicks++;
            descendTarget = false;
            target = null;
            path = null;
            ph = 0;
            phTicks = 0;
            release(Minecraft.getInstance());
            return true;
         } else {
            descendPhase = false;
            descendTarget = false;
            descendWaitTicks = 0;
            return false;
         }
      } else {
         descendPhase = false;
         descendTarget = false;
         descendWaitTicks = 0;
         return false;
      }
   }

   static BlockPos findBestNearSand(ClientLevel lv, LocalPlayer p) {
      BlockPos origin = p.blockPosition();
      List<BlockPos> list = new ArrayList<>();

      for (int dx = -5; dx <= 5; dx++) {
         for (int dz = -5; dz <= 5; dz++) {
            for (int dy = -3; dy <= 4; dy++) {
               BlockPos bp = origin.offset(dx, dy, dz);
               if (lv.getBlockState(bp).is(Blocks.SAND) && !ignoreBlocks.contains(bp.asLong()) && !underOrOver(p, bp) && !waterWithin(lv, bp, 2)) {
                  boolean exposed = false;

                  for (Direction d : Direction.values()) {
                     BlockPos nb = bp.relative(d);
                     if (lv.getBlockState(nb).getCollisionShape(lv, nb).isEmpty()) {
                        exposed = true;
                        break;
                     }
                  }

                  if (exposed) {
                     list.add(bp);
                  }
               }
            }
         }
      }

      list.sort(Comparator.comparingDouble(cx -> scoreNear(p, cx)));
      Vec3 eye = p.getEyePosition();

      for (BlockPos c : list) {
         if (canMineFromEye(lv, p, eye, c)) {
            return c;
         }
      }

      return list.isEmpty() ? null : list.get(0);
   }

   static double scoreNear(LocalPlayer p, BlockPos b) {
      double dx = b.getX() + 0.5 - p.getX();
      double dz = b.getZ() + 0.5 - p.getZ();
      double dist = Math.hypot(dx, dz);
      int dy = b.getY() - p.blockPosition().getY();
      if (dy < 0 && dist < 1.6) {
         return dist - 15.0;
      } else {
         return Math.abs(dy) <= 1 && dist < 2.8 ? dist - 6.0 : dist + Math.abs(dy) * 0.5;
      }
   }

   static boolean planToSand(LocalPlayer p, ClientLevel lv) {
      BlockPos c = findBestNearSand(lv, p);
      if (c == null) {
         return false;
      } else {
         target = c;
         descendTarget = false;
         path = null;
         pi = 0;
         phTicks = 0;
         if (canMineFromEye(lv, p, p.getEyePosition(), c)) {
            ph = 2;
            return true;
         } else if (startBaritoneToSand(c)) {
            ph = 1;
            return true;
         } else {
            List<BlockPos> r = pathToSand(lv, p, c);
            if (r != null && !r.isEmpty()) {
               path = r;
               pi = 0;
               stuck = 0;
               ph = 1;
               return true;
            } else {
               ignoreBlocks.add(c.asLong());
               target = null;
               ph = 0;
               scanCool = 2;
               return false;
            }
         }
      }
   }

   static List<BlockPos> pathToSand(ClientLevel lv, LocalPlayer p, BlockPos sand) {
      return findPath(lv, p.blockPosition(), Vec3.atCenterOf(sand), q -> canMineFromStand(lv, p, q, sand), 2500, false);
   }

   static boolean canMineFromStand(ClientLevel lv, LocalPlayer p, BlockPos stand, BlockPos sand) {
      if (Math.abs(stand.getX() - sand.getX()) + Math.abs(stand.getY() - sand.getY()) + Math.abs(stand.getZ() - sand.getZ()) > 1) {
         return false;
      } else {
         Vec3 eye = new Vec3(stand.getX() + 0.5, stand.getY() + 1.62, stand.getZ() + 0.5);
         Vec3 center = Vec3.atCenterOf(sand);
         if (eye.distanceToSqr(center) > 18.49) {
            return false;
         } else {
            BlockHitResult hit = lv.clip(new ClipContext(eye, center, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, p));
            return hit.getType() == Type.BLOCK && hit.getBlockPos().equals(sand);
         }
      }
   }

   static boolean startBaritoneToSand(BlockPos sand) {
      baritoneOwnsPath = BaritoneBridge.goTo(sand);
      return baritoneOwnsPath;
   }

   static void restoreBaritoneSettings() {
      BaritoneBridge.restore();
   }

   static void cancelBaritoneNavigation() {
      if (baritoneOwnsPath) {
         BaritoneBridge.cancel();
      }

      baritoneOwnsPath = false;
   }

   static int tickBaritoneTravel(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      if (target == null || !lv.getBlockState(target).is(Blocks.SAND) || waterWithin(lv, target, 2)) {
         cancelBaritoneNavigation();
         target = null;
         ph = 0;
         phTicks = 0;
         scanCool = 2;
         return 2;
      } else if (BaritoneBridge.isBusy()) {
         return 0;
      } else if (canMineFromEye(lv, p, p.getEyePosition(), target)) {
         baritoneOwnsPath = false;
         restoreBaritoneSettings();
         return 1;
      } else if (phTicks < 12) {
         return 0;
      } else {
         baritoneOwnsPath = false;
         restoreBaritoneSettings();
         List<BlockPos> r = pathToSand(lv, p, target);
         if (r != null && !r.isEmpty()) {
            path = r;
            pi = 0;
            stuck = 0;
            return 0;
         } else {
            return 2;
         }
      }
   }

   static boolean planExplore(ClientLevel lv, LocalPlayer p) {
      BlockPos start = p.blockPosition();
      double[] offs = new double[]{0.0, 90.0, -90.0, 180.0, 45.0, -45.0};

      for (double off : offs) {
         double ang = Math.toRadians(exploreYaw + off);
         double tx = p.getX() - Math.sin(ang) * 26.0;
         double tz = p.getZ() + Math.cos(ang) * 26.0;
         Vec3 t = new Vec3(tx, p.getY(), tz);
         List<BlockPos> r = findPath(lv, start, t, q -> Math.hypot(q.getX() + 0.5 - tx, q.getZ() + 0.5 - tz) < 3.0, 2500, true);
         if (r != null && r.size() >= 4) {
            exploreYaw += off;
            path = r;
            pi = 0;
            stuck = 0;
            ph = 4;
            phTicks = 0;
            return true;
         }
      }

      exploreYaw = Math.random() * 360.0;
      return false;
   }

   static boolean waterWithin(ClientLevel lv, BlockPos b, int r) {
      if (lv == null) {
         return false;
      } else {
         MutableBlockPos m = new MutableBlockPos();

         for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
               for (int dy = -r; dy <= r + 1; dy++) {
                  m.set(b.getX() + dx, b.getY() + dy, b.getZ() + dz);
                  if (!lv.getBlockState(m).getFluidState().isEmpty()) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   static boolean fluidAdj(ClientLevel lv, BlockPos b) {
      for (Direction d : AutoSandOrders.HORIZ) {
         BlockPos n = b.relative(d);
         if (!lv.getBlockState(n).getFluidState().isEmpty() || !lv.getBlockState(n.above()).getFluidState().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   static boolean waterRetreat(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      if (retreatCool > 0) {
         retreatCool--;
      }

      if (retreatTicks > 0) {
         retreatTicks--;
         mc.options.keyUp.setDown(true);
         mc.options.keySprint.setDown(true);
         mc.options.keyJump.setDown(false);
         mc.options.keyAttack.setDown(false);
         if (retreatTicks == 0) {
            release(mc);
            retreatCool = 60;
            scanCool = 0;
         }

         return true;
      } else if (retreatCool <= 0 && !p.isInWater() && !p.isInLava()) {
         BlockPos o = p.blockPosition();
         double sx = 0.0;
         double sz = 0.0;
         int n = 0;

         for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
               for (int dy = -1; dy <= 2; dy++) {
                  if (!lv.getBlockState(o.offset(dx, dy, dz)).getFluidState().isEmpty()) {
                     sx += dx;
                     sz += dz;
                     n++;
                  }
               }
            }
         }

         if (n == 0) {
            return false;
         } else {
            double ax = -sx / n;
            double az = -sz / n;
            double len = Math.hypot(ax, az);
            if (len < 0.3) {
               return false;
            } else {
               ax /= len;
               az /= len;
               if (AutoSandOrders.hazardDir(lv, p.getX(), p.getY(), p.getZ(), ax, az)) {
                  return false;
               } else {
                  cancelBaritoneNavigation();
                  path = null;
                  target = null;
                  itemTarget = null;
                  ph = 0;
                  phTicks = 0;
                  release(mc);
                  p.setYRot((float)Math.toDegrees(Math.atan2(-ax, az)));
                  p.setXRot(0.0F);
                  retreatTicks = 10;
                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   static List<BlockPos> scanSand(ClientLevel lv, LocalPlayer p) {
      BlockPos o = p.blockPosition();
      int r = scanRadius;
      List<AutoMiner.Cand> list = new ArrayList<>();
      MutableBlockPos m = new MutableBlockPos();

      for (int dy = -6; dy <= 5; dy++) {
         int y = o.getY() + dy;
         if (y >= baseY - 6) {
            for (int dx = -r; dx <= r; dx++) {
               for (int dz = -r; dz <= r; dz++) {
                  m.set(o.getX() + dx, y, o.getZ() + dz);
                  if (lv.getBlockState(m).is(Blocks.SAND) && !ignoreBlocks.contains(m.asLong()) && !underOrOver(p, m)) {
                     boolean open = false;
                     boolean bad = false;

                     for (Direction d : Direction.values()) {
                        BlockPos nb = m.relative(d);
                        BlockState ns = lv.getBlockState(nb);
                        if (!ns.getFluidState().isEmpty()) {
                           bad = true;
                           break;
                        }

                        if (ns.getCollisionShape(lv, nb).isEmpty()) {
                           open = true;
                        }
                     }

                     if (!bad && open) {
                        list.add(new AutoMiner.Cand(m.immutable(), dx * dx + dz * dz + 3.0 * dy * dy));
                     }
                  }
               }
            }
         }
      }

      list.sort(Comparator.comparingDouble(cx -> cx.score));
      List<BlockPos> out = new ArrayList<>();

      for (int i = 0; i < list.size() && out.size() < 12; i++) {
         BlockPos c = list.get(i).pos;
         if (!waterWithin(lv, c, 2)) {
            out.add(c);
         }
      }

      return out;
   }

   static boolean underOrOver(LocalPlayer p, BlockPos b) {
      if (b.getX() + 1 <= p.getX() - 0.35 || b.getX() >= p.getX() + 0.35) {
         return false;
      } else if (!(b.getZ() + 1 <= p.getZ() - 0.35) && !(b.getZ() >= p.getZ() + 0.35)) {
         int fy = p.blockPosition().getY();
         return b.getY() == fy - 1 || b.getY() >= fy + 2;
      } else {
         return false;
      }
   }

   static boolean colBlocked(BlockPos stand, BlockPos t) {
      return t.getX() == stand.getX() && t.getZ() == stand.getZ() && (t.getY() == stand.getY() - 1 || t.getY() >= stand.getY() + 2);
   }

   static boolean canMineFromEye(ClientLevel lv, LocalPlayer p, Vec3 eye, BlockPos t) {
      Vec3 c = Vec3.atCenterOf(t);
      if (eye.distanceToSqr(c) > 18.49) {
         return false;
      } else {
         BlockHitResult r = lv.clip(new ClipContext(eye, c, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, p));
         return r.getType() == Type.BLOCK && r.getBlockPos().equals(t);
      }
   }

   static ItemEntity findDrop(ClientLevel lv, LocalPlayer p, Vec3 c, double r, boolean sandOnly) {
      AABB box = new AABB(c.x - r, c.y - 5.0, c.z - r, c.x + r, c.y + 5.0, c.z + r);
      List<ItemEntity> list = lv.getEntitiesOfClass(
         ItemEntity.class, box, ex -> !ex.isRemoved() && (!sandOnly || ex.getItem().is(Items.SAND)) && !ignoreItems.contains(ex.getId())
      );
      ItemEntity best = null;
      double bd = Double.MAX_VALUE;

      for (ItemEntity e : list) {
         double dx = e.getX() - p.getX();
         double dz = e.getZ() - p.getZ();
         double d = dx * dx + dz * dz;
         if ((!sandOnly || !(d < 1.44) || !(Math.abs(e.getY() - p.getY()) < 1.5)) && d < bd) {
            bd = d;
            best = e;
         }
      }

      return best;
   }

   static void tickTravel(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      if (phTicks > 500) {
         failTravel(mc);
      } else {
         if (ph == 1 && baritoneOwnsPath) {
            int br = tickBaritoneTravel(mc, p, lv);
            if (br != 0) {
               if (br == 2) {
                  failTravel(mc);
                  return;
               }

               release(mc);
               path = null;
               ph = 2;
               phTicks = 0;
               return;
            }

            if (baritoneOwnsPath) {
               return;
            }
         }

         if (ph == 3) {
            if (itemTarget == null || itemTarget.isRemoved() || AutoSandOrders.isFull(p)) {
               endTravel(mc);
               return;
            }
         } else if (ph == 1 && (target == null || !lv.getBlockState(target).is(Blocks.SAND))) {
            endTravel(mc);
            return;
         }

         int r = followPath(mc, p, lv);
         if (r != 0) {
            release(mc);
            if (r == 2) {
               failTravel(mc);
            } else if (ph == 1) {
               path = null;
               ph = 2;
               phTicks = 0;
            } else {
               if (ph == 3 && itemTarget != null && !itemTarget.isRemoved()) {
                  ignoreItems.add(itemTarget.getId());
               }

               endTravel(mc);
            }
         }
      }
   }

   static void failTravel(Minecraft mc) {
      cancelBaritoneNavigation();
      if (tunnelMove) {
         tunnelFails++;
      }

      if (ph == 1 && target != null) {
         ignoreBlocks.add(target.asLong());
      }

      if (ph == 3 && itemTarget != null) {
         ignoreItems.add(itemTarget.getId());
      }

      endTravel(mc);
   }

   static void endTravel(Minecraft mc) {
      cancelBaritoneNavigation();
      tunnelMove = false;
      release(mc);
      path = null;
      itemTarget = null;
      target = null;
      ph = 0;
      phTicks = 0;
      scanCool = 2;
   }

   static void tickMine(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      release(mc);
      BlockPos t = target;
      if (t != null && lv.getBlockState(t).is(Blocks.SAND)) {
         boolean shaftCell = shaft && t.getX() == shaftX && t.getZ() == shaftZ;
         if (phTicks <= 160 && (shaftCell || descendTarget || !underOrOver(p, t))) {
            if (!canMineFromEye(lv, p, p.getEyePosition(), t)) {
               if (t.asLong() == failKey) {
                  failCount++;
               } else {
                  failKey = t.asLong();
                  failCount = 1;
               }

               if (failCount >= 3) {
                  ignoreBlocks.add(t.asLong());
                  failCount = 0;
               }

               target = null;
               descendTarget = false;
               ph = 0;
               phTicks = 0;
               scanCool = 2;
            } else {
               digBlock(mc, p, t);
            }
         } else {
            ignoreBlocks.add(t.asLong());
            target = null;
            ph = 0;
            phTicks = 0;
            scanCool = 2;
         }
      } else {
         target = null;
         descendTarget = false;
         ph = 0;
         phTicks = 0;
         scanCool = 0;
      }
   }

   static void digBlock(Minecraft mc, LocalPlayer p, BlockPos b) {
      Vec3 c = Vec3.atCenterOf(b);
      aimAt(p, c);
      ensureShovel(p);
      Vec3 e = p.getEyePosition();
      Direction face = Direction.getApproximateNearest(e.x - c.x, e.y - c.y, e.z - c.z);
      digAcc = Math.min(10000, digAcc + AutoSandOrders.mineSpeed);
      int calls = 0;

      while (digAcc >= 100 && calls < 5) {
         digAcc -= 100;
         mc.gameMode.continueDestroyBlock(b, face);
         p.swing(InteractionHand.MAIN_HAND);
         calls++;
         if (mc.level.getBlockState(b).isAir()) {
            digAcc = 0;
            break;
         }
      }
   }

   static void aimAt(LocalPlayer p, Vec3 t) {
      Vec3 e = p.getEyePosition();
      double dx = t.x - e.x;
      double dy = t.y - e.y;
      double dz = t.z - e.z;
      p.setYRot((float)Math.toDegrees(Math.atan2(-dx, dz)));
      p.setXRot((float)(-Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)))));
   }

   static void ensureShovel(LocalPlayer p) {
      if (!p.getInventory().getItem(AutoSandOrders.getSelected(p)).is(ItemTags.SHOVELS)) {
         int hs = hotbarShovel(p);
         if (hs >= 0) {
            AutoSandOrders.setSelected(p, hs);
         }
      }
   }

   static int followPath(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      if (path == null) {
         return 1;
      } else {
         Vec3 pos;
         for (pos = p.position(); pi < path.size(); stuck = 0) {
            BlockPos q = path.get(pi);
            double qx = q.getX() + 0.5 - pos.x;
            double qz = q.getZ() + 0.5 - pos.z;
            double qy = pos.y - q.getY();
            if (!(Math.hypot(qx, qz) < 0.3) || !(qy < 1.2) || !(qy > -1.2)) {
               break;
            }

            pi++;
         }

         if (pi >= path.size()) {
            release(mc);
            return 1;
         } else {
            BlockPos n = path.get(pi);
            double dx = n.getX() + 0.5 - pos.x;
            double dz = n.getZ() + 0.5 - pos.z;
            double dist = Math.hypot(dx, dz);
            if (!(dist > 6.0) && !(Math.abs(pos.y - n.getY()) > 4.0)) {
               BlockPos from = p.blockPosition();
               BlockPos blocker = null;
               if (!AutoSandOrders.passable(lv, n)) {
                  blocker = n;
               } else if (!AutoSandOrders.passable(lv, n.above())) {
                  blocker = n.above();
               } else if (n.getY() > from.getY() && !AutoSandOrders.passable(lv, from.above(2))) {
                  blocker = from.above(2);
               }

               if (blocker != null) {
                  Block bk = lv.getBlockState(blocker).getBlock();
                  if (!canDig || !DIGGABLE.contains(bk)) {
                     return 2;
                  } else if (p.getEyePosition().distanceToSqr(Vec3.atCenterOf(blocker)) > 18.49) {
                     return 2;
                  } else {
                     release(mc);
                     digBlock(mc, p, blocker);
                     stuck = 0;
                     return 0;
                  }
               } else {
                  if (dist > 0.12) {
                     p.setYRot((float)Math.toDegrees(Math.atan2(-dx, dz)));
                  }

                  p.setXRot(0.0F);
                  boolean dropAhead = n.getY() < pos.y - 1.0;
                  mc.options.keyUp.setDown(!dropAhead || !(dist < 0.5));
                  boolean jump = n.getY() > pos.y + 0.3 && dist < 1.7 || p.horizontalCollision && p.onGround();
                  mc.options.keyJump.setDown(jump);
                  mc.options.keySprint.setDown(dist > 0.1 && !dropAhead);
                  if (lastPos != null && pos.distanceToSqr(lastPos) < 0.0016) {
                     if (++stuck > 40) {
                        return 2;
                     }
                  } else {
                     stuck = 0;
                  }

                  lastPos = pos;
                  if (pgPath != path) {
                     pgPath = path;
                     pgPos = pos;
                     pgTicks = 0;
                     pgFails = 0;
                     pgHit = 0;
                  }

                  if (p.horizontalCollision) {
                     pgHit++;
                  }

                  if (++pgTicks >= 20) {
                     double mv2 = pos.distanceToSqr(pgPos);
                     if (mv2 < 0.0625) {
                        pgFails++;
                     } else {
                        pgFails = 0;
                     }

                     boolean rammed = pgHit >= 15 && mv2 < 0.36;
                     pgPos = pos;
                     pgTicks = 0;
                     pgHit = 0;
                     if (pgFails >= 2 || rammed) {
                        pgFails = 0;
                        return 2;
                     }
                  }

                  return 0;
               }
            } else {
               return 2;
            }
         }
      }
   }

   static boolean hazardState(BlockState s) {
      return s.is(Blocks.CACTUS)
         || s.is(Blocks.FIRE)
         || s.is(Blocks.SOUL_FIRE)
         || s.is(Blocks.MAGMA_BLOCK)
         || s.is(Blocks.SWEET_BERRY_BUSH)
         || s.is(Blocks.WITHER_ROSE)
         || s.is(Blocks.POWDER_SNOW)
         || s.is(Blocks.COBWEB)
         || s.is(Blocks.CAMPFIRE)
         || s.is(Blocks.SOUL_CAMPFIRE)
         || s.getFluidState().is(FluidTags.LAVA);
   }

   static double cellCost(ClientLevel lv, BlockPos c) {
      BlockState s = lv.getBlockState(c);
      if (hazardState(s) || !s.getFluidState().isEmpty()) {
         return -1.0;
      } else if (s.getCollisionShape(lv, c).isEmpty()) {
         return 0.0;
      } else {
         return canDig && DIGGABLE.contains(s.getBlock()) ? 5.0 : -1.0;
      }
   }

   static boolean solidGround(ClientLevel lv, BlockPos b) {
      BlockState s = lv.getBlockState(b);
      if (!hazardState(s) && s.getFluidState().isEmpty()) {
         VoxelShape sh = s.getCollisionShape(lv, b);
         return !sh.isEmpty() && sh.max(Axis.Y) <= 1.0001;
      } else {
         return false;
      }
   }

   static boolean lavaNear(ClientLevel lv, BlockPos f) {
      if (AutoSandOrders.lavaAt(lv, f.below())) {
         return true;
      } else {
         for (Direction d : AutoSandOrders.HORIZ) {
            BlockPos n = f.relative(d);
            if (AutoSandOrders.lavaAt(lv, n) || AutoSandOrders.lavaAt(lv, n.above())) {
               return true;
            }
         }

         return false;
      }
   }

   static void expand(ClientLevel lv, BlockPos p, List<AutoMiner.Move> out) {
      BlockPos head2 = p.above(2);

      for (Direction d : AutoSandOrders.HORIZ) {
         BlockPos n = p.relative(d);
         double c1 = cellCost(lv, n);
         double c2 = cellCost(lv, n.above());
         if (c1 >= 0.0 && c2 >= 0.0) {
            if (!solidGround(lv, n.below())) {
               if (c1 == 0.0 && c2 == 0.0) {
                  int fallMax = AutoSandOrders.avoidHole ? 2 : 3;

                  for (int k = 1; k <= fallMax; k++) {
                     BlockPos m = n.below(k);
                     if (cellCost(lv, m) != 0.0) {
                        break;
                     }

                     if (solidGround(lv, m.below())) {
                        if (!lavaNear(lv, m)) {
                           out.add(new AutoMiner.Move(m, 1.5 + 0.6 * k));
                        }
                        break;
                     }
                  }
               }
            } else {
               boolean cave = AutoSandOrders.avoidHole && AutoSandOrders.caveStep(lv, p, n, c1 == 0.0 && c2 == 0.0);
               if (!lavaNear(lv, n) && !cave) {
                  out.add(new AutoMiner.Move(n, 1.0 + c1 + c2));
               }
            }
         }

         if (solidGround(lv, n)) {
            BlockPos up = n.above();
            double a = cellCost(lv, head2);
            double b = cellCost(lv, up);
            double c = cellCost(lv, up.above());
            boolean cave = AutoSandOrders.avoidHole && AutoSandOrders.caveStep(lv, p, up, a == 0.0 && b == 0.0 && c == 0.0);
            if (a >= 0.0 && b >= 0.0 && c >= 0.0 && !lavaNear(lv, up) && !cave) {
               out.add(new AutoMiner.Move(up, 1.8 + a + b + c));
            }
         }
      }

      for (int i = 0; i < 4; i++) {
         Direction a = AutoSandOrders.HORIZ[i];
         Direction b = AutoSandOrders.HORIZ[(i + 1) % 4];
         BlockPos pa = p.relative(a);
         BlockPos pb = p.relative(b);
         BlockPos dn = pa.relative(b);
         if (cellCost(lv, pa) == 0.0
            && cellCost(lv, pa.above()) == 0.0
            && cellCost(lv, pb) == 0.0
            && cellCost(lv, pb.above()) == 0.0
            && cellCost(lv, dn) == 0.0
            && cellCost(lv, dn.above()) == 0.0
            && solidGround(lv, pa.below())
            && solidGround(lv, pb.below())
            && solidGround(lv, dn.below())
            && !lavaNear(lv, dn)) {
            out.add(new AutoMiner.Move(dn, 1.42));
         }
      }
   }

   static double heur(BlockPos n, Vec3 t) {
      double dx = n.getX() + 0.5 - t.x;
      double dz = n.getZ() + 0.5 - t.z;
      double dy = n.getY() - t.y;
      return Math.sqrt(dx * dx + dz * dz) + Math.abs(dy) * 0.7;
   }

   static List<BlockPos> findPath(ClientLevel lv, BlockPos start, Vec3 target, Predicate<BlockPos> goal, int maxNodes, boolean partial) {
      PriorityQueue<AutoMiner.Nd> open = new PriorityQueue<>(Comparator.comparingDouble(x -> x.f));
      Map<Long, Double> bestG = new HashMap<>();
      Set<Long> closed = new HashSet<>();
      AutoMiner.Nd s = new AutoMiner.Nd(start, null, 0.0, heur(start, target) * 1.3);
      open.add(s);
      bestG.put(start.asLong(), 0.0);
      AutoMiner.Nd best = s;
      double bestH = heur(start, target);
      int expanded = 0;
      List<AutoMiner.Move> mv = new ArrayList<>();

      while (!open.isEmpty() && expanded < maxNodes) {
         AutoMiner.Nd cur = open.poll();
         if (closed.add(cur.pos.asLong())) {
            expanded++;
            if (goal.test(cur.pos)) {
               return build(cur);
            }

            double h = heur(cur.pos, target);
            if (h < bestH) {
               bestH = h;
               best = cur;
            }

            mv.clear();
            expand(lv, cur.pos, mv);

            for (AutoMiner.Move m : mv) {
               BlockPos np = m.pos;
               if (Math.abs(np.getX() - start.getX()) <= 48 && Math.abs(np.getZ() - start.getZ()) <= 48) {
                  long k = np.asLong();
                  if (!closed.contains(k)) {
                     double ng = cur.g + m.cost + (fluidAdj(lv, np) ? 12.0 : 0.0);
                     Double old = bestG.get(k);
                     if (old == null || !(old <= ng)) {
                        bestG.put(k, ng);
                        open.add(new AutoMiner.Nd(np, cur, ng, ng + heur(np, target) * 1.3));
                     }
                  }
               }
            }
         }
      }

      return partial && best != s ? build(best) : null;
   }

   static List<BlockPos> build(AutoMiner.Nd end) {
      ArrayList<BlockPos> r = new ArrayList<>();

      for (AutoMiner.Nd n = end; n.parent != null; n = n.parent) {
         r.add(n.pos);
      }

      Collections.reverse(r);
      return r;
   }

   static Vec3 deathCenter() {
      return new Vec3(deathPos.getX() + 0.5, deathPos.getY(), deathPos.getZ() + 0.5);
   }

   static boolean pearlHpLow(Minecraft mc, LocalPlayer p, String what) {
      if (p.getHealth() >= 12.0F) {
         pearlHpWait = 0;
         hpEatTries = 0;
         hpEatCool = 0;
         return false;
      } else {
         if (++pearlHpWait == 1) {
            AutoSandOrders.msg(p, "Máu thấp (" + (int)p.getHealth() + "), chờ hồi máu rồi mới ném ngọc" + what + ".");
         }

         if (hpEatCool > 0) {
            hpEatCool--;
         } else if (hpEatTries < 3) {
            hpEatCool = 100;
            if (AutoSandOrders.healBeforePearl(mc, p)) {
               hpEatTries++;
            }
         }

         return true;
      }
   }

   static void tickRecovery(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      if (deathPos != null && (rec == 5 || ++recTicks <= 9000)) {
         if (hasAnyShovel(p)) {
            gotShovel = true;
         }

         if (rec != 5 && rec != 1) {
            fixShovel(mc, p);
         }

         Vec3 dc = deathCenter();
         switch (rec) {
            case 1:
               release(mc);
               if (--recTimer > 0) {
                  return;
               }

               if (kitDrop(mc, p)) {
                  recTimer = 3;
                  return;
               }

               if (homeCmd != null && !homeCmd.isBlank()) {
                  homeFrom = p.position();
                  teleported = false;
                  AutoSandOrders.send(p, "/" + homeCmd);
                  homeTries++;
                  rec = 2;
                  recTimer = 200;
                  break;
               }

               rec = deathRecover ? 3 : 2;
               recTimer = deathRecover ? 0 : 200;
               path = null;
               return;
            case 2:
               release(mc);
               if (!teleported && homeFrom != null && p.position().distanceToSqr(homeFrom) > 16.0) {
                  teleported = true;
                  AutoSandOrders.msg(p, "Đã về home, đứng yên cho đủ 10 giây.");
               }

               if (--recTimer > 0) {
                  return;
               }

               double far = Math.hypot(p.getX() - dc.x, p.getZ() - dc.z);
               if (!teleported && homeTries < 2 && far > 80.0 && homeCmd != null && !homeCmd.isBlank()) {
                  homeFrom = p.position();
                  AutoSandOrders.send(p, "/" + homeCmd);
                  homeTries++;
                  recTimer = 200;
                  return;
               }

               if (!deathRecover) {
                  endRecovery(mc, p, "đã về home (tắt nhặt đồ)");
                  return;
               }

               AutoSandOrders.msg(p, "Đi tới chỗ chết.");
               rec = 3;
               recTimer = 20;
               path = null;
               break;
            case 3:
               if (recTimer > 0) {
                  recTimer--;
                  release(mc);
                  return;
               }

               if (deathDim != null && !deathDim.equals(lv.dimension())) {
                  endRecovery(mc, p, "đang ở chiều không gian khác với chỗ chết");
                  return;
               }

               double hd = Math.hypot(p.getX() - dc.x, p.getZ() - dc.z);
               if (hd <= 3.0 && Math.abs(p.getY() - dc.y) <= 4.0) {
                  release(mc);
                  rec = 4;
                  collectIdle = 0;
                  collectTicks = 0;
                  itemWait = 0;
                  itemTarget = null;
                  path = null;
                  AutoSandOrders.msg(p, "Tới chỗ chết, bắt đầu nhặt đồ.");
                  return;
               }

               if (AutoSandOrders.usePearl && !pearlDone && hd > 60.0) {
                  rec = 6;
                  pearlThrows = 0;
                  pearlStep = 0;
                  pearlTimer = 0;
                  pearlHpWait = 0;
                  hpEatTries = 0;
                  hpEatCool = 0;
                  return;
               }

               if (recTicks % 100 == 0) {
                  double d = p.position().distanceTo(dc);
                  if (d > walkBest - 2.0) {
                     walkNoProg++;
                  } else {
                     walkNoProg = 0;
                  }

                  walkBest = Math.min(walkBest, d);
                  if (walkNoProg >= 6 || walkFails > 12) {
                     endRecovery(mc, p, "không đi tới được toạ độ chết");
                     return;
                  }
               }

               if (crossCool > 0) {
                  crossCool--;
               }

               if (path == null || pi >= path.size()) {
                  path = null;
                  List<BlockPos> rp = findPath(
                     lv,
                     p.blockPosition(),
                     dc,
                     q -> Math.hypot(q.getX() + 0.5 - dc.x, q.getZ() + 0.5 - dc.z) <= 2.5 && Math.abs(q.getY() - dc.y) <= 3.0,
                     6000,
                     true
                  );
                  boolean noWay = rp == null || rp.isEmpty();
                  boolean detour = !noWay && rp.size() > hd * 2.0 + 20.0;
                  if ((noWay || detour) && tryCross(mc, p, lv, dc)) {
                     return;
                  }

                  if (noWay) {
                     if (++blindTicks > 600) {
                        endRecovery(mc, p, "không tìm được đường tới chỗ chết");
                        return;
                     }

                     blindWalk(mc, p, dc);
                     return;
                  }

                  path = rp;
                  pi = 0;
                  stuck = 0;
               }

               int r = followPath(mc, p, lv);
               if (r == 2) {
                  path = null;
                  stuck = 0;
                  walkFails++;
                  if (walkFails >= 2 && tryCross(mc, p, lv, dc)) {
                     return;
                  }
               } else if (r == 1) {
                  path = null;
               }
               break;
            case 4:
               if (++collectTicks > 2400) {
                  orgWhy = null;
                  toOrganize(mc);
                  return;
               }

               if (itemTarget == null || itemTarget.isRemoved()) {
                  itemTarget = findDrop(lv, p, dc, hasAnyShovel(p) ? 12.0 : 24.0, false);
                  path = null;
                  itemWait = 0;
                  if (itemTarget == null) {
                     release(mc);
                     if (++collectIdle >= (hasAnyShovel(p) ? 60 : 200)) {
                        orgWhy = null;
                        toOrganize(mc);
                     }

                     return;
                  }

                  collectIdle = 0;
               }

               ItemEntity it = itemTarget;
               Vec3 ip = it.position();
               if (path == null) {
                  itemPlanPos = ip;
                  List<BlockPos> rp = findPath(
                     lv,
                     p.blockPosition(),
                     ip,
                     q -> Math.hypot(q.getX() + 0.5 - ip.x, q.getZ() + 0.5 - ip.z) < 0.9 && Math.abs(q.getY() - ip.y) < 2.0,
                     2500,
                     false
                  );
                  if (rp == null) {
                     ignoreItems.add(it.getId());
                     itemTarget = null;
                     return;
                  }

                  path = rp;
                  pi = 0;
                  stuck = 0;
               }

               int r2 = followPath(mc, p, lv);
               if (r2 == 2) {
                  ignoreItems.add(it.getId());
                  itemTarget = null;
                  path = null;
                  release(mc);
               } else if (r2 == 1) {
                  release(mc);
                  if (itemPlanPos != null && ip.distanceToSqr(itemPlanPos) > 2.25) {
                     path = null;
                     itemWait = 0;
                     return;
                  }

                  if (++itemWait > 50) {
                     ignoreItems.add(it.getId());
                     itemTarget = null;
                     path = null;
                  }
               }
               break;
            case 5:
               release(mc);
               if (--recTimer > 0) {
                  return;
               }

               if (recStep++ > 40) {
                  endRecovery(mc, p, orgWhy);
                  return;
               }

               if (organize(mc, p)) {
                  recTimer = 4;
               } else {
                  endRecovery(mc, p, orgWhy);
               }
               break;
            case 6:
               release(mc);
               if (deathDim != null && !deathDim.equals(lv.dimension())) {
                  pearlDone = true;
                  rec = 3;
                  path = null;
                  return;
               }

               double hdx = Math.hypot(p.getX() - dc.x, p.getZ() - dc.z);
               if (hdx <= 12.0 || pearlThrows >= 4) {
                  pearlDone = true;
                  rec = 3;
                  recTimer = 0;
                  path = null;
                  return;
               }

               if (pearlTimer > 0) {
                  pearlTimer--;
                  return;
               }

               if (pearlHpLow(mc, p, "")) {
                  if (pearlHpWait > 600) {
                     AutoSandOrders.msg(p, "Máu vẫn thấp sau 30 giây, đi bộ tới chỗ chết.");
                     pearlDone = true;
                     rec = 3;
                     path = null;
                  }

                  return;
               }

               int pi2x = AutoSandOrders.findItem(p, Items.ENDER_PEARL);
               if (pi2x < 0) {
                  if (pearlBuyFails >= 2) {
                     AutoSandOrders.msg(p, "Không mua được ngọc Ender, đi bộ tới chỗ chết.");
                     pearlDone = true;
                     rec = 3;
                     path = null;
                     return;
                  }

                  int need = (int)Math.min((double)(4 - pearlThrows), Math.ceil(hdx / 45.0));
                  AutoSandOrders.beginPearlBuy(mc, p, Math.max(1, need));
                  pearlTimer = 10;
                  return;
               }

               switch (pearlStep) {
                  case 0:
                     int slot = AutoSandOrders.toHotbar(mc, p, pi2x);
                     AutoSandOrders.setSelected(p, slot);
                     Vec3 eye = p.getEyePosition();
                     double dx = dc.x - eye.x;
                     double dz = dc.z - eye.z;
                     double dxx = Math.hypot(dx, dz);
                     Vec3 aim = dxx <= 45.0 ? new Vec3(dc.x, dc.y, dc.z) : new Vec3(eye.x + dx / dxx * 45.0, p.getY(), eye.z + dz / dxx * 45.0);
                     float[] ang = solvePearl(lv, p, aim);
                     if (ang == null) {
                        AutoSandOrders.msg(p, "Không tính được đường ném ngọc, đi bộ tới chỗ chết.");
                        pearlDone = true;
                        rec = 3;
                        path = null;
                        return;
                     }

                     pearlYaw = ang[0];
                     pearlPitch = ang[1];
                     p.setYRot(pearlYaw);
                     p.setXRot(pearlPitch);
                     pearlStep = 1;
                     pearlTimer = 4;
                     return;
                  case 1:
                     p.setYRot(pearlYaw);
                     p.setXRot(pearlPitch);
                     mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                     p.swing(InteractionHand.MAIN_HAND);
                     pearlThrows++;
                     pearlStep = 0;
                     pearlTimer = 60;
                     return;
                  default:
                     pearlStep = 0;
                     return;
               }
            case 7:
               release(mc);
               if (pearlTimer > 0) {
                  pearlTimer--;
                  return;
               }

               if (crossStep == 0) {
                  if (pearlHpLow(mc, p, " qua nước")) {
                     if (pearlHpWait > 600) {
                        crossCool = 600;
                        rec = 3;
                        path = null;
                     }

                     return;
                  }

                  int pi2 = AutoSandOrders.findItem(p, Items.ENDER_PEARL);
                  if (pi2 < 0) {
                     if (pearlBuyFails >= 2) {
                        AutoSandOrders.msg(p, "Không mua được ngọc Ender để qua nước, thử đi vòng.");
                        crossCool = 1200;
                        rec = 3;
                        path = null;
                        return;
                     }

                     AutoSandOrders.beginPearlBuy(mc, p, 1);
                     pearlTimer = 10;
                     return;
                  }

                  float[] ang = planCross(lv, p, dc);
                  if (ang == null) {
                     AutoSandOrders.msg(p, "Không tìm được chỗ đáp ngọc bên kia nước, thử đi vòng.");
                     crossCool = 400;
                     rec = 3;
                     path = null;
                     return;
                  }

                  AutoSandOrders.setSelected(p, AutoSandOrders.toHotbar(mc, p, pi2));
                  pearlYaw = ang[0];
                  pearlPitch = ang[1];
                  p.setYRot(pearlYaw);
                  p.setXRot(pearlPitch);
                  crossFrom = p.position();
                  crossStep = 1;
                  pearlTimer = 4;
               } else if (crossStep == 1) {
                  p.setYRot(pearlYaw);
                  p.setXRot(pearlPitch);
                  mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                  p.swing(InteractionHand.MAIN_HAND);
                  crossThrows++;
                  crossStep = 2;
                  pearlTimer = 50;
               } else {
                  boolean moved = crossFrom != null && p.position().distanceTo(crossFrom) > 4.0;
                  AutoSandOrders.msg(p, moved ? "Đã qua nước bằng ngọc Ender." : "Ngọc chưa dịch chuyển được, thử lại / đi vòng.");
                  crossStep = 0;
                  crossCool = moved ? 40 : 200;
                  rec = 3;
                  path = null;
                  stuck = 0;
                  walkFails = 0;
                  blindTicks = 0;
                  walkNoProg = 0;
                  walkBest = Double.MAX_VALUE;
               }
               break;
            default:
               rec = 0;
         }
      } else {
         endRecovery(mc, p, "quá thời gian, bỏ qua");
      }
   }

   static void toOrganize(Minecraft mc) {
      orgDone = true;
      release(mc);
      rec = 5;
      recStep = 0;
      recTimer = 5;
      path = null;
      itemTarget = null;
   }

   static void blindWalk(Minecraft mc, LocalPlayer p, Vec3 t) {
      double dx = t.x - p.getX();
      double dz = t.z - p.getZ();
      p.setYRot((float)Math.toDegrees(Math.atan2(-dx, dz)));
      p.setXRot(0.0F);
      mc.options.keyUp.setDown(true);
      mc.options.keyJump.setDown(p.horizontalCollision && p.onGround());
   }

   static void endRecovery(Minecraft mc, LocalPlayer p, String why) {
      if (!orgDone) {
         orgDone = true;
         orgWhy = why;
         toOrganize(mc);
      } else {
         orgDone = false;
         rec = 0;
         deathRecorded = false;
         deathMarker = false;
         gotShovel = false;
         release(mc);
         path = null;
         itemTarget = null;
         target = null;
         ph = 0;
         scanCool = 0;
         baseY = p.blockPosition().getY();
         ignoreItems.clear();
         AutoSandOrders.junkCool = 0;
         if (!hasAnyShovel(p)) {
            AutoSandOrders.msg(p, (why == null ? "Đã nhặt xong đồ" : "Dừng nhặt đồ: " + why) + " nhưng không có xẻng nào trong người. Tắt AutoSand.");
            AutoSandOrders.setEnabled(mc, false);
         } else {
            active = true;
            if (AutoSandOrders.useNuker) {
               AutoSandOrders.setNuker(p, true);
            }

            AutoSandOrders.msg(p, (why == null ? "Đã nhặt xong đồ, xếp lại thanh công cụ." : "Dừng nhặt đồ: " + why + ".") + " Đào tiếp.");
         }
      }
   }

   static int hotbarShovel(LocalPlayer p) {
      for (int i = 0; i < 9; i++) {
         if (p.getInventory().getItem(i).is(ItemTags.SHOVELS)) {
            return i;
         }
      }

      return -1;
   }

   static int bestShovelInBackpack(LocalPlayer p) {
      int best = -1;
      double bp = -1.0;

      for (int i = 9; i < 36; i++) {
         ItemStack st = p.getInventory().getItem(i);
         if (!st.isEmpty() && st.is(ItemTags.SHOVELS)) {
            double pct = AutoSandOrders.remainingPct(st);
            if (pct > bp) {
               bp = pct;
               best = i;
            }
         }
      }

      return best;
   }

   static int prefShovelSlot() {
      for (int i = 0; i < 9; i++) {
         if (snapShovel[i]) {
            return i;
         }
      }

      return 0;
   }

   static int freeHotbar(LocalPlayer p, int pref) {
      if (p.getInventory().getItem(pref).isEmpty()) {
         return pref;
      } else {
         for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).isEmpty()) {
               return i;
            }
         }

         return pref;
      }
   }

   static boolean organize(Minecraft mc, LocalPlayer p) {
      if (p.getOffhandItem().is(ItemTags.SHOVELS)) {
         AutoSandOrders.swapOffhand(mc, p, freeHotbar(p, 0));
         return true;
      } else {
         int[] mv = AutoSandOrders.arrangeMove(p);
         if (mv != null) {
            AutoSandOrders.swapHotbar(mc, p, mv[0], mv[1]);
            return true;
         } else {
            if (hotbarShovel(p) < 0 && !shovelWarned) {
               shovelWarned = true;
               AutoSandOrders.msg(p, "Không tìm thấy xẻng nào để đặt lại thanh công cụ.");
            }

            int hs = hotbarShovel(p);
            if (hs >= 0 && AutoSandOrders.getSelected(p) != hs) {
               AutoSandOrders.setSelected(p, hs);
               return true;
            } else {
               return false;
            }
         }
      }
   }

   static boolean isKit(ItemStack st) {
      return !st.isEmpty() && !st.is(ItemTags.SHOVELS)
         ? st.is(ItemTags.PICKAXES)
            || st.is(ItemTags.SWORDS)
            || st.is(Items.BREAD)
            || st.is(ItemTags.HEAD_ARMOR)
            || st.is(ItemTags.CHEST_ARMOR)
            || st.is(ItemTags.LEG_ARMOR)
            || st.is(ItemTags.FOOT_ARMOR)
         : false;
   }

   static int kitSlot(LocalPlayer p) {
      for (int i = 5; i <= 8; i++) {
         if (!p.inventoryMenu.getSlot(i).getItem().isEmpty()) {
            return i;
         }
      }

      for (int ix = 0; ix < 36; ix++) {
         if (isKit(p.getInventory().getItem(ix))) {
            return AutoSandOrders.menuSlot(ix);
         }
      }

      return isKit(p.getOffhandItem()) ? 45 : -1;
   }

   static boolean kitDrop(Minecraft mc, LocalPlayer p) {
      if (kitStep >= 2) {
         return false;
      } else {
         int slot = kitSlot(p);
         if (kitStep == 0) {
            if (slot < 0) {
               kitStep = 2;
               return false;
            } else {
               kitXRot = p.getXRot();
               p.setXRot(0.0F);
               kitStep = 1;
               return true;
            }
         } else if (slot >= 0 && kitDropped < 40) {
            mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, slot, 1, ClickType.THROW, p);
            kitDropped++;
            return true;
         } else {
            if (!Float.isNaN(kitXRot)) {
               p.setXRot(kitXRot);
               kitXRot = Float.NaN;
            }

            kitStep = 2;
            AutoSandOrders.msg(p, "Đã vứt " + kitDropped + " món (giáp, cúp, kiếm, bánh mì) trước khi về home.");
            return false;
         }
      }
   }

   static void fixShovel(Minecraft mc, LocalPlayer p) {
      if (++fixCool >= 20) {
         fixCool = 0;
         if (hotbarShovel(p) < 0) {
            if (p.getOffhandItem().is(ItemTags.SHOVELS)) {
               AutoSandOrders.swapOffhand(mc, p, freeHotbar(p, 0));
            } else {
               int idx = bestShovelInBackpack(p);
               if (idx >= 9) {
                  AutoSandOrders.swapHotbar(mc, p, idx, freeHotbar(p, 0));
               }
            }
         }
      }
   }

   static boolean hasAnyShovel(LocalPlayer p) {
      if (p.getOffhandItem().is(ItemTags.SHOVELS)) {
         return true;
      } else {
         for (int i = 0; i < 36; i++) {
            if (p.getInventory().getItem(i).is(ItemTags.SHOVELS)) {
               return true;
            }
         }

         return false;
      }
   }

   static void arrowTick(Minecraft mc, LocalPlayer p) {
      if (deathMarker && deathPos != null && !p.isDeadOrDying() && mc.level != null) {
         if (deathDim == null || deathDim.equals(mc.level.dimension())) {
            if (++arrowCnt % 10 == 0) {
               double dx = deathPos.getX() + 0.5 - p.getX();
               double dz = deathPos.getZ() + 0.5 - p.getZ();
               double hd = Math.hypot(dx, dz);
               if (hd < 4.0 && Math.abs(p.getY() - deathPos.getY()) < 8.0) {
                  deathMarker = false;
               } else {
                  double rel = Math.toDegrees(Math.atan2(-dx, dz)) - p.getYRot();
                  rel = ((rel + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
                  int idx = (int)Math.floorMod(Math.round(rel / 45.0), 8L);
                  mc.gui.setOverlayMessage(Component.literal(ARROWS[idx] + "  Chỗ chết: " + lastDeathText + "  -  còn " + (int)hd + " ô"), false);
               }
            }
         }
      }
   }

   static boolean waterAhead(ClientLevel lv, LocalPlayer p, Vec3 dc) {
      double dx = dc.x - p.getX();
      double dz = dc.z - p.getZ();
      double d = Math.hypot(dx, dz);
      if (d < 3.0) {
         return false;
      } else {
         dx /= d;
         dz /= d;
         int py = p.blockPosition().getY();

         for (int k = 1; k <= 12; k++) {
            for (int dy = -2; dy <= 1; dy++) {
               if (AutoSandOrders.liquidAt(lv, BlockPos.containing(p.getX() + dx * k, py + dy, p.getZ() + dz * k))) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   static boolean tryCross(Minecraft mc, LocalPlayer p, ClientLevel lv, Vec3 dc) {
      if (!AutoSandOrders.usePearl || crossThrows >= 4 || crossCool > 0) {
         return false;
      } else if (p.isInWater() || p.isInLava() || !p.onGround()) {
         return false;
      } else if (!waterAhead(lv, p, dc)) {
         return false;
      } else {
         AutoSandOrders.msg(p, "Có nước chắn đường tới chỗ chết -> ném ngọc Ender sang bên kia.");
         rec = 7;
         crossStep = 0;
         pearlTimer = 0;
         pearlHpWait = 0;
         path = null;
         release(mc);
         return true;
      }
   }

   static boolean dryLanding(ClientLevel lv, BlockPos f) {
      if (AutoSandOrders.walkable(lv, f) && !AutoSandOrders.liquidAt(lv, f) && !AutoSandOrders.liquidAt(lv, f.above())) {
         int land = 0;

         for (Direction h : AutoSandOrders.HORIZ) {
            BlockPos n = f.relative(h);
            if (AutoSandOrders.walkable(lv, n) && !AutoSandOrders.liquidAt(lv, n)) {
               land++;
            }
         }

         return land >= 2;
      } else {
         return false;
      }
   }

   static float[] planCross(ClientLevel lv, LocalPlayer p, Vec3 dc) {
      Vec3 eye = p.getEyePosition();
      double base = Math.toDegrees(Math.atan2(-(dc.x - p.getX()), dc.z - p.getZ()));
      int py = p.blockPosition().getY();
      List<BlockPos> cands = new ArrayList<>();

      for (double off : new double[]{0.0, 12.0, -12.0, 25.0, -25.0, 40.0, -40.0}) {
         double r = Math.toRadians(base + off);
         double ux = -Math.sin(r);
         double uz = Math.cos(r);
         boolean sawWater = false;

         for (int k = 2; k <= 30; k++) {
            double x = p.getX() + ux * k;
            double z = p.getZ() + uz * k;
            BlockPos found = null;
            boolean wet = false;

            for (int dy = 2; dy >= -3; dy--) {
               BlockPos f = BlockPos.containing(x, py + dy, z);
               if (AutoSandOrders.liquidAt(lv, f)) {
                  wet = true;
               }

               if (found == null && dryLanding(lv, f)) {
                  found = f;
               }
            }

            if (wet && found == null) {
               sawWater = true;
            }

            if (sawWater && found != null) {
               BlockPos best = found;

               for (int m = 1; m <= 2; m++) {
                  BlockPos g = BlockPos.containing(x + ux * m, found.getY(), z + uz * m);
                  if (dryLanding(lv, g)) {
                     best = g;
                  }
               }

               cands.add(best);
               break;
            }
         }
      }

      cands.sort(Comparator.comparingDouble(b -> Math.hypot(b.getX() + 0.5 - dc.x, b.getZ() + 0.5 - dc.z)));

      for (BlockPos c : cands) {
         Vec3 aim = new Vec3(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
         float[] ang = solvePearl(lv, p, aim);
         if (ang != null) {
            Vec3 h = simPearlHit(lv, p, eye, ang[0], ang[1]);
            if (h != null) {
               BlockPos lc = BlockPos.containing(h.x, h.y + 0.05, h.z);
               if (!(Math.hypot(h.x - aim.x, h.z - aim.z) > 3.0)
                  && AutoSandOrders.passable(lv, lc)
                  && AutoSandOrders.passable(lv, lc.above())
                  && !AutoSandOrders.liquidAt(lv, lc)
                  && !AutoSandOrders.liquidAt(lv, lc.above())) {
                  return ang;
               }
            }
         }
      }

      return null;
   }

   static double[] simPearl(ClientLevel lv, LocalPlayer p, Vec3 eye, float yaw, float pitch) {
      double yr = Math.toRadians(yaw);
      double pr = Math.toRadians(pitch);
      double cp = Math.cos(pr);
      double vx = -Math.sin(yr) * cp * 1.5;
      double vy = -Math.sin(pr) * 1.5;
      double vz = Math.cos(yr) * cp * 1.5;
      Vec3 pos = new Vec3(eye.x, eye.y - 0.1, eye.z);

      for (int t = 0; t < 160; t++) {
         Vec3 next = new Vec3(pos.x + vx, pos.y + vy, pos.z + vz);
         BlockHitResult r = lv.clip(new ClipContext(pos, next, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, p));
         if (r.getType() == Type.BLOCK) {
            Vec3 h = r.getLocation();
            return new double[]{Math.hypot(h.x - eye.x, h.z - eye.z), h.y};
         }

         pos = next;
         vx *= 0.99;
         vy = vy * 0.99 - 0.03;
         vz *= 0.99;
         if (next.y < -70.0) {
            return null;
         }
      }

      return null;
   }

   static Vec3 simPearlHit(ClientLevel lv, LocalPlayer p, Vec3 eye, float yaw, float pitch) {
      double yr = Math.toRadians(yaw);
      double pr = Math.toRadians(pitch);
      double cp = Math.cos(pr);
      double vx = -Math.sin(yr) * cp * 1.5;
      double vy = -Math.sin(pr) * 1.5;
      double vz = Math.cos(yr) * cp * 1.5;
      Vec3 pos = new Vec3(eye.x, eye.y - 0.1, eye.z);

      for (int t = 0; t < 160; t++) {
         Vec3 next = new Vec3(pos.x + vx, pos.y + vy, pos.z + vz);
         BlockHitResult r = lv.clip(new ClipContext(pos, next, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, p));
         if (r.getType() == Type.BLOCK) {
            return r.getLocation();
         }

         pos = next;
         vx *= 0.99;
         vy = vy * 0.99 - 0.03;
         vz *= 0.99;
         if (next.y < -70.0) {
            return null;
         }
      }

      return null;
   }

   static float[] solvePearl(ClientLevel lv, LocalPlayer p, Vec3 target) {
      Vec3 eye = p.getEyePosition();
      double dx = target.x - eye.x;
      double dz = target.z - eye.z;
      double want = Math.hypot(dx, dz);
      float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
      float best = Float.NaN;
      double bestErr = 1.0E9;

      for (float pitch = 10.0F; pitch >= -85.0F; pitch -= 0.5F) {
         double[] r = simPearl(lv, p, eye, yaw, pitch);
         if (r != null) {
            double err = Math.abs(r[0] - want) + Math.abs(r[1] - target.y) * 0.2;
            if (err < bestErr - 0.01) {
               bestErr = err;
               best = pitch;
            }
         }
      }

      return !Float.isNaN(best) && !(bestErr > 6.0) ? new float[]{yaw, best} : null;
   }

   static final class Cand {
      final BlockPos pos;
      final double score;

      Cand(BlockPos pos, double score) {
         this.pos = pos;
         this.score = score;
      }
   }

   static final class Move {
      final BlockPos pos;
      final double cost;

      Move(BlockPos pos, double cost) {
         this.pos = pos;
         this.cost = cost;
      }
   }

   static final class Nd {
      final BlockPos pos;
      final AutoMiner.Nd parent;
      final double g;
      final double f;

      Nd(BlockPos pos, AutoMiner.Nd parent, double g, double f) {
         this.pos = pos;
         this.parent = parent;
         this.g = g;
         this.f = f;
      }
   }
}
