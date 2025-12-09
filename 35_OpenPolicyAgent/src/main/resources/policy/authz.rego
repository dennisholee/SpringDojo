package authz

default allow := false

signing_key := "secret"

allow if {
	input.user == "alice"
	input.action == "read"
	input.resource == "/data/sensitive"

	token := input.token



	[_, claims, _] := io.jwt.decode(token)

    print("claims:", claims)

	claims.role == "user1"
}
