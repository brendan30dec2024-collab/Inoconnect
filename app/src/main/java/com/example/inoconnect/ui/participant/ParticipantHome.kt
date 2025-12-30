package com.example.inoconnect.ui.participant

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inoconnect.R
import com.example.inoconnect.data.Event
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.Project
import com.example.inoconnect.data.User
import com.example.inoconnect.ui.auth.BrandBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// --- GLOBAL FLAG FOR ONE-TIME WELCOME ---
private var sessionWelcomeShown = false

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ParticipantHome(
    onEventClick: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    onCreateProjectClick: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Data State ---
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var userProfile by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // --- Fetch Data ---
    LaunchedEffect(Unit) {
        val uid = repository.currentUserId
        if (uid != null) {
            userProfile = repository.getUserById(uid)
        }
        val fetchedEvents = repository.getAllEvents()
        val fetchedProjects = repository.getAllProjects()
        events = fetchedEvents
        projects = fetchedProjects
        isLoading = false
    }

    // --- Welcome Logic (Once Per Session) ---
    LaunchedEffect(userProfile) {
        if (!sessionWelcomeShown && userProfile != null) {
            delay(500)
            snackbarHostState.showSnackbar(
                message = "Welcome back, ${userProfile!!.username}",
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
            sessionWelcomeShown = true
        }
    }

    // --- Pager & Tab State ---
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val selectedTabIndex = pagerState.currentPage

    // --- Scroll / Collapsing Header State ---
    val density = LocalDensity.current

    // 1. Calculate Heights
    val stickyTabsHeightDp = 120.dp
    // Collapsible Content
    val fullHeaderHeightDp = 400.dp

    val fullHeaderHeightPx = with(density) { fullHeaderHeightDp.toPx() }
    val stickyTabsHeightPx = with(density) { stickyTabsHeightDp.toPx() }

    // Max Collapse
    val maxCollapsePx = (fullHeaderHeightPx - stickyTabsHeightPx) * -1f

    var headerOffsetHeightPx by remember { mutableFloatStateOf(0f) }

    // Reset Header on Tab Switch
    LaunchedEffect(pagerState.currentPage) {
        headerOffsetHeightPx = 0f
    }

    // Synchronized Scrolling
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Collapse (Scrolling Up)
                if (consumed.y < 0) {
                    val newOffset = headerOffsetHeightPx + consumed.y
                    headerOffsetHeightPx = newOffset.coerceIn(maxCollapsePx, 0f)
                }
                // Expand (Scrolling Down)
                if (available.y > 0) {
                    val newOffset = headerOffsetHeightPx + available.y
                    headerOffsetHeightPx = newOffset.coerceIn(maxCollapsePx, 0f)
                }
                return Offset.Zero
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSortedAsc by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = BrandBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = onCreateProjectClick,
                    icon = { Icon(Icons.Default.Add, "Create") },
                    text = { Text("New Project") },
                    containerColor = BrandBlue,
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // 1. Swipeable Content (The Lists)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                // Push content down by the full header height
                val contentPadding = PaddingValues(top = fullHeaderHeightDp + 16.dp, bottom = 80.dp)

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                } else {
                    if (page == 0) {
                        ProjectList(projects, searchQuery, isSortedAsc, onProjectClick, contentPadding)
                    } else {
                        EventList(events, searchQuery, isSortedAsc, onEventClick, contentPadding)
                    }
                }
            }

            // 2. The Collapsing Header Overlay
            Box(
                modifier = Modifier
                    .height(fullHeaderHeightDp)
                    .fillMaxWidth()
                    .offset { IntOffset(x = 0, y = headerOffsetHeightPx.roundToInt()) }
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column {
                    // --- PART A: Fading Banner (Collapsible) ---
                    Column(
                        modifier = Modifier
                            .height(fullHeaderHeightDp - stickyTabsHeightDp)
                            .fillMaxWidth()
                            .alpha(1f - (-headerOffsetHeightPx / -maxCollapsePx))
                    ) {
                        Text(
                            text = "InnoConnect",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            ),
                            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                        )

                        val localBanners = listOf(R.mipmap.banner1, R.mipmap.banner2)
                        HeroCarousel(bannerImages = localBanners)
                    }

                    // --- PART B: Sticky Tabs & Search (Pinned) ---
                    Column(
                        modifier = Modifier
                            .height(stickyTabsHeightDp)
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        // Tabs
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Color.White,
                            contentColor = BrandBlue,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = BrandBlue
                                )
                            }
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                                text = { Text("Student Projects", fontWeight = FontWeight.SemiBold) }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                                text = { Text("Ongoing Events", fontWeight = FontWeight.SemiBold) }
                            )
                        }

                        // Search Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Search...") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFF5F5F5),
                                    unfocusedContainerColor = Color(0xFFF5F5F5),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { isSortedAsc = !isSortedAsc }) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Sort",
                                    tint = if (isSortedAsc) BrandBlue else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(bannerImages: List<Int>) {
    val pagerState = rememberPagerState(pageCount = { bannerImages.size })
    LaunchedEffect(pagerState) {
        while (true) {
            delay(3000)
            val nextPage = (pagerState.currentPage + 1) % bannerImages.size
            pagerState.animateScrollToPage(nextPage)
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 8.dp
        ) { page ->
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = bannerImages[page],
                        contentDescription = "Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                                    startY = 100f
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun EventList(
    events: List<Event>,
    searchQuery: String,
    isSortedAsc: Boolean,
    onEventClick: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val filteredEvents = events.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }.let { list -> if (isSortedAsc) list.sortedBy { it.title } else list }

    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        items(filteredEvents) { event ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { onEventClick(event.eventId) },
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (event.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = event.imageUrl, contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Column {
                        Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(event.eventDate, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Location: ${event.location}", style = MaterialTheme.typography.bodySmall, color = BrandBlue)
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectList(
    projects: List<Project>,
    searchQuery: String,
    isSortedAsc: Boolean,
    onProjectClick: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val filteredProjects = projects.filter { project ->
        (project.title.contains(searchQuery, ignoreCase = true) ||
                project.tags.any { it.contains(searchQuery, ignoreCase = true) })
    }.let { list -> if (isSortedAsc) list.sortedBy { it.title } else list }

    if (filteredProjects.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            Text("No available projects found.", color = Color.Gray)
        }
    } else {
        LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            items(filteredProjects) { project ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onProjectClick(project.projectId) },
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            if (project.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = project.imageUrl, contentDescription = null,
                                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${project.memberIds.size}/${project.targetTeamSize} Members", fontSize = 12.sp, color = Color.Gray)
                                Text("Ends: ${project.recruitmentDeadline}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        if (project.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(project.tags) { role ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(role, fontSize = 10.sp) },
                                        modifier = Modifier.height(26.dp),
                                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = BrandBlue.copy(alpha = 0.05f)),
                                        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = BrandBlue.copy(alpha = 0.2f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}