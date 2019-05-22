package core.api

interface Extension {

    fun handleArtifacts(block: (ArtifactHolder) -> Unit)
}