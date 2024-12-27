import os
import pandas as pd
import numpy as np

# Define the path to the main folder containing sub-dataset folders
#main_folder_path = "/Users/julian/Desktop/PerformanceAggregatorSchematch/data/Vanilla/Valentine-ChEMBL"
main_folder_path = '/Users/julian/Desktop/Experiments (neu)/Combination/Complete'

# Define the subfolder and file structure
performance_folder = "_performances"
metrics_folders = ["F1Score", "Overall", "Precision", "Recall"]
file_name = "performance_overview_line1.csv"

# Initialize data structures to store all results
aggregated_data = {metric: [] for metric in metrics_folders}  # List for each metric

# Iterate through each sub-dataset folder
for sub_dataset_folder in os.listdir(main_folder_path):
    sub_dataset_path = os.path.join(main_folder_path, sub_dataset_folder)

    # Skip files and the _performances folder itself
    if not os.path.isdir(sub_dataset_path) or sub_dataset_folder == performance_folder:
        continue

    for metric in metrics_folders:
        metric_folder_path = os.path.join(sub_dataset_path, performance_folder, metric)
        file_path = os.path.join(metric_folder_path, file_name)

        if os.path.exists(file_path):  # Ensure the file exists
            # Read the CSV file
            csv_data = pd.read_csv(file_path, header=None, delimiter=',')

            # Append the CSV data to the list for the corresponding metric
            aggregated_data[metric].append(csv_data)

# Save the aggregated data for each metric
output_performance_folder = os.path.join(main_folder_path, performance_folder)
os.makedirs(output_performance_folder, exist_ok=True)

for metric, data_list in aggregated_data.items():
    if data_list:  # Check if there's data for this metric
        # Concatenate all DataFrames into one

        if not all(df.shape == data_list[0].shape for df in data_list):
            raise ValueError(f"All DataFrames for metric '{metric}' must have the same shape.")

            # Copy the structure from the first DataFrame
        combined_df = data_list[0].copy()

        # Extract numeric parts of all DataFrames
        numeric_data = []
        for df in data_list:
            # Convert the data to numeric, replacing non-numeric values with NaN
            numeric_df = df.iloc[1:, 1:].apply(pd.to_numeric, errors='coerce')
            numeric_data.append(numeric_df.values)

        # Stack the numeric arrays and calculate the element-wise average
        numeric_data = np.array(numeric_data)

        averaged_data = np.mean(numeric_data, axis=0)  # Element-wise average

        # Insert averaged numeric data back into the combined DataFrame
        combined_df.iloc[1:, 1:] = averaged_data

        # Create the output directory for this metric
        output_metric_folder = os.path.join(output_performance_folder, metric)
        os.makedirs(output_metric_folder, exist_ok=True)

        # Define the output file path and save the combined DataFrame
        output_file_path = os.path.join(output_metric_folder, file_name)
        combined_df.to_csv(output_file_path, header=False, index=False)

print("Performance data consolidated successfully!")