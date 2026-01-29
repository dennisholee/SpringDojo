# Kong Gateway

Register new service:
```
curl --location 'http://localhost:8001/services' \
--form 'name="forest_security"' \
--form 'url="http://localhost:8080"'
```
