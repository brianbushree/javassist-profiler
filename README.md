# javassist-profiler
Profiler for Java projects that tracks method duration and the creation of new Threads.

Usage:
```
java -javaagent:<AGENTJAR_PATH>=<OUT_DIR><PACKAGE1>[,<PACKAGE2>, ... ] -jar <PROGRAM_JAR>
```

**For example:**
```
java -javaagent:agent/target/agent-0.1-SNAPSHOT.jar=test -jar test/target/test-0.1-SNAPSHOT.jar
```
