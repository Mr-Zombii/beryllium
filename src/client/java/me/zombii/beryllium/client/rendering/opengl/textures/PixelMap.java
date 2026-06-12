package me.zombii.beryllium.client.rendering.opengl.textures;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PixelMap {

    private final int width, height;
    private final int[] pixels;

    public PixelMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }

    private int toIdx(int x, int y) {
        if (x >= width || y >= height) {
            throw new IndexOutOfBoundsException("width=" + width + ", height=" + height + " | x=" + x + ", y=" + y);
        }
        return y * width + x;
    }

    public void setPixel(int x, int y, int color) {
        pixels[toIdx(x, y)] = color;
    }

    public int getPixel(int x, int y) {
        return pixels[toIdx(x, y)];
    }

    public int[] getPixels() {
        return pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static PixelMap fromBufferedImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        PixelMap pixelMap = new PixelMap(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = bufferedImage.getRGB(x, y);
                pixelMap.setPixel(x, y, color);
            }
        }
        return pixelMap;
    }

    public BufferedImage toBufferedImage() {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = pixels[toIdx(x, y)];
                bufferedImage.setRGB(x, y, color);
            }
        }
        return bufferedImage;
    }

    public void toFile(File file) throws IOException {
        ImageIO.write(toBufferedImage(), "png", file);
    }

}
