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

package com.trollworks.toolkit.ui.preferences;

import com.trollworks.toolkit.annotation.Localize;
import com.trollworks.toolkit.ui.UIUtilities;
import com.trollworks.toolkit.ui.layout.ColumnLayout;
import com.trollworks.toolkit.ui.menu.Command;
import com.trollworks.toolkit.ui.menu.StdMenuBar;
import com.trollworks.toolkit.ui.widget.BandedPanel;
import com.trollworks.toolkit.ui.widget.KeyStrokeDisplay;
import com.trollworks.toolkit.ui.widget.WindowUtils;
import com.trollworks.toolkit.utility.Localization;
import com.trollworks.toolkit.utility.Preferences;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/** The menu keys preferences panel. */
public class MenuKeyPreferences extends PreferencePanel implements ActionListener {
	@Localize("Menu Keys")
	@Localize(locale = "ru", value = "Меню Клавиши")
	@Localize(locale = "de", value = "Tastaturkürzel")
	@Localize(locale = "es", value = "Teclas del menú")
	private static String				MENU_KEYS;
	@Localize("Type a keystroke\u2026")
	@Localize(locale = "ru", value = "Ввести сочетание клавиш\u2026")
	@Localize(locale = "de", value = "Tastenkombination drücken\u2026")
	@Localize(locale = "es", value = "Elige una combinación de teclas\u2026")
	private static String				TYPE_KEYSTROKE;
	@Localize("Clear")
	@Localize(locale = "ru", value = "Очистить")
	@Localize(locale = "de", value = "Löschen")
	@Localize(locale = "es", value = "Borrar")
	private static String				CLEAR;
	@Localize("Accept")
	@Localize(locale = "ru", value = "Применить")
	@Localize(locale = "de", value = "Setzen")
	@Localize(locale = "es", value = "Aceptar")
	private static String				ACCEPT;
	@Localize("Reset")
	@Localize(locale = "ru", value = "Сброс")
	@Localize(locale = "de", value = "Standard")
	@Localize(locale = "es", value = "Reiniciar")
	private static String				RESET;

	static {
		Localization.initialize();
	}

	private static final String			NONE	= "NONE";			//$NON-NLS-1$
	private static final String			MODULE	= "MenuKeys";		//$NON-NLS-1$
	private static boolean				LOADED	= false;
	private HashMap<JButton, Command>	mMap	= new HashMap<>();
	private BandedPanel					mPanel;

	/**
	 * Creates a new {@link MenuKeyPreferences}.
	 *
	 * @param owner The owning {@link PreferencesWindow}.
	 */
	public MenuKeyPreferences(PreferencesWindow owner) {
		super(MENU_KEYS, owner);
		setLayout(new BorderLayout());
		mPanel = new BandedPanel(MENU_KEYS);
		mPanel.setLayout(new ColumnLayout(2, 5, 0));
		mPanel.setBorder(new EmptyBorder(2, 5, 2, 5));
		mPanel.setOpaque(true);
		mPanel.setBackground(Color.WHITE);
		for (Command cmd : StdMenuBar.getCommands()) {
			JButton button = new JButton(KeyStrokeDisplay.getKeyStrokeDisplay(KeyStroke.getKeyStroke('Z', InputEvent.META_MASK | InputEvent.ALT_MASK | InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK)));
			UIUtilities.setOnlySize(button, button.getPreferredSize());
			button.setText(getAcceleratorText(cmd));
			mMap.put(button, cmd);
			button.addActionListener(this);
			mPanel.add(button);
			JLabel label = new JLabel(cmd.getTitle());
			mPanel.add(label);
		}
		mPanel.setSize(mPanel.getPreferredSize());
		JScrollPane scroller = new JScrollPane(mPanel);
		Dimension preferredSize = scroller.getPreferredSize();
		if (preferredSize.height > 200) {
			preferredSize.height = 200;
		}
		scroller.setPreferredSize(preferredSize);
		add(scroller);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		JButton button = (JButton) event.getSource();
		Command command = mMap.get(button);
		KeyStrokeDisplay ksd = new KeyStrokeDisplay(command.getAccelerator());
		switch (WindowUtils.showOptionDialog(this, ksd, TYPE_KEYSTROKE, false, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[] { ACCEPT, CLEAR, RESET }, null)) {
			case JOptionPane.CLOSED_OPTION:
			default:
				break;
			case JOptionPane.YES_OPTION: // Accept
				setAccelerator(button, ksd.getKeyStroke());
				break;
			case JOptionPane.NO_OPTION: // Clear
				setAccelerator(button, null);
				break;
			case JOptionPane.CANCEL_OPTION: // Reset
				setAccelerator(button, command.getOriginalAccelerator());
				break;
		}
		mPanel.setSize(mPanel.getPreferredSize());
		adjustResetButton();
	}

	@Override
	public void reset() {
		for (JButton button : mMap.keySet()) {
			setAccelerator(button, mMap.get(button).getOriginalAccelerator());
			button.invalidate();
		}
		mPanel.setSize(mPanel.getPreferredSize());
	}

	@Override
	public boolean isSetToDefaults() {
		for (Command cmd : mMap.values()) {
			if (!cmd.hasOriginalAccelerator()) {
				return false;
			}
		}
		return true;
	}

	private static String getAcceleratorText(Command cmd) {
		return KeyStrokeDisplay.getKeyStrokeDisplay(cmd.getAccelerator());
	}

	private void setAccelerator(JButton button, KeyStroke ks) {
		Command cmd = mMap.get(button);
		cmd.setAccelerator(ks);
		button.setText(getAcceleratorText(cmd));
		button.invalidate();
		Preferences prefs = Preferences.getInstance();
		String key = cmd.getCommand();
		if (cmd.hasOriginalAccelerator()) {
			prefs.removePreference(MODULE, key);
		} else {
			prefs.setValue(MODULE, key, ks != null ? ks.toString() : NONE);
		}
	}

	/** Loads the current menu key settings from the preferences file. */
	public static synchronized void loadFromPreferences() {
		if (!LOADED) {
			Preferences prefs = Preferences.getInstance();
			for (Command cmd : StdMenuBar.getCommands()) {
				String value = prefs.getStringValue(MODULE, cmd.getCommand());
				if (value != null) {
					if (NONE.equals(value)) {
						cmd.setAccelerator(null);
					} else {
						cmd.setAccelerator(KeyStroke.getKeyStroke(value));
					}
				}
			}
			LOADED = true;
		}
	}
}
