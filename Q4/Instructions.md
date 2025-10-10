## Assuming you've put train.csv dataset into hdfs

1. Using your container shell, create Q4 folder and go inside it:
```sh
mkdir Q4
cd Q4
```
2. Go inside your repo directory and open terminal:
```sh
cd Q4
# Copy all mapreduce and shell scripts to the namenode container's Q4 folder
docker cp NightLifeSpotsByMonth.java namenode:/Q4/NightLifeSpotsByMonth.java &&
docker cp TopKNightLifeSpots.java namenode:/Q4/TopKNightLifeSpots.java &&
docker cp run_Q4_pipeline.sh namenode:/Q4/run_Q4_pipeline.sh
docker cp run_experiments.sh namenode:/Q4/run_experiments.sh
```

3. Using namenode shell, compile the Java files using Hadoop’s classpath
```sh
javac -classpath "$(hadoop classpath)" NightLifeSpotsByMonth.java
javac -classpath "$(hadoop classpath)" TopKNightLifeSpots.java
```

4. Create the JAR files
```sh
jar cf NightLifeSpotsByMonth.jar NightLifeSpotsByMonth*.class
jar cf TopKNightLifeSpots.jar TopKNightLifeSpots*.class
```

5. Run the MapReduce Job
```sh
./run_Q4_pipeline.sh
```
6. Run the experiments (this will output 2 csv files that you need to copy back to repo's Q4 folder for plots)
```sh
./run_experiments.sh
```
7. Copy the csv files generated back to your repo's Q4 folder
```sh
cd Q4
docker cp namenode:/Q4/reducer_experiment_results.csv ./Q4/ &&
docker cp namenode:/Q4/slowstart_experiment_results.csv ./Q4/
```
8. Visualize the experiment results
```sh
cd Q4
python plot_experiments.py
```