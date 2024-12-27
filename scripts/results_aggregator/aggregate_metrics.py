import os
import pandas as pd
import numpy as np
import re

# Define the path to the main folder containing the dataset folders
main_folder_path = '/Users/julian/Desktop/Experiments (neu)/Combination/Complete'

# Define the subfolder and file structure
performance_folder = "_performances"
metrics_folders = ["Precision", "Recall", "F1Score", "Overall"]
file_name = "performance_overview_line1.csv"

# Initialize dictionaries to store data
data = {metric: {} for metric in metrics_folders}

# Regex pattern for extracting specific configurations
config_pattern = r"propCoeffPolicy=[^&]+ &\s*fixpoint=[^&]+"

# Iterate through each dataset folder
for dataset_folder in os.listdir(main_folder_path):
    if dataset_folder.startswith('.'):  # Ignore hidden files/folders
        continue

    dataset_path = os.path.join(main_folder_path, dataset_folder)

    if os.path.isdir(dataset_path):  # Ensure it's a folder
        for metric in metrics_folders:
            metric_folder_path = os.path.join(dataset_path, performance_folder, metric)
            file_path = os.path.join(metric_folder_path, file_name)

            if os.path.exists(file_path):  # Ensure the file exists
                # Read the CSV file
                csv_data = pd.read_csv(file_path, header=None).iloc[:, 1:]  # Disregard the first column

                # Extract configuration names (first row) and their values (second row)
                config_names = csv_data.iloc[0].values  # Configuration names
                config_values = csv_data.iloc[1].values  # Corresponding values

                filtered_configs = []
                for config in config_names:
                    match = re.search(config_pattern, config)
                    if match:
                        filtered_configs.append(match.group(0))
                    else:
                        filtered_configs.append(None)

                # Store values for each configuration under the respective metric
                for config, value in zip(filtered_configs, config_values):
                    if config not in data[metric]:
                        data[metric][config] = []
                    data[metric][config].append(float(value))

# Calculate the mean and standard deviation for each configuration
results = {}

#Bis hier hin in data korrekt
for metric, config_data in data.items():
    results[metric] = {}
    for config, values in config_data.items():
        mean_value = round(np.mean(values), 3)
        std_value = round(np.std(values), 2)
        results[metric][config] = {'Mean': mean_value, 'Standard Deviation': std_value}

# Print the results
for metric, config_results in results.items():
    print(f"--- {metric} ---")
    for config, stats in config_results.items():
        print(f"Configuration: {config}")
        print(f"  Mean: {stats['Mean']}")
        print(f"  Standard Deviation: {stats['Standard Deviation']}")
    print()