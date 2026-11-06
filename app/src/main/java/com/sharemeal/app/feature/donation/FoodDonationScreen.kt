package com.sharemeal.app.feature.donation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.sharemeal.app.R
import com.sharemeal.app.core.media.ImageReference
import com.sharemeal.app.core.ui.Spacing
import com.sharemeal.app.core.ui.components.FeedbackBanner
import com.sharemeal.app.core.ui.components.FeedbackType
import com.sharemeal.app.core.ui.components.SectionHeader
import com.sharemeal.app.core.ui.components.ShareMealButton
import com.sharemeal.app.core.ui.components.ShareMealTextField
import com.sharemeal.app.core.ui.components.ShareMealTopAppBar
import com.sharemeal.app.core.ui.labelRes
import com.sharemeal.app.core.validation.DonationField
import com.sharemeal.app.domain.model.DonationQuantityUnit
import com.sharemeal.app.domain.model.FoodCategory
import com.sharemeal.app.domain.model.FoodClassification
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FoodDonationScreen(
    onBack: () -> Unit,
    onPublished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DonationFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val form = state.form
    val context = LocalContext.current

    var imageReplaceIndex by remember { mutableStateOf<Int?>(null) }

    val addImagesPicker = rememberLauncherForActivityResult(
        contract = PickMultipleVisualMedia(DonationFormViewModel.MAX_IMAGES)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(uris.map { it.toString() })
        }
    }
    val addSingleImagePicker = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onImagesSelected(listOf(uri.toString()))
        }
    }
    val replaceImagePicker = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri ->
        val index = imageReplaceIndex
        if (uri != null && index != null) {
            viewModel.onImageReplaced(index, uri.toString())
        }
        imageReplaceIndex = null
    }

    val launchAddImages = {
        val remainingSlots = DonationFormViewModel.MAX_IMAGES - form.images.size
        if (remainingSlots > 0) {
            val request = PickVisualMediaRequest(PickVisualMedia.ImageOnly)
            if (remainingSlots == 1) {
                addSingleImagePicker.launch(request)
            } else {
                addImagesPicker.launch(request)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is DonationFormEvent.Published) {
                onPublished()
            }
        }
    }

    val categoryOptions = FoodCategory.entries.map {
        DropdownOption(it, stringResource(it.labelRes()))
    }
    val quantityUnitOptions = DonationQuantityUnit.entries.map {
        DropdownOption(it, stringResource(it.labelRes()))
    }
    val classificationOptions = FoodClassification.entries.map {
        DropdownOption(it, stringResource(it.labelRes()))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShareMealTopAppBar(
                title = stringResource(R.string.donation_screen_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (state.fieldErrors.isNotEmpty()) {
                item {
                    FeedbackBanner(
                        message = stringResource(R.string.donation_error_validation_summary),
                        type = FeedbackType.ERROR
                    )
                }
            }

            state.errorMessageRes?.let { messageRes ->
                item {
                    FeedbackBanner(
                        message = stringResource(messageRes),
                        type = FeedbackType.ERROR
                    )
                }
            }

            item { SectionHeader(title = stringResource(R.string.donation_section_basic)) }
            item {
                ShareMealTextField(
                    value = form.title,
                    onValueChange = { viewModel.onFieldChange(DonationField.TITLE, it) },
                    label = stringResource(R.string.donation_field_title),
                    leadingIcon = Icons.Rounded.Restaurant,
                    isError = state.fieldErrors.containsKey(DonationField.TITLE),
                    errorMessage = fieldError(state.fieldErrors, DonationField.TITLE),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealTextField(
                    value = form.description,
                    onValueChange = { viewModel.onFieldChange(DonationField.DESCRIPTION, it) },
                    label = stringResource(R.string.donation_field_description),
                    isError = state.fieldErrors.containsKey(DonationField.DESCRIPTION),
                    errorMessage = fieldError(state.fieldErrors, DonationField.DESCRIPTION),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = false,
                    minLines = 3,
                    enabled = !state.isPublishing
                )
            }
            item {
                DonationDropdown(
                    label = stringResource(R.string.donation_field_category),
                    options = categoryOptions,
                    selected = form.category,
                    onSelect = viewModel::onCategoryChange,
                    leadingIcon = Icons.Rounded.Category,
                    errorMessage = fieldError(state.fieldErrors, DonationField.CATEGORY),
                    enabled = !state.isPublishing
                )
            }
            item {
                DonationImageManager(
                    images = form.images,
                    maxImages = DonationFormViewModel.MAX_IMAGES,
                    displayUri = viewModel::imageDisplayUri,
                    onAdd = { launchAddImages() },
                    onReplace = { index ->
                        imageReplaceIndex = index
                        replaceImagePicker.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemove = viewModel::onImageRemoved,
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealTextField(
                    value = form.quantity,
                    onValueChange = { viewModel.onFieldChange(DonationField.QUANTITY, it) },
                    label = stringResource(R.string.donation_field_quantity),
                    leadingIcon = Icons.Rounded.Scale,
                    isError = state.fieldErrors.containsKey(DonationField.QUANTITY),
                    errorMessage = fieldError(state.fieldErrors, DonationField.QUANTITY),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !state.isPublishing
                )
            }
            item {
                DonationDropdown(
                    label = stringResource(R.string.donation_field_quantity_unit),
                    options = quantityUnitOptions,
                    selected = form.quantityUnit,
                    onSelect = viewModel::onQuantityUnitChange,
                    leadingIcon = Icons.Rounded.Scale,
                    errorMessage = fieldError(state.fieldErrors, DonationField.QUANTITY_UNIT),
                    enabled = !state.isPublishing
                )
            }
            if (form.quantityUnit == DonationQuantityUnit.OTHER) {
                item {
                    ShareMealTextField(
                        value = form.customQuantityUnit,
                        onValueChange = {
                            viewModel.onFieldChange(DonationField.CUSTOM_QUANTITY_UNIT, it)
                        },
                        label = stringResource(R.string.donation_field_custom_unit),
                        isError = state.fieldErrors.containsKey(
                            DonationField.CUSTOM_QUANTITY_UNIT
                        ),
                        errorMessage = fieldError(
                            state.fieldErrors,
                            DonationField.CUSTOM_QUANTITY_UNIT
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !state.isPublishing
                    )
                }
            }

            item {
                SectionHeader(title = stringResource(R.string.donation_section_classification))
            }
            item {
                DonationDropdown(
                    label = stringResource(R.string.donation_section_classification),
                    options = classificationOptions,
                    selected = form.classification,
                    onSelect = viewModel::onClassificationChange,
                    leadingIcon = Icons.Rounded.Restaurant,
                    errorMessage = fieldError(state.fieldErrors, DonationField.CLASSIFICATION),
                    enabled = !state.isPublishing
                )
            }

            item { SectionHeader(title = stringResource(R.string.donation_section_preparation)) }
            item {
                DonationPickerField(
                    label = stringResource(R.string.donation_field_preparation_date),
                    value = form.preparationDateMillis?.let(::formatDate),
                    placeholder = stringResource(R.string.donation_select_date),
                    leadingIcon = Icons.Rounded.CalendarMonth,
                    onClick = {
                        showDatePicker(
                            context = context,
                            initialMillis = form.preparationDateMillis,
                            onDateSelected = viewModel::onPreparationDateSelected
                        )
                    },
                    errorMessage = fieldError(state.fieldErrors, DonationField.PREPARATION_DATE),
                    enabled = !state.isPublishing
                )
            }
            item {
                DonationPickerField(
                    label = stringResource(R.string.donation_field_preparation_time),
                    value = form.preparationTimeMinutes?.let { formatTime(context, it) },
                    placeholder = stringResource(R.string.donation_select_time),
                    leadingIcon = Icons.Rounded.AccessTime,
                    onClick = {
                        showTimePicker(
                            context = context,
                            initialMinutes = form.preparationTimeMinutes,
                            onTimeSelected = viewModel::onPreparationTimeSelected
                        )
                    },
                    errorMessage = fieldError(state.fieldErrors, DonationField.PREPARATION_TIME),
                    enabled = !state.isPublishing
                )
            }
            item {
                DonationPickerField(
                    label = stringResource(R.string.donation_field_pickup_start),
                    value = form.pickupStartMinutes?.let { formatTime(context, it) },
                    placeholder = stringResource(R.string.donation_select_time),
                    leadingIcon = Icons.Rounded.Schedule,
                    onClick = {
                        showTimePicker(
                            context = context,
                            initialMinutes = form.pickupStartMinutes,
                            onTimeSelected = viewModel::onPickupStartSelected
                        )
                    },
                    errorMessage = fieldError(state.fieldErrors, DonationField.PICKUP_START),
                    enabled = !state.isPublishing
                )
            }
            item {
                DonationPickerField(
                    label = stringResource(R.string.donation_field_pickup_deadline),
                    value = form.pickupDeadlineMinutes?.let { formatTime(context, it) },
                    placeholder = stringResource(R.string.donation_select_time),
                    leadingIcon = Icons.Rounded.Alarm,
                    onClick = {
                        showTimePicker(
                            context = context,
                            initialMinutes = form.pickupDeadlineMinutes,
                            onTimeSelected = viewModel::onPickupDeadlineSelected
                        )
                    },
                    errorMessage = fieldError(state.fieldErrors, DonationField.PICKUP_DEADLINE),
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealTextField(
                    value = form.storageConditions,
                    onValueChange = {
                        viewModel.onFieldChange(DonationField.STORAGE_CONDITIONS, it)
                    },
                    label = stringResource(R.string.donation_field_storage),
                    isError = state.fieldErrors.containsKey(DonationField.STORAGE_CONDITIONS),
                    errorMessage = fieldError(
                        state.fieldErrors,
                        DonationField.STORAGE_CONDITIONS
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = false,
                    minLines = 2,
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealTextField(
                    value = form.packaging,
                    onValueChange = { viewModel.onFieldChange(DonationField.PACKAGING, it) },
                    label = stringResource(R.string.donation_field_packaging),
                    isError = state.fieldErrors.containsKey(DonationField.PACKAGING),
                    errorMessage = fieldError(state.fieldErrors, DonationField.PACKAGING),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = false,
                    minLines = 2,
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealTextField(
                    value = form.allergenInformation,
                    onValueChange = { viewModel.onFieldChange(DonationField.ALLERGENS, it) },
                    label = stringResource(R.string.donation_field_allergens),
                    isError = state.fieldErrors.containsKey(DonationField.ALLERGENS),
                    errorMessage = fieldError(state.fieldErrors, DonationField.ALLERGENS),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isPublishing
                )
            }
            item {
                ConditionDeclaration(
                    checked = form.conditionDeclared,
                    onCheckedChange = viewModel::onConditionDeclaredChange,
                    errorMessage = fieldError(
                        state.fieldErrors,
                        DonationField.CONDITION_DECLARATION
                    ),
                    enabled = !state.isPublishing
                )
            }

            item { SectionHeader(title = stringResource(R.string.donation_section_pickup)) }
            item {
                ShareMealTextField(
                    value = form.pickupAddress,
                    onValueChange = {
                        viewModel.onFieldChange(DonationField.PICKUP_ADDRESS, it)
                    },
                    label = stringResource(R.string.donation_field_pickup_address),
                    leadingIcon = Icons.Rounded.Place,
                    isError = state.fieldErrors.containsKey(DonationField.PICKUP_ADDRESS),
                    errorMessage = fieldError(
                        state.fieldErrors,
                        DonationField.PICKUP_ADDRESS
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = false,
                    minLines = 2,
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealTextField(
                    value = form.city,
                    onValueChange = { viewModel.onFieldChange(DonationField.CITY, it) },
                    label = stringResource(R.string.field_city),
                    isError = state.fieldErrors.containsKey(DonationField.CITY),
                    errorMessage = fieldError(state.fieldErrors, DonationField.CITY),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealTextField(
                    value = form.state,
                    onValueChange = { viewModel.onFieldChange(DonationField.STATE, it) },
                    label = stringResource(R.string.field_state),
                    isError = state.fieldErrors.containsKey(DonationField.STATE),
                    errorMessage = fieldError(state.fieldErrors, DonationField.STATE),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealTextField(
                    value = form.pinCode,
                    onValueChange = { viewModel.onFieldChange(DonationField.PIN_CODE, it) },
                    label = stringResource(R.string.field_pin_code),
                    isError = state.fieldErrors.containsKey(DonationField.PIN_CODE),
                    errorMessage = fieldError(state.fieldErrors, DonationField.PIN_CODE),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealTextField(
                    value = form.contactPerson,
                    onValueChange = {
                        viewModel.onFieldChange(DonationField.CONTACT_PERSON, it)
                    },
                    label = stringResource(R.string.field_contact_person),
                    leadingIcon = Icons.Rounded.Person,
                    isError = state.fieldErrors.containsKey(DonationField.CONTACT_PERSON),
                    errorMessage = fieldError(state.fieldErrors, DonationField.CONTACT_PERSON),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealTextField(
                    value = form.contactPhone,
                    onValueChange = {
                        viewModel.onFieldChange(DonationField.CONTACT_PHONE, it)
                    },
                    label = stringResource(R.string.donation_field_contact_phone),
                    leadingIcon = Icons.Rounded.Phone,
                    isError = state.fieldErrors.containsKey(DonationField.CONTACT_PHONE),
                    errorMessage = fieldError(state.fieldErrors, DonationField.CONTACT_PHONE),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealTextField(
                    value = form.specialInstructions,
                    onValueChange = viewModel::onSpecialInstructionsChange,
                    label = stringResource(R.string.donation_field_special_instructions),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = false,
                    minLines = 2,
                    enabled = !state.isPublishing
                )
            }
            item {
                ShareMealButton(
                    text = stringResource(R.string.donation_publish),
                    onClick = viewModel::publishDonation,
                    isLoading = state.isPublishing,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun fieldError(errors: Map<DonationField, Int>, field: DonationField): String? =
    errors[field]?.let { stringResource(it) }

@Composable
private fun <T> DonationDropdown(
    label: String,
    options: List<DropdownOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    leadingIcon: ImageVector,
    errorMessage: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selected }?.label
        ?: stringResource(R.string.donation_select_option)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (errorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(imageVector = leadingIcon, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option.label) },
                        onClick = {
                            onSelect(option.value)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xs)
            )
        }
    }
}

@Composable
private fun DonationPickerField(
    label: String,
    value: String?,
    placeholder: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit,
    errorMessage: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (errorMessage != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(imageVector = leadingIcon, contentDescription = null)
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = value ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    LocalContentColor.current
                },
                modifier = Modifier.weight(1f)
            )
            Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xs)
            )
        }
    }
}

@Composable
private fun DonationImageManager(
    images: List<ImageReference>,
    maxImages: Int,
    displayUri: (ImageReference) -> String,
    onAdd: () -> Unit,
    onReplace: (Int) -> Unit,
    onRemove: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.donation_field_image),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(
                    R.string.donation_images_count,
                    images.size,
                    maxImages
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))

        if (images.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clickable(enabled = enabled, onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = stringResource(R.string.donation_image_select),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(end = Spacing.sm)
            ) {
                itemsIndexed(
                    items = images,
                    key = { _, image -> image.id }
                ) { index, image ->
                    Column(modifier = Modifier.width(150.dp)) {
                        AsyncImage(
                            model = displayUri(image),
                            contentDescription = stringResource(
                                R.string.donation_image_content_description
                            ),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        if (image.isLocalOnly) {
                            Text(
                                text = stringResource(R.string.donation_image_local_preview),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = Spacing.xs)
                            )
                        }
                        Row {
                            TextButton(
                                onClick = { onReplace(index) },
                                enabled = enabled,
                                contentPadding = PaddingValues(horizontal = Spacing.sm)
                            ) {
                                Text(
                                    text = stringResource(R.string.donation_image_replace),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            TextButton(
                                onClick = { onRemove(image.id) },
                                enabled = enabled,
                                contentPadding = PaddingValues(horizontal = Spacing.sm)
                            ) {
                                Text(
                                    text = stringResource(R.string.donation_image_remove),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                if (images.size < maxImages) {
                    item {
                        OutlinedButton(
                            onClick = onAdd,
                            enabled = enabled,
                            modifier = Modifier
                                .width(110.dp)
                                .height(110.dp),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(Spacing.sm)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                                Text(
                                    text = stringResource(R.string.donation_image_add),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.donation_images_local_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConditionDeclaration(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    errorMessage: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = stringResource(R.string.donation_condition_declaration),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xs)
            )
        }
    }
}

private fun showDatePicker(
    context: Context,
    initialMillis: Long?,
    onDateSelected: (Long) -> Unit
) {
    val initial = Calendar.getInstance().apply {
        if (initialMillis != null) {
            timeInMillis = initialMillis
        }
    }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
            onDateSelected(selected.timeInMillis)
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showTimePicker(
    context: Context,
    initialMinutes: Int?,
    onTimeSelected: (Int) -> Unit
) {
    val initial = Calendar.getInstance().apply {
        if (initialMinutes != null) {
            set(Calendar.HOUR_OF_DAY, initialMinutes / 60)
            set(Calendar.MINUTE, initialMinutes % 60)
        }
    }
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onTimeSelected(hourOfDay * 60 + minute)
        },
        initial.get(Calendar.HOUR_OF_DAY),
        initial.get(Calendar.MINUTE),
        DateFormat.is24HourFormat(context)
    ).show()
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))

private fun formatTime(context: Context, minutes: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}

private data class DropdownOption<T>(
    val value: T,
    val label: String
)
