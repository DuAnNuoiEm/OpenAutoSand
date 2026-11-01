package autosand;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AutoSandOrders implements ClientModInitializer {
   static final String OWNER_NOTICE = "AutoSandOrders (c) Dương Việt Anh - all rights reserved. Do not crack, bypass the license check or redistribute.";
   static final String NUKER_ON = ".toggle nuker on";
   static final String NUKER_OFF = ".toggle nuker off";
   static final int ACTION_DELAY = 4;
   static final int THROW_DELAY = 3;
   static final int STEP_TIMEOUT = 200;
   static final int COOLDOWN_AFTER = 60;
   static final int FINISH_CLOSES = 2;
   static final int BUY_MAX_ROUNDS = 9;
   static final int BUY_RETRIES = 2;
   static final int ORDER_MAX_TRIES = 15;
   static final int ORDER_OPEN_TIMEOUT = 60;
   static final int CONFIRM_SLOT = 16;
   static final int BOTTLE_SLOT = 1;
   static final int SHOP_PEARL_SLOT = 14;
   static final int KING_SLOT = 24;
   static final int SHOP_TOTEM_SLOT = 13;
   static final int SHOP_BOTTLE_SLOT = 16;
   static final int BUY_SET64_SLOT = 17;
   static final int BUY_CONFIRM_SLOT = 23;
   static final int SHOP_FOOD_SLOT = 14;
   static final int SHOP_FOOD_ITEM_SLOT = 16;
   static final int EAT_SLOT = 8;
   static final int JUNK_INTERVAL = 100;
   static final int JUNK_MAX_DROPS = 60;
   static AutoSandOrders.Mode mode = AutoSandOrders.Mode.BUILTIN;
   static boolean useNuker = true;
   static boolean keysForward = true;
   static boolean autoDeliver = true;
   static boolean autoMend = true;
   static boolean autoBuy = true;
   static int mendPct = 10;
   static int mendUntil = 100;
   static int eatUntil = 18;
   static double minSandPrice = 0.0;
   static double maxXpUnitPrice = 0.0;
   static double maxXpSpend = 0.0;
   static double maxTotemUnitPrice = 0.0;
   static double maxTotemSpend = 0.0;
   static double xpSpent = 0.0;
   static double totemSpent = 0.0;
   static boolean autoDefend = false;
   static int defenseRadius = 6;
   static boolean lowLoad = false;
   static int fgFps = 60;
   static int bgFps = 10;
   static int lowLoadView = 6;
   static final int[] DEFENSE_LEVELS = new int[]{4, 6, 8, 12};
   static final int[] FG_FPS_LEVELS = new int[]{30, 60, 90, 120};
   static final int[] BG_FPS_LEVELS = new int[]{5, 10, 15, 30};
   static final int[] VIEW_LEVELS = new int[]{4, 6, 8, 12};
   static final double[] MIN_PRICE_LEVELS = new double[]{0.0, 1.0, 2.0, 5.0, 10.0, 20.0};
   static final Set<String> extraKeep = new LinkedHashSet<>();
   static boolean autoEat = true;
   static boolean autoBuyFood = true;
   static boolean autoDrop = true;
   static int eatBelow = 14;
   static int pickupRadius = 8;
   static boolean autoRevive = true;
   static boolean autoRejoin = true;
   static boolean usePearl = true;
   static boolean autoTotem = true;
   static int mineSpeed = 500;
   static final int[] SPEED_LEVELS = new int[]{500, 400, 300, 200, 150, 100};
   static double pricePerSand = 5.0;
   static long statSand = 0L;
   static long statTicks = 0L;
   static int statDeaths = 0;
   static final double[] PRICE_LEVELS = new double[]{1.0, 2.0, 5.0, 10.0, 20.0, 50.0};
   static final int[] PICKUP_LEVELS = new int[]{0, 4, 8, 12};
   static final int[] EAT_LEVELS = new int[]{8, 12, 14, 16};
   static String ORDER_COMMAND = "orders sand";
   static String SHOP_COMMAND = "shop";
   static final int[] PCTS = new int[]{5, 10, 20, 30};
   static AutoSandOrders.S state = AutoSandOrders.S.MINING;
   static boolean enabled = false;
   static boolean openGui = false;
   static int wait = 0;
   static int timeout = 0;
   static int cooldown = 0;
   static int lastSync = -1;
   static int clicks = 0;
   static int step = 0;
   static int shovelFrom = -1;
   static int mendThrows = 0;
   static int lastCheck = 0;
   static int lastDamage = 0;
   static int buyTries = 0;
   static int closes = 0;
   static int finishIdle = 0;
   static int buyRound = 0;
   static int buyTarget = 1;
   static int buyBefore = 0;
   static int dropStep = 0;
   static int buyRetry = 0;
   static int gone = 0;
   static int orderTry = 0;
   static int orderSlot = -1;
   static int ordersWait = 0;
   static int sandBefore = 0;
   static boolean settle = false;
   static LocalPlayer lastPlayer = null;
   static int reviveIn = 0;
   static final int REJOIN_WAIT = 200;
   static int rejoinWait = 0;
   static boolean buyFood = false;
   static boolean foodWarn = false;
   static boolean hpEating = false;
   static int foodBuyFails = 0;
   static int junkCool = 0;
   static int junkStep = 0;
   static int junkDropped = 0;
   static int eatPrevSlot = -1;
   static int eatIdle = 0;
   static int eatLastFood = 0;
   static int eatTries = 0;
   static int eatOffFrom = -1;
   static boolean eatOff = false;
   static float eatXRot = Float.NaN;
   static float junkXRot = Float.NaN;
   static boolean shovelSwapped = false;
   static boolean needBuy = false;
   static boolean buyPearl = false;
   static boolean buyTotem = false;
   static int totemCool = 0;
   static int totemBuyFails = 0;
   static boolean totemWarn = false;
   static boolean buyResume = false;
   static int buyRedo = 0;
   static int pearlNeed = 1;
   static int junkMoves = 0;
   static int lastDelivered = 0;
   static float oldXRot = Float.NaN;
   static float dropXRot = Float.NaN;
   static KeyMapping menuKey = null;
   static KeyMapping stopKey = null;
   static Boolean nukerState = null;
   static boolean nukerWater = false;
   static int nwTick = 0;
   static boolean reserveWeapon = false;
   static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
   static final Pattern PRICE_WORD = Pattern.compile("\\b(gia|price|cost|total|tong)\\b");
   static final Pattern MONEY_RE = Pattern.compile("(\\d[\\d.,]*)\\s*([kKmMbB])?");
   static String testKind = null;
   static int testTicks = 0;
   static int defendPrevSlot = -1;
   static int defendTicks = 0;
   static int defendCool = 0;
   static int defendIdle = 0;
   static int defendAttackWait = 0;
   static int defendAimWait = 0;
   static Integer origFps = null;
   static Integer origView = null;
   static final int HOLE_DEPTH = 3;
   static final int TRAP_TICKS = 4;
   static final int ESCAPE_MAX_PILLARS = 20;
   static final int GUARD_COOLDOWN = 60;
   static final int WATCHDOG_TICKS = 200;
   static final int SHORE_RADIUS = 10;
   static final int SIDE_TICKS = 14;
   static final int BRIDGE_RADIUS = 16;
   static final int BRIDGE_MAX = 40;
   static final int PICKUP_MAX_TICKS = 100;
   static final int WD_STEP = 20;
   static final int WD_IDLE_MIN = 100;
   static final int WD_IDLE_MAX = 400;
   static boolean avoidLiquid = true;
   static boolean avoidHole = true;
   static boolean bridgeWater = true;
   static AutoSandOrders.G guard = AutoSandOrders.G.NONE;
   static int guardCool = 0;
   static int guardTicks = 0;
   static int trapTicks = 0;
   static int wdTick = 0;
   static double wdX = Double.NaN;
   static double wdZ = Double.NaN;
   static BlockPos shore = null;
   static int escPhase = 0;
   static int escPhaseTicks = 0;
   static int escPillars = 0;
   static int escSlot = -1;
   static int escPrevSel = 0;
   static int escSwapIdx = -1;
   static BlockPos escBase = null;
   static float escPitch = Float.NaN;
   static int sideTicks = 0;
   static int sideStrikes = 0;
   static int sideLock = 0;
   static int sideWindow = 0;
   static BlockPos bridgeTarget = null;
   static Direction bridgeDir = null;
   static int bridgeCool = 0;
   static int brPlaced = 0;
   static int brCool = 0;
   static int surroundTicks = 0;
   static ItemEntity pickTarget = null;
   static int pickTicks = 0;
   static int pickCool = 0;
   static final Set<Integer> pickIgnore = new HashSet<>();
   static int wetTicks = 0;
   static int wdIdle = 0;
   static int wdGap = 100;
   static int wdFails = 0;
   static int wdSand = -1;
   static final Direction[] HORIZ = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
   static int escCool = 0;
   static int idleTick = 0;
   static int idleAcc = 0;
   static int idleSand = -1;
   static double idleX = Double.NaN;
   static double idleZ = Double.NaN;
   static boolean idleWorked = false;
   static boolean encCache = false;
   static double jamX = Double.NaN;
   static double jamZ = Double.NaN;
   static double unX = 0.0;
   static double unZ = 0.0;
   static int jamTick = 0;
   static int jamHit = 0;
   static int jamSand = -1;
   static int unTicks = 0;
   static int unStrikes = 0;
   static int unWindow = 0;
   static int unLock = 0;
   static float unYaw = 0.0F;
   static double liqX = Double.NaN;
   static double liqZ = Double.NaN;

   public void onInitializeClient() {
      AutoSandOrders.License.init();
      load();
      ClientTickEvents.END_CLIENT_TICK.register(AutoSandOrders::tick);
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)mc -> restorePerf(mc));
      registerMenuKey();
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> dispatcher.register(
               (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal(
                                                            "autosand"
                                                         )
                                                         .executes(ctx -> {
                                                            openGui = true;
                                                            return 1;
                                                         }))
                                                      .then(
                                                         ((LiteralArgumentBuilder)ClientCommandManager.literal("toggle")
                                                               .requires(src -> AutoSandOrders.License.valid()))
                                                            .executes(ctx -> {
                                                               setEnabled(Minecraft.getInstance(), !enabled);
                                                               return 1;
                                                            })
                                                      ))
                                                   .then(
                                                      ClientCommandManager.literal("key")
                                                         .then(ClientCommandManager.argument("key", StringArgumentType.greedyString()).executes(ctx -> {
                                                            AutoSandOrders.License.activate(StringArgumentType.getString(ctx, "key"));
                                                            ((FabricClientCommandSource)ctx.getSource())
                                                               .sendFeedback(Component.literal("[AutoSand] Đang xác thực key..."));
                                                            return 1;
                                                         }))
                                                   ))
                                                .then(ClientCommandManager.literal("status").executes(ctx -> {
                                                   ((FabricClientCommandSource)ctx.getSource())
                                                      .sendFeedback(Component.literal("[AutoSand] Key: " + AutoSandOrders.License.status()));
                                                   return 1;
                                                })))
                                             .then(
                                                ((LiteralArgumentBuilder)ClientCommandManager.literal("speed").requires(src -> AutoSandOrders.License.valid()))
                                                   .then(ClientCommandManager.argument("percent", IntegerArgumentType.integer(5, 500)).executes(ctx -> {
                                                      mineSpeed = IntegerArgumentType.getInteger(ctx, "percent");
                                                      save();
                                                      ((FabricClientCommandSource)ctx.getSource())
                                                         .sendFeedback(Component.literal("[AutoSand] Tốc độ đào: " + mineSpeed + "%"));
                                                      return 1;
                                                   }))
                                             ))
                                          .then(
                                             ((LiteralArgumentBuilder)ClientCommandManager.literal("cmd").requires(src -> AutoSandOrders.License.valid()))
                                                .then(ClientCommandManager.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                                                   String t = StringArgumentType.getString(ctx, "text").trim();
                                                   if (t.startsWith("/")) {
                                                      t = t.substring(1);
                                                   }

                                                   ORDER_COMMAND = t;
                                                   save();
                                                   ((FabricClientCommandSource)ctx.getSource())
                                                      .sendFeedback(Component.literal("[AutoSand] Lệnh orders: /" + ORDER_COMMAND));
                                                   return 1;
                                                }))
                                          ))
                                       .then(
                                          ((LiteralArgumentBuilder)ClientCommandManager.literal("shop").requires(src -> AutoSandOrders.License.valid()))
                                             .then(ClientCommandManager.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                                                String t = StringArgumentType.getString(ctx, "text").trim();
                                                if (t.startsWith("/")) {
                                                   t = t.substring(1);
                                                }

                                                SHOP_COMMAND = t;
                                                save();
                                                ((FabricClientCommandSource)ctx.getSource())
                                                   .sendFeedback(Component.literal("[AutoSand] Lệnh shop: /" + SHOP_COMMAND));
                                                return 1;
                                             }))
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)ClientCommandManager.literal("lobby").requires(src -> AutoSandOrders.License.valid()))
                                          .executes(ctx -> {
                                             AutoSandOrders.Lobby.force = true;
                                             ((FabricClientCommandSource)ctx.getSource())
                                                .sendFeedback(Component.literal("[AutoSand] Thử quay lại KingSMP từ sảnh."));
                                             return 1;
                                          })
                                    ))
                                 .then(
                                    ((LiteralArgumentBuilder)ClientCommandManager.literal("price").requires(src -> AutoSandOrders.License.valid()))
                                       .then(ClientCommandManager.argument("value", DoubleArgumentType.doubleArg(0.0)).executes(ctx -> {
                                          pricePerSand = DoubleArgumentType.getDouble(ctx, "value");
                                          save();
                                          ((FabricClientCommandSource)ctx.getSource())
                                             .sendFeedback(Component.literal("[AutoSand] Giá ước tính mỗi cát: " + pricePerSand + " $"));
                                          return 1;
                                       }))
                                 ))
                              .then(ClientCommandManager.literal("stop").executes(ctx -> {
                                 emergencyStop(Minecraft.getInstance());
                                 return 1;
                              })))
                           .then(
                              ((LiteralArgumentBuilder)ClientCommandManager.literal("set").requires(src -> AutoSandOrders.License.valid()))
                                 .then(
                                    ClientCommandManager.argument("name", StringArgumentType.word())
                                       .then(ClientCommandManager.argument("value", StringArgumentType.word()).executes(ctx -> {
                                          String r = setOption(StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "value"));
                                          ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("[AutoSand] " + r));
                                          return 1;
                                       }))
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal("test")
                                    .requires(src -> AutoSandOrders.License.valid()))
                                 .then(ClientCommandManager.literal("orders").executes(ctx -> startTest("orders"))))
                              .then(ClientCommandManager.literal("shop").executes(ctx -> startTest("shop")))
                        ))
                     .then(
                        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal("keep")
                                    .requires(src -> AutoSandOrders.License.valid()))
                                 .then(
                                    ClientCommandManager.literal("list")
                                       .executes(
                                          ctx -> {
                                             ((FabricClientCommandSource)ctx.getSource())
                                                .sendFeedback(
                                                   Component.literal("[AutoSand] Giữ thêm: " + (extraKeep.isEmpty() ? "(trống)" : String.join(", ", extraKeep)))
                                                );
                                             return 1;
                                          }
                                       )
                                 ))
                              .then(
                                 ClientCommandManager.literal("add")
                                    .then(
                                       ClientCommandManager.argument("item", StringArgumentType.greedyString())
                                          .executes(
                                             ctx -> {
                                                ((FabricClientCommandSource)ctx.getSource())
                                                   .sendFeedback(Component.literal("[AutoSand] " + keepEdit(StringArgumentType.getString(ctx, "item"), true)));
                                                return 1;
                                             }
                                          )
                                    )
                              ))
                           .then(
                              ClientCommandManager.literal("remove")
                                 .then(
                                    ClientCommandManager.argument("item", StringArgumentType.greedyString())
                                       .executes(
                                          ctx -> {
                                             ((FabricClientCommandSource)ctx.getSource())
                                                .sendFeedback(Component.literal("[AutoSand] " + keepEdit(StringArgumentType.getString(ctx, "item"), false)));
                                             return 1;
                                          }
                                       )
                                 )
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)ClientCommandManager.literal("home").requires(src -> AutoSandOrders.License.valid()))
                        .then(ClientCommandManager.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                           String t = StringArgumentType.getString(ctx, "text").trim();
                           if (t.startsWith("/")) {
                              t = t.substring(1);
                           }

                           AutoMiner.homeCmd = t;
                           save();
                           ((FabricClientCommandSource)ctx.getSource())
                              .sendFeedback(Component.literal("[AutoSand] Lệnh về home sau khi chết: /" + AutoMiner.homeCmd));
                           return 1;
                        }))
                  )
            )
         );
   }

   static void registerMenuKey() {
      menuKey = registerKey("key.autosand.menu", 301);
      stopKey = registerKey("key.autosand.stop", 269);
   }

   static KeyMapping registerKey(String id, int keyCode) {
      try {
         Constructor<?> ctor = null;

         for (Constructor<?> c : KeyMapping.class.getConstructors()) {
            Class<?>[] pt = c.getParameterTypes();
            if (pt.length == 4 && pt[0] == String.class && pt[1] == Type.class && pt[2] == int.class) {
               ctor = c;
               break;
            }
         }

         if (ctor == null) {
            return null;
         } else {
            Class<?> ct = ctor.getParameterTypes()[3];
            Object cat = "key.categories.misc";
            if (ct != String.class) {
               cat = null;

               for (Field f : ct.getFields()) {
                  if (Modifier.isStatic(f.getModifiers()) && f.getType() == ct) {
                     cat = f.get(null);
                     break;
                  }
               }

               if (cat == null) {
                  return null;
               }
            }

            KeyMapping key = (KeyMapping)ctor.newInstance(id, Type.KEYSYM, keyCode, cat);

            Class<?> helper;
            try {
               helper = Class.forName("net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper");
            } catch (ClassNotFoundException var9) {
               helper = Class.forName("net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper");
            }

            helper.getMethod("registerKeyBinding", KeyMapping.class).invoke(null, key);
            return key;
         }
      } catch (Throwable var10) {
         return null;
      }
   }

   static void emergencyStop(Minecraft mc) {
      testKind = null;
      LocalPlayer p = mc.player;
      if (enabled) {
         if (p != null && mc.screen instanceof AbstractContainerScreen) {
            p.closeContainer();
         }

         setEnabled(mc, false);
      }

      releaseKeys(mc);
      if (p != null) {
         msg(p, "DỪNG KHẨN CẤP.");
      }
   }

   static Path cfgFile() {
      return FabricLoader.getInstance().getConfigDir().resolve("autosand.properties");
   }

   static void load() {
      try {
         Path f = cfgFile();
         if (!Files.exists(f)) {
            return;
         }

         Properties pr = new Properties();

         try (InputStream in = Files.newInputStream(f)) {
            pr.load(in);
         }

         try {
            mode = AutoSandOrders.Mode.valueOf(pr.getProperty("mode", "BUILTIN"));
         } catch (Exception var7) {
            mode = AutoSandOrders.Mode.BUILTIN;
         }

         useNuker = pBool(pr, "nuker", true);
         keysForward = pBool(pr, "keysForward", true);
         autoDeliver = pBool(pr, "deliver", true);
         autoMend = pBool(pr, "mend", true);
         autoBuy = pBool(pr, "buy", true);
         mendPct = pInt(pr, "mendPct", 10);
         autoEat = pBool(pr, "eat", true);
         autoBuyFood = pBool(pr, "buyFood", true);
         autoDrop = pBool(pr, "drop", true);
         eatBelow = pInt(pr, "eatBelow", 14);
         pickupRadius = pInt(pr, "pickup", 8);
         autoRevive = pBool(pr, "revive", true);
         avoidLiquid = pBool(pr, "avoidLiquid", true);
         avoidHole = pBool(pr, "avoidHole", true);
         bridgeWater = pBool(pr, "bridge", true);
         ORDER_COMMAND = pr.getProperty("orderCmd", ORDER_COMMAND);
         SHOP_COMMAND = pr.getProperty("shopCmd", SHOP_COMMAND);
         AutoMiner.homeCmd = pr.getProperty("homeCmd", AutoMiner.homeCmd);
         AutoMiner.deathRecover = pBool(pr, "deathRecover", true);
         AutoMiner.autoRespawn = pBool(pr, "autoRespawn", true);
         AutoMiner.digThrough = pBool(pr, "digThrough", true);
         AutoMiner.scanRadius = pInt(pr, "scanRadius", 16);
         AutoMiner.tunnel = false;
         AutoMiner.tunnelLayers = pInt(pr, "tunnelLayers", 3);
         AutoMiner.tunnelWidth = pInt(pr, "tunnelWidth", 3);
         AutoMiner.tunnelBelow = pInt(pr, "tunnelBelow", 0);
         AutoMiner.shaft = false;
         AutoMiner.descendDepth = pInt(pr, "descendDepth", 2);
         AutoMiner.nukerHold = pBool(pr, "nukerHold", true);
         autoRejoin = pBool(pr, "autoRejoin", true);
         usePearl = pBool(pr, "usePearl", true);
         autoTotem = pBool(pr, "autoTotem", true);
         mineSpeed = pInt(pr, "mineSpeed", 500);
         if (mineSpeed < 5 || mineSpeed > 500) {
            mineSpeed = 500;
         }

         pricePerSand = pDbl(pr, "pricePerSand", 5.0);
         statSand = pLong(pr, "statSand", 0L);
         statTicks = pLong(pr, "statTicks", 0L);
         statDeaths = pInt(pr, "statDeaths", 0);
         mendUntil = pInt(pr, "mendUntil", 100);
         eatUntil = pInt(pr, "eatUntil", 18);
         minSandPrice = pDbl(pr, "minSandPrice", 0.0);
         maxXpUnitPrice = pDbl(pr, "maxXpUnitPrice", 0.0);
         maxXpSpend = pDbl(pr, "maxXpSpend", 0.0);
         maxTotemUnitPrice = pDbl(pr, "maxTotemUnitPrice", 0.0);
         maxTotemSpend = pDbl(pr, "maxTotemSpend", 0.0);
         autoDefend = pBool(pr, "autoDefend", false);
         defenseRadius = pInt(pr, "defenseRadius", 6);
         lowLoad = pBool(pr, "lowLoad", false);
         fgFps = pInt(pr, "fgFps", 60);
         bgFps = pInt(pr, "bgFps", 10);
         lowLoadView = pInt(pr, "lowLoadView", 6);
         extraKeep.clear();

         for (String id : pr.getProperty("keepExtra", "").split(",")) {
            if (!id.isBlank()) {
               extraKeep.add(id.trim());
            }
         }
      } catch (Exception var9) {
      }

      validate();
   }

   static int pInt(Properties pr, String k, int d) {
      try {
         return Integer.parseInt(pr.getProperty(k, String.valueOf(d)).trim());
      } catch (Exception var4) {
         return d;
      }
   }

   static long pLong(Properties pr, String k, long d) {
      try {
         return Long.parseLong(pr.getProperty(k, String.valueOf(d)).trim());
      } catch (Exception var5) {
         return d;
      }
   }

   static double pDbl(Properties pr, String k, double d) {
      try {
         return Double.parseDouble(pr.getProperty(k, String.valueOf(d)).trim());
      } catch (Exception var5) {
         return d;
      }
   }

   static boolean pBool(Properties pr, String k, boolean d) {
      String v = pr.getProperty(k);
      return v == null ? d : Boolean.parseBoolean(v.trim());
   }

   static int clampI(int v, int lo, int hi) {
      return Math.max(lo, Math.min(hi, v));
   }

   static double clampD(double v) {
      return !Double.isNaN(v) && !Double.isInfinite(v) && !(v < 0.0) ? v : 0.0;
   }

   static void validate() {
      mendPct = clampI(mendPct, 1, 90);
      mendUntil = clampI(mendUntil, 10, 100);
      eatBelow = clampI(eatBelow, 1, 19);
      eatUntil = clampI(eatUntil, Math.min(20, eatBelow + 1), 20);
      pickupRadius = clampI(pickupRadius, 0, 32);
      mineSpeed = clampI(mineSpeed, 5, 500);
      defenseRadius = clampI(defenseRadius, 2, 16);
      fgFps = clampI(fgFps, 10, 260);
      bgFps = clampI(bgFps, 1, 60);
      lowLoadView = clampI(lowLoadView, 2, 32);
      AutoMiner.scanRadius = clampI(AutoMiner.scanRadius, 4, 64);
      AutoMiner.tunnelLayers = clampI(AutoMiner.tunnelLayers, 1, 8);
      AutoMiner.tunnelWidth = clampI(AutoMiner.tunnelWidth, 1, 5);
      if (AutoMiner.tunnelWidth % 2 == 0) {
         AutoMiner.tunnelWidth++;
      }

      AutoMiner.tunnelBelow = clampI(AutoMiner.tunnelBelow, 0, Math.min(7, AutoMiner.tunnelLayers - 1));
      AutoMiner.tunnelInit = false;
      AutoMiner.descendDepth = clampI(AutoMiner.descendDepth, 0, 8);
      pricePerSand = clampD(pricePerSand);
      minSandPrice = clampD(minSandPrice);
      maxXpUnitPrice = clampD(maxXpUnitPrice);
      maxXpSpend = clampD(maxXpSpend);
      maxTotemUnitPrice = clampD(maxTotemUnitPrice);
      maxTotemSpend = clampD(maxTotemSpend);
      if (ORDER_COMMAND == null || ORDER_COMMAND.isBlank()) {
         ORDER_COMMAND = "orders sand";
      }

      if (SHOP_COMMAND == null || SHOP_COMMAND.isBlank()) {
         SHOP_COMMAND = "shop";
      }
   }

   static String setOption(String name, String v) {
      try {
         switch (name) {
            case "mendPct":
               mendPct = Integer.parseInt(v);
               break;
            case "mendUntil":
               mendUntil = Integer.parseInt(v);
               break;
            case "eatBelow":
               eatBelow = Integer.parseInt(v);
               break;
            case "eatUntil":
               eatUntil = Integer.parseInt(v);
               break;
            case "pickup":
               pickupRadius = Integer.parseInt(v);
               break;
            case "defenseRadius":
               defenseRadius = Integer.parseInt(v);
               break;
            case "fgFps":
               fgFps = Integer.parseInt(v);
               break;
            case "bgFps":
               bgFps = Integer.parseInt(v);
               break;
            case "lowLoadView":
               lowLoadView = Integer.parseInt(v);
               break;
            case "minSandPrice":
               minSandPrice = Double.parseDouble(v);
               break;
            case "maxXpUnitPrice":
               maxXpUnitPrice = Double.parseDouble(v);
               break;
            case "maxXpSpend":
               maxXpSpend = Double.parseDouble(v);
               break;
            case "maxTotemUnitPrice":
               maxTotemUnitPrice = Double.parseDouble(v);
               break;
            case "maxTotemSpend":
               maxTotemSpend = Double.parseDouble(v);
               break;
            case "autoDefend":
               autoDefend = Boolean.parseBoolean(v);
               break;
            case "lowLoad":
               lowLoad = Boolean.parseBoolean(v);
               break;
            case "descendDepth":
               AutoMiner.descendDepth = clampI(Integer.parseInt(v), 0, 8);
               break;
            case "nukerHold":
               AutoMiner.nukerHold = Boolean.parseBoolean(v);
               break;
            default:
               return "Không có mục \""
                  + name
                  + "\". Xem danh sách trong /autosand set (mendPct, mendUntil, eatBelow, eatUntil, pickup, defenseRadius, fgFps, bgFps, lowLoadView, minSandPrice, maxXpUnitPrice, maxXpSpend, maxTotemUnitPrice, maxTotemSpend, autoDefend, lowLoad, descendDepth, nukerHold).";
         }
      } catch (NumberFormatException var4) {
         return "Giá trị không hợp lệ: " + v;
      }

      validate();
      save();
      return "Đã đặt " + name + " = " + v;
   }

   static void save() {
      try {
         Properties pr = new Properties();
         pr.setProperty("mode", mode.name());
         pr.setProperty("nuker", String.valueOf(useNuker));
         pr.setProperty("keysForward", String.valueOf(keysForward));
         pr.setProperty("deliver", String.valueOf(autoDeliver));
         pr.setProperty("mend", String.valueOf(autoMend));
         pr.setProperty("buy", String.valueOf(autoBuy));
         pr.setProperty("mendPct", String.valueOf(mendPct));
         pr.setProperty("eat", String.valueOf(autoEat));
         pr.setProperty("buyFood", String.valueOf(autoBuyFood));
         pr.setProperty("drop", String.valueOf(autoDrop));
         pr.setProperty("eatBelow", String.valueOf(eatBelow));
         pr.setProperty("pickup", String.valueOf(pickupRadius));
         pr.setProperty("revive", String.valueOf(autoRevive));
         pr.setProperty("avoidLiquid", String.valueOf(avoidLiquid));
         pr.setProperty("avoidHole", String.valueOf(avoidHole));
         pr.setProperty("bridge", String.valueOf(bridgeWater));
         pr.setProperty("orderCmd", ORDER_COMMAND);
         pr.setProperty("shopCmd", SHOP_COMMAND);
         pr.setProperty("homeCmd", AutoMiner.homeCmd);
         pr.setProperty("deathRecover", String.valueOf(AutoMiner.deathRecover));
         pr.setProperty("autoRespawn", String.valueOf(AutoMiner.autoRespawn));
         pr.setProperty("digThrough", String.valueOf(AutoMiner.digThrough));
         pr.setProperty("scanRadius", String.valueOf(AutoMiner.scanRadius));
         pr.setProperty("descendDepth", String.valueOf(AutoMiner.descendDepth));
         pr.setProperty("nukerHold", String.valueOf(AutoMiner.nukerHold));
         pr.setProperty("autoRejoin", String.valueOf(autoRejoin));
         pr.setProperty("usePearl", String.valueOf(usePearl));
         pr.setProperty("autoTotem", String.valueOf(autoTotem));
         pr.setProperty("mineSpeed", String.valueOf(mineSpeed));
         pr.setProperty("pricePerSand", String.valueOf(pricePerSand));
         pr.setProperty("statSand", String.valueOf(statSand));
         pr.setProperty("statTicks", String.valueOf(statTicks));
         pr.setProperty("statDeaths", String.valueOf(statDeaths));
         pr.setProperty("mendUntil", String.valueOf(mendUntil));
         pr.setProperty("eatUntil", String.valueOf(eatUntil));
         pr.setProperty("minSandPrice", String.valueOf(minSandPrice));
         pr.setProperty("maxXpUnitPrice", String.valueOf(maxXpUnitPrice));
         pr.setProperty("maxXpSpend", String.valueOf(maxXpSpend));
         pr.setProperty("maxTotemUnitPrice", String.valueOf(maxTotemUnitPrice));
         pr.setProperty("maxTotemSpend", String.valueOf(maxTotemSpend));
         pr.setProperty("autoDefend", String.valueOf(autoDefend));
         pr.setProperty("defenseRadius", String.valueOf(defenseRadius));
         pr.setProperty("lowLoad", String.valueOf(lowLoad));
         pr.setProperty("fgFps", String.valueOf(fgFps));
         pr.setProperty("bgFps", String.valueOf(bgFps));
         pr.setProperty("lowLoadView", String.valueOf(lowLoadView));
         pr.setProperty("keepExtra", String.join(",", extraKeep));

         try (OutputStream out = Files.newOutputStream(cfgFile())) {
            pr.store(out, "AutoSand");
         }
      } catch (Exception var6) {
      }
   }

   static boolean screenBlocks(Minecraft mc) {
      Screen sc = mc.screen;
      return sc != null && !(sc instanceof ChatScreen) && !(sc instanceof PauseScreen);
   }

   static void setEnabled(Minecraft mc, boolean on) {
      if (on != enabled) {
         LocalPlayer p = mc.player;
         if (on && !AutoSandOrders.License.valid()) {
            if (p != null) {
               msg(p, "Chưa kích hoạt key (" + AutoSandOrders.License.status() + "). Dùng: /autosand key <KEY>");
            }
         } else {
            enabled = on;
            if (on) {
               totemSpent = 0.0;
            }

            nukerState = null;
            AutoSandOrders.Lobby.reset();
            state = AutoSandOrders.S.MINING;
            cooldown = 0;
            wait = 0;
            guardReset(mc, p);
            AutoMiner.reset(mc);
            if (p != null) {
               if (on) {
                  startMining(p);
               } else {
                  if (shovelSwapped && mc.gameMode != null) {
                     swapOffhand(mc, p, shovelFrom);
                     shovelSwapped = false;
                  }

                  restoreLook(mc, p);
                  stopMining(mc, p);
               }

               msg(p, on ? "BẬT" : "TẮT");
            }
         }
      }
   }

   static void startMining(LocalPlayer p) {
      if (rejoinWait <= 0) {
         if (useNuker) {
            setNuker(p, true);
         }

         if (mode == AutoSandOrders.Mode.BUILTIN) {
            AutoMiner.start(p);
         }
      }
   }

   static void stopMining(Minecraft mc, LocalPlayer p) {
      if (useNuker) {
         setNuker(p, false);
      }

      if (mode == AutoSandOrders.Mode.BUILTIN) {
         AutoMiner.stop(mc);
      } else {
         releaseKeys(mc);
      }
   }

   static void releaseKeys(Minecraft mc) {
      mc.options.keyAttack.setDown(false);
      mc.options.keyUp.setDown(false);
      mc.options.keyUse.setDown(false);
   }

   static void tick(Minecraft mc) {
      LocalPlayer p = mc.player;
      perfTick(mc);
      if (p != null && mc.gameMode != null) {
         AutoSandOrders.Reconnect.connected(mc);
         AutoSandOrders.License.refreshIfNeeded();
         AutoSandOrders.Announcements.tick(mc, p);
         if (enabled && !AutoSandOrders.License.valid()) {
            setEnabled(mc, false);
            msg(p, "Key không còn hợp lệ (" + AutoSandOrders.License.status() + "). Đã tắt AutoSand.");
         }

         if (totemCool > 0) {
            totemCool--;
         }

         while (menuKey != null && menuKey.consumeClick()) {
            openGui = true;
         }

         while (stopKey != null && stopKey.consumeClick()) {
            emergencyStop(mc);
         }

         if (testKind != null) {
            testTick(mc, p);
            if (testKind != null) {
               return;
            }
         }

         if (openGui && mc.screen == null) {
            openGui = false;
            mc.setScreen(new AutoSandOrders.AutoSandScreen());
         } else {
            AutoMiner.arrowTick(mc, p);
            if (!enabled) {
               lastPlayer = p;
            } else {
               if (p != lastPlayer) {
                  boolean had = lastPlayer != null;
                  boolean deathReturn = AutoMiner.deathRecorded && AutoMiner.deathPos != null;
                  lastPlayer = p;
                  if (had) {
                     guard = AutoSandOrders.G.NONE;
                     pickTarget = null;
                     shovelSwapped = false;
                     state = AutoSandOrders.S.MINING;
                     wait = 0;
                     cooldown = 40;
                     reviveIn = 60;
                     if (!deathReturn) {
                        reviveIn = 0;
                        rejoinWait = 200;
                        AutoMiner.stop(mc);
                        msg(p, "Vào lại server: tạm dừng đào, chờ 10 giây rồi đào tiếp.");
                     }

                     wdX = Double.NaN;
                     if (mode == AutoSandOrders.Mode.BUILTIN) {
                        AutoMiner.onRespawn(mc, p);
                     }
                  }
               }

               if (p.isDeadOrDying()) {
                  if (mode == AutoSandOrders.Mode.BUILTIN) {
                     AutoMiner.onDead(mc, p);
                  }
               } else {
                  if (reviveIn > 0 && --reviveIn == 0) {
                     startMining(p);
                  }

                  if (++statTicks % 1200L == 0L) {
                     save();
                  }

                  if (!AutoSandOrders.Lobby.tick(mc, p)) {
                     if (state != AutoSandOrders.S.MINING || screenBlocks(mc) || !guardTick(mc, p)) {
                        if (rejoinWait > 0 && --rejoinWait == 0) {
                           startMining(p);
                           msg(p, "Đã chờ xong 10 giây, tiếp tục đào.");
                        }

                        if (rejoinWait > 0) {
                           AutoMiner.release(mc);
                        } else if (state == AutoSandOrders.S.MINING) {
                           if (!screenBlocks(mc)) {
                              if (!autoDefend || !defendTick(mc, p)) {
                                 if (useNuker && ++nwTick >= 10) {
                                    nwTick = 0;
                                    boolean wn = AutoMiner.waterWithin(mc.level, p.blockPosition(), 5);
                                    if (wn != nukerWater) {
                                       nukerWater = wn;
                                       if (wn) {
                                          setNuker(p, false);
                                       } else {
                                          setNuker(p, true);
                                       }

                                       if (wn) {
                                          msg(p, "Có nước gần đây -> tạm tắt Nuker, chỉ đào cát an toàn.");
                                       }
                                    }
                                 }

                                 if (mode == AutoSandOrders.Mode.KEYS) {
                                    mc.options.keyAttack.setDown(true);
                                    mc.options.keyUp.setDown(keysForward);
                                    if (mc.screen != null
                                       && mc.hitResult instanceof BlockHitResult bhr
                                       && bhr.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                                       mc.gameMode.continueDestroyBlock(bhr.getBlockPos(), bhr.getDirection());
                                       p.swing(InteractionHand.MAIN_HAND);
                                    }
                                 }

                                 if (mode == AutoSandOrders.Mode.BUILTIN) {
                                    AutoMiner.tick(mc, p);
                                    if (AutoMiner.recovering()) {
                                       return;
                                    }
                                 }

                                 if (junkCool > 0) {
                                    junkCool--;
                                 }

                                 if (wait > 0) {
                                    wait--;
                                 } else if (cooldown > 0) {
                                    cooldown--;
                                 } else if (!autoTotem || !totemTick(mc, p)) {
                                    if (junkCool > 0 || (!autoDrop || junkSlot(p) < 0) && arrangeMove(p) == null) {
                                       if (autoEat && p.getFoodData().getFoodLevel() <= eatBelow) {
                                          if (findFood(p) >= 0) {
                                             foodWarn = false;
                                             startEat(mc, p);
                                             return;
                                          }

                                          if (autoBuyFood && emptySlot(p) >= 0) {
                                             startFoodBuy(mc, p);
                                             return;
                                          }

                                          if (autoBuyFood && autoDeliver && countItem(p, Items.SAND) > 0) {
                                             startDeliver(mc, p);
                                             return;
                                          }

                                          if (!foodWarn) {
                                             foodWarn = true;
                                             msg(
                                                p,
                                                autoBuyFood
                                                   ? "Đói nhưng túi không còn ô trống để mua đồ ăn (giao cát trước)."
                                                   : "Đói nhưng hết đồ ăn (tự mua đồ ăn đang tắt)."
                                             );
                                          }
                                       }

                                       if (autoDeliver && isFull(p) && countItem(p, Items.SAND) > 0) {
                                          startDeliver(mc, p);
                                       } else if (autoMend && lowShovel(p) >= 0) {
                                          startMend(mc, p);
                                       }
                                    } else {
                                       startJunk(mc, p);
                                    }
                                 }
                              }
                           }
                        } else if (wait > 0) {
                           wait--;
                        } else if (--timeout > 0) {
                           ChestMenu h = container(mc);
                           switch (state) {
                              case WAIT_ORDERS:
                                 if (h == null || h.getRowCount() != 6 || !loaded(h)) {
                                    return;
                                 }

                                 int pick = -1;
                                 int seen = 0;
                                 int n = h.getRowCount() * 9;

                                 for (int i = 0; i < n && i < h.slots.size(); i++) {
                                    if (((Slot)h.slots.get(i)).getItem().is(Items.SAND)) {
                                       if (minSandPrice > 0.0) {
                                          double pr = lorePrice(((Slot)h.slots.get(i)).getItem());
                                          if (pr > 0.0 && pr < minSandPrice) {
                                             continue;
                                          }
                                       }

                                       if (seen == orderTry) {
                                          pick = i;
                                          break;
                                       }

                                       seen++;
                                    }
                                 }

                                 if (pick < 0) {
                                    if (++ordersWait < 30) {
                                       return;
                                    }

                                    msg(p, "Bảng /orders không có đơn cát nào dùng được. Các ô: " + dump(h));
                                    abort(mc, "không còn đơn cát nào để thử (đã thử " + orderTry + " đơn)");
                                    return;
                                 }

                                 orderSlot = pick;
                                 click(mc, p, h, pick, ClickType.PICKUP);
                                 lastSync = h.containerId;
                                 state = AutoSandOrders.S.WAIT_DELIVER;
                                 timeout = 60;
                                 wait = 4;
                                 break;
                              case WAIT_DELIVER:
                                 if (h == null || h.containerId == lastSync || h.getRowCount() != 6) {
                                    return;
                                 }

                                 lastSync = h.containerId;
                                 sandBefore = countItem(p, Items.SAND);
                                 clicks = 0;
                                 settle = false;
                                 state = AutoSandOrders.S.DEPOSIT;
                                 timeout = 200;
                                 wait = 4;
                                 break;
                              case DEPOSIT:
                                 if (h != null && h.containerId == lastSync) {
                                    if (settle) {
                                       settle = false;
                                       if (!sandMoved(p)) {
                                          nextOrder(mc, p, "đơn không nhận cát (đầy?)");
                                          return;
                                       }
                                    }

                                    if (clicks < 120) {
                                       for (int ixx = 0; ixx < h.slots.size(); ixx++) {
                                          Slot s = (Slot)h.slots.get(ixx);
                                          if (s.container == p.getInventory() && s.getItem().is(Items.SAND)) {
                                             click(mc, p, h, ixx, ClickType.QUICK_MOVE);
                                             clicks++;
                                             if (clicks == 1) {
                                                settle = true;
                                                wait = 6;
                                             } else {
                                                wait = 1;
                                             }

                                             return;
                                          }
                                       }
                                    }

                                    if (clicks == 0) {
                                       abort(mc, "không có cát để giao");
                                       return;
                                    }

                                    p.closeContainer();
                                    state = AutoSandOrders.S.WAIT_CONFIRM;
                                    timeout = 200;
                                    wait = 4;
                                    break;
                                 }

                                 if (sandMoved(p)) {
                                    state = AutoSandOrders.S.WAIT_CONFIRM;
                                    timeout = 200;
                                    wait = 4;
                                 } else {
                                    nextOrder(mc, p, "đơn đóng bảng mà không nhận cát");
                                 }

                                 return;
                              case WAIT_CONFIRM:
                                 if (h == null || h.containerId == lastSync || h.getRowCount() != 3) {
                                    return;
                                 }

                                 int slot = find(h, st -> nameHas(st, "xác nhận", "confirm"), -1);
                                 if (slot < 0) {
                                    slot = 16;
                                 }

                                 lastDelivered = Math.max(0, sandBefore - countItem(p, Items.SAND));
                                 click(mc, p, h, slot, ClickType.PICKUP);
                                 statSand = statSand + lastDelivered;
                                 save();
                                 state = AutoSandOrders.S.FINISH;
                                 closes = 0;
                                 finishIdle = 0;
                                 timeout = 200;
                                 wait = 12;
                                 break;
                              case FINISH:
                                 if (screenBlocks(mc)) {
                                    p.closeContainer();
                                    closes++;
                                    finishIdle = 0;
                                    wait = 4;
                                    return;
                                 }

                                 int idleMax = closes < 2 ? 8 : 2;
                                 if (finishIdle < idleMax) {
                                    finishIdle++;
                                    wait = 3;
                                    return;
                                 }

                                 startMining(p);
                                 state = AutoSandOrders.S.MINING;
                                 cooldown = 60;
                                 msg(p, "Đã giao xong, tiếp tục đào.");
                                 break;
                              case MEND_PREP:
                                 if (screenBlocks(mc)) {
                                    p.closeContainer();
                                    wait = 2;
                                    return;
                                 }

                                 if (step == 0) {
                                    int bs = bottleSlot(p);
                                    if (!p.getInventory().getItem(bs).is(Items.EXPERIENCE_BOTTLE)) {
                                       int idx = findItem(p, Items.EXPERIENCE_BOTTLE);
                                       if (idx < 0) {
                                          abort(mc, "không có bình XP");
                                          return;
                                       }

                                       swapHotbar(mc, p, idx, bs);
                                       wait = 4;
                                    }

                                    step = 1;
                                    return;
                                 }

                                 if (step == 1) {
                                    int idx = lowShovel(p);
                                    if (idx < 0) {
                                       finishMend(mc, p);
                                       return;
                                    }

                                    shovelFrom = idx;
                                    swapOffhand(mc, p, idx);
                                    shovelSwapped = true;
                                    step = 2;
                                    wait = 4;
                                    return;
                                 }

                                 setSelected(p, bottleSlot(p));
                                 oldXRot = p.getXRot();
                                 mendThrows = 0;
                                 lastCheck = 0;
                                 lastDamage = p.getOffhandItem().getDamageValue();
                                 state = AutoSandOrders.S.MEND_THROW;
                                 timeout = 200;
                                 wait = 2;
                                 break;
                              case MEND_THROW:
                                 if (screenBlocks(mc)) {
                                    p.closeContainer();
                                    wait = 2;
                                    return;
                                 }

                                 ItemStack off = p.getOffhandItem();
                                 if (!off.is(ItemTags.SHOVELS) || remainingPct(off) >= mendUntil) {
                                    state = AutoSandOrders.S.MEND_RESTORE;
                                    return;
                                 }

                                 int bs = bottleSlot(p);
                                 if (!p.getInventory().getItem(bs).is(Items.EXPERIENCE_BOTTLE)) {
                                    int idxx = findItem(p, Items.EXPERIENCE_BOTTLE);
                                    if (idxx < 0) {
                                       needBuy = true;
                                       state = AutoSandOrders.S.MEND_RESTORE;
                                       return;
                                    }

                                    swapHotbar(mc, p, idxx, bs);
                                    wait = 4;
                                    return;
                                 }

                                 if (getSelected(p) != bs) {
                                    setSelected(p, bs);
                                    wait = 2;
                                    return;
                                 }

                                 if (mendThrows - lastCheck >= 10) {
                                    int dmg = off.getDamageValue();
                                    if (dmg >= lastDamage) {
                                       autoMend = false;
                                       save();
                                       msg(p, "Xẻng không được sửa (có Mending không?). Đã tắt tự sửa xẻng.");
                                       state = AutoSandOrders.S.MEND_RESTORE;
                                       return;
                                    }

                                    lastDamage = dmg;
                                    lastCheck = mendThrows;
                                 }

                                 p.setXRot(90.0F);
                                 mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                                 p.swing(InteractionHand.MAIN_HAND);
                                 mendThrows++;
                                 timeout = 200;
                                 wait = 3;
                                 break;
                              case MEND_RESTORE:
                                 if (screenBlocks(mc)) {
                                    p.closeContainer();
                                    wait = 2;
                                    return;
                                 }

                                 if (shovelSwapped) {
                                    swapOffhand(mc, p, shovelFrom);
                                    shovelSwapped = false;
                                    if (shovelFrom >= 0 && shovelFrom < 9) {
                                       setSelected(p, shovelFrom);
                                    }

                                    if (!Float.isNaN(oldXRot)) {
                                       p.setXRot(oldXRot);
                                       oldXRot = Float.NaN;
                                    }

                                    wait = 4;
                                    return;
                                 }

                                 if (needBuy) {
                                    needBuy = false;
                                    if (autoBuy && buyTries < 2 && lowShovel(p) >= 0) {
                                       buyTries++;
                                       beginBuy();
                                       return;
                                    }
                                 }

                                 finishMend(mc, p);
                                 break;
                              case BUY_PREP:
                                 if (screenBlocks(mc)) {
                                    p.closeContainer();
                                    wait = 2;
                                    return;
                                 }

                                 int d = buyDropSlot(p);
                                 if (dropStep == 0) {
                                    if (d < 0) {
                                       dropStep = 2;
                                       return;
                                    }

                                    dropXRot = p.getXRot();
                                    p.setXRot(0.0F);
                                    dropStep = 1;
                                    wait = 3;
                                    return;
                                 }

                                 if (dropStep == 1) {
                                    if (d >= 0 && junkDropped < 40) {
                                       dropStack(mc, p, d);
                                       junkDropped++;
                                       wait = 2;
                                       return;
                                    }

                                    if (!Float.isNaN(dropXRot)) {
                                       p.setXRot(dropXRot);
                                       dropXRot = Float.NaN;
                                    }

                                    dropStep = 2;
                                    wait = 4;
                                    return;
                                 }

                                 if (!buyResume) {
                                    buyRound = 0;
                                    buyTarget = Math.max(1, Math.min(9, (hotbarMissing(p) + 63) / 64));
                                 }

                                 buyResume = false;
                                 send(p, "/" + SHOP_COMMAND);
                                 state = AutoSandOrders.S.BUY_SHOP;
                                 timeout = 200;
                                 wait = 4;
                                 break;
                              case BUY_SHOP:
                                 if (h == null || h.getRowCount() != 3 || !loaded(h)) {
                                    return;
                                 }

                                 int ix;
                                 if (buyFood) {
                                    ix = find(h, st -> nameHas(st, "food"), -1);
                                    if (ix < 0) {
                                       ix = 14;
                                    }
                                 } else {
                                    ix = find(h, st -> st.is(Items.TOTEM_OF_UNDYING), -1);
                                    if (ix < 0) {
                                       ix = 13;
                                    }
                                 }

                                 click(mc, p, h, ix, ClickType.PICKUP);
                                 lastSync = h.containerId;
                                 state = AutoSandOrders.S.BUY_GEAR;
                                 timeout = 200;
                                 wait = 4;
                                 break;
                              case BUY_GEAR:
                                 if (h == null || h.containerId == lastSync || h.getRowCount() != 3 || !loaded(h)) {
                                    return;
                                 }

                                 int ix;
                                 if (buyFood) {
                                    ix = find(h, st -> st.is(Items.GOLDEN_CARROT), -1);
                                    if (ix < 0) {
                                       ix = 16;
                                    }
                                 } else if (buyPearl) {
                                    ix = find(h, st -> st.is(Items.ENDER_PEARL), -1);
                                    if (ix < 0) {
                                       ix = 14;
                                    }
                                 } else if (buyTotem) {
                                    ix = find(h, st -> st.is(Items.TOTEM_OF_UNDYING), -1);
                                    if (ix < 0) {
                                       ix = 13;
                                    }
                                 } else {
                                    ix = find(h, st -> st.is(Items.EXPERIENCE_BOTTLE), -1);
                                    if (ix < 0) {
                                       ix = 16;
                                    }
                                 }

                                 click(mc, p, h, ix, ClickType.PICKUP);
                                 lastSync = h.containerId;
                                 state = AutoSandOrders.S.BUY_AMOUNT;
                                 timeout = 200;
                                 wait = 4;
                                 break;
                              case BUY_AMOUNT:
                                 if (h == null || h.containerId == lastSync || h.getRowCount() != 3 || !loaded(h)) {
                                    return;
                                 }

                                 if (buyPearl || buyTotem) {
                                    lastSync = h.containerId;
                                    gone = 0;
                                    state = AutoSandOrders.S.BUY_CONFIRM;
                                    timeout = 200;
                                    wait = 4;
                                    return;
                                 }

                                 int ix = find(h, st -> nameHas(st, "64"), 13);
                                 if (ix < 0) {
                                    ix = 17;
                                    msg(p, "Không thấy nút 64 theo tên, bấm ô " + ix + ". Các ô: " + dump(h));
                                 }

                                 click(mc, p, h, ix, ClickType.PICKUP);
                                 lastSync = h.containerId;
                                 gone = 0;
                                 state = AutoSandOrders.S.BUY_CONFIRM;
                                 timeout = 200;
                                 wait = 8;
                                 break;
                              case BUY_CONFIRM:
                                 if (h != null && h.getRowCount() == 3) {
                                    if (!loaded(h)) {
                                       return;
                                    }

                                    gone = 0;
                                    if (!buyFood && !buyPearl && !buyTotem && buyDropSlot(p) >= 0 && buyRedo < 8) {
                                       buyRedo++;
                                       p.closeContainer();
                                       buyResume = true;
                                       dropStep = 0;
                                       junkDropped = 0;
                                       state = AutoSandOrders.S.BUY_PREP;
                                       timeout = 200;
                                       wait = 4;
                                       return;
                                    }

                                    int ix = find(h, st -> nameHas(st, "xác nhận", "confirm"), -1);
                                    if (ix < 0) {
                                       ix = 23;
                                       msg(p, "Không thấy nút xác nhận theo tên, bấm ô " + ix + ". Các ô: " + dump(h));
                                    }

                                    double cost = menuCost(h);
                                    String block = overBudget(cost);
                                    if (block != null) {
                                       boolean wasTotem = buyTotem;
                                       abort(mc, block);
                                       if (wasTotem) {
                                          totemBuyFails = 0;
                                          totemCool = 6000;
                                       } else {
                                          cooldown = 6000;
                                       }

                                       return;
                                    }

                                    if (cost > 0.0) {
                                       if (buyTotem) {
                                          totemSpent += cost;
                                       } else if (!buyFood && !buyPearl) {
                                          xpSpent += cost;
                                       }
                                    }

                                    buyBefore = boughtCount(p);
                                    click(mc, p, h, ix, ClickType.PICKUP);
                                    state = AutoSandOrders.S.BUY_DONE;
                                    timeout = 80;
                                    wait = 12;
                                    break;
                                 }

                                 if (++gone > 20) {
                                    retryBuy(mc, p, "bảng mua bị đóng trước khi xác nhận");
                                 }

                                 return;
                              case BUY_DONE:
                                 if (boughtCount(p) <= buyBefore) {
                                    return;
                                 }

                                 if (screenBlocks(mc)) {
                                    p.closeContainer();
                                    wait = 3;
                                    return;
                                 }

                                 buyRound++;
                                 buyRetry = 0;
                                 if (buyRound < buyTarget && (!buyPearl || countItem(p, Items.ENDER_PEARL) < pearlNeed)) {
                                    if (!buyPearl && !buyFood && !buyTotem) {
                                       buyResume = true;
                                       dropStep = 0;
                                       junkDropped = 0;
                                       state = AutoSandOrders.S.BUY_PREP;
                                       timeout = 200;
                                       wait = 4;
                                       return;
                                    }

                                    send(p, "/" + SHOP_COMMAND);
                                    state = AutoSandOrders.S.BUY_SHOP;
                                    timeout = 200;
                                    wait = 4;
                                    return;
                                 }

                                 finishBuy(mc, p);
                                 break;
                              case EAT_PREP:
                                 if (screenBlocks(mc)) {
                                    p.closeContainer();
                                    wait = 2;
                                    return;
                                 }

                                 if (p.getFoodData().getFoodLevel() >= eatGoal()) {
                                    finishEat(mc, p, 60);
                                    return;
                                 }

                                 int idx = findFood(p);
                                 if (idx < 0) {
                                    msg(p, "Không có đồ ăn.");
                                    finishEat(mc, p, 600);
                                    return;
                                 }

                                 eatPrevSlot = getSelected(p);
                                 idx = toHotbar(mc, p, idx);
                                 setSelected(p, idx);
                                 eatXRot = p.getXRot();
                                 p.setXRot(-90.0F);
                                 mc.gameMode.stopDestroyBlock();
                                 eatIdle = 0;
                                 eatTries = 0;
                                 eatOff = false;
                                 eatOffFrom = -1;
                                 eatLastFood = p.getFoodData().getFoodLevel();
                                 msg(p, "Đói (" + eatLastFood + "/20) -> ăn.");
                                 state = AutoSandOrders.S.EAT_USE;
                                 timeout = 200;
                                 wait = 3;
                                 break;
                              case EAT_USE:
                                 if (screenBlocks(mc)) {
                                    p.closeContainer();
                                    wait = 2;
                                    return;
                                 }

                                 boolean using = p.isUsingItem();
                                 if (!using && p.getFoodData().getFoodLevel() >= eatGoal()) {
                                    finishEat(mc, p, 60);
                                    return;
                                 }

                                 InteractionHand hand = eatOff ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                                 if (!edible(p.getItemInHand(hand))) {
                                    mc.options.keyUse.setDown(false);
                                    if (using) {
                                       return;
                                    }

                                    int idx = findFood(p);
                                    if (idx < 0) {
                                       msg(p, "Hết đồ ăn.");
                                       finishEat(mc, p, 0);
                                       return;
                                    }

                                    if (eatOff) {
                                       swapOffhand(mc, p, idx);
                                       eatOffFrom = idx;
                                    } else {
                                       setSelected(p, toHotbar(mc, p, idx));
                                    }

                                    wait = 3;
                                    return;
                                 }

                                 int fl = p.getFoodData().getFoodLevel();
                                 if (fl > eatLastFood) {
                                    eatLastFood = fl;
                                    eatTries = 0;
                                    timeout = 200;
                                 }

                                 mc.options.keyUse.setDown(true);
                                 if (using) {
                                    eatIdle = 0;
                                    return;
                                 }

                                 if (mc.screen != null || ++eatIdle >= 4) {
                                    eatIdle = 0;
                                    eatTries++;
                                    if (eatTries == 5) {
                                       p.setXRot(0.0F);
                                    }

                                    if (eatTries == 9 && !eatOff) {
                                       int cur = getSelected(p);
                                       mc.options.keyUse.setDown(false);
                                       swapOffhand(mc, p, cur);
                                       eatOff = true;
                                       eatOffFrom = cur;
                                       wait = 4;
                                       return;
                                    }

                                    if (eatTries > 16) {
                                       abort(mc, "không ăn được dù đã thử nhiều cách");
                                       return;
                                    }

                                    mc.gameMode.stopDestroyBlock();
                                    mc.gameMode.useItem(p, hand);
                                    wait = 2;
                                 }
                                 break;
                              case JUNK:
                                 if (screenBlocks(mc)) {
                                    p.closeContainer();
                                    wait = 2;
                                    return;
                                 }

                                 if (junkStep == 0) {
                                    junkXRot = p.getXRot();
                                    p.setXRot(0.0F);
                                    junkStep = 1;
                                    wait = 3;
                                    return;
                                 }

                                 int j = autoDrop ? junkSlot(p) : -1;
                                 if (j >= 0 && junkDropped < 60) {
                                    dropStack(mc, p, j);
                                    junkDropped++;
                                    timeout = 200;
                                    wait = 2;
                                    return;
                                 }

                                 int[] mv = arrangeMove(p);
                                 if (mv != null && junkMoves < 40) {
                                    swapHotbar(mc, p, mv[0], mv[1]);
                                    junkMoves++;
                                    timeout = 200;
                                    wait = 3;
                                    return;
                                 }

                                 if (!Float.isNaN(junkXRot)) {
                                    p.setXRot(junkXRot);
                                    junkXRot = Float.NaN;
                                 }

                                 state = AutoSandOrders.S.MINING;
                                 junkCool = 100;
                                 cooldown = 10;
                                 startMining(p);
                                 if (junkDropped > 0 || junkMoves > 0) {
                                    msg(p, "Đã vứt " + junkDropped + " stack, xếp lại " + junkMoves + " ô thanh công cụ.");
                                 }
                           }
                        } else if (state == AutoSandOrders.S.BUY_DONE && boughtAny(p)) {
                           finishBuy(mc, p);
                        } else if (state == AutoSandOrders.S.WAIT_DELIVER) {
                           nextOrder(mc, p, "bấm vào mà không mở được bảng giao hàng");
                        } else {
                           boolean buying = state == AutoSandOrders.S.BUY_SHOP
                              || state == AutoSandOrders.S.BUY_GEAR
                              || state == AutoSandOrders.S.BUY_AMOUNT
                              || state == AutoSandOrders.S.BUY_CONFIRM
                              || state == AutoSandOrders.S.BUY_DONE;
                           if (buying && buyRetry < 2) {
                              retryBuy(mc, p, "hết thời gian chờ ở bước " + state);
                           } else {
                              abort(mc, "hết thời gian chờ ở bước " + state);
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         AutoSandOrders.Reconnect.tick(mc);
      }
   }

   static void startDeliver(Minecraft mc, LocalPlayer p) {
      stopMining(mc, p);
      send(p, "/" + ORDER_COMMAND);
      state = AutoSandOrders.S.WAIT_ORDERS;
      timeout = 200;
      clicks = 0;
      orderTry = 0;
      ordersWait = 0;
      settle = false;
      wait = 4;
   }

   static void nextOrder(Minecraft mc, LocalPlayer p, String why) {
      int tried = orderTry + 1;
      orderTry++;
      if (orderTry >= 15) {
         abort(mc, "đã thử " + tried + " đơn mà không đơn nào nhận cát");
      } else {
         msg(p, "Đơn ở ô " + orderSlot + ": " + why + ". Thử đơn kế tiếp (" + (orderTry + 1) + "/15).");
         if (screenBlocks(mc)) {
            p.closeContainer();
         }

         send(p, "/" + ORDER_COMMAND);
         state = AutoSandOrders.S.WAIT_ORDERS;
         timeout = 200;
         clicks = 0;
         ordersWait = 0;
         settle = false;
         wait = 12;
      }
   }

   static boolean sandMoved(LocalPlayer p) {
      return countItem(p, Items.SAND) < sandBefore;
   }

   static void startJunk(Minecraft mc, LocalPlayer p) {
      stopMining(mc, p);
      state = AutoSandOrders.S.JUNK;
      junkStep = 0;
      junkDropped = 0;
      junkMoves = 0;
      timeout = 200;
      wait = 4;
   }

   static void startEat(Minecraft mc, LocalPlayer p) {
      stopMining(mc, p);
      state = AutoSandOrders.S.EAT_PREP;
      timeout = 200;
      wait = 4;
   }

   static void startFoodBuy(Minecraft mc, LocalPlayer p) {
      stopMining(mc, p);
      msg(p, "Hết đồ ăn, đi mua thêm.");
      buyFood = true;
      buyPearl = false;
      buyTotem = false;
      buyRound = 0;
      buyTarget = 1;
      buyRetry = 0;
      gone = 0;
      send(p, "/" + SHOP_COMMAND);
      state = AutoSandOrders.S.BUY_SHOP;
      timeout = 200;
      wait = 4;
   }

   static int eatGoal() {
      return hpEating ? 20 : eatUntil;
   }

   static boolean healBeforePearl(Minecraft mc, LocalPlayer p) {
      if (autoEat && state == AutoSandOrders.S.MINING && !screenBlocks(mc)) {
         if (p.getFoodData().getFoodLevel() >= 20) {
            return false;
         } else if (findFood(p) >= 0) {
            hpEating = true;
            msg(p, "Máu thấp (" + (int)p.getHealth() + ") -> ăn cho đầy thanh đói để hồi máu.");
            startEat(mc, p);
            return true;
         } else if (autoBuyFood && emptySlot(p) >= 0) {
            hpEating = true;
            msg(p, "Máu thấp, hết đồ ăn -> mua cà rốt vàng rồi ăn để hồi máu.");
            startFoodBuy(mc, p);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   static void finishEat(Minecraft mc, LocalPlayer p, int cool) {
      hpEating = false;
      restoreLook(mc, p);
      state = AutoSandOrders.S.MINING;
      cooldown = cool;
      startMining(p);
   }

   static void restoreLook(Minecraft mc, LocalPlayer p) {
      mc.options.keyUse.setDown(false);
      if (eatOff && eatOffFrom >= 0 && mc.gameMode != null) {
         swapOffhand(mc, p, eatOffFrom);
      }

      eatOff = false;
      eatOffFrom = -1;
      if (!Float.isNaN(eatXRot)) {
         p.setXRot(eatXRot);
         eatXRot = Float.NaN;
      }

      if (!Float.isNaN(junkXRot)) {
         p.setXRot(junkXRot);
         junkXRot = Float.NaN;
      }

      if (eatPrevSlot >= 0 && eatPrevSlot < 9) {
         setSelected(p, eatPrevSlot);
      }

      eatPrevSlot = -1;
   }

   static void startMend(Minecraft mc, LocalPlayer p) {
      if (countItem(p, Items.EXPERIENCE_BOTTLE) == 0 && !autoBuy) {
         msg(p, "Xẻng gần hư nhưng hết bình XP (tự mua đang tắt).");
         cooldown = 1200;
      } else {
         stopMining(mc, p);
         xpSpent = 0.0;
         buyTries = 0;
         needBuy = false;
         step = 0;
         timeout = 200;
         wait = 4;
         if (countItem(p, Items.EXPERIENCE_BOTTLE) == 0) {
            buyTries = 1;
            beginBuy();
         } else {
            state = AutoSandOrders.S.MEND_PREP;
         }
      }
   }

   static boolean totemTick(Minecraft mc, LocalPlayer p) {
      if (shovelSwapped) {
         return false;
      } else {
         ItemStack off = p.getOffhandItem();
         if (off.is(Items.TOTEM_OF_UNDYING)) {
            totemWarn = false;
            return false;
         } else if (off.is(ItemTags.SHOVELS)) {
            return false;
         } else if (p.tickCount < 100) {
            return false;
         } else {
            int idx = findItem(p, Items.TOTEM_OF_UNDYING);
            if (idx >= 0) {
               swapOffhand(mc, p, idx);
               wait = 4;
               return true;
            } else if (totemCool > 0 || !autoBuy) {
               return false;
            } else if (emptySlot(p) < 0) {
               if (!totemWarn) {
                  totemWarn = true;
                  msg(p, "Hết totem nhưng túi không còn ô trống để mua (giao cát trước).");
               }

               return false;
            } else {
               startTotemBuy(mc, p);
               return true;
            }
         }
      }
   }

   static void startTotemBuy(Minecraft mc, LocalPlayer p) {
      stopMining(mc, p);
      msg(p, "Tay trái chưa có totem -> mua 1 cái ở /shop (GEAR).");
      buyFood = false;
      buyPearl = false;
      buyTotem = true;
      buyRound = 0;
      buyTarget = 1;
      buyRetry = 0;
      gone = 0;
      send(p, "/" + SHOP_COMMAND);
      state = AutoSandOrders.S.BUY_SHOP;
      timeout = 200;
      wait = 4;
   }

   static void beginBuy() {
      buyFood = false;
      buyPearl = false;
      buyTotem = false;
      buyResume = false;
      buyRedo = 0;
      junkDropped = 0;
      dropStep = 0;
      buyRound = 0;
      buyRetry = 0;
      state = AutoSandOrders.S.BUY_PREP;
      timeout = 200;
      wait = 4;
   }

   static void retryBuy(Minecraft mc, LocalPlayer p, String why) {
      buyRetry++;
      msg(p, "Mua chưa xong (" + why + "), thử lại lần " + buyRetry + "/2.");
      if (screenBlocks(mc)) {
         p.closeContainer();
      }

      send(p, "/" + SHOP_COMMAND);
      state = AutoSandOrders.S.BUY_SHOP;
      gone = 0;
      timeout = 200;
      wait = 12;
   }

   static void finishBuy(Minecraft mc, LocalPlayer p) {
      if (screenBlocks(mc)) {
         p.closeContainer();
      }

      if (buyTotem) {
         buyTotem = false;
         totemBuyFails = 0;
         msg(p, "Đã mua totem, hiện có " + countItem(p, Items.TOTEM_OF_UNDYING) + " trong túi.");
         state = AutoSandOrders.S.MINING;
         cooldown = 5;
         startMining(p);
      } else if (buyPearl) {
         buyPearl = false;
         msg(p, "Đã mua ngọc Ender, hiện có " + countItem(p, Items.ENDER_PEARL) + ".");
         state = AutoSandOrders.S.MINING;
         cooldown = 5;
         startMining(p);
      } else if (buyFood) {
         buyFood = false;
         foodBuyFails = 0;
         foodWarn = false;
         msg(p, "Đã mua đồ ăn, hiện có " + countFood(p) + ".");
         state = AutoSandOrders.S.EAT_PREP;
         timeout = 200;
         wait = 4;
      } else {
         msg(p, "Đã mua bình XP, hiện có " + countItem(p, Items.EXPERIENCE_BOTTLE) + " bình.");
         state = AutoSandOrders.S.MEND_PREP;
         step = 0;
         timeout = 200;
         wait = 4;
      }
   }

   static void finishMend(Minecraft mc, LocalPlayer p) {
      if (screenBlocks(mc)) {
         p.closeContainer();
      }

      boolean still = lowShovel(p) >= 0;
      buyTries = 0;
      needBuy = false;
      state = AutoSandOrders.S.MINING;
      cooldown = still ? 1200 : 60;
      startMining(p);
      msg(p, still ? "Xẻng vẫn còn thấp. Thử lại sau 1 phút." : "Đã sửa xong xẻng, tiếp tục đào.");
   }

   static void abort(Minecraft mc, String why) {
      LocalPlayer p = mc.player;
      hpEating = false;
      if (p != null) {
         if (screenBlocks(mc)) {
            p.closeContainer();
         }

         if (shovelSwapped && mc.gameMode != null) {
            swapOffhand(mc, p, shovelFrom);
            shovelSwapped = false;
         }

         if (!Float.isNaN(oldXRot)) {
            p.setXRot(oldXRot);
            oldXRot = Float.NaN;
         }

         if (!Float.isNaN(dropXRot)) {
            p.setXRot(dropXRot);
            dropXRot = Float.NaN;
         }

         restoreLook(mc, p);
         startMining(p);
         msg(p, "Huỷ: " + why);
         if (buyFood && ++foodBuyFails >= 3) {
            autoBuyFood = false;
            save();
            msg(p, "Mua đồ ăn lỗi 3 lần, đã tắt tự mua đồ ăn. Kiểm tra lại ô /shop -> FOOD.");
         }
      }

      if (buyPearl) {
         AutoMiner.pearlBuyFails++;
         buyPearl = false;
      }

      if (buyTotem) {
         buyTotem = false;
         totemCool = 1200;
         if (++totemBuyFails >= 3) {
            autoTotem = false;
            save();
            if (p != null) {
               msg(p, "Mua totem lỗi 3 lần, đã tắt tự totem. Kiểm tra lại /shop -> GEAR.");
            }
         }
      }

      buyFood = false;
      state = AutoSandOrders.S.MINING;
      cooldown = 200;
   }

   static void setNuker(LocalPlayer p, boolean on) {
      if (on && (nukerWater || AutoMiner.recovering())) {
         on = false;
      }

      if (nukerState == null || nukerState != on) {
         nukerState = on;
         send(p, on ? ".toggle nuker on" : ".toggle nuker off");
      }
   }

   static int bottleSlot(LocalPlayer p) {
      for (int i = 3; i < 9; i++) {
         if (p.getInventory().getItem(i).is(Items.EXPERIENCE_BOTTLE)) {
            return i;
         }
      }

      for (int ix = 0; ix < 3; ix++) {
         if (p.getInventory().getItem(ix).is(Items.EXPERIENCE_BOTTLE)) {
            return ix;
         }
      }

      return reserveWeapon ? 4 : 3;
   }

   static boolean isWeapon(ItemStack st) {
      return st.is(ItemTags.SWORDS) || st.is(ItemTags.AXES);
   }

   static boolean correctAt(int slot, ItemStack st) {
      if (st.isEmpty()) {
         return false;
      } else {
         return switch (slot) {
            case 0 -> st.is(ItemTags.SHOVELS);
            case 1 -> st.is(Items.GOLDEN_CARROT);
            case 2 -> st.is(Items.SAND);
            case 3 -> reserveWeapon ? isWeapon(st) : st.is(Items.EXPERIENCE_BOTTLE);
            default -> st.is(Items.EXPERIENCE_BOTTLE);
         };
      }
   }

   static boolean droppable(ItemStack st) {
      return !st.isEmpty()
         && !st.is(Items.EXPERIENCE_BOTTLE)
         && !st.is(ItemTags.SHOVELS)
         && !st.is(Items.GOLDEN_CARROT)
         && !st.is(Items.ENDER_PEARL)
         && !st.is(Items.TOTEM_OF_UNDYING)
         && !isWeapon(st)
         && !edible(st);
   }

   static int buyDropSlot(LocalPlayer p) {
      for (int i = 3; i < 9; i++) {
         if (droppable(p.getInventory().getItem(i))) {
            return i;
         }
      }

      return -1;
   }

   static int[] arrangeMove(LocalPlayer p) {
      Inventory inv = p.getInventory();
      reserveWeapon = false;

      for (int j = 0; j < 36 && !reserveWeapon; j++) {
         reserveWeapon = isWeapon(inv.getItem(j));
      }

      for (int i = 0; i < 9; i++) {
         if (!correctAt(i, inv.getItem(i))) {
            int src = -1;
            double bestPct = -1.0;

            for (int j = 0; j < 36; j++) {
               if (j != i) {
                  ItemStack st = inv.getItem(j);
                  if (correctAt(i, st) && (j >= 9 || !correctAt(j, st))) {
                     double pct = i == 0 ? remainingPct(st) : 0.0;
                     if (src < 0 || pct > bestPct + 0.001 || pct == bestPct && j >= 9 && src < 9) {
                        src = j;
                        bestPct = pct;
                     }
                  }
               }
            }

            if (src >= 0) {
               return new int[]{src, i};
            }
         }
      }

      return null;
   }

   static void beginPearlBuy(Minecraft mc, LocalPlayer p, int need) {
      stopMining(mc, p);
      msg(p, "Chết xa -> mua ngọc Ender ở /shop để ném tới chỗ chết.");
      buyFood = false;
      buyPearl = true;
      buyTotem = false;
      pearlNeed = Math.max(1, need);
      buyRound = 0;
      buyTarget = Math.max(1, Math.min(4, need));
      buyRetry = 0;
      gone = 0;
      send(p, "/" + SHOP_COMMAND);
      state = AutoSandOrders.S.BUY_SHOP;
      timeout = 200;
      wait = 4;
   }

   static String fmtTime(long ticks) {
      long s = ticks / 20L;
      return String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600L, s / 60L % 60L, s % 60L);
   }

   static String fmtMoney() {
      return String.format(Locale.ROOT, "%,.0f $", statSand * pricePerSand);
   }

   static int menuSlot(int invIdx) {
      return invIdx < 9 ? invIdx + 36 : invIdx;
   }

   static void swapOffhand(Minecraft mc, LocalPlayer p, int invIdx) {
      mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, menuSlot(invIdx), 40, ClickType.SWAP, p);
   }

   static void swapHotbar(Minecraft mc, LocalPlayer p, int invIdx, int hotbar) {
      mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, menuSlot(invIdx), hotbar, ClickType.SWAP, p);
   }

   static void dropStack(Minecraft mc, LocalPlayer p, int invIdx) {
      ItemStack st = p.getInventory().getItem(invIdx);
      if (!st.isEmpty()) {
         if (droppable(st)) {
            mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, menuSlot(invIdx), 1, ClickType.THROW, p);
         }
      }
   }

   static void setSelected(LocalPlayer p, int slot) {
      p.getInventory().setSelectedSlot(slot);
   }

   static int getSelected(LocalPlayer p) {
      return p.getInventory().getSelectedSlot();
   }

   static int findItem(LocalPlayer p, Item item) {
      for (int i = 0; i < 36; i++) {
         if (p.getInventory().getItem(i).is(item)) {
            return i;
         }
      }

      return -1;
   }

   static int countItem(LocalPlayer p, Item item) {
      int n = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack st = p.getInventory().getItem(i);
         if (st.is(item)) {
            n += st.getCount();
         }
      }

      return n;
   }

   static boolean keepItem(ItemStack st) {
      return st.is(Items.SAND)
         || st.is(Items.EXPERIENCE_BOTTLE)
         || st.is(ItemTags.SHOVELS)
         || st.is(ItemTags.SWORDS)
         || st.is(ItemTags.AXES)
         || st.is(Items.GOLDEN_CARROT)
         || st.is(Items.ENDER_PEARL)
         || st.is(Items.TOTEM_OF_UNDYING)
         || !extraKeep.isEmpty() && extraKeep.contains(BuiltInRegistries.ITEM.getKey(st.getItem()).toString());
   }

   static int junkSlot(LocalPlayer p) {
      for (int i = 0; i < 36; i++) {
         ItemStack st = p.getInventory().getItem(i);
         if (!st.isEmpty() && !keepItem(st) && !edible(st)) {
            return i;
         }
      }

      return -1;
   }

   static boolean edible(ItemStack st) {
      return !st.isEmpty() && st.has(DataComponents.FOOD)
         ? !st.is(Items.ROTTEN_FLESH)
            && !st.is(Items.SPIDER_EYE)
            && !st.is(Items.POISONOUS_POTATO)
            && !st.is(Items.PUFFERFISH)
            && !st.is(Items.GOLDEN_APPLE)
            && !st.is(Items.ENCHANTED_GOLDEN_APPLE)
            && !st.is(Items.CHORUS_FRUIT)
            && !st.is(Items.SUSPICIOUS_STEW)
         : false;
   }

   static int findFood(LocalPlayer p) {
      int any = -1;

      for (int i = 0; i < 36; i++) {
         ItemStack st = p.getInventory().getItem(i);
         if (edible(st)) {
            if (st.is(Items.GOLDEN_CARROT)) {
               return i;
            }

            if (any < 0) {
               any = i;
            }
         }
      }

      return any;
   }

   static int countFood(LocalPlayer p) {
      int n = 0;

      for (int i = 0; i < 36; i++) {
         ItemStack st = p.getInventory().getItem(i);
         if (edible(st)) {
            n += st.getCount();
         }
      }

      return n;
   }

   static int boughtCount(LocalPlayer p) {
      if (buyPearl) {
         return countItem(p, Items.ENDER_PEARL);
      } else if (buyTotem) {
         return countItem(p, Items.TOTEM_OF_UNDYING);
      } else {
         return buyFood ? countFood(p) : countItem(p, Items.EXPERIENCE_BOTTLE);
      }
   }

   static boolean boughtAny(LocalPlayer p) {
      return boughtCount(p) > 0;
   }

   static int emptySlot(LocalPlayer p) {
      for (int i = 0; i < 36; i++) {
         if (p.getInventory().getItem(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   static int toHotbar(Minecraft mc, LocalPlayer p, int idx) {
      if (idx < 9) {
         return idx;
      } else {
         int target = 8;

         for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).isEmpty()) {
               target = i;
               break;
            }
         }

         swapHotbar(mc, p, idx, target);
         return target;
      }
   }

   static int hotbarSandSlot(LocalPlayer p) {
      for (int i = 0; i < 9; i++) {
         if (p.getInventory().getItem(i).is(Items.SAND)) {
            return i;
         }
      }

      return -1;
   }

   static int hotbarMissing(LocalPlayer p) {
      int need = 0;

      for (int i = 3; i < 9; i++) {
         ItemStack st = p.getInventory().getItem(i);
         if (st.isEmpty()) {
            need += 64;
         } else if (st.is(Items.EXPERIENCE_BOTTLE)) {
            need += st.getMaxStackSize() - st.getCount();
         }
      }

      return need;
   }

   static boolean isFull(LocalPlayer p) {
      for (int i = 0; i < 36; i++) {
         ItemStack st = p.getInventory().getItem(i);
         if (st.isEmpty()) {
            return false;
         }

         if (st.is(Items.SAND) && st.getCount() < st.getMaxStackSize()) {
            return false;
         }
      }

      return true;
   }

   static double remainingPct(ItemStack st) {
      int max = st.getMaxDamage();
      return max <= 0 ? 100.0 : (max - st.getDamageValue()) * 100.0 / max;
   }

   static int lowShovel(LocalPlayer p) {
      int best = -1;
      double bestPct = 101.0;

      for (int i = 0; i < 36; i++) {
         ItemStack st = p.getInventory().getItem(i);
         if (!st.isEmpty() && st.is(ItemTags.SHOVELS) && st.isDamageableItem()) {
            double pct = remainingPct(st);
            if (pct <= mendPct && pct < bestPct) {
               best = i;
               bestPct = pct;
            }
         }
      }

      return best;
   }

   static ChestMenu container(Minecraft mc) {
      return mc.screen instanceof AbstractContainerScreen<?> sc && sc.getMenu() instanceof ChestMenu m ? m : null;
   }

   static void click(Minecraft mc, LocalPlayer p, ChestMenu h, int slot, ClickType type) {
      mc.gameMode.handleInventoryMouseClick(h.containerId, slot, 0, type, p);
   }

   static boolean loaded(ChestMenu h) {
      int n = h.getRowCount() * 9;

      for (int i = 0; i < n && i < h.slots.size(); i++) {
         if (!((Slot)h.slots.get(i)).getItem().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   static int find(ChestMenu h, Predicate<ItemStack> pr, int skip) {
      int n = h.getRowCount() * 9;

      for (int i = 0; i < n && i < h.slots.size(); i++) {
         if (i != skip) {
            ItemStack st = ((Slot)h.slots.get(i)).getItem();
            if (!st.isEmpty() && pr.test(st)) {
               return i;
            }
         }
      }

      return -1;
   }

   static String norm(String s) {
      StringBuilder sb = new StringBuilder(s.length());

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         int k = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ".indexOf(c);
         sb.append(k >= 0 ? (char)(97 + k) : c);
      }

      String t = Normalizer.normalize(sb.toString().toLowerCase(Locale.ROOT), Form.NFD);
      return t.replaceAll("\\p{M}+", "").replace('đ', 'd');
   }

   static boolean nameHas(ItemStack st, String... keys) {
      if (st.isEmpty()) {
         return false;
      } else {
         String n = norm(st.getHoverName().getString());

         for (String k : keys) {
            if (n.contains(norm(k))) {
               return true;
            }
         }

         return false;
      }
   }

   static String dump(ChestMenu h) {
      StringBuilder sb = new StringBuilder();
      int n = h.getRowCount() * 9;

      for (int i = 0; i < n && i < h.slots.size(); i++) {
         ItemStack st = ((Slot)h.slots.get(i)).getItem();
         if (!st.isEmpty()) {
            sb.append(i).append('=').append(st.getHoverName().getString()).append(" | ");
         }
      }

      return sb.toString();
   }

   static double parseMoney(String text) {
      Matcher m = MONEY_RE.matcher(text);
      if (!m.find()) {
         return 0.0;
      } else {
         String n = m.group(1);

         while (n.endsWith(".") || n.endsWith(",")) {
            n = n.substring(0, n.length() - 1);
         }

         if (n.isEmpty()) {
            return 0.0;
         } else {
            int lc = n.lastIndexOf(44);
            int ld = n.lastIndexOf(46);
            if (lc >= 0 && ld >= 0) {
               n = lc > ld ? n.replace(".", "").replace(',', '.') : n.replace(",", "");
            } else if (lc >= 0) {
               n = n.length() - lc - 1 == 3 ? n.replace(",", "") : n.replace(',', '.');
            } else if (ld >= 0 && n.indexOf(46) != ld) {
               n = n.replace(".", "");
            }

            double v;
            try {
               v = Double.parseDouble(n);
            } catch (NumberFormatException var8) {
               return 0.0;
            }

            String suf = m.group(2);
            if (suf != null) {
               switch (Character.toLowerCase(suf.charAt(0))) {
                  case 'b':
                     v *= 1.0E9;
                     break;
                  case 'k':
                     v *= 1000.0;
                     break;
                  case 'm':
                     v *= 1000000.0;
               }
            }

            return v;
         }
      }
   }

   static List<String> loreLines(ItemStack st) {
      List<String> out = new ArrayList<>();
      if (st.isEmpty()) {
         return out;
      } else {
         ItemLore lore = (ItemLore)st.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);

         for (Component c : lore.lines()) {
            out.add(c.getString());
         }

         return out;
      }
   }

   static double lorePrice(ItemStack st) {
      for (String line : loreLines(st)) {
         if (line.contains("$") || PRICE_WORD.matcher(norm(line)).find()) {
            double v = parseMoney(line);
            if (v > 0.0) {
               return v;
            }
         }
      }

      return 0.0;
   }

   static double menuCost(ChestMenu h) {
      double best = 0.0;
      int n = h.getRowCount() * 9;

      for (int i = 0; i < n && i < h.slots.size(); i++) {
         best = Math.max(best, lorePrice(((Slot)h.slots.get(i)).getItem()));
      }

      return best;
   }

   static String overBudget(double cost) {
      if (cost <= 0.0) {
         return null;
      } else {
         if (buyTotem) {
            if (maxTotemUnitPrice > 0.0 && cost > maxTotemUnitPrice) {
               return "giá totem " + cost + " vượt giới hạn " + maxTotemUnitPrice;
            }

            if (maxTotemSpend > 0.0 && totemSpent + cost > maxTotemSpend) {
               return "tổng chi totem sẽ vượt giới hạn " + maxTotemSpend;
            }
         } else if (!buyFood && !buyPearl) {
            double unit = cost / 64.0;
            if (maxXpUnitPrice > 0.0 && unit > maxXpUnitPrice) {
               return "giá bình XP " + String.format(Locale.ROOT, "%.2f", unit) + " vượt giới hạn " + maxXpUnitPrice;
            }

            if (maxXpSpend > 0.0 && xpSpent + cost > maxXpSpend) {
               return "tổng chi sửa đồ sẽ vượt giới hạn " + maxXpSpend;
            }
         }

         return null;
      }
   }

   static int startTest(String kind) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return 0;
      } else if (enabled) {
         msg(mc.player, "Tắt bot trước khi chạy test.");
         return 0;
      } else {
         testKind = kind;
         testTicks = 0;
         msg(mc.player, "Test " + kind + ": đang mở bảng...");
         return 1;
      }
   }

   static void testTick(Minecraft mc, LocalPlayer p) {
      if (!enabled && p != null) {
         if (testTicks == 0) {
            send(p, "/" + (testKind.equals("orders") ? ORDER_COMMAND : SHOP_COMMAND));
         }

         testTicks++;
         ChestMenu h = container(mc);
         if (h != null && testTicks > 10 && loaded(h)) {
            msg(p, "[Test " + testKind + "] bảng " + h.getRowCount() + " hàng. Các ô: " + dump(h));
            int shown = 0;
            int n = h.getRowCount() * 9;

            for (int i = 0; i < n && i < h.slots.size() && shown < 4; i++) {
               ItemStack st = ((Slot)h.slots.get(i)).getItem();
               boolean want = testKind.equals("orders") ? st.is(Items.SAND) : !loreLines(st).isEmpty();
               if (want) {
                  shown++;
                  msg(
                     p,
                     "  ô " + i + " " + st.getHoverName().getString() + " | giá đọc được = " + lorePrice(st) + " | lore: " + String.join(" / ", loreLines(st))
                  );
               }
            }

            p.closeContainer();
            testKind = null;
         } else {
            if (testTicks > 100) {
               if (screenBlocks(mc)) {
                  p.closeContainer();
               }

               msg(p, "[Test " + testKind + "] không mở được bảng sau 5 giây (kiểm tra lệnh /autosand cmd | shop).");
               testKind = null;
            }
         }
      } else {
         testKind = null;
      }
   }

   static int weaponSlot(LocalPlayer p) {
      int axe = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack st = p.getInventory().getItem(i);
         if (st.is(ItemTags.SWORDS)) {
            return i;
         }

         if (axe < 0 && st.is(ItemTags.AXES)) {
            axe = i;
         }
      }

      return axe;
   }

   static void endDefend(Minecraft mc, LocalPlayer p) {
      mc.options.keyUp.setDown(false);
      if (defendPrevSlot >= 0) {
         setSelected(p, defendPrevSlot);
         defendPrevSlot = -1;
      }

      defendTicks = 0;
      defendIdle = 0;
      defendAttackWait = 0;
      defendAimWait = 0;
   }

   static boolean defendTick(Minecraft mc, LocalPlayer p) {
      ClientLevel lv = mc.level;
      if (lv == null || mc.gameMode == null) {
         return false;
      } else if (defendCool > 0) {
         defendCool--;
         return false;
      } else if (!shovelSwapped && !p.isUsingItem() && !p.isInWater() && !p.isInLava()) {
         LivingEntity best = null;
         double bestD = Double.MAX_VALUE;

         for (LivingEntity e : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(defenseRadius))) {
            if (e != p && e.isAlive() && !(e instanceof Player) && e instanceof Enemy && p.hasLineOfSight(e)) {
               double d = p.distanceToSqr(e);
               if (d < bestD) {
                  bestD = d;
                  best = e;
               }
            }
         }

         if (best == null) {
            if (defendPrevSlot >= 0 && ++defendIdle > 10) {
               endDefend(mc, p);
            }

            return false;
         } else {
            int w = weaponSlot(p);
            if (w < 0) {
               return false;
            } else {
               defendIdle = 0;
               if (++defendTicks > 300) {
                  endDefend(mc, p);
                  defendCool = 300;
                  return false;
               } else {
                  if (defendPrevSlot < 0) {
                     defendPrevSlot = getSelected(p);
                  }

                  if (getSelected(p) != w) {
                     setSelected(p, w);
                     return true;
                  } else {
                     mc.options.keyAttack.setDown(false);
                     if (defendAttackWait > 0) {
                        defendAttackWait--;
                     }

                     AutoMiner.aimAt(p, best.getBoundingBox().getCenter());
                     double dist = Math.sqrt(bestD);
                     mc.options.keyUp.setDown(dist > 2.7);
                     if (dist <= 2.9) {
                        if (defendAimWait < 2) {
                           defendAimWait++;
                           return true;
                        }

                        if (defendAttackWait == 0 && p.getAttackStrengthScale(0.5F) >= 0.9F && p.hasLineOfSight(best) && best.isAlive()) {
                           mc.gameMode.attack(p, best);
                           p.swing(InteractionHand.MAIN_HAND);
                           defendAttackWait = 10;
                           defendAimWait = 0;
                        }
                     } else {
                        defendAimWait = 0;
                     }

                     return true;
                  }
               }
            }
         }
      } else {
         return false;
      }
   }

   static void perfTick(Minecraft mc) {
      try {
         boolean want = lowLoad && enabled && mc.player != null;
         if (!want) {
            restorePerf(mc);
            return;
         }

         if (origFps == null) {
            origFps = (Integer)mc.options.framerateLimit().get();
            origView = (Integer)mc.options.renderDistance().get();
         }

         int fps = mc.isWindowActive() ? fgFps : bgFps;
         if ((Integer)mc.options.framerateLimit().get() != fps) {
            mc.options.framerateLimit().set(fps);
         }

         if ((Integer)mc.options.renderDistance().get() != lowLoadView) {
            mc.options.renderDistance().set(lowLoadView);
         }
      } catch (Throwable var3) {
      }
   }

   static void restorePerf(Minecraft mc) {
      if (origFps != null) {
         try {
            mc.options.framerateLimit().set(origFps);
            mc.options.renderDistance().set(origView);
         } catch (Throwable var2) {
         }

         origFps = null;
         origView = null;
      }
   }

   static String keepEdit(String raw, boolean add) {
      String id = raw.trim().toLowerCase(Locale.ROOT);
      if (id.isEmpty()) {
         return "Thiếu id vật phẩm (vd. minecraft:diamond).";
      } else {
         if (!id.contains(":")) {
            id = "minecraft:" + id;
         }

         if (!add) {
            return extraKeep.remove(id) ? saveMsg("Đã bỏ giữ " + id) : "Không có " + id + " trong danh sách giữ thêm.";
         } else {
            boolean exists = false;

            for (Item it : BuiltInRegistries.ITEM) {
               if (BuiltInRegistries.ITEM.getKey(it).toString().equals(id)) {
                  exists = true;
                  break;
               }
            }

            if (!exists) {
               return "Không có vật phẩm \"" + id + "\".";
            } else {
               extraKeep.add(id);
               return saveMsg("Đã giữ thêm " + id + " (sẽ không bị vứt).");
            }
         }
      }
   }

   static String saveMsg(String m) {
      save();
      return m;
   }

   static int nextInt(int[] arr, int cur) {
      for (int v : arr) {
         if (v > cur) {
            return v;
         }
      }

      return arr[0];
   }

   static double nextDbl(double[] arr, double cur) {
      for (double v : arr) {
         if (v > cur + 1.0E-4) {
            return v;
         }
      }

      return arr[0];
   }

   static void send(LocalPlayer p, String s) {
      if (s.startsWith("/")) {
         p.connection.sendCommand(s.substring(1));
      } else {
         p.connection.sendChat(s);
      }
   }

   static void msg(LocalPlayer p, String t) {
      p.displayClientMessage(Component.literal("[AutoSand] " + t), false);
   }

   static boolean passable(ClientLevel lv, BlockPos pos) {
      return lv.getBlockState(pos).getCollisionShape(lv, pos).isEmpty();
   }

   static boolean liquidAt(ClientLevel lv, BlockPos pos) {
      FluidState fs = lv.getBlockState(pos).getFluidState();
      return fs.is(FluidTags.WATER) || fs.is(FluidTags.LAVA);
   }

   static boolean trapped(ClientLevel lv, BlockPos f) {
      if (passable(lv, f.above()) && passable(lv, f.above(2))) {
         for (Direction d : HORIZ) {
            BlockPos n = f.relative(d);
            boolean feet = passable(lv, n);
            boolean head = passable(lv, n.above());
            if (feet && head) {
               return false;
            }

            if (!feet && head && passable(lv, n.above(2))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   static boolean lavaAt(ClientLevel lv, BlockPos pos) {
      return lv.getBlockState(pos).getFluidState().is(FluidTags.LAVA);
   }

   static boolean deepWater(ClientLevel lv, BlockPos a) {
      return liquidAt(lv, a) && liquidAt(lv, a.below()) || liquidAt(lv, a.below()) && liquidAt(lv, a.below(2));
   }

   static boolean danger(LocalPlayer p) {
      if (p.isInLava()) {
         return true;
      } else {
         if (p.isInWater()) {
            FluidState fs = p.level().getFluidState(p.blockPosition());
            if (!fs.isEmpty() && !fs.isSource()) {
               return true;
            }
         }

         return p.isInWater() && (p.isUnderWater() || !p.onGround());
      }
   }

   static boolean caveStep(ClientLevel lv, BlockPos from, BlockPos to, boolean airOnly) {
      if (!airOnly) {
         return false;
      } else {
         int ln = lv.getBrightness(LightLayer.SKY, to);
         return ln <= 7 && ln < lv.getBrightness(LightLayer.SKY, from);
      }
   }

   static boolean hazardDir(ClientLevel lv, double x, double y, double z, double dx, double dz) {
      for (double k : new double[]{1.0, 1.8}) {
         BlockPos a = BlockPos.containing(x + dx * k, y + 0.1, z + dz * k);
         if (avoidLiquid && (lavaAt(lv, a) || lavaAt(lv, a.below()) || deepWater(lv, a))) {
            return true;
         }

         if (avoidHole && passable(lv, a) && passable(lv, a.above()) && caveStep(lv, BlockPos.containing(x, y + 0.1, z), a, true)) {
            return true;
         }

         if (avoidHole && passable(lv, a)) {
            boolean drop = true;

            for (int i = 1; i <= 3; i++) {
               if (!passable(lv, a.below(i))) {
                  drop = false;
                  break;
               }
            }

            if (drop) {
               return true;
            }
         }
      }

      return false;
   }

   static BlockPos findShore(ClientLevel lv, LocalPlayer p) {
      BlockPos o = p.blockPosition();
      BlockPos best = null;
      double bestD = Double.MAX_VALUE;

      for (int dx = -10; dx <= 10; dx++) {
         for (int dz = -10; dz <= 10; dz++) {
            double d = dx * dx + dz * dz;
            if (!(d >= bestD)) {
               for (int dy = 2; dy >= -2; dy--) {
                  BlockPos f = o.offset(dx, dy, dz);
                  BlockPos g = f.below();
                  if (!lavaAt(lv, f)
                     && !lavaAt(lv, f.above())
                     && !lavaAt(lv, g)
                     && !liquidAt(lv, f)
                     && !liquidAt(lv, f.above())
                     && passable(lv, f)
                     && passable(lv, f.above())
                     && !passable(lv, g)) {
                     best = f;
                     bestD = d;
                     break;
                  }
               }
            }
         }
      }

      return best;
   }

   static boolean placeable(ItemStack st) {
      return !st.isEmpty() && (st.is(Items.SAND) || st.getItem() instanceof BlockItem);
   }

   static int findPlaceable(LocalPlayer p) {
      int idx = findItem(p, Items.SAND);
      if (idx >= 0) {
         return idx;
      } else {
         for (int i = 0; i < 36; i++) {
            if (placeable(p.getInventory().getItem(i))) {
               return i;
            }
         }

         return -1;
      }
   }

   static int hotbarPlaceSlot(LocalPlayer p) {
      int s = hotbarSandSlot(p);
      if (s >= 0) {
         return s;
      } else {
         for (int i = 0; i < 9; i++) {
            if (placeable(p.getInventory().getItem(i))) {
               return i;
            }
         }

         return -1;
      }
   }

   static void guardReset(Minecraft mc, LocalPlayer p) {
      if ((guard == AutoSandOrders.G.ESCAPE || guard == AutoSandOrders.G.BRIDGE) && p != null && mc.gameMode != null) {
         if (escSwapIdx >= 0) {
            swapHotbar(mc, p, escSwapIdx, escSlot);
            escSwapIdx = -1;
         }

         setSelected(p, escPrevSel);
         if (!Float.isNaN(escPitch)) {
            p.setXRot(escPitch);
            escPitch = Float.NaN;
         }
      }

      guard = AutoSandOrders.G.NONE;
      mc.options.keyJump.setDown(false);
      trapTicks = 0;
      guardCool = 0;
      wdTick = 0;
      wdX = Double.NaN;
      pickTarget = null;
      wdIdle = 0;
      wdGap = 100;
   }

   static void endGuard(Minecraft mc, LocalPlayer p) {
      mc.options.keyJump.setDown(false);
      releaseKeys(mc);
      guard = AutoSandOrders.G.NONE;
      guardCool = 60;
      wetTicks = 0;
      trapTicks = 0;
      wdTick = 0;
      wdX = Double.NaN;
      startMining(p);
   }

   static boolean guardTick(Minecraft mc, LocalPlayer p) {
      ClientLevel lv = mc.level;
      if (lv == null) {
         return false;
      } else if (guard == AutoSandOrders.G.LIQUID) {
         tickLiquid(mc, p, lv);
         return true;
      } else if (guard == AutoSandOrders.G.ESCAPE) {
         tickEscape(mc, p, lv);
         return true;
      } else if (guard == AutoSandOrders.G.SIDE) {
         tickSide(mc, p, lv);
         return true;
      } else if (guard == AutoSandOrders.G.PICKUP) {
         tickPickup(mc, p, lv);
         return true;
      } else if (guard == AutoSandOrders.G.BRIDGE) {
         tickBridge(mc, p, lv);
         return true;
      } else if (guard == AutoSandOrders.G.UNSTICK) {
         tickUnstick(mc, p, lv);
         return true;
      } else {
         if (guardCool > 0) {
            guardCool--;
         }

         if (pickCool > 0) {
            pickCool--;
         }

         if (sideLock > 0) {
            sideLock--;
         }

         if (sideWindow > 0 && --sideWindow == 0) {
            sideStrikes = 0;
         }

         if (avoidLiquid && danger(p)) {
            wetTicks++;
            int need = p.isInLava() ? 3 : (mode == AutoSandOrders.Mode.BUILTIN ? 2 : 15);
            if (wetTicks >= need) {
               wetTicks = 0;
               stopMining(mc, p);
               guard = AutoSandOrders.G.LIQUID;
               guardTicks = 0;
               shore = null;
               msg(p, p.isInLava() ? "Đang ở trong lava -> tự thoát lên bờ." : "Kẹt trong nước sâu -> tự bơi lên bờ.");
               return true;
            }
         } else {
            wetTicks = 0;
         }

         if (escCool > 0) {
            escCool--;
         }

         boolean shaftDigging = AutoMiner.shaft && !AutoMiner.shaftBlocked;
         boolean nukerWait = AutoMiner.holdingForNuker(lv);
         boolean tight = avoidHole && !shaftDigging && !nukerWait && p.onGround() && trapped(lv, p.blockPosition());
         boolean pocket = avoidHole && !shaftDigging && !nukerWait && !tight && p.onGround() && escCool <= 0 && pocketStuck(mc, p, lv);
         if ((tight || pocket) && escCool <= 0) {
            if (pocket || ++trapTicks >= 4) {
               trapTicks = 0;
               idleAcc = 0;
               encCache = false;
               if (startEscape(mc, p)) {
                  return true;
               }
            }
         } else {
            trapTicks = 0;
         }

         if (jamCheck(mc, p, lv)) {
            return true;
         } else {
            if (bridgeCool > 0) {
               bridgeCool--;
            }

            if (avoidLiquid && bridgeWater && bridgeCool <= 0 && p.onGround() && surroundedByWater(lv, p.blockPosition())) {
               if (++surroundTicks >= 20) {
                  surroundTicks = 0;
                  if (startBridge(mc, p)) {
                     return true;
                  }
               }
            } else {
               surroundTicks = 0;
            }

            if (mode != AutoSandOrders.Mode.BUILTIN && (avoidLiquid || avoidHole) && guardCool <= 0 && sideLock <= 0 && p.onGround()) {
               Vec3 v = p.getDeltaMovement();
               double h = Math.hypot(v.x, v.z);
               if (h > 0.08 && hazardDir(lv, p.getX(), p.getY(), p.getZ(), v.x / h, v.z / h)) {
                  startSide(mc, p, lv, Math.toDegrees(Math.atan2(-v.x, v.z)));
                  return true;
               }
            }

            if (mode != AutoSandOrders.Mode.BUILTIN && pickupRadius > 0 && pickCool <= 0 && guardCool <= 0 && p.onGround() && !isFull(p)) {
               ItemEntity it = nearestSand(lv, p);
               if (it != null) {
                  startPickup(mc, p, it);
                  return true;
               }
            }

            if (autoRevive && mode != AutoSandOrders.Mode.BUILTIN && useNuker && ++wdTick >= 20) {
               wdTick = 0;
               double moved = Double.isNaN(wdX) ? 99.0 : Math.hypot(p.getX() - wdX, p.getZ() - wdZ);
               wdX = p.getX();
               wdZ = p.getZ();
               int sandNow = countItem(p, Items.SAND);
               boolean working = moved > 0.15 || mc.gameMode.isDestroying() || sandNow != wdSand || wait > 0;
               wdSand = sandNow;
               if (working) {
                  wdIdle = 0;
                  wdGap = 100;
                  wdFails = 0;
               } else {
                  wdIdle += 20;
                  if (wdIdle >= wdGap) {
                     wdIdle = 0;
                     wdFails++;
                     wdGap = Math.min(400, wdGap * 2);
                     if (wdFails <= 2 || wdFails % 5 == 0) {
                        msg(p, "Nuker có vẻ đã dừng -> bật lại (lần " + wdFails + ").");
                     }

                     if (wdFails % 3 == 0) {
                        stopMining(mc, p);
                     }

                     startMining(p);
                  }
               }
            }

            return false;
         }
      }
   }

   static boolean enclosed(ClientLevel lv, BlockPos start) {
      int radius = 7;
      int limit = 700;
      ArrayDeque<BlockPos> queue = new ArrayDeque<>();
      Set<Long> seen = new HashSet<>();
      queue.add(start);
      seen.add(start.asLong());
      int count = 0;

      while (!queue.isEmpty()) {
         BlockPos c = queue.poll();
         if (++count > 700) {
            return false;
         }

         for (Direction d : HORIZ) {
            BlockPos n = c.relative(d);
            if (Math.abs(n.getX() - start.getX()) > 7 || Math.abs(n.getZ() - start.getZ()) > 7) {
               return false;
            }

            BlockPos land = null;
            if (passable(lv, n) && passable(lv, n.above())) {
               if (liquidAt(lv, n) || liquidAt(lv, n.below())) {
                  continue;
               }

               BlockPos m = n;

               for (int fall = 0; passable(lv, m.below()) && fall < 3; fall++) {
                  m = m.below();
               }

               if (!passable(lv, m.below())) {
                  land = m;
               }
            } else if (!passable(lv, n) && passable(lv, n.above()) && passable(lv, n.above(2)) && passable(lv, c.above(2))) {
               VoxelShape sh = lv.getBlockState(n).getCollisionShape(lv, n);
               if (!sh.isEmpty() && sh.max(Axis.Y) <= 1.0001 && !liquidAt(lv, n.above())) {
                  land = n.above();
               }
            }

            if (land != null && seen.add(land.asLong())) {
               queue.add(land);
            }
         }
      }

      return true;
   }

   static boolean pocketStuck(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      if (wait <= 0 && cooldown <= 0 && !p.isInWater() && !p.isInLava() && (mode != AutoSandOrders.Mode.BUILTIN || !AutoMiner.recovering())) {
         if (mc.gameMode != null && mc.gameMode.isDestroying()) {
            idleWorked = true;
         }

         if (++idleTick >= 20) {
            idleTick = 0;
            double moved = Double.isNaN(idleX) ? 99.0 : Math.hypot(p.getX() - idleX, p.getZ() - idleZ);
            idleX = p.getX();
            idleZ = p.getZ();
            int sandNow = countItem(p, Items.SAND);
            boolean worked = idleWorked || moved > 1.5 || sandNow != idleSand;
            idleWorked = false;
            idleSand = sandNow;
            if (worked) {
               idleAcc = 0;
               encCache = false;
            } else {
               idleAcc += 20;
               if (idleAcc >= 60) {
                  encCache = enclosed(lv, p.blockPosition());
               }
            }
         }

         return idleAcc >= 60 && encCache;
      } else {
         idleAcc = 0;
         idleTick = 0;
         encCache = false;
         return false;
      }
   }

   static int freeRun(ClientLevel lv, LocalPlayer p, float yaw) {
      double r = Math.toRadians(yaw);
      double dx = -Math.sin(r);
      double dz = Math.cos(r);
      int y = p.blockPosition().getY();
      int run = 0;

      for (int k = 1; k <= 6; k++) {
         BlockPos a = BlockPos.containing(p.getX() + dx * k, y + 0.1, p.getZ() + dz * k);
         if (!passable(lv, a) || !passable(lv, a.above())) {
            if (passable(lv, a) || !passable(lv, a.above()) || !passable(lv, a.above(2))) {
               break;
            }

            a = a.above();
            y++;
         }

         if (lavaAt(lv, a) || lavaAt(lv, a.below()) || deepWater(lv, a) || passable(lv, a.below()) && passable(lv, a.below(2)) && passable(lv, a.below(3))) {
            break;
         }

         run++;
      }

      return run;
   }

   static boolean jamCheck(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      if (unWindow > 0 && --unWindow == 0) {
         unStrikes = 0;
      }

      if (unLock > 0) {
         unLock--;
         jamTick = 0;
         return false;
      } else if (mode != AutoSandOrders.Mode.NUKER && mc.options.keyUp.isDown() && !p.isInWater() && !p.isInLava() && wait <= 0) {
         if (jamTick == 0) {
            jamX = p.getX();
            jamZ = p.getZ();
            jamHit = 0;
            jamSand = countItem(p, Items.SAND);
         }

         jamTick++;
         if (p.horizontalCollision) {
            jamHit++;
         }

         int win = mode == AutoSandOrders.Mode.BUILTIN ? 60 : 40;
         if (jamTick < win) {
            return false;
         } else {
            jamTick = 0;
            boolean jam = Math.hypot(p.getX() - jamX, p.getZ() - jamZ) < 0.6 && jamHit >= win / 2 && countItem(p, Items.SAND) <= jamSand;
            if (!jam) {
               return false;
            } else if (startUnstick(mc, p, lv)) {
               return true;
            } else if (avoidHole && escCool <= 0 && enclosed(lv, p.blockPosition()) && startEscape(mc, p)) {
               return true;
            } else {
               unLock = 100;
               return false;
            }
         }
      } else {
         jamTick = 0;
         jamHit = 0;
         return false;
      }
   }

   static boolean startUnstick(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      float base = p.getYRot();
      float bestYaw = base;
      int best = -1;

      for (float off : new float[]{90.0F, -90.0F, 180.0F, 135.0F, -135.0F, 45.0F, -45.0F}) {
         int run = freeRun(lv, p, base + off);
         if (run > best) {
            best = run;
            bestYaw = base + off;
         }
      }

      if (best < 2) {
         return false;
      } else if (++unStrikes >= 4) {
         unStrikes = 0;
         unWindow = 0;
         unLock = 200;
         msg(p, "Đâm tường nhiều lần liên tiếp -> tạm nghỉ thoát tường 10 giây.");
         return false;
      } else {
         if (unWindow <= 0) {
            unWindow = 900;
         }

         if (unStrikes == 1) {
            msg(p, "Đâm vào tường -> quay sang hướng khác.");
         }

         stopMining(mc, p);
         unYaw = bestYaw;
         unX = p.getX();
         unZ = p.getZ();
         unTicks = 0;
         guard = AutoSandOrders.G.UNSTICK;
         return true;
      }
   }

   static void tickUnstick(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      unTicks++;
      p.setYRot(unYaw);
      double r = Math.toRadians(unYaw);
      if (unTicks <= 30
         && !(Math.hypot(p.getX() - unX, p.getZ() - unZ) >= 3.0)
         && !p.isInWater()
         && !p.isInLava()
         && !hazardDir(lv, p.getX(), p.getY(), p.getZ(), -Math.sin(r), Math.cos(r))) {
         mc.options.keyUp.setDown(true);
         mc.options.keyJump.setDown(p.horizontalCollision && p.onGround());
      } else {
         endGuard(mc, p);
      }
   }

   static boolean walkable(ClientLevel lv, BlockPos n) {
      return passable(lv, n) && passable(lv, n.above()) && !passable(lv, n.below());
   }

   static boolean surroundedByWater(ClientLevel lv, BlockPos f) {
      boolean deep = false;

      for (Direction d : HORIZ) {
         BlockPos n = f.relative(d);
         if (walkable(lv, n)) {
            return false;
         }

         if (!passable(lv, n) && passable(lv, n.above()) && passable(lv, n.above(2))) {
            return false;
         }

         if (deepWater(lv, n)) {
            deep = true;
         }
      }

      return deep;
   }

   static BlockPos findDry(ClientLevel lv, LocalPlayer p, int radius) {
      BlockPos o = p.blockPosition();
      BlockPos best = null;
      double bestD = Double.MAX_VALUE;

      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            double d = dx * dx + dz * dz;
            if (!(d < 9.0) && !(d >= bestD)) {
               for (int dy = 2; dy >= -2; dy--) {
                  BlockPos f = o.offset(dx, dy, dz);
                  if (walkable(lv, f) && !liquidAt(lv, f) && !liquidAt(lv, f.above())) {
                     int land = 0;

                     for (Direction h : HORIZ) {
                        BlockPos n = f.relative(h);
                        if (walkable(lv, n) && !liquidAt(lv, n)) {
                           land++;
                        }
                     }

                     if (land >= 3) {
                        best = f;
                        bestD = d;
                        break;
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   static boolean startBridge(Minecraft mc, LocalPlayer p) {
      ClientLevel lv = mc.level;
      if (lv == null) {
         return false;
      } else {
         BlockPos t = findDry(lv, p, 16);
         if (t == null) {
            msg(p, "Bị nước bao quanh nhưng không thấy bờ trong 16 ô.");
            bridgeCool = 400;
            return false;
         } else {
            int slot = hotbarPlaceSlot(p);
            int swapped = -1;
            if (slot < 0) {
               int idx = findPlaceable(p);
               if (idx < 0) {
                  msg(p, "Bị nước bao quanh nhưng không có block để bắc cầu.");
                  bridgeCool = 400;
                  return false;
               }

               slot = toHotbar(mc, p, idx);
               swapped = idx;
            }

            stopMining(mc, p);
            bridgeTarget = t;
            bridgeDir = null;
            escSlot = slot;
            escSwapIdx = swapped;
            escPrevSel = getSelected(p);
            escPitch = p.getXRot();
            setSelected(p, slot);
            brPlaced = 0;
            brCool = 0;
            guardTicks = 0;
            guard = AutoSandOrders.G.BRIDGE;
            msg(p, "Bị nước bao quanh -> đặt block bắc cầu vào bờ.");
            return true;
         }
      }
   }

   static void tickBridge(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      guardTicks++;
      setSelected(p, escSlot);
      if (bridgeTarget == null) {
         finishBridge(mc, p, "mất mục tiêu", 200);
      } else {
         double dx = bridgeTarget.getX() + 0.5 - p.getX();
         double dz = bridgeTarget.getZ() + 0.5 - p.getZ();
         if (Math.hypot(dx, dz) < 1.2 && p.onGround() && !danger(p)) {
            finishBridge(mc, p, null, 100);
         } else if (guardTicks > 800 || brPlaced >= 40) {
            finishBridge(mc, p, "chưa tới được bờ sau " + brPlaced + " block", 400);
         } else if (p.isInLava()) {
            finishBridge(mc, p, "đang ở trong lava", 400);
         } else {
            if (guardTicks % 40 == 0) {
               BlockPos nt = findDry(lv, p, 16);
               if (nt != null) {
                  bridgeTarget = nt;
               }
            }

            Direction want = Math.abs(dx) >= Math.abs(dz) ? (dx > 0.0 ? Direction.EAST : Direction.WEST) : (dz > 0.0 ? Direction.SOUTH : Direction.NORTH);
            if (bridgeDir == null || want.getAxis() == bridgeDir.getAxis() || Math.abs(Math.abs(dx) - Math.abs(dz)) > 1.5) {
               bridgeDir = want;
            }

            Direction dir = bridgeDir;
            p.setYRot(dir.toYRot());
            p.setXRot(60.0F);
            BlockPos feet = p.blockPosition();
            BlockPos f = feet.relative(dir);
            BlockPos b = f.below();
            if (!lavaAt(lv, f) && !lavaAt(lv, b)) {
               mc.options.keyJump.setDown(p.isInWater() && !p.onGround());
               if (!passable(lv, b) && passable(lv, f) && passable(lv, f.above())) {
                  mc.options.keyUp.setDown(true);
               } else if (!passable(lv, f)) {
                  mc.options.keyUp.setDown(true);
                  mc.options.keyJump.setDown(true);
               } else {
                  mc.options.keyUp.setDown(false);
                  if (brCool > 0) {
                     brCool--;
                  } else {
                     BlockPos u = feet.below();
                     BlockHitResult hit = null;
                     if (!passable(lv, u)) {
                        hit = new BlockHitResult(Vec3.atCenterOf(u).add(dir.getStepX() * 0.5, 0.0, dir.getStepZ() * 0.5), dir, u, false);
                     } else {
                        for (Direction d : HORIZ) {
                           BlockPos n = u.relative(d);
                           if (!passable(lv, n)) {
                              hit = new BlockHitResult(Vec3.atCenterOf(n).add(-d.getStepX() * 0.5, 0.0, -d.getStepZ() * 0.5), d.getOpposite(), n, false);
                              break;
                           }
                        }

                        if (hit == null) {
                           hit = new BlockHitResult(Vec3.atCenterOf(u), Direction.UP, u, false);
                        }
                     }

                     mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
                     p.swing(InteractionHand.MAIN_HAND);
                     brPlaced++;
                     brCool = 3;
                  }
               }
            } else {
               finishBridge(mc, p, "có lava phía trước", 400);
            }
         }
      }
   }

   static void finishBridge(Minecraft mc, LocalPlayer p, String err, int cool) {
      mc.options.keyJump.setDown(false);
      if (escSwapIdx >= 0) {
         swapHotbar(mc, p, escSwapIdx, escSlot);
         escSwapIdx = -1;
      }

      setSelected(p, escPrevSel);
      if (!Float.isNaN(escPitch)) {
         p.setXRot(escPitch);
         escPitch = Float.NaN;
      }

      bridgeTarget = null;
      bridgeCool = cool;
      msg(p, err == null ? "Đã bắc cầu vào bờ (" + brPlaced + " block)." : "Bắc cầu dừng: " + err);
      endGuard(mc, p);
   }

   static ItemEntity nearestSand(ClientLevel lv, LocalPlayer p) {
      AABB box = p.getBoundingBox().inflate(pickupRadius, 3.0, pickupRadius);
      List<ItemEntity> list = lv.getEntitiesOfClass(
         ItemEntity.class, box, ex -> !ex.isRemoved() && ex.getItem().is(Items.SAND) && !pickIgnore.contains(ex.getId())
      );
      ItemEntity best = null;
      double bestD = Double.MAX_VALUE;

      for (ItemEntity e : list) {
         double dx = e.getX() - p.getX();
         double dz = e.getZ() - p.getZ();
         double d = dx * dx + dz * dz;
         if (!(d < 1.9599999999999997) && d < bestD) {
            best = e;
            bestD = d;
         }
      }

      return best;
   }

   static void startPickup(Minecraft mc, LocalPlayer p, ItemEntity it) {
      pickTarget = it;
      pickTicks = 0;
      guard = AutoSandOrders.G.PICKUP;
   }

   static void tickPickup(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      pickTicks++;
      ItemEntity it = pickTarget;
      if (it != null && !it.isRemoved() && it.getItem().is(Items.SAND) && !isFull(p) && !p.isInWater() && !p.isInLava()) {
         double dx = it.getX() - p.getX();
         double dz = it.getZ() - p.getZ();
         double d = Math.hypot(dx, dz);
         if (pickTicks <= 100 && (!(d > 0.01) || !hazardDir(lv, p.getX(), p.getY(), p.getZ(), dx / d, dz / d))) {
            p.setYRot((float)Math.toDegrees(Math.atan2(-dx, dz)));
            p.setXRot(0.0F);
            mc.options.keyUp.setDown(true);
            mc.options.keyJump.setDown(p.horizontalCollision || it.getY() > p.getY() + 0.6);
         } else {
            if (pickIgnore.size() > 60) {
               pickIgnore.clear();
            }

            pickIgnore.add(it.getId());
            endPickup(mc, p, 40);
         }
      } else {
         endPickup(mc, p, 5);
      }
   }

   static void endPickup(Minecraft mc, LocalPlayer p, int cool) {
      mc.options.keyJump.setDown(false);
      releaseKeys(mc);
      guard = AutoSandOrders.G.NONE;
      pickTarget = null;
      pickCool = cool;
      wdX = Double.NaN;
      startMining(p);
   }

   static boolean dryAt(ClientLevel lv, BlockPos f) {
      return !liquidAt(lv, f) && !liquidAt(lv, f.above()) && !lavaAt(lv, f.below()) && !passable(lv, f.below());
   }

   static void tickLiquid(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      guardTicks++;
      if (guardTicks == 1) {
         liqX = p.getX();
         liqZ = p.getZ();
      }

      if (!danger(p) && p.onGround()) {
         msg(p, "Đã lên bờ, đào tiếp.");
         endGuard(mc, p);
      } else if (guardTicks > 400) {
         msg(p, "Chưa lên được bờ sau 20 giây, thử đào tiếp.");
         endGuard(mc, p);
      } else {
         boolean noProgress = guardTicks % 40 == 0 && Math.hypot(p.getX() - liqX, p.getZ() - liqZ) < 1.0;
         if (guardTicks % 40 == 0) {
            liqX = p.getX();
            liqZ = p.getZ();
         }

         if (bridgeWater && (guardTicks >= 60 || noProgress) && bridgeCool <= 0 && !p.isInLava()) {
            mc.options.keyJump.setDown(false);
            releaseKeys(mc);
            if (startBridge(mc, p)) {
               return;
            }
         }

         if (shore == null || guardTicks % 10 == 1) {
            BlockPos ns = findShore(lv, p);
            if (ns != null
               && (
                  shore == null
                     || !dryAt(lv, shore)
                     || Math.hypot(ns.getX() - p.getX(), ns.getZ() - p.getZ()) + 1.5 < Math.hypot(shore.getX() - p.getX(), shore.getZ() - p.getZ())
               )) {
               shore = ns;
            }
         }

         mc.options.keyJump.setDown(true);
         mc.options.keyUp.setDown(true);
         if (shore != null) {
            double dx = shore.getX() + 0.5 - p.getX();
            double dz = shore.getZ() + 0.5 - p.getZ();
            if (Math.hypot(dx, dz) > 0.6) {
               p.setYRot((float)Math.toDegrees(Math.atan2(-dx, dz)));
               p.setXRot(0.0F);
            }
         }
      }
   }

   static void startSide(Minecraft mc, LocalPlayer p, ClientLevel lv, double heading) {
      stopMining(mc, p);
      double best = heading + 180.0;

      for (double off : new double[]{90.0, -90.0, 180.0}) {
         double r = Math.toRadians(heading + off);
         if (!hazardDir(lv, p.getX(), p.getY(), p.getZ(), -Math.sin(r), Math.cos(r))) {
            best = heading + off;
            break;
         }
      }

      p.setYRot((float)best);
      guard = AutoSandOrders.G.SIDE;
      sideTicks = 14;
      if (sideWindow <= 0) {
         sideWindow = 300;
      }

      sideStrikes++;
      if (sideStrikes == 1) {
         msg(p, "Có nước / lava / hố phía trước -> né sang bên.");
      }

      if (sideStrikes >= 4) {
         sideStrikes = 0;
         sideWindow = 0;
         sideLock = 600;
         msg(p, "Né liên tục nhiều lần, tạm tắt né phía trước 30 giây.");
      }
   }

   static void tickSide(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      double r = Math.toRadians(p.getYRot());
      if (--sideTicks > 0 && !p.isInWater() && !p.isInLava() && !hazardDir(lv, p.getX(), p.getY(), p.getZ(), -Math.sin(r), Math.cos(r))) {
         mc.options.keyUp.setDown(true);
      } else {
         endGuard(mc, p);
      }
   }

   static boolean startEscape(Minecraft mc, LocalPlayer p) {
      int slot = hotbarPlaceSlot(p);
      int swapped = -1;
      if (slot < 0) {
         int idx = findPlaceable(p);
         if (idx < 0) {
            msg(p, "Kẹt trong hố nhưng không có block để đặt làm bậc.");
            guardCool = 60;
            escCool = 300;
            return false;
         }

         slot = toHotbar(mc, p, idx);
         swapped = idx;
      }

      stopMining(mc, p);
      escSlot = slot;
      escSwapIdx = swapped;
      escPrevSel = getSelected(p);
      escPitch = p.getXRot();
      setSelected(p, slot);
      p.setXRot(90.0F);
      escBase = null;
      escPhase = 0;
      escPhaseTicks = 0;
      escPillars = 0;
      guardTicks = 0;
      guard = AutoSandOrders.G.ESCAPE;
      msg(p, "Kẹt trong hố -> đặt cát dưới chân để leo lên.");
      return true;
   }

   static void tickEscape(Minecraft mc, LocalPlayer p, ClientLevel lv) {
      guardTicks++;
      setSelected(p, escSlot);
      p.setXRot(90.0F);
      if (guardTicks <= 400 && escPillars < 20) {
         BlockPos f = p.blockPosition();
         if (escPhase == 0) {
            if (p.onGround()) {
               if (!passable(lv, f.above()) || !passable(lv, f.above(2))) {
                  finishEscape(mc, p, "trên đầu bị chặn");
               } else if (!trapped(lv, f) && !enclosed(lv, f)) {
                  finishEscape(mc, p, null);
               } else {
                  escBase = f;
                  escPhase = 1;
                  escPhaseTicks = 0;
                  mc.options.keyJump.setDown(true);
               }
            }
         } else if (escPhase == 1) {
            escPhaseTicks++;
            mc.options.keyJump.setDown(escPhaseTicks <= 1);
            if (p.getY() >= escBase.getY() + 1.0) {
               Vec3 hit = new Vec3(escBase.getX() + 0.5, escBase.getY(), escBase.getZ() + 0.5);
               mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, escBase.below(), false));
               p.swing(InteractionHand.MAIN_HAND);
               escPillars++;
               escPhase = 2;
               escPhaseTicks = 0;
            } else if (escPhaseTicks > 12 && p.onGround()) {
               escPhase = 0;
            }
         } else {
            escPhaseTicks++;
            if (p.onGround() || escPhaseTicks > 20) {
               escPhase = 0;
            }
         }
      } else {
         finishEscape(mc, p, "không thoát được sau " + escPillars + " lần đặt block");
      }
   }

   static void finishEscape(Minecraft mc, LocalPlayer p, String err) {
      mc.options.keyJump.setDown(false);
      if (escSwapIdx >= 0) {
         swapHotbar(mc, p, escSwapIdx, escSlot);
         escSwapIdx = -1;
      }

      setSelected(p, escPrevSel);
      if (!Float.isNaN(escPitch)) {
         p.setXRot(escPitch);
         escPitch = Float.NaN;
      }

      msg(p, err == null ? "Đã thoát khỏi hố." : "Thoát hố: " + err);
      escCool = err == null ? 40 : 300;
      idleAcc = 0;
      encCache = false;
      endGuard(mc, p);
   }

   static Component onOff(String name, boolean v) {
      return Component.literal(name + ": ").append(Component.literal(v ? "BẬT" : "TẮT").withStyle(v ? ChatFormatting.GREEN : ChatFormatting.RED));
   }

   static Component val(String name, String value) {
      return Component.literal(name + ": ").append(Component.literal(value).withStyle(ChatFormatting.AQUA));
   }

   static String pickupLabel() {
      return pickupRadius <= 0 ? "Tắt" : (pickupRadius <= 4 ? "Gần (4 ô)" : (pickupRadius <= 8 ? "Vừa (8 ô)" : "Xa (12 ô)"));
   }

   static String stateText() {
      if (!enabled) {
         return "Đang tắt";
      } else if (AutoSandOrders.Lobby.ph != 0) {
         return "Đang ở sảnh -> quay lại KingSMP";
      } else if (mode == AutoSandOrders.Mode.BUILTIN && AutoMiner.recovering()) {
         return AutoMiner.recoverText();
      } else if (state == AutoSandOrders.S.MINING && guard != AutoSandOrders.G.NONE) {
         return switch (guard) {
            case LIQUID -> "Đang thoát khỏi nước / lava";
            case ESCAPE -> "Đang đặt block leo khỏi hố";
            case SIDE -> "Đang né nguy hiểm";
            case PICKUP -> "Đang đi nhặt cát";
            case BRIDGE -> "Đang bắc cầu qua nước";
            case UNSTICK -> "Đang thoát khỏi tường";
            default -> "Đang đào cát";
         };
      } else {
         return switch (state) {
            case MINING -> "Đang đào cát";
            case WAIT_ORDERS, WAIT_DELIVER, DEPOSIT, WAIT_CONFIRM, FINISH -> "Đang giao cát vào đơn";
            case MEND_PREP, MEND_THROW, MEND_RESTORE -> "Đang sửa xẻng bằng bình XP";
            case BUY_PREP, BUY_SHOP, BUY_GEAR, BUY_AMOUNT, BUY_CONFIRM, BUY_DONE -> buyTotem
               ? "Đang mua totem"
               : (buyPearl ? "Đang mua ngọc Ender" : (buyFood ? "Đang mua đồ ăn" : "Đang mua bình XP"));
            case EAT_PREP, EAT_USE -> "Đang ăn";
            case JUNK -> "Đang dọn đồ không cần";
         };
      }
   }

   static int shovelPct(LocalPlayer p) {
      double best = 101.0;

      for (int i = 0; i < 36; i++) {
         ItemStack st = p.getInventory().getItem(i);
         if (!st.isEmpty() && st.is(ItemTags.SHOVELS) && st.isDamageableItem()) {
            best = Math.min(best, remainingPct(st));
         }
      }

      return best > 100.0 ? -1 : (int)Math.round(best);
   }

   static int lerpC(int a, int b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int r = 0;

      for (int s = 24; s >= 0; s -= 8) {
         int ca = a >> s & 0xFF;
         int cb = b >> s & 0xFF;
         r |= ((int)(ca + (cb - ca) * t) & 0xFF) << s;
      }

      return r;
   }

   static void rrect(GuiGraphics g, int x, int y, int w, int h, int r, int c) {
      r = Math.min(r, Math.min(w, h) / 2);
      if (r <= 0) {
         g.fill(x, y, x + w, y + h, c);
      } else {
         g.fill(x, y + r, x + w, y + h - r, c);

         for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int dx = r - (int)Math.round(Math.sqrt(r * r - dy * dy));
            g.fill(x + dx, y + i, x + w - dx, y + i + 1, c);
            g.fill(x + dx, y + h - i - 1, x + w - dx, y + h - i, c);
         }
      }
   }

   static final class Announcements {
      private static final long INTERVAL_NS = 60000000000L;
      private static final int MAX_SEEN = 500;
      private static final int MAX_SHOW = 5;
      private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
      private static final Set<Long> seen = new LinkedHashSet<>();
      private static boolean loaded = false;
      private static volatile boolean fetching = false;
      private static boolean attempted = false;
      private static long lastNs = 0L;
      private static Path file;

      private Announcements() {
      }

      static void tick(Minecraft mc, LocalPlayer p) {
         long now = System.nanoTime();
         if (!fetching && (!attempted || now - lastNs >= 60000000000L)) {
            attempted = true;
            lastNs = now;

            HttpRequest request;
            try {
               String name = URLEncoder.encode(p.getName().getString(), StandardCharsets.UTF_8);
               request = HttpRequest.newBuilder()
                  .uri(URI.create("https://snadpremiun.vietanduong35.workers.dev/announcements?playerName=" + name))
                  .timeout(Duration.ofSeconds(8L))
                  .GET()
                  .build();
            } catch (Exception var6) {
               return;
            }

            fetching = true;
            HTTP.sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8)).whenComplete((response, error) -> {
               try {
                  if (error == null && response != null && response.statusCode() >= 200 && response.statusCode() < 300) {
                     process(mc, response.body());
                  }
               } catch (Throwable var7) {
               } finally {
                  fetching = false;
               }
            });
         }
      }

      private static void process(Minecraft mc, String body) {
         if (body != null && !body.isBlank()) {
            JsonElement root = JsonParser.parseString(body);
            JsonArray arr = null;
            if (root.isJsonArray()) {
               arr = root.getAsJsonArray();
            } else if (root.isJsonObject() && root.getAsJsonObject().has("announcements") && root.getAsJsonObject().get("announcements").isJsonArray()) {
               arr = root.getAsJsonObject().getAsJsonArray("announcements");
            }

            if (arr != null) {
               List<String> texts = new ArrayList<>();
               synchronized (AutoSandOrders.Announcements.class) {
                  loadSeen();
                  List<Object[]> fresh = new ArrayList<>();

                  for (JsonElement e : arr) {
                     if (e.isJsonObject()) {
                        JsonObject o = e.getAsJsonObject();
                        JsonElement m = o.get("message");
                        if (m != null && m.isJsonPrimitive()) {
                           String text = clean(m.getAsString());
                           if (!text.isEmpty()) {
                              JsonElement ie = o.get("id");
                              long id;
                              if (ie != null && ie.isJsonPrimitive() && ie.getAsJsonPrimitive().isNumber()) {
                                 id = ie.getAsLong();
                              } else {
                                 id = 4611686018427387904L | text.hashCode() & 4294967295L;
                              }

                              if (!seen.contains(id)) {
                                 fresh.add(new Object[]{id, text});
                              }
                           }
                        }
                     }
                  }

                  fresh.sort(Comparator.comparingLong(x -> (Long)x[0]));

                  for (Object[] f : fresh) {
                     seen.add((Long)f[0]);
                     texts.add((String)f[1]);
                  }

                  if (!fresh.isEmpty()) {
                     saveSeen();
                  }
               }

               if (!texts.isEmpty()) {
                  mc.execute(
                     () -> {
                        LocalPlayer pl = mc.player;
                        if (pl != null) {
                           int shown = Math.min(5, texts.size());

                           for (int i = texts.size() - shown; i < texts.size(); i++) {
                              pl.displayClientMessage(Component.literal("[Thông báo] " + texts.get(i)).withStyle(ChatFormatting.GOLD), false);
                           }

                           if (texts.size() > shown) {
                              pl.displayClientMessage(
                                 Component.literal("[Thông báo] ... và " + (texts.size() - shown) + " thông báo cũ hơn.").withStyle(ChatFormatting.GRAY), false
                              );
                           }
                        }
                     }
                  );
               }
            }
         }
      }

      private static String clean(String s) {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < s.length() && sb.length() < 300; i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
               sb.append(' ');
            } else if (c >= ' ' && c != 127 && c != 167) {
               sb.append(c);
            }
         }

         return sb.toString().trim();
      }

      private static void loadSeen() {
         if (!loaded) {
            loaded = true;

            try {
               file = FabricLoader.getInstance().getConfigDir().resolve("autosandmod.ann");
               if (!Files.exists(file)) {
                  return;
               }

               for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                  try {
                     seen.add(Long.parseLong(line.trim()));
                  } catch (NumberFormatException var3) {
                  }
               }
            } catch (Exception var4) {
            }
         }
      }

      private static void saveSeen() {
         try {
            while (true) {
               if (seen.size() <= 500) {
                  StringBuilder sb = new StringBuilder();

                  for (Long id : seen) {
                     sb.append(id).append('\n');
                  }

                  if (file == null) {
                     file = FabricLoader.getInstance().getConfigDir().resolve("autosandmod.ann");
                  }

                  Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
                  break;
               }

               Long first = seen.iterator().next();
               seen.remove(first);
            }
         } catch (Exception var3) {
         }
      }
   }

   static class AutoSandScreen extends Screen {
      static final int PW = 620;
      static final int SIDE = 142;
      static final String[] TABS = new String[]{"Chính", "Đào cát", "Sinh tồn", "An toàn", "Tự đào", "Thống kê", "Nâng cao"};
      static final int[] TAB_COL = new int[]{-8596225, -604065, -9508966, -38015, -13773633, -757066, -5796870, -22195};
      static final int TXT = -1;
      static final int MUTED = -7433028;
      static final int PANEL = -234352608;
      static final int PANEL_EDGE = -15085569;
      static final int CARD = -234218703;
      static final int CARD_EDGE = -15243339;
      static final int CARD_HOV = -15713958;
      static final int GOLD_BG = -15980223;
      static final int GOLD_EDGE = -15155733;
      static final int GOLD_TXT = -4786433;
      static final int GREEN_TOP = -15077168;
      static final int GREEN_BOT = -16220515;
      static final int GREEN_EDGE = -14159105;
      static final int GREEN_TXT = -1245185;
      static final int RED_TOP = -47234;
      static final int RED_BOT = -7596209;
      static final int RED_EDGE = -45158;
      static final int RED_TXT = -1;
      static final int PILL_OFF = -12429195;
      static final int[] PARTICLE = new int[]{-1437904267, -1439023268, -1439724391, -1436917115};
      final List<AutoSandOrders.AutoSandScreen.Ctl> ctls = new ArrayList<>();
      int tab = 0;
      int x0;
      int y0;
      int ph;
      int cx;
      int cy;
      int cw;
      int availH;
      int rowH = 30;
      EditBox keyBox;
      boolean wasValid = false;

      static int grad3(float t) {
         return t < 0.5F ? AutoSandOrders.lerpC(-5026305, -15279617, t * 2.0F) : AutoSandOrders.lerpC(-15279617, -14124033, (t - 0.5F) * 2.0F);
      }

      static void card(GuiGraphics g, int x, int y, int w, int h, int r, int bg, int edge) {
         AutoSandOrders.rrect(g, x, y, w, h, r, edge);
         AutoSandOrders.rrect(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), bg);
      }

      static void button3d(GuiGraphics g, int x, int y, int w, int h, int top, int bot, int edge, boolean hov) {
         int depth = 3;
         int by = hov ? y + 1 : y;
         AutoSandOrders.rrect(g, x, y + depth, w, h - depth, 8, edge);
         AutoSandOrders.rrect(g, x, by, w, h - depth, 8, hov ? AutoSandOrders.lerpC(bot, -1, 0.12F) : bot);
         g.fillGradient(x + 5, by + 1, x + w - 5, by + 1 + (h - depth) / 2, hov ? 1728053247 : 1291845631, 16777215);
      }

      void spaced(GuiGraphics g, String s, int x, int y, int space, int col) {
         for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            g.drawString(this.font, ch, x, y, col, false);
            x += this.font.width(ch) + space;
         }
      }

      int gradWidth(String s, int space) {
         int w = 0;

         for (int i = 0; i < s.length(); i++) {
            w += this.font.width(Component.literal(s.substring(i, i + 1)).withStyle(ChatFormatting.BOLD)) + space;
         }

         return w - space;
      }

      void gradText(GuiGraphics g, String s, int x, int y, int space) {
         for (int i = 0; i < s.length(); i++) {
            Component ch = Component.literal(s.substring(i, i + 1)).withStyle(ChatFormatting.BOLD);
            g.drawString(this.font, ch, x, y, grad3((float)i / Math.max(1, s.length() - 1)), true);
            x += this.font.width(ch) + space;
         }
      }

      void backdrop(GuiGraphics g) {
         g.fillGradient(0, 0, this.width, this.height, 1912867608, -1979313110);
         long tm = System.currentTimeMillis();
         g.fill(this.width * 55 / 100, this.height * 28 / 100, this.width * 78 / 100, this.height * 28 / 100 + 9, 857611083);
         g.fill(this.width * 60 / 100, this.height * 28 / 100 - 7, this.width * 72 / 100, this.height * 28 / 100, 639507275);

         for (int i = 0; i < 38; i++) {
            int h = (i + 1) * -1640531535;
            int rx = Math.floorMod(h >>> 7, Math.max(1, this.width));
            int ry = Math.floorMod(h * 31 >>> 5, Math.max(1, this.height));
            int sz = 2 + i % 3 * 2;
            int bob = (int)Math.round(Math.sin(tm / 1400.0 + i) * 3.0);
            g.fill(rx, ry + bob, rx + sz, ry + bob + sz, PARTICLE[i % PARTICLE.length]);
         }
      }

      AutoSandScreen() {
         super(Component.literal("AutoSand"));
      }

      AutoSandOrders.AutoSandScreen.Ctl add(int kind, int x, int y, int w, int h, String title, String desc, Runnable act) {
         AutoSandOrders.AutoSandScreen.Ctl c = new AutoSandOrders.AutoSandScreen.Ctl();
         c.kind = kind;
         c.x = x;
         c.y = y;
         c.w = w;
         c.h = h;
         c.title = title;
         c.desc = desc;
         Button b = Button.builder(Component.empty(), bb -> act.run()).bounds(x, y, w, h).build();
         b.setAlpha(0.0F);
         this.addRenderableWidget(b);
         this.ctls.add(c);
         return c;
      }

      void begin(int n) {
         this.rowH = Math.max(20, Math.min(30, (this.availH - (n - 1) * 4) / n));
      }

      int cardY(int i) {
         return this.cy + i * (this.rowH + 4);
      }

      void toggle(int i, String title, String desc, BooleanSupplier get, Consumer<Boolean> set) {
         AutoSandOrders.AutoSandScreen.Ctl c = this.add(0, this.cx, this.cardY(i), this.cw, this.rowH, title, desc, () -> {
            set.accept(!get.getAsBoolean());
            AutoSandOrders.save();
         });
         c.on = get;
      }

      void cycle(int i, String title, String desc, Supplier<String> value, Runnable next) {
         AutoSandOrders.AutoSandScreen.Ctl c = this.add(1, this.cx, this.cardY(i), this.cw, this.rowH, title, desc, () -> {
            next.run();
            AutoSandOrders.save();
         });
         c.value = value;
      }

      protected void init() {
         this.ctls.clear();
         this.ph = Math.min(this.height - 20, 420);
         this.x0 = Math.max(8, (this.width - 620) / 2);
         this.y0 = this.height / 2 - this.ph / 2;
         this.cx = this.x0 + 142 + 10;
         this.cw = 458;
         this.cy = this.y0 + 54;
         this.availH = this.ph - 64 - 10;
         this.wasValid = AutoSandOrders.License.valid();
         if (!this.wasValid) {
            this.tab = 0;
            this.add(5, this.x0 + 620 - 26, this.y0 + 9, 16, 16, "x", null, () -> this.onClose());
            int bx = this.x0 + 24;
            int bw = 572;
            int by = this.y0 + this.ph / 2 - 6;
            String keep = this.keyBox == null ? "" : this.keyBox.getValue();
            this.keyBox = new EditBox(this.font, bx, by, bw, 20, Component.literal("Key"));
            this.keyBox.setMaxLength(128);
            this.keyBox.setHint(Component.literal("Dán key vào đây..."));
            this.keyBox.setValue(keep);
            this.addRenderableWidget(this.keyBox);
            this.setFocused(this.keyBox);
            this.add(2, bx, by + 30, bw, 22, "Kích hoạt", null, () -> {
               String k = this.keyBox.getValue().trim();
               if (!k.isEmpty()) {
                  AutoSandOrders.License.activate(k);
                  this.keyBox.setValue("");
               }
            });
         } else {
            for (int i = 0; i < TABS.length; i++) {
               int t = i;
               AutoSandOrders.AutoSandScreen.Ctl c = this.add(4, this.x0 + 8, this.y0 + 48 + i * 30, 128, 26, TABS[i], null, () -> {
                  this.tab = t;
                  this.rebuildWidgets();
               });
               c.selected = i == this.tab;
               c.color = TAB_COL[i];
            }

            this.add(5, this.x0 + 620 - 26, this.y0 + 9, 16, 16, "x", null, () -> this.onClose());
            switch (this.tab) {
               case 0:
                  this.add(3, this.cx, this.cy, this.cw, 46, "", null, () -> AutoSandOrders.setEnabled(Minecraft.getInstance(), !AutoSandOrders.enabled));
                  break;
               case 1:
                  this.begin(7);
                  this.cycle(0, "Chế độ đào", "Tự đào (có sẵn), giữ phím hoặc chỉ Nuker", () -> AutoSandOrders.mode.label, () -> {
                     Minecraft mc = Minecraft.getInstance();
                     boolean running = AutoSandOrders.enabled && AutoSandOrders.state == AutoSandOrders.S.MINING && mc.player != null;
                     if (running) {
                        AutoSandOrders.stopMining(mc, mc.player);
                     }

                     AutoSandOrders.mode = AutoSandOrders.mode.next();
                     if (running) {
                        AutoSandOrders.startMining(mc.player);
                     }
                  });
                  this.toggle(1, "Nuker", "Phá khối xung quanh bằng Nuker", () -> AutoSandOrders.useNuker, v -> {
                     Minecraft mc = Minecraft.getInstance();
                     AutoSandOrders.useNuker = v;
                     if (AutoSandOrders.enabled && AutoSandOrders.state == AutoSandOrders.S.MINING && mc.player != null) {
                        AutoSandOrders.nukerState = null;
                        AutoSandOrders.setNuker(mc.player, AutoSandOrders.useNuker);
                     }
                  });
                  this.toggle(2, "Giữ phím tiến", "Chế độ Giữ phím: tự giữ W đi tới", () -> AutoSandOrders.keysForward, v -> AutoSandOrders.keysForward = v);
                  this.cycle(3, "Nhặt cát", "Ưu tiên nhặt cát rơi quanh bạn", AutoSandOrders::pickupLabel, () -> {
                     int next = AutoSandOrders.PICKUP_LEVELS[0];

                     for (int v : AutoSandOrders.PICKUP_LEVELS) {
                        if (v > AutoSandOrders.pickupRadius) {
                           next = v;
                           break;
                        }
                     }

                     AutoSandOrders.pickupRadius = next;
                  });
                  this.toggle(4, "Tự giao cát", "Đầy túi -> /orders -> giao vào đơn cát", () -> AutoSandOrders.autoDeliver, v -> AutoSandOrders.autoDeliver = v);
                  this.toggle(5, "Tự bật lại khi dừng", "Nuker dừng hoặc lỗi -> bật lại", () -> AutoSandOrders.autoRevive, v -> AutoSandOrders.autoRevive = v);
                  this.cycle(6, "Tốc độ đào", "Hệ số gọi phá khối (máy chủ vẫn giới hạn tốc độ thực tế)", () -> AutoSandOrders.mineSpeed + "%", () -> {
                     int next = AutoSandOrders.SPEED_LEVELS[0];

                     for (int v : AutoSandOrders.SPEED_LEVELS) {
                        if (v < AutoSandOrders.mineSpeed) {
                           next = v;
                           break;
                        }
                     }

                     AutoSandOrders.mineSpeed = next;
                  });
                  break;
               case 2:
                  this.begin(8);
                  this.toggle(0, "Tự ăn khi đói", "Đói -> ngừng đào, ăn no rồi đào tiếp", () -> AutoSandOrders.autoEat, v -> AutoSandOrders.autoEat = v);
                  this.cycle(1, "Ăn khi đói <=", "Thanh đói từ mức này trở xuống thì ăn", () -> AutoSandOrders.eatBelow + "/20", () -> {
                     int next = AutoSandOrders.EAT_LEVELS[0];

                     for (int v : AutoSandOrders.EAT_LEVELS) {
                        if (v > AutoSandOrders.eatBelow) {
                           next = v;
                           break;
                        }
                     }

                     AutoSandOrders.eatBelow = next;
                  });
                  this.toggle(2, "Tự mua đồ ăn", "Hết đồ ăn -> /shop -> FOOD -> Cà Rốt Vàng", () -> AutoSandOrders.autoBuyFood, v -> {
                     AutoSandOrders.autoBuyFood = v;
                     AutoSandOrders.foodBuyFails = 0;
                  });
                  this.toggle(3, "Tự sửa xẻng", "Xẻng gần hư -> ném bình XP (cần Mending)", () -> AutoSandOrders.autoMend, v -> AutoSandOrders.autoMend = v);
                  this.cycle(4, "Sửa khi còn dưới", "Độ bền xẻng thấp hơn mức này thì sửa", () -> AutoSandOrders.mendPct + "%", () -> {
                     int next = AutoSandOrders.PCTS[0];

                     for (int v : AutoSandOrders.PCTS) {
                        if (v > AutoSandOrders.mendPct) {
                           next = v;
                           break;
                        }
                     }

                     AutoSandOrders.mendPct = next;
                  });
                  this.toggle(5, "Tự mua bình XP", "Hết bình XP -> mua ở /shop (chỉ vứt cát)", () -> AutoSandOrders.autoBuy, v -> AutoSandOrders.autoBuy = v);
                  this.toggle(
                     6,
                     "Vứt đồ không cần",
                     "Giữ cát, bình XP, xẻng, đồ ăn, totem. Vứt phần còn lại",
                     () -> AutoSandOrders.autoDrop,
                     v -> AutoSandOrders.autoDrop = v
                  );
                  this.toggle(7, "Tự cầm Totem", "Tay trái luôn cầm totem, hết thì mua ở /shop", () -> AutoSandOrders.autoTotem, v -> {
                     AutoSandOrders.autoTotem = v;
                     AutoSandOrders.totemBuyFails = 0;
                     AutoSandOrders.totemCool = 0;
                  });
                  break;
               case 3:
                  this.begin(3);
                  this.toggle(
                     0,
                     "Thoát nước sâu / lava",
                     "Kẹt nước sâu hoặc lava -> tự bơi lên bờ",
                     () -> AutoSandOrders.avoidLiquid,
                     v -> AutoSandOrders.avoidLiquid = v
                  );
                  this.toggle(
                     1, "Bắc cầu qua nước", "Bị nước bao quanh -> đặt block đi vào bờ", () -> AutoSandOrders.bridgeWater, v -> AutoSandOrders.bridgeWater = v
                  );
                  this.toggle(2, "Né hố & tự leo", "Rơi xuống hố -> tự đặt block leo lên", () -> AutoSandOrders.avoidHole, v -> AutoSandOrders.avoidHole = v);
                  break;
               case 4:
                  this.begin(6);
                  this.toggle(
                     0, "Chết -> về nhặt đồ", "Hồi sinh -> /home -> về chỗ chết nhặt lại đồ", () -> AutoMiner.deathRecover, v -> AutoMiner.deathRecover = v
                  );
                  this.toggle(1, "Tự hồi sinh", "Chết thì tự bấm nút Hồi sinh", () -> AutoMiner.autoRespawn, v -> AutoMiner.autoRespawn = v);
                  this.toggle(
                     2, "Đào xuyên cát khi đi", "Cát chắn đường: có xẻng đào, không thì vòng", () -> AutoMiner.digThrough, v -> AutoMiner.digThrough = v
                  );
                  this.cycle(3, "Phạm vi tìm cát", "Tìm cát trong bán kính này", () -> AutoMiner.scanRadius + " ô", () -> {
                     int next = AutoMiner.SCAN_LEVELS[0];

                     for (int v : AutoMiner.SCAN_LEVELS) {
                        if (v > AutoMiner.scanRadius) {
                           next = v;
                           break;
                        }
                     }

                     AutoMiner.scanRadius = next;
                  });
                  this.toggle(
                     4,
                     "Bị kick -> về KingSMP",
                     "Ra sảnh: bấm la bàn chọn KingSMP, đào tiếp",
                     () -> AutoSandOrders.autoRejoin,
                     v -> AutoSandOrders.autoRejoin = v
                  );
                  this.toggle(
                     5, "Dùng ngọc Ender", "Chết xa: mua ngọc ở /shop, ném tới chỗ chết", () -> AutoSandOrders.usePearl, v -> AutoSandOrders.usePearl = v
                  );
                  break;
               case 5:
                  AutoSandOrders.AutoSandScreen.Ctl c1 = this.add(1, this.cx, this.cy + 124, this.cw, 24, "Giá ước tính mỗi cát", null, () -> {
                     double next = AutoSandOrders.PRICE_LEVELS[0];

                     for (double v : AutoSandOrders.PRICE_LEVELS) {
                        if (v > AutoSandOrders.pricePerSand + 1.0E-4) {
                           next = v;
                           break;
                        }
                     }

                     AutoSandOrders.pricePerSand = next;
                     AutoSandOrders.save();
                  });
                  c1.value = () -> AutoSandOrders.pricePerSand + " $";
                  this.add(2, this.cx, this.cy + 152, this.cw, 22, "Đặt lại thống kê", null, () -> {
                     AutoSandOrders.statSand = 0L;
                     AutoSandOrders.statTicks = 0L;
                     AutoSandOrders.statDeaths = 0;
                     AutoSandOrders.save();
                  });
                  break;
               case 6:
                  this.begin(7);
                  this.toggle(
                     0,
                     "Tự phòng thủ",
                     "Quái thù địch lại gần -> cầm kiếm / rìu đánh rồi đào tiếp",
                     () -> AutoSandOrders.autoDefend,
                     v -> AutoSandOrders.autoDefend = v
                  );
                  this.cycle(
                     1,
                     "Bán kính phòng thủ",
                     "Chỉ đánh quái trong bán kính này",
                     () -> AutoSandOrders.defenseRadius + " ô",
                     () -> AutoSandOrders.defenseRadius = AutoSandOrders.nextInt(AutoSandOrders.DEFENSE_LEVELS, AutoSandOrders.defenseRadius)
                  );
                  this.toggle(
                     2,
                     "Chế độ giảm tải",
                     "Hạ FPS / tầm nhìn khi bot chạy, tắt bot thì trả lại như cũ",
                     () -> AutoSandOrders.lowLoad,
                     v -> AutoSandOrders.lowLoad = v
                  );
                  this.cycle(
                     3,
                     "FPS khi đang mở game",
                     "Giới hạn FPS khi cửa sổ game đang được chọn",
                     () -> AutoSandOrders.fgFps + "",
                     () -> AutoSandOrders.fgFps = AutoSandOrders.nextInt(AutoSandOrders.FG_FPS_LEVELS, AutoSandOrders.fgFps)
                  );
                  this.cycle(
                     4,
                     "FPS khi ở nền",
                     "Giới hạn FPS khi cửa sổ game không được chọn",
                     () -> AutoSandOrders.bgFps + "",
                     () -> AutoSandOrders.bgFps = AutoSandOrders.nextInt(AutoSandOrders.BG_FPS_LEVELS, AutoSandOrders.bgFps)
                  );
                  this.cycle(
                     5,
                     "Tầm nhìn giảm tải",
                     "Số chunk khi bật chế độ giảm tải",
                     () -> AutoSandOrders.lowLoadView + " chunk",
                     () -> AutoSandOrders.lowLoadView = AutoSandOrders.nextInt(AutoSandOrders.VIEW_LEVELS, AutoSandOrders.lowLoadView)
                  );
                  this.cycle(
                     6,
                     "Giá cát tối thiểu",
                     "Bỏ qua đơn trả thấp hơn mức này (tắt = nhận mọi đơn)",
                     () -> AutoSandOrders.minSandPrice <= 0.0 ? "tắt" : AutoSandOrders.minSandPrice + " $",
                     () -> AutoSandOrders.minSandPrice = AutoSandOrders.nextDbl(AutoSandOrders.MIN_PRICE_LEVELS, AutoSandOrders.minSandPrice)
                  );
            }
         }
      }

      void stat(GuiGraphics g, int x, int y, int w, int h, String label, String value, int vcol, float bar, int barCol) {
         card(g, x, y, w, h, 8, -234218703, -15243339);
         g.drawString(this.font, label.toUpperCase(Locale.ROOT), x + 8, y + 5, -7433028, false);
         g.drawString(this.font, value, x + 8, y + 16, vcol, false);
         if (bar >= 0.0F) {
            int segs = 5;
            int gap = 3;
            int sw = (w - 16 - gap * (segs - 1)) / segs;
            int filled = Math.round(Math.min(1.0F, bar) * segs);
            if (bar > 0.0F && filled == 0) {
               filled = 1;
            }

            for (int k = 0; k < segs; k++) {
               AutoSandOrders.rrect(g, x + 8 + k * (sw + gap), y + h - 8, sw, 4, 2, k < filled ? barCol : -12429195);
            }
         }
      }

      void bullet(GuiGraphics g, String s, int x, int y) {
         AutoSandOrders.rrect(g, x, y + 2, 4, 4, 2, -8657771);
         g.drawString(this.font, s, x + 9, y, -3946266, false);
      }

      public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
         super.renderBackground(g, mx, my, pt);
         this.backdrop(g);

         for (int i = 8; i >= 1; i--) {
            AutoSandOrders.rrect(g, this.x0 - i, this.y0 - i, 620 + 2 * i, this.ph + 2 * i, 14 + i, 18 - i << 24 | 8154367);
         }

         AutoSandOrders.rrect(g, this.x0, this.y0, 620, this.ph, 14, -15085569);
         AutoSandOrders.rrect(g, this.x0 + 1, this.y0 + 1, 618, this.ph - 2, 13, -234352608);
         g.fillGradient(this.x0 + 8, this.y0 + 2, this.x0 + 620 - 8, this.y0 + 36, 587202559, 16777215);

         for (int i = 0; i < 48; i++) {
            g.fill(this.x0 + 10 + i * 600 / 48, this.y0 + 35, this.x0 + 10 + (i + 1) * 600 / 48, this.y0 + 37, grad3(i / 47.0F));
         }

         if (this.wasValid) {
            g.fill(this.x0 + 142, this.y0 + 46, this.x0 + 142 + 1, this.y0 + this.ph - 10, -15305560);
         }

         int icx = this.x0 + 12;
         int icy = this.y0 + 7;
         AutoSandOrders.rrect(g, icx, icy + 2, 22, 22, 6, -14159105);
         AutoSandOrders.rrect(g, icx, icy, 22, 22, 6, -16220515);
         g.fillGradient(icx + 3, icy + 1, icx + 19, icy + 11, 1728053247, 16777215);
         g.fill(icx + 10, icy + 9, icx + 12, icy + 17, -13664966);
         g.fill(icx + 5, icy + 6, icx + 10, icy + 9, -13664966);
         g.fill(icx + 12, icy + 5, icx + 17, icy + 8, -12674228);
         g.fill(icx + 7, icy + 16, icx + 15, icy + 18, -9745874);
         this.gradText(g, "AUTOSAND", this.x0 + 42, this.y0 + 9, 2);
         String sub = (this.wasValid ? AutoSandOrders.mode.label.toUpperCase(Locale.ROOT) + "  ·  " : "") + "KINGSMP";
         this.spaced(g, sub, this.x0 + 42, this.y0 + 22, 1, -7433028);
         boolean runOn = AutoSandOrders.enabled && this.wasValid;
         String run = !this.wasValid ? "CHƯA KÍCH HOẠT" : (AutoSandOrders.enabled ? "ĐANG CHẠY" : "ĐANG TẮT");
         int chipW = this.font.width(run) + 24;
         int chipX = this.x0 + 620 - 34 - chipW;
         int chipY = this.y0 + 9;
         int chipCol = runOn ? -9508966 : -38015;
         AutoSandOrders.rrect(g, chipX, chipY, chipW, 16, 8, chipCol);
         AutoSandOrders.rrect(g, chipX + 1, chipY + 1, chipW - 2, 14, 7, runOn ? -15649750 : -12970458);
         AutoSandOrders.rrect(g, chipX + 7, chipY + 5, 6, 6, 3, chipCol);
         g.drawString(this.font, run, chipX + 17, chipY + 4, chipCol, false);
         LocalPlayer p = Minecraft.getInstance().player;
         if (!this.wasValid) {
            card(g, this.x0 + 16, this.y0 + this.ph / 2 - 64, 588, 118, 12, -234218703, -15243339);
            String t1 = "NHẬP KEY ĐỂ MỞ KHÓA CÁC CHỨC NĂNG";
            String t2 = "Key: " + AutoSandOrders.License.status();
            this.gradText(g, "AUTOSAND PREMIUM", this.x0 + (620 - this.gradWidth("AUTOSAND PREMIUM", 2)) / 2, this.y0 + this.ph / 2 - 54, 2);
            g.drawString(this.font, t1, this.x0 + (620 - this.font.width(t1)) / 2, this.y0 + this.ph / 2 - 38, -4786433, false);
            g.drawString(this.font, t2, this.x0 + (620 - this.font.width(t2)) / 2, this.y0 + this.ph / 2 - 26, -7433028, false);
            this.spaced(g, "KEY", this.x0 + 24, this.y0 + this.ph / 2 - 16, 1, -7433028);
         } else if (this.tab == 0) {
            card(g, this.cx, this.cy + 52, this.cw, 28, 10, -15980223, -15155733);
            AutoSandOrders.rrect(g, this.cx + 10, this.cy + 63, 7, 7, 3, AutoSandOrders.enabled ? -15077168 : -47234);
            g.drawString(this.font, AutoSandOrders.stateText(), this.cx + 24, this.cy + 61, -4786433, false);
            int sw = (this.cw - 8) / 3;
            if (p != null) {
               int sp = AutoSandOrders.shovelPct(p);
               int food = p.getFoodData().getFoodLevel();
               int spCol = sp > 50 ? -14498466 : (sp > 20 ? -680437 : -1096636);
               this.stat(g, this.cx, this.cy + 86, sw, 42, "Cát", String.valueOf(AutoSandOrders.countItem(p, Items.SAND)), -1, -1.0F, 0);
               this.stat(
                  g, this.cx + sw + 4, this.cy + 86, sw, 42, "Bình XP", String.valueOf(AutoSandOrders.countItem(p, Items.EXPERIENCE_BOTTLE)), -1, -1.0F, 0
               );
               this.stat(g, this.cx + 2 * (sw + 4), this.cy + 86, sw, 42, "Đồ ăn", String.valueOf(AutoSandOrders.countFood(p)), -1, -1.0F, 0);
               this.stat(g, this.cx, this.cy + 132, sw, 42, "Xẻng", sp < 0 ? "không có" : sp + "%", -1, sp < 0 ? -1.0F : sp / 100.0F, spCol);
               this.stat(
                  g, this.cx + sw + 4, this.cy + 132, sw, 42, "Đói", food + "/20", -1, food / 20.0F, food <= AutoSandOrders.eatBelow ? -1096636 : -680437
               );
               this.stat(g, this.cx + 2 * (sw + 4), this.cy + 132, sw, 42, "Nhặt cát", AutoSandOrders.pickupLabel(), -1, -1.0F, 0);
            } else {
               g.drawString(this.font, "Chưa vào thế giới.", this.cx + 4, this.cy + 80, -7433028, false);
            }
         } else if (this.tab == 3) {
            int iy = this.cardY(3) + 6;
            card(g, this.cx, iy, this.cw, 66, 8, -234218703, -15243339);
            this.bullet(g, "Kẹt nước sâu / lava: tự bơi lên bờ.", this.cx + 10, iy + 8);
            this.bullet(g, "Bị nước bao quanh: đặt cát bắc cầu vào bờ.", this.cx + 10, iy + 22);
            this.bullet(g, "Rơi xuống hố: nhảy + đặt cát để leo, không đào lên.", this.cx + 10, iy + 36);
            this.bullet(g, "Né nước/hố phía trước: chỉ ở chế độ Giữ phím / Nuker.", this.cx + 10, iy + 50);
         } else if (this.tab == 7) {
            int iy = this.cardY(4) + 2;
            card(g, this.cx, iy, this.cw, 62, 8, -234218703, -15243339);
            this.bullet(g, "Bật rồi bấm BẮT ĐẦU ĐÀO: hầm đi theo hướng bạn đang nhìn.", this.cx + 10, iy + 8);
            this.bullet(g, "Mỗi bước đào hết mặt cắt (rộng x cao), rồi mới tiến lên.", this.cx + 10, iy + 22);
            this.bullet(g, "Hết cát / gặp nước / bị chặn: tự rẽ, không được thì đào thường.", this.cx + 10, iy + 36);
            this.bullet(g, "Chỉ chạy ở chế độ Tự đào. Cát cạnh nước / lava bị bỏ qua.", this.cx + 10, iy + 50);
         } else if (this.tab == 5) {
            int sw = (this.cw - 4) / 2;
            this.stat(
               g,
               this.cx,
               this.cy,
               this.cw,
               36,
               "Toạ độ chết gần nhất",
               AutoMiner.lastDeathText == null ? "Chưa chết lần nào" : AutoMiner.lastDeathText,
               -1,
               -1.0F,
               0
            );
            this.stat(g, this.cx, this.cy + 40, sw, 36, "Thời gian treo", AutoSandOrders.fmtTime(AutoSandOrders.statTicks), -1, -1.0F, 0);
            this.stat(g, this.cx + sw + 4, this.cy + 40, sw, 36, "Tổng tiền kiếm được", AutoSandOrders.fmtMoney(), -14498466, -1.0F, 0);
            this.stat(g, this.cx, this.cy + 80, sw, 36, "Cát đã giao", String.valueOf(AutoSandOrders.statSand), -1, -1.0F, 0);
            this.stat(g, this.cx + sw + 4, this.cy + 80, sw, 36, "Số lần chết", String.valueOf(AutoSandOrders.statDeaths), -1096636, -1.0F, 0);
         }

         for (AutoSandOrders.AutoSandScreen.Ctl c : this.ctls) {
            this.drawCtl(g, c, mx, my);
         }

         g.drawString(this.font, "ESC để đóng", this.x0 + 12, this.y0 + this.ph - 18, -10064748, false);
         if (this.wasValid) {
            g.drawString(
               this.font,
               "Key: " + AutoSandOrders.License.status(),
               this.x0 + 142 + 10,
               this.y0 + this.ph - 18,
               AutoSandOrders.License.valid() ? -9508966 : -38015,
               false
            );
         }
      }

      void drawCtl(GuiGraphics g, AutoSandOrders.AutoSandScreen.Ctl c, int mx, int my) {
         boolean hov = mx >= c.x && mx < c.x + c.w && my >= c.y && my < c.y + c.h;
         switch (c.kind) {
            case 0:
            case 1:
               card(g, c.x, c.y, c.w, c.h, 7, hov ? -15713958 : -234218703, hov ? -9602049 : -15243339);
               g.fillGradient(c.x + 4, c.y + 1, c.x + c.w - 4, c.y + 1 + c.h / 2, 251658239, 16777215);
               boolean showDesc = c.desc != null && c.h >= 26;
               int ty = showDesc ? c.y + 4 : c.y + (c.h - 8) / 2;
               if (c.kind != 0) {
                  AutoSandOrders.rrect(g, c.x + 2, c.y + 5, 3, c.h - 10, 1, -5796870);
                  String v = c.value == null ? "" : c.value.get();
                  int chipW = this.font.width(v) + 18;
                  int chipX = c.x + c.w - chipW - 8;
                  int chipY = c.y + (c.h - 16) / 2;
                  AutoSandOrders.rrect(g, chipX, chipY, chipW, 16, 8, hov ? -7701505 : -10794021);
                  g.fillGradient(chipX + 4, chipY + 1, chipX + chipW - 4, chipY + 8, 822083583, 16777215);
                  g.drawString(this.font, v, chipX + 9, chipY + 4, -1, false);
               } else {
                  boolean on = c.on != null && c.on.getAsBoolean();
                  float target = on ? 1.0F : 0.0F;
                  if (c.t < 0.0F) {
                     c.t = target;
                  }

                  c.t = c.t + Math.max(-0.2F, Math.min(0.2F, target - c.t));
                  AutoSandOrders.rrect(g, c.x + 2, c.y + 5, 3, c.h - 10, 1, on ? -9508966 : -12429195);
                  int sx = c.x + c.w - 40;
                  int sy = c.y + (c.h - 14) / 2;
                  AutoSandOrders.rrect(g, sx, sy, 30, 14, 7, AutoSandOrders.lerpC(-12429195, -9508966, c.t));
                  AutoSandOrders.rrect(g, sx + 2 + (int)(c.t * 16.0F), sy + 2, 10, 10, 5, -1);
               }

               g.drawString(this.font, c.title, c.x + 12, ty, -1, false);
               if (showDesc) {
                  g.drawString(this.font, c.desc, c.x + 12, c.y + 15, -7433028, false);
               }
               break;
            case 2:
               button3d(g, c.x, c.y, c.w, c.h, -15077168, -16220515, -14159105, hov);
               Component t = Component.literal(c.title.toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.BOLD);
               g.drawString(this.font, t, c.x + (c.w - this.font.width(t)) / 2, c.y + (c.h - 3 - 8) / 2 + (hov ? 1 : 0) + 1, -1245185, false);
               break;
            case 3:
               float target = AutoSandOrders.enabled ? 1.0F : 0.0F;
               if (c.t < 0.0F) {
                  c.t = target;
               }

               c.t = c.t + Math.max(-0.12F, Math.min(0.12F, target - c.t));
               int glow = AutoSandOrders.enabled ? 15750767 : 7268250;

               for (int i = 5; i >= 1; i--) {
                  AutoSandOrders.rrect(g, c.x - i, c.y - i, c.w + 2 * i, c.h + 2 * i, 9 + i, 22 - i * 3 << 24 | glow);
               }

               if (AutoSandOrders.enabled) {
                  button3d(g, c.x, c.y, c.w, c.h, -47234, -7596209, -45158, hov);
               } else {
                  button3d(g, c.x, c.y, c.w, c.h, -15077168, -16220515, -14159105, hov);
               }

               Component label = Component.literal(AutoSandOrders.enabled ? "ĐANG CHẠY  ·  BẤM ĐỂ DỪNG" : "BẮT ĐẦU ĐÀO").withStyle(ChatFormatting.BOLD);
               g.drawString(
                  this.font,
                  label,
                  c.x + (c.w - this.font.width(label)) / 2,
                  c.y + (c.h - 3 - 8) / 2 + (hov ? 1 : 0) + 1,
                  AutoSandOrders.enabled ? -1 : -1245185,
                  false
               );
               break;
            case 4:
               if (c.selected) {
                  AutoSandOrders.rrect(g, c.x, c.y, c.w, c.h, 7, c.color);
                  AutoSandOrders.rrect(g, c.x + 1, c.y + 1, c.w - 2, c.h - 2, 6, AutoSandOrders.lerpC(-234352608, c.color, 0.24F));
                  AutoSandOrders.rrect(g, c.x + 2, c.y + 5, 3, c.h - 10, 1, c.color);
               } else if (hov) {
                  AutoSandOrders.rrect(g, c.x, c.y, c.w, c.h, 7, -15196854);
               }

               AutoSandOrders.rrect(g, c.x + 10, c.y + (c.h - 6) / 2, 6, 6, 3, c.color);
               g.drawString(this.font, c.title, c.x + 22, c.y + (c.h - 8) / 2, c.selected ? -1 : (hov ? -2564875 : -7433028), false);
               break;
            default:
               AutoSandOrders.rrect(g, c.x, c.y, c.w, c.h, 8, hov ? -41862 : -14538650);
               g.drawString(this.font, c.title, c.x + (c.w - this.font.width(c.title)) / 2, c.y + (c.h - 8) / 2, -1, false);
         }
      }

      public void tick() {
         super.tick();
         if (AutoSandOrders.License.valid() != this.wasValid) {
            this.rebuildWidgets();
         }
      }

      public boolean isPauseScreen() {
         return false;
      }

      static final class Ctl {
         int kind;
         int x;
         int y;
         int w;
         int h;
         int color;
         String title;
         String desc;
         BooleanSupplier on;
         Supplier<String> value;
         float t = -1.0F;
         boolean selected;
      }
   }

   static enum G {
      NONE,
      LIQUID,
      ESCAPE,
      SIDE,
      PICKUP,
      BRIDGE,
      UNSTICK;
   }

   static final class License {
      static final String API_BASE = "https://snadpremiun.vietanduong35.workers.dev";
      private static final String VERIFY_URL = "https://snadpremiun.vietanduong35.workers.dev/verify";
      private static final long CHECK_INTERVAL_NS = 30000000000L;
      private static final long OFFLINE_GRACE_NS = 900000000000L;
      private static final long SALT = new SecureRandom().nextLong();
      private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
      private static Path file;
      private static volatile String storedKey;
      private static volatile String verifiedName;
      private static volatile String boundName;
      private static volatile boolean checking;
      private static volatile boolean attempted;
      private static volatile long lastAttemptNs;
      private static volatile long lastOkNs;
      private static volatile long expireMs;
      private static volatile long seal;
      private static volatile String statusText = "chưa kích hoạt";
      private static volatile String lastShown = "";

      private License() {
      }

      static void init() {
         try {
            file = FabricLoader.getInstance().getConfigDir().resolve("autosandmod.lic");
            if (Files.exists(file)) {
               List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
               if (!lines.isEmpty() && !lines.get(0).isBlank()) {
                  storedKey = lines.get(0).trim();
                  statusText = "chờ xác thực";
               }
            }
         } catch (Exception var1) {
            statusText = "không đọc được file key";
         }
      }

      static synchronized void activate(String key) {
         String cleaned = key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
         if (cleaned.isEmpty()) {
            statusText = "chưa nhập key";
         } else {
            storedKey = cleaned;
            lastOkNs = 0L;
            seal = 0L;
            expireMs = 0L;
            boundName = null;
            attempted = false;
            lastShown = "";
            statusText = "đang xác thực";

            try {
               if (file == null) {
                  file = FabricLoader.getInstance().getConfigDir().resolve("autosandmod.lic");
               }

               Files.writeString(file, cleaned, StandardCharsets.UTF_8);
            } catch (Exception var3) {
               statusText = "không lưu được key";
               return;
            }

            verifyOnline();
         }
      }

      static synchronized void refreshIfNeeded() {
         if (storedKey != null && !storedKey.isBlank() && !checking) {
            if (!attempted || System.nanoTime() - lastAttemptNs >= 30000000000L) {
               verifyOnline();
            }
         }
      }

      private static long sealOf(long ok, String key, String name) {
         long h = SALT ^ ok * -7046029254386353131L;
         h ^= (key == null ? 0L : key.hashCode()) * 31L;
         h ^= (name == null ? 0L : name.hashCode()) * 1099511628211L;
         h ^= h >>> 29;
         return h * -4658895280553007687L;
      }

      private static String escapeJson(String value) {
         StringBuilder out = new StringBuilder();

         for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
               case '\t':
                  out.append("\\t");
                  break;
               case '\n':
                  out.append("\\n");
                  break;
               case '\r':
                  out.append("\\r");
                  break;
               case '"':
                  out.append("\\\"");
                  break;
               case '\\':
                  out.append("\\\\");
                  break;
               default:
                  if (c < ' ') {
                     out.append(String.format("\\u%04x", Integer.valueOf(c)));
                  } else {
                     out.append(c);
                  }
            }
         }

         return out.toString();
      }

      private static synchronized void verifyOnline() {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null && storedKey != null && !checking) {
            checking = true;
            attempted = true;
            lastAttemptNs = System.nanoTime();
            String keyAtSend = storedKey;
            String nameAtSend = client.player.getName().getString();
            if (lastOkNs == 0L) {
               statusText = "đang xác thực";
            }

            String body = "{\"key\":\"" + escapeJson(keyAtSend) + "\",\"authType\":\"player\",\"playerName\":\"" + escapeJson(nameAtSend) + "\"}";

            HttpRequest request;
            try {
               request = HttpRequest.newBuilder()
                  .uri(URI.create("https://snadpremiun.vietanduong35.workers.dev/verify"))
                  .timeout(Duration.ofSeconds(8L))
                  .header("Content-Type", "application/json")
                  .POST(BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                  .build();
            } catch (Exception var6) {
               checking = false;
               statusText = "lỗi tạo yêu cầu xác thực";
               return;
            }

            HTTP.sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8)).whenComplete((response, error) -> {
               try {
                  handle(client, (HttpResponse<String>)response, error, keyAtSend, nameAtSend);
               } catch (Throwable var9) {
                  synchronized (AutoSandOrders.License.class) {
                     checking = false;
                     statusText = "lỗi xử lý phản hồi máy chủ";
                  }
               }
            });
         }
      }

      private static void handle(Minecraft client, HttpResponse<String> response, Throwable error, String keyAtSend, String nameAtSend) {
         String bound = null;
         int outcome;
         String text;
         if (error == null && response != null) {
            int code = response.statusCode();
            JsonObject o = parseObject(response.body());
            if (code >= 500 || code == 429 || code == 408) {
               outcome = 2;
               text = "máy chủ đang lỗi (HTTP " + code + ")";
            } else if (o == null) {
               if (code >= 400) {
                  outcome = 1;
                  text = "key bị từ chối (HTTP " + code + ")";
               } else {
                  outcome = 2;
                  text = "máy chủ trả dữ liệu không hợp lệ";
               }
            } else {
               Boolean v = boolField(o, "valid");
               bound = firstString(o, "playerName", "player_name");
               if (v == null && code >= 200 && code < 300) {
                  outcome = 2;
                  text = "phản hồi thiếu trường valid";
               } else if (code == 200 && Boolean.TRUE.equals(v) && !flaggedDead(o)) {
                  outcome = 0;
                  text = "key hợp lệ";
               } else {
                  outcome = 1;
                  text = reason(o, bound, keyAtSend);
               }
            }
         } else {
            outcome = 2;
            text = "lỗi kết nối máy chủ";
         }

         synchronized (AutoSandOrders.License.class) {
            checking = false;
            if (!keyAtSend.equals(storedKey)) {
               return;
            }

            if (outcome == 0) {
               long now = System.nanoTime();
               verifiedName = nameAtSend;
               boundName = bound;
               lastOkNs = now;
               expireMs = expiryOf(response);
               seal = sealOf(now, keyAtSend, nameAtSend);
            } else if (outcome == 1) {
               lastOkNs = 0L;
               expireMs = 0L;
               seal = 0L;
               boundName = bound;
            }

            statusText = text;
         }

         client.execute(() -> {
            if (client.player != null) {
               String nowx = statusText;
               if (!nowx.equals(lastShown)) {
                  lastShown = nowx;
                  AutoSandOrders.msg(client.player, "Key: " + nowx);
               }
            }
         });
      }

      private static long expiryOf(HttpResponse<String> response) {
         JsonObject o = parseObject(response == null ? null : response.body());
         return o == null ? 0L : expiryMs(o);
      }

      private static JsonObject parseObject(String body) {
         try {
            if (body != null && !body.isBlank()) {
               JsonElement e = JsonParser.parseString(body);
               return e.isJsonObject() ? e.getAsJsonObject() : null;
            } else {
               return null;
            }
         } catch (Exception var2) {
            return null;
         }
      }

      private static Boolean boolField(JsonObject o, String name) {
         JsonElement e = o.get(name);
         return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : null;
      }

      private static String firstString(JsonObject o, String... names) {
         for (String n : names) {
            JsonElement e = o.get(n);
            if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
               String s = e.getAsString().trim();
               if (!s.isEmpty()) {
                  return s;
               }
            }
         }

         return null;
      }

      private static boolean has(String low, String... parts) {
         for (String s : parts) {
            if (low.contains(s)) {
               return true;
            }
         }

         return false;
      }

      private static boolean flaggedDead(JsonObject o) {
         if (Boolean.TRUE.equals(boolField(o, "expired")) || Boolean.TRUE.equals(boolField(o, "revoked")) || Boolean.TRUE.equals(boolField(o, "disabled"))) {
            return true;
         } else if (!Boolean.FALSE.equals(boolField(o, "active")) && !Boolean.FALSE.equals(boolField(o, "enabled"))) {
            String st = firstString(o, "status");
            if (st != null && has(st.toLowerCase(Locale.ROOT), "expire", "disabled", "revok", "inactive", "banned", "suspend")) {
               return true;
            } else {
               long exp = expiryMs(o);
               return exp > 0L && exp + 1000L < System.currentTimeMillis();
            }
         } else {
            return true;
         }
      }

      private static long expiryMs(JsonObject o) {
         for (String n : new String[]{"expiresAt", "expires_at", "expiry", "expireAt", "expiredAt"}) {
            JsonElement e = o.get(n);
            if (e != null && e.isJsonPrimitive()) {
               try {
                  if (!e.getAsJsonPrimitive().isNumber()) {
                     return Instant.parse(e.getAsString().trim()).toEpochMilli();
                  }

                  long v = e.getAsLong();
                  return v < 100000000000L ? v * 1000L : v;
               } catch (Exception var8) {
               }
            }
         }

         return 0L;
      }

      private static String reason(JsonObject o, String bound, String key) {
         String raw = firstString(o, "message", "reason", "error", "status");
         String low = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
         String st = firstString(o, "status");
         if (st != null) {
            low = low + " " + st.toLowerCase(Locale.ROOT);
         }

         if (Boolean.TRUE.equals(boolField(o, "expired"))) {
            return "key đã hết hạn";
         } else if (Boolean.TRUE.equals(boolField(o, "revoked"))) {
            return "key đã bị thu hồi";
         } else if (Boolean.TRUE.equals(boolField(o, "disabled"))
            || Boolean.FALSE.equals(boolField(o, "active"))
            || Boolean.FALSE.equals(boolField(o, "enabled"))) {
            return "key bị quản trị viên tắt";
         } else if (low.isEmpty() && flaggedDead(o)) {
            return "key đã hết hạn";
         } else if (has(low, "expire", "hết hạn", "het han")) {
            return "key đã hết hạn";
         } else if (has(low, "revok", "thu hồi", "thu hoi")) {
            return "key đã bị thu hồi";
         } else if (has(low, "disabled", "inactive", "suspend", "banned", "bị tắt", "đã tắt", "da tat", "khóa", "khoa")) {
            return "key bị quản trị viên tắt";
         } else if (has(low, "not found", "không tồn tại", "khong ton tai", "unknown", "invalid key")) {
            return "key không tồn tại";
         } else if (has(low, "player", "name", "tên", "ten")) {
            return "sai tên người chơi" + (bound != null ? " (key đang gắn với: " + bound + ")" : "");
         } else if (raw == null) {
            return "key bị từ chối";
         } else {
            String safe = raw.replace(key, "***");
            if (safe.length() > 100) {
               safe = safe.substring(0, 100);
            }

            return "key bị từ chối: " + safe;
         }
      }

      static boolean valid() {
         long ok = lastOkNs;
         if (ok == 0L) {
            return false;
         } else {
            long exp = expireMs;
            if (exp > 0L && System.currentTimeMillis() >= exp) {
               lastOkNs = 0L;
               seal = 0L;
               statusText = "key đã hết hạn";
               return false;
            } else {
               long age = System.nanoTime() - ok;
               if (age >= 0L && age < 900000000000L) {
                  String name = verifiedName;
                  if (seal != sealOf(ok, storedKey, name)) {
                     return false;
                  } else {
                     LocalPlayer p = Minecraft.getInstance().player;
                     return p == null || name == null || name.equals(p.getName().getString());
                  }
               } else {
                  return false;
               }
            }
         }
      }

      static String status() {
         return statusText;
      }
   }

   static final class Lobby {
      static int ph = 0;
      static int t = 0;
      static int seen = 0;
      static int tries = 0;
      static int to = 0;
      static boolean force = false;

      private Lobby() {
      }

      static void reset() {
         ph = 0;
         t = 0;
         seen = 0;
         tries = 0;
         to = 0;
      }

      static boolean isLobbyItemType(ItemStack st) {
         return !st.isEmpty() && (st.is(Items.COMPASS) || st.is(Items.RECOVERY_COMPASS) || st.is(Items.CLOCK));
      }

      static boolean isLobbyItemName(ItemStack st) {
         return !st.isEmpty() && AutoSandOrders.nameHas(st, "la ban", "compass", "dong ho", "clock", "chon server", "server", "menu", "kingsmp");
      }

      static List<Integer> candidates(LocalPlayer p) {
         List<Integer> r = new ArrayList<>();

         for (int i = 0; i < 9; i++) {
            if (isLobbyItemType(p.getInventory().getItem(i))) {
               r.add(i);
            }
         }

         for (int ix = 0; ix < 9; ix++) {
            if (!r.contains(ix) && isLobbyItemName(p.getInventory().getItem(ix))) {
               r.add(ix);
            }
         }

         if (!r.contains(4) && !p.getInventory().getItem(4).isEmpty()) {
            r.add(4);
         }

         return r;
      }

      static int compassSlot(LocalPlayer p) {
         List<Integer> c = candidates(p);
         return c.isEmpty() ? -1 : c.get(tries % c.size());
      }

      static boolean strong(LocalPlayer p) {
         for (int i = 0; i < 9; i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (isLobbyItemType(st) || isLobbyItemName(st)) {
               return true;
            }
         }

         return false;
      }

      static boolean inLobby(LocalPlayer p) {
         if (AutoMiner.hasAnyShovel(p)) {
            return false;
         } else if (!AutoMiner.recovering() && !AutoMiner.deathRecorded) {
            boolean any = false;

            for (int i = 0; i < 9; i++) {
               ItemStack st = p.getInventory().getItem(i);
               if (!st.isEmpty()) {
                  if (AutoSandOrders.keepItem(st) && !AutoSandOrders.isWeapon(st)) {
                     return false;
                  }

                  any = true;
               }
            }

            return any;
         } else {
            return strong(p);
         }
      }

      static boolean tick(Minecraft mc, LocalPlayer p) {
         if (force && ph == 0) {
            force = false;
            begin(mc, p);
            return true;
         } else if (!AutoSandOrders.autoRejoin) {
            return false;
         } else if (ph == 0) {
            ChestMenu h0 = AutoSandOrders.container(mc);
            if (h0 != null && AutoSandOrders.loaded(h0) && inLobby(p)) {
               int k = AutoSandOrders.find(h0, st -> AutoSandOrders.nameHas(st, "kingsmp", "king smp"), -1);
               if (k >= 0) {
                  AutoSandOrders.click(mc, p, h0, k, ClickType.PICKUP);
                  ph = 4;
                  t = 20;
                  to = 400;
                  return true;
               }
            }

            if (inLobby(p)) {
               if (++seen >= 20) {
                  begin(mc, p);
               }
            } else {
               seen = 0;
            }

            return ph != 0;
         } else if (!inLobby(p)) {
            finish(mc, p);
            return true;
         } else if (t > 0) {
            t--;
            return true;
         } else {
            switch (ph) {
               case 1:
                  int slot = compassSlot(p);
                  if (slot < 0) {
                     reset();
                     return false;
                  }

                  if (AutoSandOrders.screenBlocks(mc)) {
                     p.closeContainer();
                     t = 5;
                     return true;
                  }

                  AutoSandOrders.setSelected(p, slot);
                  ph = 2;
                  t = 6;
                  break;
               case 2:
                  mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                  p.swing(InteractionHand.MAIN_HAND);
                  ph = 3;
                  to = 100;
                  break;
               case 3:
                  ChestMenu h = AutoSandOrders.container(mc);
                  if (h != null && AutoSandOrders.loaded(h)) {
                     int i = AutoSandOrders.find(h, st -> AutoSandOrders.nameHas(st, "kingsmp", "king smp"), -1);
                     if (i < 0) {
                        i = 24;
                     }

                     AutoSandOrders.click(mc, p, h, i, ClickType.PICKUP);
                     ph = 4;
                     t = 20;
                     to = 400;
                  } else if (--to <= 0) {
                     retry(mc, p);
                  }
                  break;
               case 4:
                  if (--to <= 0) {
                     retry(mc, p);
                  }
                  break;
               default:
                  reset();
            }

            return true;
         }
      }

      static void begin(Minecraft mc, LocalPlayer p) {
         AutoSandOrders.stopMining(mc, p);
         AutoSandOrders.guardReset(mc, p);
         AutoSandOrders.state = AutoSandOrders.S.MINING;
         AutoSandOrders.wait = 0;
         AutoSandOrders.cooldown = 0;
         if (AutoSandOrders.screenBlocks(mc)) {
            p.closeContainer();
         }

         ph = 1;
         t = 30;
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < 9; i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (!st.isEmpty()) {
               sb.append(i + 1).append('=').append(st.getHoverName().getString()).append(" | ");
            }
         }

         AutoSandOrders.msg(p, "Đang ở sảnh -> mở menu chọn KingSMP. Thanh công cụ: " + sb);
      }

      static void retry(Minecraft mc, LocalPlayer p) {
         if (AutoSandOrders.screenBlocks(mc)) {
            p.closeContainer();
         }

         if (++tries >= 8) {
            AutoSandOrders.msg(p, "Thử quay lại KingSMP 8 lần không được. Tắt AutoSand.");
            AutoSandOrders.setEnabled(mc, false);
         } else {
            AutoSandOrders.msg(p, "Chưa vào lại được KingSMP, thử lần " + (tries + 1) + "/8.");
            ph = 1;
            t = 40;
         }
      }

      static void finish(Minecraft mc, LocalPlayer p) {
         reset();
         if (AutoSandOrders.screenBlocks(mc)) {
            p.closeContainer();
         }

         AutoMiner.stop(mc);
         AutoMiner.freshStart();
         AutoSandOrders.guardReset(mc, p);
         AutoMiner.baseY = -1000000;
         AutoSandOrders.nukerState = null;
         AutoSandOrders.state = AutoSandOrders.S.MINING;
         AutoSandOrders.cooldown = 100;
         AutoSandOrders.rejoinWait = 200;
         if (AutoSandOrders.useNuker) {
            AutoSandOrders.setNuker(p, false);
         }

         if (AutoMiner.rec != 0) {
            AutoMiner.rec = 1;
            AutoMiner.recTimer = 60;
            AutoMiner.homeTries = 0;
            AutoMiner.teleported = false;
            AutoMiner.kitStep = 2;
         }

         AutoSandOrders.msg(p, "Đã quay lại KingSMP, chờ 10 giây rồi đào tiếp.");
      }
   }

   static enum Mode {
      BUILTIN("Tự đào"),
      KEYS("Giữ phím"),
      NUKER("Chỉ Nuker");

      final String label;

      private Mode(String l) {
         this.label = l;
      }

      AutoSandOrders.Mode next() {
         return values()[(this.ordinal() + 1) % values().length];
      }
   }

   static final class Reconnect {
      static ServerData last = null;
      static int wait = 0;
      static int tries = 0;

      private Reconnect() {
      }

      static void connected(Minecraft mc) {
         ServerData d = mc.getCurrentServer();
         if (d != null) {
            last = d;
         }

         wait = 0;
         tries = 0;
      }

      static void tick(Minecraft mc) {
         if (AutoSandOrders.enabled && AutoSandOrders.autoRejoin && last != null) {
            if (!(mc.screen instanceof DisconnectedScreen)) {
               wait = 0;
            } else if (++wait >= 100) {
               wait = 0;
               if (++tries > 30) {
                  AutoSandOrders.enabled = false;
               } else {
                  try {
                     Method m = null;

                     for (Method c : ConnectScreen.class.getDeclaredMethods()) {
                        Class<?>[] pt = c.getParameterTypes();
                        if (Modifier.isStatic(c.getModifiers())
                           && pt.length >= 5
                           && pt[0] == Screen.class
                           && pt[1] == Minecraft.class
                           && pt[2] == ServerAddress.class
                           && pt[3] == ServerData.class) {
                           m = c;
                           break;
                        }
                     }

                     if (m == null) {
                        return;
                     }

                     Object[] args = new Object[m.getParameterCount()];
                     args[0] = mc.screen;
                     args[1] = mc;
                     args[2] = ServerAddress.parseString(last.ip);
                     args[3] = last;
                     if (m.getParameterTypes()[4] == boolean.class) {
                        args[4] = Boolean.FALSE;
                     }

                     m.setAccessible(true);
                     m.invoke(null, args);
                  } catch (Throwable var7) {
                  }
               }
            }
         }
      }
   }

   static enum S {
      MINING,
      WAIT_ORDERS,
      WAIT_DELIVER,
      DEPOSIT,
      WAIT_CONFIRM,
      FINISH,
      MEND_PREP,
      MEND_THROW,
      MEND_RESTORE,
      BUY_PREP,
      BUY_SHOP,
      BUY_GEAR,
      BUY_AMOUNT,
      BUY_CONFIRM,
      BUY_DONE,
      EAT_PREP,
      EAT_USE,
      JUNK;
   }
}
