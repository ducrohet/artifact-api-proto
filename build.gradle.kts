plugins {
  id("custom.plugin")
}

tasks.register("clean") {
    doLast {
        project.buildDir.deleteRecursively()
    }
}

