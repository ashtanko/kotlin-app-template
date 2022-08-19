.PHONY: check test report treport lines

check:
	./gradlew spotlessApply spotlessCheck spotlessKotlin detekt ktlintCheck --profile --daemon

test:
	./gradlew test

report:
	./gradlew jacocoTestReport

treport:
	make test & make report

lines:
	find . -name '*.kt' | xargs wc -l

.DEFAULT_GOAL := check
