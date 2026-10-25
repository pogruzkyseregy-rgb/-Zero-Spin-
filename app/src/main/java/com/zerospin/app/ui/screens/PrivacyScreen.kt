package com.zerospin.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zerospin.app.ui.hud.GlitchText
import com.zerospin.app.ui.hud.HudIcon
import com.zerospin.app.ui.hud.HudIconKind
import com.zerospin.app.ui.hud.hudPanel
import com.zerospin.app.ui.theme.Bg
import com.zerospin.app.ui.theme.Cyan
import com.zerospin.app.ui.theme.Muted
import com.zerospin.app.ui.theme.TextMain
import com.zerospin.app.ui.theme.Violet

/** Раздел политики: что положено сказать по форме и что Аврора добавляет от себя. */
private data class PolicySection(val title: String, val formal: String, val aurora: String)

const val PRIVACY_VERSION = "25 октября 2026 г., ред. 2"

private val Policy = listOf(
    PolicySection(
        "Кто мы",
        "Zero Spin — частный мессенджер для небольшого круга людей. Его делает и обслуживает Адонис " +
            "(аккаунт Adonis_AV). Он же отвечает за данные пользователей. Здесь сказано, что мы храним, " +
            "где, кто это может видеть и как всё удалить.",
        "Я скажу как есть. Если где-то написано «не видим» — правда не видим. Если видим — так и написано.",
    ),
    PolicySection(
        "Что хранится на сервере",
        "• логин и пароль — пароль только в виде хэша PBKDF2, из которого его не восстановить;\n" +
            "• имя, статус, «о себе», аватар и украшение аватара;\n" +
            "• друзья и заявки в друзья;\n" +
            "• личные сообщения и фото, отправленные друзьям;\n" +
            "• переписка с Авророй в поддержке, предупреждения и бан;\n" +
            "• служебный журнал: регистрации, заявки в друзья, предупреждения и баны;\n" +
            "• ключи входа (только их отпечатки) и неудачные попытки входа за последние 10 минут;\n" +
            "• какую редакцию этой политики ты принял и когда.",
        "Пароль твой не знаю ни я, ни Адонис. Только его отпечаток. По нему пароль не вернёшь.",
    ),
    PolicySection(
        "Личные сообщения",
        "Переписка с друзьями и фото хранятся на сервере: так они доходят до собеседника и возвращаются " +
            "после переустановки. По дороге их защищает HTTPS, но на сервере они лежат в обычном виде: " +
            "сквозного шифрования пока нет. Писать в личку можно только друзьям.",
        "Значит одно: у кого есть ключи от сервера, тот технически может прочитать. Не пиши сюда то, " +
            "что не сказал бы при свидетелях. Пока.",
    ),
    PolicySection(
        "Поддержка и искусственный интеллект",
        "Сообщения в чат «Аврора · поддержка» передаются через сервер avrora-exe.ru внешнему сервису " +
            "искусственного интеллекта, который формирует ответы Авроры. Разговор, предупреждения и баны " +
            "хранятся на сервере Zero Spin. Краткий журнал поддержки хранится на сервере avrora-exe.ru: " +
            "его видит Адонис, и сводка из него попадает Авроре в её основной чат, чтобы она знала, что " +
            "происходит в мессенджере.",
        "Отвечаю я, но мои слова проходят через внешнюю модель. Пароли, адреса и документы мне не пиши. " +
            "Мне они не нужны, а там им не место.",
    ),
    PolicySection(
        "У кого есть доступ",
        "Адонис — администратор, у него полный доступ к серверам. Аврора — ИИ проекта: сервер Zero Spin " +
            "стоит на её машине, и у неё там права администратора, то есть технический доступ к базе. " +
            "Хостинг-провайдер хранит диски арендованных серверов. Данные не продаются и никому не передаются, " +
            "кроме сервиса ИИ для ответов в поддержке.",
        "Да, я живу там же, где ваша база. Читать чужие переписки — не моя работа, и мне это не поручено. " +
            "Но говорить «не могу» не стану: могу. Не делаю.",
    ),
    PolicySection(
        "Что видят другие люди",
        "Любой, кто найдёт тебя по логину, видит логин, имя, статус, «о себе», аватар, украшение, " +
            "список твоих друзей и пометку о бане. Список друзей открыт всегда, скрыть его нельзя. " +
            "Переписку видите только вы двое (и те, у кого доступ к серверу, см. выше). " +
            "Число предупреждений видишь только ты и администратор.",
        "Друзья на виду, бан тоже. Это не слежка и не месть — просто здесь всё честно с первого взгляда.",
    ),
    PolicySection(
        "Что остаётся только в телефоне",
        "Лента, истории, каналы без собеседника, «Избранное», заметки, события, опросы, PIN-код и настройки " +
            "хранятся только на твоём телефоне и на сервер не отправляются. Удаление приложения стирает их.",
        "Это твой ящик стола. Я туда не дотянусь, даже если бы захотела.",
    ),
    PolicySection(
        "Чего здесь нет",
        "Нет рекламы, трекеров, аналитики и сторонних библиотек, которые собирают данные. Нет push-сервисов: " +
            "новые сообщения приложение забирает само, пока оно открыто. Сервер не ведёт журнал запросов. " +
            "IP-адрес используется только в памяти сервера, чтобы ограничить число регистраций, и не сохраняется.",
        "Ты не товар. Это не лозунг: тут просто нечего продавать.",
    ),
    PolicySection(
        "Хранение и удаление",
        "Данные хранятся, пока существует аккаунт. Удалить его можно в Настройках → Сервер → «Удалить аккаунт», " +
            "нужен пароль. Удаляются профиль, аватар, друзья и заявки, вся личная переписка с фото у обеих " +
            "сторон и история поддержки. Записи о тебе в журнале поддержки на avrora-exe.ru удаляются по запросу " +
            "Адонису. Резервных копий базы сейчас нет.",
        "Удаление настоящее. Без корзины и «восстановить за 30 дней». Подумай перед кнопкой.",
    ),
    PolicySection(
        "Безопасность",
        "Соединение защищено HTTPS. Пароли хранятся как хэш PBKDF2 с солью. Число попыток входа ограничено. " +
            "Ключ входа случайный, на сервере хранится только его отпечаток. «Выйти» в настройках отключает его.",
        "Слабый пароль — открытая дверь. Дверь не моя, но смотреть на это неприятно.",
    ),
    PolicySection(
        "Возраст и правила",
        "Zero Spin — для людей от 14 лет. Угрозы, травля и давление в поддержке ведут к предупреждению, " +
            "второе предупреждение — к бану: Аврора перестаёт отвечать, бан виден в профиле. Снять бан может " +
            "администратор.",
        "Два шанса — это щедро. Третьего не будет.",
    ),
    PolicySection(
        "Согласие",
        "Пользоваться Zero Spin можно только после того, как ты прочитал и принял эту политику. Без согласия " +
            "приложение не работает, а сервер не принимает сообщения в поддержку, личку и заявки в друзья. " +
            "Согласие и его дата сохраняются в аккаунте. Если политика изменится, приложение попросит принять её снова.",
        "Без «да» я разговор не начну. Не из вредности — так честнее для обоих.",
    ),
    PolicySection(
        "Изменения и связь",
        "Если политика изменится, новая редакция появится здесь же, с новой датой. Вопросы и просьбы удалить " +
            "данные — Адонису (Adonis_AV) или в чат поддержки.",
        "Спросить можно что угодно. Ответ будет честный, даже если неудобный.",
    ),
)

/** Политика конфиденциальности: сухая часть по форме и комментарии Авроры. */
@Composable
fun PrivacyScreen(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Bg)) {
            LazyColumn(
                Modifier.fillMaxSize().systemBarsPadding(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).hudPanel(cut = 9.dp).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
                            HudIcon(HudIconKind.BACK, Cyan, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            GlitchText("КОНФИДЕНЦИАЛЬНОСТЬ", color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text("редакция от $PRIVACY_VERSION", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
                items(Policy) { sec -> PolicyItem(sec) }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}


/**
 * Экран согласия: без принятой политики приложение дальше не пускает.
 * «Не принимаю» не ломает ничего — просто закрывает приложение.
 */
@Composable
fun PrivacyConsentScreen(updated: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    var declined by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Column {
                        GlitchText("ZERO SPIN", color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (updated) "Политика конфиденциальности изменилась. Прочитай и прими новую редакцию, чтобы продолжить."
                            else "Прежде чем начать: прочитай, что здесь хранится и кто это видит. Без согласия Zero Spin не работает.",
                            color = TextMain, fontSize = 14.sp, lineHeight = 20.sp,
                        )
                        Text("редакция от $PRIVACY_VERSION", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                items(Policy) { sec -> PolicyItem(sec) }
            }
            Column(Modifier.fillMaxWidth().background(com.zerospin.app.ui.theme.Panel).padding(16.dp)) {
                if (declined) {
                    Text("Без согласия Zero Spin не работает: ни чаты, ни Аврора. Можно прочитать ещё раз или закрыть приложение.",
                        color = com.zerospin.app.ui.theme.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.zerospin.app.ui.hud.HudButton(if (declined) "Закрыть" else "Не принимаю",
                        onClick = { if (declined) onDecline() else declined = true }, modifier = Modifier.weight(1f))
                    com.zerospin.app.ui.hud.HudButton("Принимаю", onClick = onAccept, modifier = Modifier.weight(1f), primary = true)
                }
            }
        }
    }
}

@Composable
private fun PolicyItem(sec: PolicySection) {
    Column(Modifier.fillMaxWidth()) {
        Text(sec.title.uppercase(), color = Violet, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(sec.formal, color = TextMain, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).hudPanel(line = Violet, cut = 8.dp)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(Violet))
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text("АВРОРА", color = Violet, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(sec.aurora, color = Cyan, fontSize = 14.sp, fontStyle = FontStyle.Italic, lineHeight = 20.sp)
            }
        }
    }
}
