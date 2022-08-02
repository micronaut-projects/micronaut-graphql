# Micronaut GraphQL

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.graphql/micronaut-graphql.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.graphql%22%20AND%20a:%22micronaut-graphql%22)
[![Build Status](https://github.com/micronaut-projects/micronaut-graphql/workflows/Java%20CI/badge.svg)](https://github.com/micronaut-projects/micronaut-graphql/actions)
[![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micronaut.io/scans)

This project includes integration between [Micronaut](http://micronaut.io) and [GraphQL Java](https://www.graphql-java.com/).

The Micronaut GraphQL integration can be used together with other GraphQL integration libraries like
[GraphQL Java Tools](https://github.com/graphql-java-kickstart/graphql-java-tools) and [GraphQL SPQR](https://github.com/leangen/graphql-spqr).

## Documentation

See the [Documentation](https://micronaut-projects.github.io/micronaut-graphql/latest/guide/) for more information.

See the [Snapshot Documentation](https://micronaut-projects.github.io/micronaut-graphql/snapshot/guide/) for the current development docs.

## Examples

Examples can be found in the [examples](https://github.com/micronaut-projects/micronaut-graphql/tree/master/examples) directory.

## Snapshots and Releases

Snaphots are automatically published to [Sonatype Snapshots](https://oss.sonatype.org/content/repositories/snapshots/) using [Github Actions](https://github.com/micronaut-projects/micronaut-graphql/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to Maven Central via [Github Actions](https://github.com/micronaut-projects/micronaut-graphql/actions).

Releases are completely automated. To perform a release use the following steps:

* [Publish the draft release](https://github.com/micronaut-projects/micronaut-graphql/releases). There should be already a draft release created, edit and publish it. The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/micronaut-projects/micronaut-graphql/actions?query=workflow%3ARelease) to check it passed successfully.
* If everything went fine, [publish to Maven Central](https://github.com/micronaut-projects/micronaut-graphql/actions?query=workflow%3A"Maven+Central+Sync").
* Celebrate!
