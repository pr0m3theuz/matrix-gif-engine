package org.example.engine

import ncsa.hdf.hdf5lib.H5
import ncsa.hdf.`object`.Dataset
import ncsa.hdf.`object`.h5.H5Datatype
import ncsa.hdf.`object`.h5.H5File
import org.example.model.State
import javax.swing.Action

data class ExperienceCollector(
	val states: MutableList<State> = mutableListOf(),
	val actions: MutableList<PossibleBitMove> = mutableListOf(),
//	val rewards: MutableList<> = mutableListOf(),
	val currentEpisodeStates: MutableList<State> = mutableListOf(),
	val currentEpisodeActions: MutableList<PossibleBitMove> = mutableListOf(),
) {
	fun beginEpisode() {

	}

//	fun endEpisode(reward: ) {}

	fun recordDecision(state: State, action: Action) {

	}

	fun toBuffer(): ExperienceBuffer {
		return ExperienceBuffer(
			states,
			actions,
//			rewards
		)
	}

}

data class ExperienceBuffer(
	val states: MutableList<State> = mutableListOf(),
	val actions: MutableList<PossibleBitMove> = mutableListOf(),
	val rewards: MutableList<String> = mutableListOf(),
) {
	fun serialize(h5File: H5File) {
		val experience = h5File.createGroup("experience", null)

//		val dataset = h5File.createScalarDS("states", experience, H5Datatype.)
	}

	fun loadExperience() {

	}
}