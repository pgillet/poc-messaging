Change any runtime configuration parameters in `src/main/resources/perf.properties` that are suitable for your test.

Apply the changes with:
```
mvn clean compile
```

To run the producer side:

```
mvn exec:exec -Psender
```

To run the consumer side:

```
mvn exec:exec -Plistener
```

