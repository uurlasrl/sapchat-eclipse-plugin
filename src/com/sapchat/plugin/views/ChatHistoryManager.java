package com.sapchat.plugin.views;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sapchat.plugin.Activator;

/**
 * Gestisce il salvataggio e il caricamento della cronologia della chat
 * all'interno del workspace di Eclipse, in formato JSON.
 */
public class ChatHistoryManager {

	private static final String HISTORY_DIR = "history";

	/**
	 * Restituisce il percorso della cartella dove vengono salvati i file JSON
	 * con la cronologia delle conversazioni. Crea la cartella se non esiste.
	 * 
	 * @return il percorso (Path) della cartella della cronologia
	 */
	public static Path getHistoryDir() {
		Path dir = Paths.get(Activator.getDefault().getStateLocation().append(HISTORY_DIR).toOSString());
		if (!Files.exists(dir)) {
			try {
				Files.createDirectories(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return dir;
	}

	/**
	 * Salva una sessione in un nuovo file JSON generato con la data e l'ora attuali.
	 * 
	 * @param session la sessione da salvare
	 * @return il nome del file generato, oppure null in caso di errore
	 */
	public static String saveHistory(ChatSession session) {
		if (session == null || session.getMessages().isEmpty()) {
			return null;
		}
		
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		String filename = "chat_" + timestamp + ".json";
		return saveHistory(session, filename);
	}

	/**
	 * Salva una sessione in un file JSON specifico (sovrascrivendolo se esiste).
	 * 
	 * @param session la sessione da salvare
	 * @param filename il nome del file JSON in cui salvare (es. "chat_history_<sessionId>.json")
	 * @return il nome del file salvato, oppure null in caso di errore
	 */
	public static String saveHistory(ChatSession session, String filename) {
		if (session == null || session.getMessages().isEmpty() || filename == null || filename.isEmpty()) {
			return null;
		}
		
		Path file = getHistoryDir().resolve(filename);
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(session);
		
		try {
			Files.writeString(file, json);
			return filename;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Recupera la lista dei file di cronologia disponibili (file .json),
	 * ordinati in ordine cronologico inverso (dal più recente al più vecchio).
	 * 
	 * @return una lista di nomi di file
	 */
	public static List<String> getAvailableHistories() {
		File dir = getHistoryDir().toFile();
		File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
		if (files == null) {
			return new ArrayList<>();
		}
		
		List<String> names = Arrays.stream(files)
				.map(File::getName)
				.sorted(Collections.reverseOrder())
				.collect(Collectors.toList());
		return names;
	}

	/**
	 * Carica la cronologia di una conversazione leggendo il file JSON specificato.
	 * 
	 * @param filename il nome del file da caricare (es. "chat_20260625_103000.json")
	 * @return la sessione recuperata dal file
	 */
	public static ChatSession loadHistory(String filename) {
		Path file = getHistoryDir().resolve(filename);
		if (!Files.exists(file)) {
			return new ChatSession();
		}
		
		try {
			String json = Files.readString(file);
			return parseSessionJson(json);
		} catch (IOException e) {
			e.printStackTrace();
			return new ChatSession();
		}
	}

	/**
	 * Un parser molto basilare per estrarre la sessione dalla stringa JSON.
	 * Sviluppato manualmente per minimizzare le dipendenze esterne.
	 * 
	 * @param json la stringa in formato JSON
	 * @return la sessione elaborata
	 */
	private static ChatSession parseSessionJson(String json) {
		json = json.trim();
		Gson gson = new Gson();
		
		// Retro-compatibilità: vecchio formato array
		if (json.startsWith("[")) {
			ChatSession session = new ChatSession();
			List<ChatMessage> messages = new ArrayList<>();
			JsonArray array = JsonParser.parseString(json).getAsJsonArray();
			for (JsonElement element : array) {
				JsonObject obj = element.getAsJsonObject();
				String role = obj.has("role") ? obj.get("role").getAsString() : "user";
				String content = obj.has("content") ? obj.get("content").getAsString() : "";
				messages.add(new ChatMessage(role, content));
			}
			session.setMessages(messages);
			return session;
		}
		
		return gson.fromJson(json, ChatSession.class);
	}
}
