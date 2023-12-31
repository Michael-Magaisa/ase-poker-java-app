package com.sap.ase.poker.model.deck;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class ShuffledDeckSupplier implements Supplier<Deck> {

  private final Supplier<List<Card>> cardSupplier;
  private final CardShuffler cardShuffler;

  public ShuffledDeckSupplier(Supplier<List<Card>> cardSupplier, CardShuffler cardShuffler) {
    this.cardSupplier = cardSupplier;
    this.cardShuffler = cardShuffler;
  }

  public Deck get() {
    Deck deck = new Deck(cardSupplier.get(), cardShuffler);
    deck.shuffle();
    return deck;
  }
}
