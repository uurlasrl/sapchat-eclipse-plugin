package com.sapchat.plugin.views;

import java.util.ArrayList;
import java.util.List;

public class ChatSession {
	private List<ChatMessage> messages;
	private String currency;
	
	private int totalInputTokens;
	private int totalOutputTokens;
	private int totalCachedTokens;
	
	private double totalInputCost;
	private double totalOutputCost;
	private double totalCachedCost;
	private double grandTotalCost;

	public ChatSession() {
		this.messages = new ArrayList<>();
		this.currency = "EUR";
	}

	public List<ChatMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<ChatMessage> messages) {
		this.messages = messages;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public int getTotalInputTokens() {
		return totalInputTokens;
	}

	public void setTotalInputTokens(int totalInputTokens) {
		this.totalInputTokens = totalInputTokens;
	}

	public int getTotalOutputTokens() {
		return totalOutputTokens;
	}

	public void setTotalOutputTokens(int totalOutputTokens) {
		this.totalOutputTokens = totalOutputTokens;
	}

	public int getTotalCachedTokens() {
		return totalCachedTokens;
	}

	public void setTotalCachedTokens(int totalCachedTokens) {
		this.totalCachedTokens = totalCachedTokens;
	}

	public double getTotalInputCost() {
		return totalInputCost;
	}

	public void setTotalInputCost(double totalInputCost) {
		this.totalInputCost = totalInputCost;
	}

	public double getTotalOutputCost() {
		return totalOutputCost;
	}

	public void setTotalOutputCost(double totalOutputCost) {
		this.totalOutputCost = totalOutputCost;
	}

	public double getTotalCachedCost() {
		return totalCachedCost;
	}

	public void setTotalCachedCost(double totalCachedCost) {
		this.totalCachedCost = totalCachedCost;
	}

	public double getGrandTotalCost() {
		return grandTotalCost;
	}

	public void setGrandTotalCost(double grandTotalCost) {
		this.grandTotalCost = grandTotalCost;
	}
	
	public void addTokensAndCost(int inputTokens, int outputTokens, int cachedTokens,
			double inputCost, double outputCost, double cachedCost) {
		this.totalInputTokens += inputTokens;
		this.totalOutputTokens += outputTokens;
		this.totalCachedTokens += cachedTokens;
		
		this.totalInputCost += inputCost;
		this.totalOutputCost += outputCost;
		this.totalCachedCost += cachedCost;
		
		this.grandTotalCost = this.totalInputCost + this.totalOutputCost + this.totalCachedCost;
	}
	
	public void clear() {
		this.messages.clear();
		this.totalInputTokens = 0;
		this.totalOutputTokens = 0;
		this.totalCachedTokens = 0;
		this.totalInputCost = 0.0;
		this.totalOutputCost = 0.0;
		this.totalCachedCost = 0.0;
		this.grandTotalCost = 0.0;
	}
}
