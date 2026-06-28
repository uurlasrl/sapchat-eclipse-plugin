package com.sapchat.plugin.views;

/**
 * Rappresenta un singolo messaggio all'interno dello storico della chat.
 * Memorizza sia il ruolo di chi ha inviato il messaggio (es. "user", "assistant", "model")
 * sia il contenuto testuale del messaggio stesso.
 */
public class ChatMessage {
	private String role;
	private String content;

	/**
	 * Costruttore di default vuoto.
	 */
	public ChatMessage() {
	}

	/**
	 * Crea un nuovo messaggio con ruolo e contenuto specificati.
	 * 
	 * @param role    il ruolo dell'autore del messaggio (es. "user" o "assistant")
	 * @param content il testo del messaggio
	 */
	public ChatMessage(String role, String content) {
		this.role = role;
		this.content = content;
	}

	/**
	 * Restituisce il ruolo associato a questo messaggio.
	 * 
	 * @return la stringa che rappresenta il ruolo
	 */
	public String getRole() {
		return role;
	}

	/**
	 * Imposta il ruolo per questo messaggio.
	 * 
	 * @param role il ruolo da impostare
	 */
	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * Restituisce il testo del messaggio.
	 * 
	 * @return il contenuto del messaggio
	 */
	public String getContent() {
		return content;
	}

	/**
	 * Imposta il contenuto del messaggio.
	 * 
	 * @param content il testo da impostare
	 */
	public void setContent(String content) {
		this.content = content;
	}
}
