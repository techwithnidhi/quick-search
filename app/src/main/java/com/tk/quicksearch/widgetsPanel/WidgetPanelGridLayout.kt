package com.tk.quicksearch.widgetsPanel

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

internal const val WIDGET_PANEL_GRID_COLUMNS = 4
internal const val WIDGET_PANEL_DEFAULT_COLUMN_SPAN = WIDGET_PANEL_GRID_COLUMNS
internal const val WIDGET_PANEL_DEFAULT_ROW_SPAN = 2
internal const val WIDGET_PANEL_MAX_ROW_SPAN = 4

internal data class WidgetGridSpec(
    val minColumnSpan: Int,
    val minRowSpan: Int,
)

internal data class WidgetGridResize(
    val column: Int,
    val row: Int,
    val columnSpan: Int,
    val rowSpan: Int,
)

internal fun resolveWidgetGridLayout(
    widgets: List<PanelWidgetInfo>,
    specs: Map<Int, WidgetGridSpec>,
    activeWidgetId: Int? = null,
): List<PanelWidgetInfo> {
    if (widgets.isEmpty()) return widgets

    val occupied = mutableSetOf<Pair<Int, Int>>()
    val orderedWidgets =
        if (activeWidgetId == null) {
            widgets
        } else {
            widgets.sortedBy { if (it.appWidgetId == activeWidgetId) 0 else 1 }
        }
    val placedById = LinkedHashMap<Int, PanelWidgetInfo>()

    orderedWidgets.forEach { widget ->
        val spec = specs[widget.appWidgetId]
        val columnSpan =
            widget.columnSpan
                .resolveSpan(spec?.minColumnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN)
                .coerceAtMost(WIDGET_PANEL_GRID_COLUMNS)
        val rowSpan =
            widget.rowSpan
                .resolveSpan(spec?.minRowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN)
                .coerceAtMost(WIDGET_PANEL_MAX_ROW_SPAN)
        val requestedColumn = widget.column?.coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - columnSpan)
        val requestedRow = widget.row?.coerceAtLeast(0)
        val position =
            if (
                requestedColumn != null &&
                requestedRow != null &&
                canPlace(
                    occupied = occupied,
                    column = requestedColumn,
                    row = requestedRow,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                )
            ) {
                requestedColumn to requestedRow
            } else {
                findFirstAvailablePosition(
                    occupied = occupied,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                )
            }

        markOccupied(
            occupied = occupied,
            column = position.first,
            row = position.second,
            columnSpan = columnSpan,
            rowSpan = rowSpan,
        )
        placedById[widget.appWidgetId] =
            widget.copy(
                column = position.first,
                row = position.second,
                columnSpan = columnSpan,
                rowSpan = rowSpan,
            )
    }

    return widgets.map { placedById[it.appWidgetId] ?: it }
}

internal fun PanelWidgetInfo.withGridPosition(
    column: Int,
    row: Int,
): PanelWidgetInfo {
    val span = columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
    return copy(
        column = column.coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - span.coerceAtMost(WIDGET_PANEL_GRID_COLUMNS)),
        row = row.coerceAtLeast(0),
    )
}

internal fun PanelWidgetInfo.withGridResize(resize: WidgetGridResize): PanelWidgetInfo =
    copy(
        column = resize.column,
        row = resize.row,
        columnSpan = resize.columnSpan,
        rowSpan = resize.rowSpan,
    )

internal fun calculateGridColumnSpan(
    minWidthDp: Float,
    cellWidthDp: Float,
    gapDp: Float,
): Int {
    return dpToGridSpan(
        sizeDp = minWidthDp,
        cellSizeDp = cellWidthDp,
        gapDp = gapDp,
        fallback = WIDGET_PANEL_DEFAULT_COLUMN_SPAN,
    ).coerceIn(1, WIDGET_PANEL_GRID_COLUMNS)
}

internal fun calculateGridRowSpan(
    minHeightDp: Float,
    rowHeightDp: Float,
    gapDp: Float,
): Int =
    dpToGridSpan(
        sizeDp = minHeightDp,
        cellSizeDp = rowHeightDp,
        gapDp = gapDp,
        fallback = WIDGET_PANEL_DEFAULT_ROW_SPAN,
    ).coerceIn(1, WIDGET_PANEL_MAX_ROW_SPAN)

internal fun calculateGridResize(
    startColumn: Int,
    startRow: Int,
    startColumnSpan: Int,
    startRowSpan: Int,
    horizontalDirection: Int,
    verticalDirection: Int,
    horizontalDeltaPx: Float,
    verticalDeltaPx: Float,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    minColumnSpan: Int,
    minRowSpan: Int,
): WidgetGridResize {
    val columnDelta =
        if (horizontalDirection == 0 || gridUnitWidthPx <= 0f) {
            0
        } else {
            (horizontalDeltaPx / gridUnitWidthPx).roundToInt()
        }
    val rowDelta =
        if (verticalDirection == 0 || gridUnitHeightPx <= 0f) {
            0
        } else {
            (verticalDeltaPx / gridUnitHeightPx).roundToInt()
        }

    var nextColumn = startColumn
    var nextRow = startRow
    var nextColumnSpan = startColumnSpan
    var nextRowSpan = startRowSpan

    when (horizontalDirection) {
        -1 -> {
            val maxLeftDelta = startColumnSpan - minColumnSpan
            val appliedDelta = columnDelta.coerceIn(-startColumn, maxLeftDelta)
            nextColumn = startColumn + appliedDelta
            nextColumnSpan = startColumnSpan - appliedDelta
        }

        1 -> {
            nextColumnSpan =
                (startColumnSpan + columnDelta)
                    .coerceIn(minColumnSpan, WIDGET_PANEL_GRID_COLUMNS - startColumn)
        }
    }

    when (verticalDirection) {
        -1 -> {
            val maxUpDelta = startRowSpan - minRowSpan
            val appliedDelta = rowDelta.coerceIn(-startRow, maxUpDelta)
            nextRow = startRow + appliedDelta
            nextRowSpan = startRowSpan - appliedDelta
        }

        1 -> {
            nextRowSpan =
                (startRowSpan + rowDelta)
                    .coerceIn(minRowSpan, WIDGET_PANEL_MAX_ROW_SPAN)
        }
    }

    return WidgetGridResize(
        column = nextColumn,
        row = nextRow,
        columnSpan = nextColumnSpan,
        rowSpan = nextRowSpan,
    )
}

private fun Int?.resolveSpan(defaultSpan: Int): Int = this ?: defaultSpan.coerceAtLeast(1)

private fun dpToGridSpan(
    sizeDp: Float,
    cellSizeDp: Float,
    gapDp: Float,
    fallback: Int,
): Int {
    if (cellSizeDp <= 0f) return fallback
    val rawSpan = ceil((sizeDp + gapDp) / (cellSizeDp + gapDp)).toInt()
    return max(1, rawSpan)
}

private fun findFirstAvailablePosition(
    occupied: Set<Pair<Int, Int>>,
    columnSpan: Int,
    rowSpan: Int,
): Pair<Int, Int> {
    var row = 0
    while (true) {
        for (column in 0..(WIDGET_PANEL_GRID_COLUMNS - columnSpan)) {
            if (canPlace(occupied, column, row, columnSpan, rowSpan)) {
                return column to row
            }
        }
        row++
    }
}

private fun canPlace(
    occupied: Set<Pair<Int, Int>>,
    column: Int,
    row: Int,
    columnSpan: Int,
    rowSpan: Int,
): Boolean {
    for (occupiedColumn in column until column + columnSpan) {
        for (occupiedRow in row until row + rowSpan) {
            if ((occupiedColumn to occupiedRow) in occupied) return false
        }
    }
    return true
}

private fun markOccupied(
    occupied: MutableSet<Pair<Int, Int>>,
    column: Int,
    row: Int,
    columnSpan: Int,
    rowSpan: Int,
) {
    for (occupiedColumn in column until column + columnSpan) {
        for (occupiedRow in row until row + rowSpan) {
            occupied += occupiedColumn to occupiedRow
        }
    }
}
