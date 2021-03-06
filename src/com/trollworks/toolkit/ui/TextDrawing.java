/*
 * Copyright (c) 1998-2015 by Richard A. Wilkes. All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * version 2.0. If a copy of the MPL was not distributed with this file, You
 * can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This Source Code Form is "Incompatible With Secondary Licenses", as defined
 * by the Mozilla Public License, version 2.0.
 */

package com.trollworks.toolkit.ui;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.swing.SwingConstants;

/** General text drawing utilities. */
public class TextDrawing {
	private static HashMap<Font, TIntIntHashMap>	WIDTH_MAP	= new HashMap<>();
	private static TObjectIntHashMap<Font>			HEIGHT_MAP	= new TObjectIntHashMap<>();
	private static final String						SPACE		= " ";							//$NON-NLS-1$
	private static final String						NEWLINE		= "\n";						//$NON-NLS-1$
	private static final char						ELLIPSIS	= '\u2026';

	/**
	 * @param font The {@link Font} to measure with.
	 * @param ch The character to measure.
	 * @return The width, in pixels.
	 */
	public static final int getWidth(Font font, char ch) {
		return getCharWidth(font, ch, getWidthMap(font));
	}

	private static int getCharWidth(Font font, char ch, TIntIntHashMap map) {
		int width = map.get(ch);
		if (width == 0) {
			width = Fonts.getFontMetrics(font).charWidth(ch);
			if (width == 0) {
				width = 1;
			}
			map.put(ch, width);
		}
		return width;
	}

	/**
	 * @param font The {@link Font} to measure with.
	 * @param text The text to measure.
	 * @return The width, in pixels.
	 */
	public static final int getSimpleWidth(Font font, String text) {
		TIntIntHashMap map = getWidthMap(font);
		int total = 0;
		int count = text.length();
		for (int i = 0; i < count; i++) {
			total += getCharWidth(font, text.charAt(i), map);
		}
		return total;
	}

	private static TIntIntHashMap getWidthMap(Font font) {
		TIntIntHashMap map = WIDTH_MAP.get(font);
		if (map == null) {
			map = new TIntIntHashMap();
			WIDTH_MAP.put(font, map);
			FontMetrics fm = Fonts.getFontMetrics(font);
			for (int i = 32; i < 127; i++) {
				map.put(i, fm.charWidth((char) i));
			}
		}
		return map;
	}

	/**
	 * Draws the text. Embedded return characters may be present.
	 *
	 * @param gc The graphics context.
	 * @param bounds The bounding rectangle to draw the text within.
	 * @param text The text to draw.
	 * @param hAlign The horizontal alignment to use. One of {@link SwingConstants#LEFT},
	 *            {@link SwingConstants#CENTER}, or {@link SwingConstants#RIGHT}.
	 * @param vAlign The vertical alignment to use. One of {@link SwingConstants#LEFT},
	 *            {@link SwingConstants#CENTER}, or {@link SwingConstants#RIGHT}.
	 * @return The bottom of the drawn text.
	 */
	public static final int draw(Graphics gc, Rectangle bounds, String text, int hAlign, int vAlign) {
		int y = bounds.y;
		if (text.length() > 0) {
			ArrayList<String> list = new ArrayList<>();
			Font font = gc.getFont();
			FontMetrics fm = gc.getFontMetrics();
			int ascent = fm.getAscent();
			int descent = fm.getDescent();
			// Don't use fm.getHeight(), as the PC adds too much dead space
			int fHeight = ascent + descent;
			StringTokenizer tokenizer = new StringTokenizer(text, " \n", true); //$NON-NLS-1$
			StringBuilder buffer = new StringBuilder(text.length());
			int textHeight = 0;
			int width;
			while (tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken();
				if (token.equals(NEWLINE)) {
					text = buffer.toString();
					textHeight += fHeight;
					list.add(text);
					buffer.setLength(0);
				} else {
					width = getSimpleWidth(font, buffer.toString() + token);
					if (width > bounds.width && buffer.length() > 0) {
						text = buffer.toString();
						textHeight += fHeight;
						list.add(text);
						buffer.setLength(0);
					}
					buffer.append(token);
				}
			}
			if (buffer.length() > 0) {
				text = buffer.toString();
				textHeight += fHeight;
				list.add(text);
			}
			if (vAlign == SwingConstants.CENTER) {
				y = bounds.y + (bounds.height - (textHeight - descent / 2)) / 2;
			} else if (vAlign == SwingConstants.BOTTOM) {
				y = bounds.y + bounds.height - textHeight;
			}
			for (String piece : list) {
				int x = bounds.x;
				if (hAlign == SwingConstants.CENTER) {
					x = x + (bounds.width - getSimpleWidth(font, piece)) / 2;
				} else if (hAlign == SwingConstants.RIGHT) {
					x = x + bounds.width - (1 + getSimpleWidth(font, piece));
				}
				gc.drawString(piece, x, y + ascent);
				y += fHeight;
			}
		}
		return y;
	}

	/**
	 * Embedded return characters may be present.
	 *
	 * @param font The font the text will be in.
	 * @param text The text to calculate a size for.
	 * @return The preferred size of the text in the specified font.
	 */
	public static final Dimension getPreferredSize(Font font, String text) {
		int width = 0;
		int height = 0;
		int length = text.length();
		if (length > 0) {
			TIntIntHashMap map = getWidthMap(font);
			int fHeight = getFontHeight(font);
			char ch = 0;
			int curWidth = 0;
			for (int i = 0; i < length; i++) {
				ch = text.charAt(i);
				if (ch == '\n') {
					height += fHeight;
					if (curWidth > width) {
						width = curWidth;
					}
					curWidth = 0;
				} else {
					curWidth += getCharWidth(font, ch, map);
				}
			}
			if (ch != '\n') {
				height += fHeight;
			}
			if (curWidth > width) {
				width = curWidth;
			}
			if (width == 0) {
				width = getCharWidth(font, ' ', map);
			}
		}
		return new Dimension(width, height);
	}

	public static final int getFontHeight(Font font) {
		int height = HEIGHT_MAP.get(font);
		if (height == 0) {
			FontMetrics fm = Fonts.getFontMetrics(font);
			// Don't use fm.getHeight(), as the PC adds too much dead space
			height = fm.getAscent() + fm.getDescent();
			HEIGHT_MAP.put(font, height);
		}
		return height;
	}

	/**
	 * Embedded return characters may be present.
	 *
	 * @param font The font the text will be in.
	 * @param text The text to calculate a size for.
	 * @return The preferred height of the text in the specified font.
	 */
	public static final int getPreferredHeight(Font font, String text) {
		int height = 0;
		int length = text.length();
		if (length > 0) {
			int fHeight = getFontHeight(font);
			char ch = 0;
			for (int i = 0; i < length; i++) {
				ch = text.charAt(i);
				if (ch == '\n') {
					height += fHeight;
				}
			}
			if (ch != '\n') {
				height += fHeight;
			}
		}
		return height;
	}

	/**
	 * @param font The font the text will be in.
	 * @param text The text to calculate a size for.
	 * @return The width of the text in the specified font.
	 */
	public static final int getWidth(Font font, String text) {
		StringTokenizer tokenizer = new StringTokenizer(text, NEWLINE, true);
		boolean veryFirst = true;
		boolean first = true;
		int width = 0;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (token.equals(NEWLINE)) {
				if (first && !veryFirst) {
					first = false;
					continue;
				}
				token = SPACE;
			} else {
				first = true;
			}
			veryFirst = false;
			int bWidth = getSimpleWidth(font, token);
			if (width < bWidth) {
				width = bWidth;
			}
		}
		return width;
	}

	/**
	 * If the text doesn't fit in the specified width, it will be shortened and an ellipse ("...")
	 * will be added. This method does not work properly on text with embedded line endings.
	 *
	 * @param font The font to use.
	 * @param text The text to work on.
	 * @param width The maximum pixel width.
	 * @param truncationPolicy One of {@link SwingConstants#LEFT}, {@link SwingConstants#CENTER}, or
	 *            {@link SwingConstants#RIGHT}.
	 * @return The adjusted text.
	 */
	public static final String truncateIfNecessary(Font font, String text, int width, int truncationPolicy) {
		if (getSimpleWidth(font, text) > width) {
			StringBuilder buffer = new StringBuilder(text);
			int max = buffer.length();
			if (truncationPolicy == SwingConstants.LEFT) {
				buffer.insert(0, ELLIPSIS);
				while (max-- > 0 && getSimpleWidth(font, buffer.toString()) > width) {
					buffer.deleteCharAt(1);
				}
			} else if (truncationPolicy == SwingConstants.CENTER) {
				int left = max / 2;
				int right = left + 1;
				boolean leftSide = false;
				buffer.insert(left--, ELLIPSIS);
				while (max-- > 0 && getSimpleWidth(font, buffer.toString()) > width) {
					if (leftSide) {
						buffer.deleteCharAt(left--);
						if (--right < max + 1) {
							leftSide = false;
						}
					} else {
						buffer.deleteCharAt(right);
						if (left >= 0) {
							leftSide = true;
						}
					}
				}
			} else if (truncationPolicy == SwingConstants.RIGHT) {
				buffer.append(ELLIPSIS);
				while (max-- > 0 && getSimpleWidth(font, buffer.toString()) > width) {
					buffer.deleteCharAt(max);
				}
			}
			text = buffer.toString();
		}
		return text;
	}

	/**
	 * @param font The font to use.
	 * @param text The text to wrap.
	 * @param width The maximum pixel width to allow.
	 * @return A new, wrapped version of the text.
	 */
	public static String wrapToPixelWidth(Font font, String text, int width) {
		int[] lineWidth = { 0 };
		StringBuilder buffer = new StringBuilder(text.length() * 2);
		StringBuilder lineBuffer = new StringBuilder(text.length());
		StringTokenizer tokenizer = new StringTokenizer(text + NEWLINE, " \t/\\\n", true); //$NON-NLS-1$
		boolean wrapped = false;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (token.equals(NEWLINE)) {
				if (lineWidth[0] > 0) {
					buffer.append(lineBuffer);
				}
				buffer.append(token);
				wrapped = false;
				lineBuffer.setLength(0);
				lineWidth[0] = 0;
			} else {
				if (!wrapped || lineWidth[0] != 0 || !token.equals(SPACE)) {
					wrapped = processOneTokenForWrapToPixelWidth(token, font, buffer, lineBuffer, width, lineWidth, wrapped);
				}
			}
		}
		if (lineWidth[0] > 0) {
			buffer.append(lineBuffer);
		}
		buffer.setLength(buffer.length() - 1);
		return buffer.toString();
	}

	private static boolean processOneTokenForWrapToPixelWidth(String token, Font font, StringBuilder buffer, StringBuilder lineBuffer, int width, int[] lineWidth, boolean hasBeenWrapped) {
		int tokenWidth = getSimpleWidth(font, token);
		if (lineWidth[0] + tokenWidth <= width) {
			lineBuffer.append(token);
			lineWidth[0] += tokenWidth;
		} else if (lineWidth[0] == 0) {
			// Special-case a line that has not had anything put on it yet
			int count = token.length();
			lineBuffer.append(token.charAt(0));
			for (int i = 1; i < count; i++) {
				lineBuffer.append(token.charAt(i));
				if (getSimpleWidth(font, lineBuffer.toString()) > width) {
					lineBuffer.deleteCharAt(lineBuffer.length() - 1);
					buffer.append(lineBuffer);
					buffer.append(NEWLINE);
					hasBeenWrapped = true;
					lineBuffer.setLength(0);
					lineBuffer.append(token.charAt(i));
				}
			}
			lineWidth[0] = getSimpleWidth(font, lineBuffer.toString());
		} else {
			buffer.append(lineBuffer);
			buffer.append(NEWLINE);
			hasBeenWrapped = true;
			lineBuffer.setLength(0);
			lineWidth[0] = 0;
			if (!token.equals(SPACE)) {
				return processOneTokenForWrapToPixelWidth(token, font, buffer, lineBuffer, width, lineWidth, hasBeenWrapped);
			}
		}
		return hasBeenWrapped;
	}
}
