# for unix
install-dist:
	./gradlew installDist

run-dist:
	./build/install/app/bin/app

build:
	./gradlew clean build

test:
	./gradlew test

report:
	./gradlew jacocoTestReport

# for windows
install-dist-win:
	.\gradlew installDist

run-dist-win:
	.\build\install\app\bin\app

build-win:
	.\gradlew clean build

test-win:
	.\gradlew test

# PHONY
.PHONY: build