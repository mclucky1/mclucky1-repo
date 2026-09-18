package org.mc131.harald.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.mc131.harald.Harald;

public class ModEntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Harald.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<HaraldEntity>> HARALD =
            ENTITIES.register("harald", () ->
                    EntityType.Builder.of(HaraldEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.8f)
                            .build("harald")
            );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
