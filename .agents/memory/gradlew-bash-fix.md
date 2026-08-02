---
name: gradlew bash fix
description: gradlew shebang and xargs process substitution issues on Ubuntu CI runners
---

# gradlew bash fix

## Rule
Always change `#!/bin/sh` to `#!/bin/bash` in gradlew when the script uses process substitution `<(...)`. Also replace the `exec xargs -a <(...)` block with a bash eval/array pattern.

## Why
Ubuntu CI runners use `dash` as `/bin/sh`, which does not support process substitution `<(...)`. Additionally, the `xargs` + `sed` escaping in newer Gradle wrapper scripts adds backslashes to JVM opts like `-Xmx64m`, causing Gradle to treat them as task names instead of JVM arguments.

## How to apply
1. Change shebang: `#!/bin/sh` → `#!/bin/bash`
2. Replace the entire xargs block with:
```bash
eval "JVM_OPTS_ARRAY=($DEFAULT_JVM_OPTS)"
exec "$JAVACMD" "${JVM_OPTS_ARRAY[@]}" $JAVA_OPTS $GRADLE_OPTS "$@"
```
This correctly parses quoted JVM opts into a bash array and executes Java directly.
