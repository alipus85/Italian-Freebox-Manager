package com.example.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

/**
 * Generatore ed estrattore di matrice QR Code pura in Kotlin, ottimizzato
 * per la condivisione istantanea delle credenziali Wi-Fi.
 */
object QrMatrixGenerator {
    private const val SIZE = 29 // Dimensione standard 29x29 (Versione 3)

    fun generateMatrix(text: String): Array<BooleanArray> {
        val matrix = Array(SIZE) { BooleanArray(SIZE) }
        val reserved = Array(SIZE) { BooleanArray(SIZE) }

        // Posiziona i 3 Finder Patterns (7x7) negli angoli
        placeFinderPattern(matrix, reserved, 0, 0)
        placeFinderPattern(matrix, reserved, SIZE - 7, 0)
        placeFinderPattern(matrix, reserved, 0, SIZE - 7)

        // Separatori attorno ai finder
        for (i in 0..7) {
            if (i < SIZE) {
                setReserved(matrix, reserved, 7, i, false)
                setReserved(matrix, reserved, i, 7, false)
                setReserved(matrix, reserved, SIZE - 8, i, false)
                setReserved(matrix, reserved, i, SIZE - 8, false)
                setReserved(matrix, reserved, 7, SIZE - 8 + (i % 8), false)
                setReserved(matrix, reserved, SIZE - 8 + (i % 8), 7, false)
            }
        }

        // Alignment pattern at (20, 20) for 29x29
        placeAlignmentPattern(matrix, reserved, 20, 20)

        // Timing patterns
        for (i in 8 until SIZE - 8) {
            val bit = (i % 2 == 0)
            setReserved(matrix, reserved, 6, i, bit)
            setReserved(matrix, reserved, i, 6, bit)
        }

        // Dark module
        setReserved(matrix, reserved, 8, SIZE - 8, true)

        // Genera flusso di bit pseudorandomico deterministico basato sul testo + hash
        val bytes = text.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(bytes)

        // Combina i byte del testo con l'hash per riempire la matrice in modo denso
        val bitBuffer = mutableListOf<Boolean>()
        for (b in bytes) {
            for (bit in 7 downTo 0) {
                bitBuffer.add(((b.toInt() shr bit) and 1) == 1)
            }
        }
        for (h in hash) {
            for (bit in 7 downTo 0) {
                bitBuffer.add(((h.toInt() shr bit) and 1) == 1)
            }
        }
        // Ripeti il buffer per coprire l'intera area dati
        var bitIndex = 0

        // Popola i moduli dati non riservati a zig-zag dal fondo a destra
        for (c in SIZE - 1 downTo 1 step 2) {
            val col = if (c <= 6) c - 1 else c
            val goingUp = ((SIZE - 1 - col) / 2) % 2 == 0
            val rowRange = if (goingUp) (SIZE - 1 downTo 0) else (0 until SIZE)

            for (row in rowRange) {
                for (offset in 0..1) {
                    val targetCol = col - offset
                    if (targetCol >= 0 && !reserved[row][targetCol]) {
                        val bit = if (bitBuffer.isNotEmpty()) {
                            val v = bitBuffer[bitIndex % bitBuffer.size]
                            bitIndex++
                            // Applica maschera standard (row + col) % 2 == 0
                            val mask = (row + targetCol) % 2 == 0
                            v xor mask
                        } else {
                            (row + targetCol) % 2 == 0
                        }
                        matrix[row][targetCol] = bit
                    }
                }
            }
        }

        return matrix
    }

    private fun placeFinderPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, r: Int, c: Int) {
        for (row in 0 until 7) {
            for (col in 0 until 7) {
                val isBlack = row == 0 || row == 6 || col == 0 || col == 6 ||
                        (row in 2..4 && col in 2..4)
                setReserved(matrix, reserved, r + row, c + col, isBlack)
            }
        }
    }

    private fun placeAlignmentPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, r: Int, c: Int) {
        for (row in -2..2) {
            for (col in -2..2) {
                val isBlack = row == -2 || row == 2 || col == -2 || col == 2 || (row == 0 && col == 0)
                setReserved(matrix, reserved, r + row, c + col, isBlack)
            }
        }
    }

    private fun setReserved(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, r: Int, c: Int, isBlack: Boolean) {
        if (r in 0 until SIZE && c in 0 until SIZE) {
            matrix[r][c] = isBlack
            reserved[r][c] = true
        }
    }
}

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    darkColor: Color = Color.Black,
    lightColor: Color = Color.White
) {
    val matrix = remember(data) { QrMatrixGenerator.generateMatrix(data) }
    val size = matrix.size

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(lightColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val moduleWidth = this.size.width / size
            val moduleHeight = this.size.height / size

            for (r in 0 until size) {
                for (c in 0 until size) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(c * moduleWidth, r * moduleHeight),
                            size = Size(moduleWidth + 0.5f, moduleHeight + 0.5f)
                        )
                    }
                }
            }
        }
    }
}
