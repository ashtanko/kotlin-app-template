.PHONY: check
check:
		./gradlew spotlessApply spotlessCheck spotlessKotlin detekt ktlintCheck --profile --daemon

.DEFAULT_GOAL := check
