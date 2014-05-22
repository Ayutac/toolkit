/*
 * Copyright (c) 1998-2014 by Richard A. Wilkes. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * version 2.0. If a copy of the MPL was not distributed with this file, You
 * can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as defined
 * by the Mozilla Public License, version 2.0.
 */

package com.trollworks.toolkit.ui.image;

import com.trollworks.toolkit.annotation.Localize;
import com.trollworks.toolkit.io.EndianUtils;
import com.trollworks.toolkit.io.StreamUtils;
import com.trollworks.toolkit.utility.Localization;
import com.trollworks.toolkit.utility.Numbers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Provides a set of icons at different resolutions. */
public class IconSet implements Comparator<ToolkitIcon> {
	@Localize("Invalid ICNS")
	private static String				INVALID_ICNS;
	@Localize("Invalid ICO")
	private static String				INVALID_ICO;
	@Localize("Unable to create PNG")
	private static String				UNABLE_TO_CREATE_PNG;

	static {
		Localization.initialize();
	}

	private static final int			TYPE_icns	= 0x69636e73;
	private static final int			TYPE_icp4	= 0x69637034;
	private static final int			TYPE_icp5	= 0x69637035;
	private static final int			TYPE_icp6	= 0x69637036;
	private static final int			TYPE_ic07	= 0x69633037;
	private static final int			TYPE_ic08	= 0x69633038;
	private static final int			TYPE_ic09	= 0x69633039;
	private static final int			TYPE_ic10	= 0x69633130;
	private static final int			TYPE_ic11	= 0x69633131;
	private static final int			TYPE_ic12	= 0x69633132;
	private static final int			TYPE_ic13	= 0x69633133;
	private static final int			TYPE_ic14	= 0x69633134;
	private static Map<String, IconSet>	SETS		= new HashMap<>();
	private static int					SEQUENCE	= 0;
	private String						mName;
	private List<ToolkitIcon>			mIcons;
	private int							mSequence;

	/**
	 * @param name The name of the {@link IconSet}.
	 * @return The {@link IconSet}.
	 */
	public static final IconSet get(String name) {
		return SETS.get(name);
	}

	public static final IconSet loadIcns(String name, URL url) throws IOException {
		try (InputStream in = url.openStream()) {
			return loadIcns(name, in);
		}
	}

	/**
	 * Loads a Mac OS X icon set file (.icns). The format is described here: <a
	 * href="http://en.wikipedia.org/wiki/Apple_Icon_Image"
	 * >http://en.wikipedia.org/wiki/Apple_Icon_Image</a>.
	 *
	 * @param name The name to give this {@link IconSet}. Note that this should be unique, as it
	 *            will replace any existing {@link IconSet} with the same name.
	 * @param in The {@link InputStream} to load the icons from.
	 */
	public static final IconSet loadIcns(String name, InputStream in) throws IOException {
		List<ToolkitIcon> images = new ArrayList<>();
		byte[] header = new byte[8];
		StreamUtils.readFully(in, header);
		if (EndianUtils.readBEInt(header, 0) != TYPE_icns) {
			throw new IOException(INVALID_ICNS);
		}
		int end = EndianUtils.readBEInt(header, 4);
		int pos = header.length;
		while (pos < end) {
			header = new byte[8];
			StreamUtils.readFully(in, header);
			pos += header.length;
			int type = EndianUtils.readBEInt(header, 0);
			int length = EndianUtils.readBEInt(header, 4);
			pos += length;
			switch (type) {
				case TYPE_icp4:
				case TYPE_icp5:
				case TYPE_icp6:
				case TYPE_ic07:
				case TYPE_ic08:
				case TYPE_ic09:
				case TYPE_ic10:
				case TYPE_ic11:
				case TYPE_ic12:
				case TYPE_ic13:
				case TYPE_ic14:
					loadImage(in, length - 8, name, images);
					break;
				default:
					StreamUtils.skipFully(in, length - 8);
					break;
			}
		}
		return images.isEmpty() ? null : new IconSet(name, images);
	}

	private static final void loadImage(InputStream in, int length, String name, List<ToolkitIcon> images) throws IOException {
		byte[] data = new byte[length];
		StreamUtils.readFully(in, data);
		ToolkitIcon img = Images.loadImage(data);
		if (img != null) {
			trackIcon(name, img);
			images.add(img);
		}
	}

	/**
	 * Loads a Windows OS icon set file (.ico). The format is described here: <a
	 * href="http://en.wikipedia.org/wiki/ICO_(file_format)"
	 * >http://en.wikipedia.org/wiki/ICO_(file_format)</a>. Note that only those ICO files that
	 * embed PNG images can be loaded with this method.
	 *
	 * @param name The name to give this {@link IconSet}. Note that this should be unique, as it
	 *            will replace any existing {@link IconSet} with the same name.
	 * @param in The {@link InputStream} to load the icons from.
	 */
	public static final IconSet loadIco(String name, InputStream in) throws IOException {
		List<ToolkitIcon> images = new ArrayList<>();
		byte[] header = new byte[6];
		StreamUtils.readFully(in, header);
		if (EndianUtils.readLEUnsignedShort(header, 0) != 0 || EndianUtils.readLEUnsignedShort(header, 2) != 1) {
			throw new IOException(INVALID_ICO);
		}
		int count = EndianUtils.readLEUnsignedShort(header, 4);
		byte[] data = new byte[count * 16];
		StreamUtils.readFully(in, data);
		ico[] toc = new ico[count];
		for (int i = 0; i < count; i++) {
			toc[i].length = EndianUtils.readLEInt(data, i * 16 + 8);
			toc[i].offset = EndianUtils.readLEInt(data, i * 16 + 12);
		}
		Arrays.sort(toc);
		int pos = header.length + data.length;
		for (int i = 0; i < count; i++) {
			if (pos != toc[i].offset) {
				StreamUtils.skipFully(in, toc[i].offset - pos);
				pos = toc[i].offset;
			}
			loadImage(in, toc[i].length, name, images);
			pos += toc[i].length;
		}
		return images.isEmpty() ? null : new IconSet(name, images);
	}

	private static class ico implements Comparable<ico> {
		int	length;
		int	offset;

		@Override
		public int compareTo(ico other) {
			return Numbers.compare(offset, other.offset);
		}
	}

	private static final void trackIcon(String name, ToolkitIcon icon) {
		Images.add("is:" + name + "_" + icon.getWidth() + "x" + icon.getHeight(), icon); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Creates a new {@link IconSet}.
	 *
	 * @param name The name of this {@link IconSet}. This can be used to retrieve the
	 *            {@link IconSet} later, via a call to {@link #get(String)}.
	 * @param images The images that belong in this {@link IconSet}.
	 */
	public IconSet(String name, List<ToolkitIcon> images) {
		mName = name;
		updateSequence();
		mIcons = new ArrayList<>(images);
		Collections.sort(mIcons, this);
		SETS.put(name, this);
	}

	@Override
	public int compare(ToolkitIcon o1, ToolkitIcon o2) {
		int result = Numbers.compare(o2.getWidth(), o1.getWidth());
		if (result == 0) {
			result = Numbers.compare(o2.getHeight(), o1.getHeight());
			if (result == 0) {
				result = Numbers.compare(o2.hashCode(), o1.hashCode());
			}
		}
		return result;
	}

	/** @return The name of this {@link IconSet}. */
	public String getName() {
		return mName;
	}

	/**
	 * @param size The width and height of the icon.
	 * @return <code>true</code> if the icon exists.
	 */
	public boolean hasIcon(int size) {
		return hasIcon(size, size);
	}

	/**
	 * @param width The width of the icon.
	 * @param height The height of the icon.
	 * @return <code>true</code> if the icon exists.
	 */
	public boolean hasIcon(int width, int height) {
		for (ToolkitIcon icon : mIcons) {
			if (width == icon.getWidth() && height == icon.getHeight()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param size The width and height of the icon.
	 * @return An icon from the set, or <code>null</code> if the desired dimensions cannot be found.
	 */
	public ToolkitIcon getIconNoCreate(int size) {
		return getIconNoCreate(size, size);
	}

	/**
	 * @param width The width of the icon.
	 * @param height The height of the icon.
	 * @return An icon from the set, or <code>null</code> if the desired dimensions cannot be found.
	 */
	public ToolkitIcon getIconNoCreate(int width, int height) {
		for (ToolkitIcon icon : mIcons) {
			if (width == icon.getWidth() && height == icon.getHeight()) {
				return icon;
			}
		}
		return null;
	}

	/**
	 * @param size The width and height of the icon.
	 * @return An icon from the set. If an exact match cannot be found, one of the existing icons
	 *         will be scaled to the desired size.
	 */
	public ToolkitIcon getIcon(int size) {
		return getIcon(size, size);
	}

	/**
	 * @param width The width of the icon.
	 * @param height The height of the icon.
	 * @return An icon from the set. If an exact match cannot be found, one of the existing icons
	 *         will be scaled to the desired size.
	 */
	public ToolkitIcon getIcon(int width, int height) {
		ToolkitIcon match = getIconNoCreate(width, height);
		if (match == null) {
			ToolkitIcon inverseMatch = null;
			int best = Integer.MAX_VALUE;
			int inverseBest = Integer.MIN_VALUE;
			for (ToolkitIcon icon : mIcons) {
				int iconWidth = icon.getWidth();
				int iconHeight = icon.getHeight();
				int heuristic = (iconWidth - width) * (iconHeight - height);
				if (heuristic > 0) {
					if (heuristic < best) {
						best = heuristic;
						match = icon;
					}
				} else if (match == null && heuristic > inverseBest) {
					inverseBest = heuristic;
					inverseMatch = icon;
				}
			}
			if (match == null) {
				match = inverseMatch;
			}
			match = Images.scale(match, width, height);
			trackIcon(mName, match);
			mIcons.add(match);
			Collections.sort(mIcons, this);
		}
		return match;
	}

	/** @return A list containing all of the icons within this {@link IconSet}. */
	public List<ToolkitIcon> toList() {
		return new ArrayList<>(mIcons);
	}

	/**
	 * @return The current sequence number of this {@link IconSet}. This can be used to determine if
	 *         the {@link IconSet} is the same as the last time you used it. These are unique across
	 *         all {@link IconSet}s.
	 */
	public synchronized int getSequence() {
		return mSequence;
	}

	private synchronized void updateSequence() {
		mSequence = ++SEQUENCE;
	}

	/** @param out The stream to write an ICNS file with this {@link IconSet}s contents to. */
	public void saveAsIcns(OutputStream out) throws IOException {
		List<byte[]> imageData = new ArrayList<>();
		List<Integer> imageType = new ArrayList<>();
		int size = 8;
		for (ToolkitIcon icon : mIcons) {
			int width = icon.getWidth();
			// We currently only write out square icons
			if (width == icon.getHeight()) {
				int type;
				// We currently only write out certain sizes
				switch (width) {
					case 1024:
						type = TYPE_ic10;
						break;
					case 512:
						type = TYPE_ic09;
						break;
					case 256:
						type = TYPE_ic08;
						break;
					case 128:
						type = TYPE_ic07;
						break;
					case 64:
						type = TYPE_icp6;
						break;
					case 32:
						type = TYPE_icp5;
						break;
					case 16:
						type = TYPE_icp4;
						break;
					default:
						type = 0;
						break;
				}
				if (type != 0) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					if (!Images.writePNG(baos, icon, 72)) {
						throw new IOException(UNABLE_TO_CREATE_PNG);
					}
					byte[] bytes = baos.toByteArray();
					imageData.add(bytes);
					imageType.add(Integer.valueOf(type));
					size += 8 + bytes.length;
				}
			}
		}
		byte[] buffer = new byte[8];
		EndianUtils.writeBEInt(TYPE_icns, buffer, 0);
		EndianUtils.writeBEInt(size, buffer, 4);
		out.write(buffer);
		int count = imageData.size();
		for (int i = 0; i < count; i++) {
			byte[] data = imageData.get(i);
			EndianUtils.writeBEInt(imageType.get(i).intValue(), buffer, 0);
			EndianUtils.writeBEInt(buffer.length + data.length, buffer, 4);
			out.write(buffer);
			out.write(data);
		}
	}
}
