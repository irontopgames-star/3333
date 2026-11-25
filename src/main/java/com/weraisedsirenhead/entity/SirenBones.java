package com.weraisedsirenhead.entity;

/** Indices de los 15 huesos del esqueleto (mismo orden que tools/build_siren_mesh.py) y alto del modelo en bloques. */
public final class SirenBones {
    public static final int ROOT = 0;
    public static final int HIPS = 1;
    public static final int SPINE = 2;
    public static final int NECK = 3;
    public static final int HEAD = 4;
    public static final int SPEAKER_A = 5;
    public static final int SPEAKER_B = 6;
    public static final int ARM_L_UP = 7;
    public static final int ARM_L_LOW = 8;
    public static final int ARM_R_UP = 9;
    public static final int ARM_R_LOW = 10;
    public static final int LEG_L_UP = 11;
    public static final int LEG_L_LOW = 12;
    public static final int LEG_R_UP = 13;
    public static final int LEG_R_LOW = 14;
    public static final int COUNT = 15;

    /** Alto de la malla original, en bloques. El tamano de cada etapa se obtiene escalandola. */
    public static final float MODEL_HEIGHT = 1.898F;

    private SirenBones() {
    }
}
