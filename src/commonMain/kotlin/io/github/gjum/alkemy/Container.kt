package io.github.gjum.alkemy

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
