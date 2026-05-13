package com.tk.quicksearch.widgetsPanel

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.HapticFeedbackConstantsCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.notes.NotesTextUtils
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.performHapticFeedbackSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val WIDGET_PANEL_HOST_ID = 8291
private const val QUICK_NOTE_SAVE_DELAY_MS = 450L
private const val WIDGET_PANEL_SWIPE_THRESHOLD_PX = 140f
private const val WIDGET_PREVIEW_FALLBACK_SIZE_PX = 96
private val WidgetMinWidth = 96.dp
private val WidgetMinHeight = 104.dp
private val WidgetManagementTouchPadding = 12.dp
private val WidgetPanelGridRowHeight = 96.dp
private val WidgetPanelGridGap = 12.dp
private val WidgetResizeEdgeHandleLength = 56.dp
private val WidgetResizeHandleThickness = 6.dp
private val WidgetResizeCornerHandleSize = 18.dp
private val QuickNoteHeight = 164.dp
private val QuickNoteFocusedHeight = 280.dp

private data class WidgetPickerApp(
    val packageName: String,
    val appLabel: String,
    val icon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>,
)

private data class PendingWidgetRequest(
    val appWidgetId: Int,
    val provider: AppWidgetProviderInfo,
)

private data class WidgetResizeDirection(
    val horizontal: Int,
    val vertical: Int,
)

@Composable
fun WidgetsPanelScreen(
    onNavigateToSearch: () -> Unit,
    appTheme: AppTheme,
    overlayThemeIntensity: Float,
    deviceThemeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val appWidgetManager = remember(appContext) { AppWidgetManager.getInstance(appContext) }
    val appWidgetHost = remember(appContext) { AppWidgetHost(appContext, WIDGET_PANEL_HOST_ID) }
    val preferences = remember(appContext) { WidgetsPanelPreferences(appContext) }

    var widgets by remember { mutableStateOf(preferences.getWidgets()) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var pendingRequest by remember { mutableStateOf<PendingWidgetRequest?>(null) }
    var reorderMode by rememberSaveable { mutableStateOf(false) }

    fun persistWidgets(next: List<PanelWidgetInfo>) {
        widgets = next
        preferences.setWidgets(next)
    }

    fun previewWidgets(next: List<PanelWidgetInfo>) {
        widgets = next
    }

    fun addBoundWidget(request: PendingWidgetRequest) {
        val provider = request.provider.provider
        widgets = preferences.addWidget(request.appWidgetId, provider)
        reorderMode = false
        showPicker = false
        pendingRequest = null
    }

    fun launchConfigureIfNeeded(
        request: PendingWidgetRequest,
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>,
    ) {
        val configure = request.provider.configure
        if (configure == null) {
            addBoundWidget(request)
            return
        }
        val intent =
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(configure)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, request.appWidgetId)
        pendingRequest = request
        launcher.launch(intent)
    }

    val configureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val request = pendingRequest ?: return@rememberLauncherForActivityResult
            if (result.resultCode == Activity.RESULT_OK) {
                addBoundWidget(request)
            } else {
                appWidgetHost.deleteAppWidgetId(request.appWidgetId)
                pendingRequest = null
            }
        }

    val bindLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val request = pendingRequest ?: return@rememberLauncherForActivityResult
            if (result.resultCode == Activity.RESULT_OK) {
                launchConfigureIfNeeded(request, configureLauncher)
            } else {
                appWidgetHost.deleteAppWidgetId(request.appWidgetId)
                pendingRequest = null
            }
        }

    fun requestAddWidget(provider: AppWidgetProviderInfo) {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        val request = PendingWidgetRequest(appWidgetId, provider)
        val canBind =
            runCatching {
                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider)
            }.getOrDefault(false)

        if (canBind) {
            launchConfigureIfNeeded(request, configureLauncher)
        } else {
            pendingRequest = request
            val intent =
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            bindLauncher.launch(intent)
        }
    }

    DisposableEffect(appWidgetHost) {
        appWidgetHost.startListening()
        onDispose { appWidgetHost.stopListening() }
    }

    BackHandlerCompat(onBack = onNavigateToSearch)

    val swipeBackModifier =
        Modifier.pointerInput(onNavigateToSearch) {
            var totalHorizontalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalHorizontalDrag = 0f },
                onHorizontalDrag = { _, dragAmount ->
                    totalHorizontalDrag += dragAmount
                },
                onDragEnd = {
                    if (totalHorizontalDrag <= -WIDGET_PANEL_SWIPE_THRESHOLD_PX) {
                        onNavigateToSearch()
                    }
                    totalHorizontalDrag = 0f
                },
                onDragCancel = { totalHorizontalDrag = 0f },
            )
        }

    SettingsScreenBackground(
        appTheme = appTheme,
        overlayThemeIntensity = overlayThemeIntensity,
        deviceThemeEnabled = deviceThemeEnabled,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(swipeBackModifier)
                    .navigationBarsPadding()
                    .imePadding(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                        .padding(top = DesignTokens.SpacingXXLarge, bottom = DesignTokens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.widgets_panel_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { showPicker = true },
                        shape = DesignTokens.ShapeXXLarge,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(DesignTokens.IconSizeSmall),
                        )
                        Spacer(modifier = Modifier.size(DesignTokens.TextButtonIconSpacing))
                        Text(text = stringResource(R.string.common_action_add))
                    }
                }

                if (reorderMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.widgets_panel_reorder_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { reorderMode = false }) {
                            Text(text = stringResource(R.string.dialog_done))
                        }
                    }
                }

                CompactQuickNoteWidget(modifier = Modifier.fillMaxWidth())

                if (widgets.isNotEmpty()) {
                    WidgetGridPanel(
                        widgets = widgets,
                        appWidgetManager = appWidgetManager,
                        appWidgetHost = appWidgetHost,
                        reorderMode = reorderMode,
                        onPreviewWidgets = ::previewWidgets,
                        onPersistWidgets = ::persistWidgets,
                        onRequestReorder = { reorderMode = true },
                        onRemove = { widget ->
                            appWidgetHost.deleteAppWidgetId(widget.appWidgetId)
                            val next = preferences.removeWidget(widget.appWidgetId)
                            widgets = next
                            if (next.isEmpty()) reorderMode = false
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                    )
                }

            }

            if (showPicker) {
                WidgetPickerSheet(
                    appWidgetManager = appWidgetManager,
                    onDismiss = { showPicker = false },
                    onSelectWidget = ::requestAddWidget,
                )
            }
        }
    }
}

@Composable
private fun WidgetGridPanel(
    widgets: List<PanelWidgetInfo>,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: AppWidgetHost,
    reorderMode: Boolean,
    onPreviewWidgets: (List<PanelWidgetInfo>) -> Unit,
    onPersistWidgets: (List<PanelWidgetInfo>) -> Unit,
    onRequestReorder: () -> Unit,
    onRemove: (PanelWidgetInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    var activeWidgetId by remember { mutableStateOf<Int?>(null) }
    var resizingWidgetId by remember { mutableStateOf<Int?>(null) }

    BoxWithConstraints(modifier = modifier) {
        val gridGap = WidgetPanelGridGap
        val cellWidth =
            (maxWidth - gridGap * (WIDGET_PANEL_GRID_COLUMNS - 1)) / WIDGET_PANEL_GRID_COLUMNS
        val gridUnitWidthPx = with(density) { (cellWidth + gridGap).toPx() }
        val gridUnitHeightPx = with(density) { (WidgetPanelGridRowHeight + gridGap).toPx() }
        val specs =
            remember(widgets, appWidgetManager, cellWidth) {
                widgets.associate { widget ->
                    val providerInfo = appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
                    val minColumnSpan =
                        calculateGridColumnSpan(
                            minWidthDp = providerInfo?.minWidth?.toFloat() ?: WidgetMinWidth.value,
                            cellWidthDp = cellWidth.value,
                            gapDp = gridGap.value,
                        )
                    val minRowSpan =
                        calculateGridRowSpan(
                            minHeightDp = providerInfo?.minHeight?.toFloat() ?: WidgetMinHeight.value,
                            rowHeightDp = WidgetPanelGridRowHeight.value,
                            gapDp = gridGap.value,
                        )
                    widget.appWidgetId to
                        WidgetGridSpec(
                            minColumnSpan = minColumnSpan,
                            minRowSpan = minRowSpan,
                        )
                }
            }
        val laidOutWidgets =
            remember(widgets, specs, activeWidgetId) {
                resolveWidgetGridLayout(
                    widgets = widgets,
                    specs = specs,
                    activeWidgetId = activeWidgetId,
                )
            }

        LaunchedEffect(laidOutWidgets) {
            if (laidOutWidgets != widgets) {
                onPersistWidgets(laidOutWidgets)
            }
        }

        val rows =
            laidOutWidgets.maxOfOrNull { widget ->
                (widget.row ?: 0) + (widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN)
            } ?: 0
        val panelHeight =
            if (rows <= 0) {
                0.dp
            } else {
                WidgetPanelGridRowHeight * rows + gridGap * (rows - 1)
            }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(panelHeight),
        ) {
            laidOutWidgets.forEach { widget ->
                key(widget.appWidgetId) {
                    val column = widget.column ?: 0
                    val row = widget.row ?: 0
                    val columnSpan = widget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
                    val rowSpan = widget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
                    val x = (cellWidth + gridGap) * column
                    val y = (WidgetPanelGridRowHeight + gridGap) * row
                    val width = cellWidth * columnSpan + gridGap * (columnSpan - 1)
                    val height = WidgetPanelGridRowHeight * rowSpan + gridGap * (rowSpan - 1)
                    val isInteracting =
                        activeWidgetId == widget.appWidgetId || resizingWidgetId == widget.appWidgetId
                    val animatedX by animateDpAsState(targetValue = x, label = "widgetGridX")
                    val animatedY by animateDpAsState(targetValue = y, label = "widgetGridY")
                    val animatedWidth by animateDpAsState(targetValue = width, label = "widgetGridWidth")
                    val animatedHeight by animateDpAsState(targetValue = height, label = "widgetGridHeight")
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (activeWidgetId == widget.appWidgetId) DesignTokens.DragAlpha else 1f,
                        label = "widgetGridAlpha",
                    )
                    var dragStartColumn by remember(widget.appWidgetId) { mutableStateOf(column) }
                    var dragStartRow by remember(widget.appWidgetId) { mutableStateOf(row) }
                    var totalDragX by remember(widget.appWidgetId) { mutableStateOf(0f) }
                    var totalDragY by remember(widget.appWidgetId) { mutableStateOf(0f) }
                    val currentColumn by rememberUpdatedState(column)
                    val currentRow by rememberUpdatedState(row)
                    val currentColumnSpan by rememberUpdatedState(columnSpan)

                    HostedWidgetItem(
                        widget = widget,
                        appWidgetManager = appWidgetManager,
                        appWidgetHost = appWidgetHost,
                        width = if (isInteracting) width else animatedWidth,
                        height = if (isInteracting) height else animatedHeight,
                        isDragging = activeWidgetId == widget.appWidgetId,
                        reorderMode = reorderMode,
                        onRequestReorder = onRequestReorder,
                        onRemove = { onRemove(widget) },
                        onResizeStart = { resizingWidgetId = widget.appWidgetId },
                        onResize = { resize ->
                            val next =
                                widgets
                                    .map { item ->
                                        if (item.appWidgetId == widget.appWidgetId) {
                                            item.withGridResize(resize)
                                        } else {
                                            item
                                        }
                                    }.let { candidates ->
                                        resolveWidgetGridLayout(
                                            widgets = candidates,
                                            specs = specs,
                                            activeWidgetId = widget.appWidgetId,
                                        )
                                    }
                            onPreviewWidgets(next)
                        },
                        onResizeFinished = {
                            resizingWidgetId = null
                            onPersistWidgets(
                                resolveWidgetGridLayout(
                                    widgets = widgets,
                                    specs = specs,
                                    activeWidgetId = widget.appWidgetId,
                                ),
                            )
                        },
                        gridUnitWidthPx = gridUnitWidthPx,
                        gridUnitHeightPx = gridUnitHeightPx,
                        spec = specs[widget.appWidgetId],
                        modifier =
                            Modifier
                                .offset {
                                    IntOffset(
                                        x = with(density) { (if (isInteracting) x else animatedX).roundToPx() },
                                        y = with(density) { (if (isInteracting) y else animatedY).roundToPx() },
                                    )
                                }.graphicsLayer { alpha = animatedAlpha }
                                .pointerInput(widget.appWidgetId, gridUnitWidthPx, gridUnitHeightPx) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            activeWidgetId = widget.appWidgetId
                                            dragStartColumn = currentColumn
                                            dragStartRow = currentRow
                                            totalDragX = 0f
                                            totalDragY = 0f
                                            performHapticFeedbackSafely(
                                                view,
                                                HapticFeedbackConstantsCompat.GESTURE_START,
                                            )
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            totalDragX += dragAmount.x
                                            totalDragY += dragAmount.y
                                            val nextColumn =
                                                (dragStartColumn + (totalDragX / gridUnitWidthPx).roundToInt())
                                                    .coerceIn(0, WIDGET_PANEL_GRID_COLUMNS - currentColumnSpan)
                                            val nextRow =
                                                (dragStartRow + (totalDragY / gridUnitHeightPx).roundToInt())
                                                    .coerceAtLeast(0)
                                            val next =
                                                widgets
                                                    .map { item ->
                                                        if (item.appWidgetId == widget.appWidgetId) {
                                                            item.withGridPosition(nextColumn, nextRow)
                                                        } else {
                                                            item
                                                        }
                                                    }.let { candidates ->
                                                        resolveWidgetGridLayout(
                                                            widgets = candidates,
                                                            specs = specs,
                                                            activeWidgetId = widget.appWidgetId,
                                                        )
                                                    }
                                            onPreviewWidgets(next)
                                        },
                                        onDragEnd = {
                                            activeWidgetId = null
                                            onPersistWidgets(
                                                resolveWidgetGridLayout(
                                                    widgets = widgets,
                                                    specs = specs,
                                                    activeWidgetId = widget.appWidgetId,
                                                ),
                                            )
                                            performHapticFeedbackSafely(
                                                view,
                                                HapticFeedbackConstantsCompat.GESTURE_END,
                                            )
                                        },
                                        onDragCancel = {
                                            activeWidgetId = null
                                            onPersistWidgets(
                                                resolveWidgetGridLayout(
                                                    widgets = widgets,
                                                    specs = specs,
                                                    activeWidgetId = widget.appWidgetId,
                                                ),
                                            )
                                        },
                                    )
                                },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactQuickNoteWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember(context) { NotesRepository(context) }
    val linkColor = AppColors.LinkColor
    var bodyInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var baselineBody by rememberSaveable { mutableStateOf<String?>(null) }
    var quickNoteId by rememberSaveable { mutableStateOf(-1L) }
    var isFocused by rememberSaveable { mutableStateOf(false) }
    val quickNoteHeight by animateDpAsState(
        targetValue = if (isFocused) QuickNoteFocusedHeight else QuickNoteHeight,
        label = "quickNoteHeight",
    )

    LaunchedEffect(Unit) {
        val note = withContext(Dispatchers.IO) { repository.getOrCreateQuickNote() }
        quickNoteId = note.noteId
        bodyInput =
            TextFieldValue(
                annotatedString =
                    NotesTextUtils.buildLinkHighlightedAnnotatedString(
                        note.markdownContent,
                        linkColor,
                    ),
                selection = TextRange(note.markdownContent.length),
            )
        baselineBody = note.markdownContent
    }

    LaunchedEffect(quickNoteId, bodyInput.text, baselineBody) {
        val id = quickNoteId
        val baseline = baselineBody ?: return@LaunchedEffect
        if (id <= 0L || bodyInput.text == baseline) return@LaunchedEffect
        delay(QUICK_NOTE_SAVE_DELAY_MS)
        withContext(Dispatchers.IO) {
            repository.updateNote(
                noteId = id,
                title = context.getString(R.string.notes_quick_note_title),
                markdownContent = bodyInput.text,
            )
        }
        baselineBody = bodyInput.text
    }

    val currentBody by rememberUpdatedState(bodyInput.text)
    val currentBaseline by rememberUpdatedState(baselineBody)
    DisposableEffect(quickNoteId) {
        onDispose {
            val id = quickNoteId
            val baseline = currentBaseline
            if (id > 0L && baseline != null && currentBody != baseline) {
                repository.updateNote(
                    noteId = id,
                    title = context.getString(R.string.notes_quick_note_title),
                    markdownContent = currentBody,
                )
            }
        }
    }

    Surface(
        modifier = modifier.height(quickNoteHeight),
        shape = DesignTokens.ExtraLargeCardShape,
        color = AppColors.getSettingsCardContainerColor(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(DesignTokens.CardHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Text(
                text = stringResource(R.string.notes_quick_note_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            HorizontalDivider(color = AppColors.SettingsDivider)
            BasicTextField(
                value = bodyInput,
                onValueChange = {
                    bodyInput =
                        it.copy(
                            annotatedString =
                                NotesTextUtils.buildLinkHighlightedAnnotatedString(
                                    it.text,
                                    linkColor,
                                ),
                        )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused },
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (bodyInput.text.isBlank()) {
                        Text(
                            text = stringResource(R.string.notes_body_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                    inner()
                },
            )
        }
    }
}

@Composable
private fun HostedWidgetItem(
    widget: PanelWidgetInfo,
    appWidgetManager: AppWidgetManager,
    appWidgetHost: AppWidgetHost,
    width: Dp,
    height: Dp,
    isDragging: Boolean,
    reorderMode: Boolean,
    onRequestReorder: () -> Unit,
    onRemove: () -> Unit,
    onResizeStart: () -> Unit,
    onResize: (WidgetGridResize) -> Unit,
    onResizeFinished: () -> Unit,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    spec: WidgetGridSpec?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val view = LocalView.current
    val providerInfo = remember(widget.appWidgetId) {
        appWidgetManager.getAppWidgetInfo(widget.appWidgetId)
    }

    var showActionsMenu by remember { mutableStateOf(false) }
    var isManaging by remember { mutableStateOf(false) }
    val configureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    val currentShowActionsMenu by rememberUpdatedState(
        newValue = {
            isManaging = true
            showActionsMenu = true
            performHapticFeedbackSafely(view, HapticFeedbackConstantsCompat.LONG_PRESS)
            Unit
        },
    )
    val configureIntent =
        remember(providerInfo, widget.appWidgetId) {
            providerInfo?.configure?.let { configure ->
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                    .setComponent(configure)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget.appWidgetId)
            }
        }

    Column(
        modifier =
            modifier
                .size(width = width, height = height)
                .graphicsLayer { alpha = if (isDragging) DesignTokens.DragAlpha else 1f },
    ) {
        if (reorderMode) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = DesignTokens.SpacingXSmall,
                            end = DesignTokens.SpacingXSmall,
                            bottom = DesignTokens.SpacingSmall,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = stringResource(R.string.settings_action_reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .size(DesignTokens.IconSize),
                )
                Text(
                    text = providerInfo?.loadLabel(context.packageManager)?.toString().orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = DesignTokens.SpacingSmall),
                    maxLines = 1,
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
        ) {
            DropdownMenu(
                expanded = showActionsMenu,
                onDismissRequest = { showActionsMenu = false },
            ) {
                if (configureIntent != null) {
                    DropdownMenuItem(
                        text = {
                            Text(text = stringResource(R.string.widgets_panel_widget_settings))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showActionsMenu = false
                            configureLauncher.launch(configureIntent)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.widgets_panel_reorder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        showActionsMenu = false
                        onRequestReorder()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_remove)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        showActionsMenu = false
                        onRemove()
                    },
                )
            }

            if (providerInfo != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = {
                            appWidgetHost.createView(it, widget.appWidgetId, providerInfo).apply {
                                setOnLongClickListener {
                                    currentShowActionsMenu()
                                    true
                                }
                                setAppWidget(widget.appWidgetId, providerInfo)
                                layoutParams =
                                    ViewGroup.LayoutParams(
                                        with(density) { width.roundToPx() },
                                        with(density) { height.roundToPx() },
                                    )
                            }
                        },
                        update = { hostView ->
                            hostView.setOnLongClickListener {
                                currentShowActionsMenu()
                                true
                            }
                            hostView.layoutParams =
                                ViewGroup.LayoutParams(
                                    with(density) { width.roundToPx() },
                                    with(density) { height.roundToPx() },
                                )
                            val widthDp = width.value.toInt()
                            val heightDp = height.value.toInt()
                            hostView.updateAppWidgetSize(
                                Bundle(),
                                widthDp,
                                heightDp,
                                widthDp,
                                heightDp,
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    WidgetManagementGesturePadding(
                        onLongPress = currentShowActionsMenu,
                        modifier = Modifier.matchParentSize(),
                    )

                    if (isManaging) {
                        WidgetResizeFrame(
                            widget = widget,
                            width = width,
                            height = height,
                            gridUnitWidthPx = gridUnitWidthPx,
                            gridUnitHeightPx = gridUnitHeightPx,
                            minColumnSpan = spec?.minColumnSpan ?: 1,
                            minRowSpan = spec?.minRowSpan ?: 1,
                            onResizeStart = onResizeStart,
                            onResize = onResize,
                            onResizeFinished = onResizeFinished,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.widgets_panel_unavailable_widget),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(DesignTokens.CardHorizontalPadding),
                )
            }
        }
    }
}

@Composable
private fun WidgetResizeFrame(
    widget: PanelWidgetInfo,
    width: Dp,
    height: Dp,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    minColumnSpan: Int,
    minRowSpan: Int,
    onResizeStart: () -> Unit,
    onResize: (WidgetGridResize) -> Unit,
    onResizeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val frameWidth = width + WidgetManagementTouchPadding * 2
    val frameHeight = height + WidgetManagementTouchPadding * 2

    Box(
        modifier =
            modifier
                .size(width = frameWidth, height = frameHeight)
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = DesignTokens.ExtraLargeCardShape,
                ),
    ) {
        WidgetResizeHandle(
            direction = WidgetResizeDirection(horizontal = 0, vertical = -1),
            widget = widget,
            gridUnitWidthPx = gridUnitWidthPx,
            gridUnitHeightPx = gridUnitHeightPx,
            minColumnSpan = minColumnSpan,
            minRowSpan = minRowSpan,
            onResizeStart = onResizeStart,
            onResize = onResize,
            onResizeFinished = onResizeFinished,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .size(
                        width = WidgetResizeEdgeHandleLength,
                        height = WidgetResizeHandleThickness,
                    ),
        )
        WidgetResizeHandle(
            direction = WidgetResizeDirection(horizontal = 0, vertical = 1),
            widget = widget,
            gridUnitWidthPx = gridUnitWidthPx,
            gridUnitHeightPx = gridUnitHeightPx,
            minColumnSpan = minColumnSpan,
            minRowSpan = minRowSpan,
            onResizeStart = onResizeStart,
            onResize = onResize,
            onResizeFinished = onResizeFinished,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .size(
                        width = WidgetResizeEdgeHandleLength,
                        height = WidgetResizeHandleThickness,
                    ),
        )
        WidgetResizeHandle(
            direction = WidgetResizeDirection(horizontal = -1, vertical = 0),
            widget = widget,
            gridUnitWidthPx = gridUnitWidthPx,
            gridUnitHeightPx = gridUnitHeightPx,
            minColumnSpan = minColumnSpan,
            minRowSpan = minRowSpan,
            onResizeStart = onResizeStart,
            onResize = onResize,
            onResizeFinished = onResizeFinished,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .size(
                        width = WidgetResizeHandleThickness,
                        height = WidgetResizeEdgeHandleLength,
                    ),
        )
        WidgetResizeHandle(
            direction = WidgetResizeDirection(horizontal = 1, vertical = 0),
            widget = widget,
            gridUnitWidthPx = gridUnitWidthPx,
            gridUnitHeightPx = gridUnitHeightPx,
            minColumnSpan = minColumnSpan,
            minRowSpan = minRowSpan,
            onResizeStart = onResizeStart,
            onResize = onResize,
            onResizeFinished = onResizeFinished,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(
                        width = WidgetResizeHandleThickness,
                        height = WidgetResizeEdgeHandleLength,
                    ),
        )
        listOf(
            Alignment.TopStart to WidgetResizeDirection(horizontal = -1, vertical = -1),
            Alignment.TopEnd to WidgetResizeDirection(horizontal = 1, vertical = -1),
            Alignment.BottomStart to WidgetResizeDirection(horizontal = -1, vertical = 1),
            Alignment.BottomEnd to WidgetResizeDirection(horizontal = 1, vertical = 1),
        ).forEach { (alignment, direction) ->
            WidgetResizeHandle(
                direction = direction,
                widget = widget,
                gridUnitWidthPx = gridUnitWidthPx,
                gridUnitHeightPx = gridUnitHeightPx,
                minColumnSpan = minColumnSpan,
                minRowSpan = minRowSpan,
                onResizeStart = onResizeStart,
                onResize = onResize,
                onResizeFinished = onResizeFinished,
                modifier =
                    Modifier
                        .align(alignment)
                        .size(WidgetResizeCornerHandleSize),
            )
        }
    }
}

@Composable
private fun WidgetResizeHandle(
    direction: WidgetResizeDirection,
    widget: PanelWidgetInfo,
    gridUnitWidthPx: Float,
    gridUnitHeightPx: Float,
    minColumnSpan: Int,
    minRowSpan: Int,
    onResizeStart: () -> Unit,
    onResize: (WidgetGridResize) -> Unit,
    onResizeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentWidget by rememberUpdatedState(widget)
    Surface(
        modifier =
            modifier.pointerInput(direction, gridUnitWidthPx, gridUnitHeightPx) {
                var totalHorizontalDelta = 0f
                var totalVerticalDelta = 0f
                var startColumn = currentWidget.column ?: 0
                var startRow = currentWidget.row ?: 0
                var startColumnSpan = currentWidget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
                var startRowSpan = currentWidget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
                detectDragGestures(
                    onDragStart = {
                        onResizeStart()
                        startColumn = currentWidget.column ?: 0
                        startRow = currentWidget.row ?: 0
                        startColumnSpan = currentWidget.columnSpan ?: WIDGET_PANEL_DEFAULT_COLUMN_SPAN
                        startRowSpan = currentWidget.rowSpan ?: WIDGET_PANEL_DEFAULT_ROW_SPAN
                        totalHorizontalDelta = 0f
                        totalVerticalDelta = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalHorizontalDelta += dragAmount.x
                        totalVerticalDelta += dragAmount.y
                        onResize(
                            calculateGridResize(
                                startColumn = startColumn,
                                startRow = startRow,
                                startColumnSpan = startColumnSpan,
                                startRowSpan = startRowSpan,
                                horizontalDirection = direction.horizontal,
                                verticalDirection = direction.vertical,
                                horizontalDeltaPx = totalHorizontalDelta,
                                verticalDeltaPx = totalVerticalDelta,
                                gridUnitWidthPx = gridUnitWidthPx,
                                gridUnitHeightPx = gridUnitHeightPx,
                                minColumnSpan = minColumnSpan,
                                minRowSpan = minRowSpan,
                            ),
                        )
                    },
                    onDragEnd = onResizeFinished,
                    onDragCancel = onResizeFinished,
                )
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        content = {},
    )
}

@Composable
private fun WidgetManagementGesturePadding(
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun Modifier.openManagementOnLongPress(): Modifier =
        pointerInput(onLongPress) {
            detectTapGestures(onLongPress = { onLongPress() })
        }

    Box(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(WidgetManagementTouchPadding)
                    .openManagementOnLongPress(),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(WidgetManagementTouchPadding)
                    .openManagementOnLongPress(),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(WidgetManagementTouchPadding)
                    .openManagementOnLongPress(),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(WidgetManagementTouchPadding)
                    .openManagementOnLongPress(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetPickerSheet(
    appWidgetManager: AppWidgetManager,
    onDismiss: () -> Unit,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var query by rememberSaveable { mutableStateOf("") }
    val apps =
        remember(appWidgetManager, packageManager) {
            appWidgetManager.installedProviders
                .groupBy { it.provider.packageName }
                .map { (packageName, providers) ->
                    val appInfo =
                        runCatching { packageManager.getApplicationInfo(packageName, 0) }
                            .getOrNull()
                    WidgetPickerApp(
                        packageName = packageName,
                        appLabel =
                            appInfo
                                ?.let { packageManager.getApplicationLabel(it).toString() }
                                ?: packageName,
                        icon = appInfo?.let { packageManager.getApplicationIcon(it) },
                        widgets =
                            providers.sortedBy {
                                it.loadLabel(packageManager)?.toString().orEmpty().lowercase()
                            },
                    )
                }
                .sortedBy { it.appLabel.lowercase() }
        }
    val filteredApps =
        remember(apps, query) {
            val normalizedQuery = query.trim().lowercase()
            if (normalizedQuery.isBlank()) {
                apps
            } else {
                apps.mapNotNull { app ->
                    val matchingWidgets =
                        app.widgets.filter { provider ->
                            app.appLabel.lowercase().contains(normalizedQuery) ||
                                provider.loadLabel(packageManager)?.toString().orEmpty().lowercase()
                                    .contains(normalizedQuery)
                        }
                    if (matchingWidgets.isEmpty()) null else app.copy(widgets = matchingWidgets)
                }
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.getDialogContainerColor(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                    .padding(bottom = DesignTokens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            Text(
                text = stringResource(R.string.widgets_picker_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            WidgetPickerSearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    WidgetPickerAppGroup(
                        app = app,
                        packageManager = packageManager,
                        onSelectWidget = onSelectWidget,
                    )
                }
                if (filteredApps.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.widgets_picker_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(DesignTokens.SpacingLarge),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = DesignTokens.ShapeXXLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.SpacingLarge, vertical = DesignTokens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (query.isBlank()) {
                        Text(
                            text = stringResource(R.string.widgets_picker_search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )
        }
    }
}

@Composable
private fun WidgetPickerAppGroup(
    app: WidgetPickerApp,
    packageManager: PackageManager,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.ExtraLargeCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(DesignTokens.SpacingLarge),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                DrawableImage(
                    drawable = app.icon,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(DesignTokens.AppIconSize)
                            .clip(CircleShape),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.widgets_picker_widget_count,
                                app.widgets.size,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            app.widgets.forEachIndexed { index, provider ->
                if (index > 0) {
                    HorizontalDivider(color = AppColors.SettingsDivider)
                }
                WidgetPickerRow(
                    provider = provider,
                    packageManager = packageManager,
                    onClick = { onSelectWidget(provider) },
                )
            }
        }
    }
}

@Composable
private fun WidgetPickerRow(
    provider: AppWidgetProviderInfo,
    packageManager: PackageManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val preview = remember(provider) {
        runCatching { provider.loadPreviewImage(context, 0) }.getOrNull()
            ?: runCatching { provider.loadIcon(context, 0) }.getOrNull()
    }
    val minWidth = with(density) { provider.minWidth.toDp() }
    val minHeight = with(density) { provider.minHeight.toDp() }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(DesignTokens.SpacingLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Surface(
            modifier = Modifier.size(width = 92.dp, height = 64.dp),
            shape = DesignTokens.ShapeMedium,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Box(contentAlignment = Alignment.Center) {
                DrawableImage(
                    drawable = preview,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(DesignTokens.SpacingSmall),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = provider.loadLabel(packageManager)?.toString().orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    stringResource(
                        R.string.widgets_picker_widget_size,
                        minWidth.value.toInt(),
                        minHeight.value.toInt(),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AssistChip(
            onClick = onClick,
            label = { Text(stringResource(R.string.common_action_add)) },
        )
    }
}

@Composable
private fun DrawableImage(
    drawable: Drawable?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (drawable == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        )
        return
    }
    val bitmap =
        remember(drawable) {
            val width =
                drawable.intrinsicWidth
                    .takeIf { it > 0 }
                    ?: WIDGET_PREVIEW_FALLBACK_SIZE_PX
            val height =
                drawable.intrinsicHeight
                    .takeIf { it > 0 }
                    ?: WIDGET_PREVIEW_FALLBACK_SIZE_PX
            runCatching {
                drawable.toBitmap(width = width, height = height).asImageBitmap()
            }.getOrNull()
        }
    if (bitmap == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        Image(
            painter = BitmapPainter(bitmap),
            contentDescription = contentDescription,
            modifier = modifier,
        )
    }
}

@Composable
private fun BackHandlerCompat(onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
}
