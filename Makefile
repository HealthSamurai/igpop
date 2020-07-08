.EXPORT_ALL_VARIABLES:
.PHONY: test deploy

SHELL = bash

VERSION = $(shell cat VERSION)
DATE = $(shell date)

repl:
	clj -A:test:nrepl -m nrepl.cmdline --middleware "[cider.nrepl/cider-middleware refactor-nrepl.middleware/wrap-refactor]"

clear:
	rm -rf target && clojure -A:build

jar:
	clojure -A:build

build:	jar
	cp target/igpop-0.0.1-standalone.jar npm/igpop/bin/igpop.jar
	cp target/igpop-0.0.1-standalone.jar target/igpop.jar

test:
	clojure -A:test:runner
