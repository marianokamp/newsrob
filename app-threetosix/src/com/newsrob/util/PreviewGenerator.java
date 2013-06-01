package com.newsrob.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;

import com.newsrob.PL;

public class PreviewGenerator {

    private File assetsDir;

    private int targetHeight;
    private int targetWidth;

    private int roundedCornerRadiusPx;

    private Context context;

    private static final Pattern invalidExtensionPattern = Pattern.compile(".+[.]htm.nr$", Pattern.CASE_INSENSITIVE);

    private static final Filter filter = new Filter();
    private static final FileLengthComparator comparator = new FileLengthComparator();

    public PreviewGenerator(Context ctx, File assetsDir, int targetWidth, int targetHeight, int roundedCornerRadiusPx) {
        this.assetsDir = assetsDir;

        this.targetHeight = targetHeight;
        this.targetWidth = targetWidth;

        this.roundedCornerRadiusPx = roundedCornerRadiusPx;

        this.context = ctx;

    }

    /**
     * Can only be called to downscale images, never upscale
     */
    public static Rect getScaledRectangle(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {

        final double widthFactor = srcWidth * 1.0f / dstWidth;
        final double heightFactor = srcHeight * 1.0f / dstHeight;

        // Prefer the dimension that needs to be
        // scaled less ... and consequently later(!) on
        // cut off the excess size of the other dimension
        final double scaleFactor = Math.min(widthFactor, heightFactor);

        // if (scaleFactor < 1.0f)
        // throw new
        // RuntimeException("getDestinationSizeAndOffset can only be used to downscale dimensions, not up.");

        final int excessWidth = (int) ((srcWidth - (dstWidth * scaleFactor)));
        final int excessHeight = (int) ((srcHeight - (dstHeight * scaleFactor)));

        final Rect r = new Rect();

        r.left = excessWidth / 2;
        r.top = excessHeight / 2;
        r.bottom = srcHeight - r.top;
        r.right = srcWidth - r.left;

        return r;
    }

    /**
     * @return true when a preview was generated.
     */
    public boolean generatePreview() {
        Timing t = new Timing("Generate preview.", context);
        try {
            File image = findBiggestImageFile();
            if (image == null) {
                PL.log("PreviewGenerator: No suitable biggest image file found.", context);
                return false;
            }
            return doGeneratePreview(image);
        } finally {
            t.stop();
        }

    }

    protected List<File> findAllImageFiles() {
        return Arrays.asList(assetsDir.listFiles(filter));
    }

    protected File findBiggestImageFile() {
        List<File> allImages = findAllImageFiles();
        if (allImages.isEmpty())
            return null;

        Collections.sort(allImages, comparator);

        // copy only images that are bigger than x bytes
        List<File> allImages2 = new ArrayList<File>(allImages.size());

        for (File file : allImages) {
            if (file.length() > getMinSizeInBytes()) {
                int height = -1;
                int width = -1;

                synchronized (PreviewGenerator.class) {
                    // using the class based synchronization to prevent to much
                    // memory usage

                    Bitmap bm = null;
                    try {
                        bm = BitmapFactory.decodeFile(file.getAbsolutePath(), null);
                    } catch (OutOfMemoryError ooe) {
                        PL.log("PreviewGenerator: " + file.getAbsolutePath()
                                + " was too big (OOM) in findingBiggestImageFile. Skipping it. " + getMemoryStatus(),
                                context);
                    }
                    if (bm == null)
                        continue;
                    width = bm.getWidth();
                    height = bm.getHeight();
                    bm.recycle();
                }
                if (width >= targetWidth / 2 && height >= targetHeight / 2)
                    allImages2.add(file);
                else
                    PL.log("PreviewGenerator: Skipped " + file + " because it was only " + width + " x " + height
                            + ". Looking for at least " + targetWidth / 2 + " x " + targetHeight / 2, context);
            } else
                PL.log("PreviewGenerator: Skipped " + file + " because it is too small.", context);
        }

        if (allImages2.isEmpty())
            return null;

        return allImages2.get(allImages2.size() - 1);
    }

    private String getMemoryStatus() {
        Runtime rt = Runtime.getRuntime();
        return String.format("-- Memory free: %4.2fMB total: %4.2fMB max: %4.2fMB\n", rt.freeMemory() / 1024 / 1024.0,
                rt.totalMemory() / 1024 / 1024.0, rt.maxMemory() / 1024 / 1024.0);
    }

    protected static int getMinSizeInBytes() {
        return 2048;
    }

    protected static int getMaxSizeInBytes() {
        return 2048 * 1024;
    }

    /**
     * @return true when a preview was generated.
     */
    protected boolean doGeneratePreview(File fromImage) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        // options.inScaled = true;

        // options.outWidth = this.targetWidth;
        // options.outHeight = this.targetHeight;

        int fileSize = (int) fromImage.length();
        int sampleFactor = 1;

        while (fileSize / sampleFactor > 500 * 1024)
            sampleFactor *= 2;

        PL.log("Sampling " + fromImage.getAbsolutePath() + " size=" + (fromImage.length() / 1024) + " scaleFactor="
                + sampleFactor + " Resulting size= " + (fromImage.length() / 1024 / sampleFactor), context);
        options.inSampleSize = sampleFactor;

        File outputFile = new File(assetsDir, "preview.pngnr");
        try {
            synchronized (PreviewGenerator.class) {
                // using the class based synchronization to prevent to much
                // memory usage

                FileOutputStream fos = new FileOutputStream(outputFile);

                Bitmap inBm = BitmapFactory.decodeFile(fromImage.getAbsolutePath(), options);

                Rect r = getScaledRectangle(inBm.getWidth(), inBm.getHeight(), this.targetWidth, this.targetHeight);

                Bitmap cutoffScaledBitmap = Bitmap
                        .createBitmap(inBm, r.left, r.top, r.right - r.left, r.bottom - r.top);

                Bitmap outBm = Bitmap.createScaledBitmap(cutoffScaledBitmap, this.targetWidth, targetHeight, true);
                cutoffScaledBitmap.recycle();
                cutoffScaledBitmap = null;

                inBm.recycle();
                inBm = null;

                Bitmap roundedBm = getRoundedCornerBitmap(outBm);
                outBm.recycle();
                outBm = null;

                roundedBm.compress(CompressFormat.PNG, 100, fos);

                fos.close();

                // inBm.recycle();
                // outBm.recycle();
                // cutoffScaledBitmap.recycle();
                roundedBm.recycle();
            }
        }

        catch (IOException e) {
            e.printStackTrace();
            if (outputFile != null)
                PL.log("Deleting output file: " + outputFile.delete(), context);
            return false;
        } catch (OutOfMemoryError ooe) {
            ooe.printStackTrace();
            PL.log(
                    "PreviewGenerator: Bitmap was too big (OOM) in generatingPreview. Skipping it. "
                            + getMemoryStatus(), context);
            if (outputFile != null)
                PL.log("Deleting output file: " + outputFile.delete(), context);

            return false;
        }

        return true;
    }

    private Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawARGB(0, 0, 0, 0);

        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        final int color = 0xffffffff;
        final Paint paint = new Paint();

        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundedCornerRadiusPx, roundedCornerRadiusPx, paint);

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));

        canvas.drawBitmap(bitmap, 0, 0, paint);

        return output;
    }

    static class Filter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String filename) {
            boolean validExtension = !invalidExtensionPattern.matcher(filename).find();// true
            // ||
            // validExtensionsPattern.matcher(filename).find();
            if (!validExtension)
                return false;

            long sizeBytes = new File(dir, filename).length();
            boolean rightSize = (sizeBytes < getMaxSizeInBytes()) && sizeBytes > getMinSizeInBytes(); // Only
            // check
            // files
            // smaller
            // than
            // 150
            // KB.
            if (!rightSize)
                return false;
            boolean fileMightBeAd = !filename.toLowerCase().contains("_ad");
            return fileMightBeAd;
        }
    }

    /**
     * 
     * Bigger files are bigger than smaller files.
     * 
     */
    static class FileLengthComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            return (int) (f1.length() - f2.length());
        }

    }
}
