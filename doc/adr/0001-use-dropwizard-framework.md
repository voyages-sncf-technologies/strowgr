# 1. Use Dropwizard framework

Date: 2017-12-08

## Status

Accepted

## Context

Admin part of Strowgr needs a web app stack which manages a REST API and provides helpers for external connectors (http,
 database).

## Decision

Dropwizard provided an easy way to implement microservice without burden of spring-like framework. 

Dropwizard provided helpful tools for monitoring (metrics).

There is no dependency injection helper but if the need appear in the future, a external tool could be used like Guice. 



## Consequences

The _main_ of the project calls dropwizard API. The code is highly dependent from this framework but it should not be 
hard to change to another one. 
