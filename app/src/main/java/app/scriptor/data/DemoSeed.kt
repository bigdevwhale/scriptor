package app.scriptor.data

import android.content.Context
import app.scriptor.AppContainer
import app.scriptor.domain.Memory
import app.scriptor.domain.Place

/**
 * A scriptorium half a season in, for screenshots (`-Pdemo` only): a book on learning with a gilded
 * page and a confident mistake, a closed codex, a lecture written up yesterday, pages fading tonight.
 * Written in the phone's language, Russian or English.
 */
object DemoSeed {
    private const val DAY = Memory.DAY

    private class P(
        val title: String, val locator: String, val body: String, val cues: String = "", val gold: String = "", val colophon: String = "",
        val quote: String = "",
        /** Days the trace holds, days since the last restoration, days since written. */
        val s: Double, val since: Double, val age: Double,
        val reps: Int = 0, val lapses: Int = 0, val erratum: Boolean = false,
        val glosses: List<Pair<GlossKind, Pair<String, String>>> = emptyList(),
        /** (days ago, mode, candles, grade, coverage, attempt) */
        val reviews: List<R> = emptyList(),
    )

    private class R(val ago: Double, val mode: Mode, val candles: Int, val grade: Int, val coverage: Float = -1f, val attempt: String = "")

    private class C(
        val kind: Kind, val title: String, val author: String, val binding: Int, val age: Double,
        val preface: String = "", val questions: String = "", val answered: Int = 0,
        val sentence: String = "", val words: String = "", val surprise: String = "", val closedAgo: Double? = null,
        val pages: List<P>,
    )

    suspend fun seedIfEmpty(context: Context, c: AppContainer) {
        val dao = c.db.dao()
        if (dao.codices().isNotEmpty()) return
        val ru = context.resources.configuration.locales[0].language == "ru"
        val now = System.currentTimeMillis()
        fun at(daysAgo: Double) = now - (daysAgo * DAY).toLong()
        val ids = mutableMapOf<String, Long>()
        for (cx in if (ru) russian() else english()) {
            val id = dao.insert(
                Codex(
                    kind = cx.kind.name, title = cx.title, author = cx.author, binding = cx.binding, preface = cx.preface,
                    questions = cx.questions, answered = cx.answered, sentence = cx.sentence, words = cx.words, surprise = cx.surprise,
                    createdAt = at(cx.age), closedAt = cx.closedAgo?.let(::at),
                )
            )
            for (p in cx.pages) {
                val pid = dao.insert(
                    Page(
                        codexId = id, title = p.title, locator = p.locator, body = p.body, cues = p.cues, gold = p.gold, colophon = p.colophon,
                        quote = p.quote, miniature = if (p.title == CURVE_RU || p.title == CURVE_EN) curve() else "", stability = p.s, difficulty = 5.0, reviewedAt = at(p.since), reps = p.reps, lapses = p.lapses,
                        erratum = p.erratum, createdAt = at(p.age), updatedAt = at(p.age),
                    )
                )
                ids[p.title] = pid
                p.glosses.forEachIndexed { i, (k, pt) ->
                    dao.insert(Gloss(pageId = pid, kind = k.name, prompt = pt.first, text = pt.second, createdAt = at(p.age) + (i + 1) * 3_600_000L))
                }
                p.reviews.forEach { r ->
                    dao.insert(Review(pageId = pid, at = at(r.ago), mode = r.mode.name, confidence = r.candles, grade = r.grade, coverage = r.coverage, attempt = r.attempt))
                }
            }
        }
        fun id(title: String) = ids[title]
        val t = { en: String, rus: String -> if (ru) rus else en }
        // Red threads across the books.
        listOf(
            Triple(t("Recall beats rereading", "Вспоминание сильнее перечитывания"), t("The forgetting curve", "Кривая забывания"),
                t("Each recall is what flattens the curve.", "Каждое вспоминание и делает кривую положе.")),
            Triple(t("The illusion of knowing", "Иллюзия знания"), t("Two systems", "Две системы"),
                t("Fluency is System 1 saying \"I know this\" before System 2 has checked.", "Беглость — это Система 1 говорит «знаю», пока Система 2 ещё не проверила.")),
            Triple(t("It isn't things that trouble us", "Нас тревожат не вещи"), t("Anchoring", "Якорение"),
                t("Both: the first judgement isn't the event. Notice it, then decide.", "Обе о том, что первое суждение — ещё не событие. Заметь его, потом решай.")),
            Triple(t("Mix the topics", "Перемешивай темы"), t("Sleep and memory", "Сон и память"),
                t("Hard practice by day, and sleep sorts it at night.", "Трудная практика днём, а ночью сон всё раскладывает.")),
        ).forEachIndexed { i, (a, b, note) ->
            val from = id(a) ?: return@forEachIndexed
            val to = id(b) ?: return@forEachIndexed
            dao.insert(Thread(fromPage = from, toPage = to, note = note, createdAt = at(10.0 - i)))
        }
        // A disputation on mixing.
        id(t("Mix the topics", "Перемешивай темы"))?.let { pid ->
            val d = app.scriptor.domain.Dispute(
                t("Mixing kinds of problems teaches better than blocks.", "Перемешанные задачи учат лучше, чем блоки."),
                listOf(
                    t("Blocks feel faster, and students do better during practice.", "С блоками быстрее, и на тренировке результаты лучше.") to
                        t("Better during practice, worse a week later: what is measured is the wrong moment.", "Лучше на тренировке, хуже через неделю: мерили не в тот момент."),
                    t("A beginner who doesn't know any method yet will just drown.", "Новичок, не знающий ни одного способа, просто утонет.") to
                        t("True: first a short block to meet each kind, then mix.", "Верно: сначала короткий блок, чтобы познакомиться, потом перемешивать."),
                ),
            )
            dao.insert(Gloss(pageId = pid, kind = GlossKind.DISPUTE.name, prompt = d.thesis, text = d.encode(), createdAt = at(8.0)))
        }
        // The monastery of memory.
        listOf(
            Triple(Place.GATE, t("Recall beats rereading", "Вспоминание сильнее перечитывания"),
                t("The gatekeeper won't open until I recite the password from memory; a sign with the password on it is nailed up, and he tears it down.",
                    "Привратник не открывает, пока я не скажу пароль по памяти; табличку с паролем он срывает с ворот.")),
            Triple(Place.REFECTORY, t("Mix the topics", "Перемешивай темы"),
                t("The cook throws soup, bread and pears into one huge pot, and the monks have to guess what each spoonful is.",
                    "Повар валит суп, хлеб и груши в один котёл, и монахи угадывают, что в каждой ложке.")),
            Triple(Place.CELL, t("Sleep and memory", "Сон и память"),
                t("On the bed, a sleeping monk sorts glowing pages into a chest, without waking.",
                    "На кровати спящий монах, не просыпаясь, раскладывает светящиеся листы по сундуку.")),
            Triple(Place.BELLTOWER, t("The forgetting curve", "Кривая забывания"),
                t("The bell rope is a slide: each time the bell rings, the slide gets less steep.",
                    "Верёвка колокола — горка: с каждым ударом колокола она всё положе.")),
            Triple(Place.CHAPEL, t("It isn't things that trouble us", "Нас тревожат не вещи"),
                t("A stone in the aisle has a label: \"just a stone\". The monks who trip are shouting at the label.",
                    "На камне в проходе табличка «просто камень». Споткнувшиеся монахи кричат на табличку.")),
        ).forEachIndexed { i, (place, page, image) ->
            val pid = id(page) ?: return@forEachIndexed
            dao.insert(Locus(place = place.name, pageId = pid, image = image, createdAt = at(5.0 - i * 0.1)))
        }
        c.settings.update {
            it.copy(onboarded = true, gender = if (ru) app.scriptor.domain.Gender.MALE else it.gender, crafts = app.scriptor.domain.Craft.entries.map { k -> k.name }.toSet())
        }
    }

    private const val CURVE_RU = "Кривая забывания"
    private const val CURVE_EN = "The forgetting curve"

    /** A hand-drawn forgetting curve: the axes, the steep first fall, and the flatter falls after each review. */
    private fun curve(): String {
        val strokes = mutableListOf<List<Pair<Float, Float>>>()
        strokes += listOf(0.12f to 0.08f, 0.12f to 0.88f, 0.94f to 0.88f)
        strokes += listOf(0.09f to 0.13f, 0.12f to 0.08f, 0.15f to 0.13f)
        var x0 = 0.12f
        val spans = listOf(0.12f, 0.17f, 0.24f, 0.29f)
        val depths = listOf(0.62f, 0.42f, 0.27f, 0.16f)
        spans.forEachIndexed { i, span ->
            val pts = (0..14).map { k ->
                val f = k / 14f
                val drop = depths[i] * (1 - kotlin.math.exp(-4f * f)) / (1 - kotlin.math.exp(-4f))
                (x0 + span * f) to (0.14f + drop)
            }
            strokes += pts
            // Back to the top: a review.
            if (i < spans.size - 1) strokes += listOf(pts.last(), (x0 + span) to 0.14f)
            x0 += span
        }
        // Little ticks for the reviews.
        var x = 0.12f
        spans.dropLast(1).forEach { span ->
            x += span
            strokes += listOf(x to 0.85f, x to 0.91f)
        }
        return app.scriptor.domain.Strokes.encode(strokes)
    }

    private fun russian() = listOf(
        C(
            Kind.BOOK, "Запомнить всё", "П. Браун, Г. Рёдигер, М. Макдэниел", 0, 64.0,
            preface = "Думаю, будет про мнемотехники и про то, что надо больше повторять. Наверное, скажут, что конспекты помогают.",
            questions = "Почему перечитывание не работает?\nКак учиться, чтобы не забывать через неделю?\nЧто делать с иллюзией, что всё понял?",
            answered = 0b011,
            pages = listOf(
                P(
                    "Вспоминание сильнее перечитывания", "Глава 2",
                    "Когда перечитываешь, текст кажется знакомым, и это ощущение легко принять за знание. А память укрепляет именно усилие достать что-то из головы. Каждый раз, когда вспоминаешь, путь к мысли протаптывается заново и становится шире. Поэтому тест — не проверка, а сама учёба. Лучше закрыть книгу и попробовать пересказать, чем прочитать главу второй раз.",
                    cues = "Почему перечитывание обманывает?\nЧто именно укрепляет память?\nЗачем тест, если ещё не выучил?",
                    gold = "Вспоминать — и есть учиться.",
                    colophon = "Закрой книгу и перескажи — это стоит трёх перечитываний.",
                    s = 210.0, since = 12.0, age = 63.0, reps = 6,
                    glosses = listOf(
                        GlossKind.NOVICE to ("Приведи пример из жизни?" to "Перед экзаменом по вождению я сто раз читал билеты и всё равно путался. Помогло, когда друг стал гонять меня вопросами вслух."),
                        GlossKind.EXEMPLUM to ("" to "Выучил испанские слова карточками за неделю, а перечитанный список забыл за два дня."),
                        GlossKind.EXEMPLUM to ("" to "На работе пересказываю коллеге статью — и наутро помню её почти целиком."),
                        GlossKind.SEAL to ("" to "Две недели после каждой лекции курса писал страницу по памяти. На итоговом тесте — 92%, раньше было около 70."),
                    ),
                    reviews = listOf(
                        R(60.0, Mode.RECALL, 2, 3, 0.62f, "Перечитывать бесполезно, надо вспоминать. Вспоминание укрепляет память, а знакомость текста обманчива."),
                        R(52.0, Mode.CUES, 2, 3), R(40.0, Mode.LACUNAE, 3, 4), R(28.0, Mode.RECALL, 3, 4, 0.88f, "Знакомое ≠ выученное. Усилие достать из памяти протаптывает путь. Тест — это учёба, а не проверка. Лучше пересказать, чем перечитать."),
                        R(12.0, Mode.CUES, 3, 4),
                    ),
                ),
                P(
                    "Перемешивай темы", "Глава 3",
                    "Если решать подряд двадцать задач одного типа, к пятой уже работаешь на автомате и не выбираешь способ решения. Когда типы перемешаны, каждый раз приходится сначала понять, что это за задача. Это тяжелее и кажется медленнее, но именно так учишься различать — а на экзамене и в жизни задачи не приходят по порядку.",
                    cues = "Почему блоки одного типа обманывают?\nЧему учит перемешивание?",
                    gold = "Трудность при учёбе — не помеха, а признак, что учишься.",
                    s = 4.0, since = 9.5, age = 40.0, reps = 2,
                    glosses = listOf(
                        GlossKind.PARABLE to ("" to "Как тренировка в теннисе: сто подач в одно место — красиво, но в игре мяч летит куда угодно."),
                        GlossKind.DISSENT to ("" to "Для самого первого знакомства с темой блоки всё-таки нужны — иначе просто тонешь."),
                    ),
                    reviews = listOf(R(30.0, Mode.RECALL, 2, 3, 0.55f, "Не решать задачи одного типа подряд. Перемешивать."), R(14.0, Mode.CUES, 1, 2)),
                ),
                P(
                    "Иллюзия знания", "Глава 5",
                    "Мы плохо судим о том, что знаем. Беглость — когда текст читается легко — создаёт чувство понимания. Проверить можно только одним способом: попробовать объяснить или ответить без подсказки. Ошибка, в которой ты был уверен, после исправления запоминается особенно крепко.",
                    cues = "Откуда берётся чувство, что понял?\nКак его проверить?",
                    gold = "Лёгкость чтения — не то же самое, что знание.",
                    s = 2.2, since = 6.0, age = 30.0, reps = 1, lapses = 1, erratum = true,
                    reviews = listOf(R(25.0, Mode.RECALL, 2, 3, 0.6f, "Беглость создаёт иллюзию понимания."), R(6.0, Mode.LACUNAE, 3, 1)),
                ),
                P(
                    "Желательные трудности", "Глава 4",
                    "Учёба, которая даётся легко, часто поверхностна. Интервалы, перемешивание, попытка ответить до того, как узнал ответ, — всё это замедляет и раздражает, но знания держатся дольше. Хорошая трудность та, которую можно преодолеть усилием.",
                    cues = "Какие трудности полезны?\nКогда трудность вредна?",
                    s = 6.0, since = 13.0, age = 26.0, reps = 1,
                    reviews = listOf(R(13.0, Mode.RECALL, 1, 2, 0.41f, "Трудности помогают учиться. Интервалы.")),
                ),
            ),
        ),
        C(
            Kind.BOOK, "Размышления", "Марк Аврелий", 3, 120.0,
            preface = "Ожидаю сборник мудрых цитат, немного скучный.",
            sentence = "Ты не управляешь событиями, но управляешь тем, что о них думаешь, — и в этом вся свобода.",
            words = "суждение, порядок, долг",
            surprise = "Это не наставления другим, а дневник человека, который уговаривает сам себя. Поэтому так живо.",
            closedAgo = 70.0,
            pages = listOf(
                P(
                    "Нас тревожат не вещи", "Книга 8",
                    "Боль причиняет не само событие, а суждение о нём. Суждение можно изменить, и это в моей власти. Если что-то раздражает — сначала отдели факт от того, что я о нём себе сказал.",
                    cues = "Что на самом деле причиняет боль?\nЧто в моей власти?",
                    gold = "Отдели событие от суждения о нём.",
                    s = 95.0, since = 20.0, age = 110.0, reps = 5,
                    glosses = listOf(GlossKind.SEAL to ("" to "Застрял в пробке и опаздывал. Сказал себе: факт — пробка, остальное — мой рассказ. Злость ушла за минуту.")),
                    reviews = listOf(R(80.0, Mode.RECALL, 3, 3, 0.8f), R(45.0, Mode.CUES, 3, 4), R(20.0, Mode.LACUNAE, 3, 4)),
                ),
                P(
                    "Утро", "Книга 2",
                    "С утра скажи себе: сегодня встречу неблагодарных, наглых, завистливых. Они такие не по злобе, а по незнанию. Меня они не могут повредить, если я сам не позволю.",
                    s = 40.0, since = 30.0, age = 115.0, reps = 3,
                    reviews = listOf(R(30.0, Mode.RECALL, 2, 3, 0.7f)),
                ),
            ),
        ),
        C(
            Kind.LECTURE, "Как мозг забывает", "открытая лекция, канал «Наука памяти»", 1, 2.0,
            preface = "Знаю про кривую Эббингауза, но не знаю, почему она такая.",
            questions = "Почему забывание такое резкое в первые дни?",
            pages = listOf(
                P(
                    "Кривая забывания", "08:30",
                    "Эббингауз учил бессмысленные слоги и проверял, сколько помнит через часы и дни. Больше всего уходит в первые сутки, потом забывание замедляется. Каждое повторение делает кривую положе, поэтому интервалы можно растягивать: день, три, неделя, месяц.",
                    cues = "Когда забываем быстрее всего?\nЧто делает повторение с кривой?",
                    gold = "Каждое повторение делает кривую положе.",
                    s = 1.0, since = 1.0, age = 1.0,
                ),
                P(
                    "Сон и память", "31:10",
                    "Во сне мозг перебирает то, что было днём, и переносит важное в долговременную память. Поэтому лучше повторить перед сном, а утром проверить себя.",
                    cues = "Что делает мозг ночью?",
                    s = 1.0, since = 1.0, age = 1.0,
                ),
            ),
        ),
        C(
            Kind.BOOK, "Думай медленно… решай быстро", "Даниэль Канеман", 4, 18.0,
            preface = "Слышал про две системы мышления. Жду, что объяснят, как не попадаться на ошибки.",
            questions = "Можно ли натренировать Систему 2?",
            pages = listOf(
                P(
                    "Две системы", "Глава 1",
                    "Система 1 работает быстро и сама: узнаёт лица, достраивает фразы, чует опасность. Система 2 медленная и ленивая, ей нужно усилие — посчитать в уме, сравнить варианты. Большую часть времени мы живём на первой, а вторая лишь одобряет её решения.",
                    cues = "Чем различаются две системы?\nПочему вторая ленива?",
                    gold = "Мы думаем, что решаем медленно, а решаем быстро.",
                    s = 3.0, since = 7.0, age = 17.0, reps = 1,
                    glosses = listOf(GlossKind.NOVICE to ("Как объяснить это десятилетнему?" to "В голове живут два помощника: быстрый угадывает сразу, а медленный проверяет, но ему лень вставать с дивана.")),
                    reviews = listOf(R(10.0, Mode.RECALL, 3, 3, 0.7f, "Система 1 быстрая и автоматическая, Система 2 медленная, требует усилия.")),
                ),
                P(
                    "Якорение", "Глава 11",
                    "Первое число, которое слышишь, тянет к себе оценку, даже если оно случайно. Продавцы называют высокую цену первой. Защита — нарочно подумать о доводах против якоря.",
                    cues = "Как работает якорь?\nКак защититься?",
                    s = 1.5, since = 6.0, age = 9.0, reps = 0,
                ),
            ),
        ),
    )

    private fun english() = listOf(
        C(
            Kind.BOOK, "Make It Stick", "P. Brown, H. Roediger, M. McDaniel", 0, 64.0,
            preface = "I expect memory tricks and the advice to review more. Probably that good notes help.",
            questions = "Why doesn't rereading work?\nHow do I study so it's not gone in a week?\nWhat to do about feeling I understood it all?",
            answered = 0b011,
            pages = listOf(
                P(
                    "Recall beats rereading", "Chapter 2",
                    "When you reread, the text feels familiar, and that feeling is easy to mistake for knowing. What strengthens memory is the effort of pulling something out of your head. Each time you recall, the path to the idea is trodden again and grows wider. So a test isn't a check, it's the learning itself. Better to close the book and retell it than to read the chapter twice.",
                    cues = "Why does rereading fool us?\nWhat actually strengthens memory?\nWhy test before you've learned it?",
                    gold = "Recalling is the learning.",
                    colophon = "Close the book and retell it: worth three rereadings.",
                    s = 210.0, since = 12.0, age = 63.0, reps = 6,
                    glosses = listOf(
                        GlossKind.NOVICE to ("Can you give an example from life?" to "Before my driving test I read the questions a hundred times and still mixed them up. What helped was a friend quizzing me out loud."),
                        GlossKind.EXEMPLUM to ("" to "Learned Spanish words with flashcards in a week; the list I only reread was gone in two days."),
                        GlossKind.EXEMPLUM to ("" to "When I retell an article to a colleague, I remember nearly all of it the next morning."),
                        GlossKind.SEAL to ("" to "For two weeks I wrote a page from memory after every lecture of the course. Final test: 92%, up from about 70."),
                    ),
                    reviews = listOf(
                        R(60.0, Mode.RECALL, 2, 3, 0.62f, "Rereading is useless, you need to recall. Recall strengthens memory, familiarity fools you."),
                        R(52.0, Mode.CUES, 2, 3), R(40.0, Mode.LACUNAE, 3, 4), R(28.0, Mode.RECALL, 3, 4, 0.88f, "Familiar isn't learned. The effort of recall treads the path. A test is learning, not a check. Retell rather than reread."),
                        R(12.0, Mode.CUES, 3, 4),
                    ),
                ),
                P(
                    "Mix the topics", "Chapter 3",
                    "Solve twenty problems of one kind in a row and by the fifth you're on autopilot, no longer choosing how to solve them. When the kinds are mixed, each time you first have to work out what sort of problem it is. It's harder and feels slower, but that's how you learn to tell them apart, and in exams and in life problems don't come in order.",
                    cues = "Why do blocks of one kind fool us?\nWhat does mixing teach?",
                    gold = "Difficulty while learning isn't in the way: it's the sign you're learning.",
                    s = 4.0, since = 9.5, age = 40.0, reps = 2,
                    glosses = listOf(
                        GlossKind.PARABLE to ("" to "Like tennis practice: a hundred serves to one spot look great, but in a match the ball comes from anywhere."),
                        GlossKind.DISSENT to ("" to "For a very first look at a topic you do need blocks, or you just drown."),
                    ),
                    reviews = listOf(R(30.0, Mode.RECALL, 2, 3, 0.55f, "Don't do problems of one type in a row. Mix them."), R(14.0, Mode.CUES, 1, 2)),
                ),
                P(
                    "The illusion of knowing", "Chapter 5",
                    "We judge badly what we know. Fluency, when a text reads easily, gives a feeling of understanding. The only check is to try to explain or answer without help. A mistake you were sure of is remembered especially well once corrected.",
                    cues = "Where does the feeling of understanding come from?\nHow do you test it?",
                    gold = "Reading easily is not the same as knowing.",
                    s = 2.2, since = 6.0, age = 30.0, reps = 1, lapses = 1, erratum = true,
                    reviews = listOf(R(25.0, Mode.RECALL, 2, 3, 0.6f, "Fluency creates an illusion of understanding."), R(6.0, Mode.LACUNAE, 3, 1)),
                ),
                P(
                    "Desirable difficulties", "Chapter 4",
                    "Learning that comes easily is often shallow. Spacing, mixing, trying to answer before you know the answer: all of it slows you down and annoys, but the knowledge lasts longer. A good difficulty is one you can overcome with effort.",
                    cues = "Which difficulties help?\nWhen does difficulty hurt?",
                    s = 6.0, since = 13.0, age = 26.0, reps = 1,
                    reviews = listOf(R(13.0, Mode.RECALL, 1, 2, 0.41f, "Difficulties help learning. Spacing.")),
                ),
            ),
        ),
        C(
            Kind.BOOK, "Meditations", "Marcus Aurelius", 3, 120.0,
            preface = "I expect a collection of wise quotes, a bit dull.",
            sentence = "You don't control what happens, but you control what you make of it, and that is all of freedom.",
            words = "judgement, order, duty",
            surprise = "It isn't advice to others but the diary of a man talking himself round. That's why it's so alive.",
            closedAgo = 70.0,
            pages = listOf(
                P(
                    "It isn't things that trouble us", "Book 8",
                    "What hurts is not the event but my judgement of it. The judgement can be changed, and that is in my power. When something annoys me, first separate the fact from what I've told myself about it.",
                    cues = "What really causes the pain?\nWhat is in my power?",
                    gold = "Separate the event from the judgement of it.",
                    s = 95.0, since = 20.0, age = 110.0, reps = 5,
                    glosses = listOf(GlossKind.SEAL to ("" to "Stuck in traffic and running late. Told myself: the fact is traffic, the rest is my story. The anger was gone in a minute.")),
                    reviews = listOf(R(80.0, Mode.RECALL, 3, 3, 0.8f), R(45.0, Mode.CUES, 3, 4), R(20.0, Mode.LACUNAE, 3, 4)),
                ),
                P(
                    "Morning", "Book 2",
                    "Say to yourself in the morning: today I'll meet the ungrateful, the rude, the envious. They are so not from malice but from ignorance. They can't harm me unless I let them.",
                    s = 40.0, since = 30.0, age = 115.0, reps = 3,
                    reviews = listOf(R(30.0, Mode.RECALL, 2, 3, 0.7f)),
                ),
            ),
        ),
        C(
            Kind.LECTURE, "How the Brain Forgets", "open lecture, Memory Science channel", 1, 2.0,
            preface = "I know about the Ebbinghaus curve but not why it looks like that.",
            questions = "Why is forgetting so steep in the first days?",
            pages = listOf(
                P(
                    "The forgetting curve", "08:30",
                    "Ebbinghaus learned nonsense syllables and tested how many he kept after hours and days. Most goes in the first day, then forgetting slows. Each review makes the curve flatter, so the gaps can stretch: a day, three, a week, a month.",
                    cues = "When do we forget fastest?\nWhat does a review do to the curve?",
                    gold = "Each review flattens the curve.",
                    s = 1.0, since = 1.0, age = 1.0,
                ),
                P(
                    "Sleep and memory", "31:10",
                    "In sleep the brain goes over the day and moves what matters into long-term memory. So review before sleep, and test yourself in the morning.",
                    cues = "What does the brain do at night?",
                    s = 1.0, since = 1.0, age = 1.0,
                ),
            ),
        ),
        C(
            Kind.BOOK, "Thinking, Fast and Slow", "Daniel Kahneman", 4, 18.0,
            preface = "I've heard of the two systems. I hope it explains how not to fall for the errors.",
            questions = "Can System 2 be trained?",
            pages = listOf(
                P(
                    "Two systems", "Chapter 1",
                    "System 1 works fast and on its own: it knows faces, finishes sentences, senses danger. System 2 is slow and lazy and needs effort: mental arithmetic, weighing options. Most of the time we live on the first, and the second merely signs off on its decisions.",
                    cues = "How do the two systems differ?\nWhy is the second one lazy?",
                    gold = "We think we decide slowly; we decide fast.",
                    s = 3.0, since = 7.0, age = 17.0, reps = 1,
                    glosses = listOf(GlossKind.NOVICE to ("How would you explain it to a child of ten?" to "Two helpers live in your head: the quick one guesses at once, the slow one checks, but he can't be bothered to get off the sofa.")),
                    reviews = listOf(R(10.0, Mode.RECALL, 3, 3, 0.7f, "System 1 is fast and automatic, System 2 slow and effortful.")),
                ),
                P(
                    "Anchoring", "Chapter 11",
                    "The first number you hear pulls your estimate towards it, even when it's random. Sellers name a high price first. The defence is to think on purpose of reasons against the anchor.",
                    cues = "How does an anchor work?\nHow to defend against it?",
                    s = 1.5, since = 6.0, age = 9.0, reps = 0,
                ),
            ),
        ),
    )
}
