import json
import random
import re

import socket
import sys
import time

HOST = "localhost"
PORT = 5003


def optimization(score, current_params, possible_values):
    """
    double
    Score needs to be maximized
    To do this we need to minimize the negative score

    current_params:
    {
        "fixpoint": "C",
        "INDV2": "false",
        "INDV1": "false",
        "FDV1": "false",
        "FDV2": "false",
       "UCCV2": "false",
       "UCCV1": "false"
    }

    possible_values:
    {
        "fixpoint": ["A", "B", "C"],
        "INDV2": ["true", "false"],
        "INDV1": ["true", "false"],
        "FDV1": ["true", "false"],
        "FDV2": ["true", "false"],
        "UCCV2": ["true", "false"],
        "UCCV1": ["true", "false"]
    }
    """
    print("Optimizing...")
    # result = gp_minimize(objective_function, list(possible_values.values()), n_calls=30, random_state=42)
    # best_params = {key: val for key, val in zip(possible_values.keys(), result.x)}

    while score > -.8:

        # Random placeholder for categorical values
        # for every categorical value, we choose a random value from the possible values
        new_params = {key: random.choice(possible_values[key]) for key in possible_values}
        print("new params: ", new_params)

        score = objective_function(new_params)

    return new_params


# Define the function to maximize
def objective_function(params):
    """
    Send parameters to Java and get the score.
    """
    print("Calling objective function")
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as client:
        print("Connecting to Java server")
        client.connect((HOST, PORT))
        print("Connected to Java server")
        param_str = json.dumps(params)
        # Wait for one second to avoid connection issues
        time.sleep(1)

        print("Sending params: ", param_str)
        client.sendall((param_str + "\n").encode())
        print("Sent params: ", param_str)
        print("Waiting for response...")
        data = client.recv(1024).decode()
        print("Received data: ", data)
        parts = data.split(" ", 1)
        score = float(parts[0])
        print(f"Score: {score}")
    return -score  # We minimize in Bayesian Optimization, so negate it


def main():
    print("Python Optimizer started")
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server:
        print("Server started")
        server.bind((HOST, PORT))
        server.listen()

        # Listen to initial connection and receive data
        print("Listening on {}:{}".format(HOST, PORT))
        print("Waiting for connection...")
        conn, addr = server.accept()
        with conn:
            print("Connected by", addr)
            data = conn.recv(1024).decode()
            while not data:
                data = conn.recv(1024).decode()
            print("Received data: ", data)
            parts = data.split("|")
            score = float(parts[0])
            params = json.loads(parts[1])
            possible_values = json.loads(parts[2])
            print("Score: ", score)
            print("Params: ", params)
            print("Possible values: ", possible_values)

            # Start optimization with initial parameters
            optimization(score, params, possible_values)


if __name__ == "__main__":
    main()
