package com.hktech.personalexpensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hktech.personalexpensetracker.data.CategoryEntity
import com.hktech.personalexpensetracker.ui.theme.CategoryColors
import com.hktech.personalexpensetracker.ui.theme.safeCategoryColor
import com.hktech.personalexpensetracker.ui.theme.safeParseColor
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementDialog(
    categories: List<CategoryEntity>,
    onAddCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onUpdateCategory: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFFFF5722)) }
    var showColorPicker by remember { mutableStateOf(false) }

    // For editing existing categories
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var editingColor by remember { mutableStateOf(Color(0xFFFF5722)) }
    var showEditColorPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manage Categories", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Add new category section
                Text(
                    "Add New Category",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(selectedColor)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { showColorPicker = !showColorPicker }
                        )
                    }
                )

                // Color picker for new category
                if (showColorPicker) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularColorPicker(
                        initialColor = selectedColor,
                        onColorSelected = { color ->
                            selectedColor = color
                        },
                        onDismiss = { showColorPicker = false }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(CategoryEntity(
                                name = newCategoryName.trim(),
                                color = colorToHex(selectedColor)
                            ))
                            newCategoryName = ""
                            selectedColor = Color(0xFFFF5722)
                            showColorPicker = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Category")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Existing categories list
                Text(
                    "Existing Categories (${categories.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories) { category ->
                        val isBuiltIn = category.name in CategoryColors
                        val displayColor = if (isBuiltIn) safeCategoryColor(category.name) else safeParseColor(category.color)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(displayColor)
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (isBuiltIn) {
                                    Text(
                                        text = "(built-in)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row {
                                if (!isBuiltIn) {
                                    IconButton(
                                        onClick = {
                                            editingCategory = category
                                            editingColor = safeParseColor(category.color)
                                            showEditColorPicker = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteCategory(category.name) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    // Edit color dialog
    if (showEditColorPicker && editingCategory != null) {
        AlertDialog(
            onDismissRequest = { showEditColorPicker = false },
            title = { Text("Edit Color: ${editingCategory?.name}") },
            text = {
                CircularColorPicker(
                    initialColor = editingColor,
                    onColorSelected = { color ->
                        editingColor = color
                    },
                    onDismiss = { showEditColorPicker = false }
                )
            },
            confirmButton = {
                Button(onClick = {
                    editingCategory?.let { cat ->
                        onUpdateCategory(cat.copy(color = colorToHex(editingColor)))
                    }
                    showEditColorPicker = false
                    editingCategory = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditColorPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CircularColorPicker(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0.8f) }
    var value by remember { mutableFloatStateOf(0.9f) }

    // Initialize from initial color
    LaunchedEffect(initialColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    val currentColor = Color.hsv(hue, saturation, value)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Color preview
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(currentColor)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hue wheel
        Box(
            modifier = Modifier
                .size(200.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val x = change.position.x - centerX
                        val y = change.position.y - centerY
                        val angle = atan2(y, x) * (180f / PI.toFloat())
                        hue = (angle + 360f) % 360f
                        onColorSelected(Color.hsv(hue, saturation, value))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val x = offset.x - centerX
                        val y = offset.y - centerY
                        val angle = atan2(y, x) * (180f / PI.toFloat())
                        hue = (angle + 360f) % 360f
                        onColorSelected(Color.hsv(hue, saturation, value))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val radius = minOf(centerX, centerY)

                // Draw hue wheel
                for (angle in 0..360 step 2) {
                    val startAngle = angle.toFloat()
                    drawArc(
                        color = Color.hsv(angle.toFloat(), 1f, 1f),
                        startAngle = startAngle - 90f,
                        sweepAngle = 2f,
                        useCenter = true,
                        topLeft = androidx.compose.ui.geometry.Offset(centerX - radius, centerY - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                }

                // Draw selector
                val selectorAngle = (hue - 90f) * (PI.toFloat() / 180f)
                val selectorRadius = radius * 0.85f
                val selectorX = centerX + selectorRadius * cos(selectorAngle)
                val selectorY = centerY + selectorRadius * sin(selectorAngle)

                drawCircle(
                    color = currentColor,
                    radius = 12.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(selectorX, selectorY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(selectorX, selectorY),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Saturation slider
        Text("Saturation", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = saturation,
            onValueChange = {
                saturation = it
                onColorSelected(Color.hsv(hue, saturation, value))
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        // Brightness slider
        Text("Brightness", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = value,
            onValueChange = {
                value = it
                onColorSelected(Color.hsv(hue, saturation, value))
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onDismiss) {
            Text("Done")
        }
    }
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt()
    )
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255).roundToInt()
    val g = (color.green * 255).roundToInt()
    val b = (color.blue * 255).roundToInt()
    return String.format("#%02X%02X%02X", r, g, b)
}