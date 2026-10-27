package com.blackholeinfinite;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Sphere;

/**
 * A bola NAO e limitada em tamanho. Para evitar problemas com o plano de corte da camera, o conjunto
 * (posicao + raio) e escalado uniformemente em torno da camera quando fica longe/grande demais:
 * o tamanho aparente na tela continua exatamente o real.
 */
public class HoleRenderer {
    private final Sphere sphere = new Sphere();

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;
        if (ClientProxy.holes.isEmpty() && ClientProxy.flying.isEmpty()) return;
        Entity v = mc.getRenderViewEntity();
        if (v == null) return;
        float pt = e.partialTicks;
        double vx = v.lastTickPosX + (v.posX - v.lastTickPosX) * pt;
        double vy = v.lastTickPosY + (v.posY - v.lastTickPosY) * pt;
        double vz = v.lastTickPosZ + (v.posZ - v.lastTickPosZ) * pt;
        int dim = mc.theWorld.provider.getDimensionId();
        double L = Math.max(64.0, mc.gameSettings.renderDistanceChunks * 16.0);
        float time = (float) (mc.theWorld.getTotalWorldTime() % 1000000L) + pt;
        boolean fog = GL11.glIsEnabled(GL11.GL_FOG);

        drawFlyingBlocks(mc, dim, vx, vy, vz, pt);

        for (ClientProxy.ClientHole h : ClientProxy.holes.values()) {
            if (h.dim != dim) continue;
            double rx = h.x - vx, ry = h.y - vy, rz = h.z - vz;
            double dist = Math.sqrt(rx * rx + ry * ry + rz * rz);
            double rr = h.visual;
            if (Double.isNaN(rr) || rr <= 0) continue;
            if (dist > L && rr / dist < 0.002) continue;
            double s = 1.0, lim = L * 0.6;
            if (dist + rr > lim) s = lim / (dist + rr);
            draw(rx * s, ry * s, rz * s, (float) (rr * s), time);
        }

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glLineWidth(1f);
        if (fog) GlStateManager.enableFog();
    }

    // ---------------------------------------------------- blocos sendo puxados
    private void drawFlyingBlocks(Minecraft mc, int dim, double vx, double vy, double vz, float pt) {
        if (ClientProxy.flying.isEmpty()) return;
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        for (ClientProxy.FlyingBlock f : ClientProxy.flying) {
            if (f.dim != dim) continue;
            IBlockState st = Block.getStateById(f.stateId);
            if (st == null) continue;
            TextureAtlasSprite sp = mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(st);
            if (sp == null) continue;
            float t = Math.min(1f, (f.age + pt) / (float) f.maxAge);
            float e = t * t; // acelera ao se aproximar do nucleo
            double rx = f.sx - f.cx, ry = f.sy - f.cy, rz = f.sz - f.cz;
            double ang = e * 1.6, c = Math.cos(ang), s = Math.sin(ang);
            double px = f.cx + (rx * c - rz * s) * (1 - e);
            double py = f.cy + ry * (1 - e);
            double pz = f.cz + (rx * s + rz * c) * (1 - e);
            float size = 0.5f * (1f - 0.85f * e);
            GL11.glPushMatrix();
            GL11.glTranslated(px - vx, py - vy, pz - vz);
            GL11.glRotatef(e * 540f, 0f, 1f, 0f);
            GL11.glRotatef(e * 300f, 1f, 0f, 1f);
            cube(sp, size);
            GL11.glPopMatrix();
        }
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private void vtx(float x, float y, float z, float u, float v) {
        GL11.glTexCoord2f(u, v);
        GL11.glVertex3f(x, y, z);
    }

    private void cube(TextureAtlasSprite sp, float s) {
        float u0 = sp.getMinU(), u1 = sp.getMaxU(), v0 = sp.getMinV(), v1 = sp.getMaxV();
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(0.8f, 0.8f, 0.8f, 1f);
        vtx(-s, -s, s, u0, v1); vtx(s, -s, s, u1, v1); vtx(s, s, s, u1, v0); vtx(-s, s, s, u0, v0);
        vtx(s, -s, -s, u0, v1); vtx(-s, -s, -s, u1, v1); vtx(-s, s, -s, u1, v0); vtx(s, s, -s, u0, v0);
        GL11.glColor4f(0.65f, 0.65f, 0.65f, 1f);
        vtx(-s, -s, -s, u0, v1); vtx(-s, -s, s, u1, v1); vtx(-s, s, s, u1, v0); vtx(-s, s, -s, u0, v0);
        vtx(s, -s, s, u0, v1); vtx(s, -s, -s, u1, v1); vtx(s, s, -s, u1, v0); vtx(s, s, s, u0, v0);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        vtx(-s, s, s, u0, v1); vtx(s, s, s, u1, v1); vtx(s, s, -s, u1, v0); vtx(-s, s, -s, u0, v0);
        GL11.glColor4f(0.5f, 0.5f, 0.5f, 1f);
        vtx(-s, -s, -s, u0, v1); vtx(s, -s, -s, u1, v1); vtx(s, -s, s, u1, v0); vtx(-s, -s, s, u0, v0);
        GL11.glEnd();
    }

    // ------------------------------------------------------------- buraco negro
    private void draw(double x, double y, double z, float r, float time) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableFog();

        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GL11.glColor4f(0f, 0f, 0f, 1f);
        sphere.setDrawStyle(GLU.GLU_FILL);
        sphere.setNormals(GLU.GLU_NONE);
        sphere.draw(r, 48, 24);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.depthMask(false);

        GlStateManager.pushMatrix();
        GlStateManager.rotate(18f, 1f, 0f, 1f);
        GlStateManager.rotate(time * 3f, 0f, 1f, 0f);
        for (int k = 0; k < 3; k++) {
            float in = r * (1.15f + 0.4f * k), out = in + r * (0.55f - 0.1f * k);
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (int i = 0; i <= 64; i++) {
                double a = i * (Math.PI * 2 / 64);
                float c = (float) Math.cos(a), s = (float) Math.sin(a);
                float br = 0.6f + 0.4f * (float) Math.sin(a * 3 + time * 0.15f + k);
                GL11.glColor4f(0.75f * br, 0.2f * br, 1f, 0.85f);
                GL11.glVertex3f(in * c, 0, in * s);
                GL11.glColor4f(0.4f * br, 0.05f, 0.8f, 0f);
                GL11.glVertex3f(out * c, 0, out * s);
            }
            GL11.glEnd();
        }
        GlStateManager.popMatrix();

        GL11.glLineWidth(2f);
        for (int k = 0; k < 4; k++) {
            float ph = ((time * 0.012f) + k / 4f) % 1f;
            float rad = r * (2f + (1f - ph) * 14f);
            float alpha = ph * 0.55f;
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glColor4f(0.7f, 0.25f, 1f, alpha);
            for (int i = 0; i < 64; i++) {
                double a = i * (Math.PI * 2 / 64);
                GL11.glVertex3f(rad * (float) Math.cos(a), 0, rad * (float) Math.sin(a));
            }
            GL11.glEnd();
        }
        GlStateManager.popMatrix();
    }
}
