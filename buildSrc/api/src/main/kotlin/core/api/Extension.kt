package core.api

interface Extension {

    /**
     * Allows appending to, transforming, and consuming build artifact.
     *
     * This runs before the Android tasks are actually created, simulating what we expect our new variant API
     * will do.
     */
    fun handleArtifacts(block: (ArtifactHolder) -> Unit)
}