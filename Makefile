.EXPORT_ALL_VARIABLES:
.PHONY: test deploy

SHELL = bash

VERSION = $(shell cat VERSION)
DATE = $(shell date)

repl:
	clojure -A:test:nrepl -R:test:nrepl -e "(-main)" -r

clear:
	rm -rf target && clojure -A:build

jar:
	clojure -A:build

build:	jar
	mv target/igpop-0.0.1-standalone.jar npm/igpop/bin/igpop.jar

test:
	clojure -A:test:runner
