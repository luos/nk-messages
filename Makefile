
.PHONY: release
release:
	sbt clean package
	@echo "Release created:"
	@ls ./target/scala-*/*jar

.PHONY: test
test:
	sbt test