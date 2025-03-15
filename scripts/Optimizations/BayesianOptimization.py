import itertools
import json
import logging
import math
import socket
import time

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


class Receiver:
    """
    A class to handle incoming connections and JSON message reception.
    It sets up a server socket, accepts a connection, and provides
    a method to continuously receive valid JSON messages.
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
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(1)
        logging.info("Listening on %s:%d for incoming connection...", self.host, self.port)
        self.connection, addr = self.server_socket.accept()
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
                logging.info("Received JSON: %s", message)
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
    A class to handle outgoing connections and sending JSON messages.
    It connects to a specified host and port and wraps the connection
    in a file-like writer for convenient output.
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
        logging.info("Connected to %s:%d for sending data.", self.host, self.port)

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
        logging.info("Sent data: %s", data)

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
    A class encapsulating the optimization loop. It uses a Sender to send parameter
    updates and a Receiver to wait for new scores until a desired threshold is reached.
    """

    def __init__(self, sender: Sender, receiver: Receiver, possible_values: dict):
        self.sender = sender
        self.receiver = receiver
        self.possible_values = possible_values

    def objective_function(self, params: dict) -> float:
        """
        Send parameters and wait for a JSON response containing a score.
        Args:
            params (dict): The parameters to evaluate.
        Returns:
            float: The score received from the response.
        """
        logging.info("Sending new parameters: %s", params)
        self.sender.send_json(params)
        logging.info("Waiting for new score...")
        result = self.receiver.receive_json()
        score = result.get("score")
        while score is None:
            logging.warning("Score not found in response, waiting for valid score...")
            result = self.receiver.receive_json()
            score = result.get("score")
        logging.info("Received new score: %f", score)
        return score

    def log_progress(self, iteration_count: int, total_iterations: int, start_time: float):
        """
        Calculate and log progress information.
        """
        elapsed_time = time.time() - start_time
        avg_time = elapsed_time / iteration_count
        remaining_iterations = total_iterations - iteration_count
        est_remaining_time = avg_time * remaining_iterations
        percentage = (iteration_count / total_iterations) * 100

        formatted_elapsed = format_time(elapsed_time)
        formatted_remaining = format_time(est_remaining_time)

        logging.info(
            "Progress: %d/%d (%.2f%%) - Elapsed: %s - Estimated remaining: %s",
            iteration_count,
            total_iterations,
            percentage,
            formatted_elapsed,
            formatted_remaining,
        )

    def optimize(self, initial_score: float) -> dict:
        """
        Evaluate all possible parameter combinations and return the best parameters.
        Args:
            initial_score (float): The starting score to compare against.
        Returns:
            dict: The parameters that resulted in the best (lowest) score.
        """
        best_score = initial_score
        best_params = {}
        keys = list(self.possible_values.keys())
        total_iterations = math.prod(len(self.possible_values[key]) for key in keys)
        all_combinations = itertools.product(*(self.possible_values[key] for key in keys))
        start_time = time.time()
        iteration_count = 0

        for combination in all_combinations:
            iteration_count += 1
            params = dict(zip(keys, combination))
            logging.info("Evaluating parameters: %s", params)
            score = self.objective_function(params)
            logging.info("Score for %s: %f", params, score)
            if score > best_score:
                best_score = score
                best_params = params

            self.log_progress(iteration_count, total_iterations, start_time)

        logging.info("Best parameters found: %s with score %f", best_params, best_score)
        return best_params


def main():
    """
    Main function to set up connections, receive initial data, and run the optimization loop.
    Ensures that all connections are properly cleaned up at the end.
    """
    receiver = Receiver(HOST, LISTEN_PORT)
    try:
        logging.info("Setting up receiving connection...")
        receiver.start_server()
        logging.info("Waiting for initial data from Java...")
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

    if receiver.server_socket:
        receiver.server_socket.close()

    sender = Sender(HOST, SEND_PORT)
    try:
        sender.connect()
    except Exception as e:
        logging.error("Error connecting for sending: %s", e)
        receiver.cleanup()
        return

    optimizer = Optimizer(sender, receiver, possible_values)
    best_params = optimizer.optimize(initial_score)
    logging.info("Optimization complete. Best parameters found: %s", best_params)

    sender.cleanup()
    receiver.cleanup()


if __name__ == "__main__":
    main()
