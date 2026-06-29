package me.zombii.beryllium.client.rendering.opengl.textures.atlas;

import finalforeach.cosmicreach.util.Identifier;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import me.zombii.beryllium.client.rendering.opengl.textures.GLPixmap;
import me.zombii.beryllium.client.rendering.opengl.textures.PixelMap;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GLAtlas extends GLPixmap {

    private final float ratioX;
    private final float ratioY;

    private final Map<String, SubTexture> textureMap;
    private final ObjectList<SubTexture> subTextures;
    private final ObjectList<SubTexture> testList;

    private final Identifier id;

    public GLAtlas(
            Identifier id,
            int width,
            int height
    ) {
        super(width, height);
        this.id = id;
        this.ratioX = 1f / width;
        this.ratioY = 1f / height;

        this.subTextures = new ObjectArrayList<>();
        this.testList = new ObjectArrayList<>();
        this.textureMap = new Object2ObjectArrayMap<>();
    }

    public Identifier getID() {
        return id;
    }

    public GLAtlas(
            int textureID,
            Identifier id,
            int width,
            int height
    ) {
        super(width, height, textureID);
        this.id = id;
        this.ratioX = 1f / width;
        this.ratioY = 1f / height;

        this.subTextures = new ObjectArrayList<>();
        this.testList = new ObjectArrayList<>();
        this.textureMap = new Object2ObjectArrayMap<>();
    }

    public void redrawSubtextures() {
        Arrays.fill(getPixels(), Color.MAGENTA.getRGB()); // clear all pixels and set to an obvious color for error
        for (SubTexture subTexture : subTextures) {
            subTexture.draw();
        }
    }

    public void uploadSubtexturesSeparately() {
        for (SubTexture subTexture : this.subTextures) {
            subTexture.upload();
        }
    }

    public static boolean colliding(Collection<SubTexture> a, SubTexture b) {
        boolean collided = false;
        for (SubTexture c : a) {
//            System.err.println(c.x + ", "+ c.y + " " + b.x + ", "+ b.y);
            collided = colliding(c, b);
            if (collided) break;
        }
        return collided;
    }

    public static boolean colliding(SubTexture a, SubTexture b) {
        int aMinX = a.getX();
        int aMaxX = a.getX() + a.getWidth() - 1;

        int bMinX = b.getX();
        int bMaxX = b.getX() + b.getWidth() - 1;

        int aMinY = a.getY();
        int aMaxY = a.getY() + a.getHeight() - 1;

        int bMinY = b.getY();
        int bMaxY = b.getY() + b.getHeight() - 1;

        return aMinX <= bMaxX &&
                aMaxX >= bMinX &&
                aMinY <= bMaxY &&
                aMaxY >= bMinY;
    }

    private void add(SubTexture texture) {
        boolean isColliding = false;
        for (int y = 0; y < getHeight(); y++) {
            if (y + texture.getHeight() > getHeight())
                break;
            for (int x = 0; x < getWidth(); x++) {
                if (x + texture.getWidth() > getWidth())
                    break;

                texture.setX(x);
                texture.setY(y);
                isColliding = colliding(this.subTextures, texture);
                if (!isColliding) break;
            }
            if (!isColliding) break;
        }
        this.subTextures.add(texture);
    }

    public SubTexture add(String name, PixelMap pixelMap) {
        return add(name, pixelMap, 1, 0.2f);
    }

    public SubTexture add(String name, PixelMap pixelMap, int frameCount, float frameDuration) {
        SubTexture existing = get(name);
        if (existing != null) return existing;

        SubTexture texture = new SubTexture(this, name, pixelMap, 0, 0, frameCount, frameDuration);
        add(texture);
        this.textureMap.put(name, texture);
        return texture;
    }

    public SubTexture get(String name) {
        return this.textureMap.get(name);
    }

    public void reorder() {
        return;
//        this.subTextures.sort((a, b) -> {
//            int areaA = a.width * a.height;
//            int areaB = b.width * b.height;
//            return Integer.compare(areaB, areaA);
//        });
//        this.testList.addAll(this.subTextures);
//        this.subTextures.clear();
//        while (!this.testList.isEmpty()) {
//            SubTexture texture = this.testList.removeFirst();
//            texture.setTBOIndex(this.subTextures.size());
//            add(texture);
//        }
    }

    public float getRatioX() {
        return ratioX;
    }

    public float getRatioY() {
        return ratioY;
    }

    public List<SubTexture> getSubTextures() {
        return ObjectLists.unmodifiable(this.subTextures);
    }

    public int getSubtextureCount() {
        return subTextures.size();
    }

    public static class SubTexture {

        private final GLAtlas atlas;
        private final PixelMap pixmap;
        private int x;
        private int y;
        private final int width;
        private final int height;
        private final String name;
        private int idx = 0;
        private final int frameCount;
        private final float frameDuration;

        public SubTexture(
                GLAtlas atlas, String name, PixelMap pixmap, int x, int y, int frameCount, float frameDuration
        ) {
            this.atlas = atlas;
            this.name = name;
            this.pixmap = pixmap;
            this.width = pixmap.getWidth();
            this.height = pixmap.getHeight();
            this.x = x;
            this.y = y;
            this.frameCount = frameCount;
            this.frameDuration = frameDuration;
        }

        public int getFrameCount() {
            return frameCount;
        }

        public float getFrameDuration() {
            return frameDuration;
        }

        public void setTBOIndex(int idx) {
            this.idx = idx;
        }

        public int getTBOIndex() {
            return idx;
        }

        public void draw() {
            int subX = x;
            int subY = y;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int pixel = pixmap.getPixel(x, y);
                    atlas.setPixel(subX + x, subY + y, pixel);
                }
            }
        }

        public void upload() {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlas.getHandle());
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixmap.getPixels());
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }

        public String getName() {
            return name;
        }

        public GLAtlas getAtlas() {
            return atlas;
        }

        public PixelMap getPixmap() {
            return pixmap;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public float[] getUV(float[] uv) {
            float rx = atlas.getRatioX();
            float ry = atlas.getRatioY();
            uv[0] = x * rx;
            uv[1] = y * ry;
            uv[2] = (x + width) * rx;
            uv[3] = (y + height) * ry;
            return uv;
        }

        public float[] getUV() {
            return getUV(new float[4]);
        }
    }

}
