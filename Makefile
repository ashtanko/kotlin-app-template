.PHONY: default check test report treport lines md all kover diktat bump-gradle

check:
	./gradlew spotlessApply spotlessCheck detekt ktlintCheck diktatCheck --profile --daemon

default:
	make check && make md

md:
	./gradlew detektMergeMd && truncate -s0 README.md && cat config/main.md >> README.md && cat build/reports/detekt/detekt.md >> README.md && cat config/license.md >> README.md

all:
	make check && ./gradlew build && make md

test:
	./gradlew test

report:
	./gradlew jacocoTestReport

treport:
	make test && make report

lines:
	find . -name '*.kt' | xargs wc -l

kover:
	./gradlew koverHtmlReport

diktat:
	./gradlew diktatCheck

detekt:
	./gradlew detekt

bump-gradle:
	chmod +x gradlew && ./gradlew wrapper --gradle-version 9.5

.DEFAULT_GOAL := default

