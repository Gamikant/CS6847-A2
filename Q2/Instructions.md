## Assuming you've put train.csv dataset into hdfs

### 1. Now in your container root, create Q2 folder
```sh
mkdir Q2
```

### 2. Go inside your local Q2 directory and copy all files to the namenode container's Q2 folder
```sh
cd /Q2
# Running the MR job a single time
docker cp ExpensiveRoutesByMonth.java namenode:/Q2/ExpensiveRoutesByMonth.java &&
docker cp TopKExpensiveRoutes.java namenode:/Q2/TopKExpensiveRoutes.java &&
docker cp run_q2_pipeline.sh namenode:/Q2/run_q2_pipeline.sh
# Running experiments for different number of reducers and slow start parameters
docker cp run_experiments.sh namenode:/Q2/run_experiments.sh
```

### 3. Compile the Java files using Hadoop’s classpath
```sh
javac -classpath "$(hadoop classpath)" ExpensiveRoutesByMonth.java
javac -classpath "$(hadoop classpath)" TopKExpensiveRoutes.java
```

### 4. Create the JAR files

```sh
jar cf ExpensiveRoutesByMonth.jar ExpensiveRoutesByMonth*.class
jar cf TopKExpensiveRoutes.jar TopKExpensiveRoutes*.class
```

### 5. Run the MapReduce Job
```sh
./run_q2_pipeline.sh
```

### 6. Run the experiments (this will give output 2 csv files that you need to copy back to local Q2 folder for plots)
```sh
./run_experiments.sh
```

### 7. Copy the csv files back to your local Q2 folder
```sh 
docker cp namenode:/Q2/reducer_experiment_results.csv ./Q2/ &&
docker cp namenode:/Q2/slowstart_experiment_results.csv ./Q2/
```

### 8. Visualize the experiment results
```sh
cd Q2
python plot_experiments.py
```