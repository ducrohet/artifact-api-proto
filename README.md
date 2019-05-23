Small prototype of an API allowing people to manipulate build intermediates and outputs as artifacts objects rather than dealing with the outputs and inputs of tasks.

This allows users of the API to read, append and transform artifacts in a way that makes it work if multiple API users (e.g. plugins or code in build.gradle) where to use the API in random order.

The project is organized the following way:
- `buildSrc/api`: the exposed API. This include the public plugin that users would apply.
- `buildSrc/core-plugin`: the implementation plugin. Depends on `api`.
- `buildSrc/third-party-plugin`: a 3rd party plugin, depends on `api` and uses the api.
- main project contains just a build file that applies the public plugin and the 3rd party one.

To test, run the `assemble` task. This will run a bunch of tasks, each printing out their inputs/outputs.

You will witness a few android-like intermediate tasks (compileCode, dexer, manifest merger) that outputs in the intermediates folder, as well as a final package task that writes the output in the outputs folder.

Rerun the same `assemble` task with `clean` and `-Pandroid.transform=true/false`, to have the 3rd party plugin insert new tasks in the mix.
Notice all the transforms tasks inject themselves between the original producer and consumer.
Notice how the original package task now output in the intermediates folder since another task transforms its output and generates the final package.
