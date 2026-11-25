package com.weraisedsirenhead.entity;

import com.weraisedsirenhead.config.SirenHeadConfig;
import com.weraisedsirenhead.entity.ai.SirenBrain;
import com.weraisedsirenhead.entity.ai.goal.AttackStateGoal;
import com.weraisedsirenhead.entity.ai.goal.ChaseStateGoal;
import com.weraisedsirenhead.entity.ai.goal.FollowStateGoal;
import com.weraisedsirenhead.entity.ai.goal.HideStateGoal;
import com.weraisedsirenhead.entity.ai.goal.IdleStateGoal;
import com.weraisedsirenhead.entity.ai.goal.ObserveStateGoal;
import com.weraisedsirenhead.entity.ai.goal.SearchStateGoal;
import com.weraisedsirenhead.entity.ai.goal.SleepStateGoal;
import com.weraisedsirenhead.entity.ai.goal.StalkStateGoal;
import com.weraisedsirenhead.entity.ai.goal.WanderStateGoal;
import com.weraisedsirenhead.registry.ModSounds;
import com.weraisedsirenhead.sound.SirenSounds;
import com.weraisedsirenhead.story.GrowthStage;
import com.weraisedsirenhead.story.PlayerStory;
import com.weraisedsirenhead.story.StoryData;
import com.weraisedsirenhead.story.StoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Siren Head. Una sola entidad que crece en 5 etapas. Esta clase solo guarda el estado
 * (etapa, estado de IA, hambre...) y la animacion; el comportamiento esta en entity.ai y la
 * historia en story/director.
 */
public class SirenHeadEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(SirenHeadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(SirenHeadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRANSFORM =
            SynchedEntityData.defineId(SirenHeadEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_FEAST =
            SynchedEntityData.defineId(SirenHeadEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_STAY =
            SynchedEntityData.defineId(SirenHeadEntity.class, EntityDataSerializers.BOOLEAN);


    private UUID ownerId;
    private int hunger = 70;
    private int overrideTicks;
    private int attackCooldown;
    private int chaseCooldown;
    private int chaseDelay;
    private int lastChirp;
    private int lastStep;
    private int wanderRadius = 8;
    private Vec3 anchor;
    private Vec3 searchPos;
    private int stateStartTick;
    private float gaitSpeed;
    private float gaitSpeedO;
    private float gaitPhase;
    private float gaitPhaseO;
    private float renderScale;
    private float renderScaleO;

    public SirenHeadEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        applyStageAttributes();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 96.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STATE, SirenState.IDLE.ordinal());
        this.entityData.define(DATA_STAGE, 1);
        this.entityData.define(DATA_TRANSFORM, 0);
        this.entityData.define(DATA_STAY, false);
        this.entityData.define(DATA_FEAST, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new AttackStateGoal(this));
        this.goalSelector.addGoal(3, new ChaseStateGoal(this));
        this.goalSelector.addGoal(4, new SleepStateGoal(this));
        this.goalSelector.addGoal(4, new FollowStateGoal(this));
        this.goalSelector.addGoal(4, new WanderStateGoal(this));
        this.goalSelector.addGoal(4, new ObserveStateGoal(this));
        this.goalSelector.addGoal(4, new HideStateGoal(this));
        this.goalSelector.addGoal(4, new StalkStateGoal(this));
        this.goalSelector.addGoal(4, new SearchStateGoal(this));
        this.goalSelector.addGoal(5, new IdleStateGoal(this));
    }

    // ------------------------------------------------------------------ tamano y etapa

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return GrowthStage.byId(this.entityData.get(DATA_STAGE)).dimensions;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_STAGE.equals(key)) {
            this.refreshDimensions();
        } else if (DATA_STATE.equals(key)) {
            this.stateStartTick = this.tickCount;
        }
        super.onSyncedDataUpdated(key);
    }

    /** Caja usada para decidir si se dibuja: cubre todo el modelo visual (la caja de colision es mas chica). */
    public AABB getBoundingBoxForCulling() {
        double h = getGrowthStage().visualHeight + 0.5D;
        double w = Math.max(getBbWidth(), h * 0.4D);
        return new AABB(getX() - w, getY(), getZ() - w, getX() + w, getY() + h, getZ() + w);
    }

    public GrowthStage getGrowthStage() {
        return GrowthStage.byId(this.entityData.get(DATA_STAGE));
    }

    public int getStage() {
        return this.entityData.get(DATA_STAGE);
    }

    public void setStage(int stage) {
        int clamped = Mth.clamp(stage, 1, 5);
        int old = this.entityData.get(DATA_STAGE);
        this.entityData.set(DATA_STAGE, clamped);
        applyStageAttributes();
        if (!this.level().isClientSide && clamped > old && this.level() instanceof ServerLevel serverLevel) {
            SirenSounds.at(serverLevel, this.position(), ModSounds.INTERFERENCE.get(), 2.5F, 0.8F);
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + 1.0D, getZ(), 24, 0.5D, 0.9D, 0.5D, 0.05D);
        }
    }

    private void applyStageAttributes() {
        GrowthStage stage = getGrowthStage();
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(stage.speed);
        }
        if (this.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(stage.id <= 3);
        }
    }

    /** Escala para que la malla (1.898 bloques de alto) mida el alto visual de la etapa actual. */
    public float getVisualScale() {
        return getGrowthStage().visualHeight / SirenBones.MODEL_HEIGHT;
    }

    // ------------------------------------------------------------------ estado de IA

    public SirenState getSirenState() {
        return SirenState.byId(this.entityData.get(DATA_STATE));
    }

    public void setSirenState(SirenState state) {
        if (getSirenState() != state) {
            this.entityData.set(DATA_STATE, state.ordinal());
            if (!this.level().isClientSide) {
                this.getNavigation().stop();
            }
        }
    }

    /** Fuerza un estado durante cierto tiempo; mientras dure, el cerebro no lo cambia. */
    public void forceState(SirenState state, int ticks) {
        setSirenState(state);
        this.overrideTicks = ticks;
    }

    public int getOverrideTicks() {
        return overrideTicks;
    }

    public void clearOverride() {
        this.overrideTicks = 0;
    }

    public boolean isTransforming() {
        return this.entityData.get(DATA_TRANSFORM) > 0;
    }

    public int getTransformTicks() {
        return this.entityData.get(DATA_TRANSFORM);
    }

    public void beginTransformation(int ticks) {
        this.entityData.set(DATA_TRANSFORM, ticks);
        this.entityData.set(DATA_STAY, false);
        forceState(SirenState.IDLE, ticks + 20);
    }

    /** true mientras esta devorando un cadaver (cambia la animacion a "feast"). */
    public boolean isFeasting() {
        return this.entityData.get(DATA_FEAST);
    }

    public void setFeasting(boolean feasting) {
        this.entityData.set(DATA_FEAST, feasting);
    }

    public boolean isStay() {
        return this.entityData.get(DATA_STAY);
    }

    public void setStay(boolean stay) {
        this.entityData.set(DATA_STAY, stay);
    }

    public int getHunger() {
        return hunger;
    }

    public int getAttackCooldown() {
        return attackCooldown;
    }

    public void setAttackCooldown(int ticks) {
        this.attackCooldown = ticks;
    }

    public int getChaseCooldown() {
        return chaseCooldown;
    }

    public void setChaseCooldown(int ticks) {
        this.chaseCooldown = ticks;
    }

    public int getChaseDelay() {
        return chaseDelay;
    }

    public void setChaseDelay(int ticks) {
        this.chaseDelay = ticks;
    }

    public Vec3 getAnchor() {
        return anchor;
    }

    public void setAnchor(Vec3 anchor) {
        this.anchor = anchor;
    }

    public int getWanderRadius() {
        return wanderRadius;
    }

    public void setWanderRadius(int radius) {
        this.wanderRadius = radius;
    }

    public Vec3 getSearchPos() {
        return searchPos;
    }

    public void setSearchPos(Vec3 pos) {
        this.searchPos = pos;
    }

    // ------------------------------------------------------------------ dueno

    public void setOwner(UUID owner) {
        this.ownerId = owner;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Player getOwnerPlayer() {
        return ownerId == null ? null : this.level().getPlayerByUUID(ownerId);
    }

    public boolean isOwner(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    // ------------------------------------------------------------------ parametros segun la etapa

    public double followDistance() {
        return getStage() <= 1 ? 3.5D : 5.0D;
    }

    public double stalkDistance() {
        return getStage() >= 5 ? 14.0D : 10.0D;
    }

    public double stalkSpeed() {
        return getStage() >= 5 ? 0.85D : 0.6D;
    }

    public double chaseSpeed() {
        return SirenHeadConfig.CHASE_SPEED.get();
    }

    public double attackReach() {
        return getGrowthStage().reach;
    }

    /** Gira el cuerpo y la cabeza hacia un punto, de a poco. */
    public void faceTowards(Vec3 target, float maxStep) {
        double dx = target.x - getX();
        double dz = target.z - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, maxStep));
        this.yBodyRot = this.getYRot();
        this.setYHeadRot(this.getYRot());
    }

    // ------------------------------------------------------------------ comida y trato

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isOwner(player) || getStage() > 2 || isTransforming()) {
            return super.mobInteract(player, hand);
        }
        if (stack.isEdible()) {
            if (!this.level().isClientSide) {
                feed(player, stack);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (stack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
            if (!this.level().isClientSide) {
                setStay(!isStay());
                player.displayClientMessage(Component.translatable(isStay()
                        ? "message.weraisedsirenhead.stay" : "message.weraisedsirenhead.follow"), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void feed(Player player, ItemStack stack) {
        ItemStack eaten = stack.copy();
        eaten.setCount(1);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.hunger = Math.min(100, this.hunger + 40);
        forceState(SirenState.EATING, 50);
        if (this.level() instanceof ServerLevel serverLevel) {
            double y = getY() + getGrowthStage().visualHeight * 0.6D;
            serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, eaten), getX(), y, getZ(), 10, 0.2D, 0.2D, 0.2D, 0.05D);
            serverLevel.sendParticles(getStage() == 1 ? ParticleTypes.HEART : ParticleTypes.SOUL,
                    getX(), y + 0.4D, getZ(), 4, 0.3D, 0.3D, 0.3D, 0.02D);
            SirenSounds.at(serverLevel, this.position(), ModSounds.BABY_CHIRP.get(), 1.3F, 1.5F);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            StoryManager.onFed(serverPlayer);
        }
    }

    /** Pequeno sonido de interferencia del bebe (con enfriamiento). */
    public void chirp() {
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount - lastChirp > 80) {
            lastChirp = this.tickCount;
            SirenSounds.at(serverLevel, this.position(), ModSounds.BABY_CHIRP.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.5F);
        }
    }

    // ------------------------------------------------------------------ ataque

    public void strike(Player target) {
        GrowthStage stage = getGrowthStage();
        float damage = stage.id >= 5
                ? SirenHeadConfig.ATTACK_DAMAGE_DAY5.get().floatValue()
                : SirenHeadConfig.ATTACK_DAMAGE_DAY4.get().floatValue();
        if (stage.id < 5) {
            damage = Math.min(damage, target.getHealth() - 2.0F);
        }
        if (damage > 0.0F) {
            target.hurt(this.level().damageSources().mobAttack(this), damage);
        }
        target.knockback(1.1D, getX() - target.getX(), getZ() - target.getZ());
        target.hurtMarked = true;
        if (this.level() instanceof ServerLevel serverLevel) {
            SirenSounds.at(serverLevel, this.position(), ModSounds.STEP.get(), 4.0F, 0.55F);
        }
    }

    // ------------------------------------------------------------------ invulnerabilidad y persistencia

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // Sin sonido de pasos de vanilla: los pasos pesados se reproducen en updateFootsteps().
    }

    // ------------------------------------------------------------------ tick del servidor

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.overrideTicks > 0 && --this.overrideTicks == 0) {
            onOverrideExpired();
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.chaseCooldown > 0) {
            this.chaseCooldown--;
        }
        if (this.chaseDelay > 0) {
            this.chaseDelay--;
        }
        int transform = this.entityData.get(DATA_TRANSFORM);
        if (transform > 0) {
            this.entityData.set(DATA_TRANSFORM, transform - 1);
        }
        if (this.tickCount % 100 == 0 && isOrphan(serverLevel)) {
            this.discard();
            return;
        }
        if (this.tickCount % 400 == 0 && this.hunger > 0) {
            this.hunger--;
        }
        if (this.tickCount % 20 == 0) {
            SirenBrain.think(this, serverLevel);
            ambientSounds(serverLevel);
        }
        updateFootsteps(serverLevel);
    }

    private void onOverrideExpired() {
        if (getSirenState() == SirenState.CHASING) {
            this.chaseCooldown = 500 + this.random.nextInt(300);
            Player owner = getOwnerPlayer();
            if (owner != null) {
                this.searchPos = owner.position();
            }
            forceState(SirenState.SEARCHING, 160);
        }
    }

    /** Una criatura que ya no es la de la historia de su dueno (por ejemplo tras reaparecer otra) se elimina sola. */
    private boolean isOrphan(ServerLevel level) {
        if (ownerId == null) {
            return true;
        }
        PlayerStory story = StoryData.get(level.getServer()).get(ownerId);
        return story == null || !story.active || story.completed
                || (story.creature != null && !story.creature.equals(this.getUUID()));
    }

    private void ambientSounds(ServerLevel level) {
        GrowthStage stage = getGrowthStage();
        SirenState state = getSirenState();
        if (stage.id <= 2) {
            if (state != SirenState.SLEEPING && this.random.nextInt(14) == 0) {
                chirp();
            } else if (stage.id == 2 && this.random.nextInt(40) == 0) {
                SirenSounds.at(level, this.position(), ModSounds.INTERFERENCE.get(), 1.2F, 1.3F);
            }
        } else if (stage.id >= 4 && (state == SirenState.OBSERVING_PLAYER || state == SirenState.STALKING
                || state == SirenState.HIDING) && this.random.nextInt(14) == 0) {
            SirenSounds.at(level, this.position(), ModSounds.BREATH.get(), 1.5F, 0.8F);
        }
    }

    private void updateFootsteps(ServerLevel level) {
        int stage = getStage();
        if (stage < 3 || this.getDeltaMovement().horizontalDistanceSqr() < 4.0E-4D) {
            return;
        }
        int interval = stage == 3 ? 16 : stage == 4 ? 14 : 12;
        if (this.tickCount - lastStep < interval || !this.onGround()) {
            return;
        }
        lastStep = this.tickCount;
        float volume = stage == 3 ? 1.5F : stage == 4 ? 2.5F : 4.0F;
        float pitch = (stage == 3 ? 1.1F : stage == 4 ? 0.9F : 0.75F) + this.random.nextFloat() * 0.1F;
        SirenSounds.at(level, this.position(), ModSounds.STEP.get(), volume, pitch);
    }

    // ------------------------------------------------------------------ guardado

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SirenStage", getStage());
        tag.putInt("SirenHunger", hunger);
        tag.putBoolean("SirenStay", isStay());
        tag.putInt("SirenTransform", getTransformTicks());
        if (ownerId != null) {
            tag.putUUID("SirenOwner", ownerId);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SirenStage")) {
            this.entityData.set(DATA_STAGE, Mth.clamp(tag.getInt("SirenStage"), 1, 5));
        }
        this.hunger = tag.contains("SirenHunger") ? tag.getInt("SirenHunger") : 70;
        this.entityData.set(DATA_STAY, tag.getBoolean("SirenStay"));
        this.entityData.set(DATA_TRANSFORM, tag.getInt("SirenTransform"));
        if (tag.hasUUID("SirenOwner")) {
            this.ownerId = tag.getUUID("SirenOwner");
        }
        applyStageAttributes();
    }

    // ------------------------------------------------------------------ animacion del cliente (la dibuja SirenHeadRenderer)

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            updateClientAnimation();
        }
    }

    /** Calcula en el cliente la velocidad y fase de la marcha y el tamano suavizado (crece de a poco al cambiar de dia). */
    private void updateClientAnimation() {
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        float speed = (float) Math.sqrt(dx * dx + dz * dz);
        this.gaitSpeedO = this.gaitSpeed;
        this.gaitSpeed += (speed - this.gaitSpeed) * 0.4F;
        float stride = Math.max(0.5F, getGrowthStage().visualHeight * 0.3F);
        this.gaitPhaseO = this.gaitPhase;
        this.gaitPhase += speed * (float) Math.PI / stride;
        float target = getVisualScale();
        this.renderScaleO = this.renderScale;
        if (this.renderScale <= 0.0F) {
            this.renderScale = target;
            this.renderScaleO = target;
        } else {
            this.renderScale += (target - this.renderScale) * 0.05F;
        }
    }

    public float getGaitSpeed(float partialTick) {
        return Mth.lerp(partialTick, this.gaitSpeedO, this.gaitSpeed);
    }

    public float getGaitPhase(float partialTick) {
        return Mth.lerp(partialTick, this.gaitPhaseO, this.gaitPhase);
    }

    public float getRenderScale(float partialTick) {
        return Mth.lerp(partialTick, this.renderScaleO, this.renderScale);
    }

    /** Ticks transcurridos desde el ultimo cambio de estado (para animaciones como el golpe). */
    public float getStateAge(float partialTick) {
        return this.tickCount + partialTick - this.stateStartTick;
    }
}
