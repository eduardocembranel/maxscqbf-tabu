## Compile (Linux or Mac)

```
javac -d out $(find src -name "*.java")
```

## Run

```
java -cp out Main <instance_name> <method>
```

####  Run example for the instance *exact_n25* with method *std*
```
java -cp out Main exact_n25 std
```

#### Available methods are:
* std
* std+t2
* std+best
* std+div
* std+int

## See the results

The result output will be available in the file `./results/<method>/<instance_name>.txt`
