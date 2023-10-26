package com.sap.ase.poker.smokeTests;

import static org.assertj.core.api.Assertions.assertThat;

import com.sap.ase.poker.dto.GetTableResponseDto;
import com.sap.ase.poker.dto.PlayerDto;
import com.sap.ase.poker.model.GameState;
import org.junit.jupiter.api.Test;

class CheckSmokeTest extends SmokeTest {
  // ██╗  ██╗ █████╗ ███╗   ██╗██████╗ ███████╗     ██████╗ ███████╗███████╗██╗██╗██╗
  // ██║  ██║██╔══██╗████╗  ██║██╔══██╗██╔════╝    ██╔═══██╗██╔════╝██╔════╝██║██║██║
  // ███████║███████║██╔██╗ ██║██║  ██║███████╗    ██║   ██║█████╗  █████╗  ██║██║██║
  // ██╔══██║██╔══██║██║╚██╗██║██║  ██║╚════██║    ██║   ██║██╔══╝  ██╔══╝  ╚═╝╚═╝╚═╝
  // ██║  ██║██║  ██║██║ ╚████║██████╔╝███████║    ╚██████╔╝██║     ██║     ██╗██╗██╗
  // ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═════╝ ╚══════╝     ╚═════╝ ╚═╝     ╚═╝     ╚═╝╚═╝╚═╝
  @Test
  void check_shouldAdvanceGameState() {
    addTwoPlayers();
    startGame();

    PlayerDto currentPlayerAtStart = getCurrentPlayerDto();

    performAction(checkActionAsString());

    GetTableResponseDto tableResponseDto = getTableResponseDtoForPlayer(ALICE_ID);
    assertThat(tableResponseDto.getCurrentPlayer())
        .usingRecursiveComparison()
        .isNotEqualTo(currentPlayerAtStart);
  }

  @Test
  void check_shouldAdvancePlayer() {
    addTwoPlayers();
    startGame();

    PlayerDto currentPlayerAtStart = getCurrentPlayerDto();

    performAction(checkActionAsString());

    GetTableResponseDto tableResponseDto = getTableResponseDtoForPlayer(ALICE_ID);
    assertThat(tableResponseDto.getCurrentPlayer())
        .usingRecursiveComparison()
        .isNotEqualTo(currentPlayerAtStart);
  }

  @Test
  void check_afterRaise_throwsIllegalActionException() {
    addTwoPlayers();
    startGame();

    PlayerDto currentPlayer = getCurrentPlayerDto();
    performAction(raiseActionAsString(10));

    PlayerDto currentPlayerAtStart = getCurrentPlayerDto();

    assertIllegalActionException(() -> performAction(checkActionAsString()));
  }

  @Test
  void check_shouldAdvanceState() {
    addTwoPlayers();
    startGame();

    PlayerDto startingPlayer = getCurrentPlayerDto();

    performAction(checkActionAsString());
    performAction(checkActionAsString());

    GetTableResponseDto tableResponseDto = getTableResponseDtoForPlayer(ALICE_ID);

    assertThat(tableResponseDto.getCurrentPlayer())
        .usingRecursiveComparison()
        .isEqualTo(startingPlayer);
    assertThat(tableResponseDto.getState()).isEqualTo(GameState.FLOP.getValue());
    assertThat(tableResponseDto.getCommunityCards()).hasSize(3);
  }
}
