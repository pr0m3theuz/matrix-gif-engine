package org.example

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.example.model.Model
import org.example.model.Strength
import org.example.model.getModel
import org.example.model.getStrength

data class AgentConfig(
    val model: Model = Model.MCTS,
    val strength: Strength = Strength.NULL,
    val timeControl: Boolean = false,
    val timeDuration: Duration = Duration.ZERO,
    val useRAVE: Boolean = false,
    val enableFPU: Boolean = false,
    val enablePW: Boolean = false,
    val iterations: Int = 0,
    val depth: Int = 1,
)

fun createAgentOptions(name: String, prefix: String, shortPrefix: String): List<Option> {

  val model =
      Option.builder(shortPrefix)
          .longOpt("$prefix-model")
          .hasArg(true)
          .argName("${prefix.uppercase()}-MODEL")
          .desc("AI Model of $name (default: Monte Carlo Tree Search)")
          .get()

  val strength =
      Option.builder("${shortPrefix}s")
          .longOpt("$prefix-strength")
          .hasArg(true)
          .argName("${prefix.uppercase()}-STRENGTH")
          .desc("Strength of $name (default: easy)")
          .get()

  val timeControl =
      Option.builder("${shortPrefix}tc")
          .longOpt("$prefix-time-control")
          .hasArg(true)
          .argName("${prefix.uppercase()}-TIME-CONTROL")
          .desc("Time in milliseconds given to $name search for a move")
          .get()

  val timeDuration =
      Option.builder("${shortPrefix}td")
          .longOpt("$prefix-time-duration")
          .hasArg(true)
          .argName("${prefix.uppercase()}-TIME-DURATION")
          .desc("Time spent searching for a move")
          .get()

  val rave =
      Option.builder("${shortPrefix}rv")
          .longOpt("$prefix-mcts-rave")
          .hasArg(false)
          .desc("Enable MCTS RAVE for $name (default: false)")
          .get()

  val fpu =
      Option.builder("${shortPrefix}fpu")
          .longOpt("$prefix-mcts-fpu")
          .hasArg(false)
          .desc("Enable First Play Urgency for $name (default: false)")
          .get()

  val pw =
      Option.builder("${shortPrefix}pw")
          .longOpt("$prefix-mcts-pw")
          .hasArg(false)
          .desc("Enable MCTS Progressive Widening for $name (default: false)")
          .get()

  val iterations =
      Option.builder("${shortPrefix}i")
          .longOpt("$prefix-mcts-iterations")
          .hasArg(true)
          .argName("${prefix.uppercase()}-ITERATIONS")
          .desc("Number of MCTS Iterations for $name (default: 0)")
          .get()

  val depth =
      Option.builder("${shortPrefix}dp")
          .longOpt("$prefix-minimax-depth")
          .hasArg(true)
          .argName("${prefix.uppercase()}-DEPTH")
          .desc("Depth of Minimax Search for $name (default: 1)")
          .get()

  return listOf(model, strength, timeControl, timeDuration, rave, fpu, pw, iterations, depth)
}

fun parseAgentArgs(
    cmd: CommandLine,
    name: String,
    prefix: String,
    shortPrefix: String,
): AgentConfig {
  val model = getModel(cmd.getOptionValue("$prefix-model"))

  val strength =
      if (cmd.hasOption("$prefix-strength")) getStrength(cmd.getOptionValue("$prefix-strength"))
      else Strength.NULL

  val timeControl = cmd.hasOption("$prefix-time-control")

  val timeDuration =
      if (cmd.hasOption("$prefix-time-duration"))
          cmd.getOptionValue("$prefix-time-duration").toInt().milliseconds
      else Duration.ZERO

  val raveMode = cmd.hasOption("$prefix-mcts-rave")
  val fpu = cmd.hasOption("$prefix-mcts-fpu")
  val pw = cmd.hasOption("$prefix-mcts-pw")

  val iterations =
      if (cmd.hasOption("$prefix-mcts-iterations")) cmd.getOptionValue("$prefix-mcts-iterations").toInt()
      else 0

  val depth =
      if (cmd.hasOption("$prefix-minimax-depth")) cmd.getOptionValue("$prefix-minimax-depth").toInt() else 1

  return AgentConfig(
      model = model,
      strength = strength,
      timeControl = timeControl,
      timeDuration = timeDuration,
      useRAVE = raveMode,
      enableFPU = fpu,
      enablePW = pw,
      iterations = iterations,
      depth = depth,
  )
}

fun createCLIOptions(): Options {
  val options =
      Options().apply {
        addOption(
            Option.builder("b")
                .longOpt("batch")
                .hasArg(false)
                //                .argName("batch")
                .desc(
                    "Normal batch run: java -Xmx48g -jar app.jar run -b -g <TOTAL_GAMES> -p <PARALLELISM> -s <SEED>" +
                        " -m <AGENT-1-MODEL> -ms <AGENT-1-STRENGTH> -M <AGENT-2-MODEL> -MS <AGENT-2-DIFFICULTY>"
                )
                .get()
        )
        addOption(
            Option.builder("r")
                .longOpt("replay")
                .hasArg(false)
                //                .argName("REPLAY")
                .desc(
                    "Replay a single failed game (verbose, single-threaded): java -jar app.jar -r -G <GAME-ID> -s <SEED> -v <VERBOSE>" +
                        " -m <AGENT-1-MODEL> -ms <AGENT-1-DIFFICULTY> -M <AGENT-2-MODEL> -MS <AGENT-2-DIFFICULTY>"
                )
                .get()
        )
        addOption(
            Option.builder("g")
                .longOpt("total-games")
                .hasArg()
                .argName("COUNT")
                .desc("Total number of games (default: 5000)")
                .get()
        )
        addOption(
            Option.builder("G")
                .longOpt("game-id")
                .hasArg()
                .argName("GAME-ID")
                .desc("ID of the game to replay")
                .get()
        )
        addOption(
            Option.builder("p")
                .longOpt("parallelism")
                .hasArg()
                .argName("THREADS")
                .desc("Number of parallel threads (default: cores - 2)")
                .get()
        )
        addOption(
            Option.builder("s")
                .longOpt("seed")
                .hasArg()
                .argName("SEED")
                .desc("Base seed for run reproducibility (default: current system time)")
                .get()
        )

        addOption(
            Option.builder("v")
                .longOpt("verbose")
                .hasArg()
                .argName("VERBOSE")
                .desc(
                    "If verbose is true, prints full state summaries each turn — use this only for" +
                        " single-game replay (default: false)"
                )
                .get()
        )

        addOption("h", "help", false, "Print this help message")

        createAgentOptions("Agent 1 (White)", "agent-1", "m").forEach {
          addOption(it)
        }
        createAgentOptions("Agent 2 (Black)", "agent-2", "M").forEach {
          addOption(it)
        }
      }
  return options
}
