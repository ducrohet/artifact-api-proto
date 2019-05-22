package core.internal.impl

import core.api.ArtifactHolder
import core.api.Extension

open class ExtensionImpl: Extension {

    internal var block: ((ArtifactHolder) -> Unit)? = null
        private set

    override fun handleArtifacts(block: (ArtifactHolder) -> Unit) {
        this.block = block
    }
}