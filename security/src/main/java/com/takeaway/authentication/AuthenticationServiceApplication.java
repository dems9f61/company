package com.takeaway.authentication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.TimeZone;

@EnableJpaAuditing
@EnableJpaRepositories(repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
@SpringBootApplication
public class AuthenticationServiceApplication
{
  public static void main(String[] args)
  {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(AuthenticationServiceApplication.class, args);
  }
}
