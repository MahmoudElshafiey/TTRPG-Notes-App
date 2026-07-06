package com.example.dndnotes.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

import kotlinx.serialization.Serializable

@Serializable
data class SerializablePoint(val x: Float, val y: Float)

@Serializable
data class SerializablePath(
    val points: List<SerializablePoint>,
    val color: Int, // ARGB
    val strokeWidth: Float
)

data class PathState(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    initialPaths: List<SerializablePath> = emptyList(),
    onDrawingChanged: (List<SerializablePath>) -> Unit,
    strokeColor: Color = Color.Red,
    strokeWidth: Float = 5f,
    undoTrigger: Int = 0,
    clearTrigger: Int = 0
) {
    val paths = remember { mutableStateListOf<PathState>() }
    val serializablePaths = remember { mutableStateListOf<SerializablePath>() }
    
    // Initialize from saved state
    LaunchedEffect(initialPaths) {
        if (paths.isEmpty() && initialPaths.isNotEmpty()) {
            initialPaths.forEach { sPath ->
                val path = Path()
                if (sPath.points.isNotEmpty()) {
                    path.moveTo(sPath.points[0].x, sPath.points[0].y)
                    for (i in 1 until sPath.points.size) {
                        path.lineTo(sPath.points[i].x, sPath.points[i].y)
                    }
                }
                paths.add(PathState(path, Color(sPath.color), sPath.strokeWidth))
                serializablePaths.add(sPath)
            }
        }
    }

    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints = remember { mutableStateListOf<SerializablePoint>() }
    var redrawTrigger by remember { mutableStateOf(0) }

    // Handle Undo
    LaunchedEffect(undoTrigger) {
        if (paths.isNotEmpty() && undoTrigger > 0) {
            paths.removeAt(paths.size - 1)
            serializablePaths.removeAt(serializablePaths.size - 1)
            redrawTrigger++
            onDrawingChanged(serializablePaths.toList())
        }
    }

    // Handle Clear
    LaunchedEffect(clearTrigger) {
        if (clearTrigger > 0) {
            paths.clear()
            serializablePaths.clear()
            redrawTrigger++
            onDrawingChanged(emptyList())
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath = Path().apply {
                            moveTo(offset.x, offset.y)
                        }
                        currentPoints.clear()
                        currentPoints.add(SerializablePoint(offset.x, offset.y))
                    },
                    onDrag = { change, _ ->
                        currentPath?.lineTo(change.position.x, change.position.y)
                        currentPoints.add(SerializablePoint(change.position.x, change.position.y))
                        redrawTrigger++
                    },
                    onDragEnd = {
                        currentPath?.let {
                            paths.add(PathState(it, strokeColor, strokeWidth))
                            val sPath = SerializablePath(
                                points = currentPoints.toList(),
                                color = strokeColor.toArgb(),
                                strokeWidth = strokeWidth
                            )
                            serializablePaths.add(sPath)
                            onDrawingChanged(serializablePaths.toList())
                        }
                        currentPath = null
                        redrawTrigger++
                    }
                )
            }
    ) {
        // Access redrawTrigger to ensure recomposition
        val _trigger = redrawTrigger

        paths.forEach { pathState ->
            drawPath(
                path = pathState.path,
                color = pathState.color,
                style = Stroke(
                    width = pathState.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        currentPath?.let {
            drawPath(
                path = it,
                color = strokeColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
