package eu.kanade.tachiyomi.ui.reader.model

/**
 * Core component for InsertPage within the Animetail presentation and UI layer.
 */
class InsertPage(val parent: ReaderPage) : ReaderPage(parent.index, parent.url, parent.imageUrl) {

    override var chapter: ReaderChapter = parent.chapter

    init {
        status = State.READY
        stream = parent.stream
    }
}
