.PHONY: check test report treport

check:
	./gradlew spotlessApply spotlessCheck spotlessKotlin detekt ktlintCheck --profile --daemon

test:
	./gradlew test

report:
	./gradlew jacocoTestReport

treport:
	make test & make report

.DEFAULT_GOAL := check
