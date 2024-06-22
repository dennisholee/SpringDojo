package io.forest.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FooValueResolver {
	
	@PostConstruct
	void init() {
		log.info("Loaded FooValueResolver");
	}

	Optional<Object> resolve(ProceedingJoinPoint joinPoint) throws NoSuchMethodException,
															SecurityException,
															FooParameterNotDefinedException {
		
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		Class<?>[] parameterTypes = method.getParameterTypes();

		Annotation[][] parameterAnnotations = joinPoint.getTarget()
				.getClass()
				.getMethod(method.getName(), parameterTypes)
				.getParameterAnnotations();

		for (int i = 0; i < joinPoint.getArgs().length; i++) {
			Annotation[] annotations = parameterAnnotations[i];

			for (Annotation a : annotations) {
				if (a.annotationType() == FooParameterAnnotation.class) {
					return Optional.ofNullable(joinPoint.getArgs()[i]);
				}
			}
		}

		throw new FooParameterNotDefinedException();
	}

}
