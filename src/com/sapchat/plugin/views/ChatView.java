package com.sapchat.plugin.views;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Color;
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

	private Browser chatHistory;
	private Text chatInput;
	private Button sendButton;
	private Combo actionsCombo;
	private Combo historyCombo;
	
	private ChatSession currentSession = new ChatSession();
	private String currentSessionId = null;

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

		chatHistory = new Browser(sashForm, SWT.NONE);
		updateBrowserContent();

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
					if ((e.stateMask & SWT.SHIFT) == 0 && (e.stateMask & SWT.ALT) == 0) {
						e.doit = false;
						sendMessage();
					}
				}
			}
		});
		
		sashForm.setWeights(new int[] { 70, 30 });
		
		createActions();
	}
	
	private void addSystemMessage(String text) {
		currentSession.getMessages().add(new ChatMessage("system", text));
		updateBrowserContent();
	}
	
	private void updateBrowserContent() {
		if (chatHistory == null || chatHistory.isDisposed()) return;
		
		StringBuilder html = new StringBuilder();
		
		// Ottieni il colore di sfondo di sistema
		Color bgColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		Color fgColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
		
		String bgHex = String.format("#%02x%02x%02x", bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue());
		String fgHex = String.format("#%02x%02x%02x", fgColor.getRed(), fgColor.getGreen(), fgColor.getBlue());
		
		html.append("<html><head><style>");
		html.append("body { background-color: ").append(bgHex).append("; color: ").append(fgHex).append("; font-family: sans-serif; padding: 10px; }");
		html.append("pre { font-family: Consolas, Monaco, monospace; font-size: 13px; }");
		html.append(".user-msg { margin-bottom: 15px; }");
		html.append(".ai-msg { margin-bottom: 15px; }");
		html.append(".sys-msg { margin-bottom: 15px; font-style: italic; color: #cc0000; }");
		html.append("</style></head><body>");
		
		if (currentSession.getMessages().isEmpty()) {
			html.append("<div class='sys-msg'>Benvenuto! Come posso aiutarti con il tuo sviluppo SAP ABAP?</div>");
		} else {
			for (ChatMessage msg : currentSession.getMessages()) {
				String roleClass = "user".equals(msg.getRole()) ? "user-msg" : ("system".equals(msg.getRole()) ? "sys-msg" : "ai-msg");
				String name = "user".equals(msg.getRole()) ? "<b>Tu:</b><br/>" : ("system".equals(msg.getRole()) ? "<b>Sistema:</b><br/>" : "<b>AI:</b><br/>");
				
				html.append("<div class='").append(roleClass).append("'>");
				html.append(name);
				html.append(MarkdownToHtmlConverter.convert(msg.getContent()));
				html.append("</div>");
			}
		}
		
		html.append("</body></html>");
		chatHistory.setText(html.toString());
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
		currentSession.clear();
		updateBrowserContent();
		chatInput.setText("");
		currentSessionId = null;
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
		currentSession = ChatHistoryManager.loadHistory(filename);
		
		// Imposta il currentSessionId estraendolo dal nome del file caricato
		if (filename.startsWith("chat_history_") && filename.endsWith(".json")) {
			currentSessionId = filename.substring("chat_history_".length(), filename.length() - ".json".length());
		} else if (filename.startsWith("chat_") && filename.endsWith(".json")) {
			currentSessionId = filename.substring("chat_".length(), filename.length() - ".json".length());
		} else {
			currentSessionId = filename;
		}
		
		updateBrowserContent();
	}

	/**
	 * Salva lo storico della conversazione corrente sul disco.
	 * Se non esiste ancora una sessione, ne genera una basata su timestamp.
	 */
	private void saveCurrentHistory() {
		if (currentSession == null || currentSession.getMessages().isEmpty()) {
			return;
		}
		if (currentSessionId == null) {
			currentSessionId = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		}
		String filename = "chat_history_" + currentSessionId + ".json";
		ChatHistoryManager.saveHistory(currentSession, filename);
	}

	/**
	 * Filtra la lista dei messaggi in base alle impostazioni di ottimizzazione (Sliding Window).
	 * Restituisce una nuova lista indipendente per evitare eccezioni di modifica concorrente o out-of-bounds.
	 */
	private List<ChatMessage> getMessagesToSend(List<ChatMessage> fullHistory, boolean enableOptimization, int windowSize) {
		if (fullHistory == null) {
			return new ArrayList<>();
		}
		int size = fullHistory.size();
		if (!enableOptimization || windowSize <= 0 || size <= windowSize + 1) {
			return new ArrayList<>(fullHistory);
		}
		
		List<ChatMessage> optimizedList = new ArrayList<>();
		// Include sempre il primissimo messaggio (es. regole di ingaggio)
		optimizedList.add(fullHistory.get(0));
		
		// Include gli ultimi N messaggi
		int start = size - windowSize;
		optimizedList.addAll(fullHistory.subList(start, size));
		
		return optimizedList;
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

		currentSession.getMessages().add(new ChatMessage("user", text));
		saveCurrentHistory(); // Salva immediatamente il messaggio dell'utente su disco
		updateBrowserContent();
		chatInput.setText("");
		
		chatInput.setEnabled(false);
		sendButton.setEnabled(false);
		
		// Add a temporary loading message to currentSession (to be removed later)
		ChatMessage loadingMsgObj = new ChatMessage("system", "Caricamento...");
		currentSession.getMessages().add(loadingMsgObj);
		updateBrowserContent();

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
			currentSession.getMessages().remove(loadingMsgObj);
			addSystemMessage("Errore. Nessuna API Key trovata per " + provider + ".");
			chatInput.setEnabled(true);
			sendButton.setEnabled(true);
			chatInput.setFocus();
			return;
		}

		CompletableFuture.runAsync(() -> {
			String responseText = "";
			String telemetryBlock = "";
			try {
				HttpClient client = HttpClient.newHttpClient();
				HttpRequest request;
				
				String debugUrl = cleanEndpoint;
				String debugPayload = "";
				
				// Leggi le preferenze per la sliding window
				boolean enableOptimization = store.getBoolean(PreferenceConstants.ENABLE_HISTORY_OPTIMIZATION);
				int windowSize = store.getInt(PreferenceConstants.SLIDING_WINDOW_SIZE);
				List<ChatMessage> messagesToSend = getMessagesToSend(currentSession.getMessages(), enableOptimization, windowSize);
				
				if (isGemini) {
					if (cleanEndpoint.contains("/interactions")) {
						StringBuilder payloadBuilder = new StringBuilder();
						payloadBuilder.append("{\"model\": \"").append(cleanModel).append("\", \"input\": \"");
						for (int i = 0; i < messagesToSend.size(); i++) {
							ChatMessage msg = messagesToSend.get(i);
							String prefix = "user".equals(msg.getRole()) ? "User: " : "AI: ";
							String escaped = msg.getContent().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
							payloadBuilder.append(prefix).append(escaped);
							if (i < messagesToSend.size() - 1) payloadBuilder.append("\\n\\n");
						}
						payloadBuilder.append("\"}");
						debugPayload = payloadBuilder.toString();
					} else {
						StringBuilder payloadBuilder = new StringBuilder();
						payloadBuilder.append("{\"model\": \"").append(cleanModel).append("\", \"contents\": [");
						for (int i = 0; i < messagesToSend.size(); i++) {
							ChatMessage msg = messagesToSend.get(i);
							String r = "user".equals(msg.getRole()) ? "user" : "model";
							String escaped = msg.getContent().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
							payloadBuilder.append("{\"role\": \"").append(r).append("\", \"parts\": [{\"text\": \"").append(escaped).append("\"}]}");
							if (i < messagesToSend.size() - 1) payloadBuilder.append(", ");
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
					for (int i = 0; i < messagesToSend.size(); i++) {
						ChatMessage msg = messagesToSend.get(i);
						String escaped = msg.getContent().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
						payloadBuilder.append("{\"role\": \"").append(msg.getRole()).append("\", \"content\": \"").append(escaped).append("\"}");
						if (i < messagesToSend.size() - 1) payloadBuilder.append(", ");
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
				final boolean showDebug = store.getBoolean(PreferenceConstants.ENABLE_DEBUG_OUTPUT);
				Display.getDefault().asyncExec(() -> {
					if (showDebug) {
						addSystemMessage("--- DEBUG INFO ---\nURL: " + dUrl + "\nPayload: " + dPayload);
					}
				});
				
				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
				String body = response.body();
				
				// Parse JSON con Gson
				JsonObject root = JsonParser.parseString(body).getAsJsonObject();
				
				int promptTokens = 0;
				int completionTokens = 0;
				int cachedTokens = 0;
				telemetryBlock = "";
				
				try {
					if (isGemini) {
						if (root.has("candidates") && root.getAsJsonArray("candidates").size() > 0) {
							JsonObject candidate = root.getAsJsonArray("candidates").get(0).getAsJsonObject();
							if (candidate.has("content")) {
								responseText = candidate.getAsJsonObject("content")
										.getAsJsonArray("parts").get(0).getAsJsonObject()
										.get("text").getAsString();
							}
						}
						
						if (root.has("usageMetadata")) {
							JsonObject usage = root.getAsJsonObject("usageMetadata");
							promptTokens = usage.has("promptTokenCount") ? usage.get("promptTokenCount").getAsInt() : 0;
							completionTokens = usage.has("candidatesTokenCount") ? usage.get("candidatesTokenCount").getAsInt() : 0;
							cachedTokens = usage.has("cachedContentTokenCount") ? usage.get("cachedContentTokenCount").getAsInt() : 0;
						}
					} else {
						if (root.has("choices") && root.getAsJsonArray("choices").size() > 0) {
							JsonObject choice = root.getAsJsonArray("choices").get(0).getAsJsonObject();
							if (choice.has("message")) {
								responseText = choice.getAsJsonObject("message").get("content").getAsString();
							}
						}
						
						if (root.has("usage")) {
							JsonObject usage = root.getAsJsonObject("usage");
							promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").getAsInt() : 0;
							completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").getAsInt() : 0;
							
							if (usage.has("prompt_cache_hit_tokens")) {
								cachedTokens = usage.get("prompt_cache_hit_tokens").getAsInt();
							} else if (usage.has("prompt_tokens_details")) {
								JsonObject details = usage.getAsJsonObject("prompt_tokens_details");
								cachedTokens = details.has("cached_tokens") ? details.get("cached_tokens").getAsInt() : 0;
							}
						}
					}
					
					// Recupero preferenze per calcolo costi
					String prefCurrencyKey = isGemini ? PreferenceConstants.GEMINI_CURRENCY : PreferenceConstants.DEEPSEEK_CURRENCY;
					String prefInputCostKey = isGemini ? PreferenceConstants.GEMINI_INPUT_COST : PreferenceConstants.DEEPSEEK_INPUT_COST;
					String prefOutputCostKey = isGemini ? PreferenceConstants.GEMINI_OUTPUT_COST : PreferenceConstants.DEEPSEEK_OUTPUT_COST;
					String prefCacheCostKey = isGemini ? PreferenceConstants.GEMINI_CACHE_COST : PreferenceConstants.DEEPSEEK_CACHE_COST;
					
					String currency = store.getString(prefCurrencyKey);
					double inputCostPerM = parseDoubleSafe(store.getString(prefInputCostKey));
					double outputCostPerM = parseDoubleSafe(store.getString(prefOutputCostKey));
					double cacheCostPerM = parseDoubleSafe(store.getString(prefCacheCostKey));
					
					int actualInputTokens = promptTokens - cachedTokens;
					
					double inputCost = (actualInputTokens / 1_000_000.0) * inputCostPerM;
					double outputCost = (completionTokens / 1_000_000.0) * outputCostPerM;
					double cacheCost = (cachedTokens / 1_000_000.0) * cacheCostPerM;
					
					// Aggiornamento sessione
					currentSession.setCurrency(currency);
					currentSession.addTokensAndCost(actualInputTokens, completionTokens, cachedTokens, inputCost, outputCost, cacheCost);
					
					// Aggiungi alla conversazione e salva
					currentSession.getMessages().add(new ChatMessage("assistant", responseText));
					saveCurrentHistory(); // Sovrascrive lo stesso file con la risposta dell'assistente e i nuovi token
					
					// Costruzione del blocco di telemetria
					String sym = "EUR".equalsIgnoreCase(currency) ? "€" : "$";
					telemetryBlock = String.format(java.util.Locale.US,
						"--------------------------------------------------\n" +
						"📊 Ultimo Invio (Token): Input: %d | Output: %d | Cache: %d\n" +
						"📈 Totali Sessione (Token): Input: %d | Output: %d | Cache: %d\n" +
						"💸 Costi Sessione: Input: %.6f %s | Output: %.6f %s | Cache: %.6f %s\n" +
						"💰 Costo Complessivo Chat: %.6f %s\n" +
						"--------------------------------------------------\n\n",
						actualInputTokens, completionTokens, cachedTokens,
						currentSession.getTotalInputTokens(), currentSession.getTotalOutputTokens(), currentSession.getTotalCachedTokens(),
						currentSession.getTotalInputCost(), sym, currentSession.getTotalOutputCost(), sym, currentSession.getTotalCachedCost(), sym,
						currentSession.getGrandTotalCost(), sym);

					// Aggiungiamo il blocco di telemetria come messaggio di sistema alla chat session (opzionale, o lo appendiamo come AI?)
					// Per ora lo mettiamo come system message così non viene ricaricato come contesto per il LLM.
					
				} catch (Exception e) {
					responseText = "Errore nel parsing della risposta JSON: " + e.getMessage() + "\nBody: " + body;
				}
				
			} catch (Exception ex) {
				responseText = "Eccezione durante la chiamata: " + ex.getMessage();
			}
			
			final String finalRes = responseText;
			final String finalTelemetry = telemetryBlock;
			Display.getDefault().asyncExec(() -> {
				currentSession.getMessages().remove(loadingMsgObj);
				
				if (!finalRes.startsWith("Errore") && !finalRes.startsWith("Eccezione")) {
					// L'assistente ha risposto correttamente, il messaggio l'abbiamo già aggiunto in `currentSession` e salvato
					if (finalTelemetry != null && !finalTelemetry.isEmpty()) {
						// Aggiungiamo telemetria alla fine come system message
						currentSession.getMessages().add(new ChatMessage("system", finalTelemetry));
					}
				} else {
					// Mostra l'errore come system
					currentSession.getMessages().add(new ChatMessage("system", finalRes));
				}
				
				updateBrowserContent();
				refreshHistoryCombo();
				
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

	private double parseDoubleSafe(String val) {
		if (val == null || val.trim().isEmpty()) return 0.0;
		try {
			// Converti virgola in punto se l'utente ha digitato la virgola
			return Double.parseDouble(val.replace(",", ".").trim());
		} catch (Exception e) {
			return 0.0;
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
			addSystemMessage("Nessun editor attivo trovato.");
			return;
		}

		ITextEditor textEditor = activeEditor.getAdapter(ITextEditor.class);
		if (textEditor == null && activeEditor instanceof ITextEditor) {
			textEditor = (ITextEditor) activeEditor;
		}

		if (textEditor == null) {
			addSystemMessage("L'editor attivo non è un editor testuale.");
			return;
		}

		IDocumentProvider provider = textEditor.getDocumentProvider();
		if (provider == null) {
			addSystemMessage("Impossibile recuperare il contenuto dell'editor.");
			return;
		}
		
		IDocument document = provider.getDocument(textEditor.getEditorInput());
		if (document == null) {
			addSystemMessage("Impossibile leggere il documento.");
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
			addSystemMessage("Nessun editor attivo trovato.");
			return;
		}

		ITextEditor textEditor = activeEditor.getAdapter(ITextEditor.class);
		if (textEditor == null && activeEditor instanceof ITextEditor) {
			textEditor = (ITextEditor) activeEditor;
		}

		if (textEditor == null) {
			addSystemMessage("L'editor attivo non è un editor testuale.");
			return;
		}

		org.eclipse.jface.viewers.ISelectionProvider selectionProvider = textEditor.getSelectionProvider();
		if (selectionProvider == null) {
			addSystemMessage("Impossibile recuperare la selezione dall'editor.");
			return;
		}

		org.eclipse.jface.viewers.ISelection selection = selectionProvider.getSelection();
		if (selection instanceof org.eclipse.jface.text.ITextSelection) {
			org.eclipse.jface.text.ITextSelection textSelection = (org.eclipse.jface.text.ITextSelection) selection;
			String selectedText = textSelection.getText();
			
			if (selectedText == null || selectedText.isEmpty()) {
				addSystemMessage("Errore. Nessun testo selezionato nell'editor attivo.");
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
			addSystemMessage("La selezione corrente non è testuale.");
		}
	}
}
