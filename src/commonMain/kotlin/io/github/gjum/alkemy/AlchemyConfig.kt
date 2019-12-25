package io.github.gjum.alkemy

data class AlchemyConfig(
	val elements: Map<String, Element>,
	val bondsConfig: BondsConfig
)

typealias BondsConfig = Collection<BondSpec>

fun BondsConfig.matchingBondSpec(left: Atom, right: Atom): BondSpec? {
	return asSequence().map { it.matchingBondSpec(left, right) }.firstOrNull()
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

fun parseConfig(text: String): AlchemyConfig {
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

	return AlchemyConfig(elements, bondsConfig)
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
