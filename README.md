## Overview

Tardigrade is a testing tool for HTTP clients. Point a client at Tardigrade to see every
request it makes, write those requests to disk, and answer them with canned responses so you
can exercise an application without its real dependencies.

## Features

- **Request logging**: method, URI, headers and body of every request, on any path.
- **Mock responses**: map a URL to a file (or to a body written in the config) and answer with it.
- **File serving and capture**: read files from an input directory, write request bodies to an output directory.
- **Colorful console output**: ANSI codes, switchable off.
- **Configurable**: command line flags or a YAML file.

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
   cd Tardigrade2
   ```
3. Build the project using Gradle:
   ```bash
   gradle build
   ```

### Running the Server

The build leaves the jar and its resources under `build/libs`:

```bash
java -jar build/libs/tardigrade.jar
```

`configuration.yml`, `input/` and `output/` are resolved against the directory holding the jar,
not against the working directory, so the whole folder can be copied elsewhere and run from
anywhere. Absolute paths given to `-i` or `-o` are used as they are.

### Command Line Arguments

- **`-p` or `--port`**: Server port. Default `8050`. Use `0` to take any free port.
- **`-o` or `--output`**: Output directory for written files. Default `output`.
- **`-i` or `--input`**: Input directory for served files and mock bodies. Default `input`.
- **`-c` or `--config`**: Configuration file to read instead of the one next to the jar.
- **`-q` or `--quiet`**: Quiet mode, suppresses the startup banner.
- **`-d` or `--disable`**: Disables features. Accepts `color`, `header` and `body`, in any
  combination: `-d color header`.

## Configuration

Tardigrade reads `configuration.yml` from the directory the jar sits in, and falls back to the
copy bundled in the jar. Command line flags win over the file, and the file wins over the
built-in defaults.

```yaml
port: 8050
input: input
output: output
color: true
loglevel: info
```

## Mock Responses

Declare pairs of URL and response under `mocks`. When a request matches, Tardigrade logs it as
usual and answers with the content instead of the default acknowledgement.

```yaml
mocks:
  - path: /clientes/42
    file: cliente-42.json

  - path: /clientes/*          # prefix match
    file: cliente-generico.json

  - path: /pagos
    method: POST               # limits the mock to one verb
    file: pago-creado.json
    status: 201

  - path: /health
    body: '{"status":"UP"}'    # written inline instead of in a file
```

- `file` is resolved against the input directory. `body` is an alternative to it.
- `status` defaults to `200`. `method` defaults to any verb.
- The Content-Type comes from the file extension, or from the shape of an inline body. Set
  `contentType` to override it.
- An exact path always beats a wildcard; among wildcards, the longest prefix wins.
- Missing mock files are reported at startup and answered with a 500 that names the file.
- Mocks cannot shadow `/read`, `/write` and `/log`, which are reserved.

### Live reload

The configuration file is watched while the server runs. Edit it and the mock table is rebuilt
without a restart, so responses can be adjusted in the middle of a test run.

```
INFO | Config | Watching .../configuration.yml for changes.
INFO | Mocks  | Loaded 2 mock route(s).
```

A file that cannot be parsed leaves the running configuration untouched and says so, instead of
dropping every mock over a typo. `port` and `color` are read once at startup and need a restart.

## Handlers and Endpoints

- `/read`: serves files from the input directory. A trailing slash lists the directory.
  Unknown files return 404.
- `/write`: `POST`, `PUT` or `PATCH` stores the request body in the output directory.
  `/write/name.json` writes `name.json`; without a name, the file is named after the timestamp
  and the Content-Type. Other verbs return 405.
- `/log`: logs the request and acknowledges it.
- Every other path falls through to the mock table, and then to logging.

## Embedded Use

The server has a life cycle of its own and holds no static state, so a test can start one on a
free port, drive it, and stop it. Every request it answers is kept in memory to assert on.

```java
var config = new TardigradeConfiguration(new String[]{"-p", "0", "-c", "mocks.yml"});
var server = new TardigradeServer(config);
int port = server.start();          // the port the OS handed out

client.call(server.baseUrl() + "/api/clientes/42");

var received = server.requests().last().orElseThrow();
assertEquals("GET", received.method());
assertTrue(received.hasHeader("X-CORRELACION-ID"));

server.stop();
```

`requests()` gives the log: `all()`, `forPath(path)`, `last()`, `last(path)`, `count()`,
`count(path)` and `clear()`. It keeps the last 1000 requests, so a long-running server does not
grow without bound. `/read` and `/write` are utility endpoints and stay out of the log.

The project is not published anywhere. To use it from another build, point at the jar directly:

```gradle
testImplementation files("C:/programs/tardigrade/tardigrade.jar")
```

The jar bundles its own dependencies, so nothing else needs declaring. It also bundles logback
and jansi, which will meet whatever the consuming project already uses. Splitting a slim
`tardigrade-core` out of the executable jar is the step to take if that ever bites.

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
