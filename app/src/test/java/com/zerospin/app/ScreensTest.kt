package com.zerospin.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.zerospin.app.data.AppStore
import com.zerospin.app.data.Chat
import com.zerospin.app.data.ChatMessage
import com.zerospin.app.data.Comment
import com.zerospin.app.data.Post
import com.zerospin.app.ui.ZeroSpinApp
import com.zerospin.app.ui.theme.ZeroSpinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Снимки основных экранов. Запускается на GitHub (workflow Screenshots): так видно,
 * как выглядит приложение, без телефона. Картинки — в артефакте zero-spin-screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ScreensTest {
    @get:Rule
    val rule = createComposeRule()

    private fun seed() {
        val store = AppStore(RuntimeEnvironment.getApplication())
        store.settings.value = store.settings.value.copy(authSkipped = true, animatedBackground = false)
        store.profile.value = store.profile.value.copy(
            name = "Адонис", nick = "adonis", status = "Пишу код ночью", bio = "Делаю Zero Spin и Аврору.",
            friends = listOf("anna_k", "bob_r"), cosmetic = "crown",
        )
        store.chats.add(Chat(5, "Анна", peer = "anna_k", unread = 2))
        store.messages[5] = listOf(
            ChatMessage(1, false, "Привет! Как продвигается мессенджер?", "21:40"),
            ChatMessage(2, true, "Почти готово: друзья и личка уже работают", "21:42", remoteId = 11),
            ChatMessage(3, false, "Круто! Скинешь apk?", "21:43", replyTo = 2, myReaction = "❤️", reactionCount = 1),
            ChatMessage(4, true, "Сейчас соберу", "21:44"),
        )
        store.chats.add(Chat(6, "Ночной эфир"))
        store.messages[6] = listOf(ChatMessage(1, true, "Заметки на ночь: **проверить** сборку", "02:10"))
        store.posts.add(Post(1, "Адонис", "12:30", "Первый пост в Zero Spin. Тут будут новости круга.", emptyList(), 4, true,
            comments = listOf(Comment(1, "Анна", "Ура!", "12:31"))))
        store.posts.add(Post(2, "Аврора", "11:05", "Тишина тоже сигнал.", emptyList(), 12, false, reposts = 2))
        store.save(store.toJsonString())
    }

    private fun start() {
        seed()
        rule.mainClock.autoAdvance = false
        rule.setContent { ZeroSpinTheme { ZeroSpinApp() } }
        settle()
    }

    private fun settle() = rule.mainClock.advanceTimeBy(1_500)

    private fun shot(name: String) {
        settle()
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun tap(text: String) {
        rule.onAllNodesWithText(text, substring = true, ignoreCase = true)[0].performClick()
        settle()
    }

    @Test
    fun feed() {
        start()
        shot("1_feed")
    }

    @Test
    fun chats() {
        start()
        tap("ЧАТ")
        shot("2_chats")
        tap("Анна")
        shot("3_conversation")
    }

    @Test
    fun profile() {
        start()
        tap("ПРОФИЛЬ")
        shot("4_profile")
    }

    @Test
    fun cosmetics() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            ZeroSpinTheme {
                androidx.compose.foundation.layout.Column(
                    androidx.compose.ui.Modifier.fillMaxSize().background(com.zerospin.app.ui.theme.Bg).padding(24.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(36.dp),
                ) {
                    com.zerospin.app.ui.hud.AvatarCosmetics.chunked(3).forEach { row ->
                        androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                            row.forEach { (id, title) ->
                                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.width(100.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.size(84.dp)) {
                                        com.zerospin.app.ui.hud.TargetRing(androidx.compose.ui.Modifier.size(84.dp)) {
                                            androidx.compose.material3.Text("A", color = com.zerospin.app.ui.theme.Cyan)
                                        }
                                        com.zerospin.app.ui.hud.CosmeticOverlay(id, 84)
                                    }
                                    androidx.compose.material3.Text(title, color = com.zerospin.app.ui.theme.Muted)
                                }
                            }
                        }
                    }
                }
            }
        }
        shot("6_cosmetics")
    }

    @Test
    fun profileEdit() {
        start()
        tap("ПРОФИЛЬ")
        rule.onNodeWithContentDescription("Изменить профиль").performClick()
        settle()
        rule.onRoot().performTouchInput { swipeUp() }
        shot("7_profile_edit")
    }

    @Test
    fun apps() {
        start()
        tap("ПРИЛОЖЕНИЯ")
        shot("5_apps")
    }
}
