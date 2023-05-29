package com.braveripple;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReader;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi;

public class Tiff2Jpeg {

	public static void main(String[] filePaths) throws Exception {
		for (String filePath : filePaths) {
			exec(filePath);
		}
	}

	private static void exec(String filePath) throws Exception {

		System.out.println("File Name : " + filePath);
		final File file = new File(filePath);

		final ImageInputStream is = ImageIO.createImageInputStream(file);
		final TIFFImageReader reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
		reader.setInput(is);
		final int dirs = reader.getNumImages(true);
		System.out.println("Page Number : " + dirs);

		for (int i = 0; i < dirs; i++) {

			final BufferedImage bufferedImage = reader.read(i);

			// 画像を右回転する
//			final AffineTransform transform = new AffineTransform();
//			transform.translate(bufferedImage.getHeight() / 2, bufferedImage.getWidth() / 2);
//			transform.rotate(Math.toRadians(90));
//			transform.translate(-bufferedImage.getWidth() / 2, -bufferedImage.getHeight() / 2);
//
//			final AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
//			final BufferedImage rotatedImage = op.filter(bufferedImage, null);
//
//			// 画像からアルファ情報を削除する（アルファ情報があるとJPEG保存ができないため）
//			final BufferedImage newImage = new BufferedImage(rotatedImage.getWidth(), rotatedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
//			final ColorConvertOp op2 = new ColorConvertOp(null);
//			op2.filter(rotatedImage, newImage);

			final String fileName = file.getName();
			final int lastDotIndex = fileName.lastIndexOf('.');
			final String basename = (lastDotIndex != -1) ? fileName.substring(0, lastDotIndex): fileName;
			final String outputFileName = basename+"_"+i+".jpeg";
			final File outputFile = new File(file.getParent(), outputFileName);
			ImageIO.write(bufferedImage, "jpeg", outputFile);
			System.out.println("Output FileName : "+ outputFile);
		}

	}

}