#!/usr/bin/env python3
"""
Plot performance experiments for Query 4
Generates plots for:
1. Execution time vs Number of Reducers
2. Execution time vs Slow Start Parameter
"""

import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import sys
import os

def plot_reducer_experiment(csv_file='reducer_experiment_results.csv', output_file='q4_reducers_plot.png'):
    """Plot execution time vs number of reducers"""
    
    if not os.path.exists(csv_file):
        print(f"Error: {csv_file} not found!")
        return False
    
    df = pd.read_csv(csv_file)
    df = df.sort_values('num_reducers')
    
    plt.figure(figsize=(12, 7))
    
    plt.plot(df['num_reducers'], df['time_seconds'], 
             marker='o', linewidth=2.5, markersize=10, 
             color='#2C2891', markerfacecolor='#E63946',
             markeredgewidth=2, markeredgecolor='#2C2891')
    
    for i, row in df.iterrows():
        plt.annotate(f"{row['time_seconds']}s", 
                    (row['num_reducers'], row['time_seconds']),
                    textcoords="offset points", 
                    xytext=(0,10), 
                    ha='center',
                    fontsize=9,
                    bbox=dict(boxstyle='round,pad=0.3', facecolor='yellow', alpha=0.3))
    
    plt.xlabel('Number of Reducers', fontsize=14, fontweight='bold')
    plt.ylabel('Execution Time (seconds)', fontsize=14, fontweight='bold')
    plt.title('Query 4: Execution Time vs Number of Reducers\n(Nightlife Spots 8PM-2AM - NYC Taxi 2013)', 
              fontsize=16, fontweight='bold', pad=20)
    plt.grid(True, alpha=0.3, linestyle='--')
    plt.xticks(df['num_reducers'], fontsize=11)
    plt.yticks(fontsize=11)
    
    best_idx = df['time_seconds'].idxmin()
    best_reducers = df.loc[best_idx, 'num_reducers']
    best_time = df.loc[best_idx, 'time_seconds']
    worst_time = df['time_seconds'].max()
    improvement = ((worst_time - best_time) / worst_time) * 100
    
    stats_text = f'Best Performance:\n{int(best_reducers)} reducers\n{best_time}s\n{improvement:.1f}% improvement'
    plt.text(0.02, 0.98, stats_text, 
             transform=plt.gca().transAxes,
             fontsize=10,
             verticalalignment='top',
             bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))
    
    plt.tight_layout()
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✓ Saved: {output_file}")
    
    return True

def plot_slowstart_experiment(csv_file='slowstart_experiment_results.csv', output_file='q4_slowstart_plot.png'):
    """Plot execution time vs slow start parameter"""
    
    if not os.path.exists(csv_file):
        print(f"Error: {csv_file} not found!")
        return False
    
    df = pd.read_csv(csv_file)
    df = df.sort_values('slowstart_fraction')
    
    plt.figure(figsize=(12, 7))
    
    plt.plot(df['slowstart_fraction'], df['time_seconds'],
             marker='s', linewidth=2.5, markersize=10,
             color='#9B2226', markerfacecolor='#F4A261',
             markeredgewidth=2, markeredgecolor='#9B2226')
    
    for i, row in df.iterrows():
        plt.annotate(f"{row['time_seconds']}s",
                    (row['slowstart_fraction'], row['time_seconds']),
                    textcoords="offset points",
                    xytext=(0,10),
                    ha='center',
                    fontsize=9,
                    bbox=dict(boxstyle='round,pad=0.3', facecolor='lightblue', alpha=0.3))
    
    plt.xlabel('Slow Start Fraction (mapreduce.job.reduce.slowstart.completedmaps)', 
               fontsize=14, fontweight='bold')
    plt.ylabel('Execution Time (seconds)', fontsize=14, fontweight='bold')
    plt.title('Query 4: Execution Time vs Slow Start Parameter\n(Nightlife Spots 8PM-2AM - NYC Taxi 2013)', 
              fontsize=16, fontweight='bold', pad=20)
    plt.grid(True, alpha=0.3, linestyle='--')
    plt.xticks(df['slowstart_fraction'], fontsize=11)
    plt.yticks(fontsize=11)
    
    best_idx = df['time_seconds'].idxmin()
    best_slowstart = df.loc[best_idx, 'slowstart_fraction']
    best_time = df.loc[best_idx, 'time_seconds']
    worst_time = df['time_seconds'].max()
    improvement = ((worst_time - best_time) / worst_time) * 100
    
    stats_text = f'Best Performance:\nSlowstart = {best_slowstart}\n{best_time}s\n{improvement:.1f}% improvement'
    plt.text(0.02, 0.98, stats_text,
             transform=plt.gca().transAxes,
             fontsize=10,
             verticalalignment='top',
             bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))
    
    plt.tight_layout()
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✓ Saved: {output_file}")
    
    return True

def print_summary(reducer_csv='reducer_experiment_results.csv', 
                 slowstart_csv='slowstart_experiment_results.csv'):
    """Print summary statistics"""
    
    print("\n" + "="*60)
    print("QUERY 4 EXPERIMENT SUMMARY")
    print("="*60)
    
    if os.path.exists(reducer_csv):
        df_red = pd.read_csv(reducer_csv)
        print("\n📊 Reducer Experiment:")
        print(f"   Configurations tested: {len(df_red)}")
        print(f"   Fastest: {df_red['time_seconds'].min():.0f}s with {int(df_red.loc[df_red['time_seconds'].idxmin(), 'num_reducers'])} reducers")
        print(f"   Slowest: {df_red['time_seconds'].max():.0f}s with {int(df_red.loc[df_red['time_seconds'].idxmax(), 'num_reducers'])} reducers")
        print(f"   Range: {df_red['time_seconds'].max() - df_red['time_seconds'].min():.0f}s difference")
    
    if os.path.exists(slowstart_csv):
        df_slow = pd.read_csv(slowstart_csv)
        print("\n📊 Slow Start Experiment:")
        print(f"   Configurations tested: {len(df_slow)}")
        print(f"   Fastest: {df_slow['time_seconds'].min():.0f}s with slowstart={df_slow.loc[df_slow['time_seconds'].idxmin(), 'slowstart_fraction']}")
        print(f"   Slowest: {df_slow['time_seconds'].max():.0f}s with slowstart={df_slow.loc[df_slow['time_seconds'].idxmax(), 'slowstart_fraction']}")
        print(f"   Range: {df_slow['time_seconds'].max() - df_slow['time_seconds'].min():.0f}s difference")
    
    print("\n" + "="*60)

if __name__ == "__main__":
    print("="*60)
    print("Query 4 Performance Analysis - Plot Generator")
    print("(Most Popular Nightlife Spots 8PM-2AM)")
    print("="*60)
    
    reducer_exists = os.path.exists('reducer_experiment_results.csv')
    slowstart_exists = os.path.exists('slowstart_experiment_results.csv')
    
    if not reducer_exists and not slowstart_exists:
        print("\n❌ Error: No CSV files found!")
        print("Please run ./run_experiments.sh first to generate the data.")
        sys.exit(1)
    
    print("\nGenerating plots...")
    
    success = True
    if reducer_exists:
        success &= plot_reducer_experiment()
    else:
        print("⚠️  Skipping reducer plot (CSV not found)")
    
    if slowstart_exists:
        success &= plot_slowstart_experiment()
    else:
        print("⚠️  Skipping slowstart plot (CSV not found)")
    
    if success:
        print_summary()
        print("\n✅ All plots generated successfully!")
        print("\nGenerated files:")
        if reducer_exists:
            print("  - q4_reducers_plot.png")
        if slowstart_exists:
            print("  - q4_slowstart_plot.png")
    else:
        print("\n⚠️  Some plots could not be generated")
        sys.exit(1)
