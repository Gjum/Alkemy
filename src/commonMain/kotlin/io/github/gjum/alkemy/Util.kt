package io.github.gjum.alkemy

import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt

fun Double.round(places: Int): Double {
	val zeroes = 10.0.pow(places)
	return (this * zeroes).roundToInt() / zeroes
}

val Double.nearZero get() = this.absoluteValue < 0.000001

fun <T> List<T>.removeOne(e: T) = this.toMutableList().apply { remove(e) }

fun ReactionResult.print(): List<String> {
	val lines = mutableListOf<String>()
	val (liquidFromGas, gasFromLiquid, getOverflow) = this

	if (null != gasFromLiquid && gasFromLiquid.volume > 0) {
		lines += "Bubbling ... (${gasFromLiquid.volume.round(6)}ml)"
	}
	if (null != liquidFromGas && liquidFromGas.volume > 0) {
		lines += "Condensing ... (${liquidFromGas.volume.round(6)}ml)"
	}

	val overflow = getOverflow()
	if (null != overflow.gas && overflow.gas!!.volume > 0) {
		lines += "Fuming ... (${overflow.gas!!.volume.round(6)}ml)"
	}
	if (null != overflow.liquid && overflow.liquid!!.volume > 0) {
		lines += "Spilling ... (${overflow.liquid!!.volume.round(6)}ml)"
	}

	if (lines.isEmpty()) lines += "Nothing happens ..."

	return lines
}

fun BondSpec.buildTreeString(depth: Int): String {
	val shortSpecStr = toString().split('{')[1].split('}')[0]
	var result = " ".repeat(depth) + shortSpecStr
	for (child in children) {
		result += "\n" + child.buildTreeString(depth + 1)
	}
	return result
}
