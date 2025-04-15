import os
import shutil

def delete_metadata_folders(start_path):
    """
    Deletes all folders named 'metadata' in the given directory tree.
    """
    print(f"Starting from directory: {start_path}")
    if not os.path.exists(start_path):
        print(f"Error: The directory '{start_path}' does not exist.")
        return

    for root, dirs, files in os.walk(start_path, topdown=False):  # Process subdirectories first
        print(f"Scanning directory: {root}")  # Debug: Check which directory is being scanned
        for dir_name in dirs:
            if dir_name == "metadata":
                metadata_path = os.path.join(root, dir_name)
                print(f"Found 'metadata' folder at: {metadata_path}")  # Debug: Found target folder
                try:
                    shutil.rmtree(metadata_path)
                    print(f"Deleted: {metadata_path}")
                except Exception as e:
                    print(f"Error deleting {metadata_path}: {e}")

    print("Operation completed.")

if __name__ == "__main__":
    start_directory = "/Volumes/qStivi/jetbrains/IdeaProjects/juliantroesser/schematch/data"
    if start_directory:
        delete_metadata_folders(start_directory)
    else:
        print("No directory provided. Exiting.")