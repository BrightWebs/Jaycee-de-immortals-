package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.appearance.Features;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3f;
import org.joml.Matrix3x2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Gallery thumbnails: the option's voxels drawn in a three-quarter view on a plain mannequin,
 * each visible cube face filled as a shaded parallelogram (painter's order, far to near).
 */
final class Thumb {
    private Thumb() {}

    private static final float[][] HOSTS = {{0, 0, 0}, {0, 0, 0}, {-5, 2, 0}, {5, 2, 0}, {-1.9f, 12, 0}, {1.9f, 12, 0}};
    private static final Vector3f LIGHT = new Vector3f(-0.35f, -0.8f, -0.5f).normalize();

    private record Face(float depth, Vector3f p0, Vector3f p1, Vector3f p3, int color) {}

    /** The mannequin under the features: a pale head, and for body views a body, arms and legs. */
    private static List<Features.Box> mannequin(Features.View view, boolean legs) {
        List<Features.Box> m = new ArrayList<>();
        int skin = Features.WHITE;
        m.add(new Features.Box(Features.HEAD, 0, 0, 0, 0, 0, 0, -4, -8, -4, 8, 8, 8, skin));
        if (view == Features.View.BODY || view == Features.View.BACK) {
            m.add(new Features.Box(Features.BODY, 0, 0, 0, 0, 0, 0, -4, 0, -2, 8, 12, 4, Features.DARK));
            m.add(new Features.Box(Features.RIGHT_ARM, 0, 0, 0, 0, 0, 0, -3, -2, -2, 4, 12, 4, Features.DARK));
            m.add(new Features.Box(Features.LEFT_ARM, 0, 0, 0, 0, 0, 0, -1, -2, -2, 4, 12, 4, Features.DARK));
            if (legs) {
                m.add(new Features.Box(Features.RIGHT_LEG, 0, 0, 0, 0, 0, 0, -2, 0, -2, 4, 12, 4, Features.DARK));
                m.add(new Features.Box(Features.LEFT_LEG, 0, 0, 0, 0, 0, 0, -2, 0, -2, 4, 12, 4, Features.DARK));
            }
        }
        return m;
    }

    /**
     * Draws boxes into the rectangle. color maps a box to ARGB; the mannequin uses pale and dark greys.
     */
    static void draw(GuiGraphicsExtractor g, List<Features.Box> boxes, ToIntFunction<Features.Box> color, Features.View view,
                     float x, float y, float w, float h) {
        boolean legs = boxes.stream().noneMatch(b -> b.part() == Features.BODY && b.y() > 10 && b.h() > 6 && b.w() > 7);
        float yaw = switch (view) {
            case HEAD -> 28;
            case FACE -> 18;
            case BACK -> 205;
            case BODY -> 24;
        };
        Matrix3f cam = new Matrix3f().rotateX((float) Math.toRadians(-10)).rotateY((float) Math.toRadians(yaw));
        List<Face> faces = new ArrayList<>();
        for (Features.Box b : mannequin(view, legs)) addFaces(faces, b, cam, b.tint() == Features.WHITE ? 0xFFE6E4EC : 0xFF3A4258);
        for (Features.Box b : boxes) addFaces(faces, b, cam, color.applyAsInt(b));
        if (faces.isEmpty()) return;

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Face f : faces) {
            for (Vector3f p : new Vector3f[]{f.p0, f.p1, f.p3, new Vector3f(f.p1).add(f.p3).sub(f.p0)}) {
                minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x); minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
            }
        }
        float s = Math.min(w * 0.86f / Math.max(1, maxX - minX), h * 0.86f / Math.max(1, maxY - minY));
        float ox = x + w / 2 - (minX + maxX) / 2 * s, oy = y + h / 2 - (minY + maxY) / 2 * s;
        faces.sort((a, b) -> Float.compare(b.depth, a.depth));
        for (Face f : faces) {
            g.pose().pushMatrix();
            g.pose().mul(new Matrix3x2f(
                    (f.p1.x - f.p0.x) * s, (f.p1.y - f.p0.y) * s,
                    (f.p3.x - f.p0.x) * s, (f.p3.y - f.p0.y) * s,
                    ox + f.p0.x * s, oy + f.p0.y * s));
            g.fill(0, 0, 1, 1, f.color);
            g.pose().popMatrix();
        }
    }

    private static void addFaces(List<Face> out, Features.Box b, Matrix3f cam, int argb) {
        Matrix3f rot = new Matrix3f().rotationZYX((float) Math.toRadians(b.rz()), (float) Math.toRadians(b.ry()), (float) Math.toRadians(b.rx()));
        float[] host = HOSTS[Math.max(0, Math.min(5, b.part()))];
        Vector3f[] c = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            Vector3f v = new Vector3f(b.x() + ((i & 1) != 0 ? b.w() : 0), b.y() + ((i & 2) != 0 ? b.h() : 0), b.z() + ((i & 4) != 0 ? b.d() : 0));
            rot.transform(v);
            v.add(b.px() + host[0], b.py() + host[1], b.pz() + host[2]);
            cam.transform(v);
            c[i] = v;
        }
        // each face: origin corner, two edge corners (corner index bits: 1 = +x, 2 = +y, 4 = +z)
        int[][] f = {{0, 4, 2}, {1, 3, 5}, {0, 1, 4}, {2, 6, 3}, {0, 2, 1}, {4, 5, 6}}; // wound so the cross product points outward
        for (int[] q : f) {
            Vector3f p0 = c[q[0]], p1 = c[q[1]], p3 = c[q[2]];
            Vector3f n = new Vector3f(p1).sub(p0).cross(new Vector3f(p3).sub(p0));
            if (n.lengthSquared() < 1e-6f) continue;
            n.normalize();
            if (n.z >= 0) continue; // faces away from the viewer (who looks along +z)
            float lit = 0.55f + 0.45f * Math.max(0, n.dot(LIGHT));
            float depth = Math.max(Math.max(p0.z, p1.z), Math.max(p3.z, p1.z + p3.z - p0.z)); // farthest corner: keeps decals on big faces
            out.add(new Face(depth, p0, p1, p3, shade(argb, lit)));
        }
    }

    static int shade(int argb, float k) {
        int r = Math.min(255, Math.round(((argb >> 16) & 255) * k)), gg = Math.min(255, Math.round(((argb >> 8) & 255) * k)),
                b = Math.min(255, Math.round((argb & 255) * k));
        return (argb & 0xFF000000) | (r << 16) | (gg << 8) | b;
    }
}
