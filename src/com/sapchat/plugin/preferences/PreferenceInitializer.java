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
		store.setDefault(PreferenceConstants.GEMINI_CURRENCY, "EUR");
		store.setDefault(PreferenceConstants.GEMINI_INPUT_COST, "0.0");
		store.setDefault(PreferenceConstants.GEMINI_OUTPUT_COST, "0.0");
		store.setDefault(PreferenceConstants.GEMINI_CACHE_COST, "0.0");
		
		store.setDefault(PreferenceConstants.DEEPSEEK_ENDPOINT, "https://api.deepseek.com/chat/completions");
		store.setDefault(PreferenceConstants.DEEPSEEK_MODEL, "deepseek-chat");
		store.setDefault(PreferenceConstants.DEEPSEEK_CURRENCY, "USD");
		store.setDefault(PreferenceConstants.DEEPSEEK_INPUT_COST, "0.0");
		store.setDefault(PreferenceConstants.DEEPSEEK_OUTPUT_COST, "0.0");
		store.setDefault(PreferenceConstants.DEEPSEEK_CACHE_COST, "0.0");
		
		store.setDefault(PreferenceConstants.ENABLE_HISTORY_OPTIMIZATION, false);
		store.setDefault(PreferenceConstants.SLIDING_WINDOW_SIZE, 6);
		
		store.setDefault(PreferenceConstants.ENABLE_DEBUG_OUTPUT, false);
	}
}
