package io.github.gjum.alkemy

import kotlin.math.E
import kotlin.math.exp

typealias Volume = Double // measured in milliliters
typealias Mass = Double
typealias Temperature = Double
typealias Energy = Double
// think: one unit of Temperature is one unit of Energy per one unit of Mass

const val ROOM_TEMPERATURE: Temperature = 300.0

fun calculateTemperature(energy: Energy, mass: Mass) = if (mass.nearZero) 0.0 else energy / mass * ROOM_TEMPERATURE

data class Element(val name: String, val nBonds: Int, val mass: Mass) : Comparable<Element> {
	override operator fun compareTo(other: Element) = name.toLowerCase().compareTo(other.name.toLowerCase())
}

class Atom(val element: Element) {
	var neighbors = mutableListOf<Atom>()
	val openBonds get() = element.nBonds - neighbors.size
}

enum class Phase { Liquid, Gas }

data class Molecule(val atoms: Collection<Atom>, val bondsConfig: BondsConfig) {
	val mass: Mass = atoms.sumByDouble { it.element.mass }

	val boilingTemperature: Temperature = mass * ROOM_TEMPERATURE / 3.75 // TODO calculate molecule boiling temperature

	fun phase(temperature: Temperature) = if (temperature <= boilingTemperature) Phase.Liquid else Phase.Gas

	fun volume(temperature: Temperature): Double {
		val massTimesBonds = atoms.sumByDouble { it.element.mass * it.element.nBonds }
		var volume = massTimesBonds * exp(temperature / ROOM_TEMPERATURE) * .625 / E
		if (phase(temperature) == Phase.Gas) volume *= 100
		return volume
	}

	val unbondedAtoms get() = atoms.filter { it.openBonds > 0 }
}
