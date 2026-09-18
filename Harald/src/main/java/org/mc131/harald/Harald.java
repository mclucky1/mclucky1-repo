package org.mc131.harald;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.mc131.harald.entity.HaraldEntity;
import org.mc131.harald.entity.ModEntityInit;
import org.slf4j.Logger;

import java.util.Map;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Harald.MODID)
public class Harald {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "harald";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Harald(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        modEventBus.addListener(this::registerAttributes);

        ModEntityInit.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(
                ModEntityInit.HARALD.get(),
                HaraldEntity.createAttributes().build()
        );
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("harald")
                        .then(Commands.literal("initialize")
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();

                                    if (source.getEntity() instanceof  ServerPlayer player) {
                                        HaraldEntity harald = ModEntityInit.HARALD.get().create(player.level());

                                        if (harald != null) {
                                            harald.setPos(
                                                    player.getX(),
                                                    player.getY(),
                                                    player.getZ()
                                            );

                                            player.level().addFreshEntity(harald);

                                            source.sendSuccess(
                                                    () -> Component.literal("Harald initialized"),
                                                    true
                                            );

                                            return 1;
                                        }
                                    }

                                    source.sendFailure(Component.literal("Couldn't spawn Harald"));

                                    return 0;
                                }
                                )
                        )
                        .then(Commands.literal("goTo")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> {

                                            ServerPlayer player =
                                                    context.getSource().getPlayerOrException();

                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(
                                                    context,
                                                    "pos"
                                            );

                                            HaraldEntity harald = find(player);

                                            if (harald == null || !harald.isAlive()) {
                                                context.getSource().sendFailure(
                                                        Component.literal("Harald is not initialized")
                                                );
                                                return 0;
                                            }

                                            if (harald.navigateTo(pos)) {
                                                context.getSource().sendSuccess(
                                                        () -> Component.literal(
                                                                "Harald moving to "
                                                                        + pos.getX() + " "
                                                                        + pos.getY() + " "
                                                                        + pos.getZ()
                                                        ),
                                                        true
                                                );
                                                return 1;
                                            }

                                            context.getSource().sendFailure(
                                                    Component.literal("Harald couldn't find a path")
                                            );
                                            return 0;
                                        })
                                )
                        )
                        .then(Commands.literal("mine")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> {

                                            ServerPlayer player =
                                                    context.getSource().getPlayerOrException();

                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(
                                                    context,
                                                    "pos"
                                            );

                                            HaraldEntity harald = find(player);

                                            if (harald == null || !harald.isAlive()) {
                                                context.getSource().sendFailure(
                                                        Component.literal("Harald is not initialized")
                                                );
                                                return 0;
                                            }

                                            if (!harald.startMining(pos)) {
                                                context.getSource().sendFailure(
                                                        Component.literal("Harald cannot mine this block")
                                                );
                                                return 0;
                                            }

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "Harald mined " +
                                                                    pos.getX() + " " +
                                                                    pos.getY() + " " +
                                                                    pos.getZ()
                                                    ),
                                                    true
                                            );

                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("place")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument(
                                                                "slot",
                                                                IntegerArgumentType.integer(0, 35)
                                                        )
                                                        .executes(context -> {

                                                            ServerPlayer player =
                                                                    context.getSource().getPlayerOrException();

                                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(
                                                                    context,
                                                                    "pos"
                                                            );

                                                            int slot = IntegerArgumentType.getInteger(
                                                                    context,
                                                                    "slot"
                                                            );

                                                            HaraldEntity harald = find(player);

                                                            if (harald == null || !harald.isAlive()) {
                                                                context.getSource().sendFailure(
                                                                        Component.literal(
                                                                                "Harald is not initialized"
                                                                        )
                                                                );
                                                                return 0;
                                                            }

                                                            if (!harald.startPlace(pos, slot)) {
                                                                context.getSource().sendFailure(
                                                                        Component.literal(
                                                                                "Harald couldn't place the block"
                                                                        )
                                                                );
                                                                return 0;
                                                            }

                                                            context.getSource().sendSuccess(
                                                                    () -> Component.literal(
                                                                            "Harald placing block at "
                                                                                    + pos.getX() + " "
                                                                                    + pos.getY() + " "
                                                                                    + pos.getZ()
                                                                                    + " using slot "
                                                                                    + slot
                                                                    ),
                                                                    true
                                                            );

                                                            return 1;
                                                        })
                                        )
                                )
                        )
                        .then(Commands.literal("item")
                                .then(Commands.literal("list")
                                        .executes(context -> {

                                            ServerPlayer player =
                                                    context.getSource().getPlayerOrException();

                                            HaraldEntity harald = find(player);

                                            if (harald == null || !harald.isAlive()) {
                                                context.getSource().sendFailure(
                                                        Component.literal("Harald is not initialized")
                                                );
                                                return 0;
                                            }

                                            boolean inventoryFound = false;

                                            // Inventory
                                            for (int i = 0; i < harald.getInventory().getContainerSize(); i++) {
                                                ItemStack stack = harald.getInventory().getItem(i);

                                                if (!stack.isEmpty()) {
                                                    if (!inventoryFound) {
                                                        inventoryFound = true;

                                                        context.getSource().sendSuccess(
                                                                () -> Component.literal("Inventory:"),
                                                                false
                                                        );
                                                    }

                                                    final int slot = i;
                                                    final String itemId = BuiltInRegistries.ITEM
                                                            .getKey(stack.getItem())
                                                            .toString();
                                                    final int count = stack.getCount();

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal(
                                                                    "Slot " + slot + ": "
                                                                            + itemId
                                                                            + " x"
                                                                            + count
                                                            ),
                                                            false
                                                    );
                                                }
                                            }

                                            boolean armorFound = false;

                                            // Armor
                                            EquipmentSlot[] armorSlots = {
                                                    EquipmentSlot.HEAD,
                                                    EquipmentSlot.CHEST,
                                                    EquipmentSlot.LEGS,
                                                    EquipmentSlot.FEET
                                            };

                                            for (int i = 0; i < armorSlots.length; i++) {
                                                ItemStack stack = harald.getItemBySlot(armorSlots[i]);

                                                if (!stack.isEmpty()) {
                                                    if (!armorFound) {
                                                        armorFound = true;

                                                        context.getSource().sendSuccess(
                                                                () -> Component.literal("Armor:"),
                                                                false
                                                        );
                                                    }

                                                    final int slot = i;
                                                    final String itemId = BuiltInRegistries.ITEM
                                                            .getKey(stack.getItem())
                                                            .toString();
                                                    final int count = stack.getCount();

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal(
                                                                    "Slot " + slot + ": "
                                                                            + itemId
                                                                            + " x"
                                                                            + count
                                                            ),
                                                            false
                                                    );
                                                }
                                            }

                                            if (!inventoryFound && !armorFound) {
                                                context.getSource().sendSuccess(
                                                        () -> Component.literal(
                                                                "Harald's inventory is empty"
                                                        ),
                                                        false
                                                );
                                            }

                                            return 1;
                                        })
                                )
                                .then(Commands.literal("drop")
                                        .then(Commands.argument("slot", StringArgumentType.word())
                                                .executes(context -> {
                                                    ServerPlayer player =
                                                            context.getSource().getPlayerOrException();

                                                    HaraldEntity harald = find(player);

                                                    if (harald == null || !harald.isAlive()) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("Harald is not initialized")
                                                        );
                                                        return 0;
                                                    }

                                                    String slot = StringArgumentType.getString(context, "slot");

                                                    if (slot.equalsIgnoreCase("all")) {
                                                        harald.dropAllItems();

                                                        context.getSource().sendSuccess(
                                                                () -> Component.literal("Harald dropped all items"),
                                                                true
                                                        );

                                                        return 1;
                                                    }

                                                    int slotNumber;

                                                    try {
                                                        slotNumber = Integer.parseInt(slot);
                                                    } catch (NumberFormatException e) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("Slot must be between 0 and 35 or 'all'")
                                                        );
                                                        return 0;
                                                    }

                                                    if (slotNumber < 0 || slotNumber > 35) {
                                                        context.getSource().sendFailure(
                                                                Component.literal("Slot must be between 0 and 35")
                                                        );
                                                        return 0;
                                                    }

                                                    if (harald.dropItem(slotNumber)) {
                                                        context.getSource().sendSuccess(
                                                                () -> Component.literal(
                                                                        "Harald dropped item from slot " + slotNumber + ""
                                                                ),
                                                                true
                                                        );
                                                        return 1;
                                                    }

                                                    context.getSource().sendFailure(
                                                            Component.literal("That slot is empty")
                                                    );

                                                    return 0;
                                                })
                                        )
                                )
                                .then(Commands.literal("armor")
                                        .then(Commands.literal("equip")
                                                .then(Commands.argument(
                                                                        "slot",
                                                                        IntegerArgumentType.integer(0, 35)
                                                                )
                                                                .executes(context -> {
                                                                    CommandSourceStack source = context.getSource();
                                                                    ServerPlayer player = source.getPlayerOrException();

                                                                    HaraldEntity harald = find(player);

                                                                    if (harald == null || !harald.isAlive()) {
                                                                        source.sendFailure(
                                                                                Component.literal("Harald is not initialized")
                                                                        );
                                                                        return 0;
                                                                    }

                                                                    int slot = IntegerArgumentType.getInteger(
                                                                            context,
                                                                            "slot"
                                                                    );

                                                                    if (!harald.equipArmor(slot)) {
                                                                        source.sendFailure(
                                                                                Component.literal(
                                                                                        "Harald couldn't equip the item from slot " + slot
                                                                                )
                                                                        );
                                                                        return 0;
                                                                    }

                                                                    source.sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "Harald equipped item from slot " + slot
                                                                            ),
                                                                            true
                                                                    );

                                                                    return 1;
                                                                })
                                                )
                                        )
                                        .then(Commands.literal("unequip")
                                                .then(Commands.argument(
                                                                        "slot",
                                                                        IntegerArgumentType.integer(0, 3)
                                                                )
                                                                .executes(context -> {
                                                                    CommandSourceStack source = context.getSource();
                                                                    ServerPlayer player = source.getPlayerOrException();

                                                                    HaraldEntity harald = find(player);

                                                                    if (harald == null || !harald.isAlive()) {
                                                                        source.sendFailure(
                                                                                Component.literal("Harald is not initialized")
                                                                        );
                                                                        return 0;
                                                                    }

                                                                    int slot = IntegerArgumentType.getInteger(
                                                                            context,
                                                                            "slot"
                                                                    );

                                                                    if (!harald.unequipArmor(slot)) {
                                                                        source.sendFailure(
                                                                                Component.literal(
                                                                                        "Harald couldn't unequip armor slot " + slot
                                                                                )
                                                                        );
                                                                        return 0;
                                                                    }

                                                                    source.sendSuccess(
                                                                            () -> Component.literal(
                                                                                    "Harald unequipped armor slot " + slot
                                                                            ),
                                                                            true
                                                                    );

                                                                    return 1;
                                                                })
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("status")
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerPlayer player = source.getPlayerOrException();

                                    HaraldEntity harald = find(player);

                                    if (harald == null || !harald.isAlive()) {
                                        source.sendFailure(Component.literal("Harald is not initialized"));
                                        return 0;
                                    }

                                    Map<String, Float> status = harald.getStatus();

                                    source.sendSuccess(
                                            () -> Component.literal(
                                                    "Health: " + status.get("health") + "/" + status.get("maxHealth")
                                                            + "\nHunger: " + status.get("hunger")  + "/" + status.get("maxHunger")
                                                            + "\nSaturation: " + status.get("saturation")  + "/" + status.get("maxSaturation")
                                            ),
                                            true
                                    );
                                    return 1;
                                })
                        )

        );
    }

    public static HaraldEntity find(ServerPlayer player) {
        return player.level().getEntitiesOfClass(
                HaraldEntity.class,
                player.getBoundingBox().inflate(64),
                HaraldEntity::isAlive
        ).stream().findFirst().orElse(null);
    }
}