package io.ticticboom.mods.mm.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class FxMesh {
    private FxMesh() {
    }

    public static void quad(VertexConsumer vc, PoseStack.Pose pose,
                            double ax, double ay, double az, float au, float av,
                            double bx, double by, double bz, float bu, float bv,
                            double cx, double cy, double cz, float cu, float cv,
                            double dx, double dy, double dz, float du, float dv,
                            int color, int light, double nx, double ny, double nz) {
        facing(vc, pose, ax, ay, az, au, av, bx, by, bz, bu, bv, cx, cy, cz, cu, cv, dx, dy, dz, du, dv,
                color, light, nx, ny, nz, nx, ny, nz);
    }

    public static void facing(VertexConsumer vc, PoseStack.Pose pose,
                              double ax, double ay, double az, float au, float av,
                              double bx, double by, double bz, float bu, float bv,
                              double cx, double cy, double cz, float cu, float cv,
                              double dx, double dy, double dz, float du, float dv,
                              int color, int light, double ox, double oy, double oz, double nx, double ny, double nz) {
        double e1x = bx - ax, e1y = by - ay, e1z = bz - az, e2x = cx - ax, e2y = cy - ay, e2z = cz - az;
        double crx = e1y * e2z - e1z * e2y, cry = e1z * e2x - e1x * e2z, crz = e1x * e2y - e1y * e2x;
        if (crx == 0 && cry == 0 && crz == 0) {
            e1x = cx - ax;
            e1y = cy - ay;
            e1z = cz - az;
            e2x = dx - ax;
            e2y = dy - ay;
            e2z = dz - az;
            crx = e1y * e2z - e1z * e2y;
            cry = e1z * e2x - e1x * e2z;
            crz = e1x * e2y - e1y * e2x;
        }
        if (crx * ox + cry * oy + crz * oz < 0) {
            vertex(vc, pose, ax, ay, az, au, av, color, light, nx, ny, nz);
            vertex(vc, pose, dx, dy, dz, du, dv, color, light, nx, ny, nz);
            vertex(vc, pose, cx, cy, cz, cu, cv, color, light, nx, ny, nz);
            vertex(vc, pose, bx, by, bz, bu, bv, color, light, nx, ny, nz);
            return;
        }
        vertex(vc, pose, ax, ay, az, au, av, color, light, nx, ny, nz);
        vertex(vc, pose, bx, by, bz, bu, bv, color, light, nx, ny, nz);
        vertex(vc, pose, cx, cy, cz, cu, cv, color, light, nx, ny, nz);
        vertex(vc, pose, dx, dy, dz, du, dv, color, light, nx, ny, nz);
    }

    public static void vertex(VertexConsumer vc, PoseStack.Pose pose, double x, double y, double z, float u, float v,
                              int color, int light, double nx, double ny, double nz) {
        FxStats.vertices++;
        vc.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, (float) nx, (float) ny, (float) nz);
    }

    public static void lathe(VertexConsumer vc, PoseStack.Pose pose, double x, double y, double z, double[] radius, double[] dy, int count,
                             int sides, int color, int light, double uSeed, double vSeed) {
        float u0b = (float) frac(uSeed) * 0.75f;
        float v0b = (float) frac(vSeed * 0.37);
        double[] cos = FxTrig.cos(sides);
        double[] sin = FxTrig.sin(sides);
        for (int k = 0; k < count - 1; k++) {
            double r0 = radius[k];
            double y0 = y + dy[k];
            double r1 = radius[k + 1];
            double y1 = y + dy[k + 1];
            double slope = (r0 - r1) / Math.max(1e-4, y0 - y1);
            for (int s = 0; s < sides; s++) {
                double c0 = cos[s], s0 = sin[s], c1 = cos[s + 1], s1 = sin[s + 1];
                double nx = (c0 + c1) * 0.5;
                double nz = (s0 + s1) * 0.5;
                double ny = -slope * 0.5;
                double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
                float u0 = u0b + s * 0.25f / sides;
                float u1 = u0b + (s + 1) * 0.25f / sides;
                float v0 = v0b + k * 0.03f;
                float v1 = v0b + (k + 1) * 0.03f;
                quad(vc, pose, x + c0 * r0, y0, z + s0 * r0, u0, v0, x + c0 * r1, y1, z + s0 * r1, u0, v1,
                        x + c1 * r1, y1, z + s1 * r1, u1, v1, x + c1 * r0, y0, z + s1 * r0, u1, v0,
                        color, light, nx / nl, ny / nl, nz / nl);
            }
        }
    }

    public static void box(VertexConsumer vc, PoseStack.Pose pose, double cx, double cy, double cz, double hw, double hh, int color, int light) {
        double x0 = cx - hw, x1 = cx + hw, y0 = cy - hh, y1 = cy + hh, z0 = cz - hw, z1 = cz + hw;
        quad(vc, pose, x0, y0, z0, 0, 0, x1, y0, z0, 1, 0, x1, y0, z1, 1, 1, x0, y0, z1, 0, 1, color, light, 0, -1, 0);
        quad(vc, pose, x0, y0, z0, 0, 0, x0, y1, z0, 0, 1, x0, y1, z1, 1, 1, x0, y0, z1, 1, 0, color, light, -1, 0, 0);
        quad(vc, pose, x1, y0, z0, 0, 0, x1, y1, z0, 0, 1, x1, y1, z1, 1, 1, x1, y0, z1, 1, 0, color, light, 1, 0, 0);
        quad(vc, pose, x0, y0, z0, 0, 0, x0, y1, z0, 0, 1, x1, y1, z0, 1, 1, x1, y0, z0, 1, 0, color, light, 0, 0, -1);
        quad(vc, pose, x0, y0, z1, 0, 0, x0, y1, z1, 0, 1, x1, y1, z1, 1, 1, x1, y0, z1, 1, 0, color, light, 0, 0, 1);
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }
}
