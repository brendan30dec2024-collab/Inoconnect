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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inoconnect.R
import com.example.inoconnect.data.Event
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.Project
import com.example.inoconnect.ui.auth.BrandBlue
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ParticipantHome(
    onEventClick: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    onCreateProjectClick: () -> Unit
) {
    val repository = remember { FirebaseRepository() }

    // --- 1. SNACKBAR STATE ---
    val snackbarHostState = remember { SnackbarHostState() }

    // Data State
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch Data
    LaunchedEffect(Unit) {
        val fetchedEvents = repository.getAllEvents()
        val fetchedProjects = repository.getAllProjects()
        events = fetchedEvents
        projects = fetchedProjects
        isLoading = false
    }

    // --- 2. WELCOME TOAST/SNACKBAR TRIGGER ---
    LaunchedEffect(Unit) {
        // Small delay so it pops up after the screen transition
        delay(300)
        snackbarHostState.showSnackbar(
            message = "Welcome back, InnoUser! 👋",
            duration = SnackbarDuration.Short,
            withDismissAction = true
        )
    }

    // UI State
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSortedAsc by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        // --- 3. ADD SNACKBAR HOST TO SCAFFOLD ---
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                // Optional: Custom styling for the snackbar to match BrandBlue
                Snackbar(
                    snackbarData = data,
                    containerColor = BrandBlue,
                    contentColor = Color.White,
                    actionColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // --- HERO SECTION (Static Banners) ---
            val localBanners = listOf(
                R.mipmap.banner1,
                R.mipmap.banner2
            )

            HeroCarousel(bannerImages = localBanners)

            Spacer(modifier = Modifier.height(16.dp))

            // --- TABS ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = BrandBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BrandBlue
                    )
                }
            ) {
                // Tab 0: Student Projects (Left)
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Student Projects", fontWeight = FontWeight.SemiBold) }
                )
                // Tab 1: Ongoing Events (Right)
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Ongoing Events", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- SEARCH ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search title...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
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

            // --- LIST CONTENT ---
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    if (selectedTab == 0) {
                        // Tab 0 is now Projects
                        ProjectList(projects, searchQuery, isSortedAsc, onProjectClick)
                    } else {
                        // Tab 1 is now Events
                        EventList(events, searchQuery, isSortedAsc, onEventClick)
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

    // Auto-play Logic
    LaunchedEffect(pagerState) {
        while (true) {
            delay(3000)
            val nextPage = (pagerState.currentPage + 1) % bannerImages.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 8.dp
        ) { page ->
            val resId = bannerImages[page]

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = resId,
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

        // Dots Indicator
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) BrandBlue else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
fun EventList(
    events: List<Event>,
    searchQuery: String,
    isSortedAsc: Boolean,
    onEventClick: (String) -> Unit
) {
    val filteredEvents = events.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }.let { list ->
        if (isSortedAsc) list.sortedBy { it.title } else list
    }

    LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)) {
        items(filteredEvents) { event ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { onEventClick(event.eventId) },
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (event.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = event.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp)),
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
    onProjectClick: (String) -> Unit
) {
    // --- UPDATED FILTER LOGIC ---
    val filteredProjects = projects.filter { project ->
        val matchesSearch = project.title.contains(searchQuery, ignoreCase = true) ||
                project.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }

        // HIDE projects that are full
        val isNotFull = project.memberIds.size < project.targetTeamSize

        matchesSearch && isNotFull
    }.let { list ->
        if (isSortedAsc) list.sortedBy { it.title } else list
    }

    if (filteredProjects.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
            Text("No available projects found.", color = Color.Gray)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)) {
            items(filteredProjects) { project ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { onProjectClick(project.projectId) },
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            // 1. Thumbnail
                            if (project.imageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = project.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            // 2. Main Info (Title, Team Size, Deadline)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(6.dp))

                                // Team Size: "2/5 Team Members"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${project.memberIds.size}/${project.targetTeamSize} Team Members",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Deadline
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Deadline: ${project.recruitmentDeadline}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. "Looking For" Tags
                        if (project.tags.isNotEmpty()) {
                            Text("Looking for:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(project.tags) { role ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(role, fontSize = 10.sp) },
                                        modifier = Modifier.height(26.dp),
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = BrandBlue.copy(alpha = 0.05f)
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(

                                            enabled = true,
                                            borderColor = BrandBlue.copy(alpha = 0.2f)
                                        )
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