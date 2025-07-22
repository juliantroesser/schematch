#!/usr/bin/env python3
import itertools
import json
import logging
import socket
import time

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
import skopt
from skopt import gp_minimize
from skopt.space import Real


def plot_parameter_pairwise_heatmaps(results, param_names):
    """
    For each pair of parameters, plot a heatmap of mean score for each value combination.
    """
    import math
    df = pd.DataFrame([dict(**params, score=score) for params, score in results])

    n_params = len(param_names)
    n_pairs = n_params * (n_params - 1) // 2

    # Calculate rows and columns for grid
    ncols = 3 if n_pairs > 3 else n_pairs
    nrows = math.ceil(n_pairs / ncols)
    plt.figure(figsize=(5 * ncols, 5 * nrows))

    plot_idx = 1
    for i in range(n_params):
        for j in range(i + 1, n_params):
            p1 = param_names[i]
            p2 = param_names[j]
            pivot = df.pivot_table(index=p1, columns=p2, values="score", aggfunc="mean")
            plt.subplot(nrows, ncols, plot_idx)
            sns.heatmap(pivot, annot=True, fmt=".3f", cmap="viridis")
            plt.title(f"Score Heatmap: {p1} vs {p2}")
            plt.xlabel(p2)
            plt.ylabel(p1)
            plot_idx += 1

    plt.tight_layout()
    plt.show()


def plot_parameter_effects_bruteforce(results, param_names):
    """
    Plot a boxplot (or stripplot/violinplot) of score for each value of every parameter.
    - results: list of (params_dict, score)
    - param_names: list of parameter names
    """
    df = pd.DataFrame([dict(**params, score=score) for params, score in results])

    n_params = len(param_names)
    plt.figure(figsize=(4 * n_params, 5))
    for i, param in enumerate(param_names, 1):
        plt.subplot(1, n_params, i)
        # Use boxplot for distribution, or swap to sns.stripplot for all points
        sns.boxplot(x=param, y='score', data=df)
        # sns.stripplot(x=param, y='score', data=df, color='black', alpha=0.4, jitter=True)
        plt.title(param)
        plt.xlabel(param)
        if i == 1:
            plt.ylabel('Score')
        else:
            plt.ylabel('')
    plt.suptitle("Parameter Effect on Score (marginalized over other params)", y=1.04)
    plt.tight_layout()
    plt.show()


# Configure logging for detailed debug output.
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

HOST = "localhost"
LISTEN_PORT = 5003
SEND_PORT = 5005


def format_time(seconds: float) -> str:
    """
    Format a time duration in seconds into hours, minutes, and seconds.
    """
    hours = int(seconds // 3600)
    minutes = int((seconds % 3600) // 60)
    secs = seconds % 60
    return f"{hours}h {minutes}m {secs:.2f}s"


def plot_iteration_times(iteration_durations):
    """
    Plot individual iteration durations and cumulative elapsed time.
    """
    iterations = np.arange(1, len(iteration_durations) + 1)
    cumulative_times = np.cumsum(iteration_durations)

    plt.figure(figsize=(12, 5))

    # Plot individual iteration durations.
    plt.subplot(1, 2, 1)
    plt.plot(iterations, iteration_durations, marker='o', linestyle='-', label='Iteration Duration')
    plt.xlabel("Iteration")
    plt.ylabel("Time (s)")
    plt.title("Individual Iteration Durations")
    plt.legend()
    plt.grid(True)

    # Plot cumulative elapsed time.
    plt.subplot(1, 2, 2)
    plt.plot(iterations, cumulative_times, marker='o', linestyle='-', label='Cumulative Time')
    plt.xlabel("Iteration")
    plt.ylabel("Cumulative Time (s)")
    plt.title("Cumulative Optimization Time")
    plt.legend()
    plt.grid(True)

    plt.tight_layout()
    plt.show()


def plot_semilog_cumulative(iteration_durations):
    """
    Plot cumulative elapsed time using a semilog scale.
    """
    iterations = np.arange(1, len(iteration_durations) + 1)
    cumulative_times = np.cumsum(iteration_durations)

    plt.figure(figsize=(6, 5))
    plt.semilogy(iterations, cumulative_times, marker='o', linestyle='-', label='Cumulative Time (log scale)')
    plt.xlabel("Iteration")
    plt.ylabel("Cumulative Time (s, log scale)")
    plt.title("Semilog Plot of Cumulative Optimization Time")
    plt.legend()
    plt.grid(True)
    plt.show()


def plot_parameter_effects(res, space):
    """
    Plot the effect of each parameter on the score.

    For continuous parameters a scatter plot is created; for categorical parameters,
    a box plot is used.
    """
    param_names = [dim.name for dim in space]
    # Create a DataFrame with one row per evaluation.
    df = pd.DataFrame(res.x_iters, columns=param_names)
    # Convert the minimized objective back to positive scores.
    df['score'] = -np.array(res.func_vals)

    for param in param_names:
        # Create a new figure and axis explicitly
        fig, ax = plt.subplots()
        if df[param].dtype == object:
            df.boxplot(column='score', by=param, ax=ax)
            ax.set_title(f"Score by {param}")
            ax.set_xlabel(param)
            ax.set_ylabel("Score")
            # Remove the automatic suptitle that pandas adds.
            plt.suptitle("")
        else:
            ax.scatter(df[param], df['score'])
            ax.set_title(f"Score vs {param}")
            ax.set_xlabel(param)
            ax.set_ylabel("Score")
        ax.grid(True)
        plt.show()


class Receiver:
    """
    Handle incoming connections and JSON message reception.
    """

    def __init__(self, host: str, port: int, buffer_size: int = 1024):
        self.host = host
        self.port = port
        self.buffer_size = buffer_size
        self.server_socket = None
        self.connection = None
        self.reader = None

    def start_server(self):
        """
        Set up a server socket, bind it to the host and port, listen for one connection,
        accept it, and wrap the connection in a file-like reader for line-based JSON input.
        """
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(1)
        logging.info("Listening on %s:%d for incoming connection...", self.host, self.port)
        while True:
            try:
                self.connection, addr = self.server_socket.accept()
                break
            except socket.error as e:
                logging.warning("Port not open yet, retrying...")
                time.sleep(1)
        logging.info("Accepted connection from %s", addr)
        self.reader = self.connection.makefile("r")

    def receive_json(self) -> dict:
        """
        Continuously read a line from the connection until valid JSON is decoded.
        Returns:
            A dictionary containing the JSON data.
        """
        while True:
            line = self.reader.readline()
            if not line:
                logging.warning("No data received, waiting...")
                time.sleep(0.5)
                continue
            try:
                message = json.loads(line)
                # logging.info("Received JSON: %s", message)
                return message
            except json.JSONDecodeError:
                logging.error("Error decoding JSON, waiting for complete data...")
                time.sleep(0.5)

    def cleanup(self):
        """
        Close the server socket and connection.
        """
        if self.server_socket:
            self.server_socket.close()
        if self.connection:
            self.connection.close()


class Sender:
    """
    Handle outgoing connections and sending JSON messages.
    """

    def __init__(self, host: str, port: int):
        self.host = host
        self.port = port
        self.socket = None
        self.writer = None

    def connect(self):
        """
        Create a socket connection to the host and port, and wrap it with
        a file-like object for sending JSON messages.
        """
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect((self.host, self.port))
        self.writer = self.socket.makefile("w")
        # logging.info("Connected to %s:%d for sending data.", self.host, self.port)

    def send_json(self, data: dict):
        """
        Encode the provided data as JSON, append a newline, write it to the connection,
        and flush the writer to ensure the data is sent immediately.
        Args:
            data (dict): The data to send.
        """
        payload = json.dumps(data) + "\n"
        self.writer.write(payload)
        self.writer.flush()
        # logging.info("Sent data: %s", data)

    def cleanup(self):
        """
        Close the writer and the underlying socket.
        """
        if self.writer:
            self.writer.close()
        if self.socket:
            self.socket.close()


class Optimizer:
    """
    Encapsulate the optimization loop. Uses a Sender to send parameter
    updates and a Receiver to wait for new scores until a desired threshold is reached.
    """

    def __init__(self, sender: Sender, receiver: Receiver, possible_values: dict, start_time):
        self.sender = sender
        self.receiver = receiver
        self.possible_values = possible_values
        self.start_time = start_time
        self.iteration_durations = []
        self.last_callback_time = None
        self.res = None  # Will store the optimization result.
        self.space = None  # Will store the search space.

    def objective_function(self, params: dict) -> float:
        """
        Send parameters and wait for a JSON response containing a score.
        Args:
            params (dict): The parameters to evaluate.
        Returns:
            float: The score received from the response.
        """
        # logging.info("Sending new parameters: %s", params)
        self.sender.send_json(params)
        # logging.info("Waiting for new score...")
        result = self.receiver.receive_json()
        score = result.get("score")
        while score is None:
            logging.warning("Score not found in response, waiting for valid score...")
            result = self.receiver.receive_json()
            score = result.get("score")
        # logging.info("Received new score: %f", score)
        return score

    def estimate_total_time_quadratic(self, total_iterations: int) -> float:
        """
        Fit a quadratic to the cumulative times and predict total time at total_iterations.
        """
        current_iter = len(self.iteration_durations)
        if current_iter < 3:
            if current_iter == 0:
                return 0
            elapsed = sum(self.iteration_durations)
            return elapsed / current_iter * total_iterations

        x = np.arange(1, current_iter + 1)
        y = np.cumsum(self.iteration_durations)
        a, b, c = np.polyfit(x, y, 2)
        predicted_total = a * (total_iterations ** 2) + b * total_iterations + c

        actual_so_far = y[-1]
        if predicted_total < actual_so_far:
            logging.warning("Quadratic fit predicted total < actual so far; falling back to average.")
            predicted_total = (actual_so_far / current_iter) * total_iterations

        return predicted_total

    def log_progress(self, iteration_count: int, total_iterations: int):
        """
        Log the progress of the optimization process.
        """
        elapsed_wall_time = time.time() - self.start_time
        predicted_total_time = self.estimate_total_time_quadratic(total_iterations)
        est_remaining_time = predicted_total_time - elapsed_wall_time
        percentage = (iteration_count / total_iterations) * 100

        logging.info(
            "Progress: %d/%d (%.2f%%) - Elapsed: %s - Estimated total: %s - Estimated remaining: %s",
            iteration_count,
            total_iterations,
            percentage,
            format_time(elapsed_wall_time),
            format_time(predicted_total_time),
            format_time(est_remaining_time),
        )

    def all_params_are_categorical(self):
        """
        Return True if all possible_values lists do not contain 'normalizedValue'
        """
        return all("normalizedValue" not in vals for vals in self.possible_values.values())

    def optimize(self) -> dict:
        """
        Run either brute-force search (all categorical) or Bayesian optimization (any continuous).
        """
        if self.all_params_are_categorical():
            # --- Brute-force all permutations ---
            keys = list(self.possible_values.keys())
            values = [self.possible_values[k] for k in keys]
            all_combinations = list(itertools.product(*values))
            logging.info("Brute-force: evaluating %d combinations...", len(all_combinations))

            results = []
            self.iteration_durations = []
            start_time = time.time()

            for i, combo in enumerate(all_combinations, 1):
                params = {k: str(v) for k, v in zip(keys, combo)}
                t0 = time.time()
                score = self.objective_function(params)
                duration = time.time() - t0
                self.iteration_durations.append(duration)
                results.append((params, score))
                logging.info("Evaluated %d/%d: %s => Score: %f (%.2fs)", i, len(all_combinations), params, score, duration)

                elapsed = time.time() - start_time
                percent = (i / len(all_combinations)) * 100
                logging.info("Progress: %.2f%% | Elapsed: %s", percent, format_time(elapsed))

            best_params, best_score = max(results, key=lambda x: x[1])
            logging.info("Best parameters: %s with score: %f", best_params, best_score)

            plot_parameter_effects_bruteforce(results, keys)
            plot_parameter_pairwise_heatmaps(results, keys)

            return best_params

        else:
            # --- Bayesian optimization (skopt) ---
            space = []
            for key, value in self.possible_values.items():
                if "normalizedValue" in value:
                    space.append(Real(0, 1, name=key))
                else:
                    space.append(skopt.space.space.Categorical(value, name=key))
            self.space = space

            def objective(params):
                param_dict = {dim.name: str(val) for dim, val in zip(space, params)}
                score = self.objective_function(param_dict)
                logging.info("Score for %s: %f", param_dict, score)
                return -score

            total_iterations = 60

            def progress_callback(res):
                current_time = time.time()
                if self.last_callback_time is not None:
                    duration = current_time - self.last_callback_time
                    self.iteration_durations.append(duration)
                self.last_callback_time = current_time
                iteration_count = len(res.x_iters)
                self.log_progress(iteration_count, total_iterations)

            res = gp_minimize(
                objective,
                space,
                n_calls=total_iterations,
                random_state=42,
                callback=progress_callback
            )
            self.res = res

            def convert(value):
                return value.item() if hasattr(value, "item") else value

            best_params = {dim.name: convert(res.x[i]) for i, dim in enumerate(space)}
            best_score = -res.fun

            logging.info("Best parameters found: %s with score %f", best_params, best_score)
            logging.info("Optimization finished in %.2f seconds", time.time() - self.start_time)
            return best_params


def main():
    """
    Main function to set up connections, receive initial data, and run the optimization loop.
    """
    logging.info("Program execution started.")

    receiver = Receiver(HOST, LISTEN_PORT)
    try:
        logging.info("Setting up receiving connection...")
        receiver.start_server()
        logging.info("Waiting for initial data...")
        initial_data = receiver.receive_json()
        while initial_data.get("score") is None:
            logging.warning("Initial data did not include a score. Waiting for valid data...")
            initial_data = receiver.receive_json()
        initial_score = initial_data.get("score")
        possible_values = initial_data.get("possible_values", {})
        logging.info("Initial data received - Score: %s, Possible values: %s", initial_score, possible_values)
    except Exception as e:
        logging.error("Error during receiving initial data: %s", e)
        receiver.cleanup()
        return

    # Close the server socket after initial data is received.
    if receiver.server_socket:
        receiver.server_socket.close()

    sender = Sender(HOST, SEND_PORT)
    try:
        sender.connect()
    except Exception as e:
        logging.error("Error connecting for sending: %s", e)
        receiver.cleanup()
        return

    start_time = time.time()
    optimizer = Optimizer(sender, receiver, possible_values, start_time)
    best_params = optimizer.optimize()
    # logging.info("Optimization complete. Best parameters found: %s", best_params)

    # Plot iteration durations and the cumulative time.
    plot_iteration_times(optimizer.iteration_durations)
    plot_semilog_cumulative(optimizer.iteration_durations)
    # Plot the effect of each parameter on the score.
    if optimizer.res is not None and optimizer.space is not None:
        plot_parameter_effects(optimizer.res, optimizer.space)

    sender.cleanup()
    receiver.cleanup()
    logging.info("Program execution finished.")


if __name__ == "__main__":
    main()
