As a framework developer, there are times when annotations are used to inject desired behaviors without the need to insert logic that is irrelevant to the logic flow. A case in point is the desire to augment the behavior of a method based on a particular argument. The problem is determining which parameter. In such situations, the following is one potential to solve this problem:

```
@FooMethodAnnotation
public String greetings(@FooParameterAnnotation boolean value) {
	return "Hello world";
}
```
