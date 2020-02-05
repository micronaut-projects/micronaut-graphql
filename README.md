# Micronaut GraphQL

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.graphql/micronaut-graphql.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.graphql%22%20AND%20a:%22micronaut-graphql%22)
[![Build Status](https://github.com/micronaut-projects/micronaut-graphql/workflows/Java%20CI/badge.svg)](https://github.com/micronaut-projects/micronaut-graphql/actions)

This project includes integration between [Micronaut](http://micronaut.io) and [GraphQL Java](https://www.graphql-java.com/).

The Micronaut GraphQL integration can be used together with other GraphQL integration libraries like
[GraphQL Java Tools](https://github.com/graphql-java-kickstart/graphql-java-tools) and [GraphQL SPQR](https://github.com/leangen/graphql-spqr).

> **IMPORTANT NOTE**: 
> - Micronaut GraphQL `1.3.0` and above requires Micronaut Core `1.3.0` as minimal dependency.
> - Micronaut GraphQL `1.2.0` and above requires Micronaut Core `1.2.x` as minimal dependency.

## Documentation ##

See the [Documentation](https://micronaut-projects.github.io/micronaut-graphql/latest/guide/index.html) for more information.

## Examples ##

There are various [examples](https://github.com/micronaut-projects/micronaut-graphql/tree/master/examples) provided in this repository.


## Snapshots and Releases

Snaphots are automatically published to [JFrog OSS](https://oss.jfrog.org/artifactory/oss-snapshot-local/) using [Github Actions](https://github.com/micronaut-projects/micronaut-graphql/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to JCenter and Maven Central via [Github Actions](https://github.com/micronaut-projects/micronaut-graphql/actions).

A release is performed with the following steps:

* [Edit the version](https://github.com/micronaut-projects/micronaut-graphql/edit/master/gradle.properties) specified by `projectVersion` in `gradle.properties` to a semantic, unreleased version. Example `1.0.0`
* [Create a new release](https://github.com/micronaut-projects/micronaut-graphql/releases/new). The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/micronaut-projects/micronaut-graphql/actions?query=workflow%3ARelease) to check it passed successfully.
* Celebrate!

