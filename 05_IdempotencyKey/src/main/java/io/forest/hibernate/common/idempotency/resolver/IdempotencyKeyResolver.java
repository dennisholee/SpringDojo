package io.forest.hibernate.common.idempotency.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import io.forest.hibernate.common.idempotency.annotation.IdempotencyKey;
import io.forest.hibernate.common.idempotency.throwable.IdempotencyKeyNotDefinedException;

public class IdempotencyKeyResolver {

	public Optional<Object> resolve(ProceedingJoinPoint joinPoint) throws NoSuchMethodException,
															SecurityException {

		if (joinPoint.getSignature() instanceof MethodSignature) {

			MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
			Method method = methodSignature.getMethod();
			String name = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();

			Annotation[][] parameterAnnotations = joinPoint.getTarget()
					.getClass()
					.getMethod(name, parameterTypes)
					.getParameterAnnotations();

			for (int i = 0; i < joinPoint.getArgs().length; i++) {
				Annotation[] annotations = parameterAnnotations[i];

				for (Annotation a : annotations) {
					if (a.annotationType() == IdempotencyKey.class) {
						return Optional.ofNullable(joinPoint.getArgs()[i]);
					}
				}
			}
		}
		
		throw new IdempotencyKeyNotDefinedException();
	}
}
