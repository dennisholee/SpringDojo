
To convert policy to a `wasm` type

```shell
docker run --rm  -v "$(pwd):/policy"  openpolicyagent/opa build -t wasm -e authz/allow /policy -o /policy/bundle.tar.gz
```

Get OPA capabilities

```shell
docker run --rm  -v "$(pwd)/src/main/resources/policy:/policy"  openpolicyagent/opa capabilities --current
```

Interactive mode

```shell
docker run -it --rm  -v "$(pwd)/src/main/resources/policy:/policy"  openpolicyagent/opa run
```

To get the current version:

```shell
> data.system.version
```

```json
{
  "build_commit": "45cbfa1d83841971d0db96b7803b5aeeae91020e",
  "build_hostname": "",
  "build_timestamp": "2025-11-26T13:01:54Z",
  "version": "1.11.0"
}
```
