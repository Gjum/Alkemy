package io.github.gjum.alkemy

typealias Volume = Int // measured in milliliters
typealias ThermalEnergy = Int
typealias Mass = Int
typealias Temperature = Int // think: one Kelvin is one unit of ThermalEnergy per one unit of Mass

const val ROOM_TEMPERATURE: Temperature = 50

data class Element(val name: String, val mass: Mass)

data class Atom(val type: Element, val bonds: Collection<Atom>)

data class Molecule(val atoms: Collection<Atom>) {
	val mass: Mass get() = atoms.sumBy { mass }
	val volume: Volume get() = TODO("calculate molecule volume")
	val boilingTemperature: Temperature get() = TODO("calculate molecule boiling temperature")
}

data class Slag(val mass: Mass, val volume: Volume, val thermalEnergy: ThermalEnergy)

class Mixture(val molecules: Map<Molecule, Int>, val thermalEnergy: ThermalEnergy) {
	val mass = molecules.entries.map { (m, n) -> n * m.mass }.sum()
	val volume = molecules.entries.map { (m, n) -> n * m.volume }.sum()

	fun addThermalEnergy(addedEnergy: ThermalEnergy): LiquidAndGas {
		// TODO react liquid at temperature
		return this to null
	}

	fun combine(other: Mixture?): Mixture {
		if (other == null) return this
		val aUpdated = mutableMapOf<Molecule, Int>()
		for ((aMolecule, aCount) in this.molecules.entries) {
			val bCount = other.molecules[aMolecule] ?: 0
			aUpdated[aMolecule] = aCount + bCount
		}
		val combined = other.molecules + aUpdated
		// TODO convert traces to slag
		// TODO react after combination, adjust thermalEnergy, return LiquidAndGas
		return Mixture(combined, thermalEnergy)
	}

	fun splitByVolume(volume: Volume): Pair<Mixture, Mixture> {
		TODO("split mixture by volume")
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
	val volume: Volume,
	var liquid: Liquid?,
	var gas: Gas?
) {
	val combinedMass get() = (liquid?.mass ?: 0) + (gas?.mass ?: 0)
	val thermalEnergy get() = (liquid?.thermalEnergy ?: 0) + (liquid?.thermalEnergy ?: 0)
	val temperature: Temperature get() = thermalEnergy / combinedMass

	fun changeThermalEnergyBy(delta: ThermalEnergy): ReactionResult {
		return distributeOverflow(
			liquid?.addThermalEnergy(delta),
			gas?.addThermalEnergy(delta)
		)
	}

	abstract fun distributeOverflow(liquidResults: LiquidAndGas?, gasResults: LiquidAndGas?): ReactionResult

	class Flask(volume: Volume, liquid: Liquid?) :
		Container(volume, liquid, null) {
		override fun distributeOverflow(liquidResults: LiquidAndGas?, gasResults: LiquidAndGas?): ReactionResult {
			liquid = liquidResults?.liquid?.combine(gasResults?.liquid)
			val (liquidKept, liquidOverflow) = liquid?.splitByVolume(volume) ?: liquid to null
			liquid = liquidKept

			val getGasOverflow = { gasResults?.gas?.combine(liquidResults?.gas) }
			gas = null

			return ReactionResult(gasResults?.liquid, liquidResults?.gas) { liquidOverflow to getGasOverflow() }
		}
	}

	class Canister(volume: Volume, liquid: Liquid?, gas: Gas?) :
		Container(volume, liquid, gas) {
		override fun distributeOverflow(liquidResults: LiquidAndGas?, gasResults: LiquidAndGas?): ReactionResult {
			val liquidCombined = liquidResults?.liquid?.combine(gasResults?.liquid)
			val (liquidKept, liquidOverflow) = liquidCombined?.splitByVolume(volume) ?: liquidCombined to null
			liquid = liquidKept

			val getGasCombined = { gasResults?.gas?.combine(liquidResults?.gas) }

			val volumeLeftForGas = volume - (liquid?.volume ?: 0)
			val (gasKept, getGasOverflow) = if (volumeLeftForGas <= 0) {
				null to getGasCombined // all gas escapes; skip reaction
			} else {
				// some gas remains in the canister; need to calculate the reaction result
				val gasCombined = getGasCombined()
				val (gasKept, gasOverflow) = gasCombined?.splitByVolume(volumeLeftForGas) ?: gasCombined to null
				gasKept to { gasOverflow }
			}
			gas = gasKept
			return ReactionResult(gasResults?.liquid, liquidResults?.gas) { liquidOverflow to getGasOverflow() }
		}
	}
}
