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

	val boilingTemperature: Temperature =
		mass * ROOM_TEMPERATURE / 3.75 // TODO calculate molecule boiling temperature

	fun phase(temperature: Temperature) = if (temperature <= boilingTemperature) Phase.Liquid else Phase.Gas

	fun volume(temperature: Temperature): Double {
		val massTimesBonds = atoms.sumByDouble { it.element.mass * it.element.nBonds }
		var volume = massTimesBonds * exp(temperature / ROOM_TEMPERATURE) * .625 / E
		if (phase(temperature) == Phase.Gas) volume *= 100
		return volume
	}

	val unbondedAtoms get() = atoms.filter { it.openBonds > 0 }
}

data class Slag(val mass: Mass, val volume: Volume) {
	operator fun plus(other: Slag) = Slag(mass + other.mass, volume + other.volume)
	operator fun minus(other: Slag) = Slag(mass * -other.mass, volume - other.volume)
	operator fun times(factor: Double) = Slag(mass * factor, volume * factor)
}

data class Mixture(
	val molecules: Map<Molecule, Double>,
	val slag: Slag,
	val thermalEnergy: Energy
) {
	val mass = slag.mass + molecules.entries.sumByDouble { (m, n) -> n * m.mass }
	val temperature = calculateTemperature(thermalEnergy, mass)
	val volume = slag.volume + molecules.entries.sumByDouble { (m, n) -> n * m.volume(temperature) }

	fun addThermalEnergy(addedEnergy: Energy): LiquidAndGas {
		val newEnergy = addedEnergy + thermalEnergy
		val temperature = calculateTemperature(newEnergy, mass)

		val afterReaction = molecules // XXX run reaction

		// separate liquid and gaseous phases
		val liquidMap = mutableMapOf<Molecule, Double>()
		val gasMap = mutableMapOf<Molecule, Double>()
		for ((molecule, mMass) in afterReaction.entries) {
			if (molecule.phase(temperature) == Phase.Liquid) {
				liquidMap[molecule] = mMass
			} else {
				gasMap[molecule] = mMass
			}
		}
		val liquidFactor = liquidMap.entries.sumByDouble { (m, n) -> n * m.mass } / mass
		val gasFactor = gasMap.entries.sumByDouble { (m, n) -> n * m.mass } / mass
		val liquidSlag = slag // TODO distribute energy across both slags
		val liquid = Liquid(liquidMap, liquidSlag, newEnergy * liquidFactor)
		val gas = Gas(gasMap, slag - liquid.slag, newEnergy * gasFactor)
		// delete traces
		return liquid.tracesToSlag() to gas.tracesToSlag()
	}

	/**
	 * Increment the value at `key` by `increment`.
	 * Start at 0.0 if `key` is unset.
	 */
	private fun <K> MutableMap<K, Double>.incrementBy(key: K, increment: Double) {
		this[key] = increment + (this[key] ?: 0.0)
	}

	/**
	 * Does not trigger a reaction from resulting molecules.
	 * The caller has to take care of triggering a reaction at a suitable time later.
	 */
	fun combine(other: Mixture?): Mixture {
		if (other == null) return this
		val updatedThis = mutableMapOf<Molecule, Double>()
		for ((moleculeThis, massThis) in this.molecules.entries) {
			val massOther = other.molecules[moleculeThis] ?: 0.0
			updatedThis[moleculeThis] = massThis + massOther
		}
		val combinedMolecules = other.molecules + updatedThis
		val combinedSlag = slag + other.slag
		val combinedEnergy = thermalEnergy + other.thermalEnergy
		val combinedMix = Mixture(combinedMolecules, combinedSlag, combinedEnergy)
		return combinedMix.tracesToSlag()
	}

	private fun tracesToSlag(keepNumMolecules: Int = 100): Mixture {
		if (molecules.size < keepNumMolecules) return this
		val newMolecules = mutableMapOf<Molecule, Double>()
		molecules.entries.asSequence()
			.sortedByDescending { (m, n) -> n * m.mass }
			.take(keepNumMolecules)
			.forEach { (m, n) -> newMolecules[m] = n }
		val newMass = newMolecules.entries.sumByDouble { (m, n) -> n * m.mass }
		val newVolume = newMolecules.entries.sumByDouble { (m, n) -> n * m.volume(temperature) }
		val newSlag = Slag(slag.mass + mass - newMass, slag.volume + volume - newVolume)
		return Mixture(newMolecules, newSlag, thermalEnergy)
	}

	fun splitByVolume(splitVolume: Volume): Pair<Mixture, Mixture?> {
		val ratio = splitVolume / volume
		if (ratio >= 1) return this to null
		val moleculesA = mutableMapOf<Molecule, Double>()
		val moleculesB = mutableMapOf<Molecule, Double>()
		for ((molecule, mMass) in molecules) {
			val massA = mMass * ratio
			moleculesA[molecule] = massA
			moleculesB[molecule] = mMass - massA
		}
		val mixA = Mixture(moleculesA, slag * ratio, thermalEnergy * ratio)
		val mixB = Mixture(moleculesB, slag - mixA.slag, thermalEnergy - mixA.thermalEnergy)
		return mixA to mixB
	}
}

typealias Liquid = Mixture
typealias Gas = Mixture
typealias LiquidAndGas = Pair<Liquid?, Gas?>

val LiquidAndGas.liquid get() = first
val LiquidAndGas.gas get() = second

data class ReactionResult(
	val liquidFromGas: Liquid?,
	val gasFromLiquid: Gas?,
	val getOverflow: () -> LiquidAndGas
)

sealed class Container(
	val volumeMax: Volume,
	var liquid: Liquid?,
	var gas: Gas?
) {
	val combinedMass get() = (liquid?.mass ?: .0) + (gas?.mass ?: .0)
	val thermalEnergy get() = (liquid?.thermalEnergy ?: .0) + (gas?.thermalEnergy ?: .0)
	val temperature get() = calculateTemperature(thermalEnergy, combinedMass)

	fun addLiquid(mixture: Liquid): ReactionResult {
		liquid = liquid?.combine(mixture) ?: mixture
		// trigger reaction
		return addThermalEnergy(.0)
	}

	fun heatTo(temperature: Temperature): ReactionResult {
		val thermalEnergyRequired = combinedMass * temperature / ROOM_TEMPERATURE
		return addThermalEnergy(thermalEnergyRequired - thermalEnergy)
	}

	fun addThermalEnergy(energy: Energy): ReactionResult {
		return distributeOverflow(
			liquid?.addThermalEnergy(energy),
			gas?.addThermalEnergy(energy)
		)
	}

	abstract fun distributeOverflow(liquidResults: LiquidAndGas?, gasResults: LiquidAndGas?): ReactionResult

	class Flask(volume: Volume, liquid: Liquid?) : Container(volume, liquid, null) {
		override fun distributeOverflow(liquidResults: LiquidAndGas?, gasResults: LiquidAndGas?): ReactionResult {
			val liquidCombined = liquidResults?.liquid?.combine(gasResults?.liquid)
			val (liquidKept, liquidOverflow) = liquidCombined?.splitByVolume(volumeMax) ?: liquidCombined to null
			liquid = liquidKept
			// TODO react combined liquid

			val getGasOverflow = { gasResults?.gas?.combine(liquidResults?.gas) }
			gas = null

			return ReactionResult(gasResults?.liquid, liquidResults?.gas) { liquidOverflow to getGasOverflow() }
		}
	}

	class Canister(volume: Volume, liquid: Liquid?, gas: Gas?) : Container(volume, liquid, gas) {
		override fun distributeOverflow(liquidResults: LiquidAndGas?, gasResults: LiquidAndGas?): ReactionResult {
			val liquidCombined = liquidResults?.liquid?.combine(gasResults?.liquid)
			val (liquidKept, liquidOverflow) = liquidCombined?.splitByVolume(volumeMax) ?: liquidCombined to null
			liquid = liquidKept
			// TODO react combined liquid

			val getGasCombined = { gasResults?.gas?.combine(liquidResults?.gas) }

			val volumeLeftForGas = volumeMax - (liquid?.volume ?: 0.0)
			val (gasKept, getGasOverflow) = if (volumeLeftForGas <= 0) {
				null to getGasCombined // all gas escapes; skip reaction
			} else {
				// some gas remains in the canister; need to calculate the reaction result
				val gasCombined = getGasCombined()
				// TODO react combined gas
				val (gasKept, gasOverflow) = gasCombined?.splitByVolume(volumeLeftForGas) ?: gasCombined to null
				gasKept to { gasOverflow }
			}
			gas = gasKept
			return ReactionResult(gasResults?.liquid, liquidResults?.gas) { liquidOverflow to getGasOverflow() }
		}
	}
}

/**
 * leftNeighbors and rightNeighbors must be sorted.
 */
class BondSpec(
	val strength: Int,
	val leftElem: Element,
	val rightElem: Element,
	val leftNeighbors: Collection<Element>?,
	val rightNeighbors: Collection<Element>?
) {
	val children = mutableListOf<BondSpec>()

	/**
	 * Recurses the exception tree until the best match is found.
	 * Returns null if none match.
	 */
	fun matchingBondSpec(left: Atom, right: Atom): BondSpec? {
		// check wrong order
		if (left.element > right.element) return matchingBondSpec(right, left)
		// check matches this bond
		if (leftElem != left.element || rightElem != right.element) return null
		if (leftNeighbors != null && leftNeighbors.isNotEmpty()) {
			val actualNb = left.neighbors
				.removeOne(right)
				.sortedBy { it.element }
			for ((actual, expected) in actualNb.zip(leftNeighbors)) {
				if (actual.element != expected) return null
			}
		}
		if (rightNeighbors != null && rightNeighbors.isNotEmpty()) {
			val actualNb = right.neighbors
				.removeOne(left)
				.sortedBy { it.element }
			for ((actual, expected) in actualNb.zip(rightNeighbors)) {
				if (actual.element != expected) return null
			}
		}
		// check child preferred
		for (child in children) {
			val childMatch = child.matchingBondSpec(left, right)
			if (childMatch != null) return childMatch
		}
		// no child preferred, this is best match
		return this
	}

	override fun toString(): String {
		val lnStr = leftNeighbors?.joinToString("") { it.name }?.let { it + ',' } ?: ""
		val rnStr = rightNeighbors?.joinToString("") { it.name }?.let { ',' + it } ?: ""
		val bondStr = lnStr + leftElem.name + '-' + rightElem.name + rnStr + ':' + strength
		return "BondSpec{$bondStr}"
	}
}

typealias BondsConfig = Collection<BondSpec>

fun BondsConfig.matchingBondSpec(left: Atom, right: Atom): BondSpec? {
	return asSequence().map { it.matchingBondSpec(left, right) }.firstOrNull()
}

data class World(
	val elements: Map<String, Element>,
	val bondsConfig: BondsConfig
)

fun parseConfig(text: String): World {
	val elements = mutableMapOf<String, Element>()
	val bondsConfig = mutableListOf<BondSpec>() // top level
	var bondsStack = mutableListOf<BondSpec>() // for tree building
	var state = "?"
	for ((lineNr, line) in text.lines().withIndex()) {
		if (line.startsWith('@')) {
			state = line.substring(1)
		} else when (state) {
			"atoms" -> {
				val (name, nBonds, mass) = line.split(':')
				elements[name] = Element(name, nBonds.toInt(), mass.toDouble())
			}
			"bonds" -> {
				val trimmedLine = line.trimStart(' ')
				val indent = line.length - trimmedLine.length

				val spec = parseBondSpec(trimmedLine, elements, lineNr)

				// go to correct level in tree
				if (bondsStack.size > indent) bondsStack = bondsStack.take(indent).toMutableList()
				bondsStack.lastOrNull()?.apply {
					children.add(spec) // insert spec into tree
				} ?: bondsConfig.add(spec) // top level spec
				// put spec on stack for future lines containing children
				bondsStack.add(spec)
			}
//			else -> {
//				if (line.trim().isNotEmpty()) println("In state $state: skipping line $line")
//			}
		}
	}

	return World(elements, bondsConfig)
}

private fun parseBondSpec(
	trimmedLine: String,
	elements: MutableMap<String, Element>,
	lineNr: Int
): BondSpec {
	val (specStr, strengthStr) = trimmedLine.split(':')
	val (leftStr, rightStr) = specStr.split('-')
	val (leftNb, leftElem) = if (',' in leftStr) {
		val (leftNbStr, leftElem) = leftStr.split(',')
		val leftNb = leftNbStr.toList()
		leftNb to leftElem
	} else {
		null to leftStr
	}
	val (rightElem, rightNb) = if (',' in rightStr) {
		val (rightElem, rightNbStr) = rightStr.split(',')
		val rightNb = rightNbStr.toList()
		rightElem to rightNb
	} else {
		rightStr to null
	}

	fun String.toElement() = elements[this]
		?: error("No such element $this in line $lineNr")

	fun List<Char>.toElements() = map { e ->
		elements[e.toString()] ?: error("No such element $e in line $lineNr")
	}

	return BondSpec(
		strengthStr.toInt(),
		leftElem.toElement(),
		rightElem.toElement(),
		leftNb?.toElements()?.sorted(),
		rightNb?.toElements()?.sorted()
	)
}
