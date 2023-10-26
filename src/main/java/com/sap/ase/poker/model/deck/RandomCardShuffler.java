package com.sap.ase.poker.model.deck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RandomCardShuffler implements CardShuffler {

  @Override
  public List<Card> shuffle(List<Card> cards) {
    ArrayList<Card> shuffled = new ArrayList<>(cards);
    Collections.shuffle(shuffled);
    return new ArrayList<>(shuffled);
  }
}
