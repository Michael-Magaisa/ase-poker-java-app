package com.sap.ase.poker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.sap.ase.poker.model.GameState;
import com.sap.ase.poker.model.IllegalActionException;
import com.sap.ase.poker.model.IllegalAmountException;
import com.sap.ase.poker.model.Player;
import com.sap.ase.poker.model.deck.Card;
import com.sap.ase.poker.model.deck.Deck;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TableServiceTest {

  @Mock Card cardMock;
  @Mock Deck deckMock;
  @Mock private Supplier<Deck> deckSupplierMock;
  private TableService cut;

  @BeforeEach
  void setUp() {
    lenient().when(deckSupplierMock.get()).thenReturn(deckMock);
    lenient().when(deckMock.draw()).thenReturn(cardMock);
    lenient()
        .when(deckMock.drawThreeCommunityCards())
        .thenReturn(List.of(cardMock, cardMock, cardMock));
    cut = new TableService(deckSupplierMock);
  }

  @Test
  void gameStateShouldBeOpenBeforeTheGameStarts() {
    assertThat(cut.getState()).isEqualTo(GameState.OPEN);
  }

  @Test
  void gameStateShouldBePreFlopWhenEnoughRequiredPlayersJoinTheTable() {
    givenTwoPlayersJoinTheGame();
    cut.start();
    assertThat(cut.getState()).isEqualTo(GameState.PRE_FLOP);
  }

  @Test
  void getPlayersShouldReturnEmptyListWhenNoPlayersJoinTheTable() {
    assertThat(cut.getPlayers()).isEmpty();
  }

  @Test
  void shouldGetEveryPlayerWhoHasJoinedTheTable() {
    givenTwoPlayersJoinTheGame();
    assertThat(cut.getPlayers())
        .extracting(Player::getId)
        .containsExactlyInAnyOrder("al-capone", "alice");
  }

  @Test
  void everyJoinedPlayerShouldBeInactiveByDefault() {
    givenTwoPlayersJoinTheGame();
    assertThat(cut.getPlayers()).extracting(Player::isActive).containsOnly(false);
  }

  @Test
  void shouldNotStartGameAndSetStatusToPreFlopWithoutEnoughPlayers() {
    Player player = new Player("al-capone", "Al", 100);
    cut.addPlayer(player.getId(), player.getName());
    cut.start();
    assertThat(cut.getState()).isEqualTo(GameState.OPEN);
  }

  @Test
  void shouldStartGameAndSetStatusToPreFlopWhenEnoughPlayersJoinTheTable() {
    givenTwoPlayersJoinTheGame();
    cut.start();
    assertThat(cut.getState()).isEqualTo(GameState.PRE_FLOP);
  }

  @Test
  void everyPlayerShouldGetDealtTwoCardsAtStart() {
    List<Player> players = givenThreePlayersJoinTheGame();
    cut.start();
    verify(deckMock, times(players.size() * 2)).draw();

    assertThat(cut.getPlayers())
        .extracting(Player::getHandCards)
        .allSatisfy(handCards -> assertThat(handCards).hasSize(2));
  }

  @Test
  void whenTheGameStartsEveryPlayerShouldBeSetToActiveAfterBeingDealtTheirTwoStartingCards() {
    givenThreePlayersJoinTheGame();
    cut.start();
    assertThat(cut.getPlayers()).extracting(Player::isActive).containsOnly(true);
  }

  @Test
  void shouldReturnOptionalEmptyWhenNoRoundHasStarted() {
    assertThat(cut.getCurrentPlayer()).isEmpty();
  }

  @Test
  void firstPlayerInTheListShouldBeTheInitialCurrentPlayer() {
    List<Player> players = givenThreePlayersJoinTheGame();
    cut.start();
    assertThat(cut.getCurrentPlayer().get())
        .extracting(Player::getId)
        .isEqualTo(players.get(0).getId());
  }

  @Test
  void getPlayerCardsShouldReturnTwoCardsJustAfterTheGameStarts() {
    givenThreePlayersJoinTheGame();
    cut.start();
    assertThat(cut.getPlayerCards("al-capone")).hasSize(2);
  }

  @Test
  void getPlayerCardsShouldReturnEmptyListWhenGameHasNotStarted() {
    givenThreePlayersJoinTheGame();
    assertThat(cut.getPlayerCards("al-capone")).isEmpty();
  }

  @Test
  void getCommunityCardsShouldReturnEmptyListInPreFlop() {
    givenThreePlayersJoinTheGame();
    cut.start();
    assertThat(cut.getState()).isEqualTo(GameState.PRE_FLOP);
    assertThat(cut.getCommunityCards()).isEmpty();
  }

  @Test
  void checkShouldSetCurrentPlayerToNextPlayerInThePlayersList() {
    givenThreePlayersJoinTheGame();
    cut.start();
    assertThat(cut.getCurrentPlayer().get().getId()).isEqualTo("al-capone");
    cut.performAction("check", 0);
    assertThat(cut.getCurrentPlayer().get().getId()).isEqualTo("alice");
    cut.performAction("check", 0);
    assertThat(cut.getCurrentPlayer().get().getId()).isEqualTo("bob");
  }

  @Test
  void onceEveryPlayerHasCheckedDuringPreFlopThreeCommunityCardsShouldBeDrawn() {
    List<Player> players = givenThreePlayersJoinTheGame();
    cut.start();
    players.forEach(player -> cut.performAction("check", 0));
    assertThat(cut.getCommunityCards()).hasSize(3);
  }

  @Test
  void onceEveryPlayerChecksDuringPreFlopGameStateShouldBeConvertedToFlop() {
    List<Player> players = givenThreePlayersJoinTheGame();
    cut.start();
    players.forEach(player -> cut.performAction("check", 0));
    assertThat(cut.getState()).isEqualTo(GameState.FLOP);
  }

  @Test
  void allPlayersShouldResetCheckStatusAfterRoundIsFinished() {
    List<Player> players = givenThreePlayersJoinTheGame();
    cut.start();
    players.forEach(player -> cut.performAction("check", 0));
    assertThat(cut.getPlayers()).extracting(Player::isChecked).containsOnly(false);
  }

  @Test
  void shouldNotResetCheckStatusUnlessRoundIsOver() {
    givenThreePlayersJoinTheGame();
    cut.start();
    cut.performAction("check", 0);
    cut.performAction("check", 0);
    assertThat(cut.getPlayers()).extracting(Player::isChecked).containsSequence(true, true, false);
  }

  @Test
  void playerBetForRaiseActionShouldBeHigherThanCurrentBet() {
    givenThreePlayersJoinTheGame();
    cut.start();
    cut.performAction("raise", 50);
    assertThrows(IllegalAmountException.class, () -> cut.performAction("raise", 40));
  }

  @Test
  void shouldRaiseSuccessfullyIfRaiseAmountHigherThanCurrentBet() {
    givenThreePlayersJoinTheGame();
    cut.start();

    cut.performAction("raise", 40);
    assertThat(cut.getBets().values().stream().max(Integer::compareTo).orElse(0)).isEqualTo(40);

    cut.performAction("raise", 60);
    assertThat(cut.getBets().values().stream().max(Integer::compareTo).orElse(0)).isEqualTo(60);
  }

  @Test
  void shouldDeductBetAmountFromPlayersCash() {
    givenThreePlayersJoinTheGame();
    cut.start();
    cut.performAction("raise", 50);
    assertThat(cut.getPlayers().stream().findFirst().get())
        .extracting(Player::getCash)
        .isEqualTo(50);
  }

  @Test
  void throwIllegalAmountExceptionWhenRaiseAmountIsGreaterThanCurrentPlayerCash() {
    givenThreePlayersJoinTheGame();
    cut.start();
    assertThrows(IllegalAmountException.class, () -> cut.performAction("raise", 101));
  }

  @Test
  void throwIllegalAmountExceptionWhenRaiseAmountIsGreaterThanOtherPlayersCash() {
    givenThreePlayersJoinTheGame();
    cut.start();
    cut.performAction("raise", 50);
    assertThrows(IllegalAmountException.class, () -> cut.performAction("raise", 90));
  }

  @Test
  void foldActionShouldSetPlayerStateToInActive() {
    givenThreePlayersJoinTheGame();
    cut.start();
    cut.performAction("fold", 0);
    assertThat(cut.getPlayers()).extracting(Player::isActive).containsSequence(false, true, true);
  }

  @Test
  void
      foldActionShouldSetGameStateToEndedIfOnlyOnePlayerRemainsAndRemainingPlayerIsDeclaredWinner() {
    givenThreePlayersJoinTheGame();
    cut.start();
    cut.performAction("fold", 0);
    cut.performAction("fold", 0);
    assertThat(cut.getState()).isEqualTo(GameState.ENDED);
    assertThat(cut.getWinner().get()).extracting(Player::getId).isEqualTo("bob");
    assertThat(cut.getWinnerHand())
        .isEqualTo(
            cut.getPlayers().stream()
                .filter(Player::isActive)
                .findFirst()
                .map(Player::getHandCards)
                .get());
    assertThat(cut.getPot()).isZero();
    assertTrue(cut.isRoundIsComplete());
  }

  @Test
  void callActionShouldOnlyBePerformedWhenOneOfThePreviousPlayersHasRaised() {
    givenThreePlayersJoinTheGame();
    cut.start();
    cut.performAction("check", 0);
    cut.performAction("check", 0);
    assertThrows(IllegalActionException.class, () -> cut.performAction("call", 20));
  }

  @Test
  void callShouldDeductCallAmountFromPlayerCashAndMatchMaximumBetSize() {
    givenThreePlayersJoinTheGame();
    cut.start();
    cut.performAction("raise", 10);
    cut.performAction("raise", 20);
    cut.performAction("call", 20);
    assertThat(cut.getPlayers()).extracting(Player::getCash).containsSequence(90, 80, 80);
  }

  @Test
  void callAmountShouldMatchAllSubSequentRaises() {
    givenThreePlayersJoinTheGame();
    cut.start();
    playFirstIteration();
    playSecondIterationAndReachConsensus();
    assertThat(cut.getPlayers()).extracting(Player::getCash).containsSequence(70, 70, 70);
  }

  @Test
  void shouldNotDeductAnyMoneyFromCurrentPlayer() {
    givenThreePlayersJoinTheGame();
    cut.start();
    playFirstIteration();
    playSecondIterationAndReachConsensus();
    assertThat(cut.getPlayers()).extracting(Player::getCash).containsSequence(70, 70, 70);
  }

  @Test
  void
      shouldThrowIllegalActionExceptionWhenOneTriesToCheckIfSomeoneOneHasBetMoreMoneyInTheCurrentRound() {
    givenThreePlayersJoinTheGame();
    cut.start();
    playFirstIteration();
    cut.performAction("call", 20);
    cut.performAction("raise", 20);
    assertThrows(IllegalActionException.class, () -> cut.performAction("check", 0));
  }

  @Test
  void determineAnotherCurrentPlayerIfCurrentPlayerFallsOnInActivePlayer() {
    givenThreePlayersJoinTheGame();
    cut.start();
    playFirstIteration();
    assertThat(cut.getCurrentPlayer()).isEqualTo(cut.getPlayers().stream().findFirst());
    playSecondIterationAndReachConsensus();
    assertThat(cut.getCurrentPlayer()).isEqualTo(cut.getPlayers().stream().findFirst());
    player2FoldsAndReachConsensusInThirdIteration();
    List<Player> players = cut.getPlayers();
    assertThat(players).extracting(Player::getCash).containsSequence(70, 50, 50);
    assertThat(cut.getPlayers()).extracting(Player::isActive).containsSequence(false, true, true);
    assertThat(cut.getCurrentPlayer()).isNotEqualTo(cut.getPlayers().stream().findFirst());
  }

  @Test
  void shouldThrowIllegalActionIfPlayerTriesUndefinedActions() {
    givenThreePlayersJoinTheGame();
    cut.start();
    assertThrows(IllegalActionException.class, () -> cut.performAction("undefined", 0));
  }

  @Test
  void onlyPerformActionWhenActionIsNotBlank() {
    givenThreePlayersJoinTheGame();
    cut.start();
    assertThrows(IllegalActionException.class, () -> cut.performAction("", 0));
  }

  @Test
  void moveFromTurnToRiver() {
    givenGameStateInTurn();
    iterationOfChecks();
    assertThat(cut.getState()).isEqualTo(GameState.RIVER);
  }

  @Test
  void moveFromRiverToEnded() {
    givenGameStateInTurn();
    iterationOfChecks();
    iterationOfChecks();
    assertThat(cut.getState()).isEqualTo(GameState.ENDED);
  }

  private void givenGameStateInTurn() {
    givenThreePlayersJoinTheGame();
    cut.start();
    playFirstIteration();
    playSecondIterationAndReachConsensus();
    iterationOfChecks();
  }

  private void iterationOfChecks() {
    cut.performAction("check", 0);
    cut.performAction("check", 0);
    cut.performAction("check", 0);
  }

  private void player2FoldsAndReachConsensusInThirdIteration() {
    cut.performAction("fold", 0);
    cut.performAction("raise", 20);
    cut.performAction("call", 0);
  }

  private void playSecondIterationAndReachConsensus() {
    cut.performAction("call", 20);
    cut.performAction("call", 10);
    cut.performAction("check", 0);
  }

  private void playFirstIteration() {
    cut.performAction("raise", 10);
    cut.performAction("raise", 20);
    cut.performAction("raise", 30);
  }

  private List<Player> givenTwoPlayersJoinTheGame() {
    List<Player> players =
        List.of(new Player("al-capone", "Al", 100), new Player("alice", "Alice", 100));
    players.forEach(player -> cut.addPlayer(player.getId(), player.getName()));
    return players;
  }

  private List<Player> givenThreePlayersJoinTheGame() {
    List<Player> players =
        List.of(
            new Player("al-capone", "Al", 100),
            new Player("alice", "Alice", 100),
            new Player("bob", "Bob", 100));
    players.forEach(player -> cut.addPlayer(player.getId(), player.getName()));
    return players;
  }
}
