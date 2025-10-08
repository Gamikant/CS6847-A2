Here are the concise steps to compile your Java MapReduce code into a JAR inside the namenode container:

***

### 1. Copy the Java file to the namenode container

From your project directory on your laptop:
```sh
docker cp example_task.java namenode:/example_task.java
```

***

### 2. Open a shell inside the namenode container

```sh
docker exec -it namenode bash
```

***

### 3. Compile the Java file using Hadoop’s classpath

```sh
javac -classpath "$(hadoop classpath)" example_task.java
```

***

### 4. Create the JAR file

```sh
jar cf example_task.jar example_task*.class
```

### 5. Run the MapReduce Job
```sh
hadoop jar example_task.jar example_task /user/root/Data/train.csv /user/root/output_example_task
```

### 6. To delete an output
```sh
hdfs dfs -rm -r /user/root/output_popular_routes
```

### 7. To disable namenode's safe mode
```sh
hdfs dfsadmin -safemode leave
```

### 8. File size of outputs
```sh
hdfs dfs -du -h /user/root/output_example_task
```

### 9. To get IPs of each node
```sh
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' <container_name>
```

### 10. To make a directory in namenode hdfs dfs
```sh
hdfs dfs -mkdir -p /user/root/Data
```

### 11. Put the file into HDFS
```sh
hdfs dfs -put /tmp/train.csv /user/root/Data/train.csv
```
***