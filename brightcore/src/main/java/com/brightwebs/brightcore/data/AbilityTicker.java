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

// server side upkeep: passive ticks, on-hit hooks, clearing cooldowns
@EventBusSubscriber(modid = BrightCore.ID)
public final class AbilityTicker {
    private AbilityTicker() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("AbilityTicker.onPlayerTick", () -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            ServerLevel level = (ServerLevel) player.level();
    
            AbilityData owned = AbilityAPI.get(player);
            for (Ability ability : AbilityAPI.owned(player)) {
                if (ability.isSwitchable()) { if (AbilityAPI.isSwitchedOn(player, ability)) ability.tick(player, level); }
                else if (!ability.isToggle() || owned.isToggled(ability.id())) ability.tick(player, level);
            }
    
            if (player.tickCount % 20 == 0) {
                AbilityData data = AbilityAPI.get(player);
                AbilityData updated = data.withExpiredCooldownsRemoved(level.getGameTime());
                if (!updated.equals(data)) player.setData(BrightAttachments.ABILITIES, updated);
            }
        });
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        com.brightwebs.brightcore.api.util.Safe.run("AbilityTicker.onDamage", () -> {
            if (!(event.getSource().getDirectEntity() instanceof ServerPlayer player)) return;
            for (Ability ability : AbilityAPI.owned(player)) {
                ability.onHit(player, event.getEntity(), event.getInflictedDamage());
            }
        });
    }
}
