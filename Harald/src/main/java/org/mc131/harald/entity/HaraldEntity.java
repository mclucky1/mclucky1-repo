package org.mc131.harald.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class HaraldEntity extends PathfinderMob {
    public HaraldEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setCanPickUpLoot(true);

        this.setCustomName(Component.literal("Harald"));
        this.setCustomNameVisible(true);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    private float hunger = 20.0f;
    private float saturation = 0.0f;
    private static final float maxHunger = 20.0f;
    private static final float maxSaturation = 20.0f;
    private int foodCooldown = 0;
    private int healCooldown = 0;

    private final SimpleContainer inventory = new SimpleContainer(36);

    public Container getInventory() {
        return inventory;
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(Component.literal("Harald"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);

        tag.put(
                "HaraldInventory",
                this.inventory.createTag(this.registryAccess())
        );

        tag.putFloat(
                "HaraldHunger",
                this.hunger
        );
        tag.putFloat(
                "HaraldSaturation",
                this.saturation
        );

        tag.putInt(
                "HaraldFoodCooldown",
                this.foodCooldown
        );
        tag.putInt(
                "HaraldHealCooldown",
                this.healCooldown
        );
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("HaraldInventory")) {
            this.inventory.fromTag(
                    tag.getList("HaraldInventory", 10),
                    this.registryAccess()
            );
        }

        if (tag.contains("HaraldHunger")) {
            this.hunger = tag.getFloat("HaraldHunger");
        }
        if (tag.contains("HaraldSaturation")) {
            this.saturation = tag.getFloat("HaraldSaturation");
        }

        if (tag.contains("HaraldFoodCooldown")) {
            this.foodCooldown = tag.getInt("HaraldFoodCooldown");
        }
        if (tag.contains("HaraldHealCooldown")) {
            this.healCooldown = tag.getInt("HaraldHealCooldown");
        }
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level,
            DamageSource damageSource,
            boolean recentlyHit
    ) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        // Inventar droppen
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);

            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack);
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }

        // Rüstung droppen
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        }) {
            ItemStack stack = getItemBySlot(slot);

            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack);
                setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void hurtArmor(DamageSource source, float damage) {
        int damageAmount = (int) Math.max(1.0F, damage / 4.0F);

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        }) {
            ItemStack stack = getItemBySlot(slot);

            if (stack.isEmpty()) {
                continue;
            }

            if (!(stack.getItem() instanceof ArmorItem)) {
                continue;
            }

            stack.hurtAndBreak(
                    damageAmount,
                    this,
                    slot
            );
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isDeadOrDying()) {
            return;
        }

        if (this.level().isClientSide) {
            return;
        }

        if (miningTarget != null) {
            double distance = this.distanceToSqr(
                    miningTarget.getX() + 0.5,
                    miningTarget.getY(),
                    miningTarget.getZ() + 0.5
            );

            if (distance <= 25.0) {
                BlockPos target = miningTarget;
                miningTarget = null;
                mine(target);
            }
        }

        if (placeTarget != null) {
            double distance = this.distanceToSqr(
                    placeTarget.getX() + 0.5,
                    placeTarget.getY(),
                    placeTarget.getZ() + 0.5
            );

            if (distance <= 25.0) {
                BlockPos target = placeTarget;
                int slot = placeSlot;

                placeTarget = null;
                placeSlot = -1;

                place(target, slot);
            }
        }

        if (foodCooldown > 0) {
            foodCooldown--;
        }
        if (healCooldown > 0) {
            healCooldown--;
        }

        if (hunger < (maxHunger - 2.5f) && foodCooldown <= 0) {
            int foodSlot = hasFood();
            if (foodSlot != -1) {
                eatFood(foodSlot);
            }
        }
        if (getHealth() < (getMaxHealth() - 2) && (hunger > 0 || saturation > 0) && healCooldown <= 0) {
            heal();
        }

        for (ItemEntity item : level().getEntitiesOfClass(
                ItemEntity.class,
                getBoundingBox().inflate(1.0)
        )) {
            if (item.isRemoved() || item.hasPickUpDelay()) {
                continue;
            }

            double dx = this.getX() - item.getX();
            double dy = (this.getY() + this.getBbHeight() * 0.5) - item.getY();
            double dz = this.getZ() - item.getZ();

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance < 1.5) {
                item.setDeltaMovement(
                        dx * 0.15,
                        dy * 0.15,
                        dz * 0.15
                );
            }

            if (this.getBoundingBox().intersects(item.getBoundingBox())) {
                pickUpItem(item);
            }
        }
    }

    public void pickUpItem(ItemEntity item) {
        ItemStack stack = item.getItem();

        ItemStack remaining = inventory.addItem(stack.copy());

        int pickedUp = stack.getCount() - remaining.getCount();

        if (pickedUp <= 0) {
            return;
        }

        if (remaining.isEmpty()) {
            item.discard();
        } else {
            item.setItem(remaining);
        }

        this.take(item, pickedUp);
    }

    public boolean navigateTo(BlockPos pos) {
        return this.getNavigation().moveTo(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                1.0
        );
    }

    public boolean dropItem(int slot) {
        ItemStack stack = inventory.removeItem(
                slot,
                inventory.getItem(slot).getCount()
        );

        if (stack.isEmpty()) {
            return false;
        }

        Vec3 look = this.getViewVector(1.0F);

        ItemEntity item = new ItemEntity(
                level(),
                getX() + look.x * 0.5,
                getY() + 1.0,
                getZ() + look.z * 0.5,
                stack
        );

        item.setDefaultPickUpDelay();

        item.setDeltaMovement(
                look.x * 0.3,
                look.y * 0.3 + 0.1,
                look.z * 0.3
        );

        level().addFreshEntity(item);

        return true;
    }

    public void dropAllItems() {
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            dropItem(i);
        }
    }

    private BlockPos miningTarget;

    public boolean startMining(BlockPos pos) {
        if (!this.level().isLoaded(pos) || this.level().getBlockState(pos).isAir()) {
            return false;
        }

        double distance = this.distanceToSqr(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5
        );

        if (distance <= 25.0) {
            return mine(pos);
        }

        miningTarget = pos.immutable();
        return navigateTo(pos);
    }

    public boolean mine(BlockPos pos) {
        if (!this.level().isLoaded(pos)) {
            return false;
        }

        var state = this.level().getBlockState(pos);

        if (state.isAir()) {
            return false;
        }

        // Werkzeug aus Haralds Inventar suchen
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack tool = inventory.getItem(i);

            if (tool.isEmpty()) {
                continue;
            }

            if (tool.isCorrectToolForDrops(state)) {
                this.level().destroyBlock(pos, true);
                tool.hurtAndBreak(
                        1,
                        this,
                        net.minecraft.world.entity.EquipmentSlot.MAINHAND
                );
                return true;
            }
        }

        // Kein Werkzeug nötig
        if (state.requiresCorrectToolForDrops()) {
            return false;
        }

        this.level().destroyBlock(pos, true);
        return true;
    }

    private BlockPos placeTarget;
    private int placeSlot = -1;

    public boolean startPlace(BlockPos pos, int slot) {
        if (!this.level().isLoaded(pos)) {
            return false;
        }

        ItemStack stack = this.inventory.getItem(slot);

        if (stack.isEmpty()) {
            return false;
        }

        double distance = this.distanceToSqr(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5
        );

        // Nah genug → direkt platzieren
        if (distance <= 25.0) {
            return place(pos, slot);
        }

        // Zu weit weg → hinlaufen und danach platzieren
        this.placeTarget = pos.immutable();
        this.placeSlot = slot;

        return navigateTo(pos);
    }

    public boolean place(BlockPos pos, int slot) {
        ItemStack stack = this.inventory.getItem(slot);

        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        if (!this.level().getBlockState(pos).canBeReplaced()) {
            return false;
        }

        BlockState blockState = blockItem.getBlock().defaultBlockState();

        if (!blockState.canSurvive(this.level(), pos)) {
            return false;
        }

        this.level().setBlock(
                pos,
                blockState,
                3
        );

        this.inventory.removeItem(slot, 1);

        return true;
    }

    public Map<String, Float> getStatus() {
        Map<String, Float> status = new HashMap<>();

        status.put("health", getHealth());
        status.put("maxHealth", getMaxHealth());
        status.put("hunger", hunger);
        status.put("maxHunger", maxHunger);
        status.put("saturation", saturation);
        status.put("maxSaturation", maxSaturation);

        return status;
    }

    public int hasFood() {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);

            if (stack.getFoodProperties(null) != null) {
                return i;
            }
        }

        return -1;
    }

    public boolean eatFood(int slot) {
        ItemStack stack = inventory.getItem(slot);

        if (stack.isEmpty()) {
            return false;
        }

        FoodProperties food = stack.getFoodProperties(this);

        if (food == null) {
            return false;
        }

        int nutrition = food.nutrition();

        float saturationGain =
                nutrition * food.saturation();

        hunger = Math.min(
                hunger + nutrition,
                maxHunger
        );

        saturation = Math.min(
                saturation + saturationGain,
                maxSaturation
        );

        inventory.removeItem(slot, 1);

        foodCooldown = 60;

        return true;
    }

    public boolean heal() {
        if (saturation > 0) {
            saturation--;
        }
        else if (hunger > 0) {
            hunger--;
        }
        else {
            return false;
        }
        setHealth(getHealth() + 1);
        healCooldown = 20;
        return true;
    }

    public boolean equipArmor(int slot) {
        if (slot < 0 || slot >= inventory.getContainerSize()) {
            return false;
        }

        ItemStack stack = inventory.getItem(slot);

        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem)) {
            return false;
        }

        EquipmentSlot equipmentSlot = armorItem.getEquipmentSlot();

        if (!getItemBySlot(equipmentSlot).isEmpty()) {
            return false;
        }

        ItemStack equipped = inventory.removeItem(slot, 1);

        setItemSlot(equipmentSlot, equipped);

        return true;
    }

    public boolean unequipArmor(int slot) {
        if (slot < 0 || slot > 3) {
            return false;
        }

        EquipmentSlot equipmentSlot = switch (slot) {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.CHEST;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.FEET;
            default -> throw new IllegalStateException();
        };

        ItemStack armorStack = getItemBySlot(equipmentSlot);

        if (armorStack.isEmpty()) {
            return false;
        }

        ItemStack remaining = inventory.addItem(armorStack.copy());

        if (!remaining.isEmpty()) {
            return false;
        }

        setItemSlot(equipmentSlot, ItemStack.EMPTY);

        return true;
    }
}
