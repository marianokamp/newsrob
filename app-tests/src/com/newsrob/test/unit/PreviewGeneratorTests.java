package com.newsrob.test.unit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.test.InstrumentationTestCase;

import com.newsrob.util.PreviewGenerator;

public class PreviewGeneratorTests extends InstrumentationTestCase {

	private final String TEST_DIR_BASE = "/sdcard/test/preview_generator_tests";
	private String TEST_DIR;
	private File biggestFoundImage;
	private Integer numberOfRelevantImages;
	private File testDir;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		TEST_DIR = TEST_DIR_BASE + System.currentTimeMillis();
		testDir = new File(TEST_DIR);
		testDir.mkdirs();

		// clear when used before
		for (File f : testDir.listFiles())
			f.delete();

		// clear?
		assertEquals(0, testDir.list().length);

		// 5 valid, 3 invalid files
		createFile(testDir, "big.jpgnr", 12, 8); // yes - biggest

		// createFile(testDir, "0.jpegnr", 1); // no - too small
		createFile(testDir, "1.jpegnr", 3, 3); // yes - jpeg supported
		createFile(testDir, "2.jPegnr", 4, 4); // yes - mixed case
		createFile(testDir, "3.jpegxnr", 5, 5); // no - only contains jpeg
		createFile(testDir, "4.jpgnr", 6, 6); // yes - no 'e'
		createFile(testDir, "5.gifnr", 7, 7); // no - no gifs
		createFile(testDir, "6.pngnr", 8, 8); // yes - png supported
		createFile(testDir, "7.png.extnr", 9, 9); // no - wrong extension

	}

	private void createFile(File testDir, String name, int width, int height)
			throws IOException {

		int LENGTH = 100;
		Bitmap outBm = Bitmap.createBitmap(width * LENGTH, height * LENGTH,
				Config.ARGB_8888);

		File outputFile = new File(testDir.getAbsolutePath() + File.separator
				+ name);

		FileOutputStream fos = new FileOutputStream(outputFile);

		final int color = 0xffff0000;
		final Paint paint = new Paint();

		paint.setAntiAlias(true);
		paint.setColor(color);

		Canvas c = new Canvas(outBm);
		c.drawARGB(255, 0, 0, 0);

		for (int i = 0; i < height * 2; i++) {
			// horizontal
			c.drawLine(0.0f, i * LENGTH / 2, LENGTH * width, i * LENGTH / 2,
					paint);

		}

		for (int i = 0; i < width * 2; i++) {
			// vertical
			c.drawLine(i * LENGTH / 2, 0f, i * LENGTH / 2, LENGTH * height,
					paint);
		}

		outBm.compress(CompressFormat.PNG, 100, fos);
		fos.close();
		outBm.recycle();

		// canvas.drawARGB(0, 0, 0, 0);

	}

	public void testFindBiggestRelevantImage() throws Exception {
		new PreviewGenerator(getInstrumentation().getContext(), new File(
				TEST_DIR), 60, 60, 3) {
			@Override
			protected File findBiggestImageFile() {
				biggestFoundImage = super.findBiggestImageFile();
				return biggestFoundImage;
			}
		}.generatePreview();
		assertNotNull(biggestFoundImage);
		assertEquals("big.jpgnr", biggestFoundImage.getName());
	}

	/** meanwhile all extensions are allowed */
	public void oldtestFindOnlyRelevantImages() {
		new PreviewGenerator(getInstrumentation().getContext(), new File(
				TEST_DIR), 60, 60, 3) {
			protected List<File> findAllImageFiles() {
				List<File> images = super.findAllImageFiles();
				numberOfRelevantImages = images.size();
				return images;
			}
		}.generatePreview();
		assertNotNull(numberOfRelevantImages);
		assertEquals(5, numberOfRelevantImages.intValue());
	}

	public void testGenerationSuccessful() {
		assertFalse(new File(TEST_DIR, "preview.pngnr").exists());
		assertTrue(new PreviewGenerator(getInstrumentation().getContext(),
				new File(TEST_DIR), 30, 30, 3).generatePreview());
		assertTrue(new File(TEST_DIR, "preview.pngnr").exists());

	}

	public void testPreviewIsOffTheRightSize() {
		assertTrue(new PreviewGenerator(getInstrumentation().getContext(),
				new File(TEST_DIR), 60, 60, 3).generatePreview());
		Bitmap bm = BitmapFactory
				.decodeFile(new File(TEST_DIR, "preview.pngnr")
						.getAbsolutePath());
		assertEquals(60, bm.getWidth());
		assertEquals(60, bm.getHeight());

		new File(TEST_DIR, "preview.pngnr").delete();

		assertTrue(new PreviewGenerator(getInstrumentation().getContext(),
				new File(TEST_DIR), 12, 6, 3).generatePreview());
		bm = BitmapFactory.decodeFile(new File(TEST_DIR, "preview.pngnr")
				.getAbsolutePath());
		assertEquals(12, bm.getWidth());
		assertEquals(6, bm.getHeight());

	}

}

class PG extends PreviewGenerator {

	public PG(Context ctx, File assetsDir, int targetWidth, int targetHeight,
			int roundedCornerRadiusPx) {
		super(ctx, assetsDir, targetWidth, targetHeight, roundedCornerRadiusPx);
	}

	protected static int getMinSizeInBytes() {
		return 100;
	}
}
