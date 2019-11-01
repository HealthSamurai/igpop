.EXPORT_ALL_VARIABLES:
.PHONY: test deploy

SHELL = bash

VERSION = $(shell cat VERSION)
DATE = $(shell date)

repl:
	clj -A:test:nrepl -R:test:nrepl -e "(-main)" -r

clear:
	rm -rf target && clj -A:build

jar:
	clj -A:build

build: jar
	cp target/igpop-0.0.1-standalone.jar npm/igpop/bin/igpop.jar

test:
	clj -A:test:runner
