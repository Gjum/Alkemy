package io.github.gjum.alkemy

import java.io.File
import kotlin.math.min

fun main() {
	val configText = File("test_config_1.txt").readText()
	val world = parseConfig(configText)
	val (elements, bondsConfig) = world

	checkConfigParser(bondsConfig, configText)

	println("room temperature is ${ROOM_TEMPERATURE}K")

	val moleculeWater = Molecule(
		listOf(
			Atom(elements["a"]!!),
			Atom(elements["a"]!!),
			Atom(elements["C"]!!)
		), bondsConfig
	)
	printMoleculeInfo("Water", moleculeWater)
	println()

	val moleculeDD = Molecule(listOf(Atom(elements["D"]!!), Atom(elements["D"]!!)), bondsConfig)
	printMoleculeInfo("DD", moleculeDD)
	println()

	val liquid = Liquid(
		mutableMapOf(
			moleculeWater to 1.0 / moleculeWater.volume(ROOM_TEMPERATURE),
			moleculeDD to 1.0 / moleculeDD.volume(ROOM_TEMPERATURE)
		), Slag(0.0, 0.0), 0.0
	).atTemperature(ROOM_TEMPERATURE)

	println("Liquid temperature is ${liquid.temperature.r2}K (${liquid.thermalEnergy.r2}J in ${liquid.mass.r6}g)")

	val flask = Container.Flask(100 * liquid.volume, null)

	println("\nAdding ${liquid.volume.r6}ml (${liquid.mass.r6}g) liquid to flask ...")
	val addResult = flask.addLiquid(liquid)
	println(addResult.print().joinToString("\n"))
	printFlaskState(flask)

	val temperatureTarget = 1.0 + min(moleculeWater.boilingTemperature, moleculeDD.boilingTemperature)
	println("\nHeating flask to ${temperatureTarget.r2}K ...")
	val heatResult = flask.heatTo(temperatureTarget)
	println(heatResult.print().joinToString("\n"))
	printFlaskState(flask)
}

private fun printMoleculeInfo(moleculeName: String, molecule: Molecule) {
	println("$moleculeName boils at ${molecule.boilingTemperature.r2}K")
	for (temperature in listOf(
		0.0,
		ROOM_TEMPERATURE,
		molecule.boilingTemperature - 1,
		molecule.boilingTemperature + 1
	).sorted()) {
		val volume = molecule.volume(temperature)
		val phase = molecule.phase(temperature)
		println("$moleculeName at ${temperature}K: ${volume.r6}ml ($phase)")
	}
}

private fun printFlaskState(flask: Container.Flask) {
	println("Flask is at ${flask.temperature.r2}K (${flask.thermalEnergy.r2}J in ${flask.combinedMass.r6}g)")
	val liquidVol = flask.liquid?.volume ?: 0.0
	val slagVol = flask.liquid?.slag?.volume ?: 0.0
	println("Flask contains ${liquidVol.r6}ml liquid with ${slagVol.r6}ml slag (${(slagVol pctOf liquidVol).r4}% of liquid)")
}

private val Double.r2 get() = round(2).let { rounded -> if (this != rounded) "~$rounded" else "$rounded" }
private val Double.r4 get() = round(4).let { rounded -> if (this != rounded) "~$rounded" else "$rounded" }
private val Double.r6 get() = round(6).let { rounded -> if (this != rounded) "~$rounded" else "$rounded" }

private infix fun Double.pctOf(total: Double) = if (total.nearZero) Double.POSITIVE_INFINITY else 100 * this / total

private fun Mixture.atTemperature(targetTemperature: Temperature): Mixture {
	return Mixture(molecules, slag, mass * (targetTemperature / ROOM_TEMPERATURE))
}

private fun checkConfigParser(bondsConfig: BondsConfig, configText: String) {
	var bondsTextOut = ""
	for (bondSpec in bondsConfig) {
		bondsTextOut += bondSpec.buildTreeString(0) + "\n"
	}

	val bondsTextIn = configText.replace("\r", "").split("@bonds\n")[1].split("@molecules")[0]
	for ((cNr, cPair) in bondsTextIn.zip(bondsTextOut).withIndex()) {
		val (cIn, cOut) = cPair
		if (cIn != cOut) {
			val prevNewline = bondsTextIn.lastIndexOf("\n", startIndex = cNr - 1)
			val line = bondsTextIn.substring(prevNewline, bondsTextIn.indexOf('\n', startIndex = cNr))
			val lineOffset = configText.split("@bonds")[0].lines().size
			val lineNr = lineOffset + bondsTextIn.substring(0, cNr).lines().size
			println(bondsTextOut)
			println("Output differs from config in line $lineNr: $line")
			break
		}
	}
}
