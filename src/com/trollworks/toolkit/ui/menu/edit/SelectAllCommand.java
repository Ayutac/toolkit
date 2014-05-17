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

package com.trollworks.toolkit.ui.menu.edit;

import com.trollworks.toolkit.annotation.Localize;
import com.trollworks.toolkit.ui.menu.Command;
import com.trollworks.toolkit.utility.Localization;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.text.JTextComponent;

/** Provides the "Select All" command. */
public class SelectAllCommand extends Command {
	@Localize("Select All")
	private static String					SELECT_ALL;

	static {
		Localization.initialize();
	}

	/** The action command this command will issue. */
	public static final String				CMD_SELECT_ALL	= "SelectAll";				//$NON-NLS-1$

	/** The singleton {@link SelectAllCommand}. */
	public static final SelectAllCommand	INSTANCE		= new SelectAllCommand();

	private SelectAllCommand() {
		super(SELECT_ALL, CMD_SELECT_ALL, KeyEvent.VK_A);
	}

	@Override
	public void adjust() {
		boolean isEnabled = false;
		boolean checkWindow = false;
		Component comp = getFocusOwner();
		if (comp != null && comp.isEnabled()) {
			if (comp instanceof JTextComponent) {
				JTextComponent textComp = (JTextComponent) comp;
				String text = textComp.getSelectedText();
				int length = text != null ? text.length() : 0;
				isEnabled = length != textComp.getDocument().getLength();
			} else if (comp instanceof SelectAllCapable) {
				isEnabled = ((SelectAllCapable) comp).canSelectAll();
			} else {
				checkWindow = true;
			}
		} else {
			checkWindow = true;
		}
		if (checkWindow) {
			Window window = getActiveWindow();
			if (window instanceof SelectAllCapable) {
				isEnabled = ((SelectAllCapable) window).canSelectAll();
			}
		}
		setEnabled(isEnabled);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		boolean checkWindow = false;
		Component comp = getFocusOwner();
		if (comp.isEnabled()) {
			if (comp instanceof JTextComponent) {
				((JTextComponent) comp).selectAll();
			} else if (comp instanceof SelectAllCapable) {
				((SelectAllCapable) comp).selectAll();
			} else {
				checkWindow = true;
			}
		} else {
			checkWindow = true;
		}
		if (checkWindow) {
			Window window = getActiveWindow();
			if (window instanceof SelectAllCapable) {
				((SelectAllCapable) window).selectAll();
			}
		}
	}
}
