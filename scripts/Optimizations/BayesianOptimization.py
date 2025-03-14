import json
import random
import socket
import time

HOST = "localhost"
LISTEN_PORT = 5003
SEND_PORT = 5005

def setup_receiving_connection():
    """
    Set up a server socket on LISTEN_PORT and accept one connection.
    Returns the server socket (to be closed after accepting)
    and the accepted connection (used for all receives).
    """
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((HOST, LISTEN_PORT))
    server_socket.listen(1)
    print(f"Listening on {HOST}:{LISTEN_PORT} for incoming connection...")
    conn, addr = server_socket.accept()
    print("Accepted connection from", addr)
    return server_socket, conn

def setup_sending_connection():
    """
    Create a persistent connection to SEND_PORT for sending data.
    Wrap the socket with a file-like object for easier flushing.
    """
    send_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    send_sock.connect((HOST, SEND_PORT))
    send_file = send_sock.makefile("w")  # Create a writable file-like object
    print(f"Connected to {HOST}:{SEND_PORT} for sending data.")
    return send_sock, send_file

def receive_json_from_connection(conn):
    """
    Block until valid JSON data is received from the provided connection.
    """
    while True:
        data = conn.recv(1024)
        if not data:
            print("No data received, waiting...")
            time.sleep(0.5)
            continue
        try:
            message = json.loads(data.decode('utf-8'))
            print("Received JSON:", message)
            return message
        except json.JSONDecodeError:
            print("Error decoding JSON, waiting for complete data...")
            time.sleep(0.5)

def send_json_over_connection(send_file, data):
    """
    Encode the given data as JSON, write it to the provided file-like object,
    and flush to ensure the data is sent immediately.
    """
    payload = json.dumps(data) + "\n"
    send_file.write(payload)
    send_file.flush()  # Flush to send data immediately
    print("Data sent over persistent connection.")

def objective_function(send_file, receiving_conn, params):
    """
    Send new parameters over the persistent sending connection and wait for a valid new score.
    """
    print("Sending new parameters to Java...")
    send_json_over_connection(send_file, params)

    print("Waiting for new score from Java...")
    result = receive_json_from_connection(receiving_conn)
    score = result.get("score")
    while score is None:
        print("Score not found in received data; waiting for valid response...")
        result = receive_json_from_connection(receiving_conn)
        score = result.get("score")
    print(f"Received new score: {score}")
    return score

def optimization(send_file, receiving_conn, initial_score, possible_values):
    """
    Loop to adjust parameters until the score falls below -0.8.
    Chooses a random value for each parameter from the provided possible values.
    """
    score = initial_score
    while score > -0.8:
        print(f"Current score: {score}")
        new_params = {key: random.choice(values) for key, values in possible_values.items()}
        print("Evaluating parameters:", new_params)
        score = objective_function(send_file, receiving_conn, new_params)
    return new_params

def main():
    # Set up the receiving connection and wait for the initial data.
    print("Setting up receiving connection...")
    server_socket, receiving_conn = setup_receiving_connection()
    print("Waiting for initial data from Java...")
    initial_data = receive_json_from_connection(receiving_conn)
    while initial_data.get("score") is None:
        print("Initial data did not include a score. Waiting for valid data...")
        initial_data = receive_json_from_connection(receiving_conn)
    score = initial_data.get("score")
    possible_values = initial_data.get("possible_values", {})

    print("Initial data received:")
    print(" Score:", score)
    print(" Possible values:", possible_values)

    # We can close the server socket now since the connection is accepted.
    server_socket.close()

    # Connect to Java for sending only after receiving the initial data.
    send_sock, send_file = setup_sending_connection()

    best_params = optimization(send_file, receiving_conn, score, possible_values)
    print("Optimization complete. Best parameters found:")
    print(best_params)

    # Clean up persistent connections.
    send_file.close()
    send_sock.close()
    receiving_conn.close()

if __name__ == "__main__":
    main()
