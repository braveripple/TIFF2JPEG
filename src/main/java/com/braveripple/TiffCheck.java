package com.braveripple;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageMetadata;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReader;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageReaderSpi;

public class TiffCheck {

	/**
	 * タグ番号(カンマ区切り)
	 */
	@Option(name = "-t", aliases = { "--tag" }, metaVar = "tags", required = true, usage = "tiff properties tag number")
	private static String tags;

	/**
	 * ファイルパス
	 */
	@Argument(index = 0, metaVar = "filePaths", required = true, usage = "File Paths ...")
	private static String[] filePaths;

	/**
	 * 区切り文字
	 */
	@Option(name = "-d", aliases = { "--delimiter" }, metaVar = "delimiter", required = false, usage = "delimiter")
	private static String delimiter = ":";

	/**
	 * ファイル名を表示しない
	 */
	@Option(name = "-h", aliases = {
			"--no-filename" }, metaVar = "noFileName", required = false, usage = "suppress the file name prefix on output")
	private static boolean noFilename;

	/**
	 * ファイル名を表示する
	 */
	@Option(name = "-H", aliases = {
			"--with-filename" }, metaVar = "withFileName", required = false, usage = "print file name with output lines")
	private static boolean withFilename;

	/**
	 * ラベルを付与する
	 */
	@Option(name = "--label", metaVar = "label", required = false, usage = "use LABEL as the standard input file name prefix")
	private static String label;

	private static boolean outputFilename = false;

	private static List<Integer> tagList = new ArrayList<>();

	private static Map<Integer, String> tiffTagMap;
	
	static {
		tiffTagMap = new HashMap<>();
		tiffTagMap.put(256, "ImageWidth");
		tiffTagMap.put(257, "ImageHeight");
		tiffTagMap.put(259, "Compression");
		tiffTagMap.put(269, "DocumentName");
		tiffTagMap.put(270, "ImageDescription");
		tiffTagMap.put(274, "Orientation");		
	}
	
	private static void printUsage(CmdLineParser parser) {
		System.out.println("usage:");
		parser.printSingleLineUsage(System.out);
		System.out.println();
		parser.printUsage(System.out);		
	}
	
	public static void main(String[] args) throws Exception {

		final TiffCheck main = new TiffCheck();
		final CmdLineParser parser = new CmdLineParser(main);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			printUsage(parser);
			return;
		}

		if (noFilename && withFilename) {
			// -hと-Hが両方指定されていたら出力を優先
			outputFilename = true;
		} else if (!noFilename && !withFilename) {
			// -hと-Hが両方未指定ならファイルパスの数で出力の有無を決定
			if (filePaths.length > 1) {
				outputFilename = true;
			} else {
				outputFilename = false;
			}
		} else if (withFilename) {
			outputFilename = true;
		} else if (noFilename) {
			outputFilename = false;
		}

		// タグ番号をリストに格納
		final String[] tagArray = tags.split(",");
		for (final String tag : tagArray) {
			if (tag.matches("^\\d+$")) {
				tagList.add(Integer.parseInt(tag));
			}
        }
		
		
		for (String filePath : filePaths) {
			main.execute(filePath);
		}
	}

	private void execute(String filePath) throws Exception {

		// TIFFファイルが存在し、ファイルであることをチェック
		final File file = new File(filePath);
		if (!file.exists()) {
			System.err.println("TiffCheck: " + filePath + ": No such file");
			return;
		}
		if (file.isDirectory()) {
			System.err.println("TiffCheck: " + filePath + ": Is a directory");
			return;
		}
		
		// TIFFファイルを読み込む
		final ImageInputStream is;
		final TIFFImageReader reader;
		final int dirs;
		
		try {
			is = ImageIO.createImageInputStream(file);
			reader = (TIFFImageReader) new TIFFImageReaderSpi().createReaderInstance();
			reader.setInput(is);
			dirs = reader.getNumImages(true);
		} catch (IOException e) {
			System.err.println("TiffCheck: " + filePath + ": Failed to read TIFF file");
			return;
		}
		
		// TIFFファイルのプロパティを取得する
		for (int i = 0; i < dirs; i++) {
			
			// プロパティを取得
			for (final int tag : tagList) {
				
				final TIFFImageMetadata metadata = (TIFFImageMetadata)reader.getImageMetadata(i);
				
				final Entry e = metadata.getTIFFField(tag);
				
				final String fieldName;
				final String fieldValue;
				if (tiffTagMap.containsKey(tag)) {
					fieldName = tiffTagMap.get(tag);
					if (e == null || e.getFieldName() == null || "null".equals(e.getFieldName())) {
						fieldValue = "(null)";
					} else {
						fieldValue = e.getValueAsString();
					}
				} else {
					if (e == null) {
						System.err.println("TiffCheck: " + filePath +": " + (i+1) + ": " + tag + ": No such tag");
						continue;
					}
					if (e.getFieldName() == null || "null".equals(e.getFieldName())) {
						System.err.println("TiffCheck: " + filePath +": " + (i+1) + ": " + tag + ": Field name is null");
						continue;					
					}
					fieldName = e.getFieldName();
					fieldValue = e.getValueAsString();
				}
				
				final StringBuilder sb = new StringBuilder();
				
				// ファイル名またはラベルを出力
				if (outputFilename) {
					if (label != null) {
						sb.append(label);
						sb.append(delimiter);
					} else {
						sb.append(filePath);
						sb.append(delimiter);
					}
				}
				// ページ番号を出力
				sb.append(i+1);
				sb.append(delimiter);

				// タグ番号を出力
				sb.append(tag);
				sb.append(delimiter);
				
				// タグ名称を出力
				sb.append(fieldName);
				sb.append(delimiter);
				
				// タグの値を出力
				sb.append(fieldValue);

				System.out.println(sb.toString());

			}
		}

	}

}