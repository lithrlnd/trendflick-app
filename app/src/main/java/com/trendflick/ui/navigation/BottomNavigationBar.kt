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
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding

data class NavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showBadge: Boolean = false,
    val isLocked: Boolean = false, // For Home item that can't be removed
    val isDraggable: Boolean = true
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

    // State for dragging and editing
    var isEditMode by remember { mutableStateOf(false) }
    var draggedItem by remember { mutableStateOf<CustomCategory?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    
    // Track initial navigation items separately
    val initialNavItems = remember {
        listOf(
            NavItem(Screen.Home, "Home", Icons.Default.Home, Icons.Outlined.Home, isLocked = true),
            NavItem(Screen.Messages, "Messages", Icons.Default.Message, Icons.Outlined.Message),
            NavItem(Screen.Search, "Search", Icons.Default.Search, Icons.Outlined.Search),
            NavItem(Screen.AI, "AI", Icons.Default.SmartToy, Icons.Outlined.SmartToy),
            NavItem(Screen.Profile, "Profile", Icons.Default.Person, Icons.Outlined.Person)
        )
    }
    
    // State for current navigation items
    var currentNavItems by remember { mutableStateOf(initialNavItems) }
    
    // Track removed initial items
    var removedInitialItems by remember { mutableStateOf<List<NavItem>>(emptyList()) }

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
            // If it's an initial item, add to removedInitialItems
            if (initialNavItems.any { it.screen == item.screen }) {
                removedInitialItems = removedInitialItems + item
            } else {
                // If it's a custom category, add back to available categories
                val category = availableCategories.find { it.id == (item.screen as? Screen.Custom)?.id }
                if (category != null) {
                    availableCategories = availableCategories + category
                }
            }
            currentNavItems = currentNavItems - item
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
            customCategories + removedInitialItems.map { navItem ->
                CustomCategory(
                    id = navItem.screen.route,
                    icon = navItem.selectedIcon,
                    label = navItem.label,
                    type = CategoryType.APP_VIEW,
                    description = "Navigation item",
                    onClick = {}
                )
            }
        } else {
            customCategories
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    Box(modifier = modifier.fillMaxWidth()) {
        // Navigation bar with slide up gesture
        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (dragAmount.y < -50f) { // Swipe Up
                                scope.launch {
                                    sharedViewModel.toggleBottomSheet(true)
                                }
                            }
                        }
                    )
                },
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
                        isEditMode = isEditMode,
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
                        onLongPress = { if (!item.isLocked) isEditMode = true },
                        onRemove = { handleItemRemoval(item) }
                    )
                }
            }
        }
    }

    // Bottom sheet content
    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { 
                scope.launch {
                    sharedViewModel.toggleBottomSheet(false)
                    isEditMode = false
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            ) {
                // Current Navigation Section with animation
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Text(
                            text = "Current Navigation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(currentNavItems) { item ->
                                NavigationItemWithWiggle(
                                    item = item,
                                    isSelected = currentRoute == item.screen.route,
                                    isEditMode = isEditMode,
                                    onItemClick = {},
                                    onLongPress = { if (!item.isLocked) isEditMode = true },
                                    onRemove = { handleItemRemoval(item) }
                                )
                            }
                        }
                    }
                }

                // Available Items Section with animation
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Text(
                            text = "Available Items",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        // Show Navigation group first if there are removed initial items
                        if (removedInitialItems.isNotEmpty()) {
                            Text(
                                text = "Navigation",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(getAllAvailableItems("Navigation")) { category ->
                                    DraggableCategoryChip(
                                        category = category,
                                        onDragStart = { draggedItem = category },
                                        onDragComplete = { position ->
                                            if (position.y < 0 && currentNavItems.size < 7) {
                                                // Check if it's a removed initial item
                                                val initialItem = removedInitialItems.find { it.screen.route == category.id }
                                                if (initialItem != null) {
                                                    currentNavItems = currentNavItems + initialItem
                                                    removedInitialItems = removedInitialItems - initialItem
                                                } else {
                                                    // Handle custom category
                                                    val navItem = NavItem(
                                                        screen = Screen.Custom(category.id),
                                                        label = category.label,
                                                        selectedIcon = category.icon,
                                                        unselectedIcon = category.icon
                                                    )
                                                    availableCategories = availableCategories - category
                                                    currentNavItems = currentNavItems + navItem
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Show other category groups
                        groupedCategories.forEach { (groupTitle, _) ->
                            val availableItems = getAllAvailableItems(groupTitle)
                            if (availableItems.isNotEmpty() && groupTitle != "Navigation") {
                                Text(
                                    text = groupTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(availableItems) { category ->
                                        DraggableCategoryChip(
                                            category = category,
                                            onDragStart = { draggedItem = category },
                                            onDragComplete = { position ->
                                                if (position.y < 0 && currentNavItems.size < 7) {
                                                    val navItem = NavItem(
                                                        screen = Screen.Custom(category.id),
                                                        label = category.label,
                                                        selectedIcon = category.icon,
                                                        unselectedIcon = category.icon
                                                    )
                                                    availableCategories = availableCategories - category
                                                    currentNavItems = currentNavItems + navItem
                                                }
                                            }
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

    // Show drag preview if an item is being dragged
    draggedItem?.let { item ->
        dragPosition?.let { position ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(position.x.toInt(), position.y.toInt()) }
                    .alpha(0.7f)
            ) {
                DraggableCategoryChip(
                    category = item,
                    onDragStart = { draggedItem = item },
                    onDragComplete = { pos ->
                        // Check if dragged to navigation area
                        if (pos.y < 0) {
                            // Convert category to NavItem and add to navigation
                            val navItem = NavItem(
                                screen = Screen.Custom(item.id),
                                label = item.label,
                                selectedIcon = item.icon,
                                unselectedIcon = item.icon
                            )
                            if (currentNavItems.size < 7) { // Limit to 7 items
                                currentNavItems = currentNavItems + navItem
                            }
                        }
                    },
                    modifier = Modifier.scale(1.1f)
                )
            }
        }
    }
}

@Composable
private fun NavigationItemWithWiggle(
    item: NavItem,
    isSelected: Boolean,
    isEditMode: Boolean,
    onItemClick: () -> Unit,
    onLongPress: () -> Unit,
    onRemove: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    
    val rotation by animateFloatAsState(
        targetValue = if (isEditMode && !item.isLocked) 1.5f else 0f,  // Reduced rotation
        animationSpec = infiniteRepeatable(
            animation = tween(250),  // Slightly slower for better feel
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,  // Reduced scale
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        label = "tint"
    )

    Box(
        modifier = Modifier
            .graphicsLayer(
                rotationZ = rotation,
                scaleX = scale,
                scaleY = scale
            )
    ) {
        // Show remove button when in edit mode - smaller and more native-like
        if (isEditMode && !item.isLocked) {
            Box(
                modifier = Modifier
                    .size(16.dp)  // Smaller size
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)  // Adjusted offset
                    .background(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                        shape = CircleShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRemove()
                            }
                        )
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier
                        .size(12.dp)  // Smaller icon
                        .align(Alignment.Center)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(12.dp)
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

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "drag_scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .offset { IntOffset(position.x.toInt(), position.y.toInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        isDragging = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        position += dragAmount
                        if (dragAmount != Offset.Zero) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDragComplete(position)
                        position = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        position = Offset.Zero
                    }
                )
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