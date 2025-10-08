## Assuming you've put train.csv dataset into hdfs

### 1. Now in your container root, create Q4 folder
```sh
mkdir Q4
```

### 2. Go inside your local Q4 directory and copy all files to the namenode container's Q4 folder
```sh
cd /Q4
# Running the MR job a single time
docker cp NightlifeSpotsByMonth.java namenode:/Q4/NightlifeSpotsByMonth.java &&
docker cp TopKNightlifeSpots.java namenode:/Q4/TopKNightlifeSpots.java &&
docker cp run_q4_pipeline.sh namenode:/Q4/run_q4_pipeline.sh
# Running experiments for different number of reducers and slow start parameters
docker cp run_experiments.sh namenode:/Q4/run_experiments.sh
```

### 3. Compile the Java files using Hadoop’s classpath
```sh
javac -classpath "$(hadoop classpath)" NightlifeSpotsByMonth.java
javac -classpath "$(hadoop classpath)" TopKNightlifeSpots.java
```

### 4. Create the JAR files

```sh
jar cf NightlifeSpotsByMonth.jar NightlifeSpotsByMonth*.class
jar cf TopKNightlifeSpots.jar TopKNightlifeSpots*.class
```

### 5. Run the MapReduce Job
```sh
./run_q4_pipeline.sh
```

### 6. Run the experiments (this will give output 2 csv files that you need to copy back to local Q4 folder for plots)
```sh
./run_experiments.sh
```

### 7. Copy the csv files back to your local Q4 folder
```sh 
docker cp namenode:/Q4/reducer_experiment_results.csv ./Q4/ &&
docker cp namenode:/Q4/slowstart_experiment_results.csv ./Q4/
```

### 8. Visualize the experiment results
```sh
cd Q4
python plot_experiments.py
```