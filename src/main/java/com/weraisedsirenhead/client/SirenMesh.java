package com.weraisedsirenhead.client;

import com.weraisedsirenhead.SirenHeadMod;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * La malla de Siren Head (unas 10 mil caras) con su esqueleto y pesos de piel, cargada de
 * assets/weraisedsirenhead/models/siren_head.bin (lo genera tools/build_siren_mesh.py a partir del .bbmodel).
 */
public final class SirenMesh {
    private static final String PATH = "/assets/" + SirenHeadMod.MOD_ID + "/models/siren_head.bin";
    private static SirenMesh instance;
    private static boolean failed;

    public final int vertexCount;
    public final int faceCount;
    public final int boneCount;
    public final float[] x;
    public final float[] y;
    public final float[] z;
    public final byte[] bone0;
    public final byte[] bone1;
    public final float[] weight0;
    /** 4 indices de vertice por cara (los triangulos repiten el ultimo). */
    public final int[] faces;
    /** 8 floats por cara: u,v de cada una de las 4 esquinas. */
    public final float[] uv;
    public final int[] parent;
    public final float[] pivotX;
    public final float[] pivotY;
    public final float[] pivotZ;

    private SirenMesh(DataInputStream in) throws IOException {
        if (in.readInt() != 0x53495245) {
            throw new IOException("Archivo de malla invalido");
        }
        vertexCount = in.readInt();
        faceCount = in.readInt();
        boneCount = in.readInt();
        parent = new int[boneCount];
        pivotX = new float[boneCount];
        pivotY = new float[boneCount];
        pivotZ = new float[boneCount];
        for (int b = 0; b < boneCount; b++) {
            int nameLength = in.readUnsignedShort();
            in.skipBytes(nameLength);
            parent[b] = in.readInt();
            pivotX[b] = in.readFloat();
            pivotY[b] = in.readFloat();
            pivotZ[b] = in.readFloat();
        }
        x = new float[vertexCount];
        y = new float[vertexCount];
        z = new float[vertexCount];
        bone0 = new byte[vertexCount];
        bone1 = new byte[vertexCount];
        weight0 = new float[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            x[i] = in.readFloat();
            y[i] = in.readFloat();
            z[i] = in.readFloat();
            bone0[i] = in.readByte();
            bone1[i] = in.readByte();
            weight0[i] = in.readFloat();
        }
        faces = new int[faceCount * 4];
        uv = new float[faceCount * 8];
        for (int f = 0; f < faceCount; f++) {
            for (int k = 0; k < 4; k++) {
                faces[f * 4 + k] = in.readUnsignedShort();
            }
            for (int k = 0; k < 8; k++) {
                uv[f * 8 + k] = in.readFloat();
            }
        }
    }

    /** Devuelve la malla (se carga la primera vez) o null si no se pudo leer. */
    public static SirenMesh get() {
        if (instance == null && !failed) {
            try (InputStream raw = SirenMesh.class.getResourceAsStream(PATH)) {
                if (raw == null) {
                    throw new IOException("No se encontro " + PATH);
                }
                instance = new SirenMesh(new DataInputStream(new BufferedInputStream(raw)));
                SirenHeadMod.LOGGER.info("[{}] Malla de Siren Head cargada: {} vertices, {} caras",
                        SirenHeadMod.MOD_ID, instance.vertexCount, instance.faceCount);
            } catch (IOException | RuntimeException e) {
                failed = true;
                SirenHeadMod.LOGGER.error("[{}] No se pudo cargar la malla de Siren Head", SirenHeadMod.MOD_ID, e);
            }
        }
        return instance;
    }
}
