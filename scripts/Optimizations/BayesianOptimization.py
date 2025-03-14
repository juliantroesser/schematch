import json
import re
import sys
import sklearn


def bayesian_optimization(score, current_params, possible_values):
    """
    Placeholder function for Bayesian Optimization.
    Currently returns the same parameters as input.
    """

    result = gp_minimize(objective_function, search_space, n_calls=30, random_state=42)

    # Best found parameters (maximizing f(x, y))
    best_x, best_y = result.x
    best_value = -result.fun  # Convert back from negative

    print(f"Optimal x: {best_x:.4f}, Optimal y: {best_y:.4f}")
    print(f"Maximum f(x, y): {best_value:.4f}")

    return current_params

# Define the function to maximize
def objective_function(params):
    x, y = params
    return - (np.sin(x) + np.cos(y))  # Minimize the negative


def transform_to_json_compatible(input_str):
    """
    Converts an input string to a JSON-compatible format.
    """
    input_str = input_str.replace("=", ":")
    input_str = re.sub(r'(\w+):', r'"\1":', input_str)
    input_str = re.sub(r':(\w+)', r':"\1"', input_str)
    input_str = input_str.replace(':"true"', ':true').replace(':"false"', ':false')
    input_str = re.sub(r'\[([^\[\]]+)]',
                       lambda m: '[' + ', '.join(f'"{x.strip()}"' for x in m.group(1).split(',')) + ']', input_str)
    return input_str


if __name__ == "__main__":
    # Example input for testing
    sys.argv = ['scripts/Optimizations/BayesianOptimization.py', '0.7827264368534088',
        '{fixpoint=C, INDV2=false, INDV1=false, FDV1=false, FDV2=false, UCCV2=false, UCCV1=false}',
        '{fixpoint=[A, B, C], INDV2=[true, false], INDV1=[true, false], FDV1=[true, false], FDV2=[true, false], UCCV2=[true, false], UCCV1=[true, false]}']

    if len(sys.argv) != 4:
        print("Usage: python3 BayesianOptimization.py <score> <current_params> <possible_values>")
        sys.exit(1)

    score = float(sys.argv[1])

    current_params = json.loads(transform_to_json_compatible(sys.argv[2]))
    current_params = {k: (str(v).lower() if isinstance(v, bool) else v) for k, v in current_params.items()}

    possible_values = json.loads(transform_to_json_compatible(sys.argv[3]))

    # Define the search space for x and y
    search_space = [
        Real(-np.pi, np.pi, name='x'),  # Search space for x
        Real(-np.pi, np.pi, name='y')   # Search space for y
    ]

    # Perform Bayesian optimization to find the next best set of parameters
    next_params = bayesian_optimization(score, current_params, possible_values)

    # Output the recommended next set of parameters
    for key, value in next_params.items():
        print(f"{key}={value}")
