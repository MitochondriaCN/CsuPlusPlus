package ink.qingshuige.csuplusplus.presentation.page.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ink.qingshuige.csuplusplus.data.dto.CollegeResponse

@Composable
internal fun HomeAdminDialogHost(
    dialog: HomeAdminDialog,
    universityName: String?,
    colleges: List<CollegeResponse>,
    selectedCollegeId: Long?,
    isSubmitting: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onCreateUniversity: (String) -> Unit,
    onCreateCollege: (String) -> Unit,
    onCreateCourse: (name: String, collegeId: Long?, alias: String?, hours: Int?) -> Unit,
    onConfirmDeleteCourse: () -> Unit,
) {
    when (dialog) {
        HomeAdminDialog.CreateUniversity -> SingleNameAdminDialog(
            title = "新建大学",
            label = "大学名称",
            placeholder = "请输入 2–64 个字符",
            isSubmitting = isSubmitting,
            errorMessage = errorMessage,
            onClearError = onClearError,
            onDismiss = onDismiss,
            onConfirm = onCreateUniversity,
        )

        HomeAdminDialog.CreateCollege -> SingleNameAdminDialog(
            title = "新建学院",
            subtitle = universityName?.takeIf { it.isNotBlank() }?.let { "所属大学：$it" },
            label = "学院名称",
            placeholder = "请输入 2–64 个字符",
            isSubmitting = isSubmitting,
            errorMessage = errorMessage,
            onClearError = onClearError,
            onDismiss = onDismiss,
            onConfirm = onCreateCollege,
        )

        HomeAdminDialog.CreateCourse -> CreateCourseDialog(
            colleges = colleges,
            initialCollegeId = selectedCollegeId,
            isSubmitting = isSubmitting,
            errorMessage = errorMessage,
            onClearError = onClearError,
            onDismiss = onDismiss,
            onConfirm = onCreateCourse,
        )

        is HomeAdminDialog.DeleteCourse -> ConfirmDeleteDialog(
            title = "删除课程",
            message = "确定要删除「${dialog.course.name}」吗？删除后将无法恢复。",
            isDeleting = isSubmitting,
            errorMessage = errorMessage,
            onConfirm = onConfirmDeleteCourse,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun SingleNameAdminDialog(
    title: String,
    label: String,
    placeholder: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    subtitle: String? = null,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onClearError()
                    },
                    label = { Text(label) },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                AdminErrorText(errorMessage)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = !isSubmitting && name.trim().length >= 2,
            ) {
                SubmittingLabel(isSubmitting = isSubmitting, idleText = "创建", busyText = "创建中...")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateCourseDialog(
    colleges: List<CollegeResponse>,
    initialCollegeId: Long?,
    isSubmitting: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (name: String, collegeId: Long?, alias: String?, hours: Int?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var hoursText by remember { mutableStateOf("") }
    var collegeId by remember(initialCollegeId) { mutableStateOf(initialCollegeId) }
    var collegeMenuExpanded by remember { mutableStateOf(false) }

    val selectedCollegeName = colleges.find { it.id == collegeId }?.name ?: "不指定学院"
    val parsedHours = hoursText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
    val isHoursValid = hoursText.isBlank() || (parsedHours != null && parsedHours >= 1)

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("新建课程") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        if (it.length <= 20) name = it
                        onClearError()
                    },
                    label = { Text("课程名称 *") },
                    placeholder = { Text("1–20 个字符") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = collegeMenuExpanded,
                    onExpandedChange = { if (!isSubmitting) collegeMenuExpanded = !collegeMenuExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedCollegeName,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isSubmitting,
                        label = { Text("所属学院") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = collegeMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = collegeMenuExpanded,
                        onDismissRequest = { collegeMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("不指定学院") },
                            onClick = {
                                collegeId = null
                                collegeMenuExpanded = false
                            },
                        )
                        colleges.forEach { college ->
                            DropdownMenuItem(
                                text = { Text(college.name) },
                                onClick = {
                                    collegeId = college.id
                                    collegeMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = alias,
                        onValueChange = {
                            if (it.length <= 20) alias = it
                            onClearError()
                        },
                        label = { Text("课程别名") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = {
                            if (it.all(Char::isDigit)) hoursText = it
                            onClearError()
                        },
                        label = { Text("学时") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        isError = !isHoursValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(96.dp),
                    )
                }

                AdminErrorText(errorMessage)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, collegeId, alias, parsedHours) },
                enabled = !isSubmitting && name.isNotBlank() && isHoursValid,
            ) {
                SubmittingLabel(isSubmitting = isSubmitting, idleText = "创建", busyText = "创建中...")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("取消") }
        },
    )
}

@Composable
private fun AdminErrorText(errorMessage: String?) {
    errorMessage?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SubmittingLabel(
    isSubmitting: Boolean,
    idleText: String,
    busyText: String,
) {
    if (isSubmitting) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = LocalContentColor.current,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(busyText)
    } else {
        Text(idleText)
    }
}
