# Thalia JUnit Rule for Embedded PostgreSQL testing

[![Build Status](https://travis-ci.org/ThaliaBuecherGmbH/thalia-junit-postgres.svg?branch=master)](https://travis-ci.org/ThaliaBuecherGmbH/thalia-junit-postgres) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.thalia.junit/postgres-rule/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.thalia.junit/postgres-rule)

## Why

We often write complex and PostgreSQL specific JDBC code. This code is hard to test
using mocks and embedded databases like Derby or H2. To get full test coverage
we need a real database, we need a PostgreSQL!

This `JUnit Rule` makes it easy to spawn an embedded PostgreSQL instance
for your `JUnit` tests, control it and shut it down when you are finished.

## Minimum requirements

* Java 1.8

## How

Add the following Maven dependency to your Maven project:

```
<dependency>
    <groupId>de.thalia.junit</groupId>
    <artifactId>postgres-rule</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

Unit testing is quite easy, just take a look at the following code to see it in action:

```
import org.junit.Rule;
import org.junit.Test;

import de.thalia.junit.postgres.*;

public class PostgresEmbedderRuleTest {

    @Rule
    public PostgresEmbedderRule postgresEmbedderRule = PostgresEmbedderRule
        .builder()
        .withVersion(PostgresEmbedder.PostgreVersion.V10_3)
        .build();
    
    @Test
    public void testRule() throws SQLException {
        // Here we get the JDBC Datasource to the embedded instance
        final DataSource source = postgresEmbedderRule.getDataSource();
        
        // Do some JDBC stuff
        final ResultSet result = source
            .getConnection()
            .createStatement()
            .executeQuery("select current_date");
            
        while(result.next()) {
        }
        result.close();
        
        // We are done!
    }
}
```
