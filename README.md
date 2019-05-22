Small prototype of an API allowing people to manipulate build intermediates and outputs as artifacts objects rather than dealing with the outputs and inputs of tasks.

This allows users of the API to read, append and transform artifacts in a way that makes it work if multiple API users (e.g. plugins or code in build.gradle) where to use the API in random order.

To test, run the `assemble` task.

You will witness a few android-like intermediate tasks (compileCode, dexer, manifest merger) that outputs in the intermediate folders, as well as a final package task that writes the output in the outputs folder.

Rerun the same `assemble` task with `clean` and `-Pandroid.transform=true/false`, to have the 3rd party plugin insert new tasks in the mix.
Notice all the transform tasks inject themselves between the original producer and consumer.
Notice how the origina package task now output in the intermediates folder since another task transforms its output.
