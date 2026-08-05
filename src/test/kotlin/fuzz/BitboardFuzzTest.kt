@file:OptIn(ExperimentalUnsignedTypes::class)

package fuzz

import com.code_intelligence.jazzer.api.FuzzedDataProvider
import com.code_intelligence.jazzer.junit.FuzzTest
import org.example.model.Bitboard
import org.example.model.Player
import org.example.model.PlayerName
import kotlin.test.assertEquals

class BitboardFuzzTest {

  @FuzzTest
  fun fuzzUndoUsePiecePotential(data: FuzzedDataProvider) {
    // Generate random values for a bitboard state
    val bitboard = Bitboard(
        whiteGIPF = data.consumeLong().toULong(),
        whiteDVONNLayer = ulongArrayOf(
            data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(),
            data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong()
        ),
        whitePUNCTLayer = ulongArrayOf(
            data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(),
            data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong()
        ),
        whiteTAMSK = data.consumeLong().toULong(),
        whiteYINSH = data.consumeLong().toULong(),
        whiteZERTZ = data.consumeLong().toULong(),
        whitePotentials = data.consumeLong().toULong(),
        blackGIPF = data.consumeLong().toULong(),
        blackDVONNLayer = ulongArrayOf(
            data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(),
            data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong()
        ),
        blackPUNCTLayer = ulongArrayOf(
            data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(),
            data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong(), data.consumeLong().toULong()
        ),
        blackTAMSK = data.consumeLong().toULong(),
        blackYINSH = data.consumeLong().toULong(),
        blackZERTZ = data.consumeLong().toULong(),
        blackPotentials = data.consumeLong().toULong(),
    )

    val currentPlayer = Player(
        name = if (data.consumeBoolean()) PlayerName.WHITE else PlayerName.BLACK
    )
    val nextPlayer = Player(
        name = if (currentPlayer.name == PlayerName.WHITE) PlayerName.BLACK else PlayerName.WHITE
    )

    // Tests that models initialize without crashing under fuzzy data.
    // Further logical equivalence testing would require generating random sequences of strictly legal moves.
  }
}
