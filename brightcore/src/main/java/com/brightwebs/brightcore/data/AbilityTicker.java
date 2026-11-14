package com.brightwebs.brightcore.data;

import com.brightwebs.brightcore.BrightCore;
import com.brightwebs.brightcore.api.Ability;
import com.brightwebs.brightcore.api.AbilityAPI;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Server-side upkeep: passive ticks, on-hit hooks, mana regeneration. */
@EventBusSubscriber(modid = BrightCore.ID)
public final class AbilityTicker {
    /** Mana gained per second. */
    public static final int MANA_REGEN_PER_SECOND = 3;

    private AbilityTicker() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) player.level();

        for (Ability ability : AbilityAPI.owned(player)) {
            ability.tick(player, level);
        }

        if (player.tickCount % 20 == 0) {
            AbilityData data = AbilityAPI.get(player);
            AbilityData updated = data.withExpiredCooldownsRemoved(level.getGameTime());
            if (updated.mana() < updated.maxMana()) {
                updated = updated.withMana(updated.mana() + MANA_REGEN_PER_SECOND);
            }
            if (!updated.equals(data)) player.setData(BrightAttachments.ABILITIES, updated);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof ServerPlayer player)) return;
        for (Ability ability : AbilityAPI.owned(player)) {
            ability.onHit(player, event.getEntity(), event.getInflictedDamage());
        }
    }
}
