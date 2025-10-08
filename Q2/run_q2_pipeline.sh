#!/bin/bash
# File: run_q2_pipeline.sh

# Configuration
YEAR="2013"
INPUT_CSV="/user/root/Data/train.csv"
Q2_DIR="/user/root/Q2"
STAGE1_OUTPUT="${Q2_DIR}/stage1_monthly_maxfares"
STAGE2_OUTPUT="${Q2_DIR}/stage2_top5_expensive"
NUM_REDUCERS=12
TOP_K=5

echo "======================================"
echo "Query 2 Pipeline: Expensive Routes 2013"
echo "======================================"

# Clean up previous outputs
echo "[1/5] Cleaning up previous outputs..."
hdfs dfs -rm -r -f ${STAGE1_OUTPUT}
hdfs dfs -rm -r -f ${STAGE2_OUTPUT}
hdfs dfs -rm -r -f ${STAGE2_OUTPUT}_temp

# Stage 1: Find max fare per route by month
echo "[2/5] Running Stage 1: Monthly Max Fares..."
echo "  Input: ${INPUT_CSV}"
echo "  Output: ${STAGE1_OUTPUT}"
echo "  Reducers: ${NUM_REDUCERS}"

START_TIME=$(date +%s)
hadoop jar /Q2/ExpensiveRoutesByMonth.jar ExpensiveRoutesByMonth \
    ${INPUT_CSV} ${STAGE1_OUTPUT} ${NUM_REDUCERS}
STAGE1_EXIT=$?
END_TIME=$(date +%s)
STAGE1_DURATION=$((END_TIME - START_TIME))

if [ $STAGE1_EXIT -ne 0 ]; then
    echo "ERROR: Stage 1 failed!"
    exit 1
fi

echo "  Stage 1 completed in ${STAGE1_DURATION} seconds"

# Stage 2: Aggregate and select top K
echo "[3/5] Running Stage 2: Top-K Expensive Routes..."
echo "  Input: ${STAGE1_OUTPUT}"
echo "  Output: ${STAGE2_OUTPUT}"
echo "  K: ${TOP_K}"

START_TIME=$(date +%s)
hadoop jar /Q2/TopKExpensiveRoutes.jar TopKExpensiveRoutes \
    ${STAGE1_OUTPUT} ${STAGE2_OUTPUT} ${TOP_K}
STAGE2_EXIT=$?
END_TIME=$(date +%s)
STAGE2_DURATION=$((END_TIME - START_TIME))

if [ $STAGE2_EXIT -ne 0 ]; then
    echo "ERROR: Stage 2 failed!"
    exit 1
fi

echo "  Stage 2 completed in ${STAGE2_DURATION} seconds"

# Display results
echo "[4/5] Displaying Top ${TOP_K} Most Expensive Routes for ${YEAR}:"
echo "========================================"
hdfs dfs -cat ${STAGE2_OUTPUT}/part-r-00000

# Show monthly breakdown file sizes
echo ""
echo "[5/5] Monthly Breakdown (12 files):"
echo "========================================"
hdfs dfs -du -h ${STAGE1_OUTPUT}

echo ""
echo "Pipeline completed successfully!"
echo "Total execution time: $((STAGE1_DURATION + STAGE2_DURATION)) seconds"
