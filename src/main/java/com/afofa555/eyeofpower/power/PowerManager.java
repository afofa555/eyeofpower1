package com.afofa555.eyeofpower.power;

import com.afofa555.eyeofpower.EyeOfPowerMod;
import com.afofa555.eyeofpower.ModItems;
import com.afofa555.eyeofpower.network.TimeStopSyncPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/**
 * Вся серверная логика сил Глаза: остановка времени и чёрная дыра.
 */
@EventBusSubscriber(modid = EyeOfPowerMod.MODID)
public class PowerManager {

    public static final int POWER_TIME_STOP = 0;
    public static final int POWER_BLACK_HOLE = 1;

    private static final UUID NONE = new UUID(0L, 0L);

    // ---------- Время ----------
    private static UUID timeStopper = null;
    private static boolean wasFrozenBefore = false;
    private static int timeStopAge = 0;
    private static final Map<UUID, Vec3> frozenPositions = new HashMap<>();
    private static final Set<UUID> noGravityFlagged = new HashSet<>();

    private static final DustColorTransitionOptions GREEN_DUST = new DustColorTransitionOptions(
            new Vector3f(0.1f, 1.0f, 0.25f), new Vector3f(0.0f, 0.55f, 0.15f), 1.8f);
    private static final DustParticleOptions LIME_DUST = new DustParticleOptions(
            new Vector3f(0.55f, 1.0f, 0.4f), 1.4f);

    // ---------- Чёрная дыра ----------
    private static final double HOLE_RADIUS = 22.0;
    private static final int HOLE_LIFETIME = 400; // 20 секунд
    private static final double SWALLOW_DISTANCE = 2.0;
    private static final List<BlackHole> holes = new ArrayList<>();

    private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(
            new Vector3f(0.0f, 0.0f, 0.0f), 2.2f);
    private static final DustParticleOptions DARK_DUST = new DustParticleOptions(
            new Vector3f(0.06f, 0.0f, 0.1f), 1.6f);

    private static final class BlackHole {
        final ResourceKey<Level> dimension;
        final Vec3 center;
        final UUID owner;
        int age = 0;

        BlackHole(ResourceKey<Level> dimension, Vec3 center, UUID owner) {
            this.dimension = dimension;
            this.center = center;
            this.owner = owner;
        }
    }

    // =====================================================================
    //  Вход от клиента
    // =====================================================================

    public static boolean isWearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.EYE_OF_POWER.get());
    }

    public static void usePower(ServerPlayer player, int power) {
        if (!isWearing(player)) {
            player.displayClientMessage(Component.translatable("message.eyeofpower.not_wearing"), true);
            return;
        }
        switch (power) {
            case POWER_TIME_STOP -> toggleTimeStop(player);
            case POWER_BLACK_HOLE -> toggleBlackHole(player);
            default -> { }
        }
    }

    // =====================================================================
    //  Остановка времени
    // =====================================================================

    private static void toggleTimeStop(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (timeStopper != null) {
            if (timeStopper.equals(player.getUUID())) {
                stopTime(server);
            } else {
                player.displayClientMessage(Component.translatable("message.eyeofpower.time_other"), true);
            }
            return;
        }
        startTime(server, player);
    }

    private static void startTime(MinecraftServer server, ServerPlayer player) {
        timeStopper = player.getUUID();
        timeStopAge = 0;
        frozenPositions.clear();
        noGravityFlagged.clear();

        ServerTickRateManager rate = server.tickRateManager();
        wasFrozenBefore = rate.isFrozen();
        // Ванильная заморозка: стоят блоки, рост, вода, redstone, мобы, предметы, снаряды, погода и время суток.
        // Игроки в ванильной заморозке продолжают тикать - остальных игроков замораживаем сами.
        rate.setFrozen(true);

        PacketDistributor.sendToAllPlayers(new TimeStopSyncPayload(true, timeStopper));

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    private static void stopTime(MinecraftServer server) {
        UUID oldStopper = timeStopper;
        timeStopper = null;

        server.tickRateManager().setFrozen(wasFrozenBefore);

        for (UUID id : noGravityFlagged) {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) {
                sp.setNoGravity(false);
            }
        }
        noGravityFlagged.clear();
        frozenPositions.clear();

        PacketDistributor.sendToAllPlayers(new TimeStopSyncPayload(false, NONE));

        if (oldStopper != null) {
            ServerPlayer sp = server.getPlayerList().getPlayer(oldStopper);
            if (sp != null) {
                sp.serverLevel().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0f, 0.6f);
            }
        }
    }

    private static boolean isFrozenPlayer(Player player) {
        return timeStopper != null && !player.getUUID().equals(timeStopper);
    }

    private static void tickTimeStop(MinecraftServer server) {
        if (timeStopper == null) {
            return;
        }
        ServerPlayer stopper = server.getPlayerList().getPlayer(timeStopper);
        if (stopper == null || !stopper.isAlive() || !isWearing(stopper)) {
            stopTime(server);
            return;
        }
        timeStopAge++;

        // Замораживаем остальных игроков
        for (ServerPlayer other : server.getPlayerList().getPlayers()) {
            if (other.getUUID().equals(timeStopper)) {
                continue;
            }
            if (noGravityFlagged.add(other.getUUID())) {
                other.setNoGravity(true);
            }
            Vec3 frozen = frozenPositions.computeIfAbsent(other.getUUID(), id -> other.position());
            other.setDeltaMovement(Vec3.ZERO);
            if (!other.isPassenger() && other.position().distanceToSqr(frozen) > 0.01) {
                other.connection.teleport(frozen.x, frozen.y, frozen.z, other.getYRot(), other.getXRot());
            }
        }

        // Зелёные эффекты
        ServerLevel level = stopper.serverLevel();
        double cx = stopper.getX();
        double cy = stopper.getY() + stopper.getBbHeight() * 0.5;
        double cz = stopper.getZ();
        RandomSource random = level.random;

        if (timeStopAge <= 40) {
            int count = Math.max(25, 100 - timeStopAge * 2);
            for (int i = 0; i < count; i++) {
                double u = random.nextDouble() * 2.0 - 1.0;
                double theta = random.nextDouble() * Math.PI * 2.0;
                double s = Math.sqrt(1.0 - u * u);
                double dx = s * Math.cos(theta);
                double dy = u;
                double dz = s * Math.sin(theta);
                double speed = 0.15 + random.nextDouble() * 0.45;
                switch (i % 3) {
                    case 0 -> level.sendParticles(GREEN_DUST, cx, cy, cz, 0, dx, dy, dz, speed);
                    case 1 -> level.sendParticles(LIME_DUST, cx, cy, cz, 0, dx, dy, dz, speed);
                    default -> level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, cy, cz, 0, dx, dy, dz, speed * 1.6);
                }
            }
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cx, cy, cz, 12, 1.8, 1.2, 1.8, 0.0);
        }

        if (timeStopAge % 2 == 0) {
            double base = timeStopAge * 0.35;
            for (int i = 0; i < 10; i++) {
                double a = base + i * (Math.PI * 2.0 / 10.0);
                double rr = 1.2;
                double y = stopper.getY() + ((timeStopAge + i * 3) % 20) * 0.1;
                level.sendParticles(GREEN_DUST, cx + Math.cos(a) * rr, y, cz + Math.sin(a) * rr, 1, 0, 0, 0, 0);
            }
        }

        if (timeStopAge % 20 == 0) {
            stopper.displayClientMessage(Component.translatable("message.eyeofpower.time_active"), true);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cx, cy, cz, 6, 1.0, 1.0, 1.0, 0.0);
        }
    }

    // =====================================================================
    //  Чёрная дыра
    // =====================================================================

    private static void toggleBlackHole(ServerPlayer player) {
        // Повторное использование закрывает уже открытую дыру
        Iterator<BlackHole> it = holes.iterator();
        while (it.hasNext()) {
            BlackHole h = it.next();
            if (h.owner.equals(player.getUUID())) {
                ServerLevel lvl = player.server.getLevel(h.dimension);
                if (lvl != null) {
                    collapse(lvl, h);
                }
                it.remove();
                return;
            }
        }

        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(25.0));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 center = hit.getType() == HitResult.Type.MISS
                ? eye.add(look.scale(14.0))
                : hit.getLocation().subtract(look.scale(2.0));

        holes.add(new BlackHole(level.dimension(), center, player.getUUID()));
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 0.5f);
    }

    private static void collapse(ServerLevel level, BlackHole h) {
        level.sendParticles(ParticleTypes.SQUID_INK, h.center.x, h.center.y, h.center.z, 80, 1.2, 1.2, 1.2, 0.25);
        level.sendParticles(BLACK_DUST, h.center.x, h.center.y, h.center.z, 60, 1.5, 1.5, 1.5, 0.0);
        level.playSound(null, h.center.x, h.center.y, h.center.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.5f, 0.3f);
    }

    private static void tickBlackHoles(MinecraftServer server) {
        if (holes.isEmpty()) {
            return;
        }
        Iterator<BlackHole> it = holes.iterator();
        while (it.hasNext()) {
            BlackHole h = it.next();
            ServerLevel level = server.getLevel(h.dimension);
            if (level == null) {
                it.remove();
                continue;
            }
            h.age++;
            if (h.age > HOLE_LIFETIME) {
                collapse(level, h);
                it.remove();
                continue;
            }
            holeEffects(level, h);
            holePull(level, h);
        }
    }

    private static void holeEffects(ServerLevel level, BlackHole h) {
        Vec3 c = h.center;
        RandomSource random = level.random;

        // Ядро
        level.sendParticles(BLACK_DUST, c.x, c.y, c.z, 14, 0.6, 0.6, 0.6, 0.0);
        level.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, 2, 0.5, 0.5, 0.5, 0.01);

        // Вращающийся аккреционный диск
        double t = h.age * 0.28;
        for (int i = 0; i < 28; i++) {
            double ring = 2.0 + (i % 4) * 1.0;
            double a = t * (1.6 - ring * 0.12) + i * (Math.PI * 2.0 / 7.0);
            double x = c.x + Math.cos(a) * ring;
            double z = c.z + Math.sin(a) * ring;
            double y = c.y + Math.sin(a * 2.0 + t) * 0.25;
            level.sendParticles(i % 3 == 0 ? DARK_DUST : BLACK_DUST, x, y, z, 1, 0, 0, 0, 0);
        }

        // Тёмные струи, втягивающиеся в центр
        for (int i = 0; i < 8; i++) {
            double u = random.nextDouble() * 2.0 - 1.0;
            double theta = random.nextDouble() * Math.PI * 2.0;
            double s = Math.sqrt(1.0 - u * u);
            double dist = 6.0 + random.nextDouble() * 12.0;
            double px = c.x + s * Math.cos(theta) * dist;
            double py = c.y + u * dist;
            double pz = c.z + s * Math.sin(theta) * dist;
            Vec3 dir = c.subtract(px, py, pz).normalize();
            level.sendParticles(ParticleTypes.SQUID_INK, px, py, pz, 0, dir.x, dir.y, dir.z, 0.35);
        }

        if (h.age % 60 == 1) {
            level.playSound(null, c.x, c.y, c.z, SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.4f);
        }
    }

    private static void holePull(ServerLevel level, BlackHole h) {
        AABB box = new AABB(h.center, h.center).inflate(HOLE_RADIUS);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> (e instanceof Mob || e instanceof Player)
                        && !e.getUUID().equals(h.owner)   // владелец Глаза не страдает
                        && !e.isSpectator());

        for (LivingEntity e : targets) {
            Vec3 body = e.position().add(0.0, e.getBbHeight() * 0.5, 0.0);
            Vec3 to = h.center.subtract(body);
            double dist = to.length();
            if (dist > HOLE_RADIUS) {
                continue;
            }
            if (dist < SWALLOW_DISTANCE) {
                swallow(level, e);
                continue;
            }
            double step = Math.min(dist, 0.25 + (1.0 - dist / HOLE_RADIUS) * 1.0);
            Vec3 newPos = e.position().add(to.normalize().scale(step));
            moveEntity(e, newPos);
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static void swallow(ServerLevel level, LivingEntity e) {
        double x = e.getX();
        double z = e.getZ();
        level.sendParticles(ParticleTypes.SQUID_INK, x, e.getY() + 1.0, z, 30, 0.4, 0.6, 0.4, 0.1);
        level.playSound(null, x, e.getY(), z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.4f);

        // В бездну под миром
        double abyssY = level.getMinBuildHeight() - 80.0;
        moveEntity(e, new Vec3(x, abyssY, z));
        e.hurt(level.damageSources().fellOutOfWorld(), 1000.0f);
    }

    private static void moveEntity(LivingEntity e, Vec3 pos) {
        if (e instanceof ServerPlayer sp) {
            sp.connection.teleport(pos.x, pos.y, pos.z, sp.getYRot(), sp.getXRot());
            if (frozenPositions.containsKey(sp.getUUID())) {
                frozenPositions.put(sp.getUUID(), pos);
            }
        } else {
            e.setPos(pos.x, pos.y, pos.z);
        }
        e.fallDistance = 0.0f;
    }

    // =====================================================================
    //  События
    // =====================================================================

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        tickTimeStop(server);
        tickBlackHoles(server);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (timeStopper != null) {
            event.getServer().tickRateManager().setFrozen(wasFrozenBefore);
        }
        timeStopper = null;
        frozenPositions.clear();
        noGravityFlagged.clear();
        holes.clear();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && timeStopper != null) {
            PacketDistributor.sendToPlayer(sp, new TimeStopSyncPayload(true, timeStopper));
        }
    }

    // Замороженные игроки не могут действовать
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (isFrozenPlayer(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (isFrozenPlayer(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (isFrozenPlayer(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (isFrozenPlayer(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (isFrozenPlayer(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (isFrozenPlayer(e.getPlayer())) e.setCanceled(true);
    }
}
