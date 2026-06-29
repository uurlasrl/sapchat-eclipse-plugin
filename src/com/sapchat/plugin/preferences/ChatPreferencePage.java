package com.sapchat.plugin.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.sapchat.plugin.Activator;

public class ChatPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public ChatPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Configurazione per l'Assistente AI SAP Chat");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		addField(new ComboFieldEditor(PreferenceConstants.ACTIVE_PROVIDER, "Provider Attivo:",
				new String[][] { 
					{ "Gemini", PreferenceConstants.PROVIDER_GEMINI },
					{ "DeepSeek", PreferenceConstants.PROVIDER_DEEPSEEK } 
				}, getFieldEditorParent()));
		
		addSeparator();
		
		// Gemini Config
		addField(new ComboFieldEditor(PreferenceConstants.GEMINI_MODEL, "Modello Gemini:",
				new String[][] {
					{ "gemini-3.1-pro-preview", "gemini-3.1-pro-preview" },
					{ "gemini-3-flash-preview", "gemini-3-flash-preview" },
					{ "gemini-3.5-flash", "gemini-3.5-flash" },
					{ "gemini-3.1-flash-lite", "gemini-3.1-flash-lite" }
				}, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.GEMINI_ENDPOINT, "Endpoint Gemini:", getFieldEditorParent()));
		StringFieldEditor geminiKeyField = new StringFieldEditor(PreferenceConstants.GEMINI_API_KEY, "API Key Gemini:", getFieldEditorParent()) {
			@Override
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
				getTextControl().setEchoChar('*');
			}
		};
		addField(geminiKeyField);
		
		addSeparator();
		
		// DeepSeek Config
		addField(new StringFieldEditor(PreferenceConstants.DEEPSEEK_MODEL, "Modello DeepSeek:", getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.DEEPSEEK_ENDPOINT, "Endpoint DeepSeek:", getFieldEditorParent()));
		StringFieldEditor deepseekKeyField = new StringFieldEditor(PreferenceConstants.DEEPSEEK_API_KEY, "API Key DeepSeek:", getFieldEditorParent()) {
			@Override
			protected void doFillIntoGrid(Composite parent, int numColumns) {
				super.doFillIntoGrid(parent, numColumns);
				getTextControl().setEchoChar('*');
			}
		};
		addField(deepseekKeyField);
		
		addSeparator();
		
		// History Optimization Config
		addField(new BooleanFieldEditor(
				PreferenceConstants.ENABLE_HISTORY_OPTIMIZATION,
				"Enable History Optimization (Sliding Window)",
				getFieldEditorParent()));
				
		IntegerFieldEditor windowSizeEditor = new IntegerFieldEditor(
				PreferenceConstants.SLIDING_WINDOW_SIZE,
				"Sliding Window Size (Number of messages)",
				getFieldEditorParent());
		windowSizeEditor.setValidRange(1, 1000);
		addField(windowSizeEditor);
	}
	
	private void addSeparator() {
		Label separator = new Label(getFieldEditorParent(), SWT.HORIZONTAL | SWT.SEPARATOR);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		separator.setLayoutData(gd);
	}
}
