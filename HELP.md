# Getting Started

## Configuration

Before deploying, fill in the following values in their respective files.

### CodeBuild Environment Variables (`buildspec.yml`)
Set these as environment variables in your CodeBuild project (not hardcoded in the file):

| Variable | Description |
|---|---|
| `AWS_ACCOUNT_ID` | Your AWS account ID |
| `AWS_REGION` | AWS region (e.g. `us-east-1`) |
| `ECR_REPO_NAME` | ECR repository name (e.g. `my-org/my-app`) |

### AppSpec (`appspec.yml`)
Replace the placeholder comments with your actual values:

| Placeholder | Description |
|---|---|
| `SUBNET_ID_1` … `SUBNET_ID_4` | Subnet IDs for the ECS service VPC configuration |
| `SECURITY_GROUP_ID` | Security group ID attached to the ECS service |

---

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.6/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.6/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.6/reference/web/servlet.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.6/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Security](https://docs.spring.io/spring-boot/4.0.6/reference/web/spring-security.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

