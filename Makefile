.EXPORT_ALL_VARIABLES:
.PHONY: test deploy

SHELL = bash

VERSION = $(shell cat VERSION)
DATE = $(shell date)

include .env

repl:
	clj -A:test:nrepl -R:test:nrepl -e "(-main)" -r

up:
	docker-compose up -d

stop:
	docker-compose stop

down:
	docker-compose down

clear:
	rm -rf target && clj -A:build

jar:
	clj -A:build

build: jar
	cp target/igpop-0.0.1-standalone.jar ../igpop/bin/igpop.jar

test:
	clj -A:test:runner
