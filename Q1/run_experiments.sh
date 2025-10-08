#!/bin/bash
# File: run_experiments.sh

# Make sure we're in the right directory
cd /Q1

# Configuration
INPUT_CSV="/user/root/Data/train.csv"
Q1_DIR="/user/root/Q1"

echo "=========================================="
echo "Query 1: Performance Experiments"
echo "=========================================="
echo "This will take a while. Each experiment runs on the full dataset."
echo ""

# Initialize CSV files with headers
echo "num_reducers,time_seconds" > reducer_experiment_results.csv
echo "slowstart_fraction,time_seconds" > slowstart_experiment_results.csv

# Experiment 1: Vary number of reducers
echo "Experiment 1: Varying Number of Reducers"
echo "=========================================="

for NUM_REDUCERS in 1 2; do # 4 6 8 12 16 24
    echo "Testing with ${NUM_REDUCERS} reducers..."
    
    # Clean output
    hdfs dfs -rm -r -f ${Q1_DIR}/exp_reducers_${NUM_REDUCERS}
    
    # Run job and time it
    START=$(date +%s)
    hadoop jar PopularRoutesByMonth.jar PopularRoutesByMonth \
        ${INPUT_CSV} \
        ${Q1_DIR}/exp_reducers_${NUM_REDUCERS} \
        ${NUM_REDUCERS} > /tmp/log_reducers_${NUM_REDUCERS}.txt 2>&1
    EXIT_CODE=$?
    END=$(date +%s)
    DURATION=$((END - START))
    
    if [ $EXIT_CODE -eq 0 ]; then
        echo "${NUM_REDUCERS},${DURATION}" >> reducer_experiment_results.csv
        echo "  ✓ Completed in ${DURATION} seconds"
    else
        echo "  ✗ Failed! Check /tmp/log_reducers_${NUM_REDUCERS}.txt"
    fi
done

echo ""
echo "Experiment 2: Varying Slow Start Parameter"
echo "=========================================="

for SLOWSTART in 0.05 0.25; do  # 0.50 0.75 0.80 0.95
    echo "Testing with slow start = ${SLOWSTART}..."
    
    # Clean output
    hdfs dfs -rm -r -f ${Q1_DIR}/exp_slowstart_${SLOWSTART}
    
    # Run job with slow start parameter and time it
    START=$(date +%s)
    hadoop jar PopularRoutesByMonth.jar PopularRoutesByMonth \
        ${INPUT_CSV} \
        ${Q1_DIR}/exp_slowstart_${SLOWSTART} \
        12 \
        -Dmapreduce.job.reduce.slowstart.completedmaps=${SLOWSTART} \
        > /tmp/log_slowstart_${SLOWSTART}.txt 2>&1
    EXIT_CODE=$?
    END=$(date +%s)
    DURATION=$((END - START))
    
    if [ $EXIT_CODE -eq 0 ]; then
        echo "${SLOWSTART},${DURATION}" >> slowstart_experiment_results.csv
        echo "  ✓ Completed in ${DURATION} seconds"
    else
        echo "  ✗ Failed! Check /tmp/log_slowstart_${SLOWSTART}.txt"
    fi
done

echo ""
echo "=========================================="
echo "Experiments completed!"
echo "=========================================="
echo "Results saved to:"
echo "  - /Q1/reducer_experiment_results.csv"
echo "  - /Q1/slowstart_experiment_results.csv"
echo ""
echo "To generate plots, copy these CSV files to your local machine and run:"
echo "  python3 plot_experiments.py"
