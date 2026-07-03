package com.sapchat.plugin.views;

public class MarkdownToHtmlConverter {

	public static String convert(String markdown) {
		if (markdown == null || markdown.isEmpty()) {
			return "";
		}

		StringBuilder html = new StringBuilder();
		// Gestiamo correttamente i ritorni a capo per diversi OS
		String[] lines = markdown.replace("\r", "").split("\n");
		
		boolean inCodeBlock = false;
		StringBuilder codeBlockContent = new StringBuilder();
		String codeLanguage = "";
		
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			
			if (line.trim().startsWith("```")) {
				if (inCodeBlock) {
					// Chiusura del blocco di codice
					html.append(renderCodeBlock(codeLanguage, codeBlockContent.toString()));
					inCodeBlock = false;
					codeBlockContent.setLength(0);
					codeLanguage = "";
				} else {
					// Apertura del blocco di codice
					inCodeBlock = true;
					codeLanguage = line.trim().substring(3).trim();
				}
				continue;
			}
			
			if (inCodeBlock) {
				codeBlockContent.append(line).append("\n");
			} else {
				// Formattazione inline base
				String formattedLine = escapeHtmlBase(line);
				formattedLine = formatInline(formattedLine);
				html.append(formattedLine).append("<br/>\n");
			}
		}
		
		// Nel caso il blocco di codice non sia stato chiuso correttamente
		if (inCodeBlock) {
			html.append(renderCodeBlock(codeLanguage, codeBlockContent.toString()));
		}
		
		return html.toString();
	}
	
	private static String renderCodeBlock(String language, String content) {
		// Tolgo l'ultimo \n in eccesso se presente
		if (content.endsWith("\n")) {
			content = content.substring(0, content.length() - 1);
		}
		
		int lineCount = content.isEmpty() ? 0 : content.split("\n").length;
		String escapedContent = escapeHtmlCode(content);
		
		StringBuilder sb = new StringBuilder();
		
		if (lineCount < 5) {
			sb.append("<details open>");
		} else {
			sb.append("<details>");
		}
		
		String langLabel = language.isEmpty() ? "" : " [" + language + "]";
		sb.append("<summary style=\"cursor:pointer; font-weight:bold; margin-bottom: 5px;\">\uD83D\uDDB1\uFE0F Mostra/Nascondi Codice").append(langLabel).append(" (").append(lineCount).append(" righe)</summary>");
		sb.append("<pre style=\"margin: 0; padding: 10px; background-color: rgba(128, 128, 128, 0.1); border-radius: 4px; overflow-x: auto;\"><code>");
		sb.append(escapedContent);
		sb.append("</code></pre>");
		sb.append("</details>\n");
		
		return sb.toString();
	}
	
	private static String escapeHtmlBase(String text) {
		return text.replace("&", "&amp;")
				   .replace("<", "&lt;")
				   .replace(">", "&gt;");
	}
	
	private static String escapeHtmlCode(String text) {
		return text.replace("&", "&amp;")
				   .replace("<", "&lt;")
				   .replace(">", "&gt;")
				   .replace("\"", "&quot;")
				   .replace("'", "&#39;");
	}
	
	private static String formatInline(String text) {
		// Grassetto **testo**
		text = text.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
		// Corsivo *testo*
		text = text.replaceAll("\\*(.*?)\\*", "<em>$1</em>");
		// Inline code `testo`
		text = text.replaceAll("`(.*?)`", "<code style=\"background-color: rgba(128,128,128,0.2); padding: 2px 4px; border-radius: 3px;\">$1</code>");
		
		return text;
	}
}
