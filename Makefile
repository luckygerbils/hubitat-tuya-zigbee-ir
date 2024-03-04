.PHONY: test

DIR := $(dir $(realpath $(lastword $(MAKEFILE_LIST))))
WD := /home/groovy/scripts

test:
	docker run --rm -it -v "$(DIR)":"$(WD)" -w "$(WD)" $(docker build -q .) groovy:2.4.16 groovy -cp tst tst/all.groovy