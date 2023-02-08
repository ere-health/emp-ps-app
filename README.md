# emp-ps-app

In case you cannot use `mvn package`, you would need to put this dependency inside the `jaxb2-maven-plugin`:

```xml
<dependency>
	<groupId>org.glassfish.jaxb</groupId>
	<artifactId>jaxb-runtime</artifactId>
    <version>2.3.1</version>
</dependency>
'''