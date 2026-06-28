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
	 * Salva una lista di messaggi in un nuovo file JSON generato con la data e l'ora attuali.
	 * 
	 * @param messages la lista di messaggi (ChatMessage) da salvare
	 * @return il nome del file generato, oppure null in caso di errore
	 */
	public static String saveHistory(List<ChatMessage> messages) {
		if (messages == null || messages.isEmpty()) {
			return null;
		}
		
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		String filename = "chat_" + timestamp + ".json";
		Path file = getHistoryDir().resolve(filename);
		
		StringBuilder json = new StringBuilder("[\n");
		for (int i = 0; i < messages.size(); i++) {
			ChatMessage msg = messages.get(i);
			String escapedContent = msg.getContent().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
			json.append("  {\n");
			json.append("    \"role\": \"").append(msg.getRole()).append("\",\n");
			json.append("    \"content\": \"").append(escapedContent).append("\"\n");
			json.append("  }");
			if (i < messages.size() - 1) {
				json.append(",");
			}
			json.append("\n");
		}
		json.append("]");
		
		try {
			Files.writeString(file, json.toString());
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
	 * @return una lista di ChatMessage recuperata dal file
	 */
	public static List<ChatMessage> loadHistory(String filename) {
		Path file = getHistoryDir().resolve(filename);
		if (!Files.exists(file)) {
			return new ArrayList<>();
		}
		
		try {
			String json = Files.readString(file);
			return parseSimpleJsonArray(json);
		} catch (IOException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Un parser molto basilare per estrarre la lista di messaggi dalla stringa JSON.
	 * Sviluppato manualmente per minimizzare le dipendenze esterne.
	 * 
	 * @param json la stringa in formato JSON
	 * @return la lista di ChatMessage elaborata
	 */
	private static List<ChatMessage> parseSimpleJsonArray(String json) {
		List<ChatMessage> messages = new ArrayList<>();
		
		// Extremely simple parser since we control the format
		int idx = 0;
		while ((idx = json.indexOf("\"role\":", idx)) != -1) {
			int roleStart = json.indexOf("\"", idx + 7) + 1;
			int roleEnd = json.indexOf("\"", roleStart);
			String role = json.substring(roleStart, roleEnd);
			
			int contentIdx = json.indexOf("\"content\":", roleEnd);
			if (contentIdx == -1) break;
			
			int contentStart = json.indexOf("\"", contentIdx + 10) + 1;
			int contentEnd = contentStart;
			while (contentEnd < json.length()) {
				if (json.charAt(contentEnd) == '"' && json.charAt(contentEnd - 1) != '\\') {
					break;
				}
				contentEnd++;
			}
			
			String content = json.substring(contentStart, contentEnd)
					.replace("\\n", "\n")
					.replace("\\\"", "\"")
					.replace("\\\\", "\\");
					
			messages.add(new ChatMessage(role, content));
			idx = contentEnd;
		}
		
		return messages;
	}
}
