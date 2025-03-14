import json
import random

import socket
import time

HOST = "localhost"
LISTEN_PORT = 5004
SEND_PORT = 5005

# 1. When starting, will listen to initial JSON data from Java
# 2. We parse initial JSON data and save possible values
# 3. We start the optimization process to find the next parameters
# 4. We send the new parameters back to Java via the objective function
# 5. We listen to the new score from Java in the objective function
# 6. We return the new score to the optimization process
# 7. We repeat the process until the score is below a certain threshold
# We use a different connection for sending and receiving data

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
        print("Score: ", score)
        # Random placeholder for categorical values
        # for every categorical value, we choose a random value from the possible values
        print("Possible values: ", possible_values)
        new_params = {key: random.choice(possible_values[key]) for key in possible_values}
        print("new params: ", new_params)

        print("Evaluated new params")
        score = objective_function(new_params)

    return new_params


def objective_function(params):
    # Send new parameters to Java
    print("Sending new params to Java...")
    print("Starting connection...")
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        print("Connecting to port", SEND_PORT)
        s.connect((HOST, SEND_PORT))
        print("Connected")
        print("Sending data...")
        s.sendall(json.dumps(params).encode('utf-8') + b'\n')
        print("Data sent")
        print("Closing connection...")
        s.close()
    print("Connection closed")

    # Receive new score from Java
    print("Receiving new score from Java...")
    print("Starting connection...")
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        print("Binding to port", LISTEN_PORT)
        server_socket.bind((HOST, LISTEN_PORT))
        print("Listening...")
        server_socket.listen()
        print("Waiting for connection...")
        conn, addr = server_socket.accept()
        with conn:
            print("Connected by", addr)

            # Receive data
            data = conn.recv(1024)
            if not data:
                print("No data received")
            print("Received data:", data)

            # Decode JSON data
            try:
                received_json = json.loads(data.decode('utf-8'))
                print("Received JSON:", received_json)
            except json.JSONDecodeError:
                print("Invalid JSON received")

            # Extract values
            score = received_json.get("score", 1.0)  # Default to 1 if missing
            print("Score:", score)
        print("Closing connection...")
        conn.close()
    print("Closing server socket...")
    server_socket.close()

    return score

if __name__ == "__main__":
    print("Starting optimization...")
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server_socket:
        print("Binding to port", LISTEN_PORT)
        server_socket.bind((HOST, LISTEN_PORT))
        print("Listening...")
        server_socket.listen()
        print("Waiting for connection...")
        conn, addr = server_socket.accept()
        with conn:
            print("Connected by", addr)

            # Receive data
            data = conn.recv(1024)
            if not data:
                print("No data received")
            print("Received data:", data)

            # Decode JSON data
            try:
                received_json = json.loads(data.decode('utf-8'))
                print("Received JSON:", received_json)
            except json.JSONDecodeError:
                print("Invalid JSON received")

            # Extract values
            score = received_json.get("score", 1.0)  # Default to 1 if missing
            print("Score:", score)
            current_params = received_json.get("current_params", {})
            print("Current params:", current_params)
            possible_values = received_json.get("possible_values", {})
            print("Possible values:", possible_values)
        print("Closing connection...")
        conn.close()
    print("Closing server socket...")
    server_socket.close()

    # Start optimization
    print("Starting optimization...")
    optimization(score, current_params, possible_values)
