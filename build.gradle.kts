plugins {
    id("third.party.plugin")
    id("core.api")
}

tasks.register("clean") {
    doLast {
        project.buildDir.deleteRecursively()
    }
}

