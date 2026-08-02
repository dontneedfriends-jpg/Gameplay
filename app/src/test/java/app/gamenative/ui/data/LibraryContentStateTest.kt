package app.gamenative.ui.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryContentStateTest {
    @Test
    fun `prioritizes loading and error before empty states`() {
        assertEquals(
            LibraryContentState.LOADING,
            libraryContentState(isLoading = true, hasItems = false, loadError = false, isSearching = false),
        )
        assertEquals(
            LibraryContentState.ERROR,
            libraryContentState(isLoading = false, hasItems = false, loadError = true, isSearching = false),
        )
        assertEquals(
            LibraryContentState.SEARCH_NO_RESULTS,
            libraryContentState(isLoading = false, hasItems = false, loadError = false, isSearching = true),
        )
        assertEquals(
            LibraryContentState.NO_RESULTS,
            libraryContentState(isLoading = false, hasItems = false, loadError = false, isSearching = false),
        )
        assertEquals(
            LibraryContentState.CONTENT,
            libraryContentState(isLoading = false, hasItems = true, loadError = false, isSearching = false),
        )
    }
}
