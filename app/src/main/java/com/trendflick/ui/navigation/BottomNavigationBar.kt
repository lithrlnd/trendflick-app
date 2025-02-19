package com.trendflick.ui.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.trendflick.ui.viewmodels.SharedViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import android.view.MotionEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import kotlin.math.abs
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity

data class NavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showBadge: Boolean = false,
    val isLocked: Boolean = false, // For Home item that can't be removed
    val isDraggable: Boolean = true,
    val id: String // Add unique ID field
)

data class CustomCategory(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val type: CategoryType,
    val description: String,
    val onClick: () -> Unit,
    val allowedEngagements: Set<EngagementType> = type.engagementTypes,
    val allowedPostTypes: Set<PostType> = type.postTypes,
    val requiresAuth: Boolean = true,
    val showTrending: Boolean = false,
    val showHashtags: Boolean = false
)

enum class CategoryType {
    APP_VIEW {
        override val engagementTypes = setOf(
            EngagementType.LIKE,
            EngagementType.COMMENT,
            EngagementType.REPOST,
            EngagementType.SHARE
        )
        override val postTypes = setOf(
            PostType.TEXT,
            PostType.IMAGE,
            PostType.VIDEO
        )
    },
    FEED_GENERATOR {
        override val engagementTypes = setOf(
            EngagementType.LIKE,
            EngagementType.COMMENT,
            EngagementType.REPOST,
            EngagementType.SHARE,
            EngagementType.SAVE
        )
        override val postTypes = setOf(
            PostType.TEXT,
            PostType.IMAGE,
            PostType.VIDEO,
            PostType.RICH_MEDIA
        )
    },
    CUSTOM_FEED {
        override val engagementTypes = setOf(
            EngagementType.LIKE,
            EngagementType.COMMENT,
            EngagementType.REPOST,
            EngagementType.SHARE,
            EngagementType.SAVE
        )
        override val postTypes = setOf(
            PostType.TEXT,
            PostType.IMAGE,
            PostType.VIDEO
        )
    },
    AGGREGATOR {
        override val engagementTypes = setOf(
            EngagementType.LIKE,
            EngagementType.COMMENT,
            EngagementType.REPOST,
            EngagementType.SHARE
        )
        override val postTypes = setOf(
            PostType.TEXT,
            PostType.IMAGE,
            PostType.VIDEO,
            PostType.THREAD
        )
    };

    abstract val engagementTypes: Set<EngagementType>
    abstract val postTypes: Set<PostType>
}

enum class EngagementType {
    LIKE,
    COMMENT,
    REPOST,
    SHARE,
    SAVE
}

enum class PostType {
    TEXT,
    IMAGE,
    VIDEO,
    RICH_MEDIA,
    THREAD
}

private val defaultNavItems = listOf(
    NavItem(
        screen = Screen.Home,
        label = "What's Hot",
        selectedIcon = Icons.Default.TrendingUp,
        unselectedIcon = Icons.Outlined.TrendingUp,
        isLocked = true,
        id = "home"
    ),
    NavItem(
        screen = Screen.WhatsHot,
        label = "Following",
        selectedIcon = Icons.Default.People,
        unselectedIcon = Icons.Outlined.People,
        id = "whats_hot"
    ),
    NavItem(
        screen = Screen.Search,
        label = "Search",
        selectedIcon = Icons.Default.Search,
        unselectedIcon = Icons.Outlined.Search,
        id = "search"
    ),
    NavItem(
        screen = Screen.AI,
        label = "AI",
        selectedIcon = Icons.Default.SmartToy,
        unselectedIcon = Icons.Outlined.SmartToy,
        id = "ai"
    ),
    NavItem(
        screen = Screen.Profile,
        label = "Profile",
        selectedIcon = Icons.Default.Person,
        unselectedIcon = Icons.Outlined.Person,
        id = "profile"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    sharedViewModel: SharedViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedFeed by sharedViewModel.selectedFeed.collectAsState()
    val isBottomSheetVisible by sharedViewModel.isBottomSheetVisible.collectAsState()
    val scope = rememberCoroutineScope()

    // Track initial navigation items separately with unique IDs
    val initialNavItems = remember {
        listOf(
            NavItem(Screen.Home, "What's Hot", Icons.Default.TrendingUp, Icons.Outlined.TrendingUp, isLocked = true, id = "home"),
            NavItem(Screen.WhatsHot, "Following", Icons.Default.People, Icons.Outlined.People, id = "whats_hot"),
            NavItem(Screen.Search, "Search", Icons.Default.Search, Icons.Outlined.Search, id = "search"),
            NavItem(Screen.AI, "AI", Icons.Default.SmartToy, Icons.Outlined.SmartToy, id = "ai"),
            NavItem(Screen.Profile, "Profile", Icons.Default.Person, Icons.Outlined.Person, id = "profile")
        )
    }

    // State for dragging and editing - now considers bottom sheet visibility
    var isEditMode by remember { mutableStateOf(false) }
    var draggedItem by remember { mutableStateOf<CustomCategory?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    var draggedItemStartPosition by remember { mutableStateOf<Offset?>(null) }
    var isDraggingToNav by remember { mutableStateOf(false) }

    // Automatically disable edit mode when bottom sheet becomes visible
    LaunchedEffect(isBottomSheetVisible) {
        if (isBottomSheetVisible) {
            isEditMode = false
        }
    }
    
    // State for current navigation items
    var currentNavItems by remember { mutableStateOf(initialNavItems) }
    
    // Track removed initial items with their unique IDs
    var removedInitialItems by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Update the screen bounds state to use Rect instead of IntRect
    var screenBounds by remember { mutableStateOf<Rect?>(null) }
    var navigationBounds by remember { mutableStateOf<Rect?>(null) }

    // Add tap to dismiss edit mode
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            // Listen for taps outside navigation items
            val listener = { event: MotionEvent ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    isEditMode = false
                    true
                } else {
                    false
                }
            }
            // Add and remove listener as needed
        }
    }

    // Categories grouped by type
    val groupedCategories = remember {
        listOf(
            "Feed Types" to listOf(
                CustomCategory(
                    id = "fyp",
                    icon = Icons.Default.Recommend,
                    label = "For You",
                    type = CategoryType.APP_VIEW,
                    description = "Personalized feed based on your interests",
                    onClick = { /* Handle click */ },
                    showTrending = true
                ),
                CustomCategory(
                    id = "trending",
                    icon = Icons.Default.TrendingUp,
                    label = "What's Hot",
                    type = CategoryType.APP_VIEW,
                    description = "Popular content across the network",
                    onClick = { /* Handle click */ },
                    showTrending = true,
                    showHashtags = true
                ),
                CustomCategory(
                    id = "following",
                    icon = Icons.Default.People,
                    label = "Following",
                    type = CategoryType.APP_VIEW,
                    description = "Posts from people you follow",
                    onClick = { /* Handle click */ }
                )
            ),
            "Communication" to listOf(
                CustomCategory(
                    id = "messages",
                    icon = Icons.Default.Message,
                    label = "Messages",
                    type = CategoryType.APP_VIEW,
                    description = "Direct messages and conversations",
                    onClick = { /* Handle click */ }
                )
            ),
            "Discovery" to listOf(
                CustomCategory(
                    id = "explore",
                    icon = Icons.Default.Explore,
                    label = "Explore",
                    type = CategoryType.FEED_GENERATOR,
                    description = "Discover new content and creators",
                    onClick = { /* Handle click */ },
                    showTrending = true,
                    showHashtags = true,
                    allowedEngagements = CategoryType.FEED_GENERATOR.engagementTypes + EngagementType.SAVE
                ),
                CustomCategory(
                    id = "media",
                    icon = Icons.Default.VideoLibrary,
                    label = "Media",
                    type = CategoryType.APP_VIEW,
                    description = "Photos and videos",
                    onClick = { /* Handle click */ },
                    allowedPostTypes = setOf(PostType.IMAGE, PostType.VIDEO)
                ),
                CustomCategory(
                    id = "hashtags",
                    icon = Icons.Default.Tag,
                    label = "Hashtags",
                    type = CategoryType.AGGREGATOR,
                    description = "Trending topics and discussions",
                    onClick = { /* Handle click */ },
                    showHashtags = true,
                    showTrending = true
                )
            ),
            "Content" to listOf(
                CustomCategory(
                    id = "search",
                    icon = Icons.Default.Search,
                    label = "Search",
                    type = CategoryType.APP_VIEW,
                    description = "Search for content and users",
                    onClick = { /* Handle click */ },
                    requiresAuth = false
                )
            ),
            "Personal" to listOf(
                CustomCategory(
                    id = "bookmarks",
                    icon = Icons.Default.Bookmarks,
                    label = "Saved",
                    type = CategoryType.CUSTOM_FEED,
                    description = "Your saved content",
                    onClick = { /* Handle click */ }
                ),
                CustomCategory(
                    id = "lists",
                    icon = Icons.Default.List,
                    label = "Lists",
                    type = CategoryType.APP_VIEW,
                    description = "Your custom lists",
                    onClick = { /* Handle click */ }
                ),
                CustomCategory(
                    id = "drafts",
                    icon = Icons.Default.Edit,
                    label = "Drafts",
                    type = CategoryType.CUSTOM_FEED,
                    description = "Your draft posts",
                    onClick = { /* Handle click */ }
                )
            ),
            "Network" to listOf(
                CustomCategory(
                    id = "mutuals",
                    icon = Icons.Default.GroupAdd,
                    label = "Mutuals",
                    type = CategoryType.AGGREGATOR,
                    description = "People who follow you back",
                    onClick = { /* Handle click */ }
                ),
                CustomCategory(
                    id = "mentions",
                    icon = Icons.Default.AlternateEmail,
                    label = "Mentions",
                    type = CategoryType.APP_VIEW,
                    description = "Posts you're mentioned in",
                    onClick = { /* Handle click */ }
                ),
                CustomCategory(
                    id = "reposts",
                    icon = Icons.Default.Repeat,
                    label = "Reposts",
                    type = CategoryType.APP_VIEW,
                    description = "Your reposts",
                    onClick = { /* Handle click */ }
                )
            )
        )
    }

    // Track available categories separately from grouped categories
    var availableCategories by remember { 
        mutableStateOf(groupedCategories.flatMap { it.second }) 
    }

    // Function to handle item removal
    fun handleItemRemoval(item: NavItem) {
        if (!item.isLocked) {
            // If it's an initial item, add its ID to removedInitialItems
            if (initialNavItems.any { it.id == item.id }) {
                removedInitialItems = removedInitialItems + item.id
            } else {
                // If it's a custom category, add back to available categories
                val category = availableCategories.find { it.id == (item.screen as? Screen.Custom)?.id }
                if (category != null) {
                    availableCategories = (availableCategories + category).toList()
                }
            }
            currentNavItems = currentNavItems.filter { it.id != item.id }
        }
    }

    // Function to show all available items
    fun getAllAvailableItems(groupTitle: String): List<CustomCategory> {
        val customCategories = groupedCategories
            .find { it.first == groupTitle }
            ?.second
            ?.filter { category ->
                currentNavItems.none { it.screen is Screen.Custom && (it.screen as Screen.Custom).id == category.id }
            } ?: emptyList()

        // For the "Navigation" group, add removed initial items
        return if (groupTitle == "Navigation") {
            customCategories + removedInitialItems.mapNotNull { id ->
                initialNavItems.find { it.id == id }?.let { navItem ->
                    CustomCategory(
                        id = navItem.id, // Use the original ID
                        icon = navItem.selectedIcon,
                        label = navItem.label,
                        type = CategoryType.APP_VIEW,
                        description = "Navigation item",
                        onClick = {}
                    )
                }
            }
        } else {
            customCategories
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    Box(modifier = modifier.fillMaxWidth()) {
        // Bottom sheet content - now with higher z-index when visible
        if (isBottomSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { 
                    scope.launch {
                        sharedViewModel.toggleBottomSheet(false)
                    }
                },
                sheetState = sheetState,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .zIndex(3f)  // Ensure bottom sheet is above everything
            ) {
                BottomSheetContent(
                    currentNavItems = currentNavItems,
                    availableCategories = availableCategories,
                    isEditMode = false, // Always false in bottom sheet
                    onItemClick = { handleItemRemoval(it) },
                    onItemLongPress = { /* Disabled in bottom sheet */ },
                    onItemRemove = { handleItemRemoval(it) },
                    onCategoryDragStart = { availableCategories = availableCategories - it },
                    onCategoryDragComplete = { _, _ -> },
                    onNavigationUpdate = { currentNavItems = it }
                )
            }
        }

        // Navigation bar - now considers bottom sheet visibility for edit mode
        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .onGloballyPositioned { coordinates ->
                    navigationBounds = coordinates.boundsInRoot()
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            if (!isBottomSheetVisible && dragAmount.y < -50f) {
                                change.consume()
                                scope.launch {
                                    sharedViewModel.toggleBottomSheet(true)
                                }
                            }
                        }
                    )
                }
                .zIndex(if (isBottomSheetVisible) 1f else 2f), // Lower z-index when sheet is visible
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 8.dp
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(currentNavItems) { item ->
                    NavigationItemWithWiggle(
                        item = item,
                        isSelected = currentRoute == item.screen.route,
                        isEditMode = isEditMode && !isBottomSheetVisible, // Disable edit mode when sheet is visible
                        onItemClick = {
                            if (item.screen == Screen.Search) {
                                scope.launch {
                                    sharedViewModel.toggleBottomSheet(true)
                                }
                            } else {
                                navController.navigate(item.screen.route) {
                                    if (item.screen == Screen.Home) {
                                        popUpTo(navController.graph.startDestinationId)
                                    }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onLongPress = { 
                            if (!isBottomSheetVisible && !item.isLocked) {
                                isEditMode = true
                            }
                        },
                        onRemove = { handleItemRemoval(item) }
                    )
                }
            }
        }

        // Dragged item overlay - now at the end of the Box to be on top
        draggedItem?.let { item ->
            dragPosition?.let { position ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(100f)
                ) {
                    DraggableCategoryChip(
                        category = item,
                        onDragStart = { draggedItem = item },
                        onDragComplete = { pos ->
                            // Check if dragged to navigation area
                            if (pos.y < -60 && navigationBounds != null) {
                                val navItem = NavItem(
                                    screen = Screen.Custom(item.id),
                                    label = item.label,
                                    selectedIcon = item.icon,
                                    unselectedIcon = item.icon,
                                    id = item.id
                                )
                                if (currentNavItems.size < 7) {
                                    currentNavItems = currentNavItems + navItem
                                    // Remove the dragged item from available categories
                                    availableCategories = availableCategories.filter { it.id != item.id }
                                }
                            }
                            draggedItem = null
                            dragPosition = null
                        },
                        modifier = Modifier
                            .offset { 
                                // Constrain position within screen bounds
                                val x = position.x.toInt().coerceIn(
                                    (screenBounds?.left ?: 0f).toInt(),
                                    (screenBounds?.right ?: 0f).toInt() - 100
                                )
                                val y = position.y.toInt().coerceIn(
                                    (screenBounds?.top ?: 0f).toInt() + 60,
                                    (navigationBounds?.top ?: 0f).toInt() - 60
                                )
                                IntOffset(x, y)
                            }
                            .alpha(0.7f)
                            .scale(1.1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomSheetContent(
    currentNavItems: List<NavItem>,
    availableCategories: List<CustomCategory>,
    isEditMode: Boolean,
    onItemClick: (NavItem) -> Unit,
    onItemLongPress: (NavItem) -> Unit,
    onItemRemove: (NavItem) -> Unit,
    onCategoryDragStart: (CustomCategory) -> Unit,
    onCategoryDragComplete: (CustomCategory, Offset) -> Unit,
    onNavigationUpdate: (List<NavItem>) -> Unit
) {
    var currentItems by remember { mutableStateOf(currentNavItems) }
    var draggedNavItem by remember { mutableStateOf<NavItem?>(null) }
    var draggedCategory by remember { mutableStateOf<CustomCategory?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    var dragStartPosition by remember { mutableStateOf<Offset?>(null) }
    var currentNavBounds by remember { mutableStateOf<Rect?>(null) }
    var availableItemsBounds by remember { mutableStateOf<Rect?>(null) }
    var isDraggingToNav by remember { mutableStateOf(false) }
    var isDraggingToAvailable by remember { mutableStateOf(false) }

    // Maintain a single source of truth for available items
    var availableItems by remember(availableCategories, currentItems) {
        mutableStateOf(
            (availableCategories + currentItems
                .filter { !it.isLocked }
                .map { navItem ->
                    CustomCategory(
                        id = navItem.id,
                        icon = navItem.selectedIcon,
                        label = navItem.label,
                        type = CategoryType.APP_VIEW,
                        description = "Navigation item",
                        onClick = {}
                    )
                })
                .distinctBy { it.id }
                .filter { category ->
                    currentItems.none { it.id == category.id }
                }
        )
    }

    LaunchedEffect(currentItems) {
        onNavigationUpdate(currentItems)
    }

    // Function to handle item movement between sections
    fun moveItemToNavigation(category: CustomCategory) {
        if (currentItems.size < 7 && !currentItems.any { it.id == category.id }) {
            val newNavItem = NavItem(
                screen = Screen.Custom(category.id),
                label = category.label,
                selectedIcon = category.icon,
                unselectedIcon = category.icon,
                id = category.id
            )
            currentItems = currentItems + newNavItem
            availableItems = availableItems.filter { it.id != category.id }
        }
    }

    fun moveItemToAvailable(item: NavItem) {
        if (!item.isLocked) {
            currentItems = currentItems - item
            val newCategory = CustomCategory(
                id = item.id,
                icon = item.selectedIcon,
                label = item.label,
                type = CategoryType.APP_VIEW,
                description = "Navigation item",
                onClick = {}
            )
            availableItems = (availableItems + newCategory).distinctBy { it.id }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
        ) {
            Text(
                text = "Current Navigation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp, top = 16.dp)
            )

            // Current Navigation items
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        currentNavBounds = coordinates.boundsInRoot()
                    }
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(currentItems, key = { it.id }) { item ->
                        DraggableItem(
                            item = item,
                            onDragStart = { offset, itemPosition ->
                                if (!item.isLocked) {
                                    draggedNavItem = item
                                    dragStartPosition = itemPosition
                                    dragPosition = itemPosition
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragPosition = dragPosition?.plus(dragAmount)
                                availableItemsBounds?.let { bounds ->
                                    dragPosition?.let { pos ->
                                        isDraggingToAvailable = pos.y > bounds.top
                                    }
                                }
                            },
                            onDragEnd = {
                                if (isDraggingToAvailable && !item.isLocked) {
                                    moveItemToAvailable(item)
                                }
                                draggedNavItem = null
                                dragPosition = null
                                dragStartPosition = null
                                isDraggingToAvailable = false
                            },
                            onClick = { clickedItem ->
                                (clickedItem as? NavItem)?.let { onItemClick(it) }
                            },
                            onLongPress = { pressedItem ->
                                (pressedItem as? NavItem)?.let { onItemLongPress(it) }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Available Items Section with reordering grid
            Text(
                text = "Available Items",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        availableItemsBounds = coordinates.boundsInRoot()
                    }
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableItems.size, key = { availableItems[it].id }) { index ->
                        val category = availableItems[index]
                        DraggableItem(
                            item = category,
                            onDragStart = { offset, itemPosition ->
                                draggedCategory = category
                                dragStartPosition = itemPosition
                                dragPosition = itemPosition
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragPosition = dragPosition?.plus(dragAmount)
                                currentNavBounds?.let { bounds ->
                                    dragPosition?.let { pos ->
                                        isDraggingToNav = pos.y < bounds.bottom
                                    }
                                }
                            },
                            onDragEnd = {
                                if (isDraggingToNav) {
                                    moveItemToNavigation(category)
                                }
                                draggedCategory = null
                                dragPosition = null
                                dragStartPosition = null
                                isDraggingToNav = false
                            },
                            onClick = { clickedItem ->
                                (clickedItem as? CustomCategory)?.onClick?.invoke()
                            },
                            onLongPress = { pressedItem ->
                                if (pressedItem is CustomCategory) {
                                    draggedCategory = pressedItem
                                    dragStartPosition = Offset.Zero
                                    dragPosition = Offset.Zero
                                }
                            }
                        )
                    }
                }
            }
        }

        // Drag overlay with larger icons
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {
            dragPosition?.let { position ->
                draggedNavItem?.let { item ->
                    DraggedItemOverlay(
                        navItem = item,
                        position = position,
                        isDraggingToTarget = isDraggingToAvailable
                    )
                }
                draggedCategory?.let { category ->
                    DraggedItemOverlay(
                        category = category,
                        position = position,
                        isDraggingToTarget = isDraggingToNav
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggedItemOverlay(
    navItem: NavItem? = null,
    category: CustomCategory? = null,
    position: Offset,
    isDraggingToTarget: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isDraggingToTarget) 1.1f else 1f,
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isDraggingToTarget) 0.7f else 0.9f,
        label = "alpha"
    )

    Box(
        modifier = modifier
            .offset { 
                IntOffset(
                    (position.x - 50).toInt(),
                    (position.y - 50).toInt()
                )
            }
            .scale(scale)
            .alpha(alpha)
            .zIndex(100f)
    ) {
        when {
            navItem != null -> NavigationItemContent(item = navItem)
            category != null -> CategoryItemContent(category = category)
        }
    }
}

@Composable
private fun DraggableItem(
    item: Any,
    onDragStart: (Offset, Offset) -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onClick: (Any) -> Unit = {},
    onLongPress: (Any) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.3f else 1f,
        label = "dragScale"
    )

    Box(
        modifier = modifier
            .zIndex(if (isDragging) 99f else 1f)
            .onGloballyPositioned { coordinates ->
                itemPosition = coordinates.boundsInRoot().center
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                shadowElevation = if (isDragging) 8f else 0f
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> 
                        isDragging = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDragStart(offset, itemPosition)
                    },
                    onDrag = { change, dragAmount ->
                        if (dragAmount.getDistance() > 5f) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onDrag(change, dragAmount)
                    },
                    onDragEnd = {
                        isDragging = false
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        onDragEnd()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick(item) },
                    onLongPress = { onLongPress(item) }
                )
            }
    ) {
        when (item) {
            is NavItem -> NavigationItemContent(
                item = item,
                isDragging = isDragging
            )
            is CustomCategory -> CategoryItemContent(
                category = item,
                isDragging = isDragging
            )
            else -> throw IllegalArgumentException("Unsupported item type")
        }
    }
}

@Composable
private fun NavigationItemContent(
    item: NavItem,
    isDragging: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            imageVector = item.selectedIcon,
            contentDescription = item.label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(if (isDragging) 40.dp else 28.dp)
                .graphicsLayer(
                    scaleX = if (isDragging) 1.3f else 1f,
                    scaleY = if (isDragging) 1.3f else 1f
                )
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CategoryItemContent(
    category: CustomCategory,
    isDragging: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(if (isDragging) 40.dp else 28.dp)
                .graphicsLayer(
                    scaleX = if (isDragging) 1.3f else 1f,
                    scaleY = if (isDragging) 1.3f else 1f
                )
        )
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NavigationItemWithWiggle(
    item: NavItem,
    isSelected: Boolean,
    isEditMode: Boolean,
    onItemClick: () -> Unit,
    onLongPress: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    
    val rotation by animateFloatAsState(
        targetValue = if (isEditMode && !item.isLocked) 1.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        },
        label = "tint"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "container"
    )

    Box(
        modifier = modifier
            .graphicsLayer(
                rotationZ = rotation,
                scaleX = scale,
                scaleY = scale
            )
    ) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(4.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { 
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onItemClick()
                        },
                        onLongPress = { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        }
                    )
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = iconTint
                )
            }
        }
    }
}

@Composable
private fun DraggableCategoryChip(
    category: CustomCategory,
    onDragStart: () -> Unit,
    onDragComplete: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(Offset.Zero) }
    var startPosition by remember { mutableStateOf<Offset?>(null) }
    var isOverDropZone by remember { mutableStateOf(false) }
    var isLongPressed by remember { mutableStateOf(false) }

    // Enhanced animations
    val scale by animateFloatAsState(
        targetValue = when {
            isDragging && isOverDropZone -> 1.15f
            isDragging -> 1.1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "drag_scale"
    )

    val elevation by animateFloatAsState(
        targetValue = when {
            isDragging && isOverDropZone -> 12f
            isDragging -> 8f
            else -> 1f
        },
        label = "elevation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isOverDropZone) 0.85f else 1f,
        label = "alpha"
    )

    val rotationZ by animateFloatAsState(
        targetValue = if (isDragging) 2f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )

    Box {
        // Enhanced drop zone indicator
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .offset(y = (-80).dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (isOverDropZone) 0.15f else 0.1f
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (isOverDropZone) 0.8f else 0.3f
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (isOverDropZone) 0.8f else 0.3f
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    if (isOverDropZone) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Release to add to navigation",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    shadowElevation = elevation,
                    alpha = alpha,
                    rotationZ = rotationZ
                )
                .offset { IntOffset(position.x.toInt(), position.y.toInt()) }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            isLongPressed = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isDragging = true
                            onDragStart()
                        }
                    )
                }
                .pointerInput(Unit) {
                    if (isLongPressed) {
                        detectDragGestures(
                            onDragStart = { offset -> 
                                startPosition = offset
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                position += dragAmount
                                val wasOverDropZone = isOverDropZone
                                isOverDropZone = position.y < -60
                                
                                if (wasOverDropZone != isOverDropZone) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                isOverDropZone = false
                                isLongPressed = false
                                haptics.performHapticFeedback(
                                    if (position.y < -60) 
                                        HapticFeedbackType.LongPress
                                    else 
                                        HapticFeedbackType.TextHandleMove
                                )
                                if (position.y < -60) {
                                    onDragComplete(position)
                                }
                                position = Offset.Zero
                                startPosition = null
                            },
                            onDragCancel = {
                                isDragging = false
                                isOverDropZone = false
                                isLongPressed = false
                                position = Offset.Zero
                                startPosition = null
                            }
                        )
                    }
                }
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .animateContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = category.type.name.lowercase().replace('_', ' ').capitalize(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SlideUpPreview(
    isVisible: Boolean,
    dragProgress: Float,
    modifier: Modifier = Modifier
) {
    val previewHeight by animateFloatAsState(
        targetValue = if (isVisible) 60f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "previewHeight"
    )

    val previewAlpha by animateFloatAsState(
        targetValue = dragProgress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "previewAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(previewHeight.dp)
            .alpha(previewAlpha)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Customize Navigation",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Drag items to reorder or remove",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            rotationZ = dragProgress * 180f
                        }
                )
            }
        }
    }
}

@Composable
private fun SlideUpProgressIndicator(
    dragProgress: Float,
    modifier: Modifier = Modifier
) {
    val indicatorWidth by animateFloatAsState(
        targetValue = (dragProgress * 48f).coerceIn(24f, 48f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorWidth"
    )

    val indicatorAlpha by animateFloatAsState(
        targetValue = dragProgress.coerceIn(0.2f, 0.8f),
        label = "indicatorAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(indicatorWidth.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = indicatorAlpha),
                            MaterialTheme.colorScheme.primary.copy(alpha = indicatorAlpha * 1.2f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun ScrollableNavigationBar(
    currentItems: List<NavItem>,
    currentRoute: String?,
    onItemClick: (NavItem) -> Unit,
    onItemLongPress: (NavItem) -> Unit,
    onItemReorder: (List<NavItem>) -> Unit,
    isEditMode: Boolean,
    modifier: Modifier = Modifier
) {
    var draggingItem by remember { mutableStateOf<NavItem?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var itemPositions by remember { mutableStateOf(mutableMapOf<String, Offset>()) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    
    // Calculate item width including spacing
    val itemWidth = 80.dp
    val itemSpacing = 8.dp
    val totalItemWidth = itemWidth + itemSpacing
    val totalItemWidthPx = with(density) { totalItemWidth.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            currentItems.forEachIndexed { index, item ->
                val isDragging = draggingItem == item
                
                // Calculate target position for animations
                val targetOffset = if (isDragging) {
                    dragOffset
                } else {
                    val draggedIndex = currentItems.indexOf(draggingItem)
                    if (draggedIndex != -1 && draggingItem != null) {
                        val draggedPos = itemPositions[draggingItem!!.id] ?: Offset.Zero
                        val thisPos = itemPositions[item.id] ?: Offset.Zero
                        val dragDistance = draggedPos.x - thisPos.x
                        
                        if (abs(dragDistance) > totalItemWidthPx / 2) {
                            // Calculate shift direction and amount
                            val direction = if (dragDistance > 0) 1 else -1
                            Offset(direction * totalItemWidthPx, 0f)
                        } else {
                            Offset.Zero
                        }
                    } else {
                        Offset.Zero
                    }
                }

                val offset by animateOffsetAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "offset"
                )

                Box(
                    modifier = Modifier
                        .size(itemWidth)
                        .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                        .onGloballyPositioned { coordinates ->
                            itemPositions[item.id] = coordinates.boundsInRoot().center
                        }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    if (!item.isLocked && isEditMode) {
                                        draggingItem = item
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (draggingItem != null) {
                                        dragOffset += Offset(dragAmount.x, 0f)
                                        
                                        // Find closest item for swapping
                                        val draggedPos = itemPositions[draggingItem!!.id] ?: return@detectDragGesturesAfterLongPress
                                        val targetPos = draggedPos + dragOffset
                                        
                                        val closestItem = itemPositions
                                            .entries
                                            .filter { it.key != draggingItem!!.id }
                                            .minByOrNull { abs(it.value.x - targetPos.x) }
                                            ?.key
                                            ?.let { id -> currentItems.find { it.id == id } }
                                        
                                        if (closestItem != null && !closestItem.isLocked) {
                                            val fromIndex = currentItems.indexOf(draggingItem)
                                            val toIndex = currentItems.indexOf(closestItem)
                                            
                                            if (fromIndex != -1 && toIndex != -1 && 
                                                abs(targetPos.x - (itemPositions[closestItem.id]?.x ?: 0f)) < totalItemWidthPx / 2
                                            ) {
                                                val newList = currentItems.toMutableList()
                                                newList.removeAt(fromIndex)
                                                newList.add(toIndex, draggingItem!!)
                                                onItemReorder(newList)
                                                dragOffset = Offset.Zero
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggingItem = null
                                    dragOffset = Offset.Zero
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragCancel = {
                                    draggingItem = null
                                    dragOffset = Offset.Zero
                                }
                            )
                        }
                        .clickable(
                            enabled = !isDragging,
                            onClick = { onItemClick(item) }
                        )
                ) {
                    NavigationItemWithWiggle(
                        item = item,
                        isSelected = currentRoute == item.screen.route,
                        isEditMode = isEditMode,
                        onItemClick = { onItemClick(item) },
                        onLongPress = { onItemLongPress(item) },
                        onRemove = { /* Handled by parent */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun animateOffsetAsState(
    targetValue: Offset,
    animationSpec: AnimationSpec<Offset>,
    label: String
): State<Offset> {
    val anim = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    
    LaunchedEffect(targetValue) {
        anim.animateTo(targetValue, animationSpec)
    }
    
    return anim.asState()
}

@Composable
private fun NavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    sharedViewModel: SharedViewModel = hiltViewModel()
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var dragProgress by remember { mutableStateOf(0f) }
    var isSlideGestureActive by remember { mutableStateOf(false) }
    var lastPosition by remember { mutableStateOf(0f) }
    var isEditMode by remember { mutableStateOf(false) }
    var currentNavItems by remember { mutableStateOf(defaultNavItems) }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isBottomSheetVisible by sharedViewModel.isBottomSheetVisible.collectAsState()
    
    val velocityTracker = remember { VelocityTracker() }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        // Enhanced slide-up preview
        SlideUpPreview(
            isVisible = isSlideGestureActive,
            dragProgress = dragProgress,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Progress indicator
        SlideUpProgressIndicator(
            dragProgress = dragProgress,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        ScrollableNavigationBar(
            currentItems = currentNavItems,
            currentRoute = currentRoute,
            onItemClick = { item ->
                if (item.screen == Screen.Search) {
                    scope.launch {
                        sharedViewModel.toggleBottomSheet(true)
                    }
                } else {
                    navController.navigate(item.screen.route) {
                        if (item.screen == Screen.Home) {
                            popUpTo(navController.graph.startDestinationId)
                        }
                        launchSingleTop = true
                    }
                }
            },
            onItemLongPress = { 
                if (!isBottomSheetVisible && !it.isLocked) {
                    isEditMode = true
                }
            },
            onItemReorder = { newItems ->
                currentNavItems = newItems
            },
            isEditMode = isEditMode,
            modifier = Modifier
                .fillMaxWidth()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (!isBottomSheetVisible) {
                            lastPosition += delta
                            dragProgress = (-lastPosition / 150f).coerceIn(0f, 1f)
                            
                            velocityTracker.addPosition(
                                System.currentTimeMillis(),
                                Offset(0f, delta)
                            )
                            
                            if (!isSlideGestureActive && abs(lastPosition) > 5f) {
                                isSlideGestureActive = true
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            
                            when {
                                dragProgress > 0.9f -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                dragProgress > 0.7f -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                dragProgress > 0.4f -> {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                else -> { /* No feedback needed */ }
                            }
                            
                            val velocity = velocityTracker.calculateVelocity()
                            if (velocity.y < -1000f || dragProgress > 0.7f) {
                                scope.launch {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    animate(
                                        initialValue = dragProgress,
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) { value, _ ->
                                        dragProgress = value
                                    }
                                    
                                    sharedViewModel.toggleBottomSheet(true)
                                    lastPosition = 0f
                                    isSlideGestureActive = false
                                    velocityTracker.clear()
                                }
                            }
                        }
                    },
                    onDragStarted = {
                        if (!isBottomSheetVisible) {
                            isSlideGestureActive = true
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            velocityTracker.clear()
                        }
                    },
                    onDragStopped = {
                        if (dragProgress < 0.7f) {
                            scope.launch {
                                animate(
                                    initialValue = dragProgress,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) { value, _ ->
                                    dragProgress = value
                                }
                                
                                lastPosition = 0f
                                isSlideGestureActive = false
                                velocityTracker.clear()
                            }
                        }
                    }
                )
        )
    }
}

// Helper class for tracking velocity
private class VelocityTracker {
    private var lastPosition = Offset.Zero
    private var lastTime = 0L
    
    fun addPosition(timeMillis: Long, position: Offset) {
        lastPosition = position
        lastTime = timeMillis
    }
    
    fun calculateVelocity(): Offset {
        val timeDelta = (System.currentTimeMillis() - lastTime).coerceAtLeast(1L)
        return Offset(
            lastPosition.x / timeDelta.toFloat(),
            lastPosition.y / timeDelta.toFloat()
        )
    }
    
    fun clear() {
        lastPosition = Offset.Zero
        lastTime = 0L
    }
} 