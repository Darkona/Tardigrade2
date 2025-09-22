## Overview

Tardigrade is a testing tool for HTTP clients. It allows you to point your HTTP clients to Tardigrade to observe and log the requests they make. Additionally, you can write these requests to a file for further analysis.

## Features

- **Colorful Console Output**: Utilizes ANSI codes for enhanced console display.
- **UTF-8 Detection**: Automatically detects and adjusts for UTF-8 console compatibility.
- **Configurable Directories**: Allows setting custom input and output directories.
- **Customizable Port**: Server port can be configured via the configuration file.

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 11 or higher
- Gradle

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Darkona/Tardigrade2.git
   ```
2. Navigate to the project directory:
   ```bash
   cd Tardigrade
   ```
3. Build the project using Gradle:
   ```bash
   gradle build
   ```

### Running the Server

To start the server, execute the following command:
```bash
java -jar build/libs/tardigrade.jar
```

### Command Line Arguments

The application supports several command line arguments for configuration:

- **`-p` or `--port`**: Specifies the server port. Default is `8050`.
- **`-o` or `--output`**: Sets the output directory for writing files.
- **`-i` or `--input`**: Sets the input directory for loading files.
- **`-q` or `--quiet`**: Enables quiet mode, suppressing console output.
- **`-d` or `--disable`**: Disables specific features. Accepts multiple arguments such as `color`, `header`, and `body` to disable color output, headers, and body content respectively.

### Configuration

The server can be configured using the `configuration.yml` file located in the `src/main/resources` directory. Key configurations include:

- **Port**: The port on which the server will run.
- **Input/Output Directories**: Directories for reading and writing files.

## Handlers and Endpoints

The application uses several handlers to manage different types of HTTP requests:

- **LogHandler**: Logs HTTP request details such as headers, body, and method.
- **ReadHandler**: Manages reading files from the server, serving directories, and handling file not found errors.
- **WriteHandler**: Handles writing data to the server.

### Endpoints

- `/read`: Uses `ReadHandler` to handle read requests.
- `/write`: Uses `WriteHandler` to handle write requests.

## License

This project is licensed under the MIT License. See the full license text below:

```
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Contact

For more information, please contact the project maintainer at [Darkona](https://github.com/Darkona).
