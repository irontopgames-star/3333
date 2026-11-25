package com.weraisedsirenhead.story;

import net.minecraft.world.entity.EntityDimensions;

/**
 * Etapas fisicas de la criatura (una por dia). La caja de colision es pequena a proposito
 * (asi puede pasar entre arboles); el tamano visual lo da la escala del renderer.
 */
public enum GrowthStage {
    STAGE_1(1, 1.0F, 1.5F, 0.6F, 1.4F, 0.24D, 2.0D),
    STAGE_2(2, 1.6F, 2.4F, 0.8F, 1.9F, 0.25D, 2.5D),
    STAGE_3(3, 2.6F, 3.9F, 1.0F, 2.5F, 0.26D, 3.0D),
    STAGE_4(4, 4.0F, 6.0F, 1.2F, 2.7F, 0.27D, 3.4D),
    STAGE_5(5, 1.0F, 12.0F, 1.6F, 2.9F, 0.36D, 4.2D);

    public final int id;
    /** Escala aplicada al modelo (el modelo adulto ya mide 12 bloques, por eso escala 1). */
    public final float visualScale;
    /** Altura visual aproximada en bloques. */
    public final float visualHeight;
    public final EntityDimensions dimensions;
    /** Atributo de velocidad base. */
    public final double speed;
    /** Alcance de sus golpes, en bloques. */
    public final double reach;

    GrowthStage(int id, float visualScale, float visualHeight, float width, float height, double speed, double reach) {
        this.id = id;
        this.visualScale = visualScale;
        this.visualHeight = visualHeight;
        this.dimensions = EntityDimensions.scalable(width, height);
        this.speed = speed;
        this.reach = reach;
    }

    public static GrowthStage byId(int id) {
        int index = Math.max(1, Math.min(5, id)) - 1;
        return values()[index];
    }
}
