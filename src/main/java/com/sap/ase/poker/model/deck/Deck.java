package com.sap.ase.poker.model.deck;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class Deck {
  private List<Card> cards;
  private final CardShuffler cardShuffler;
  private final List<Card> pokerCardsSupply;

  public Deck(List<Card> pokerCardsSupply, CardShuffler cardShuffler) {
    this.pokerCardsSupply = new ArrayList<>(pokerCardsSupply);
    this.cards = new ArrayList<>(pokerCardsSupply);
    this.cardShuffler = cardShuffler;
  }

  public Card draw() {
    if (cards.isEmpty()) {
      throw new OutOfCardsException("No cards left to draw.");
    }
    return cards.remove(0);
  }

  public List<Card> drawThreeCommunityCards() {
    return List.of(draw(), draw(), draw());
  }

  public void shuffle() {
    cards = cardShuffler.shuffle(pokerCardsSupply);
  }
}
