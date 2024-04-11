# Server Implementation README

## Overview

This server is designed as a multithreaded TCP server, capable of handling HTTP requests with compliance with basic HTTP standards. Below is an overview of the implemented classes and the server's design philosophy.

## Classes Overview

- **TCPServerMultithreaded.java:** Acts as the server's backbone, setting up a server socket and listening for incoming connections. On accepting a connection, it spawns a new thread using EchoRunnable to handle the request, ensuring concurrent handling of multiple clients.

- **HTTPRequest.java:** Represents an HTTP request, parsing and storing its various components like method type (GET, POST, etc.), requested resource, headers, and parameters. It includes validation to ensure requests are well-formed and to prevent basic security vulnerabilities.

- **EchoRunnable.java:** Implements the Runnable interface, handling the logic for each client connection in a separate thread. It reads the client's request, processes it according to HTTP standards, and formulates an appropriate response.

## Running the Server

To run the server on a Linux machine, follow these steps:

1. Ensure you have Java Development Kit (JDK) installed on your system.
2. Clone this repository to your local machine.
3. Navigate to the root directory of the cloned repository.
4. Make the compile.sh and run.sh scripts executable by running the following commands:

    ```bash
    $ chmod +x ./compile.sh ./run.sh
    ```

5. Run the compile.sh script to compile the Java source files:

    ```bash
    $ ./compile.sh
    ```

6. Once the compilation is successful, run the run.sh script to start the server:

    ```bash
    $ ./run.sh
    ```

7. The server will start listening for incoming connections on the specified port.

## Design Philosophy

The server's design emphasizes modularity and simplicity, allowing for easy extension and maintenance. By decoupling the connection handling (EchoRunnable) from the server logic (TCPServerMultithreaded) and request parsing (HTTPRequest), the system facilitates clear separation of concerns. This not only aids in debugging and development but also enhances the server's ability to evolve and include more features, such as additional HTTP methods or more complex routing logic.

A multithreaded approach was chosen to efficiently manage multiple simultaneous connections, ensuring that the server can serve multiple clients without significant latency. This is crucial for high-traffic environments and improves the overall user experience by reducing wait times.

## Conclusion

This server implementation provides a simple yet useful, scalable foundation for web applications, balancing performance with maintainability and security.
