## Assuming you've put train.csv dataset into hdfs

1. Using your container shell, create Q1 folder and go inside it:
```sh
mkdir Q1
cd Q1
```
2. Go inside your repo directory and open terminal:
```sh
cd Q1
# Copy all mapreduce and shell scripts to the namenode container's Q1 folder
docker cp PopularRoutesByMonth.java namenode:/Q1/PopularRoutesByMonth.java &&
docker cp TopKRoutes.java namenode:/Q1/TopKRoutes.java &&
docker cp run_q1_pipeline.sh namenode:/Q1/run_q1_pipeline.sh
docker cp run_experiments.sh namenode:/Q1/run_experiments.sh
```

3. Using namenode shell, compile the Java files using Hadoop’s classpath
```sh
javac -classpath "$(hadoop classpath)" PopularRoutesByMonth.java
javac -classpath "$(hadoop classpath)" TopKRoutes.java
```

4. Create the JAR files
```sh
jar cf PopularRoutesByMonth.jar PopularRoutesByMonth*.class
jar cf TopKRoutes.jar TopKRoutes*.class
```

5. Run the MapReduce Job
```sh
./run_q1_pipeline.sh
```
6. Run the experiments (this will output 2 csv files that you need to copy back to repo's Q1 folder for plots)
```sh
./run_experiments.sh
```
7. Copy the csv files generated back to your repo's Q1 folder
```sh
cd Q1
docker cp namenode:/Q1/reducer_experiment_results.csv ./Q1/ &&
docker cp namenode:/Q1/slowstart_experiment_results.csv ./Q1/
```
8. Visualize the experiment results
```sh
cd Q1
python plot_experiments.py
```