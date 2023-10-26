package com.sap.ase.poker.service;

import static com.sap.ase.poker.model.GameState.*;

import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.IllegalActionException;
import com.sap.ase.poker.model.IllegalAmountException;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Deck;
import io.micrometer.common.util.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class TableService {

  public static final int STARTING_CASH = 100;
  public static final int MINIMUM_PLAYERS_REQUIRED_TO_START_A_GAME = 2;
  private final Supplier<Deck> deckSupplier;
  private GameState state = OPEN;
  private final List<Player> players = new ArrayList<>();
  private Optional<Player> currentPlayer = Optional.empty();
  private int pot = 0;
  private final List<Card> communityCards = new ArrayList<>();
  private final Map<String, Integer> bets = new HashMap<>();
  private Optional<Player> winner = Optional.empty();
  private boolean roundIsComplete = false;
  private final List<Card> winnerHand = new ArrayList<>();

  public TableService(Supplier<Deck> deckSupplier) {
    this.deckSupplier = deckSupplier;
  }

  public void performAction(String action, int amount) {
    if (StringUtils.isNotBlank(action)) {
      switch (action) {
        case "check" -> performCheckAction();
        case "raise" -> performRaiseAction(amount);
        case "fold" -> performFoldAction();
        case "call" -> performCallAction();
        default -> throw new IllegalActionException("Unsupported action: " + action);
      }
      checkIfIsRoundComplete(action);
    } else {
      throw new IllegalActionException("Action cannot be empty");
    }
  }

  public void start() {
    if (players.size() >= MINIMUM_PLAYERS_REQUIRED_TO_START_A_GAME) {
      state = PRE_FLOP;
      prepareForPreFlopRound();
      determineNextPlayer();
    }
  }

  public List<Card> getPlayerCards(String playerId) {
    return players.stream()
        .filter(player -> player.getId().equals(playerId))
        .findFirst()
        .map(Player::getHandCards)
        .orElse(Collections.emptyList());
  }

  private void prepareForPreFlopRound() {
    players.forEach(
        player -> {
          dealTwoCardsForEachPlayer(player);
          player.setActive();
        });
  }

  private void dealTwoCardsForEachPlayer(Player player) {
    player.setHandCards(List.of(deckSupplier.get().draw(), deckSupplier.get().draw()));
  }

  public void addPlayer(String playerId, String playerName) {
    Player player = new Player(playerId, playerName, STARTING_CASH);
    players.add(player);
    bets.put(playerId, 0);
  }

  private void checkIfIsRoundComplete(String action) {
    if (action.equals("check")) {
      completeACheckConsensus();
    } else {
      completeANonCheckConsensus();
    }
  }

  private void completeANonCheckConsensus() {
    roundIsComplete = isAllPlayersAreInConsensus();
    if (roundIsComplete) {
      moveToNextRound();
    }
  }

  private boolean isAllPlayersAreInConsensus() {
    Integer currentMaximumBet = determineCurrentMaximumBet();
    Integer currentMinimumBet = determineCurrentMinimumBet();
    return currentMaximumBet.equals(currentMinimumBet);
  }

  private void completeACheckConsensus() {
    roundIsComplete = players.stream().allMatch(Player::isChecked);
    if (roundIsComplete) {
      moveToNextRound();
      players.forEach(player -> player.setChecked(false));
    }
  }

  private void moveToNextRound() {
    switch (state) {
      case PRE_FLOP -> moveGameStateFromPreFlopToFlop();
      case FLOP -> moveGameStateFromFlopToTurn();
      case TURN -> moveGameStateFromTurnToRiver();
      case RIVER -> gameHasEnded();
    }
    //    if(!state.equals(GameState.ENDED)) determineNextPlayer();
  }

  private void gameHasEnded() {
    moveGameStateFromRiverToEnd();
    determineWinner();
  }

  private void determineWinner() {
    winner = currentPlayer;
    winner.ifPresent(
        player -> {
          player.addCash(pot);
          pot = 0;
        });
    winnerHand.addAll(winner.get().getHandCards());
  }

  private Integer determineCurrentMinimumBet() {
    return players.stream().map(Player::getBet).min(Integer::compareTo).orElse(0);
  }

  private void moveGameStateFromRiverToEnd() {
    determineNextPlayer();
    state = ENDED;
  }

  private void moveGameStateFromTurnToRiver() {
    drawOneCommunityCard();
    determineNextPlayer();
    state = RIVER;
  }

  private void moveGameStateFromFlopToTurn() {
    drawOneCommunityCard();
    state = TURN;
    determineNextPlayer();
  }

  private void drawOneCommunityCard() {
    communityCards.add(deckSupplier.get().draw());
  }

  private void determineGameStateWhenAPlayerFolds() {
    long remainingActivePlayers = players.stream().filter(Player::isActive).count();
    if (remainingActivePlayers > 1) {
      determineNextPlayer();
    } else {
      endGameAndDeclareWinnerWhenEveryoneHasFoldedExceptOnePlayer();
    }
  }

  private void endGameAndDeclareWinnerWhenEveryoneHasFoldedExceptOnePlayer() {
    state = ENDED;
    determineNextPlayer();
    winner = currentPlayer;
    winner.ifPresent(
        player -> {
          player.addCash(pot);
          pot = 0;
          player.setHandCards(Collections.emptyList());
        });
  }

  private void validateCheckAction() {
    Integer currentPlayerPreviousBet = bets.get(currentPlayer.get().getId());
    Integer currentMaximumBet = determineCurrentMaximumBet();
    if (currentPlayerPreviousBet < currentMaximumBet) {
      throw new IllegalActionException(
          "Can not perform check action. You have an outstanding bet amount of "
              + determineCallAmount());
    }
  }

  private void validateRaiseAction(int amount, int callAmount) {
    boolean sufficientFunds = amount <= currentPlayer.get().getCash();
    if (!sufficientFunds) {
      throw new IllegalAmountException("Raise amount cannot be more than player's cash");
    }

    boolean sufficientRaiseAmount = amount > callAmount;
    if (!sufficientRaiseAmount) {
      throw new IllegalAmountException("Raise amount must be at greater than " + callAmount);
    }

    boolean isAmountTooHigh = players.stream().anyMatch(player -> player.getCash() < amount);
    if (isAmountTooHigh) {
      throw new IllegalAmountException(
          "Raise amount cannot be more than other player's remaining cash");
    }
  }

  private void validateCallAction() {
    boolean noneOfPreviousPlayersRaised = players.stream().noneMatch(Player::isRaised);
    if (noneOfPreviousPlayersRaised) {
      throw new IllegalActionException(
          "Can not perform call action. None of the previous players raised");
    }
  }

  private Integer determineCurrentMaximumBet() {
    return players.stream().map(Player::getBet).max(Integer::compareTo).orElse(0);
  }

  private void moveGameStateFromPreFlopToFlop() {
    communityCards.addAll(deckSupplier.get().drawThreeCommunityCards());
    state = FLOP;
  }

  private int determineCallAmount() {
    int previousBetAmountPlacedByCurrentPlayer = bets.getOrDefault(currentPlayer.get().getId(), 0);
    Integer currentMaximumBet = determineCurrentMaximumBet();
    return currentMaximumBet - previousBetAmountPlacedByCurrentPlayer;
  }

  private void placeBet(int amount) {
    currentPlayer.get().bet(amount);
    pot += amount;
  }

  private void determineNextPlayer() {
    currentPlayer.ifPresentOrElse(
        player -> {
          assignNextPlayerAsNewCurrentPlayer(player);
          determineAnotherCurrentPlayerIfNewCurrentPlayerIsInActive();
        },
        () -> currentPlayer = players.stream().findFirst());
  }

  private void assignNextPlayerAsNewCurrentPlayer(Player player) {
    int currentPlayerIndex = players.indexOf(player);
    int nextPlayerIndex = (currentPlayerIndex + 1) % players.size();
    currentPlayer = Optional.of(players.get(nextPlayerIndex));
  }

  private void determineAnotherCurrentPlayerIfNewCurrentPlayerIsInActive() {
    if (!currentPlayer.get().isActive()) {
      determineNextPlayer();
    }
  }

  protected void performRaiseAction(int amount) {
    validateRaiseAction(amount, determineCallAmount());
    placeBet(amount);
    bets.put(currentPlayer.get().getId(), amount);
    currentPlayer.ifPresent(player -> player.setRaised(true));
    determineNextPlayer();
  }

  protected void performCallAction() {
    int callAmount = determineCallAmount();
    validateCallAction();
    placeBet(callAmount);
    bets.computeIfPresent(
        currentPlayer.get().getId(),
        (key, value) -> value.equals(0) ? callAmount : value + callAmount);
    determineNextPlayer();
  }

  protected void performFoldAction() {
    currentPlayer.ifPresent(Player::setInactive);
    determineGameStateWhenAPlayerFolds();
  }

  protected void performCheckAction() {
    validateCheckAction();
    currentPlayer.ifPresent(player -> player.setChecked(true));
    determineNextPlayer();
  }
}
