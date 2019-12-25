package io.github.gjum.alkemy

data class Slag(val mass: Mass, val volume: Volume) {
	operator fun plus(other: Slag) = Slag(mass + other.mass, volume + other.volume)
	operator fun minus(other: Slag) = Slag(mass - other.mass, volume - other.volume)
	operator fun times(factor: Double) = Slag(mass * factor, volume * factor)
}

typealias Liquid = Mixture
typealias Gas = Mixture
typealias LiquidAndGas = Pair<Liquid?, Gas?>

val LiquidAndGas.liquid get() = first
val LiquidAndGas.gas get() = second

data class Mixture(
	val molecules: Map<Molecule, Double>, // mass of each molecule in this mixture
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
