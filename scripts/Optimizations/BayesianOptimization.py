import json
import re
import sys

import numpy as np
from scipy.optimize import minimize
from scipy.stats import norm  # Import norm from scipy.stats for probability calculations
from sklearn.gaussian_process import GaussianProcessRegressor
from sklearn.gaussian_process.kernels import Matern


def bayesian_optimization(score, current_params, possible_values):
    param_space = {}

    # Convert categorical values to numerical indices for optimization
    for key, values in possible_values.items():
        if isinstance(values, list):
            param_space[key] = {str(v).lower(): i for i, v in enumerate(values)}

    # Convert current categorical parameters to their numerical representation
    current_numeric = np.array([param_space[key][str(value).lower()] for key, value in current_params.items()])

    # Generate a larger initial random sample space for better exploration
    num_initial_samples = 100  # Increase the number of initial random samples
    X_sample = np.random.randint(0, max(len(v) for v in param_space.values()), (num_initial_samples, len(param_space)))
    Y_sample = np.random.rand(num_initial_samples)  # Random initial scores

    # Append the current parameter configuration and its score to the dataset
    X_sample = np.vstack((X_sample, current_numeric))
    Y_sample = np.append(Y_sample, score)

    # Normalize Y_sample to improve optimization performance
    Y_sample = (Y_sample - np.min(Y_sample)) / (np.max(Y_sample) - np.min(Y_sample) + 1e-6)

    # Define Gaussian Process Regressor with a Matern kernel for flexible modeling
    kernel = Matern(nu=2.5)
    gp = GaussianProcessRegressor(kernel=kernel, n_restarts_optimizer=25)  # Increase restart attempts for better optimization
    gp.fit(X_sample, Y_sample)  # Fit the model to observed samples

    # Define the acquisition function using Expected Improvement (EI) for maximization
    def acquisition(x):
        x = x.reshape(1, -1)
        mu, sigma = gp.predict(x, return_std=True)
        mu_sample = gp.predict(X_sample)
        sigma = sigma.reshape(-1, 1)

        with np.errstate(divide='warn'):
            imp = mu - np.max(mu_sample)  # Improvement over the best observed sample
            Z = imp / sigma  # Standardized improvement
            ei = imp * norm.cdf(Z) + sigma * norm.pdf(Z)  # Expected improvement calculation
            ei[sigma == 0.0] = 0.0  # Avoid division errors for zero variance

        # print(f"Acquisition for {x}: EI={ei}")  # Debug output

        return -ei  # Minimize the negative EI to maximize improvement

    # Optimize the acquisition function to find the next best set of parameters
    bounds = [(0, len(v) - 1 + 0.1) for v in param_space.values()]  # Expand search space slightly
    res = minimize(acquisition, current_numeric, bounds=bounds, method='L-BFGS-B')
    next_numeric = np.round(res.x).astype(int)  # Convert optimized values to integers

    # Convert numerical representation back to categorical values
    next_params = {key: values[list(param_space[key].values()).index(val)] for (key, values), val in zip(possible_values.items(), next_numeric)}

    return next_params


def transform_to_json_compatible(input_str):
    # Convert input string to JSON-compatible format
    input_str = input_str.replace("=", ":")
    input_str = re.sub(r'(\w+):', r'"\1":', input_str)
    input_str = re.sub(r':(\w+)', r':"\1"', input_str)
    input_str = input_str.replace(':"true"', ':true').replace(':"false"', ':false')
    input_str = re.sub(r'\[([^\[\]]+)\]', lambda m: '[' + ', '.join(f'"{x.strip()}"' for x in m.group(1).split(',')) + ']', input_str)
    return input_str


if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python3 BayesianOptimization.py <score> <current_params> <possible_values>")
        sys.exit(1)

    score = float(sys.argv[1])

    # Ensure Boolean values are consistently stored as lowercase strings
    current_params = json.loads(transform_to_json_compatible(sys.argv[2]))
    current_params = {k: str(v).lower() for k, v in current_params.items()}

    possible_values = json.loads(transform_to_json_compatible(sys.argv[3]))

    # Perform Bayesian optimization to find the next best set of parameters
    next_params = bayesian_optimization(score, current_params, possible_values)

    # Output the recommended next set of parameters
    for key, value in next_params.items():
        print(f"{key}={value}")
