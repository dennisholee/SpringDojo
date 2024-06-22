package io.forest.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import lombok.extern.log4j.Log4j2;

@Log4j2
@SpringBootApplication
public class Application {

	@Bean
	FooValueResolver fooValueResolver() {
		return new FooValueResolver();
	}

	@Bean
	FooAnnotationHandler fooAnnotationHandler() {
		return new FooAnnotationHandler();
	}

	@FooMethodAnnotation
	public String greetings(@FooParameterAnnotation boolean value) {
		return "Hello world";
	}

	public static void main(String[] args) {
		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(FooMethodAnnotation.class) {
			@Override
			protected boolean matchSelf(MetadataReader metadataReader) {
				AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
				return metadata.hasAnnotatedMethods(getAnnotationType().getName());
			}
		});

		Set<BeanDefinition> beanDefs = provider.findCandidateComponents("io.forest.annotation");
		for (BeanDefinition bd : beanDefs) {
			if (bd instanceof AnnotatedBeanDefinition) {
				log.info("Annotation found in: {}", bd.getBeanClassName());

			}
		}

		ApplicationContext context = SpringApplication.run(Application.class, args);

		Application app = context.getBean(Application.class);

		System.out.println(app.greetings(true));
		System.out.println(app.greetings(false));
	}
}
