package org.example

import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

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
        Option.builder("m")
          .longOpt("agent-1-model")
          .hasArg(true)
          .argName("AGENT-1-MODEL")
          .desc("AI Model of Agent 1 (white) (default: Monte Carlo Tree Search)")
          .get()
      )
      addOption(
        Option.builder("ms")
          .longOpt("agent-1-strength")
          .hasArg(true)
          .argName("AGENT-1-STRENGTH")
          .desc("Strength of Agent 1 (white) (default: easy)")
          .get()
      )
      addOption(
        Option.builder("mtc")
          .longOpt("agent-1-time-control")
          .hasArg(false)
          .argName("AGENT-1-TIME-CONTROL")
          .desc("Time Given to Agent 1 search for a move (white)")
          .get()
      )
      addOption(
        Option.builder("mrv")
          .longOpt("agent-1-mcts-rave")
          .hasArg(false)
          //            .argName("AGENT-1-MCTS-RAVE")
          .desc("Enable MCTS RAVE for Agent 1 (white) (default: false)")
          .get()
      )
      addOption(
        Option.builder("M")
          .longOpt("agent-2-model")
          .hasArg(true)
          .argName("AGENT-2-MODEL")
          .desc("AI Model of Agent 2 (black) (default: Monte Carlo Tree Search)")
          .get()
      )
      addOption(
        Option.builder("MS")
          .longOpt("agent-2-strength")
          .hasArg(true)
          .argName("AGENT-2-STRENGTH")
          .desc("Strength of Agent 2 (white) (default: easy)")
          .get()
      )
      addOption(
        Option.builder("MTC")
          .longOpt("agent-2-time-control")
          .hasArg(false)
          //            .argName("AGENT-2-TIME-CONTROL")
          .desc("Time Given to Agent 2 search for a move(black)")
          .get()
      )
      addOption(
        Option.builder("MRV")
          .longOpt("agent-2-mcts-rave")
          .hasArg(false)
          //            .argName("AGENT-2-MCTS-RAVE")
          .desc("Enable MCTS RAVE for Agent 2 (black)")
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
    }
  return options
}