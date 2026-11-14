package com.findaries.horror.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;

/**
 * Tiny immediate-mode helper: coloured quads in an object's local space, turned around Y,
 * moved to a camera-relative origin and (for things in the sky) shrunk towards the camera.
 * Shrinking everything by the same factor around the eye does not change the picture, it only
 * keeps kilometre-sized objects inside the far plane.
 */
final class Geo {
    private final BufferBuilder buf;
    private final Matrix4f mat;
    private double ox, oy, oz;
    private double fx = 0.0D, fz = 1.0D;
    private double k = 1.0D;
    private int alpha = 255;
    private double rollCos = 1.0D, rollSin = 0.0D;

    Geo(BufferBuilder buf, Matrix4f mat) {
        this.buf = buf;
        this.mat = mat;
    }

    /** @param forwardX,forwardZ unit vector the object's local +Z axis points along */
    void begin(double originX, double originY, double originZ, double forwardX, double forwardZ, double scale, float alpha) {
        this.ox = originX;
        this.oy = originY;
        this.oz = originZ;
        this.fx = forwardX;
        this.fz = forwardZ;
        this.k = scale;
        this.alpha = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
        this.rollCos = 1.0D;
        this.rollSin = 0.0D;
    }

    /** Banks the object around its forward axis (until the next begin). */
    void roll(double angle) {
        this.rollCos = Math.cos(angle);
        this.rollSin = Math.sin(angle);
    }

    private void v(double lx, double ly, double z, int rgb, float shade) {
        double x = lx * this.rollCos - ly * this.rollSin;
        double y = lx * this.rollSin + ly * this.rollCos;
        double wx = x * this.fz + z * this.fx;
        double wz = -x * this.fx + z * this.fz;
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * shade));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * shade));
        int b = Math.min(255, (int) ((rgb & 255) * shade));
        this.buf.vertex(this.mat, (float) ((this.ox + wx) * this.k), (float) ((this.oy + y) * this.k), (float) ((this.oz + wz) * this.k))
                .color(r, g, b, this.alpha).endVertex();
    }

    void quad(double x0, double y0, double z0, double x1, double y1, double z1,
              double x2, double y2, double z2, double x3, double y3, double z3, int rgb, float shade) {
        v(x0, y0, z0, rgb, shade);
        v(x1, y1, z1, rgb, shade);
        v(x2, y2, z2, rgb, shade);
        v(x3, y3, z3, rgb, shade);
    }

    void tri(double x0, double y0, double z0, double x1, double y1, double z1, double x2, double y2, double z2, int rgb, float shade) {
        quad(x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, rgb, shade);
    }

    /** Axis-aligned (in local space) box. With {@code lit} the faces get simple fake lighting. */
    void box(double x0, double y0, double z0, double x1, double y1, double z1, int rgb, boolean lit) {
        float top = 1.0F, bottom = lit ? 0.5F : 1.0F, ns = lit ? 0.8F : 1.0F, ew = lit ? 0.65F : 1.0F;
        quad(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, rgb, top);
        quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, rgb, bottom);
        quad(x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, rgb, ns);
        quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, rgb, ns);
        quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, rgb, ew);
        quad(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, rgb, ew);
    }

    /** Square beam between two points. */
    void beam(double ax, double ay, double az, double bx, double by, double bz, double thickness, int rgb) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-6D) {
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;
        // any vector not parallel to the beam
        double ux = 0.0D, uy = 1.0D, uz = 0.0D;
        if (Math.abs(dy) > 0.9D) {
            ux = 1.0D;
            uy = 0.0D;
        }
        // p = d x u, q = d x p
        double px = dy * uz - dz * uy, py = dz * ux - dx * uz, pz = dx * uy - dy * ux;
        double pl = Math.sqrt(px * px + py * py + pz * pz);
        double h = thickness * 0.5D;
        px = px / pl * h;
        py = py / pl * h;
        pz = pz / pl * h;
        double qx = (dy * pz - dz * py), qy = (dz * px - dx * pz), qz = (dx * py - dy * px);
        float[] shades = {0.85F, 0.65F, 1.0F, 0.75F};
        double[][] c = {
                {px + qx, py + qy, pz + qz},
                {px - qx, py - qy, pz - qz},
                {-px - qx, -py - qy, -pz - qz},
                {-px + qx, -py + qy, -pz + qz}
        };
        for (int i = 0; i < 4; i++) {
            double[] c0 = c[i], c1 = c[(i + 1) & 3];
            quad(ax + c0[0], ay + c0[1], az + c0[2], ax + c1[0], ay + c1[1], az + c1[2],
                    bx + c1[0], by + c1[1], bz + c1[2], bx + c0[0], by + c0[1], bz + c0[2], rgb, shades[i]);
        }
    }
}
