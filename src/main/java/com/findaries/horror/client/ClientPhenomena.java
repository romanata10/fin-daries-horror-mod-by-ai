package com.findaries.horror.client;

import com.findaries.horror.FinDaries;
import com.findaries.horror.network.PhenomenonPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Client side of the mod: draws the things in the sky, the walkers and the tower,
 * shows the fake version string and performs the "thrown to the main menu" kick.
 */
@Mod.EventBusSubscriber(modid = FinDaries.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientPhenomena {
    public static final String VERSION_TEXT = "minecraft.1.wonderland.0.";
    /** Shown instead of the version while a lying triangle is in the sky. */
    public static final String LYING_TEXT = "K***S.RU";

    private static final class Active {
        final int kind;
        final double x, y, z;
        final float angle;
        final long seed;
        final long start;

        Active(PhenomenonPacket p, long start) {
            this.kind = p.kind();
            this.x = p.x();
            this.y = p.y();
            this.z = p.z();
            this.angle = p.angle();
            this.seed = p.seed();
            this.start = start;
        }
    }

    private static final List<Active> ACTIVE = new ArrayList<>();
    private static long clientTicks;
    private static boolean pendingKick;

    private static int duration(int kind) {
        switch (kind) {
            case PhenomenonPacket.TRIANGLE:
            case PhenomenonPacket.TRIANGLE_LYING:
            case PhenomenonPacket.TRIANGLE_STAR:
            case PhenomenonPacket.TRIANGLE_EYES: return 600;
            case PhenomenonPacket.SHIP: return 100;
            case PhenomenonPacket.SLAB: return 540;
            case PhenomenonPacket.UFO: return 690;
            case PhenomenonPacket.WALKERS: return 140;
            case PhenomenonPacket.TOWER: return 260;
            case PhenomenonPacket.GIANT_NEVELIM: return 300;
            default: return 0;
        }
    }

    private static boolean isSky(int kind) {
        return PhenomenonPacket.isTriangle(kind) || kind == PhenomenonPacket.SLAB || kind == PhenomenonPacket.UFO;
    }

    /** Called on the client main thread when the server sends a phenomenon. */
    public static void receive(PhenomenonPacket packet) {
        if (packet.kind() == PhenomenonPacket.KICK) {
            pendingKick = true;
            return;
        }
        ACTIVE.add(new Active(packet, clientTicks));
    }

    /** Test hook: show a phenomenon as if it had started {@code age} ticks ago. */
    static void debugShow(PhenomenonPacket packet, int age) {
        ACTIVE.clear();
        ACTIVE.add(new Active(packet, clientTicks - age));
    }

    static void debugClear() {
        ACTIVE.clear();
    }

    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVE.clear();
            pendingKick = false;
            return;
        }
        if (pendingKick) {
            pendingKick = false;
            ACTIVE.clear();
            kickToTitle(mc);
            return;
        }
        if (!mc.isPaused()) {
            clientTicks++;
            ACTIVE.removeIf(a -> clientTicks - a.start > duration(a.kind));
        }
    }

    /** Same sequence as the "Save and Quit to Title" button. */
    private static void kickToTitle(Minecraft mc) {
        boolean local = mc.isLocalServer();
        if (mc.level != null) {
            mc.level.disconnect();
        }
        if (local) {
            mc.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
        } else {
            mc.clearLevel();
        }
        mc.setScreen(new TitleScreen());
    }

    // ---------------------------------------------------------------------------------------------
    // version string
    // ---------------------------------------------------------------------------------------------

    private static boolean inDream(Minecraft mc) {
        if (mc.level == null || !mc.level.dimension().location().getNamespace().equals(FinDaries.MODID)) {
            return false;
        }
        return switch (mc.level.dimension().location().getPath()) {
            case "wonderland", "hallway", "wonderflowers", "cage_wonderland" -> true;
            default -> false; // hellway, the_wonderdream and stonehell have the normal version
        };
    }

    private static boolean lyingTriangle() {
        for (Active a : ACTIVE) {
            if (a.kind == PhenomenonPacket.TRIANGLE_LYING) {
                return true;
            }
        }
        return false;
    }

    /** True while a star triangle is in the sky and has not yet flown past the player. */
    private static boolean vhs(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        for (Active a : ACTIVE) {
            if (a.kind != PhenomenonPacket.TRIANGLE_STAR) {
                continue;
            }
            double dx = -Math.sin(a.angle), dz = Math.cos(a.angle);
            double t = clientTicks - a.start;
            double x = a.x + dx * TRIANGLE_SPEED * t, z = a.z + dz * TRIANGLE_SPEED * t;
            double ahead = (mc.player.getX() - x) * dx + (mc.player.getZ() - z) * dz;
            if (ahead > -380.0D) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        if (vhs(mc)) {
            drawVhs(mc, g);
        }
        if (mc.options.hideGui || mc.options.renderDebug) {
            return;
        }
        if (lyingTriangle()) {
            g.drawString(mc.font, LYING_TEXT, 2, 2, 0xFFFFFF);
        } else if (inDream(mc)) {
            g.drawString(mc.font, VERSION_TEXT, 2, 2, 0xFFFFFF);
        }
    }

    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getLeft().isEmpty()) {
            return;
        }
        if (lyingTriangle()) {
            event.getLeft().set(0, LYING_TEXT);
        } else if (inDream(mc)) {
            event.getLeft().set(0, VERSION_TEXT);
        }
    }

    /** Old video tape: scan lines, a rolling tracking bar, noise, colour bleed and the PLAY label. */
    private static void drawVhs(Minecraft mc, GuiGraphics g) {
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        long tick = clientTicks;
        Random r = new Random(tick / 2 * 7919L);
        g.fill(0, 0, w, h, 0x1C182C50);
        for (int y = 0; y < h; y += 3) {
            g.fill(0, y, w, y + 1, 0x38000000);
        }
        // colour bleed at the edges
        g.fill(0, 0, 3, h, 0x30FF2020);
        g.fill(w - 3, 0, w, h, 0x302040FF);
        // rolling tracking bar
        int bar = (int) ((tick * 3) % (h + 60)) - 30;
        g.fill(0, bar, w, bar + 9, 0x30FFFFFF);
        g.fill(0, bar + 3, w, bar + 4, 0x70FFFFFF);
        // torn lines of noise
        for (int i = 0; i < 7; i++) {
            int y = r.nextInt(h);
            int x = r.nextInt(w);
            g.fill(x, y, Math.min(w, x + 20 + r.nextInt(w / 3)), y + 1, 0x60FFFFFF);
        }
        // head switching noise at the bottom
        for (int x = 0; x < w; x += 6) {
            int a = 40 + r.nextInt(150);
            g.fill(x, h - 9 + r.nextInt(3), x + 6, h - 5, (a << 24) | 0xE8E8E8);
        }
        g.pose().pushPose();
        g.pose().scale(2.0F, 2.0F, 1.0F);
        if ((tick / 12) % 4 != 3) {
            g.drawString(mc.font, "PLAY \u25B6", 8, 12, 0xFFFFFF);
        }
        g.drawString(mc.font, "SP", w / 2 - 22, 12, 0xFFFFFF);
        long sec = tick / 20;
        g.drawString(mc.font, String.format("0:%02d:%02d", (sec / 60) % 60, sec % 60), 8, h / 2 - 22, 0xFFFFFF);
        g.pose().popPose();
    }

    // ---------------------------------------------------------------------------------------------
    // rendering
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        // Clouds are drawn AFTER_SKY: draw the sky objects once more after the weather so
        // that they cover the clouds instead of having cloud shapes punched through them.
        boolean sky = event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY
                || event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER;
        boolean world = event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES;
        if (!sky && !world) {
            return;
        }
        boolean any = false;
        for (Active a : ACTIVE) {
            if (isSky(a.kind) == sky) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        float partial = Minecraft.getInstance().isPaused() ? 0.0F : event.getPartialTick();

        // Draw with the camera rotation from the event and an identity model-view,
        // whatever the surrounding vanilla code left on the stack.
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(world);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Geo g = new Geo(buffer, event.getPoseStack().last().pose());
        for (Active a : ACTIVE) {
            if (isSky(a.kind) != sky) {
                continue;
            }
            float t = (clientTicks - a.start) + partial;
            switch (a.kind) {
                case PhenomenonPacket.TRIANGLE, PhenomenonPacket.TRIANGLE_LYING, PhenomenonPacket.TRIANGLE_STAR,
                        PhenomenonPacket.TRIANGLE_EYES -> drawTriangle(g, a, t, cam);
                case PhenomenonPacket.SHIP -> drawShip(g, a, t, cam);
                case PhenomenonPacket.SLAB -> drawSlab(g, a, t, cam);
                case PhenomenonPacket.UFO -> drawUfo(g, a, t, cam);
                case PhenomenonPacket.WALKERS -> drawWalkers(g, a, t, cam);
                case PhenomenonPacket.TOWER -> drawTower(g, a, t, cam);
                case PhenomenonPacket.GIANT_NEVELIM -> drawGiant(g, a, t, cam);
                default -> {
                }
            }
        }
        tesselator.end();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        modelView.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    private static float fade(float t, int duration, float edge) {
        float f = Math.min(t / edge, (duration - t) / edge);
        return Math.max(0.0F, Math.min(1.0F, f));
    }

    /** Places a flying object and returns false if it is not visible this frame. */
    private static boolean beginFlying(Geo g, Active a, float t, Vec3 cam, double speed, double radius, float alpha) {
        double dx = -Math.sin(a.angle), dz = Math.cos(a.angle);
        double x = a.x + dx * speed * t - cam.x;
        double y = a.y - cam.y;
        double z = a.z + dz * speed * t - cam.z;
        if (alpha <= 0.004F) {
            return false;
        }
        double reach = Math.sqrt(x * x + y * y + z * z) + radius;
        double k = Math.min(1.0D, 100.0D / reach);
        g.begin(x, y, z, dx, dz, k, alpha);
        return true;
    }

    // ---- 1. all-black 3D cones (tip always pointing down) -------------------------------------

    private static final double TRIANGLE_SPEED = 6.0D;

    /** A 3D cone: wide circular base on top, sharp point at the bottom. */
    private static void drawCone(Geo g, double radius, double halfHeight, int rgb) {
        final int n = 28;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2.0D * i / n, a1 = Math.PI * 2.0D * (i + 1) / n;
            double x0 = Math.cos(a0) * radius, z0 = Math.sin(a0) * radius;
            double x1 = Math.cos(a1) * radius, z1 = Math.sin(a1) * radius;
            float shade = 0.5F + 0.5F * (float) (0.5D + 0.5D * Math.cos(a0 - 0.7D));
            g.tri(x0, halfHeight, z0, x1, halfHeight, z1, 0.0D, -halfHeight, 0.0D, rgb, shade);
            g.tri(0.0D, halfHeight, 0.0D, x1, halfHeight, z1, x0, halfHeight, z0, 0x111319, 0.9F);
        }
    }

    /** A white oval in a plane of constant local Z - the side turned towards the player. */
    private static void oval(Geo g, double z, double cx, double cy, double rx, double ry, int rgb) {
        final int n = 12;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2.0D * i / n, a1 = Math.PI * 2.0D * (i + 1) / n;
            g.tri(cx, cy, z,
                    cx + Math.cos(a0) * rx, cy + Math.sin(a0) * ry, z,
                    cx + Math.cos(a1) * rx, cy + Math.sin(a1) * ry, z, rgb, 1.0F);
        }
    }

    private static void drawTriangle(Geo g, Active a, float t, Vec3 cam) {
        if (!beginFlying(g, a, t, cam, TRIANGLE_SPEED, 420.0D, fade(t, 600, 70.0F))) {
            return;
        }
        if (a.kind == PhenomenonPacket.TRIANGLE_STAR) {
            drawStar3D(g, a, t);
            return;
        }
        boolean lying = a.kind == PhenomenonPacket.TRIANGLE_LYING;
        boolean eyes = a.kind == PhenomenonPacket.TRIANGLE_EYES;
        // the lying cone leans on its side, but the tip still points mostly down
        g.roll(lying ? 1.15D : 0.0D);
        final double r = 215.0D, hh = 235.0D;
        drawCone(g, r, hh, 0x05070B);
        if (eyes) {
            double z = -r * 0.34D, cy = hh * 0.28D;
            oval(g, z, -r * 0.20D, cy, 42.0D, 27.0D, 0xF7F5EE);
            oval(g, z, r * 0.20D, cy, 42.0D, 27.0D, 0xF7F5EE);
        }
    }

    /** Image-14: a soft, blurry blue 3D diamond / octahedron in the sky. */
    private static void drawStar3D(Geo g, Active a, float t) {
        final double R = 230.0D, D = 78.0D;
        final int light = 0xB6DCFF, mid = 0x7FB0EA, dark = 0x4E86C8;
        double[][] v = {{0, R, 0}, {0, -R, 0}, {-R, 0, 0}, {R, 0, 0}, {0, 0, -D}, {0, 0, D}};
        int[][] faces = {{0, 4, 2}, {0, 3, 4}, {0, 2, 5}, {0, 5, 3},
                         {1, 2, 4}, {1, 4, 3}, {1, 5, 2}, {1, 3, 5}};
        int[] cols = {light, light, mid, mid, mid, dark, dark, dark};
        float[] sh = {1.0F, 0.94F, 0.88F, 0.82F, 0.86F, 0.78F, 0.72F, 0.66F};
        for (int i = 0; i < faces.length; i++) {
            int[] f = faces[i];
            g.tri(v[f[0]][0], v[f[0]][1], v[f[0]][2],
                  v[f[1]][0], v[f[1]][1], v[f[1]][2],
                  v[f[2]][0], v[f[2]][1], v[f[2]][2], cols[i], sh[i]);
        }
    }

    // ---- 10. the 860 block tall Nevelim ----------------------------------------------------------

    private static void drawGiant(Geo g, Active a, float t, Vec3 cam) {
        double rise = Math.min(1.0D, t / 70.0D);
        double sink = -860.0D * (1.0D - rise);
        double dx = a.x - cam.x, dy = a.y - cam.y + sink, dz = a.z - cam.z;
        double k = Math.min(1.0D, 100.0D / (Math.sqrt(dx * dx + dy * dy + dz * dz) + 430.0D));
        g.begin(dx, dy, dz, 0.0D, 1.0D, k, fade(t, 300, 24.0F));
        final int body = 0x1B1D22, head = 0x0C0D10, bone = 0xE8E8E0;
        // image-21 seen from below: two enormous legs, wide torso, long arms, cube head
        g.box(-70, 0, -20, -20, 520, 20, body, true);
        g.box(20, 0, -20, 70, 520, 20, body, true);
        g.box(-90, 520, -24, 90, 760, 24, body, true);
        g.box(-140, 300, -18, -90, 740, 18, body, true);
        g.box(90, 300, -18, 140, 740, 18, body, true);
        g.box(-70, 760, -30, 70, 860, 30, head, true);
        g.box(-46, 800, -32, -18, 818, -30, bone, false);
        g.box(18, 800, -32, 46, 818, -30, bone, false);
        for (int i = 0; i < 6; i++) {
            g.box(-40 + i * 14, 770, -32, -34 + i * 14, 786, -30, bone, false);
        }
        if (t >= 130 && t < 225) {
            g.box(-34, -40, -34, 34, 770, -32, 0xFF6A12, false);
            g.box(-14, -40, -37, 14, 770, -35, 0xFFC060, false);
        }
        if (t >= 215) {
            double burst = Math.min(1.0D, (t - 215) / 28.0D) * 420.0D;
            g.quad(-burst, -10, 0, burst, -10, 0, burst, burst * 0.55D, 0, -burst, burst * 0.55D, 0, 0xFFB03C, 0.9F);
            g.quad(0, -10, -burst, 0, -10, burst, 0, burst * 0.55D, burst, 0, burst * 0.55D, -burst, 0xFFE0A0, 0.9F);
        }
    }

    /** Everything shakes while the giant climbs out of the ground. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        boolean shaking = false;
        for (Active a : ACTIVE) {
            if (a.kind == PhenomenonPacket.GIANT_NEVELIM && clientTicks - a.start < 270) {
                shaking = true;
                break;
            }
        }
        if (!shaking) {
            return;
        }
        Random r = new Random(clientTicks * 7919L);
        float amp = 2.2F;
        event.setYaw(event.getYaw() + (r.nextFloat() - 0.5F) * amp);
        event.setPitch(event.getPitch() + (r.nextFloat() - 0.5F) * amp);
        event.setRoll(event.getRoll() + (r.nextFloat() - 0.5F) * amp * 1.6F);
    }

    // ---- the falling ship of the Sky People -------------------------------------------------------

    private static void drawShip(Geo g, Active a, float t, Vec3 cam) {
        double p = Math.max(0.0D, 1.0D - t / 100.0D);
        double x = a.x - 90.0D * p, y = a.y + 260.0D * p, z = a.z;
        g.begin(x - cam.x, y - cam.y, z - cam.z, 0.0D, 1.0D, 1.0D, 1.0F);
        g.box(-7, 0, -5, 7, 2, 5, 0x1E6FC8, true);
        g.box(-4, 2, -4, 4, 3, 4, 0x2E8FE8, true);
        g.box(-1, 3, -1, 1, 4, 1, 0xFF8A28, false);
        double flick = 0.8D + 0.2D * Math.sin(t * 2.1F);
        g.beam(0, 1, 0, -30 * flick, 80 * flick, 0, 4.0D, 0x3A6FA0);
    }

    // ---- 4. the dark slab with green and orange lights -------------------------------------------

    private static void drawSlab(Geo g, Active a, float t, Vec3 cam) {
        if (!beginFlying(g, a, t, cam, 2.2D, 80.0D, fade(t, 540, 60.0F))) {
            return;
        }
        final double hw = 25.0D, hh = 5.0D, hl = 70.0D;
        g.box(-hw, -hh, -hl, hw, hh, hl, 0x0A0B10, true);
        for (int i = 0; i < 4; i++) {
            double z = -48.0D + i * 32.0D;
            int color = (i % 2 == 0) ? 0x37FF5A : 0xFF7A1F;
            // lights on the top, on the bottom and on both long sides
            g.quad(-9, hh + 1.5D, z - 5, 9, hh + 1.5D, z - 5, 9, hh + 1.5D, z + 5, -9, hh + 1.5D, z + 5, color, 1.0F);
            g.quad(-9, -hh - 1.5D, z - 5, 9, -hh - 1.5D, z - 5, 9, -hh - 1.5D, z + 5, -9, -hh - 1.5D, z + 5, color, 1.0F);
            g.quad(hw + 1.5D, -2, z - 5, hw + 1.5D, 2, z - 5, hw + 1.5D, 2, z + 5, hw + 1.5D, -2, z + 5, color, 1.0F);
            g.quad(-hw - 1.5D, -2, z - 5, -hw - 1.5D, 2, z - 5, -hw - 1.5D, 2, z + 5, -hw - 1.5D, -2, z + 5, color, 1.0F);
        }
    }

    // ---- 9. the thousand block thing --------------------------------------------------------------

    private static final double[][] UFO_OUTLINE = {
            {-120, 500}, {120, 500}, {210, 300}, {210, -300}, {120, -500}, {-120, -500}, {-210, -300}, {-210, 300}
    };

    private static boolean insideUfo(double x, double z, double margin) {
        double ax = Math.abs(x), az = Math.abs(z);
        if (az > 500.0D - margin) {
            return false;
        }
        double limit = az <= 300.0D ? 210.0D : 210.0D - (az - 300.0D) * 0.45D;
        return ax < limit - margin;
    }

    private static void drawUfo(Geo g, Active a, float t, Vec3 cam) {
        if (!beginFlying(g, a, t, cam, 7.0D, 560.0D, fade(t, 690, 80.0F))) {
            return;
        }
        final double h = 55.0D;
        final int body = 0x12161E, bodyTop = 0x1C2230;
        int n = UFO_OUTLINE.length;
        for (int i = 0; i < n; i++) {
            double[] p = UFO_OUTLINE[i], q = UFO_OUTLINE[(i + 1) % n];
            g.tri(0, h, 0, p[0], h, p[1], q[0], h, q[1], bodyTop, 1.0F);
            g.tri(0, -h, 0, p[0], -h, p[1], q[0], -h, q[1], body, 0.8F);
            g.quad(p[0], -h, p[1], q[0], -h, q[1], q[0], h, q[1], p[0], h, p[1], body, 0.75F + 0.25F * ((i * 3) % 4) / 3.0F);
        }
        Random r = new Random(a.seed);
        // pale streaks along the hull, underneath and on the flanks
        for (int i = 0; i < 90; i++) {
            double x = (r.nextDouble() * 2.0D - 1.0D) * 200.0D;
            double z = (r.nextDouble() * 2.0D - 1.0D) * 470.0D;
            double len = 30.0D + r.nextDouble() * 150.0D;
            double w = 2.0D + r.nextDouble() * 4.0D;
            int color = r.nextInt(3) == 0 ? 0x8E9AA8 : 0x4E5866;
            if (!insideUfo(x, z - len * 0.5D, w + 4.0D) || !insideUfo(x, z + len * 0.5D, w + 4.0D)) {
                continue;
            }
            double y = -h - 4.0D;
            g.quad(x - w, y, z - len * 0.5D, x + w, y, z - len * 0.5D, x + w, y, z + len * 0.5D, x - w, y, z + len * 0.5D, color, 1.0F);
        }
        for (int i = 0; i < 40; i++) {
            double side = r.nextBoolean() ? 214.0D : -214.0D;
            double z = (r.nextDouble() * 2.0D - 1.0D) * 280.0D;
            double len = 20.0D + r.nextDouble() * 80.0D;
            double y = (r.nextDouble() * 2.0D - 1.0D) * (h - 6.0D);
            double w = 1.5D + r.nextDouble() * 2.5D;
            if (Math.abs(z) + len * 0.5D > 296.0D) {
                continue;
            }
            int color = r.nextInt(3) == 0 ? 0x8E9AA8 : 0x4E5866;
            g.quad(side, y - w, z - len * 0.5D, side, y + w, z - len * 0.5D, side, y + w, z + len * 0.5D, side, y - w, z + len * 0.5D, color, 1.0F);
        }
        // the yellow circle and the yellow specks around it
        final int yellow = 0xF2D54A;
        double cx = -70.0D, cz = 260.0D, rad = 62.0D, y = -h - 6.0D;
        for (int i = 0; i < 24; i++) {
            double a0 = Math.PI * 2.0D * i / 24.0D, a1 = Math.PI * 2.0D * (i + 1) / 24.0D;
            g.tri(cx, y, cz, cx + Math.cos(a0) * rad, y, cz + Math.sin(a0) * rad, cx + Math.cos(a1) * rad, y, cz + Math.sin(a1) * rad, yellow, 1.0F);
        }
        for (int i = 0; i < 34; i++) {
            double ang = r.nextDouble() * Math.PI * 2.0D;
            double d = rad + 12.0D + r.nextDouble() * 90.0D;
            double x = cx + Math.cos(ang) * d, z = cz + Math.sin(ang) * d;
            double s = 2.5D + r.nextDouble() * 4.0D;
            if (!insideUfo(x, z, s + 3.0D)) {
                continue;
            }
            g.quad(x - s, y, z - s, x + s, y, z - s, x + s, y, z + s, x - s, y, z + s, yellow, 1.0F);
        }
    }

    // ---- 5. giant mushrooms on legs ----------------------------------------------------------------

    private static void drawWalkers(Geo g, Active a, float t, Vec3 cam) {
        Random r = new Random(a.seed);
        int count = 3 + r.nextInt(3);
        final int color = 0xE8E4E0, dark = 0xCFC9C4;
        for (int i = 0; i < count; i++) {
            double ang = r.nextDouble() * Math.PI * 2.0D;
            double dist = i == 0 ? 0.0D : 25.0D + r.nextDouble() * 55.0D;
            double height = 72.0D + r.nextDouble() * 44.0D;
            double turn = r.nextDouble() * Math.PI * 2.0D;
            double x = a.x + Math.cos(ang) * dist - cam.x;
            double y = a.y - 4.0D - cam.y;
            double z = a.z + Math.sin(ang) * dist - cam.z;
            g.begin(x, y, z, Math.sin(turn), Math.cos(turn), 1.0D, 0.86F);
            double h = height;
            // cap
            g.box(-13, h - 3, -13, 13, h, 13, dark, true);
            g.box(-16, h, -16, 16, h + 5, 16, color, true);
            g.box(-14, h + 5, -14, 14, h + 9, 14, color, true);
            g.box(-11, h + 9, -11, 11, h + 12, 11, color, true);
            g.box(-7, h + 12, -7, 7, h + 14, 7, color, true);
            // neck
            g.box(-3, h - 13, -3, 3, h - 3, 3, dark, true);
            // four legs: out from under the cap, then all the way down
            for (int leg = 0; leg < 4; leg++) {
                double la = Math.PI * 0.25D + Math.PI * 0.5D * leg;
                double cx = Math.cos(la), cz = Math.sin(la);
                double kneeR = 17.0D + r.nextDouble() * 6.0D, footR = 30.0D + r.nextDouble() * 12.0D;
                double kneeY = h - 20.0D - r.nextDouble() * 8.0D;
                g.beam(0, h - 12, 0, cx * kneeR, kneeY, cz * kneeR, 1.8D, dark);
                g.beam(cx * kneeR, kneeY, cz * kneeR, cx * footR, 0, cz * footR, 1.5D, color);
            }
        }
    }

    // ---- 6. the rising tower ------------------------------------------------------------------------

    private static void towerFace(Geo g, int face, double u0, double v0, double u1, double v1, double half, double off, int color) {
        double d = half + off;
        switch (face) {
            case 0 -> g.quad(u0, v0, -d, u1, v0, -d, u1, v1, -d, u0, v1, -d, color, 1.0F);
            case 1 -> g.quad(u0, v0, d, u1, v0, d, u1, v1, d, u0, v1, d, color, 1.0F);
            case 2 -> g.quad(-d, v0, u0, -d, v0, u1, -d, v1, u1, -d, v1, u0, color, 1.0F);
            default -> g.quad(d, v0, u0, d, v0, u1, d, v1, u1, d, v1, u0, color, 1.0F);
        }
    }

    private static void drawTower(Geo g, Active a, float t, Vec3 cam) {
        final double half = 6.0D, height = 64.0D;
        double base = a.y - height + 0.55D * t;
        g.begin(a.x - cam.x, base - cam.y, a.z - cam.z, 0.0D, 1.0D, 1.0D, 1.0F);
        g.box(-half, 0, -half, half, height, half, 0x1A1C22, true);
        Random r = new Random(a.seed);
        for (int i = 0; i < 44; i++) {
            double u = (r.nextDouble() * 2.0D - 1.0D) * (half - 0.6D);
            double v = r.nextDouble() * (height - 16.0D);
            double len = 3.0D + r.nextDouble() * 12.0D;
            double w = 0.08D + r.nextDouble() * 0.16D;
            towerFace(g, r.nextInt(4), u - w, v, u + w, v + len, half, 0.05D, 0x3A3E48);
        }
        for (int i = 0; i < 46; i++) {
            double u = (r.nextDouble() * 2.0D - 1.0D) * (half - 0.8D);
            double v = 1.0D + r.nextDouble() * (height - 6.0D);
            double len = 1.4D + r.nextDouble() * 2.2D;
            towerFace(g, r.nextInt(4), u - 0.28D, v, u + 0.28D, v + len, half, 0.1D, 0xF2C230);
        }
        // yellow dots circling it
        for (int i = 0; i < 7; i++) {
            double phase = r.nextDouble() * Math.PI * 2.0D;
            double radius = 16.0D + r.nextDouble() * 18.0D;
            double speed = (0.02D + r.nextDouble() * 0.03D) * (r.nextBoolean() ? 1.0D : -1.0D);
            double hy = 8.0D + r.nextDouble() * (height - 16.0D) + Math.sin(t * 0.05D + phase) * 3.0D;
            double ang = phase + t * speed;
            double x = Math.cos(ang) * radius, z = Math.sin(ang) * radius;
            g.box(x - 1.3D, hy - 0.8D, z - 1.3D, x + 1.3D, hy + 0.8D, z + 1.3D, 0xFFD84A, false);
        }
    }

    private ClientPhenomena() {
    }
}
