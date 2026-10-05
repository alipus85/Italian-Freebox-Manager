package com.example.util

/**
 * A lightweight, zero-dependency pure Kotlin QR Code generator (Model 2, Byte Mode, ECC Level M).
 * Generates a boolean 2D matrix representing dark (true) and light (false) modules.
 */
object QrCodeGenerator {

    // Galois Field (256) log and antilog tables for Reed-Solomon coding
    private val expTable = IntArray(256)
    private val logTable = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            expTable[i] = x
            logTable[x] = i
            x = x shl 1
            if (x >= 256) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            // Unused in basic setup
        }
    }

    private fun gfMul(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return expTable[(logTable[x] + logTable[y]) % 255]
    }

    private fun computeRs(data: IntArray, ecCount: Int): IntArray {
        var gen = intArrayOf(1)
        for (i in 0 until ecCount) {
            val factor = expTable[i]
            val nextGen = IntArray(gen.size + 1)
            for (j in gen.indices) {
                nextGen[j] = nextGen[j] xor gfMul(gen[j], factor)
                nextGen[j + 1] = nextGen[j + 1] xor gen[j]
            }
            gen = nextGen
        }

        val res = IntArray(ecCount)
        for (b in data) {
            val factor = b xor res[0]
            for (j in 0 until ecCount - 1) {
                res[j] = res[j + 1] xor gfMul(gen[j + 1], factor)
            }
            res[ecCount - 1] = gfMul(gen[ecCount], factor)
        }
        return res
    }

    /**
     * Encodes a string into a 2D boolean array (QR matrix).
     * Automatically chooses Version 2 (25x25), Version 4 (33x33), or Version 6 (41x41).
     */
    fun encode(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val len = bytes.size

        // Choose version based on capacity for Byte mode with ECC Level M
        val (version, totalDataBytes, ecBytes) = when {
            len <= 26 -> Triple(2, 28, 16)
            len <= 62 -> Triple(4, 64, 36)
            else -> Triple(6, 108, 64)
        }

        val size = 17 + 4 * version
        val matrix = Array(size) { BooleanArray(size) }
        val isReserved = Array(size) { BooleanArray(size) }

        // 1. Bitstream preparation
        val bitBuffer = mutableListOf<Int>()
        fun putBits(value: Int, numBits: Int) {
            for (i in numBits - 1 downTo 0) {
                bitBuffer.add((value shr i) and 1)
            }
        }

        // Mode indicator: 0100 (Byte mode)
        putBits(4, 4)
        // Character count indicator (8 bits for versions 1-9 in byte mode)
        putBits(len, 8)
        // Data bytes
        for (b in bytes) {
            putBits(b.toInt() and 0xFF, 8)
        }
        // Terminator (up to 4 zeroes)
        val termZeros = minOf(4, totalDataBytes * 8 - bitBuffer.size)
        putBits(0, termZeros)
        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0) {
            bitBuffer.add(0)
        }
        // Pad bytes (0xEC, 0x11 alternating)
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIndex = 0
        while (bitBuffer.size < totalDataBytes * 8) {
            putBits(padBytes[padIndex % 2], 8)
            padIndex++
        }

        // Convert bitBuffer to IntArray data bytes
        val dataBytes = IntArray(totalDataBytes)
        for (i in 0 until totalDataBytes) {
            var b = 0
            for (j in 0 until 8) {
                b = (b shl 1) or bitBuffer[i * 8 + j]
            }
            dataBytes[i] = b
        }

        // Error correction codewords
        val ecCodewords = computeRs(dataBytes, ecBytes)
        val allCodewords = dataBytes + ecCodewords

        // 2. Finder patterns (7x7) at three corners
        fun drawFinder(top: Int, left: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val row = top + r
                    val col = left + c
                    if (row in 0 until size && col in 0 until size) {
                        isReserved[row][col] = true
                        val inSquare = (r in 0..6 && (c == 0 || c == 6)) || (c in 0..6 && (r == 0 || r == 6)) || (r in 2..4 && c in 2..4)
                        matrix[row][col] = inSquare
                    }
                }
            }
        }
        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // 3. Timing patterns
        for (i in 8 until size - 8) {
            if (!isReserved[6][i]) {
                isReserved[6][i] = true
                matrix[6][i] = (i % 2 == 0)
            }
            if (!isReserved[i][6]) {
                isReserved[i][6] = true
                matrix[i][6] = (i % 2 == 0)
            }
        }

        // 4. Alignment pattern for Version >= 2
        val alignPos = when (version) {
            2 -> intArrayOf(6, 18)
            4 -> intArrayOf(6, 26)
            6 -> intArrayOf(6, 34)
            else -> intArrayOf(6, 18)
        }
        for (r in alignPos) {
            for (c in alignPos) {
                if (!isReserved[r][c]) {
                    for (dr in -2..2) {
                        for (dc in -2..2) {
                            val row = r + dr
                            val col = c + dc
                            isReserved[row][col] = true
                            matrix[row][col] = (maxOf(Math.abs(dr), Math.abs(dc)) != 1)
                        }
                    }
                }
            }
        }

        // Dark module
        isReserved[4 * version + 9][8] = true
        matrix[4 * version + 9][8] = true

        // Reserve format info areas
        for (i in 0..8) {
            isReserved[8][i] = true
            isReserved[i][8] = true
        }
        for (i in 0..7) {
            isReserved[8][size - 1 - i] = true
            isReserved[size - 1 - i][8] = true
        }

        // 5. Fill Data & ECC into matrix (zigzag upward/downward)
        val allBits = mutableListOf<Int>()
        for (byte in allCodewords) {
            for (i in 7 downTo 0) {
                allBits.add((byte shr i) and 1)
            }
        }
        // Extra remainder bits
        while (allBits.size < size * size) {
            allBits.add(0)
        }

        var bitIdx = 0
        var upward = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing column
            val rows = if (upward) (size - 1 downTo 0) else (0 until size)
            for (r in rows) {
                for (c in intArrayOf(col, col - 1)) {
                    if (!isReserved[r][c]) {
                        val bit = if (bitIdx < allBits.size) allBits[bitIdx++] else 0
                        // Mask pattern 0: (row + col) % 2 == 0
                        val mask = (r + c) % 2 == 0
                        matrix[r][c] = (bit xor (if (mask) 1 else 0)) == 1
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // 6. Format info bits (ECC Level M = 00, Mask 0 = 000 -> 00000 xor 101010000010010)
        // With BCH code (15, 5), format 00000 gives 0000000000, XORed with mask 101010000010010 = 101010000010010
        val formatBits = intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0)
        // Top-left
        val tlCoords = arrayOf(
            Pair(8, 0), Pair(8, 1), Pair(8, 2), Pair(8, 3), Pair(8, 4), Pair(8, 5),
            Pair(8, 7), Pair(8, 8), Pair(7, 8), Pair(5, 8), Pair(4, 8), Pair(3, 8),
            Pair(2, 8), Pair(1, 8), Pair(0, 8)
        )
        for (i in 0 until 15) {
            val (r, c) = tlCoords[i]
            matrix[r][c] = (formatBits[i] == 1)
        }
        // Bottom-left & Top-right
        for (i in 0..6) {
            matrix[size - 1 - i][8] = (formatBits[i] == 1)
        }
        for (i in 7..14) {
            matrix[8][size - 15 + i] = (formatBits[i] == 1)
        }

        return matrix
    }
}
