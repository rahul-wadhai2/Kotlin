package com.example.githubrepository.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import com.example.githubrepository.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.githubrepository.domain.model.Repository
import com.example.githubrepository.presentation.viewmodel.SearchViewModel

/**
 * DetailScreen displays complete repository information such as
 * description, stars, forks, and owner details. It also allows
 * the user to mark/unmark the repository as a favorite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    repo: Repository,
    viewModel: SearchViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val favorites by viewModel.favorites.collectAsState()
    val isFavorite = favorites.contains(repo.id)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repo.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // CRITICAL CHANGE: Pass the full 'repo' object to the ViewModel
                    IconButton(onClick = { viewModel.toggleFavorite(repo) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite
                            else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (isFavorite) Color.Red else Color.Gray
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Owner Info
            AsyncImage(
                model = repo.ownerAvatarUrl,
                contentDescription = "Owner Avatar",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = repo.ownerName,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Repository Details
            DetailItem(
                label = stringResource(id = R.string.description),
                value = repo.description,
                icon = Icons.Filled.Info
            )
            DetailItem(
                label = stringResource(id = R.string.language),
                value = repo.language,
                icon = Icons.Filled.Star
            )
            DetailItem(
                label = stringResource(id = R.string.stars),
                value = repo.starCount.toString(),
                icon = Icons.Filled.Star,
                valueColor = Color(0xFFFFC107)
            )
            DetailItem(
                label = stringResource(id = R.string.forks),
                value = repo.forkCount.toString(),
                icon = Icons.Default.Favorite
            )
        }
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = Color.Unspecified
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$label:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = valueColor,
            modifier = Modifier.padding(start = 24.dp)
        )
    }
}