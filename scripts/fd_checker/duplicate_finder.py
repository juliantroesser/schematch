import pandas as pd

def find_duplicates(file_path):
    df = pd.read_csv(file_path)
    duplicates = df[df.duplicated(subset=[df.columns[0]], keep=False)]
    return duplicates

if __name__ == "__main__":
    file_path = '/Volumes/qStivi/jetbrains/IdeaProjects/schematch/data/Valentine-Wikidata/Musicians_joinable/source/musicians_joinable_source.csv'
    duplicates = find_duplicates(file_path)
    if not duplicates.empty:
        print("Duplicates found:")
        print(duplicates)
    else:
        print("No duplicates found.")