package com.weraisedsirenhead.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configuracion COMMON (config/weraisedsirenhead-common.toml). */
public final class SirenHeadConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;
    public static final ForgeConfigSpec.IntValue FINAL_NIGHT_SPEED;
    public static final ForgeConfigSpec.BooleanValue ALLOW_BLOCK_DESTRUCTION;
    public static final ForgeConfigSpec.DoubleValue ATTACK_DAMAGE_DAY4;
    public static final ForgeConfigSpec.DoubleValue ATTACK_DAMAGE_DAY5;
    public static final ForgeConfigSpec.DoubleValue EVENT_FREQUENCY;
    public static final ForgeConfigSpec.DoubleValue CHASE_SPEED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Opciones generales de We Raised Siren Head").push("general");
        DEBUG_LOGGING = builder
                .comment("Escribe informacion extra en el log (cambios de dia, eventos de terror).")
                .define("debugLogging", false);
        builder.pop();

        builder.comment("Dificultad y ritmo").push("gameplay");
        FINAL_NIGHT_SPEED = builder
                .comment("Velocidad a la que pasa la noche del dia 5 (1 = normal, 3 = tres veces mas rapido).")
                .defineInRange("finalNightSpeed", 3, 1, 10);
        ALLOW_BLOCK_DESTRUCTION = builder
                .comment("Siren Head adulto puede romper puertas, vidrios, madera, lana y hojas. Respeta la gamerule mobGriefing.")
                .define("allowBlockDestruction", true);
        ATTACK_DAMAGE_DAY4 = builder
                .comment("Dano de sus golpes el dia 4 (nunca mata: deja al jugador con al menos 1 corazon).")
                .defineInRange("attackDamageDay4", 4.0D, 0.0D, 40.0D);
        ATTACK_DAMAGE_DAY5 = builder
                .comment("Dano de sus golpes el dia 5 (puede matar).")
                .defineInRange("attackDamageDay5", 8.0D, 0.0D, 40.0D);
        EVENT_FREQUENCY = builder
                .comment("Multiplicador de frecuencia de los eventos de terror (0.5 = mas espaciados, 2 = el doble de seguidos).")
                .defineInRange("eventFrequency", 1.0D, 0.25D, 4.0D);
        CHASE_SPEED = builder
                .comment("Multiplicador de velocidad de persecucion (1.0 = normal; bajalo si te resulta imposible escapar).")
                .defineInRange("chaseSpeed", 1.0D, 0.5D, 1.5D);
        builder.pop();

        SPEC = builder.build();
    }

    private SirenHeadConfig() {
    }
}
