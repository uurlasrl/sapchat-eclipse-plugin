package com.sapchat.plugin.views;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.IDocument;

import com.sapchat.plugin.Activator;
import com.sapchat.plugin.preferences.PreferenceConstants;

/**
 * La vista principale del plugin SAP Chat AI.
 * Gestisce l'interfaccia utente della chat, la comunicazione HTTP con i provider (Gemini/DeepSeek)
 * e l'interazione con l'editor di Eclipse.
 */
public class ChatView extends ViewPart {

	public static final String ID = "com.sapchat.plugin.views.ChatView";

	private Text chatHistory;
	private Text chatInput;
	private Button sendButton;
	private Combo actionsCombo;
	private Combo historyCombo;
	
	private List<ChatMessage> currentConversation = new ArrayList<>();

//	private List<ChatMessage> currentConversation = new ArrayList<>();

	/**
	 * Costruttore predefinito.
	 */
	public ChatView() {
	}

	/**
	 * Crea e inizializza i controlli visivi della vista.
	 * 
	 * @param parent il composite genitore in cui verranno inseriti i controlli
	 */
	@Override
	public void createPartControl(Composite parent) {
		GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(parent);

		Composite topBar = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(topBar);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(topBar);
		
		historyCombo = new Combo(topBar, SWT.READ_ONLY | SWT.DROP_DOWN);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(historyCombo);
		historyCombo.add("Nuova Conversazione...");
		historyCombo.select(0);
		refreshHistoryCombo();
		
		historyCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = historyCombo.getSelectionIndex();
				if (index > 0) {
					String filename = historyCombo.getItem(index);
					loadConversation(filename);
				} else {
					clearChat();
				}
			}
		});

		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(sashForm);

		chatHistory = new Text(sashForm, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		chatHistory.setText("Benvenuto! Come posso aiutarti con il tuo sviluppo SAP ABAP?\n\n");

		Composite inputComposite = new Composite(sashForm, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(inputComposite);

		actionsCombo = new Combo(inputComposite, SWT.READ_ONLY | SWT.DROP_DOWN);
		actionsCombo.setItems("⚡ Azioni rapide...", "📎 Includi Codice Attivo", "✂️ Trasferisci Selezione");
		actionsCombo.select(0);
		GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(actionsCombo);

		actionsCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int index = actionsCombo.getSelectionIndex();
				if (index == 1) {
					includeActiveCode();
				} else if (index == 2) {
					includeSelectedCode();
				}
				actionsCombo.select(0);
			}
		});

		chatInput = new Text(inputComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(chatInput);

		sendButton = new Button(inputComposite, SWT.PUSH);
		sendButton.setText("Invia");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BOTTOM).hint(80, SWT.DEFAULT).applyTo(sendButton);

		sendButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sendMessage();
			}
		});

		chatInput.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.CR || e.character == SWT.LF) {
					if ((e.stateMask & SWT.SHIFT) == 0) {
						e.doit = false;
						sendMessage();
					}
				}
			}
		});
		
		sashForm.setWeights(new int[] { 70, 30 });
		
		createActions();
	}

	/**
	 * Crea e registra le azioni aggiuntive della vista, come il pulsante per pulire la chat.
	 */
	private void createActions() {
		IActionBars bars = getViewSite().getActionBars();
		IToolBarManager manager = bars.getToolBarManager();

		Action clearAction = new Action("Clear Chat") {
			public void run() {
				clearChat();
				historyCombo.select(0);
			}
		};
		clearAction.setToolTipText("Svuota la chat e inizia una nuova conversazione");
		// Using text since we don't have a specific icon ready
		clearAction.setText("🗑️ Clear");
		
		manager.add(clearAction);
	}
	
	/**
	 * Ripulisce la conversazione corrente, sia in memoria che nell'interfaccia grafica.
	 */
	private void clearChat() {
		currentConversation.clear();
		chatHistory.setText("Benvenuto! Come posso aiutarti con il tuo sviluppo SAP ABAP?\n\n");
		chatInput.setText("");
	}
	
	/**
	 * Aggiorna il menu a tendina che mostra lo storico delle conversazioni salvate.
	 */
	private void refreshHistoryCombo() {
		String current = historyCombo.getText();
		historyCombo.removeAll();
		historyCombo.add("Nuova Conversazione...");
		
		List<String> files = ChatHistoryManager.getAvailableHistories();
		for (String f : files) {
			historyCombo.add(f);
		}
		
		int idx = historyCombo.indexOf(current);
		if (idx != -1) {
			historyCombo.select(idx);
		} else {
			historyCombo.select(0);
		}
	}
	
	/**
	 * Carica una conversazione precedentemente salvata a partire dal nome del file.
	 * 
	 * @param filename il nome del file JSON da caricare
	 */
	private void loadConversation(String filename) {
		currentConversation = ChatHistoryManager.loadHistory(filename);
		chatHistory.setText("");
		for (ChatMessage msg : currentConversation) {
			String name = "user".equals(msg.getRole()) ? "Tu" : "AI";
			chatHistory.append(name + ": " + msg.getContent() + "\n\n");
		}
		chatHistory.setSelection(chatHistory.getText().length());
	}

	/**
	 * Raccoglie il testo digitato dall'utente e lo invia al provider AI selezionato.
	 * Gestisce anche l'aggiornamento asincrono dell'interfaccia.
	 */
	private void sendMessage() {
		String text = chatInput.getText().trim();
		if (text.isEmpty()) {
			return;
		}

		currentConversation.add(new ChatMessage("user", text));
		chatHistory.append("Tu: " + text + "\n\n");
		chatInput.setText("");
		
		chatInput.setEnabled(false);
		sendButton.setEnabled(false);
		
		String loadingMsg = "Caricamento...\n";
		chatHistory.append(loadingMsg);
		chatHistory.setSelection(chatHistory.getText().length());

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String provider = store.getString(PreferenceConstants.ACTIVE_PROVIDER);
		boolean isGemini = PreferenceConstants.PROVIDER_GEMINI.equals(provider);

		String apiKey = store.getString(isGemini ? PreferenceConstants.GEMINI_API_KEY : PreferenceConstants.DEEPSEEK_API_KEY);
		String endpoint = store.getString(isGemini ? PreferenceConstants.GEMINI_ENDPOINT : PreferenceConstants.DEEPSEEK_ENDPOINT);
		String model = store.getString(isGemini ? PreferenceConstants.GEMINI_MODEL : PreferenceConstants.DEEPSEEK_MODEL);

		final String cleanApiKey = (apiKey == null) ? "" : apiKey.replaceAll("\\s+", "").replace("\"", "");
		final String cleanEndpoint = (endpoint == null) ? "" : endpoint.trim();
		final String cleanModel = (model == null) ? "" : model.trim();

		if (cleanApiKey.isEmpty()) {
			chatHistory.setText(chatHistory.getText().replace(loadingMsg, ""));
			chatHistory.append("Sistema: Errore. Nessuna API Key trovata per " + provider + ".\n\n");
			chatInput.setEnabled(true);
			sendButton.setEnabled(true);
			chatInput.setFocus();
			return;
		}

		CompletableFuture.runAsync(() -> {
			String responseText = "";
			try {
				HttpClient client = HttpClient.newHttpClient();
				HttpRequest request;
				
				String debugUrl = cleanEndpoint;
				String debugPayload = "";
				
				if (isGemini) {
					if (cleanEndpoint.contains("/interactions")) {
						StringBuilder payloadBuilder = new StringBuilder();
						payloadBuilder.append("{\"model\": \"").append(cleanModel).append("\", \"input\": \"");
						for (int i = 0; i < currentConversation.size(); i++) {
							ChatMessage msg = currentConversation.get(i);
							String prefix = "user".equals(msg.getRole()) ? "User: " : "AI: ";
							String escaped = msg.getContent().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
							payloadBuilder.append(prefix).append(escaped);
							if (i < currentConversation.size() - 1) payloadBuilder.append("\\n\\n");
						}
						payloadBuilder.append("\"}");
						debugPayload = payloadBuilder.toString();
					} else {
						StringBuilder payloadBuilder = new StringBuilder();
						payloadBuilder.append("{\"model\": \"").append(cleanModel).append("\", \"contents\": [");
						for (int i = 0; i < currentConversation.size(); i++) {
							ChatMessage msg = currentConversation.get(i);
							String r = "user".equals(msg.getRole()) ? "user" : "model";
							String escaped = msg.getContent().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
							payloadBuilder.append("{\"role\": \"").append(r).append("\", \"parts\": [{\"text\": \"").append(escaped).append("\"}]}");
							if (i < currentConversation.size() - 1) payloadBuilder.append(", ");
						}
						payloadBuilder.append("]}");
						debugPayload = payloadBuilder.toString();
					}
					
					request = HttpRequest.newBuilder()
						.uri(URI.create(cleanEndpoint))
						.header("Content-Type", "application/json")
						.header("x-goog-api-key", cleanApiKey)
						.POST(HttpRequest.BodyPublishers.ofString(debugPayload))
						.build();
				} else {
					StringBuilder payloadBuilder = new StringBuilder();
					payloadBuilder.append("{\"model\": \"").append(cleanModel).append("\", \"messages\": [");
					for (int i = 0; i < currentConversation.size(); i++) {
						ChatMessage msg = currentConversation.get(i);
						String escaped = msg.getContent().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
						payloadBuilder.append("{\"role\": \"").append(msg.getRole()).append("\", \"content\": \"").append(escaped).append("\"}");
						if (i < currentConversation.size() - 1) payloadBuilder.append(", ");
					}
					payloadBuilder.append("]}");
					debugPayload = payloadBuilder.toString();
					
					request = HttpRequest.newBuilder()
						.uri(URI.create(cleanEndpoint))
						.header("Content-Type", "application/json")
						.header("Authorization", "Bearer " + cleanApiKey)
						.POST(HttpRequest.BodyPublishers.ofString(debugPayload))
						.build();
				}
				
				final String dUrl = debugUrl;
				final String dPayload = debugPayload;
				Display.getDefault().asyncExec(() -> {
					if (chatHistory != null && !chatHistory.isDisposed()) {
						chatHistory.append("--- DEBUG INFO ---\nURL: " + dUrl + "\nPayload: " + dPayload + "\n------------------\n\n");
						chatHistory.setSelection(chatHistory.getText().length());
					}
				});
				
				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
				String body = response.body();
				
				// Estrazione manuale basica del testo dal JSON
				String searchKey = isGemini ? "\"text\":" : "\"content\":";
				int idx = body.indexOf(searchKey);
				if (idx != -1) {
					int start = body.indexOf("\"", idx + searchKey.length()) + 1;
					int end = start;
					while (end < body.length()) {
						if (body.charAt(end) == '"' && body.charAt(end - 1) != '\\') {
							break;
						}
						end++;
					}
					responseText = body.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
					
					// Aggiungi alla conversazione e salva
					currentConversation.add(new ChatMessage("assistant", responseText));
					ChatHistoryManager.saveHistory(currentConversation);
				} else {
					responseText = "Errore API (" + response.statusCode() + "): " + body;
				}
				
			} catch (Exception ex) {
				responseText = "Eccezione durante la chiamata: " + ex.getMessage();
			}
			
			final String finalRes = responseText;
			Display.getDefault().asyncExec(() -> {
				if (!chatHistory.isDisposed()) {
					String current = chatHistory.getText();
					if (current.endsWith(loadingMsg)) {
						chatHistory.setText(current.substring(0, current.length() - loadingMsg.length()));
					}
					chatHistory.append("AI: " + finalRes + "\n\n");
					chatHistory.setSelection(chatHistory.getText().length());
					refreshHistoryCombo();
				}
				
				if (!chatInput.isDisposed()) {
					chatInput.setEnabled(true);
					chatInput.setFocus();
				}
				
				if (!sendButton.isDisposed()) {
					sendButton.setEnabled(true);
				}
			});
		});
	}

	/**
	 * Passa il focus (il cursore) alla casella di input della chat quando la vista viene attivata.
	 */
	@Override
	public void setFocus() {
		if (chatInput != null && !chatInput.isDisposed()) {
			chatInput.setFocus();
		}
	}

	/**
	 * Recupera il codice sorgente dall'editor di testo attualmente in uso su Eclipse
	 * e lo incolla formattato all'interno della casella di input della chat.
	 */
	private void includeActiveCode() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) return;
		
		IEditorPart activeEditor = window.getActivePage().getActiveEditor();
		if (activeEditor == null) {
			chatHistory.append("Sistema: Nessun editor attivo trovato.\n\n");
			return;
		}

		ITextEditor textEditor = activeEditor.getAdapter(ITextEditor.class);
		if (textEditor == null && activeEditor instanceof ITextEditor) {
			textEditor = (ITextEditor) activeEditor;
		}

		if (textEditor == null) {
			chatHistory.append("Sistema: L'editor attivo non è un editor testuale.\n\n");
			return;
		}

		IDocumentProvider provider = textEditor.getDocumentProvider();
		if (provider == null) {
			chatHistory.append("Sistema: Impossibile recuperare il contenuto dell'editor.\n\n");
			return;
		}
		
		IDocument document = provider.getDocument(textEditor.getEditorInput());
		if (document == null) {
			chatHistory.append("Sistema: Impossibile leggere il documento.\n\n");
			return;
		}

		String code = document.get();
		String currentInput = chatInput.getText();
		if (!currentInput.isEmpty() && !currentInput.endsWith("\n")) {
			currentInput += "\n";
		}
		chatInput.setText(currentInput + "```abap\n" + code + "\n```\n");
		chatInput.setSelection(chatInput.getText().length());
		chatInput.setFocus();
	}

	/**
	 * Recupera solo il testo selezionato nell'editor attivo e lo incolla
	 * all'interno della casella di input della chat. Mostra un errore se non c'è selezione.
	 */
	private void includeSelectedCode() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) return;
		
		IEditorPart activeEditor = window.getActivePage().getActiveEditor();
		if (activeEditor == null) {
			chatHistory.append("Sistema: Nessun editor attivo trovato.\n\n");
			return;
		}

		ITextEditor textEditor = activeEditor.getAdapter(ITextEditor.class);
		if (textEditor == null && activeEditor instanceof ITextEditor) {
			textEditor = (ITextEditor) activeEditor;
		}

		if (textEditor == null) {
			chatHistory.append("Sistema: L'editor attivo non è un editor testuale.\n\n");
			return;
		}

		org.eclipse.jface.viewers.ISelectionProvider selectionProvider = textEditor.getSelectionProvider();
		if (selectionProvider == null) {
			chatHistory.append("Sistema: Impossibile recuperare la selezione dall'editor.\n\n");
			return;
		}

		org.eclipse.jface.viewers.ISelection selection = selectionProvider.getSelection();
		if (selection instanceof org.eclipse.jface.text.ITextSelection) {
			org.eclipse.jface.text.ITextSelection textSelection = (org.eclipse.jface.text.ITextSelection) selection;
			String selectedText = textSelection.getText();
			
			if (selectedText == null || selectedText.isEmpty()) {
				chatHistory.append("Sistema: Errore. Nessun testo selezionato nell'editor attivo.\n\n");
				return;
			}
			
			String currentInput = chatInput.getText();
			if (!currentInput.isEmpty() && !currentInput.endsWith("\n")) {
				currentInput += "\n";
			}
			chatInput.setText(currentInput + "```abap\n" + selectedText + "\n```\n");
			chatInput.setSelection(chatInput.getText().length());
			chatInput.setFocus();
		} else {
			chatHistory.append("Sistema: La selezione corrente non è testuale.\n\n");
		}
	}
}
