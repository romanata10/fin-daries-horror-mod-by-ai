package com.findiaries.horror.client;

import com.findiaries.horror.FinDiaries;
import com.findiaries.horror.network.PhenomenonPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
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
@Mod.EventBusSubscriber(modid = FinDiaries.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientPhenomena {
    public static final String VERSION_TEXT = "minecraft.1.wonderland.0.";

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
            case PhenomenonPacket.TRIANGLE: return 600;
            case PhenomenonPacket.SLAB: return 540;
            case PhenomenonPacket.UFO: return 690;
            case PhenomenonPacket.WALKERS: return 140;
            case PhenomenonPacket.TOWER: return 260;
            default: return 0;
        }
    }

    private static boolean isSky(int kind) {
        return kind == PhenomenonPacket.TRIANGLE || kind == PhenomenonPacket.SLAB || kind == PhenomenonPacket.UFO;
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
        return mc.level != null && mc.level.dimension().location().getNamespace().equals(FinDiaries.MODID);
    }

    @SubscribeEvent
    public static void onHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!inDream(mc) || mc.options.hideGui || mc.options.renderDebug) {
            return;
        }
        event.getGuiGraphics().drawString(mc.font, VERSION_TEXT, 2, 2, 0xFFFFFF);
    }

    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        Minecraft mc = Minecraft.getInstance();
        if (inDream(mc) && !event.getLeft().isEmpty()) {
            event.getLeft().set(0, VERSION_TEXT);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // rendering
    // ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        boolean sky = event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY;
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
        RenderSystem.depthMask(true);
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
                case PhenomenonPacket.TRIANGLE -> drawTriangle(g, a, t, cam);
                case PhenomenonPacket.SLAB -> drawSlab(g, a, t, cam);
                case PhenomenonPacket.UFO -> drawUfo(g, a, t, cam);
                case PhenomenonPacket.WALKERS -> drawWalkers(g, a, t, cam);
                case PhenomenonPacket.TOWER -> drawTower(g, a, t, cam);
                default -> {
                }
            }
        }
        tesselator.end();

        if (sky) {
            // sky objects were drawn shrunk towards the camera; drop their depth so the world covers them
            RenderSystem.clear(256, Minecraft.ON_OSX);
        }
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

    // ---- 1. the black triangle -------------------------------------------------------------------

    private static void drawTriangle(Geo g, Active a, float t, Vec3 cam) {
        if (!beginFlying(g, a, t, cam, 6.0D, 420.0D, fade(t, 600, 70.0F))) {
            return;
        }
        final double nose = 340.0D, tail = -230.0D, half = 300.0D, h = 18.0D;
        final int top = 0x09090B, bottom = 0x020203, side = 0x0E0E12;
        g.tri(0, h, nose, -half, h, tail, half, h, tail, top, 1.0F);
        g.tri(0, -h, nose, -half, -h, tail, half, -h, tail, bottom, 1.0F);
        g.quad(0, -h, nose, -half, -h, tail, -half, h, tail, 0, h, nose, side, 1.0F);
        g.quad(0, -h, nose, half, -h, tail, half, h, tail, 0, h, nose, side, 0.8F);
        g.quad(-half, -h, tail, half, -h, tail, half, h, tail, -half, h, tail, side, 0.6F);
    }

    // ---- 4. the dark slab with green and orange lights -------------------------------------------

    private static void drawSlab(Geo g, Active a, float t, Vec3 cam) {
        if (!beginFlying(g, a, t, cam, 2.2D, 80.0D, fade(t, 540, 60.0F))) {
            return;
        }
        final double hw = 25.0D, hh = 5.0D, hl = 70.0D;
        g.box(-hw, -hh, -hl, hw, hh, hl, 0x17181F, true);
        for (int i = 0; i < 4; i++) {
            double z = -48.0D + i * 32.0D;
            int color = (i % 2 == 0) ? 0x3FD24F : 0xE8822E;
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
        final int body = 0x1B222C, bodyTop = 0x242C38;
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
            int color = r.nextInt(3) == 0 ? 0xAEB8C4 : 0x6F7B8A;
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
            int color = r.nextInt(3) == 0 ? 0xAEB8C4 : 0x6F7B8A;
            g.quad(side, y - w, z - len * 0.5D, side, y + w, z - len * 0.5D, side, y + w, z + len * 0.5D, side, y - w, z + len * 0.5D, color, 1.0F);
        }
        // the yellow circle and the yellow specks around it
        final int yellow = 0xF2E23A;
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
        final int color = 0xDCCACC, dark = 0xC4B0B4;
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
                g.beam(0, h - 12, 0, cx * kneeR, kneeY, cz * kneeR, 2.6D, dark);
                g.beam(cx * kneeR, kneeY, cz * kneeR, cx * footR, 0, cz * footR, 2.2D, color);
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
        g.box(-half, 0, -half, half, height, half, 0x2D3138, true);
        Random r = new Random(a.seed);
        for (int i = 0; i < 44; i++) {
            double u = (r.nextDouble() * 2.0D - 1.0D) * (half - 0.6D);
            double v = r.nextDouble() * (height - 16.0D);
            double len = 3.0D + r.nextDouble() * 12.0D;
            double w = 0.08D + r.nextDouble() * 0.16D;
            towerFace(g, r.nextInt(4), u - w, v, u + w, v + len, half, 0.05D, 0x596069);
        }
        for (int i = 0; i < 46; i++) {
            double u = (r.nextDouble() * 2.0D - 1.0D) * (half - 0.8D);
            double v = 1.0D + r.nextDouble() * (height - 6.0D);
            double len = 1.4D + r.nextDouble() * 2.2D;
            towerFace(g, r.nextInt(4), u - 0.28D, v, u + 0.28D, v + len, half, 0.1D, 0xF5D21F);
        }
        // yellow dots circling it
        for (int i = 0; i < 7; i++) {
            double phase = r.nextDouble() * Math.PI * 2.0D;
            double radius = 16.0D + r.nextDouble() * 18.0D;
            double speed = (0.02D + r.nextDouble() * 0.03D) * (r.nextBoolean() ? 1.0D : -1.0D);
            double hy = 8.0D + r.nextDouble() * (height - 16.0D) + Math.sin(t * 0.05D + phase) * 3.0D;
            double ang = phase + t * speed;
            double x = Math.cos(ang) * radius, z = Math.sin(ang) * radius;
            g.box(x - 1.3D, hy - 0.8D, z - 1.3D, x + 1.3D, hy + 0.8D, z + 1.3D, 0xF7E120, false);
        }
    }

    private ClientPhenomena() {
    }
}
