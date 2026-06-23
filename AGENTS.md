# Repository Guidelines

## Project Structure & Module Organization
`rpc-core` is a Java 17 Maven repository centered on a custom RPC framework. The parent reactor in [pom.xml](D:\14297\idea_projects\rpc-core\pom.xml) includes `common`, `consumer`, `provider`, `rpc-easy`, and `rpc-extend`. Shared models and service interfaces live in `common/src/main/java`. Core protocol, registry, serializer, and transport code lives in `rpc-extend/src/main/java`. `consumer` and `provider` contain simple usage examples.

`my-rpc-springboot-starter`, `exam-consumer-springboot`, and `exam-provider-springboot` are separate Maven projects beside the main reactor. Treat them as integration samples for the starter rather than part of the root build.

## Build, Test, and Development Commands
Use Maven from the repo root for reactor modules:

- `mvn clean test` - build and run tests for the root modules.
- `mvn clean install` - install reactor artifacts to the local Maven repository.
- `mvn -pl rpc-extend -am test` - test one module and its dependencies.

For the Spring Boot samples, run commands from each sample directory:

- `./mvnw test` or `mvnw.cmd test` - run sample tests.
- `./mvnw spring-boot:run` - start a sample app locally.

Build `my-rpc-springboot-starter` before running the sample apps so its artifact is available locally.

## Coding Style & Naming Conventions
Follow the existing Java style: 4-space indentation, UTF-8 source files, `com.yzy...` packages, `UpperCamelCase` class names, and `lowerCamelCase` methods and fields. Keep interfaces in shared modules when possible. Match current suffix patterns such as `*Factory`, `*Serializer`, `*Register`, and `*Handler`.

The repo uses Lombok in several modules. No formatter or linter config is committed, so keep imports tidy and avoid unrelated reformatting.

## Testing Guidelines
Most reactor modules use JUnit 4 with tests under `src/test/java` and names ending in `*Test`. Spring Boot samples use `spring-boot-starter-test` and JUnit 5. Add tests next to the module you change, especially for protocol handling, registry behavior, retries, and serialization paths.

## Commit & Pull Request Guidelines
Recent history uses short, change-focused commit messages, often describing the affected component directly. Prefer messages like `rpc-extend: fix TcpBufferHandlerWrapper callback handling` over vague titles such as `update`.

Pull requests should state the affected module(s), summarize behavior changes, list validation steps, and note any config needed to run examples locally. Include logs or screenshots only when changing sample app behavior or startup flows.
