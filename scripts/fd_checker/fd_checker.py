import argparse

import colorama
import pandas as pd
from colorama import Fore, Style


def parse_determinant(det_str):
    """Parse a determinant string into a list of attributes."""
    det_str = det_str.strip()
    if det_str.startswith("[") and det_str.endswith("]"):
        return [attr.strip().split('.')[-1] for attr in det_str[1:-1].split(",")]
    return [det_str.split('.')[-1]]


def check_fd(df, determinant, dependent):
    """Check if the functional dependency holds in the dataset."""
    violations = [(key, group[dependent].unique()) for key, group in df.groupby(determinant) if
                  len(group[dependent].unique()) > 1]
    return violations


def compute_closure(attributes, fd_set):
    """Compute the closure of a set of attributes with respect to a set of functional dependencies."""
    closure, changed = set(attributes), True
    while changed:
        changed = any(dep not in closure and closure.issuperset(det) and not closure.add(dep) for det, dep in fd_set)
    return closure


def find_transitive_fds(fd_list):
    """Identify transitively derivable (redundant) functional dependencies."""
    return [(det, dep) for i, (det, dep) in enumerate(fd_list) if
            dep in compute_closure(det, fd_list[:i] + fd_list[i + 1:])]


def fd_to_str(det, dep):
    """Convert a functional dependency to string format."""
    return f"({' '.join(sorted(det))}) -> {dep}" if len(det) > 1 else f"{next(iter(det))} -> {dep}"


def main():
    colorama.init(autoreset=True)
    parser = argparse.ArgumentParser(description="Check FDs in a dataset and find transitive dependencies.")
    parser.add_argument("--data",
                        default="/Volumes/qStivi/jetbrains/IdeaProjects/schematch/data/Valentine-Wikidata/Musicians_viewunion/target/musicians_viewunion_target.csv",
                        required=False, help="Path to the dataset CSV file.")
    parser.add_argument("--fd",
                        default="/Volumes/qStivi/jetbrains/IdeaProjects/schematch/data/Valentine-Wikidata/Musicians_viewunion/metadata/target/musicians_viewunion_target/FD_truth.txt",
                        required=False, help="Path to the FD table CSV file.")
    args = parser.parse_args()

    df = pd.read_csv(args.data, dtype=str)
    with open(args.fd, 'r') as fd_file:
        fd_lines = fd_file.readlines()
    fds = [(frozenset(parse_determinant(line.split(" --> ")[0])), line.split(" --> ")[1].strip().split('.')[-1]) for
           line in fd_lines]

    print(f"{Fore.CYAN}{Style.BRIGHT}=== Checking Functional Dependencies ===\n{Style.RESET_ALL}")
    failing_fds = [(det, dep, check_fd(df, list(det), dep)) for det, dep in fds if check_fd(df, list(det), dep)]

    for det, dep, violations in failing_fds:
        det_str = ', '.join(sorted(det)) if len(det) > 1 else next(iter(det))
        print(f"{Fore.RED}{det_str} -> {dep} DOES NOT hold. Violations:{Style.RESET_ALL}")
        for key, values in violations:
            print(f"  When {det_str} = {key}, {dep} has values: {list(values)}")

    print(
        f"\n{Fore.BLUE}{Style.BRIGHT}Summary: {len(fds) - len(failing_fds)} out of {len(fds)} FDs hold true.{Style.RESET_ALL}")

    print(f"\n{Fore.CYAN}{Style.BRIGHT}=== Finding Transitive Dependencies ===\n{Style.RESET_ALL}")
    transitive_fds = find_transitive_fds(fds)
    print(
        f"{Fore.MAGENTA if transitive_fds else Fore.GREEN}{'No' if not transitive_fds else 'The following'} transitive FDs found:{Style.RESET_ALL}")
    for det, dep in transitive_fds:
        print(f"  {fd_to_str(det, dep)}")

    dataset_attributes, fd_attributes = set(df.columns), {attr for det, dep in fds for attr in det.union({dep})}
    missing_attrs = dataset_attributes - fd_attributes
    print(
        f"\n{Fore.YELLOW if missing_attrs else Fore.GREEN}{'Warning: Missing attributes in FD table:' if missing_attrs else 'All attributes are covered by an FD.'}{Style.RESET_ALL}")
    for attr in sorted(missing_attrs):
        print(f"  {attr}")


if __name__ == "__main__":
    main()
