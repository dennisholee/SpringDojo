# Rabbit MQ Four Exchange Types

1. An available Rabbit MQ Environment. Refer to https://github.com/dennisholee/RabbitMQDojo/tree/main/01_Install/ansible_home for ansible playbook.
2. Expose RabbitMQ service locally if service is remote `ssh -L 5672:localhost:5672 ${ssh_user}@${remote_host}`.
3. Update environment variables according to the corresponding table below to select the exchange type to execute.
4. Run from IDE or package and then run the Spring Boot application.

Environment variables

|Exchange Type|Environment Variable|
|-|-|
|Direct|spring_profiles_active=direct|
|Fanout|spring_profiles_active=fanout|
|Headers|spring_profiles_active=headers|
|Topic|spring_profiles_active=topic|
