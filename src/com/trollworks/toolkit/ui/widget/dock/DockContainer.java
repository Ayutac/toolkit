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

package com.trollworks.toolkit.ui.widget.dock;

import com.trollworks.toolkit.ui.UIUtilities;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

/** All {@link Dockable}s are wrapped in a {@link DockContainer} when placed within a {@link Dock}. */
public class DockContainer extends JPanel implements DockLayoutNode, LayoutManager {
	private DockHeader		mHeader;
	private List<Dockable>	mDockables	= new ArrayList<>();
	private int				mCurrent;
	private boolean			mActive;

	/**
	 * Creates a new {@link DockContainer} for the specified {@link Dockable}.
	 *
	 * @param dockable The {@link Dockable} to wrap.
	 */
	public DockContainer(Dockable dockable) {
		setLayout(this);
		setOpaque(true);
		setBackground(Color.WHITE);
		mDockables.add(dockable);
		mHeader = new DockHeader(this);
		add(mHeader);
		add(dockable.getContent());
		setMinimumSize(new Dimension(0, 0));
	}

	/** @return The {@link Dock} this {@link DockContainer} resides in. */
	public Dock getDock() {
		return (Dock) UIUtilities.getAncestorOfType(this, Dock.class);
	}

	/** @return The current list of {@link Dockable}s in this {@link DockContainer}. */
	public List<Dockable> getDockables() {
		return mDockables;
	}

	/** @param dockable The {@link Dockable} to stack into this {@link DockContainer}. */
	public void stack(Dockable dockable) {
		stack(dockable, -1);
	}

	/**
	 * @param dockable The {@link Dockable} to stack into this {@link DockContainer}.
	 * @param index The position within this container to place it. Values out of range will result
	 *            in the {@link Dockable} being placed at the end.
	 */
	public void stack(Dockable dockable, int index) {
		DockContainer dc = dockable.getDockContainer();
		if (dc != null) {
			if (dc == this && mDockables.size() == 1) {
				setCurrentDockable(dockable);
				acquireFocus();
				return;
			}
			dc.close(dockable);
		}
		if (index < 0 || index >= mDockables.size()) {
			mDockables.add(dockable);
		} else {
			mDockables.add(index, dockable);
		}
		add(dockable.getContent());
		mHeader.addTab(dockable, mDockables.indexOf(dockable));
		setCurrentDockable(dockable);
		acquireFocus();
	}

	/** Transfers focus to this container if it doesn't already have the focus. */
	public void acquireFocus() {
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
		Component content = getCurrentDockable().getContent();
		while (focusOwner != null && focusOwner != content) {
			focusOwner = focusOwner.getParent();
		}
		if (focusOwner == null) {
			EventQueue.invokeLater(() -> transferFocus());
		}
	}

	/** @return The {@link DockHeader} for this {@link DockContainer}. */
	public DockHeader getHeader() {
		return mHeader;
	}

	/**
	 * Calls the owning {@link Dock}'s {@link Dock#maximize(DockContainer)} method with this
	 * {@link DockContainer} as the argument.
	 */
	public void maximize() {
		getDock().maximize(this);
	}

	/** Calls the owning {@link Dock}'s {@link Dock#restore()} method. */
	public void restore() {
		getDock().restore();
	}

	/** @return The current tab index. */
	public int getCurrentTabIndex() {
		return mCurrent;
	}

	/** @return The current {@link Dockable}. */
	public Dockable getCurrentDockable() {
		return mCurrent >= 0 && mCurrent < mDockables.size() ? mDockables.get(mCurrent) : null;
	}

	/** @param dockable The {@link Dockable} to make current. */
	public void setCurrentDockable(Dockable dockable) {
		int index = mDockables.indexOf(dockable);
		if (index != -1) {
			mCurrent = index;
			for (Dockable one : mDockables) {
				one.getContent().setVisible(dockable == one);
			}
			mHeader.revalidate();
			repaint();
			acquireFocus();
		}
	}

	@SuppressWarnings("nls")
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		if (getParent() == null) {
			buffer.append("FLOATING ");
		}
		buffer.append("Dock Container [x:");
		buffer.append(getX());
		buffer.append(" y:");
		buffer.append(getY());
		buffer.append(" w:");
		buffer.append(getWidth());
		buffer.append(" h:");
		buffer.append(getHeight());
		int count = mDockables.size();
		for (int i = 0; i < count; i++) {
			buffer.append(' ');
			if (i == mCurrent) {
				buffer.append('*');
			}
			buffer.append('d');
			buffer.append(i);
			buffer.append(':');
			buffer.append(mDockables.get(i).getTitle());
		}
		buffer.append("]");
		return buffer.toString();
	}

	/**
	 * Attempt to close a {@link Dockable} within this {@link DockContainer}. This only has an
	 * affect if the {@link Dockable} is contained by this {@link DockContainer} and implements the
	 * {@link DockCloseable} interface. If the last {@link Dockable} within this
	 * {@link DockContainer} is closed, then the {@link DockContainer} is also closed.
	 */
	public void attemptClose(Dockable dockable) {
		if (dockable instanceof DockCloseable) {
			if (mDockables.contains(dockable)) {
				if (((DockCloseable) dockable).attemptClose()) {
					close(dockable);
				}
			}
		}
	}

	/**
	 * Closes the specified {@link Dockable}. If the last {@link Dockable} within this
	 * {@link DockContainer} is closed, then this {@link DockContainer} is also removed from the
	 * {@link Dock}.
	 */
	public void close(Dockable dockable) {
		int index = mDockables.indexOf(dockable);
		if (index != -1) {
			remove(dockable.getContent());
			mDockables.remove(dockable);
			mHeader.close(dockable);
			if (mDockables.isEmpty()) {
				Dock dock = getDock();
				if (dock != null) {
					restore();
					dock.remove(this);
					dock.revalidate();
					dock.repaint();
				}
			} else {
				if (index > 0) {
					index--;
				}
				setCurrentDockable(mDockables.get(index));
			}
		}
	}

	/**
	 * @return <code>true</code> if this {@link DockContainer} or one of its children has the
	 *         keyboard focus.
	 */
	public boolean isActive() {
		return mActive;
	}

	/** Called by the {@link Dock} to update the active highlight. */
	void updateActiveHighlight() {
		boolean wasActive = mActive;
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
		mActive = focusOwner == this;
		if (!mActive && focusOwner != null) {
			mActive = UIUtilities.getAncestorOfType(focusOwner, DockContainer.class) == this;
		}
		if (mActive != wasActive) {
			mHeader.repaint();
		}
	}

	@Override
	public void addLayoutComponent(String name, Component comp) {
		// Unused
	}

	@Override
	public void removeLayoutComponent(Component comp) {
		// Unused
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		Dimension size = mHeader.getPreferredSize();
		int width = size.width;
		int height = size.height;
		if (!mDockables.isEmpty()) {
			size = getCurrentDockable().getContent().getPreferredSize();
			if (width < size.width) {
				width = size.width;
			}
			height += size.height;
		}
		Insets insets = parent.getInsets();
		return new Dimension(insets.left + width + insets.right, insets.top + height + insets.bottom);
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		Dimension size = mHeader.getMinimumSize();
		int width = size.width;
		int height = size.height;
		Dockable current = getCurrentDockable();
		if (current != null) {
			size = current.getContent().getMinimumSize();
			if (width < size.width) {
				width = size.width;
			}
			height += size.height;
		}
		Insets insets = parent.getInsets();
		return new Dimension(insets.left + width + insets.right, insets.top + height + insets.bottom);
	}

	@Override
	public void layoutContainer(Container parent) {
		Insets insets = parent.getInsets();
		int height = mHeader.getPreferredSize().height;
		int width = parent.getWidth() - (insets.left + insets.right);
		mHeader.setBounds(insets.left, insets.top, width, height);
		Dockable current = getCurrentDockable();
		if (current != null) {
			int remaining = getHeight() - (insets.top + height);
			if (remaining < 0) {
				remaining = 0;
			}
			current.getContent().setBounds(insets.left, insets.top + height, width, remaining);
		}
	}
}
