package com.example.nudgev1

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nudgev1.data.ClassSchedule
import com.example.nudgev1.data.Task
import com.example.nudgev1.weather.WeatherViewModel
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- COLORS (Ultramarine Blue Theme) ---
val NudgeBlue = Color(0xFF2B4CFF)
val NudgeLightBlue = Color(0xFFE8ECFF)
val NudgeBackground = Color(0xFFFAF7F2)
val NudgeDarkBrown = Color(0xFF2E1C15)

val NudgeRed = Color(0xFFE53935)
val NudgeRedLight = Color(0xFFFFEBEE)
val NudgeGreen = Color(0xFF43A047)
val NudgeGreenLight = Color(0xFFE8F5E9)
val NudgeYellow = Color(0xFFFBC02D)
val NudgeYellowLight = Color(0xFFFFF9C4)

// --- CUSTOM FONT ---
val Fredoka = FontFamily(Font(R.font.fredoka_variable, weight = FontWeight.Normal))

private val defaultTypography = Typography()
val NudgeTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = Fredoka),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = Fredoka),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = Fredoka),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = Fredoka),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = Fredoka),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = Fredoka),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = Fredoka),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = Fredoka),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = Fredoka),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = Fredoka),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = Fredoka),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = Fredoka),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = Fredoka),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = Fredoka),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = Fredoka)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = NudgeBlue,
                    secondary = NudgeLightBlue,
                    background = NudgeBackground
                ),
                typography = NudgeTypography
            ) {
                val context = LocalContext.current
                val prefs = context.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
                var userName by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
                var userCity by remember { mutableStateOf(prefs.getString("user_city", "") ?: "") }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { }
                    LaunchedEffect(Unit) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                if (userName.isBlank() || userCity.isBlank()) {
                    OnboardingScreen { name, city ->
                        prefs.edit().putString("user_name", name).putString("user_city", city).apply()
                        userName = name
                        userCity = city
                    }
                } else {
                    NudgeApp(userName = userName, userCity = userCity)
                }
            }
        }
    }
}

@Composable
fun NudgeApp(userName: String, userCity: String) {
    val navController = rememberNavController()
    val taskViewModel: TaskViewModel = viewModel()
    val classViewModel: ClassViewModel = viewModel()
    val aiViewModel: AIViewModel = viewModel()
    val weatherViewModel: WeatherViewModel = viewModel()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showAddClassDialog by remember { mutableStateOf(false) }
    var activeFocusTask by remember { mutableStateOf<Task?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(NudgeBackground)) {
        Scaffold(
            bottomBar = { BottomNavigationBar(navController) },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = NudgeBackground
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("home") {
                    HomeScreen(
                        navController = navController,
                        taskViewModel = taskViewModel,
                        classViewModel = classViewModel,
                        weatherViewModel = weatherViewModel,
                        userName = userName,
                        userCity = userCity,
                        onAddClassClick = { showAddClassDialog = true }
                    )
                }
                composable("tasks") {
                    TasksScreen(
                        viewModel = taskViewModel,
                        onAddTaskClick = { showAddTaskDialog = true },
                        onEditTask = { task -> editingTask = task },
                        onStartFocus = { task -> activeFocusTask = task },
                        onShowUndoSnackbar = { deletedTask ->
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Task deleted: ${deletedTask.title}",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    taskViewModel.addTask(deletedTask)
                                }
                            }
                        }
                    )
                }
                composable("weather") { WeatherScreen(userCity = userCity, viewModel = weatherViewModel) }
                composable("study") { StudyAIScreen(viewModel = aiViewModel) }
                composable("calendar") { CalendarScreen(taskViewModel = taskViewModel, navController = navController) }
            }
        }

        if (showAddTaskDialog || editingTask != null) {
            val context = LocalContext.current
            AddTaskDialog(
                taskToEdit = editingTask,
                onDismiss = {
                    showAddTaskDialog = false
                    editingTask = null
                },
                onSaveTask = { title, subject, category, deadline, priority ->
                    val currentEditTask = editingTask
                    if (currentEditTask != null) {
                        NotificationHelper.cancelReminder(context, currentEditTask.title)
                        val updated = currentEditTask.copy(
                            title = title,
                            subject = subject,
                            category = category,
                            deadline = deadline,
                            priority = priority
                        )
                        taskViewModel.updateTask(updated)
                    } else {
                        val newTask = Task(
                            title = title,
                            subject = subject,
                            category = category,
                            deadline = deadline,
                            priority = priority
                        )
                        taskViewModel.addTask(newTask)
                    }
                    NotificationHelper.sendTaskNotification(context, title)
                    showAddTaskDialog = false
                    editingTask = null
                }
            )
        }

        if (showAddClassDialog) {
            AddClassDialog(
                onDismiss = { showAddClassDialog = false },
                onSaveClass = { subjectName, room, startTime, endTime ->
                    classViewModel.addClass(
                        ClassSchedule(
                            subjectName = subjectName,
                            room = room,
                            startTime = startTime,
                            endTime = endTime
                        )
                    )
                    showAddClassDialog = false
                }
            )
        }

        activeFocusTask?.let { task ->
            FocusModeDialog(
                task = task,
                onDismiss = { activeFocusTask = null },
                onCompleteSession = { minutes ->
                    taskViewModel.addStudyTime(task, minutes)
                    activeFocusTask = null
                }
            )
        }
    }
}

// --- NAVIGATION BAR ---
@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Triple("home", "Home", Icons.Outlined.Home),
        Triple("tasks", "Tasks", Icons.Outlined.CheckCircle),
        Triple("weather", "Weather", Icons.Outlined.WbSunny),
        Triple("study", "Study AI", Icons.Outlined.Psychology)
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 12.sp, fontFamily = Fredoka) },
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NudgeBlue,
                    selectedTextColor = NudgeBlue,
                    indicatorColor = NudgeLightBlue,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

// --- HOME SCREEN ---
@Composable
fun HomeScreen(
    navController: NavController,
    taskViewModel: TaskViewModel,
    classViewModel: ClassViewModel,
    weatherViewModel: WeatherViewModel,
    userName: String,
    userCity: String,
    onAddClassClick: () -> Unit
) {
    val taskList by taskViewModel.tasks.collectAsStateWithLifecycle()
    val classList by classViewModel.classes.collectAsStateWithLifecycle()
    val userStats by taskViewModel.userStats.collectAsStateWithLifecycle()
    val weatherUiState by weatherViewModel.uiState.collectAsStateWithLifecycle()

    val pendingCount = taskList.count { !it.isCompleted }
    val doneCount = taskList.count { it.isCompleted }
    val upcomingTasks = taskList.filter { !it.isCompleted }.take(3)
    val streak = userStats?.currentStreak ?: 1

    val greetingText = if (pendingCount > 0) "You've got $pendingCount things to do." else "You're all caught up! 🎉"

    LaunchedEffect(userCity) {
        weatherViewModel.fetchWeatherForCity(userCity)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NudgeBackground),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NudgeBlue, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.nudge_wordmark),
                            contentDescription = "Nudge Wordmark",
                            modifier = Modifier.height(26.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { navController.navigate("calendar") }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Welcome back!", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontFamily = Fredoka)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Hey $userName! 👋", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("$streak 🔥", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
                        }
                    }
                    Text(greetingText, color = Color.White, fontSize = 20.sp, fontFamily = Fredoka)

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.WbSunny, contentDescription = "Weather", tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(weatherUiState.temp, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
                                Text("${weatherUiState.condition} • ${weatherUiState.city}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = Fredoka)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Feels like ${weatherUiState.feelsLike}", color = NudgeBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = (-10).dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(pendingCount.toString(), "Pending")
                StatCard("0", "Due today")
                StatCard(doneCount.toString(), "Done")
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Today's classes", Icons.Outlined.DateRange)
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Class",
                        tint = NudgeBlue,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onAddClassClick() }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (classList.isEmpty()) {
                    Text("No classes today.", color = Color.Gray, fontSize = 14.sp, fontFamily = Fredoka)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(classList, key = { it.id }) { schedule ->
                            ClassCard(
                                time = "${schedule.startTime} - ${schedule.endTime}",
                                title = schedule.subjectName,
                                room = schedule.room
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Upcoming deadlines", Icons.AutoMirrored.Outlined.List)
                    Text(
                        "See all",
                        color = NudgeBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Fredoka,
                        modifier = Modifier.clickable { navController.navigate("tasks") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (upcomingTasks.isEmpty()) {
                    Text("No upcoming deadlines. Enjoy your free time! 🥳", color = Color.Gray, fontSize = 14.sp, fontFamily = Fredoka)
                } else {
                    upcomingTasks.forEach { task ->
                        DeadlineCard(
                            title = task.title,
                            tag = task.category,
                            time = task.deadline,
                            tagBg = when (task.category) {
                                "Exam" -> NudgeRedLight
                                "Project" -> NudgeGreenLight
                                else -> NudgeYellowLight
                            },
                            tagText = when (task.category) {
                                "Exam" -> NudgeRed
                                "Project" -> NudgeGreen
                                else -> NudgeYellow
                            }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(NudgeDarkBrown, RoundedCornerShape(16.dp))
                    .clickable { navController.navigate("study") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = NudgeBlue)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Stuck on a topic?", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
                    Text("Ask your AI study buddy anything", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = Fredoka)
                }
            }
        }
    }
}

// --- TASKS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TaskViewModel,
    onAddTaskClick: () -> Unit,
    onEditTask: (Task) -> Unit,
    onStartFocus: (Task) -> Unit,
    onShowUndoSnackbar: (Task) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Due Date") }

    val filters = listOf("All", "Pending", "Done", "Assignment", "Project")
    val sortOptions = listOf("Due Date", "Priority", "Alphabetical")

    val taskList by viewModel.tasks.collectAsStateWithLifecycle()

    val totalTasks = taskList.size
    val completedTasks = taskList.count { it.isCompleted }
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    val filteredTasks = taskList.filter { task ->
        when (selectedFilter) {
            "All" -> true
            "Pending" -> !task.isCompleted
            "Done" -> task.isCompleted
            else -> task.category == selectedFilter
        }
    }.sortedWith { a, b ->
        when (sortBy) {
            "Priority" -> {
                val priorityMap = mapOf("High" to 1, "Medium" to 2, "Low" to 3)
                (priorityMap[a.priority] ?: 2).compareTo(priorityMap[b.priority] ?: 2)
            }
            "Alphabetical" -> a.title.compareTo(b.title, ignoreCase = true)
            else -> a.deadline.compareTo(b.deadline)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(NudgeBackground)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("$completedTasks / $totalTasks completed", color = Color.Gray, fontSize = 14.sp, fontFamily = Fredoka)
                    Text("My Tasks", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = NudgeDarkBrown, fontFamily = Fredoka)
                }
                NudgeLogo()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = NudgeBlue,
                trackColor = NudgeLightBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontFamily = Fredoka) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NudgeBlue,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Sort by:", fontSize = 12.sp, color = Color.Gray, fontFamily = Fredoka)
                sortOptions.forEach { option ->
                    Box(
                        modifier = Modifier
                            .background(if (sortBy == option) NudgeLightBlue else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { sortBy = option }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            option,
                            fontSize = 12.sp,
                            color = if (sortBy == option) NudgeBlue else Color.Gray,
                            fontWeight = if (sortBy == option) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = Fredoka
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Assignment, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No tasks yet! Tap + to add one.", color = Color.Gray, fontSize = 14.sp, fontFamily = Fredoka)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredTasks, key = { it.id }) { task ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        viewModel.toggleTaskCompletion(task)
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        viewModel.deleteTask(task)
                                        onShowUndoSnackbar(task)
                                        true
                                    }
                                    else -> false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.StartToEnd -> NudgeBlue
                                        SwipeToDismissBoxValue.EndToStart -> NudgeRed
                                        else -> Color.Transparent
                                    },
                                    label = "SwipeBackground"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) {
                                    if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                        Icon(Icons.Default.Check, contentDescription = "Complete", tint = Color.White)
                                    } else if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                                    }
                                }
                            },
                            content = {
                                TaskListItem(
                                    task = task,
                                    onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                    onDelete = {
                                        viewModel.deleteTask(task)
                                        onShowUndoSnackbar(task)
                                    },
                                    onStartFocus = { onStartFocus(task) },
                                    onEditTask = { onEditTask(task) }
                                )
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddTaskClick,
            containerColor = NudgeBlue,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }
}

// --- CALENDAR SCREEN ---
@Composable
fun CalendarScreen(taskViewModel: TaskViewModel, navController: NavController) {
    val taskList by taskViewModel.tasks.collectAsStateWithLifecycle()
    var selectedDay by remember { mutableStateOf(18) }

    val daysInMonth = (1..31).toList()

    Column(modifier = Modifier.fillMaxSize().background(NudgeBackground).padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.clickable { navController.popBackStack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("October 2026", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NudgeDarkBrown, fontFamily = Fredoka)
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(260.dp)
        ) {
            items(daysInMonth) { day ->
                val isSelected = day == selectedDay
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (isSelected) NudgeBlue else Color.White, CircleShape)
                        .clickable { selectedDay = day },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$day",
                        color = if (isSelected) Color.White else NudgeDarkBrown,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = Fredoka
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionTitle("Tasks for Oct $selectedDay", Icons.Outlined.CheckCircle)
        Spacer(modifier = Modifier.height(12.dp))

        val dayTasks = taskList.filter { !it.isCompleted }
        if (dayTasks.isEmpty()) {
            Text("No tasks scheduled for this day.", color = Color.Gray, fontSize = 14.sp, fontFamily = Fredoka)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayTasks) { task ->
                    DeadlineCard(
                        title = task.title,
                        tag = task.category,
                        time = task.deadline,
                        tagBg = NudgeLightBlue,
                        tagText = NudgeBlue
                    )
                }
            }
        }
    }
}

// --- FOCUS MODE (POMODORO) DIALOG ---
@Composable
fun FocusModeDialog(
    task: Task,
    onDismiss: () -> Unit,
    onCompleteSession: (Long) -> Unit
) {
    var timeLeftSeconds by remember { mutableStateOf(1500) }
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning, timeLeftSeconds) {
        if (isRunning && timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds--
        } else if (timeLeftSeconds == 0) {
            onCompleteSession(25)
        }
    }

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Focus Session", fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = Fredoka)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(task.title, fontWeight = FontWeight.Bold, color = NudgeDarkBrown, fontSize = 16.sp, fontFamily = Fredoka)
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(NudgeLightBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(timeFormatted, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = NudgeBlue, fontFamily = Fredoka)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { isRunning = !isRunning },
                        colors = ButtonDefaults.buttonColors(containerColor = NudgeBlue)
                    ) {
                        Text(if (isRunning) "Pause" else "Resume", fontFamily = Fredoka)
                    }
                    OutlinedButton(
                        onClick = { timeLeftSeconds = 1500; isRunning = false }
                    ) {
                        Text("Reset", fontFamily = Fredoka)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCompleteSession(25) },
                colors = ButtonDefaults.buttonColors(containerColor = NudgeBlue)
            ) {
                Text("Finish Session", color = Color.White, fontFamily = Fredoka)
            }
        }
    )
}

// --- WEATHER SCREEN ---
@Composable
fun WeatherScreen(
    userCity: String,
    viewModel: WeatherViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userCity) {
        viewModel.fetchWeatherForCity(userCity)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(NudgeBackground)) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NudgeBlue, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(uiState.city, color = Color.White, fontSize = 14.sp, fontFamily = Fredoka)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Icon(Icons.Outlined.WbSunny, contentDescription = "Sun", tint = Color.White, modifier = Modifier.size(64.dp))
                    Text(uiState.temp, color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
                    Text("${uiState.condition} • Feels like ${uiState.feelsLike}", color = Color.White, fontFamily = Fredoka)

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        WeatherStat(Icons.Default.WaterDrop, uiState.humidity, "Humidity")
                        WeatherStat(Icons.Default.Air, uiState.wind, "Wind")
                        WeatherStat(Icons.Default.Umbrella, "70%", "Rain")
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(24.dp)) {
                if (uiState.error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NudgeRedLight, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = NudgeRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Weather Error", color = NudgeRed, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
                            Text(uiState.error!!, color = NudgeRed, fontSize = 12.sp, fontFamily = Fredoka)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NudgeRedLight, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = NudgeRed)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Thunderstorm advisory", color = NudgeRed, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
                        Text("Heavy rain expected 3–6 PM. Bring an umbrella before heading to class!", color = NudgeRed, fontSize = 12.sp, fontFamily = Fredoka)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle("Hourly", Icons.Outlined.AccessTime)
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { HourlyCard("Now", Icons.Outlined.WbSunny, uiState.temp, true) }
                    item { HourlyCard("1 PM", Icons.Outlined.WbSunny, "32°", false) }
                    item { HourlyCard("3 PM", Icons.Outlined.Cloud, "28°", false) }
                    item { HourlyCard("5 PM", Icons.Outlined.Thunderstorm, "27°", false) }
                    item { HourlyCard("7 PM", Icons.Outlined.Cloud, "27°", false) }
                }

                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle("This week", Icons.Outlined.DateRange)
                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    WeeklyRow("Mon", Icons.Outlined.Thunderstorm, "30° 25°")
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    WeeklyRow("Tue", Icons.Outlined.WbSunny, "32° 26°")
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    WeeklyRow("Wed", Icons.Outlined.WbSunny, "33° 26°")
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    WeeklyRow("Thu", Icons.Outlined.Thunderstorm, "29° 25°")
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    WeeklyRow("Fri", Icons.Outlined.WbSunny, "31° 25°")
                }
            }
        }
    }
}

// --- STUDY AI SCREEN ---
@Composable
fun StudyAIScreen(viewModel: AIViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxSize().background(NudgeBackground).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(NudgeBlue, CircleShape), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.nudge_logo_n),
                    contentDescription = "Nudge Cover Mark",
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Study Buddy", fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(NudgeGreen, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isLoading) "Typing..." else "Online", color = NudgeGreen, fontSize = 12.sp, fontFamily = Fredoka)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatHistory) { message -> ChatBubble(message) }
            if (isLoading) {
                item {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NudgeBlue, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nudge is thinking...", color = Color.Gray, fontSize = 12.sp, fontFamily = Fredoka)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (chatHistory.size == 1) {
            SuggestionChip("Explain Big-O notation") { viewModel.sendMessage("Explain Big-O notation") }
            SuggestionChip("What is MVVM architecture?") { viewModel.sendMessage("What is MVVM architecture?") }
            SuggestionChip("Help me solve a quadratic equation") { viewModel.sendMessage("Help me solve a quadratic equation") }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = NudgeDarkBrown, fontFamily = Fredoka),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                        keyboardController?.hide()
                    }
                }),
                decorationBox = { innerTextField ->
                    Box {
                        if (inputText.isEmpty()) {
                            Text("Ask a study question...", color = Color.Gray, fontFamily = Fredoka)
                        }
                        innerTextField()
                    }
                }
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (inputText.isNotBlank()) NudgeBlue else Color.LightGray, CircleShape)
                    .clickable(enabled = inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                        keyboardController?.hide()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val bgColor = if (message.isUser) NudgeBlue else Color.White
    val textColor = if (message.isUser) Color.White else NudgeDarkBrown
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(bgColor, shape)
                .padding(16.dp)
        ) {
            if (message.isUser) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontFamily = Fredoka
                )
            } else {
                Markdown(
                    content = message.text,
                    colors = markdownColor(
                        text = textColor
                    )
                )
            }
        }
    }
}

// --- ADD / EDIT TASK DIALOG ---
@Composable
fun AddTaskDialog(
    taskToEdit: Task? = null,
    onDismiss: () -> Unit,
    onSaveTask: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var subject by remember { mutableStateOf(taskToEdit?.subject ?: "") }
    var selectedCategory by remember { mutableStateOf(taskToEdit?.category ?: "Assignment") }
    var selectedPriority by remember { mutableStateOf(taskToEdit?.priority ?: "Medium") }
    var deadline by remember { mutableStateOf(taskToEdit?.deadline ?: "") }
    var selectedReminder by remember { mutableStateOf("1 hour before") }

    val categories = listOf("Assignment", "Project", "Exam", "Personal")
    val priorities = listOf("High", "Medium", "Low")
    val reminderOptions = listOf("15 mins before", "1 hour before", "1 day before")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (taskToEdit == null) "New task" else "Edit task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    fontFamily = Fredoka
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What do you need to do?", fontFamily = Fredoka) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NudgeBackground,
                        focusedContainerColor = NudgeBackground,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = NudgeBlue
                    )
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = { Text("Subject", fontFamily = Fredoka) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NudgeBackground,
                        focusedContainerColor = NudgeBackground,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = NudgeBlue
                    )
                )

                Text("Category", fontSize = 12.sp, color = Color.Gray, fontFamily = Fredoka)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selectedCategory == category) NudgeBlue else NudgeBackground,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                category,
                                color = if (selectedCategory == category) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = Fredoka
                            )
                        }
                    }
                }

                Text("Priority", fontSize = 12.sp, color = Color.Gray, fontFamily = Fredoka)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorities.forEach { priority ->
                        val pColor = when (priority) {
                            "High" -> NudgeRed
                            "Medium" -> NudgeYellow
                            else -> NudgeGreen
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selectedPriority == priority) pColor else NudgeBackground,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedPriority = priority }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                priority,
                                color = if (selectedPriority == priority) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = Fredoka
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    placeholder = { Text("mm/dd/yyyy --:-- --", fontFamily = Fredoka) },
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Date") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NudgeBackground,
                        focusedContainerColor = NudgeBackground,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = NudgeBlue
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NudgeLightBlue, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = "Reminder", tint = NudgeDarkBrown, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remind me", color = NudgeDarkBrown, fontSize = 14.sp, fontFamily = Fredoka)
                    }
                    Text(selectedReminder, color = NudgeDarkBrown, fontSize = 14.sp, fontFamily = Fredoka)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSaveTask(
                            title,
                            subject,
                            selectedCategory,
                            deadline.ifBlank { "No deadline" },
                            selectedPriority
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NudgeBlue),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(if (taskToEdit == null) "Add task" else "Save changes", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
            }
        }
    )
}

// --- ADD CLASS DIALOG ---
@Composable
fun AddClassDialog(
    onDismiss: () -> Unit,
    onSaveClass: (String, String, String, String) -> Unit
) {
    var subjectName by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New class", fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = Fredoka)
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    placeholder = { Text("Subject Name (e.g. Data Structures)", fontFamily = Fredoka) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NudgeBackground,
                        focusedContainerColor = NudgeBackground,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = NudgeBlue
                    )
                )
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    placeholder = { Text("Room (e.g. Rm 402)", fontFamily = Fredoka) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NudgeBackground,
                        focusedContainerColor = NudgeBackground,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = NudgeBlue
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        placeholder = { Text("Start (8:00 AM)", fontFamily = Fredoka) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = NudgeBackground,
                            focusedContainerColor = NudgeBackground,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = NudgeBlue
                        )
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        placeholder = { Text("End (9:30 AM)", fontFamily = Fredoka) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = NudgeBackground,
                            focusedContainerColor = NudgeBackground,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = NudgeBlue
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subjectName.isNotBlank() && room.isNotBlank()) {
                        onSaveClass(
                            subjectName,
                            room,
                            startTime.ifBlank { "8:00 AM" },
                            endTime.ifBlank { "9:30 AM" }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NudgeBlue),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Add class", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
            }
        }
    )
}

// --- HELPER COMPOSABLES ---

@Composable
fun NudgeLogo() {
    Image(
        painter = painterResource(id = R.drawable.nudge_logo_n),
        contentDescription = "Nudge Logo",
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
    )
}

@Composable
fun StatCard(number: String, label: String) {
    Card(
        modifier = Modifier.width(100.dp).height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(number, color = NudgeBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
            Text(label, color = Color.Gray, fontSize = 12.sp, fontFamily = Fredoka)
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = NudgeBlue, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NudgeDarkBrown, fontFamily = Fredoka)
    }
}

@Composable
fun ClassCard(time: String, title: String, room: String) {
    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(time, color = NudgeBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NudgeDarkBrown, fontFamily = Fredoka)
            Text(room, color = Color.Gray, fontSize = 12.sp, fontFamily = Fredoka)
        }
    }
}

@Composable
fun DeadlineCard(title: String, tag: String, time: String, tagBg: Color, tagText: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(24.dp).border(2.dp, NudgeBlue, CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NudgeDarkBrown, fontFamily = Fredoka)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.background(tagBg, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(tag, color = tagText, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(time, fontSize = 12.sp, color = Color.Gray, fontFamily = Fredoka)
                }
            }
        }
    }
}

@Composable
fun TaskListItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onStartFocus: () -> Unit,
    onEditTask: () -> Unit
) {
    val isCompleted = task.isCompleted
    val priorityColor = when (task.priority) {
        "High" -> NudgeRed
        "Low" -> NudgeGreen
        else -> NudgeYellow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditTask() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint = NudgeGreen,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().border(2.dp, NudgeBlue, CircleShape))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isCompleted) Color.Gray else NudgeDarkBrown,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        fontFamily = Fredoka,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(task.priority, color = priorityColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.category,
                        color = if (isCompleted) NudgeGreen else NudgeBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Fredoka
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        task.deadline,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontFamily = Fredoka
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pomodoro Focus Button
            Icon(
                Icons.Outlined.PlayCircle,
                contentDescription = "Focus Mode",
                tint = NudgeBlue,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onStartFocus() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete",
                tint = Color.LightGray,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onDelete() }
            )
        }
    }
}

@Composable
fun WeatherStat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Fredoka)
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = Fredoka)
    }
}

@Composable
fun HourlyCard(time: String, icon: ImageVector, temp: String, isSelected: Boolean) {
    Column(
        modifier = Modifier
            .width(70.dp)
            .background(if (isSelected) NudgeBlue else Color.White, RoundedCornerShape(24.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(time, color = if (isSelected) Color.White else Color.Gray, fontSize = 12.sp, fontFamily = Fredoka)
        Spacer(modifier = Modifier.height(8.dp))
        Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else NudgeBlue)
        Spacer(modifier = Modifier.height(8.dp))
        Text(temp, color = if (isSelected) Color.White else NudgeDarkBrown, fontWeight = FontWeight.Bold, fontFamily = Fredoka)
    }
}

@Composable
fun WeeklyRow(day: String, icon: ImageVector, temps: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(day, modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold, color = NudgeDarkBrown, fontFamily = Fredoka)
        Icon(icon, contentDescription = null, tint = NudgeBlue, modifier = Modifier.size(20.dp))
        Text(temps, color = NudgeDarkBrown, fontWeight = FontWeight.Medium, fontFamily = Fredoka)
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(NudgeLightBlue.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = NudgeBlue, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = NudgeDarkBrown, fontSize = 14.sp, fontFamily = Fredoka)
    }
}
