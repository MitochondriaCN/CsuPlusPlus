package ink.qingshuige.csuplusplus.presentation.page.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthPage(
    viewModel: AuthPageViewModel = koinViewModel(),
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (uiState is AuthPageUiState.Success) {
            viewModel.resetToLogin()
        }
    }

    when (val state = uiState) {
        is AuthPageUiState.LoggingIn -> LoggingInScreen(
            modifier = modifier,
            errorMessage = state.errorMessage,
            isLoading = state.isLoading,
            onSwitchToSignUp = viewModel::switchToSignUp,
            onSubmit = viewModel::login,
        )

        is AuthPageUiState.SigningUp -> SigningUpScreen(
            state = state,
            modifier = modifier,
            onSelectUniversity = viewModel::selectUniversity,
            onSelectCollege = viewModel::selectCollege,
            onRetryUniversities = viewModel::loadUniversities,
            onSwitchToLogin = viewModel::switchToLogin,
            onSubmit = viewModel::signUp,
        )

        AuthPageUiState.Success -> {
            LaunchedEffect(Unit) {
                onFinished()
            }
            LoggingInScreen(
                modifier = modifier,
                isLoading = true,
                onSwitchToSignUp = {},
                onSubmit = { _, _ -> },
            )
        }
    }
}

@Composable
private fun LoggingInScreen(
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onSwitchToSignUp: () -> Unit,
    onSubmit: (email: String, password: String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val showFailure = errorMessage != null
    val canSubmit = email.isNotBlank() && password.isNotBlank()

    fun submit() {
        if (canSubmit && !isLoading) {
            focusManager.clearFocus()
            onSubmit(email, password)
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "登录",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "使用邮箱和密码登录您的账号",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    label = { Text("邮箱") },
                    placeholder = { Text("name@example.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Lucide.Mail,
                            contentDescription = null,
                        )
                    },
                    isError = showFailure,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    label = { Text("密码") },
                    leadingIcon = {
                        Icon(
                            imageVector = Lucide.Lock,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            enabled = !isLoading,
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Lucide.EyeOff else Lucide.Eye,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                            )
                        }
                    },
                    isError = showFailure,
                    supportingText = if (errorMessage != null) {
                        {
                            ErrorSupportingLine(message = errorMessage)
                        }
                    } else {
                        null
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submit() },
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { submit() },
                    enabled = canSubmit && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current,
                        )
                    } else {
                        Text("登录")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "还没有账号？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onSwitchToSignUp,
                        enabled = !isLoading,
                    ) {
                        Text("立即注册")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SigningUpScreen(
    state: AuthPageUiState.SigningUp,
    modifier: Modifier = Modifier,
    onSelectUniversity: (Long) -> Unit,
    onSelectCollege: (Long) -> Unit,
    onRetryUniversities: () -> Unit,
    onSwitchToLogin: () -> Unit,
    onSubmit: (email: String, password: String, nickname: String, collegeId: Long) -> Unit,
) {
    var nickname by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var universityExpanded by rememberSaveable { mutableStateOf(false) }
    var collegeExpanded by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val selectedUniversity = state.universities.find { it.id == state.selectedUniversityId }
    val selectedCollege = state.colleges.find { it.id == state.selectedCollegeId }
    val passwordMismatch = confirmPassword.isNotEmpty() && confirmPassword != password

    val canSubmit = nickname.isNotBlank() &&
            email.isNotBlank() &&
            selectedCollege != null &&
            password.isNotBlank() &&
            password == confirmPassword

    fun submit() {
        val collegeId = selectedCollege?.id ?: return
        if (canSubmit && !state.isLoading) {
            focusManager.clearFocus()
            onSubmit(email, password, nickname, collegeId)
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "注册",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "创建您的账号并选择所属学院",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    label = { Text("昵称") },
                    placeholder = { Text("输入您的昵称") },
                    leadingIcon = {
                        Icon(
                            imageVector = Lucide.User,
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    label = { Text("邮箱") },
                    placeholder = { Text("name@example.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Lucide.Mail,
                            contentDescription = null,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )

                ExposedDropdownMenuBox(
                    expanded = universityExpanded,
                    onExpandedChange = {
                        if (!state.isLoading && !state.isLoadingUniversities) {
                            universityExpanded = it
                        }
                    },
                ) {
                    OutlinedTextField(
                        value = selectedUniversity?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = !state.isLoading && !state.isLoadingUniversities,
                            ),
                        label = { Text("学校") },
                        placeholder = {
                            Text(if (state.isLoadingUniversities) "正在加载学校..." else "请选择学校")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Lucide.GraduationCap,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            if (state.isLoadingUniversities) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = universityExpanded)
                            }
                        },
                        singleLine = true,
                    )

                    ExposedDropdownMenu(
                        expanded = universityExpanded,
                        onDismissRequest = { universityExpanded = false },
                    ) {
                        if (state.universities.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("重新加载学校列表") },
                                onClick = {
                                    universityExpanded = false
                                    onRetryUniversities()
                                },
                            )
                        } else {
                            state.universities.forEach { university ->
                                DropdownMenuItem(
                                    text = { Text(university.name) },
                                    onClick = {
                                        universityExpanded = false
                                        onSelectUniversity(university.id)
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }

                val collegeEnabled = !state.isLoading && state.selectedUniversityId != null && !state.isLoadingColleges
                ExposedDropdownMenuBox(
                    expanded = collegeExpanded,
                    onExpandedChange = {
                        if (collegeEnabled) {
                            collegeExpanded = it
                        }
                    },
                ) {
                    OutlinedTextField(
                        value = selectedCollege?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        enabled = !state.isLoading && state.selectedUniversityId != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = collegeEnabled,
                            ),
                        label = { Text("学院") },
                        placeholder = {
                            Text(
                                when {
                                    state.selectedUniversityId == null -> "请先选择学校"
                                    state.isLoadingColleges -> "正在加载学院..."
                                    else -> "请选择学院"
                                },
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Lucide.BookOpen,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            if (state.isLoadingColleges) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = collegeExpanded)
                            }
                        },
                        singleLine = true,
                    )

                    ExposedDropdownMenu(
                        expanded = collegeExpanded,
                        onDismissRequest = { collegeExpanded = false },
                    ) {
                        if (state.colleges.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("暂无可选学院") },
                                enabled = false,
                                onClick = { collegeExpanded = false },
                            )
                        } else {
                            state.colleges.forEach { college ->
                                DropdownMenuItem(
                                    text = { Text(college.name) },
                                    onClick = {
                                        collegeExpanded = false
                                        onSelectCollege(college.id)
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    label = { Text("密码") },
                    leadingIcon = {
                        Icon(
                            imageVector = Lucide.Lock,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            enabled = !state.isLoading,
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Lucide.EyeOff else Lucide.Eye,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    label = { Text("确认密码") },
                    leadingIcon = {
                        Icon(
                            imageVector = Lucide.Lock,
                            contentDescription = null,
                        )
                    },
                    isError = passwordMismatch,
                    supportingText = if (passwordMismatch) {
                        {
                            ErrorSupportingLine(message = "两次输入的密码不一致")
                        }
                    } else {
                        null
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submit() },
                    ),
                )

                state.errorMessage?.let { errorMessage ->
                    ErrorSupportingLine(
                        message = errorMessage,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        useErrorColor = true,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { submit() },
                    enabled = canSubmit && !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current,
                        )
                    } else {
                        Text("注册")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "已有账号？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onSwitchToLogin,
                        enabled = !state.isLoading,
                    ) {
                        Text("返回登录")
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorSupportingLine(
    message: String,
    modifier: Modifier = Modifier,
    useErrorColor: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Lucide.CircleAlert,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (useErrorColor) MaterialTheme.colorScheme.error else LocalContentColor.current,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = if (useErrorColor) MaterialTheme.colorScheme.error else LocalContentColor.current,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}