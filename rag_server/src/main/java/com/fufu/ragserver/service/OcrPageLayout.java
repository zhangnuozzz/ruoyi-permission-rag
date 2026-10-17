package com.fufu.ragserver.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** 对可确认的单栏文本定位行区域；复杂版式沿用整页识别。 */
final class OcrPageLayout implements AutoCloseable
{
    private static final long MAX_PIXELS = 16000000L;
    private static final int MAX_LINES = 60;
    private static final int PADDING = 20;
    private static final int SCALE = 2;
    private final BufferedImage image;
    private final List<Rectangle> lines;

    private OcrPageLayout(BufferedImage image, List<Rectangle> lines)
    {
        this.image = image;
        this.lines = lines;
    }

    static OcrPageLayout detect(byte[] content) throws IOException
    {
        BufferedImage image;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content)))
        {
            if (input == null)
            {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext())
            {
                return null;
            }
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS)
                {
                    return null;
                }
                image = reader.read(0);
            }
            finally
            {
                reader.dispose();
            }
        }
        if (image == null)
        {
            return null;
        }
        List<Rectangle> lines = locateLines(image);
        if (lines == null || lines.size() < 3 || lines.size() > MAX_LINES)
        {
            image.flush();
            return null;
        }
        return new OcrPageLayout(image, lines);
    }

    private static List<Rectangle> locateLines(BufferedImage image)
    {
        List<Rectangle> lines = new ArrayList<>();
        int start = -1;
        int last = -1;
        for (int y = 0; y <= image.getHeight(); y++)
        {
            int ink = 0;
            if (y < image.getHeight())
            {
                for (int x = 0; x < image.getWidth(); x++)
                {
                    if (isInk(image.getRGB(x, y)))
                    {
                        ink++;
                    }
                }
            }
            if (ink >= Math.max(3, image.getWidth() / 1000))
            {
                if (start < 0)
                {
                    start = y;
                }
                last = y;
            }
            else if (start >= 0 && (y - last > 8 || y == image.getHeight()))
            {
                Rectangle line = lineBounds(image, start, last);
                if (line == null)
                {
                    return null;
                }
                lines.add(line);
                if (lines.size() > MAX_LINES)
                {
                    return null;
                }
                start = -1;
            }
        }
        return lines;
    }

    private static Rectangle lineBounds(BufferedImage image, int top, int bottom)
    {
        int height = bottom - top + 1;
        if (height < 28 || height > 200)
        {
            return null;
        }
        int minX = image.getWidth();
        int maxX = -1;
        int emptyColumns = 0;
        int largestGap = 0;
        for (int x = 0; x < image.getWidth(); x++)
        {
            boolean ink = false;
            for (int y = top; y <= bottom; y++)
            {
                if (isInk(image.getRGB(x, y)))
                {
                    ink = true;
                    break;
                }
            }
            if (ink)
            {
                minX = Math.min(minX, x);
                maxX = x;
                largestGap = Math.max(largestGap, emptyColumns);
                emptyColumns = 0;
            }
            else if (maxX >= 0)
            {
                emptyColumns++;
            }
        }
        if (maxX < minX || largestGap > Math.max(height * 6, image.getWidth() / 10))
        {
            return null;
        }
        return new Rectangle(minX, top, maxX - minX + 1, height);
    }

    private static boolean isInk(int color)
    {
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        return red * 299 + green * 587 + blue * 114 < 180000;
    }

    int lineCount()
    {
        return lines.size();
    }

    BufferedImage renderLine(int index)
    {
        Rectangle line = lines.get(index);
        BufferedImage result = new BufferedImage((line.width + PADDING * 2) * SCALE,
                (line.height + PADDING * 2) * SCALE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try
        {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, result.getWidth(), result.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(image, PADDING * SCALE, PADDING * SCALE,
                    (PADDING + line.width) * SCALE, (PADDING + line.height) * SCALE,
                    line.x, line.y, line.x + line.width, line.y + line.height, null);
        }
        finally
        {
            graphics.dispose();
        }
        return result;
    }

    @Override
    public void close()
    {
        image.flush();
    }
}
