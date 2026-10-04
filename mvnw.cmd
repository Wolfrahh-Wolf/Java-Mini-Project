@ECHO OFF
SETLOCAL

REM ============================================================
REM mvnw.cmd — Maven Wrapper for Vehicle Service Management System
REM
REM This wrapper uses Maven 3.9.9 downloaded by the Maven Wrapper
REM bootstrap. The Maven distribution is located in:
REM   %USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9-bin\
REM
REM If the distribution is not yet downloaded, run the bootstrap
REM step described in README.md.
REM
REM Usage:
REM   .\mvnw.cmd compile
REM   .\mvnw.cmd exec:java
REM   .\mvnw.cmd package
REM ============================================================

REM ── Set JAVA_HOME to Temurin JDK 25 if not already set ──────
SET JAVA_HOME_DEFAULT=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot
IF "%JAVA_HOME%"=="" SET JAVA_HOME=%JAVA_HOME_DEFAULT%

REM ── Locate the Maven distribution downloaded by the wrapper ──
SET MAVEN_DIST_BASE=%USERPROFILE%\.m2\wrapper\dists
SET MVN_EXE=%MAVEN_DIST_BASE%\apache-maven-3.9.9-bin\33b4b2b4\apache-maven-3.9.9\bin\mvn.cmd

REM ── Fall back: try to find any mvn.cmd in the dists folder ───
IF NOT EXIST "%MVN_EXE%" (
    FOR /R "%MAVEN_DIST_BASE%" %%F IN (mvn.cmd) DO (
        IF EXIST "%%F" SET MVN_EXE=%%F
    )
)

REM ── Final check ──────────────────────────────────────────────
IF NOT EXIST "%MVN_EXE%" (
    ECHO [mvnw] ERROR: Maven 3.9.9 not found in %MAVEN_DIST_BASE%
    ECHO [mvnw] Run the bootstrap once by executing:
    ECHO [mvnw]   java -jar .mvn\wrapper\maven-wrapper.jar
    ECHO [mvnw] Or manually install Maven and ensure mvn.cmd is on PATH.
    EXIT /B 1
)

REM ── Delegate to the real Maven binary ────────────────────────
CALL "%MVN_EXE%" %*
EXIT /B %ERRORLEVEL%
