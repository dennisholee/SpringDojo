package io.forest.annotation;

import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Aspect
public class FooAnnotationHandler {

	@Autowired(required = true)
	FooValueResolver resolver;

	@PostConstruct
	void init() {
		log.info("Loaded FooAnnotationHandler");
	}

	@Around("@annotation(io.forest.annotation.FooMethodAnnotation)")
	public Object handler(ProceedingJoinPoint joinPoint) throws Throwable {

		Optional<Object> resolvedValue = resolver.resolve(joinPoint);

		log.info("resolvedValue: {}", resolvedValue);

		if (resolvedValue.isPresent() && Boolean.TRUE.equals(resolvedValue.get())) {
			return joinPoint.proceed();
		}

		return "Skipped";
	}
}
