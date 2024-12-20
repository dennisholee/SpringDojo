package io.forest.hibernate.common.idempotency.resolver;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public class ServiceTypeResolver {

	public String resolve(ProceedingJoinPoint joinPoint) {
		String className = joinPoint.getTarget()
				.getClass()
				.getCanonicalName();
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		String methodName = signature.getMethod()
				.getName();

		return String.format("%s.%s", className, methodName);
	}
}
