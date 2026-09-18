package org.mc131.harald.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.mc131.harald.Harald;
import org.mc131.harald.entity.HaraldEntity;
import org.mc131.harald.entity.ModEntityInit;

@EventBusSubscriber(
        modid = Harald.MODID,
        value = Dist.CLIENT
)
public class HaraldEntityRenderer
        extends MobRenderer<HaraldEntity, PlayerModel<HaraldEntity>> {

    public HaraldEntityRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new PlayerModel<>(
                        context.bakeLayer(ModelLayers.PLAYER),
                        false
                ),
                0.5F
        );

        this.addLayer(
                new HumanoidArmorLayer<>(
                        this,
                        new HumanoidModel<>(
                                context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)
                        ),
                        new HumanoidModel<>(
                                context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)
                        ),
                        context.getModelManager()
                )
        );
    }

    @SubscribeEvent
    public static void registerRenderer(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerEntityRenderer(
                ModEntityInit.HARALD.get(),
                HaraldEntityRenderer::new
        );
    }

    @Override
    public ResourceLocation getTextureLocation(HaraldEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(
                Harald.MODID,
                "textures/entity/harald.png"
        );
    }
}