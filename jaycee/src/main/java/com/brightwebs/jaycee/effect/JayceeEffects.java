package com.brightwebs.jaycee.effect;

import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.skill.Pressure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

// our status effects, numbers taken from tensura's
public final class JayceeEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, JayceeMod.ID);

    private static Identifier id(String s) { return Identifier.fromNamespaceAndPath(JayceeMod.ID, s); }

    // fear from haki / coercion. -5% speed a level (level 1 doesn't slow you yet, same as tensura).
    // at level 5+ you take damage if you're near whoever scared you, at level 10+ you just take damage.
    // mobs at level 5+ are too scared to go after anything
    public static final DeferredHolder<MobEffect, MobEffect> FEAR = EFFECTS.register("fear", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x3A4B52) {
        {
            addAttributeModifier(Attributes.MOVEMENT_SPEED, id("fear"), -0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        @Override public void addAttributeModifiers(AttributeMap map, int amp) { if (amp > 0) super.addAttributeModifiers(map, amp); }
        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amp) { return duration % 40 == 0; }
        @Override public boolean applyEffectTick(ServerLevel level, LivingEntity e, int amp) {
            Pressure.fearTick(level, e, amp);
            return true;
        }
    });

    // strength / steel strength: +6 attack damage a level
    public static final DeferredHolder<MobEffect, MobEffect> STRENGTHEN = EFFECTS.register("strengthen", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xD04030) {
        {
            addAttributeModifier(Attributes.ATTACK_DAMAGE, id("strengthen"), 6.0, AttributeModifier.Operation.ADD_VALUE);
        }
    });

    // paralysis: slower, can't jump as high, slower swings
    public static final DeferredHolder<MobEffect, MobEffect> PARALYSIS = EFFECTS.register("paralysis", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xE8D85A) {
        {
            addAttributeModifier(Attributes.MOVEMENT_SPEED, id("paralysis"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.JUMP_STRENGTH, id("paralysis"), -0.33, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.ATTACK_SPEED, id("paralysis"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    });

    // demon lord haki's coat: your physical hits cut through resistances (see Pressure.onHitWithCoat)
    public static final DeferredHolder<MobEffect, MobEffect> HAKI_COAT = EFFECTS.register("haki_coat", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x3A0101) {});

    private JayceeEffects() {}

    public static void init(IEventBus bus) { EFFECTS.register(bus); }
}
