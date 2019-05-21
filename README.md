Small prototype of an API allowing people to manipulate build intermediates and outputs as artifacts objects rather than dealing with the outputs and inputs of tasks.

This allows users of the API to read, append and transform artifacts in a way that makes it work if multiple API users (e.g. plugins or code in build.gradle) where to use the API in random order.

To test, run one of the following tasks:
- finalFile
- finalDir
- finalFileList

with or without `-Pandroid.transform=true/false`.

e.g.:
	`./gradlew -Pandroid.transform=true finalDir --rerun-tasks`
