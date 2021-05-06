FROM docker.pkg.github.com/navikt/dokgen:latest

USER root
RUN mkdir /secure-logs && chown apprunner /secure-logs
USER apprunner

COPY content content