package com.weraisedsirenhead.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.weraisedsirenhead.SirenHeadMod;
import com.weraisedsirenhead.entity.SirenBones;
import com.weraisedsirenhead.entity.SirenHeadEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Dibuja la malla de Siren Head con piel por huesos (dos huesos por vertice, mezcla lineal).
 * Tamano segun el dia: se escala la malla para que mida el alto de la etapa actual (1.5 a 12 bloques).
 */
public class SirenHeadRenderer extends EntityRenderer<SirenHeadEntity> {
    private static final ResourceLocation TEXTURE = SirenHeadMod.id("textures/entity/siren_head.png");
    private static final float DEG = (float) (Math.PI / 180.0D);

    private final Matrix4f[] global = new Matrix4f[SirenBones.COUNT];
    private final Matrix4f[] skin = new Matrix4f[SirenBones.COUNT];
    private final float[] rot = new float[SirenBones.COUNT * 3];
    private final float[] offset = new float[3];
    private final Vector3f tmp = new Vector3f();
    private float[] outX = new float[0];
    private float[] outY = new float[0];
    private float[] outZ = new float[0];

    public SirenHeadRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.6F;
        for (int i = 0; i < SirenBones.COUNT; i++) {
            global[i] = new Matrix4f();
            skin[i] = new Matrix4f();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SirenHeadEntity entity) {
        return TEXTURE;
    }

    /** Es una sola entidad enorme: no se recorta por el cuadro de la camara (solo por distancia). */
    @Override
    public boolean shouldRender(SirenHeadEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return entity.shouldRender(camX, camY, camZ);
    }

    @Override
    public void render(SirenHeadEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        SirenMesh mesh = SirenMesh.get();
        if (mesh == null) {
            return;
        }
        if (outX.length < mesh.vertexCount) {
            outX = new float[mesh.vertexCount];
            outY = new float[mesh.vertexCount];
            outZ = new float[mesh.vertexCount];
        }
        SirenPose.compute(entity, partialTick, rot, offset);
        buildSkinMatrices(mesh);
        skinVertices(mesh);

        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float scale = entity.getRenderScale(partialTick);
        if (entity.isTransforming()) {
            scale *= 1.0F + 0.04F * (float) Math.sin((entity.tickCount + partialTick) * 1.5F);
        }
        this.shadowRadius = Math.min(0.5F * scale + 0.3F, 6.0F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(scale, scale, scale);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        emitFaces(mesh, consumer, pose.pose(), pose.normal(), packedLight);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /** Matriz de piel de cada hueso = transformacion global del hueso x traslacion inversa de su pivote. */
    private void buildSkinMatrices(SirenMesh mesh) {
        for (int b = 0; b < mesh.boneCount; b++) {
            Matrix4f g = global[b];
            int parent = mesh.parent[b];
            float px = mesh.pivotX[b];
            float py = mesh.pivotY[b];
            float pz = mesh.pivotZ[b];
            if (parent < 0) {
                g.identity().translate(offset[0] + px, offset[1] + py, offset[2] + pz);
            } else {
                g.set(global[parent]).translate(px - mesh.pivotX[parent], py - mesh.pivotY[parent], pz - mesh.pivotZ[parent]);
            }
            g.rotateZ(rot[b * 3 + 2] * DEG).rotateY(rot[b * 3 + 1] * DEG).rotateX(rot[b * 3] * DEG);
            skin[b].set(g).translate(-px, -py, -pz);
        }
    }

    private void skinVertices(SirenMesh mesh) {
        for (int i = 0; i < mesh.vertexCount; i++) {
            float vx = mesh.x[i];
            float vy = mesh.y[i];
            float vz = mesh.z[i];
            float w = mesh.weight0[i];
            skin[mesh.bone0[i] & 0xFF].transformPosition(vx, vy, vz, tmp);
            float ox = tmp.x * w;
            float oy = tmp.y * w;
            float oz = tmp.z * w;
            if (w < 0.999F) {
                skin[mesh.bone1[i] & 0xFF].transformPosition(vx, vy, vz, tmp);
                float w1 = 1.0F - w;
                ox += tmp.x * w1;
                oy += tmp.y * w1;
                oz += tmp.z * w1;
            }
            outX[i] = ox;
            outY[i] = oy;
            outZ[i] = oz;
        }
    }

    private void emitFaces(SirenMesh mesh, VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light) {
        for (int f = 0; f < mesh.faceCount; f++) {
            int a = mesh.faces[f * 4];
            int b = mesh.faces[f * 4 + 1];
            int c = mesh.faces[f * 4 + 2];
            int d = mesh.faces[f * 4 + 3];
            float ax = outX[c] - outX[a];
            float ay = outY[c] - outY[a];
            float az = outZ[c] - outZ[a];
            float bx = outX[d] - outX[b];
            float by = outY[d] - outY[b];
            float bz = outZ[d] - outZ[b];
            float nx = ay * bz - az * by;
            float ny = az * bx - ax * bz;
            float nz = ax * by - ay * bx;
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 1.0E-8F) {
                nx /= length;
                ny /= length;
                nz /= length;
            } else {
                nx = 0.0F;
                ny = 1.0F;
                nz = 0.0F;
            }
            for (int k = 0; k < 4; k++) {
                int v = mesh.faces[f * 4 + k];
                consumer.vertex(pose, outX[v], outY[v], outZ[v])
                        .color(255, 255, 255, 255)
                        .uv(mesh.uv[f * 8 + k * 2], mesh.uv[f * 8 + k * 2 + 1])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(light)
                        .normal(normal, nx, ny, nz)
                        .endVertex();
            }
        }
    }
}
