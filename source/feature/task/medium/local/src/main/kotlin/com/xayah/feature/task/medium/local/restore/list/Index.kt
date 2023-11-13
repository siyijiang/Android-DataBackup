package com.xayah.feature.task.medium.local.restore.list

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.xayah.core.database.model.OperationMask
import com.xayah.core.ui.component.ActionChip
import com.xayah.core.ui.component.AnimatedRoundChip
import com.xayah.core.ui.component.RoundChip
import com.xayah.core.ui.model.ImageVectorToken
import com.xayah.core.ui.model.StringResourceToken
import com.xayah.core.ui.util.fromString
import com.xayah.core.ui.util.fromStringArgs
import com.xayah.core.ui.util.fromStringId
import com.xayah.core.ui.util.fromVector
import com.xayah.core.ui.util.value
import com.xayah.feature.task.medium.common.R
import com.xayah.feature.task.medium.common.component.ListScaffold
import com.xayah.feature.task.medium.common.component.MediumCard
import com.xayah.feature.task.medium.common.component.MediumCardShimmer
import com.xayah.feature.task.medium.local.restore.TaskPackagesRestoreRoutes

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun PageList(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<IndexViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topBarState by viewModel.topBarState.collectAsStateWithLifecycle()
    val selectedAPKsCount by viewModel.selectedAPKsCountState.collectAsStateWithLifecycle()
    val selectedDataCount by viewModel.selectedDataCountState.collectAsStateWithLifecycle()
    val packagesState by viewModel.packagesState.collectAsStateWithLifecycle()
    val packagesSelected by viewModel.packagesSelectedState.collectAsStateWithLifecycle()
    val packagesNotSelected by viewModel.packagesNotSelectedState.collectAsStateWithLifecycle()
    val shimmering by viewModel.shimmeringState.collectAsStateWithLifecycle()
    val timestampIndexState by viewModel.timestampIndexState.collectAsStateWithLifecycle()
    val timestampListState by viewModel.timestampListState.collectAsStateWithLifecycle()

    LaunchedEffect(null) {
        viewModel.emitIntent(IndexUiIntent.Initialize)
    }

    ListScaffold(
        topBarState = topBarState,
        fabVisible = uiState.activating.not(),
        fabEmphasizedState = uiState.emphasizedState,
        fabSelectedState = packagesSelected.isNotEmpty(),
        selectedDataCount = selectedDataCount,
        shimmering = shimmering,
        shimmerCount = uiState.shimmerCount,
        selectedItems = packagesSelected,
        notSelectedItems = packagesNotSelected,
        itemKey = { "${it.packageName} - ${it.operationCode}" },
        onFabClick = {
            if (packagesSelected.isEmpty()) viewModel.emitIntent(IndexUiIntent.Emphasize)
            else navController.navigate(TaskPackagesRestoreRoutes.Processing.route)
        },
        onSearchTextChange = { text ->
            viewModel.emitIntent(IndexUiIntent.FilterByKey(key = text))
        },
        actionChipGroup = { targetState ->
            ActionChip(
                enabled = targetState.not(),
                label = StringResourceToken.fromStringArgs(
                    StringResourceToken.fromStringId(R.string.batching_select),
                    StringResourceToken.fromString("(${uiState.batchSelection.size})"),
                ),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.Checklist),
                onClick = {
                    viewModel.emitIntent(IndexUiIntent.BatchingSelectAll)
                },
            )
            var batchingApkSelection by remember { mutableStateOf(true) }
            ActionChip(
                enabled = targetState.not(),
                label = StringResourceToken.fromStringId(R.string.apk),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.TripOrigin),
                onClick = {
                    viewModel.launchOnIO {
                        val packageNames = uiState.batchSelection.ifEmpty { packagesState.map { it.packageName } }

                        if (batchingApkSelection)
                            viewModel.emitIntent(IndexUiIntent.BatchOrOp(mask = OperationMask.Apk, packageNames = packageNames))
                        else
                            viewModel.emitIntent(IndexUiIntent.BatchAndOp(mask = OperationMask.Apk.inv(), packageNames = packageNames))

                        batchingApkSelection = batchingApkSelection.not()
                    }
                },
            )
            var batchingDataSelection by remember { mutableStateOf(true) }
            ActionChip(
                enabled = targetState.not(),
                label = StringResourceToken.fromStringId(R.string.data),
                leadingIcon = ImageVectorToken.fromVector(Icons.Rounded.TripOrigin),
                onClick = {
                    viewModel.launchOnIO {
                        val packageNames = uiState.batchSelection.ifEmpty { packagesState.map { it.packageName } }

                        if (batchingDataSelection)
                            viewModel.emitIntent(IndexUiIntent.BatchOrOp(mask = OperationMask.Data, packageNames = packageNames))
                        else
                            viewModel.emitIntent(IndexUiIntent.BatchAndOp(mask = OperationMask.Data.inv(), packageNames = packageNames))

                        batchingDataSelection = batchingDataSelection.not()
                    }
                },
            )
        },
        shimmerItem = {
            MediumCardShimmer()
        }
    ) { item ->
        val enabled = remember(item.sizeBytes) { item.isExists }

        MediumCard(
            enabled = enabled,
            cardSelected = item.packageName in uiState.batchSelection,
            packageRestore = item,
            onDataSelected = {
                                viewModel.emitIntent(
                                    IndexUiIntent.UpdatePackage(
                                        entity = item.copy(operationCode = item.operationCode xor OperationMask.Data)
                                    )
                                )
                            },
            onCardClick = {
                                if (uiState.batchSelection.isNotEmpty()) {
                                    viewModel.emitIntent(IndexUiIntent.BatchingSelect(packageName = item.packageName))
                                } else {
                                    viewModel.emitIntent(
                                        IndexUiIntent.UpdatePackage(
                                            entity = item.copy(operationCode = if (item.operationCode == OperationMask.None) item.backupOpCode else OperationMask.None)
                                        )
                                    )
                                }
                            },
            onCardLongClick = {
                                viewModel.emitIntent(IndexUiIntent.BatchingSelect(packageName = item.packageName))
                            },
        ) {
                        LaunchedEffect(item) {
                            viewModel.emitIntent(IndexUiIntent.UpdatePackageState(entity = item))
                        }
                        if (enabled) {
                            if (item.versionName.isNotEmpty()) RoundChip(text = item.versionName)
                            AnimatedRoundChip(text = item.sizeDisplay)
                            AnimatedRoundChip(text = StringResourceToken.fromStringId(if (item.installed) R.string.installed else R.string.not_installed).value)
                        } else {
                            RoundChip(text = StringResourceToken.fromStringId(R.string.not_exist).value, enabled = false)
                        }
                    }
    }
}
