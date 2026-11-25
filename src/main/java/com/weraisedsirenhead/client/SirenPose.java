package com.weraisedsirenhead.client;

import com.weraisedsirenhead.entity.SirenBones;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import com.weraisedsirenhead.entity.SirenState;
import net.minecraft.util.Mth;

import java.util.Arrays;

import static com.weraisedsirenhead.entity.SirenBones.*;

/**
 * Animaciones de Siren Head: calcula la rotacion (grados X, Y, Z) de cada hueso segun su estado.
 * Convenciones: el frente del modelo es -Z. Para un hueso colgando (brazo, pierna) rotar +X lo lleva hacia ADELANTE;
 * para un hueso que apunta hacia arriba (columna, cuello) rotar -X lo inclina hacia adelante.
 * Cada animacion es una funcion corta: se pueden retocar numeros sin tocar nada mas.
 */
public final class SirenPose {
    private static final float PI = (float) Math.PI;

    /**
     * @param rot    salida: COUNT * 3 floats (rx, ry, rz) en grados
     * @param offset salida: desplazamiento del modelo (x, y, z) en bloques de modelo
     */
    public static void compute(SirenHeadEntity e, float partial, float[] rot, float[] offset) {
        Arrays.fill(rot, 0.0F);
        Arrays.fill(offset, 0.0F);
        float t = e.tickCount + partial;
        float walk = Mth.clamp(e.getGaitSpeed(partial) / 0.06F, 0.0F, 1.0F);
        float phase = e.getGaitPhase(partial);
        SirenState state = e.getSirenState();

        idle(rot, t);
        if (e.isTransforming()) {
            transform(rot, t);
            return;
        }
        if (e.isFeasting()) {
            feast(rot, t);
            look(e, partial, rot, 0.3F);
            return;
        }
        switch (state) {
            case SLEEPING:
                if (walk < 0.2F) {
                    sleep(rot, offset, t);
                    return;
                }
                gait(rot, offset, phase, walk, false);
                break;
            case EATING:
                eat(rot, t);
                break;
            case ATTACKING:
                attack(rot, e.getStateAge(partial) / 26.0F);
                break;
            case CHASING:
                gait(rot, offset, phase, walk, true);
                if (walk < 0.2F) {
                    observe(rot, t, 1.0F);
                }
                break;
            case OBSERVING_PLAYER:
            case HIDING:
            case STALKING:
                gait(rot, offset, phase, walk, false);
                observe(rot, t, 1.0F - walk);
                break;
            default:
                gait(rot, offset, phase, walk, false);
                break;
        }
        look(e, partial, rot, 1.0F);
    }

    private static void add(float[] r, int bone, float rx, float ry, float rz) {
        r[bone * 3] += rx;
        r[bone * 3 + 1] += ry;
        r[bone * 3 + 2] += rz;
    }

    private static float smooth(float v) {
        float c = Mth.clamp(v, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    /** Respiracion lenta y pequenos tics: siempre activa de fondo. */
    private static void idle(float[] r, float t) {
        add(r, SPINE, 0.0F, 0.0F, (float) Math.sin(t * 0.05F) * 1.2F);
        add(r, NECK, (float) Math.sin(t * 0.04F) * 2.0F, (float) Math.sin(t * 0.03F) * 3.0F, 0.0F);
        add(r, ARM_L_UP, (float) Math.sin(t * 0.045F) * 2.0F, 0.0F, -1.0F);
        add(r, ARM_R_UP, -(float) Math.sin(t * 0.045F) * 2.0F, 0.0F, 1.0F);
    }

    /** Marcha. w = cuanto esta caminando (0..1); run = persecucion (zancadas largas e inclinado hacia adelante). */
    private static void gait(float[] r, float[] offset, float phase, float w, boolean run) {
        if (w <= 0.001F) {
            return;
        }
        float leg = (run ? 42.0F : 24.0F) * w;
        float knee = (run ? 60.0F : 34.0F) * w;
        float arm = (run ? 48.0F : 16.0F) * w;
        float elbow = (run ? 55.0F : 10.0F) * w;
        float lean = (run ? -14.0F : -3.0F) * w;
        float sl = (float) Math.sin(phase);
        float sr = (float) Math.sin(phase + PI);
        add(r, LEG_L_UP, sl * leg, 0.0F, 0.0F);
        add(r, LEG_R_UP, sr * leg, 0.0F, 0.0F);
        add(r, LEG_L_LOW, -knee * Math.max(0.0F, (float) Math.cos(phase)), 0.0F, 0.0F);
        add(r, LEG_R_LOW, -knee * Math.max(0.0F, (float) Math.cos(phase + PI)), 0.0F, 0.0F);
        add(r, ARM_L_UP, sr * arm, 0.0F, 0.0F);
        add(r, ARM_R_UP, sl * arm, 0.0F, 0.0F);
        add(r, ARM_L_LOW, elbow * Math.max(0.0F, sr), 0.0F, 0.0F);
        add(r, ARM_R_LOW, elbow * Math.max(0.0F, sl), 0.0F, 0.0F);
        add(r, SPINE, lean, sl * 4.0F * w, (float) Math.cos(phase) * 2.5F * w);
        add(r, HIPS, 0.0F, -sl * 4.0F * w, 0.0F);
        offset[1] += Math.abs((float) Math.cos(phase)) * 0.025F * w;
    }

    /** Quieto, mirando: cuello torcido y los parlantes con tics ocasionales. */
    private static void observe(float[] r, float t, float w) {
        add(r, NECK, -4.0F * w, 0.0F, 0.0F);
        add(r, HEAD, 0.0F, 0.0F, (float) Math.sin(t * 0.02F) * 3.0F * w);
        add(r, SPEAKER_A, 0.0F, twitch(t, 0.0F) * w, 0.0F);
        add(r, SPEAKER_B, 0.0F, -twitch(t, 1.7F) * w, 0.0F);
    }

    private static float twitch(float t, float seed) {
        float s = (float) Math.sin(t * 0.9F + seed) * (float) Math.sin(t * 0.13F + seed * 2.0F);
        return s > 0.55F ? (s - 0.55F) * 60.0F : 0.0F;
    }

    /** Dormido boca arriba en el suelo. */
    private static void sleep(float[] r, float[] offset, float t) {
        add(r, ROOT, 90.0F, 0.0F, 0.0F);
        offset[1] += 0.17F;
        add(r, SPINE, 0.0F, 0.0F, (float) Math.sin(t * 0.06F) * 1.5F);
        add(r, HEAD, (float) Math.sin(t * 0.05F) * 4.0F, 0.0F, 0.0F);
    }

    /** Comiendo la comida que le das (bebe): baja la cabeza y se lleva las manos. */
    private static void eat(float[] r, float t) {
        add(r, NECK, -(25.0F + (float) Math.sin(t * 0.5F) * 10.0F), 0.0F, 0.0F);
        add(r, ARM_L_UP, 45.0F, 0.0F, 0.0F);
        add(r, ARM_R_UP, 45.0F, 0.0F, 0.0F);
        add(r, ARM_L_LOW, 40.0F, 0.0F, 0.0F);
        add(r, ARM_R_LOW, 40.0F, 0.0F, 0.0F);
    }

    /** Devorando un cadaver: inclinado sobre la presa, brazos tirando, parlantes temblando. */
    private static void feast(float[] r, float t) {
        float chew = (float) Math.sin(t * 0.55F);
        add(r, SPINE, -30.0F + chew * 4.0F, 0.0F, 0.0F);
        add(r, NECK, -50.0F - chew * 12.0F, 0.0F, 0.0F);
        add(r, HEAD, -20.0F, 0.0F, 0.0F);
        add(r, ARM_L_UP, 55.0F + (float) Math.sin(t * 0.5F) * 25.0F, 0.0F, 0.0F);
        add(r, ARM_R_UP, 55.0F - (float) Math.sin(t * 0.5F) * 25.0F, 0.0F, 0.0F);
        add(r, ARM_L_LOW, 40.0F, 0.0F, 0.0F);
        add(r, ARM_R_LOW, 40.0F, 0.0F, 0.0F);
        add(r, SPEAKER_A, 0.0F, (float) Math.sin(t * 2.3F) * 9.0F, (float) Math.sin(t * 1.9F) * 5.0F);
        add(r, SPEAKER_B, 0.0F, (float) Math.sin(t * 2.7F + 1.0F) * 9.0F, (float) Math.sin(t * 2.1F) * 5.0F);
        add(r, LEG_L_UP, -8.0F, 0.0F, 0.0F);
        add(r, LEG_R_UP, -8.0F, 0.0F, 0.0F);
    }

    /** Golpe: p va de 0 a 1 (26 ticks): levanta los brazos, los baja de golpe y se recupera. */
    private static void attack(float[] r, float p) {
        float arms;
        float spine;
        if (p < 0.46F) {
            float k = smooth(p / 0.46F);
            arms = 150.0F * k;
            spine = 14.0F * k;
        } else if (p < 0.62F) {
            float k = smooth((p - 0.46F) / 0.16F);
            arms = 150.0F + (15.0F - 150.0F) * k;
            spine = 14.0F + (-28.0F - 14.0F) * k;
        } else {
            float k = smooth((p - 0.62F) / 0.38F);
            arms = 15.0F * (1.0F - k);
            spine = -28.0F * (1.0F - k);
        }
        add(r, ARM_L_UP, arms, 0.0F, -6.0F);
        add(r, ARM_R_UP, arms, 0.0F, 6.0F);
        add(r, ARM_L_LOW, arms * 0.25F, 0.0F, 0.0F);
        add(r, ARM_R_LOW, arms * 0.25F, 0.0F, 0.0F);
        add(r, SPINE, spine, 0.0F, 0.0F);
        add(r, NECK, spine * 0.6F, 0.0F, 0.0F);
    }

    /** Transformacion: convulsiones, brazos abiertos, parlantes enloquecidos. */
    private static void transform(float[] r, float t) {
        float k = 1.0F + 0.5F * (float) Math.sin(t * 0.05F);
        add(r, SPINE, (float) Math.sin(t * 1.7F) * 9.0F * k, (float) Math.sin(t * 2.3F) * 6.0F, (float) Math.sin(t * 2.9F) * 8.0F);
        add(r, NECK, (float) Math.sin(t * 3.1F) * 14.0F, (float) Math.sin(t * 2.1F) * 16.0F, (float) Math.sin(t * 2.7F) * 10.0F);
        add(r, HEAD, (float) Math.sin(t * 4.3F) * 10.0F, (float) Math.sin(t * 3.7F) * 12.0F, 0.0F);
        add(r, SPEAKER_A, 0.0F, (float) Math.sin(t * 5.1F) * 28.0F, (float) Math.sin(t * 4.1F) * 14.0F);
        add(r, SPEAKER_B, 0.0F, (float) Math.sin(t * 4.7F + 1.0F) * 28.0F, (float) Math.sin(t * 3.9F) * 14.0F);
        add(r, ARM_L_UP, (float) Math.sin(t * 2.2F) * 20.0F, 0.0F, -35.0F + (float) Math.sin(t * 3.3F) * 10.0F);
        add(r, ARM_R_UP, (float) Math.sin(t * 2.5F) * 20.0F, 0.0F, 35.0F + (float) Math.sin(t * 3.0F) * 10.0F);
        add(r, LEG_L_UP, (float) Math.sin(t * 3.4F) * 5.0F, 0.0F, 0.0F);
        add(r, LEG_R_UP, (float) Math.sin(t * 3.2F) * 5.0F, 0.0F, 0.0F);
    }

    /** La cabeza sigue hacia donde mira la entidad (60% la cabeza, 40% el cuello). */
    private static void look(SirenHeadEntity e, float partial, float[] r, float amount) {
        float bodyYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        float headYaw = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float net = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -70.0F, 70.0F) * amount;
        float pitch = Mth.clamp(Mth.lerp(partial, e.xRotO, e.getXRot()), -35.0F, 35.0F) * amount;
        add(r, NECK, -pitch * 0.4F, -net * 0.4F, 0.0F);
        add(r, HEAD, -pitch * 0.6F, -net * 0.6F, 0.0F);
    }

    private SirenPose() {
    }
}
