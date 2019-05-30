Small prototype of an API allowing plugin and build files to manipulate build intermediates and outputs as artifact objects rather than directly via task inputs and outputs (as we consider these to not be stable APIs.)

This also allows users of the API to read, append to and transform artifacts in a way that works even if multiple API users (e.g. different plugins or customizations in build.gradle) were to use the API without knowing, or taking the others into account.

The project is organized the following way:
- `buildSrc/api`: the exposed API. This includes the public plugin that users would apply.
- `buildSrc/core-plugin`: the implementation plugin. Depends on `api`.
- `buildSrc/third-party-plugin`: a 3rd party plugin, depends on `api` and uses the api.
- main project contains just a build file that applies the public plugin and the 3rd party one.

To see what happens, run the `assemble` task. This will run a bunch of tasks, each printing out their inputs and outputs.

You will notice a few android-like tasks (compileCode, dexer, manifest merger) that output in the `intermediates` folder, as well as a final packaging task that writes the output in the `outputs` folder.

Running the same `assemble` task with `clean` and `-Ptransform.[resources|manifest|code|package|all]=true` will show what happens when the 3rd party plugin inserts new tasks in the mix, using the API.

You can also use `-Preplace.dexer=true` to see what happens when a task fully replaces an existing task.

Notice the transforms tasks injecting themselves between the original producer and consumer tasks.
Notice how the original packaging task now outputs in the `intermediates` folder since another task transforms its output and generates the final package (in the `outputs` folder.)
