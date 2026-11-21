package com.brightwebs.jaycee.ichor;

import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// first join birthright, and the faint gold glow on the divine grades
@EventBusSubscriber(modid = JayceeMod.ID)
public final class IchorEvents {
    private static final DustParticleOptions GOLD = new DustParticleOptions(0xF3CF82, 0.8f);

    private IchorEvents() {}

    // paused while we do the race system first, the greek stuff comes back later
    public static final boolean BIRTHRIGHT_ENABLED = false;

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        com.brightwebs.brightcore.api.util.Safe.run("IchorEvents.onLogin", () -> {
            if (BIRTHRIGHT_ENABLED && event.getEntity() instanceof ServerPlayer player
                    && IchorAPI.get(player).birthright() == Birthright.UNDRAWN) {
                IchorAPI.drawBirthright(player);
            }
        });
    }

    // demigod: a few gold specks now and then. lesser god and up: a bit more
    // kept subtle on purpose, a god's full true form comes later as an ability
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("IchorEvents.onTick", () -> {
            if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
            Grade grade = IchorAPI.get(player).grade();
            if (grade.number() < Grade.DEMIGOD.number()) return;
            boolean god = grade.number() >= Grade.LESSER_GOD.number();
            if (player.tickCount % (god ? 10 : 20) != 0) return;
            ServerLevel level = (ServerLevel) player.level();
            level.sendParticles(GOLD, player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(),
                    god ? 4 : 2, 0.35, 0.5, 0.35, 0.0);
        });
    }
}
