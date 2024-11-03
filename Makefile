# for unix
run:
	./gradlew bootRun --args='--spring.profiles.active=development'

report:
	./gradlew jacocoTestReport

# for windows
run-win:
	.\gradlew bootRun --args='--spring.profiles.active=development'

# PHONY
.PHONY: build