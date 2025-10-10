# CS6847 Cloud Computing Assignment 2
## MapReduce Analysis on NYC Taxi Dataset

[![Hadoop](https://img.shields.io/badge/Hadoop-3.2.1-yellow)](https://hadoop.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue)](https://www.docker.com/)
[![Java](https://img.shields.io/badge/Java-8-orange)](https://www.oracle.com/java/)
[![Python](https://img.shields.io/badge/Python-3.8+-green)](https://www.python.org/)

**Author:** Pranav Yadav  
**Institute:** IIT Madras  
**Course:** CS6847 - Cloud Computing  
**Repository:** [https://github.com/Gamikant/CS6847-A2](https://github.com/Gamikant/CS6847-A2)

---

## 📋 Table of Contents

- [Overview](#overview)
- [Problem Statement](#problem-statement)
- [Dataset](#dataset)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
- [Query Implementations](#query-implementations)
- [Performance Experiments](#performance-experiments)
- [Results & Analysis](#results--analysis)
- [Technologies Used](#technologies-used)
- [Acknowledgments](#acknowledgments)

---

## 🎯 Overview

This project implements a comprehensive MapReduce-based analysis of the NYC Taxi Trip Fare dataset using Apache Hadoop. The implementation runs on a multi-node Hadoop cluster deployed using Docker containers, analyzing 55+ million taxi trip records (~5.3 GB) to extract meaningful insights about taxi usage patterns in New York City.

The project demonstrates:
- **Distributed data processing** using MapReduce paradigm
- **Performance tuning** through experimentation with Hadoop parameters
- **Scalability analysis** by varying cluster configurations
- **Data quality handling** with coordinate validation and outlier filtering

---

## 📝 Problem Statement

Analyze the NYC Taxi dataset to find:

1. **Query 1 (Q1):** Top 5 most popular routes in 2013
2. **Query 2 (Q2):** Top 5 most expensive routes in 2013
3. **Query 3 (Q3):** Top 5 most visited pickup and dropoff locations in 2013
4. **Query 4 (Q4):** Top 5 most popular nightlife spots (8 PM - 2 AM dropoffs) in 2013

### Evaluation Criteria

For each query:
- Generate monthly partitioned outputs (12 files, one per month)
- Aggregate results across all months
- Perform experiments varying:
  - Number of reducers (6, 8, 12, 16)
  - Slow start parameter (0.25, 0.50, 0.75, 0.95)
- Generate performance plots and analyze execution behavior

---

## 📊 Dataset

**Source:** [NYC Taxi Fare Data](https://www.kaggle.com/c/new-york-city-taxi-fare-prediction/data)

**Specifications:**
- **Size:** ~5.3 GB
- **Records:** 55,423,857 rows
- **Format:** CSV
- **Year Range:** 2009-2015 (filtered to 2013 for analysis)

**Schema:**
```
key,fare_amount,pickup_datetime,pickup_longitude,pickup_latitude,dropoff_longitude,dropoff_latitude,passenger_count
```

**Sample Record:**
```
2013-06-15 17:26:21.0000001,4.5,2013-06-15 17:26:21 UTC,-73.844311,40.721319,-73.84161,40.712278,1
```

**Data Quality Measures:**
- Filtered invalid coordinates (0.0 values)
- Validated fare amounts (0 < fare ≤ $2000)
- Used 4/5 decimal precision for location coordinates (~11-110 meter accuracy)

---

## 🏗️ Architecture

### Cluster Architecture

The project uses a **3-node Hadoop cluster** deployed via Docker Compose:

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Network                          │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐   │
│  │   NameNode   │  │  DataNode    │  │ ResourceManager  │   │
│  │              │  │              │  │                  │   │
│  │  - HDFS      │  │  - Storage   │  │  - YARN          │   │
│  │  - Port 9870 │  │  - Port 9864 │  │  - Port 8088     │   │
│  └──────────────┘  └──────────────┘  └──────────────────┘   │
│                                                             │
│  ┌──────────────┐  ┌──────────────────────────────────────┐ │
│  │ NodeManager  │  │   History Server                     │ │
│  │              │  │                                      │ │
│  │  - Port 8042 │  │   - Port 8188                        │ │
│  └──────────────┘  └──────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### MapReduce Job Architecture

Each query follows a **two-stage pipeline**:

```
Stage 1: Monthly Aggregation
┌─────────────┐     ┌──────────────┐     ┌────────────────┐
│   Mapper    │───▶│  Partitioner │────▶│   Reducers     │
│   (Filter   │     │  (By Month)  │     │   (12 tasks)   │
│    & Map)   │     │              │     │                │
└─────────────┘     └──────────────┘     └────────────────┘
                                                │
                                                ▼
                                    ┌──────────────────────┐
                                    │  12 Monthly Files    │
                                    │  (part-r-00000 to    │
                                    │   part-r-00011)      │
                                    └──────────────────────┘

Stage 2: Top-K Selection
┌──────────────┐     ┌──────────────┐     ┌────────────────┐
│  Job 2.1:    │────▶│   Job 2.2:   │────▶│  Final Output  │
│  Aggregate   │     │  Sort & Top-K│     │  (Top 5 items) │
│  Across      │     │  Selection   │     │                │
│  Months      │     │              │     │                │
└──────────────┘     └──────────────┘     └────────────────┘
```

**Key Design Decisions:**
- **Custom Partitioner:** Ensures even distribution across reducers by month
- **Combiner Functions:** Reduce network I/O by pre-aggregating map outputs
- **Descending Sort Comparator:** Custom comparator for top-K selection
- **Single Reducer in Stage 2:** Ensures global top-K (not per-partition)

---

## 📁 Project Structure

```
CS6847-A2/
├── README.md                      # This file
├── Setup.md                       # Initial setup instructions
├── docker-compose.yml             # Hadoop cluster configuration
├── hadoop.env                     # Hadoop environment variables
│
├── Q1/                            # Query 1: Most Popular Routes
│   ├── Instructions.md            # Query-specific instructions
│   ├── PopularRoutesByMonth.java  # Stage 1: Count routes by month
│   ├── TopKRoutes.java            # Stage 2: Select top 5 routes
│   ├── run_q1_pipeline.sh         # Pipeline execution script
│   ├── run_experiments.sh         # Performance experiments script
│   ├── plot_experiments.py        # Visualization script
│   ├── reducer_experiment_results.csv
│   ├── slowstart_experiment_results.csv
│   ├── q1_reducers_plot.png       # Performance plots
│   └── q1_slowstart_plot.png
│
├── Q2/                            # Query 2: Most Expensive Routes
│   ├── Instructions.md
│   ├── ExpensiveRoutesByMonth.java
│   ├── TopKExpensiveRoutes.java
│   ├── run_q2_pipeline.sh
│   ├── run_experiments.sh
│   ├── plot_experiments.py
│   ├── reducer_experiment_results.csv
│   ├── slowstart_experiment_results.csv
│   ├── q2_reducers_plot.png
│   └── q2_slowstart_plot.png
│
├── Q3/                            # Query 3: Most Visited Locations
│   ├── Instructions.md
│   ├── PopularLocationsByMonth.java
│   ├── TopKLocations.java
│   ├── run_q3_pipeline.sh
│   ├── run_experiments.sh
│   ├── plot_experiments.py
│   ├── reducer_experiment_results.csv
│   ├── slowstart_experiment_results.csv
│   ├── q3_reducers_plot.png
│   └── q3_slowstart_plot.png
│
└── Q4/                            # Query 4: Popular Nightlife Spots
    ├── Instructions.md
    ├── NightlifeSpotsByMonth.java
    ├── TopKNightlifeSpots.java
    ├── run_q4_pipeline.sh
    ├── run_experiments.sh
    ├── plot_experiments.py
    ├── reducer_experiment_results.csv
    ├── slowstart_experiment_results.csv
    ├── q4_reducers_plot.png
    └── q4_slowstart_plot.png
....
....
```

---

## 🔧 Prerequisites (for Windows 11)

### Required Software

- **Docker Desktop** (v20.10+)
- **Docker Compose** (v1.29+)
- **Java JDK** 8 (for local compilation, optional)
- **Python** 3.8+ with libraries:
  - `pandas`
  - `matplotlib`
  - `numpy`
- **Git** (for cloning repository)

### System Requirements

- **RAM:** Minimum 8 GB (16 GB recommended)
- **Disk Space:** Minimum 20 GB free space
- **CPU:** 4+ cores recommended
- **OS:** Windows 10/11, macOS, or Linux

---

## 🚀 Setup Instructions

### Step 1: Clone the Repository

```
git clone https://github.com/Gamikant/CS6847-A2.git
cd CS6847-A2
```

### Step 2: Start Hadoop Cluster

```
# Open Docker Desktop first, then run:
docker-compose up -d

# Verify all containers are running
docker ps
```

**Expected Output:**
```
CONTAINER ID   IMAGE                              STATUS
<id>           bde2020/hadoop-namenode:2.0.0-hadoop3.2.1-java8      Up
<id>           bde2020/hadoop-datanode:2.0.0-hadoop3.2.1-java8      Up
<id>           bde2020/hadoop-resourcemanager:2.0.0-hadoop3.2.1-java8  Up
<id>           bde2020/hadoop-nodemanager:2.0.0-hadoop3.2.1-java8   Up
<id>           bde2020/hadoop-historyserver:2.0.0-hadoop3.2.1-java8 Up
```

### Step 3: Access Hadoop Web UIs

- **NameNode:** http://localhost:9870
- **ResourceManager:** http://localhost:8088
- **DataNode:** http://localhost:9864
- **NodeManager:** http://localhost:8042
- **History Server:** http://localhost:8188

### Step 4: Upload Dataset to HDFS

```
# Copy dataset to namenode container
docker cp /path/to/train.csv namenode:/tmp/train.csv

# Enter namenode container
docker exec -it namenode bash

# Inside container: Create HDFS directory and upload data
hdfs dfs -mkdir -p /user/root/Data
hdfs dfs -put /tmp/train.csv /user/root/Data/train.csv

# Verify upload
hdfs dfs -ls /user/root/Data
hdfs dfs -du -h /user/root/Data/train.csv
```

**Expected:** `5.3 GB` file uploaded successfully

### Step 5: Run a Query (Example: Q1)

See detailed instructions in each query folder's `Instructions.md` file.

**Quick Start for Q1:**

```
# 1. Copy files to container
cd Q1
docker cp PopularRoutesByMonth.java namenode:/Q1/
docker cp TopKRoutes.java namenode:/Q1/
docker cp run_q1_pipeline.sh namenode:/Q1/
docker cp run_experiments.sh namenode:/Q1/

# 2. Inside namenode container
docker exec -it namenode bash
cd /Q1

# 3. Compile Java files
javac -classpath "$(hadoop classpath)" PopularRoutesByMonth.java
javac -classpath "$(hadoop classpath)" TopKRoutes.java

# 4. Create JAR files
jar cf PopularRoutesByMonth.jar PopularRoutesByMonth*.class
jar cf TopKRoutes.jar TopKRoutes*.class

# 5. Run pipeline
chmod +x run_q1_pipeline.sh
./run_q1_pipeline.sh

# 6. Run experiments (optional)
chmod +x run_experiments.sh
./run_experiments.sh
```

### Step 6: Generate Plots (Local Machine)

```
# Copy experiment results from container
docker cp namenode:/Q1/reducer_experiment_results.csv ./Q1/
docker cp namenode:/Q1/slowstart_experiment_results.csv ./Q1/

# Generate plots
cd Q1
python3 plot_experiments.py
```

---

## 🔍 Query Implementations

### Query 1: Most Popular Routes

**Objective:** Find the top 5 most frequently traveled routes in 2013.

**Algorithm:**
1. **Filter:** Year = 2013, valid coordinates
2. **Map:** Extract route (pickup → dropoff coordinates) with 4 decimal precision
3. **Reduce (Stage 1):** Count occurrences per route per month
4. **Aggregate (Stage 2):** Sum counts across all months for each route
5. **Sort & Select:** Pick top 5 routes by frequency

**Key Files:**
- `PopularRoutesByMonth.java` - Stage 1 MapReduce
- `TopKRoutes.java` - Stage 2 Top-K selection

**Output Format:**
```
-73.9870,40.7560,-73.9900,40.7450    1658
-73.9380,40.7580,-73.9380,40.7580    670
...
```

---

### Query 2: Most Expensive Routes

**Objective:** Find the top 5 routes with highest maximum fare in 2013.

**Algorithm:**
1. **Filter:** Year = 2013, valid coordinates, 0 < fare ≤ $500
2. **Map:** Extract route and fare_amount
3. **Reduce (Stage 1):** Find maximum fare per route per month
4. **Aggregate (Stage 2):** Find global maximum fare for each route
5. **Sort & Select:** Pick top 5 routes by maximum fare

**Key Files:**
- `ExpensiveRoutesByMonth.java` - Stage 1 MapReduce
- `TopKExpensiveRoutes.java` - Stage 2 Top-K selection

**Output Format:**
```
-73.7880,40.6430,-73.9870,40.7230    450.00
-74.0060,40.7330,-73.7890,40.6440    425.50
...
```

---

### Query 3: Most Visited Locations

**Objective:** Find the top 5 most visited pickup locations AND top 5 most visited dropoff locations in 2013.

**Algorithm:**
1. **Filter:** Year = 2013, valid coordinates
2. **Map:** Emit BOTH pickup and dropoff locations (with type prefix)
3. **Reduce (Stage 1):** Count visits per location type per month
4. **Aggregate (Stage 2):** Sum counts across months
5. **Sort & Select:** Pick top 5 for each type (pickup/dropoff)

**Key Files:**
- `PopularLocationsByMonth.java` - Stage 1 MapReduce
- `TopKLocations.java` - Stage 2 Top-K selection

**Output Format:**
```
PICKUP: -73.9870,40.7560    12458
PICKUP: -73.9900,40.7500    11234
...
DROPOFF: -73.9850,40.7480   10987
DROPOFF: -73.9920,40.7560   9876
...
```

---

### Query 4: Popular Nightlife Spots

**Objective:** Find the top 5 most popular nightlife dropoff spots (8 PM - 2 AM) in 2013.

**Algorithm:**
1. **Filter:** Year = 2013, time ∈ [20:00-23:59] ∪ [00:00-02:59], valid coordinates
2. **Map:** Extract dropoff locations during nightlife hours
3. **Reduce (Stage 1):** Count dropoffs per location per month
4. **Aggregate (Stage 2):** Sum counts across months
5. **Sort & Select:** Pick top 5 nightlife hotspots

**Key Files:**
- `NightlifeSpotsByMonth.java` - Stage 1 MapReduce
- `TopKNightlifeSpots.java` - Stage 2 Top-K selection

**Output Format:**
```
-73.9910,40.7560    8765
-73.9980,40.7480    7654
...
```

---

## ⚡ Performance Experiments

### Experiment 1: Number of Reducers

**Hypothesis:** Increasing reducers improves parallelism but adds coordination overhead.

**Configurations Tested:** 1, 2, 4, 6, 8, 12, 16, 24 reducers

**Metrics:**
- Execution time (seconds)
- Speedup ratio
- Resource utilization

**Expected Behavior:**
- Initial speedup with more reducers (2-12)
- Diminishing returns beyond optimal point
- Overhead dominates at very high reducer counts (24+)

---

### Experiment 2: Slow Start Parameter

**Hypothesis:** Starting reducers earlier (lower value) enables better shuffle overlap.

**Configurations Tested:** 0.05, 0.25, 0.50, 0.75, 0.80, 0.95

**Parameter Meaning:**
- `mapreduce.job.reduce.slowstart.completedmaps` controls when reducers start
- `0.05` = reducers start when 5% of maps complete
- `0.95` = reducers start when 95% of maps complete

**Expected Behavior:**
- Lower values (0.05-0.25): Better overlap, faster shuffle
- Higher values (0.75-0.95): Reducers idle longer, slower overall

---

### Experiment Automation

Each query includes `run_experiments.sh` that:
1. Runs 8 reducer experiments (1, 2, 4, 6, 8, 12, 16, 24)
2. Runs 6 slow start experiments (0.05, 0.25, 0.50, 0.75, 0.80, 0.95)
3. Records execution time for each configuration
4. Generates CSV files for plotting

**Total Experiments:** 14 per query × 4 queries = **56 MapReduce jobs**

---

## 📈 Results & Analysis

### Sample Results (Query 1)

**Top 5 Most Popular Routes (2013):**

| Rank | Route (Pickup → Dropoff) | Trip Count |
|------|--------------------------|------------|
| 1 | -73.9490,40.7450 → -73.9490,40.7450 | 1,658 |
| 2 | -73.9380,40.7580 → -73.9380,40.7580 | 670 |
| 3 | -73.9020,40.7640 → -73.9020,40.7640 | 624 |
| 4 | -73.9780,40.7520 → -73.9910,40.7500 | 372 |
| 5 | -74.0230,40.7660 → -74.0230,40.7660 | 368 |

**Observation:** Most popular "routes" are actually same-location trips (pickup = dropoff), likely representing:
- Very short trips within a block
- Taxi stand waiting patterns
- GPS coordinate rounding effects

---

### Performance Analysis (Sample from Q1)

#### Reducer Count Impact

| Reducers | Time (s) | Speedup | Efficiency |
|----------|----------|---------|------------|
| 1 | 196 | 1.0× | 100% |
| 2 | 175 | 1.12× | 56% |
| 4 | 165 | 1.19× | 30% |
| 8 | 158 | 1.24× | 15.5% |
| 12 | 155 | 1.26× | 10.5% |

**Analysis:**
- **Best Performance:** 12 reducers (155s) - 21% faster than single reducer
- **Diminishing Returns:** Beyond 8 reducers, improvement < 2%
- **Optimal Range:** 8-12 reducers for this dataset and cluster

---

#### Slow Start Impact

| Slow Start | Time (s) | Difference |
|-----------|----------|------------|
| 0.05 | 198 | Baseline |
| 0.25 | 201 | +1.5% |
| 0.50 | 205 | +3.5% |
| 0.75 | 210 | +6.1% |

**Analysis:**
- **Best Configuration:** 0.05 (earliest start)
- **Impact:** Relatively minor (3-12s difference)
- **Reason:** Dataset shuffle phase benefits from early reducer startup

---

## 🛠️ Technologies Used

### Core Technologies

- **Apache Hadoop 3.2.1**
  - HDFS (Hadoop Distributed File System)
  - YARN (Yet Another Resource Negotiator)
  - MapReduce v2 (MRv2)

- **Docker & Docker Compose**
  - Multi-container orchestration
  - Network isolation
  - Volume persistence

- **Java 8**
  - MapReduce job implementation
  - Custom partitioners and comparators
  - Writable data types

- **Python 3.8+**
  - Pandas for data analysis
  - Matplotlib for visualization
  - NumPy for numerical operations

### Supporting Tools

- **Bash Scripting**
  - Pipeline automation
  - Experiment orchestration
  - File management

- **HDFS CLI**
  - Data upload/download
  - Directory management
  - File inspection

---

## 🎓 Key Learnings

1. **Data Quality Matters:** Invalid coordinates (0.0 values) accounted for ~25% of records
2. **Precision Trade-offs:** 4 decimal places provides optimal balance between accuracy and aggregation
3. **Partitioning Strategy:** Custom month-based partitioning ensures even workload distribution
4. **Combiner Optimization:** Pre-aggregation reduces shuffle data by ~60%
5. **Resource Tuning:** Optimal reducer count depends on data size, not just cluster capacity
6. **Slow Start Impact:** Early reducer start helps when shuffle phase is I/O bound

---

## 🐛 Known Issues & Limitations

1. **Single-Location Routes:** Many "popular routes" have identical pickup/dropoff due to:
   - GPS coordinate rounding
   - Very short trips
   - Taxi stand patterns

2. **Cluster Size:** 3-node cluster limits parallelism beyond 12-16 reducers

3. **Memory Constraints:** Large aggregations may cause reducer OOM for very high precision

4. **Time Zone:** Dataset uses UTC timestamps; local NYC time analysis would require conversion

## 👨‍💻 Author

**Pranav Yadav (Gamikant)**  
Dual Degree Data Science Student  
Indian Institute of Technology Madras  
Email: pranavyadav454545@gmail.com  
GitHub: [@Gamikant](https://github.com/Gamikant)

---

## 📄 License

This project is part of academic coursework at IIT Madras. The code is provided as-is for educational purposes. The base Docker Hadoop configuration is from [big-data-europe/docker-hadoop](https://github.com/big-data-europe/docker-hadoop) (MIT License).

---

## 🙏 Acknowledgments

- **Prof. Janakiram** - Course CS6847: Cloud Computing
- **Big Data Europe** - Docker Hadoop base images
- **NYC TLC** - Open dataset availability

---

## 📞 Support

For issues or questions:
1. Check `Setup.md` and query-specific `Instructions.md`
2. Open an issue on GitHub
3. Contact: pranavyadav454545@gmail.com

---

**Last Updated:** October 2025  
**Version:** 1.0.0

***