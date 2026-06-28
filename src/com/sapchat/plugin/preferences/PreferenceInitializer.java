package com.sapchat.plugin.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.sapchat.plugin.Activator;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		store.setDefault(PreferenceConstants.ACTIVE_PROVIDER, PreferenceConstants.PROVIDER_GEMINI);
		
		store.setDefault(PreferenceConstants.GEMINI_ENDPOINT, "https://generativelanguage.googleapis.com/v1beta/interactions");
		store.setDefault(PreferenceConstants.GEMINI_MODEL, "gemini-3.1-pro");
		
		store.setDefault(PreferenceConstants.DEEPSEEK_ENDPOINT, "https://api.deepseek.com/chat/completions");
		store.setDefault(PreferenceConstants.DEEPSEEK_MODEL, "deepseek-chat");
	}
}
