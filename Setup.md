### Follow these instructions for the initial setup
1. Clone this repo - `https://github.com/Gamikant/CS6847-A2`
3. Open docker desktop
3. After cloning the repo, run these commands in your terminal:
    ```sh
    cd CS6847-A2
    # Running hadoop using docker compose stack
    docker-compose up -d
    ```
4. Wait for a while for all containers in the docker hadoop compose stack to start up.
5. Get the IPs of all containers running (namenode, datanode, resourcemanager, nodemanager, historyserver)
    ```sh
    docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' <container_name>
    ```
5. Go inside the folder where `train.csv` (55M rows, ~5.3GB) lies and run this in the terminal:
    ```sh
    # Copying the input data to the namenode container
    docker cp train.csv namenode:/tmp/train.csv
    ```
6. Open namenode container shell by running this in the terminal:
    ```sh
    docker exec -it namenode bash
    ```
6. Using the namenode shell, put the input data into HDFS
    ```sh
    hdfs dfs -mkdir -p /user/root/Data
    hdfs dfs -put /tmp/train.csv /user/root/Data/train.csv
    ```
7. Keep this namenode shell open for running all 4 queries. 
8. Now go to any query folder inside the repo you cloned (e.g. Q1)
***
This folder contains all necessary files required for your to perform a Map Reduce job for the that query of the assignment. You just need to follow the instructions provided by the `Instructions.md` tailored for that query folder.
***
### Some spare commands you might need once in a while
***
1. To delete an MR job output
    ```sh
    hdfs dfs -rm -r /user/root/output_directory_name
    ```

2. To disable namenode's safe mode
    ```sh
    hdfs dfsadmin -safemode leave
    ```

3. File size of MR job's outputs
    ```sh
    hdfs dfs -du -h /user/root/output_directory_name
    ```
***
