package com.zerospin.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zerospin.app.data.Post
import com.zerospin.app.ui.components.Avatar
import com.zerospin.app.ui.components.ImagePlaceholder
import com.zerospin.app.ui.theme.CardBg
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.TextMain
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun FeedScreen(posts: MutableList<Post>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                onLike = {
                    val index = posts.indexOfFirst { it.id == post.id }
                    if (index >= 0) {
                        val current = posts[index]
                        posts[index] = current.copy(
                            liked = !current.liked,
                            likes = if (current.liked) current.likes - 1 else current.likes + 1,
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun PostCard(post: Post, onLike: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        border = BorderStroke(1.dp, post.accent.copy(alpha = 0.55f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(name = post.author, accent = post.accent, size = 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = post.author,
                        color = post.accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = post.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Ещё",
                    tint = Muted,
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = post.text,
                color = TextMain,
                style = MaterialTheme.typography.bodyLarge,
            )

            if (post.hasImage) {
                Spacer(Modifier.height(10.dp))
                ImagePlaceholder(accent = post.accent)
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike) {
                    Icon(
                        imageVector = if (post.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Лайк",
                        tint = if (post.liked) post.accent else Muted,
                    )
                }
                Text(
                    text = if (post.liked) "ты поставил лайк" else "${post.likes} лайков",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
