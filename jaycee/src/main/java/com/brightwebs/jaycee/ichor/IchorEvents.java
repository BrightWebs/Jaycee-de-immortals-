package com.brightwebs.jaycee.ichor;

import com.brightwebs.jaycee.JayceeMod;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** First-join Birthright, and the faint gold glow of the divine Grades. */
@EventBusSubscriber(modid = JayceeMod.ID)
public final class IchorEvents {
    private static final DustParticleOptions GOLD = new DustParticleOptions(0xF3CF82, 0.8f);

    private IchorEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && IchorAPI.get(player).birthright() == Birthright.UNDRAWN) {
            IchorAPI.drawBirthright(player);
        }
    }

    /**
     * Demigod: a few gold motes now and then. Lesser God and up: a little more.
     * Kept subtle on purpose; a god's full True Form comes later as an ability.
     */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        Grade grade = IchorAPI.get(player).grade();
        if (grade.number() < Grade.DEMIGOD.number()) return;
        boolean god = grade.number() >= Grade.LESSER_GOD.number();
        if (player.tickCount % (god ? 10 : 20) != 0) return;
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(GOLD, player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(),
                god ? 4 : 2, 0.35, 0.5, 0.35, 0.0);
    }
}
