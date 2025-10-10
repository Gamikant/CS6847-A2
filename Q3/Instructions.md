## Assuming you've put train.csv dataset into hdfs

1. Using your container shell, create Q3 folder and go inside it:
```sh
mkdir Q3
cd Q3
```
2. Go inside your repo directory and open terminal:
```sh
cd Q3
# Copy all mapreduce and shell scripts to the namenode container's Q3 folder
docker cp PopularLocationsByMonth.java namenode:/Q3/PopularLocationsByMonth.java &&
docker cp TopKLocations.java namenode:/Q3/TopKLocations.java &&
docker cp run_Q3_pipeline.sh namenode:/Q3/run_Q3_pipeline.sh
docker cp run_experiments.sh namenode:/Q3/run_experiments.sh
```

3. Using namenode shell, compile the Java files using Hadoop’s classpath
```sh
javac -classpath "$(hadoop classpath)" PopularLocationsByMonth.java
javac -classpath "$(hadoop classpath)" TopKLocations.java
```

4. Create the JAR files
```sh
jar cf PopularLocationsByMonth.jar PopularLocationsByMonth*.class
jar cf TopKLocations.jar TopKLocations*.class
```

5. Run the MapReduce Job
```sh
./run_Q3_pipeline.sh
```
6. Run the experiments (this will output 2 csv files that you need to copy back to repo's Q3 folder for plots)
```sh
./run_experiments.sh
```
7. Copy the csv files generated back to your repo's Q3 folder
```sh
cd Q3
docker cp namenode:/Q3/reducer_experiment_results.csv ./Q3/ &&
docker cp namenode:/Q3/slowstart_experiment_results.csv ./Q3/
```
8. Visualize the experiment results
```sh
cd Q3
python plot_experiments.py
```