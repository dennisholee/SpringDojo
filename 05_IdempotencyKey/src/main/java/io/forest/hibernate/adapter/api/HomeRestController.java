package io.forest.hibernate.adapter.api;

import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.forest.hibernate.common.idempotency.annotation.IdempotencyKey;
import io.forest.hibernate.common.idempotency.annotation.Idempotent;

@RestController
@RequestMapping("/")
public class HomeRestController {

	@PostMapping(path = "/home")
	@Idempotent
	public ResponseEntity<String> index(@IdempotencyKey @RequestHeader("idempotency-key") String idempotencyKey) {

		return ResponseEntity.ok()
				.header("content-type", MimeTypeUtils.APPLICATION_JSON_VALUE)
				.header("Accept-Charset", "UTF-8")
				.body("Hello");
	}
}
